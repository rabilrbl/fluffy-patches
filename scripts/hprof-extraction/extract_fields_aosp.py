#!/usr/bin/env python3
"""
Extract static String field values from HPROF heap dump for target classes.
Uses modified AOSP hprofdump.py parsing logic to avoid alignment issues.

Usage:
  hprof-conv input.hprof output-std.hprof
  python3 extract_fields_aosp.py output-std.hprof classes_to_dump.json output.json
"""
import struct
import json
import sys
import os

# Add AOSP hprofdump to path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

def main():
    filepath = sys.argv[1] if len(sys.argv) > 1 else '/tmp/pairip-dump/jiotv-heap-std.hprof'
    targets_file = sys.argv[2] if len(sys.argv) > 2 else '/tmp/pairip-dump/classes_to_dump.json'
    output_file = sys.argv[3] if len(sys.argv) > 3 else '/tmp/pairip-dump/hprof_static_fields.json'
    
    with open(targets_file) as f:
        targets = json.load(f)
    target_names = set(targets.keys())
    
    # Import and use AOSP's parser
    from hprofdump import HProfParser
    
    parser = HProfParser(filepath)
    parser.parse()
    
    print(f"Strings: {len(parser.strings):,}")
    print(f"Load classes: {len(parser.load_classes):,}")
    
    # Build reverse: object_id -> class_name
    class_obj_to_name = {}
    string_class_id = None
    for class_obj_id, (serial, stack, name_string_id) in parser.load_classes.items():
        name = parser.strings.get(name_string_id, '')
        class_obj_to_name[class_obj_id] = name
        if name == 'java.lang.String':
            string_class_id = class_obj_id
            print(f"  String class ID: {class_obj_id:#010x}")
    
    # Find target classes in class dumps
    target_class_ids = {}
    for cls in parser.class_dumps:
        name = class_obj_to_name.get(cls.class_object_id, '')
        if name in target_names:
            target_class_ids[cls.class_object_id] = name
    
    print(f"  Found {len(target_class_ids)}/{len(target_names)} target classes in class dumps")
    
    # Check what the class_dump objects look like
    if parser.class_dumps:
        sample = parser.class_dumps[0]
        print(f"\n  Sample class_dump attributes: {[a for a in dir(sample) if not a.startswith('_')]}")
        print(f"  class_object_id: {sample.class_object_id:#010x}")
        print(f"  instance_size: {sample.instance_size}")
        print(f"  static_fields type: {type(sample.static_fields)}")
        if hasattr(sample, 'static_fields') and sample.static_fields:
            sf = sample.static_fields[0]
            print(f"  static_field type: {type(sf)}, attrs: {[a for a in dir(sf) if not a.startswith('_')]}")
            print(f"  static_field sample: {sf}")
    
    # Collect all char arrays (for String resolution)
    char_arrays = {}
    for arr in parser.prim_array_dumps:
        if arr.element_type == 5:  # char
            chars = arr.elements
            if isinstance(chars, (bytes, bytearray)):
                try:
                    char_arrays[arr.array_id] = chars.decode('utf-16-be', errors='replace')
                except:
                    char_arrays[arr.array_id] = ''
            elif isinstance(chars, str):
                char_arrays[arr.array_id] = chars
    
    # Collect String instance -> char array refs
    string_instances = {}
    for inst in parser.instance_dumps:
        if inst.class_object_id == string_class_id:
            # Parse instance data to find the 'value' field (char[] reference)
            if inst.instance_data and len(inst.instance_data) >= 8:
                data = inst.instance_data
                # String fields in order: shadow$_klass_(4) + shadow$_monitor_(4) + value(4) + hash(4) + hashIsZero(1) + coder(1)
                # value is at offset 8 for id_size=4 (after both shadow fields)
                # BUT hprof-conv may strip the klass pointer, so: shadow$_monitor_(4) + value(4) = offset 4
                for offset in [4, 8, 0]:
                    if offset + 4 <= len(data):
                        val = struct.unpack('>I', data[offset:offset+4])[0]
                        if val != 0:
                            string_instances[inst.object_id] = val
                            break
    
    print(f"\n  Instance dumps: {len(parser.instance_dumps):,}")
    print(f"  String instances found: {len(string_instances):,}")
    print(f"  Char arrays found: {len(char_arrays):,}")
    print(f"  Prim array dumps: {len(parser.prim_array_dumps):,}")
    
    # Resolve static fields for target classes
    TY_OBJECT = 2
    TY_BOOLEAN = 4
    TY_CHAR = 5
    TY_FLOAT = 6
    TY_DOUBLE = 7
    TY_BYTE = 8
    TY_SHORT = 9
    TY_INT = 10
    TY_LONG = 11
    
    results = {}
    resolved = 0
    unresolved = 0
    null_fields = 0
    
    for cls in parser.class_dumps:
        if cls.class_object_id not in target_class_ids:
            continue
        class_name = target_class_ids[cls.class_object_id]
        class_result = {}
        
        for sf in cls.static_fields:
            field_name = parser.strings.get(sf.name_string_id, f"_{sf.name_string_id:#x}")
            fty = sf.type
            fval = sf.value
            
            if fty == TY_OBJECT:
                if fval == 0:
                    class_result[field_name] = None
                    null_fields += 1
                elif fval in string_instances:
                    char_arr_id = string_instances[fval]
                    if char_arr_id in char_arrays:
                        class_result[field_name] = char_arrays[char_arr_id]
                        resolved += 1
                    else:
                        class_result[field_name] = f"<char_arr_not_found:{char_arr_id:#x}>"
                        unresolved += 1
                else:
                    class_result[field_name] = f"<obj_not_found:{fval:#x}>"
                    unresolved += 1
            elif fty == TY_BOOLEAN:
                class_result[field_name] = bool(fval)
            elif fty == TY_CHAR:
                class_result[field_name] = chr(fval) if 0 < fval < 0x10000 else fval
            elif fty in (TY_BYTE, TY_SHORT, TY_INT, TY_LONG):
                class_result[field_name] = fval
            elif fty == TY_FLOAT:
                class_result[field_name] = struct.unpack('>f', struct.pack('>I', fval))[0]
            elif fty == TY_DOUBLE:
                class_result[field_name] = struct.unpack('>d', struct.pack('>Q', fval))[0]
        
        results[class_name] = class_result
        str_f = {k: v for k, v in class_result.items() if isinstance(v, str) and not v.startswith('<')}
        print(f"\n  {class_name}:")
        print(f"    Resolved: {len(str_f)}, Null: {sum(1 for v in class_result.values() if v is None)}, Total: {len(class_result)}")
        for k, v in list(str_f.items())[:3]:
            print(f"    {k} = {str(v)[:100]}")
        if len(str_f) > 3:
            print(f"    ... +{len(str_f)-3} more")
    
    print(f"\n{'='*60}")
    print(f"SUMMARY")
    print(f"{'='*60}")
    print(f"Classes resolved: {len(results)}/{len(target_names)}")
    print(f"String fields resolved: {resolved}")
    print(f"Unresolved references: {unresolved}")
    print(f"Null fields: {null_fields}")
    
    missing = target_names - set(results.keys())
    if missing:
        print(f"\nMissing {len(missing)} classes:")
        for c in sorted(missing)[:10]:
            print(f"  {c}")
    
    with open(output_file, 'w') as f:
        json.dump(results, f, indent=2, ensure_ascii=False)
    print(f"\nResults saved to {output_file}")

if __name__ == '__main__':
    main()