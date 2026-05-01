#!/usr/bin/env python3
"""
HPROF heap dump parser — extracts static String field values from target classes.

Uses the standard Java HPROF format (after hprof-conv conversion).
Based on AOSP's hprofdump.py reference implementation.

Usage:
  1. Convert ART dump to standard format:
     hprof-conv input.hprof output-std.hprof
  2. Run this script:
     python3 extract_strings.py output-std.hprof classes_to_dump.json output.json
"""
import struct
import json
import sys

def main():
    filepath = sys.argv[1] if len(sys.argv) > 1 else '/tmp/pairip-dump/jiotv-heap-std.hprof'
    targets_file = sys.argv[2] if len(sys.argv) > 2 else '/tmp/pairip-dump/classes_to_dump.json'
    output_file = sys.argv[3] if len(sys.argv) > 3 else '/tmp/pairip-dump/hprof_static_fields.json'
    
    with open(targets_file) as f:
        targets = json.load(f)
    target_names = set(targets.keys())
    
    # Type constants
    TY_OBJECT = 2
    TY_BOOLEAN = 4
    TY_CHAR = 5
    TY_FLOAT = 6
    TY_DOUBLE = 7
    TY_BYTE = 8
    TY_SHORT = 9
    TY_INT = 10
    TY_LONG = 11
    
    def valsize(ty):
        return {TY_OBJECT: None, TY_BOOLEAN: 1, TY_CHAR: 2, TY_FLOAT: 4,
                TY_DOUBLE: 8, TY_BYTE: 1, TY_SHORT: 2, TY_INT: 4, TY_LONG: 8}[ty]
    
    hprof = open(filepath, 'rb')
    
    # Read header
    version = b''
    while True:
        c = hprof.read(1)
        if c == b'\x00':
            break
        version += c
    print(f"Version: {version.decode('ascii', errors='replace')}")
    
    idsize = struct.unpack('>I', hprof.read(4))[0]
    print(f"ID Size: {idsize} bytes")
    
    def readID():
        if idsize == 4:
            return struct.unpack('>I', hprof.read(4))[0]
        elif idsize == 8:
            return struct.unpack('>Q', hprof.read(8))[0]
    
    def readu1():
        return struct.unpack('>B', hprof.read(1))[0]
    
    def readu2():
        return struct.unpack('>H', hprof.read(2))[0]
    
    def readu4():
        return struct.unpack('>I', hprof.read(4))[0]
    
    def readu8():
        return struct.unpack('>Q', hprof.read(8))[0]
    
    def readN(n):
        if n == 1: return readu1()
        if n == 2: return readu2()
        if n == 4: return readu4()
        if n == 8: return readu8()
    
    def readval(ty):
        if ty == TY_OBJECT:
            return readID()
        return readN(valsize(ty))
    
    timestamp = (readu4() << 32) | readu4()
    print(f"Timestamp: {timestamp}")
    
    # Pass 1: collect STRING and LOAD_CLASS records
    strings = {}
    load_classes = {}   # class_object_id -> class_name_string_id
    string_class_id = None
    
    print("\n=== Pass 1: Collecting STRING and LOAD_CLASS ===")
    pos = hprof.tell()
    while True:
        tag_data = hprof.read(1)
        if not tag_data:
            break
        tag = tag_data[0]
        time = readu4()
        length = readu4()
        start = hprof.tell()
        
        if tag == 0x01:  # STRING
            sid = readID()
            s = hprof.read(length - idsize)
            try:
                strings[sid] = s.decode('utf-8', errors='replace')
            except:
                strings[sid] = s.hex()
        elif tag == 0x02:  # LOAD_CLASS
            serial = readu4()
            classobj = readID()
            stack = readu4()
            classname = readID()
            load_classes[classobj] = classname
            if classname in strings and strings[classname] == 'java.lang.String':
                string_class_id = classobj
        elif tag in (0x0C, 0x1C):
            # Skip heap dump data for now — we'll re-read in Pass 2
            hprof.read(length)
        else:
            hprof.read(length)
    
    print(f"  Strings: {len(strings):,}")
    print(f"  Load classes: {len(load_classes):,}")
    print(f"  String class ID: {string_class_id:#010x}" if string_class_id else "  String class NOT FOUND!")
    
    # Build target class -> obj_id mapping
    target_class_ids = {}
    for classobj, classname in load_classes.items():
        if classname in strings:
            name = strings[classname]
            if name in target_names:
                target_class_ids[classobj] = name
    
    print(f"  Found {len(target_class_ids)}/{len(target_names)} target classes")
    
    # Pass 2: parse heap dump segments for CLASS_DUMP, INSTANCE_DUMP, PRIM_ARRAY_DUMP
    print("\n=== Pass 2: Parsing heap dump segments ===")
    
    class_dumps = {}       # class_obj_id -> {field_name: (type, value)}
    string_instances = {}  # obj_id -> char_array_id (String.value reference)
    char_arrays = {}       # array_obj_id -> decoded_string
    
    hprof.seek(pos)  # Reset to start of Pass 2
    
    stats = {}
    
    while True:
        tag_data = hprof.read(1)
        if not tag_data:
            break
        tag = tag_data[0]
        time = readu4()
        length = readu4()
        
        if tag not in (0x0C, 0x1C):
            hprof.read(length)
            continue
        
        remaining = length
        
        while remaining > 0:
            subtag = readu1()
            remaining -= 1
            stats[subtag] = stats.get(subtag, 0) + 1
            
            if subtag == 0x01:  # ROOT_JNI_GLOBAL
                readID(); readID()
                remaining -= idsize * 2
            elif subtag == 0x02:  # ROOT_JNI_LOCAL
                readID(); readu4(); readu4()
                remaining -= idsize + 8
            elif subtag == 0x03:  # ROOT_JAVA_FRAME
                readID(); readu4(); readu4()
                remaining -= idsize + 8
            elif subtag == 0x04:  # ROOT_NATIVE_STACK
                readID(); readu4()
                remaining -= idsize + 4
            elif subtag == 0x05:  # ROOT_STICKY_CLASS
                readID()
                remaining -= idsize
            elif subtag == 0x06:  # ROOT_THREAD_BLOCK
                readID(); readu4()
                remaining -= idsize + 4
            elif subtag == 0x07:  # ROOT_MONITOR_USED
                readID()
                remaining -= idsize
            elif subtag == 0x08:  # ROOT_THREAD_OBJ
                readID(); readu4(); readu4()
                remaining -= idsize + 8
            elif subtag == 0xFF:  # ROOT_UNKNOWN
                readID()
                remaining -= idsize
            elif subtag == 0x89:  # ROOT_INTERNED_STRING
                readID()
                remaining -= idsize
            elif subtag == 0x8B:  # ROOT_DEBUGGER
                readID()
                remaining -= idsize
            elif subtag == 0x8D:  # ROOT_VM_INTERNAL
                readID()
                remaining -= idsize
            elif subtag == 0x8E:  # ROOT_JNI_MONITOR
                readID(); readu4()
                remaining -= idsize + 4
            elif subtag == 0xFE:  # HEAP_DUMP_INFO
                htype = readu4()
                hnameid = readID()
                remaining -= 4 + idsize
            elif subtag == 0x20:  # CLASS_DUMP
                class_obj_id = readID()
                remaining -= idsize
                stack_trace = readu4()
                remaining -= 4
                superclass = readID()
                remaining -= idsize
                loader = readID()
                remaining -= idsize
                signers = readID()
                remaining -= idsize
                prot_domain = readID()
                remaining -= idsize
                reserved1 = readID()
                remaining -= idsize
                reserved2 = readID()
                remaining -= idsize
                instance_size = readu4()
                remaining -= 4
                
                # Constant pool
                pool_count = readu2()
                remaining -= 2
                for _ in range(pool_count):
                    readu2()  # cp_index
                    remaining -= 2
                    ty = readu1()
                    remaining -= 1
                    vsize = valsize(ty) if ty != TY_OBJECT else idsize
                    hprof.read(vsize)
                    remaining -= vsize
                
                # Static fields
                sf_count = readu2()
                remaining -= 2
                static_fields = {}
                for _ in range(sf_count):
                    name_id = readID()
                    remaining -= idsize
                    ty = readu1()
                    remaining -= 1
                    val = readval(ty)
                    remaining -= (idsize if ty == TY_OBJECT else valsize(ty))
                    name_str = strings.get(name_id, f"_{name_id:#x}")
                    static_fields[name_str] = (ty, val)
                
                # Instance field descriptors
                if_count = readu2()
                remaining -= 2
                for _ in range(if_count):
                    readID()  # name_id
                    remaining -= idsize
                    readu1()  # type
                    remaining -= 1
                
                # Store if target class
                if class_obj_id in target_class_ids:
                    class_name = target_class_ids[class_obj_id]
                    class_dumps[class_name] = static_fields
                
            elif subtag == 0x21:  # INSTANCE_DUMP
                obj_id = readID()
                remaining -= idsize
                stack = readu4()
                remaining -= 4
                cls_id = readID()
                remaining -= idsize
                num_bytes = readu4()
                remaining -= 4
                instance_data = hprof.read(num_bytes)
                remaining -= num_bytes
                
                # Check if String instance
                if cls_id == string_class_id and num_bytes >= 8:
                    # String layout in hprof-conv'd format (standard HPROF):
                    # Object fields: shadow$_klass_(ID) + shadow$_monitor_(u4) = id_size + 4
                    # Then String.value(char[]): ID at offset id_size+4? No...
                    # Actually in standard HPROF, INSTANCE_DUMP includes ALL instance data
                    # but the object header (klass pointer) is NOT included — hprof-conv strips it
                    # So: offset 0 = shadow$_monitor_(u4), then instance fields
                    # Wait — let's check. In ART's output:
                    # shadow$_klass_ is an object ref (id_size bytes)  
                    # shadow$_monitor_ is an int (4 bytes)
                    # But in standard HPROF format, the class_object_id already identifies
                    # the class, so shadow$_klass_ is omitted.
                    # The instance data starts with shadow$_monitor_ (4 bytes)
                    # Then: value(char[] ref, id_size), hash(int, 4), coder(byte, 1)
                    # Total for id_size=4: 4 + 4 + 4 + 1 = 13, padded to 16
                    
                    # Try offset 0 first (no header), then offset 4 (monitor only)
                    # Actually, in hprof-conv'd format, the instance data contains
                    # ALL instance fields EXCEPT the klass pointer (which is the class_object_id).
                    # So it's: shadow$_monitor_(4) + value(ID) + hash(4) + hashIsZero(1) + coder(1)
                    # = 4 + idsize + 4 + 1 + 1 = 4 + 4 + 4 + 1 + 1 = 14 bytes
                    
                    # value is at offset 4 (after shadow$_monitor_)
                    if num_bytes >= 4 + idsize:
                        value_ref = struct.unpack('>I', instance_data[4:4+idsize])[0]
                        if value_ref != 0:
                            string_instances[obj_id] = value_ref
            
            elif subtag == 0x22:  # OBJ_ARRAY_DUMP
                arr_id = readID()
                remaining -= idsize
                stack = readu4()
                remaining -= 4
                cls_id = readID()
                remaining -= idsize
                count = readu4()
                remaining -= 4
                hprof.read(idsize * count)
                remaining -= idsize * count
            
            elif subtag == 0x23:  # PRIM_ARRAY_DUMP
                arr_id = readID()
                remaining -= idsize
                stack = readu4()
                remaining -= 4
                count = readu4()
                remaining -= 4
                ty = readu1()
                remaining -= 1
                
                esize = valsize(ty) if ty != TY_OBJECT else idsize
                arr_data = hprof.read(esize * count)
                remaining -= esize * count
                
                # Collect char arrays (for String.value)
                if ty == TY_CHAR:  # char array (UTF-16 BE)
                    try:
                        char_arrays[arr_id] = arr_data.decode('utf-16-be', errors='replace')
                    except:
                        pass
            elif subtag == 0xC3:  # PRIM_ARRAY_NODATA (ART-specific)
                arr_id = readID()
                remaining -= idsize
                stack = readu4()
                remaining -= 4
                count = readu4()
                remaining -= 4
                ty = readu1()
                remaining -= 1
                # No data payload
            
            else:
                print(f"  Unknown sub-tag {subtag:#04x}, remaining={remaining}")
                # Try to recover by seeking to end of this heap segment
                if remaining > 0:
                    hprof.read(remaining)
                    remaining = 0
                break
    
    hprof.close()
    
    tag_names = {
        0x01: 'JNI_GLOBAL', 0x02: 'JNI_LOCAL', 0x03: 'JAVA_FRAME', 0x04: 'NATIVE_STACK',
        0x05: 'STICKY_CLASS', 0x06: 'THREAD_BLOCK', 0x07: 'MONITOR_USED', 0x08: 'THREAD_OBJ',
        0xFF: 'ROOT_UNKNOWN', 0x20: 'CLASS_DUMP', 0x21: 'INSTANCE_DUMP',
        0x22: 'OBJ_ARRAY_DUMP', 0x23: 'PRIM_ARRAY_DUMP',
        0x89: 'INTERNED_STRING', 0x8B: 'DEBUGGER', 0x8D: 'VM_INTERNAL', 0x8E: 'JNI_MONITOR',
        0xFE: 'HEAP_DUMP_INFO', 0xC3: 'PRIM_NODATA',
    }
    
    print(f"\nSub-record stats:")
    for tag, count in sorted(stats.items(), key=lambda x: -x[1]):
        name = tag_names.get(tag, f'UNKNOWN_{tag:#04x}')
        print(f"  {name} ({tag:#04x}): {count:,}")
    
    print(f"\nFound {len(class_dumps)} target class dumps")
    print(f"Found {len(string_instances):,} String instances")
    print(f"Found {len(char_arrays):,} char arrays")
    
    # Resolve static String fields
    print("\n=== Resolving String fields ===")
    results = {}
    resolved = 0
    unresolved = 0
    null_fields = 0
    non_string = 0
    
    for class_name, fields in class_dumps.items():
        class_result = {}
        for field_name, (fty, fval) in fields.items():
            if fty == TY_OBJECT:
                # This could be a String reference
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
            elif fty == TY_BYTE:
                class_result[field_name] = fval & 0xFF
            elif fty == TY_SHORT:
                class_result[field_name] = fval & 0xFFFF
            elif fty == TY_INT:
                class_result[field_name] = fval
            elif fty == TY_LONG:
                class_result[field_name] = fval
            elif fty == TY_FLOAT:
                class_result[field_name] = struct.unpack('>f', struct.pack('>I', fval))[0]
            elif fty == TY_DOUBLE:
                class_result[field_name] = struct.unpack('>d', struct.pack('>Q', fval))[0]
        
        if class_result:
            results[class_name] = class_result
            str_fields = {k: v for k, v in class_result.items() 
                         if isinstance(v, str) and not v.startswith('<')}
            null_f = sum(1 for v in class_result.values() if v is None)
            print(f"\n  {class_name}:")
            print(f"    Resolved strings: {len(str_fields)}, Null: {null_f}, Total: {len(class_result)}")
            for k, v in list(str_fields.items())[:3]:
                print(f"    {k} = {str(v)[:100]}")
            if len(str_fields) > 3:
                print(f"    ... +{len(str_fields)-3} more")
    
    print(f"\n{'='*60}")
    print(f"SUMMARY")
    print(f"{'='*60}")
    print(f"Classes resolved: {len(results)}/{len(target_names)}")
    print(f"String fields resolved: {resolved}")
    print(f"Unresolved references: {unresolved}")
    print(f"Null fields: {null_fields}")
    print(f"Non-string primitive fields: {non_string}")
    
    # Missing classes
    found = set(results.keys())
    missing = target_names - found
    if missing:
        print(f"\nMissing {len(missing)} classes:")
        for c in sorted(missing)[:10]:
            print(f"  {c}")
        if len(missing) > 10:
            print(f"  ... +{len(missing)-10} more")
    
    with open(output_file, 'w') as f:
        json.dump(results, f, indent=2, ensure_ascii=False)
    print(f"\nResults saved to {output_file}")

if __name__ == '__main__':
    main()