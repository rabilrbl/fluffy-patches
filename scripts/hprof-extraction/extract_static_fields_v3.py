#!/usr/bin/env python3
"""
Parse standard-format HPROF to extract static String field values from JioTV's
pairip-encrypted classes.

Uses hprof-conv'd dump (standard Java format, no ART-specific sub-tags).
"""
import struct
import json
import sys

filepath = '/tmp/pairip-dump/jiotv-heap-std.hprof'
targets_file = '/tmp/pairip-dump/classes_to_dump.json'
output_file = '/tmp/pairip-dump/hprof_static_fields.json'
id_size = 4  # from header

with open(targets_file) as f:
    targets = json.load(f)
target_names = set(targets.keys())

# ============================================================
# Pass 1: collect STRING and LOAD_CLASS records
# ============================================================
strings = {}       # string_id -> string_value
load_class = {}    # class_object_id -> class_name_string_id
string_class_id = None

print("Pass 1: Collecting STRING and LOAD_CLASS records...")
with open(filepath, 'rb') as f:
    # Skip header
    while True:
        b = f.read(1)
        if b == b'\x00':
            break
    f.read(4)  # id_size
    f.read(8)  # timestamp

    while True:
        tag_data = f.read(1)
        if not tag_data:
            break
        tag = tag_data[0]
        f.read(4)  # time
        length = struct.unpack('>I', f.read(4))[0]
        
        if tag == 0x01:  # STRING
            data = f.read(length)
            sid = struct.unpack('>I', data[:id_size])[0]
            try:
                strings[sid] = data[id_size:].decode('utf-8', errors='replace')
            except:
                strings[sid] = data[id_size:].hex()
        elif tag == 0x02:  # LOAD_CLASS
            data = f.read(length)
            class_serial = struct.unpack('>I', data[:4])[0]
            class_obj_id = struct.unpack('>I', data[4:8])[0]
            stack_trace = struct.unpack('>I', data[8:12])[0]
            class_name_id = struct.unpack('>I', data[12:16])[0]
            load_class[class_obj_id] = class_name_id
            if class_name_id in strings and strings[class_name_id] == 'java.lang.String':
                string_class_id = class_obj_id
        else:
            f.read(length)

print(f"  Strings: {len(strings):,}")
print(f"  Load classes: {len(load_class):,}")
print(f"  String class ID: {string_class_id:#010x}" if string_class_id else "  String class NOT FOUND!")

# Build target class -> obj_id mapping
target_obj_ids = {}
for obj_id, name_id in load_class.items():
    if name_id in strings:
        name = strings[name_id]
        if name in target_names:
            target_obj_ids[obj_id] = name

print(f"  Found {len(target_obj_ids)}/{len(target_names)} target classes")

# ============================================================
# Pass 2: parse heap sub-records from HEAP_DUMP_SEGMENT records
# ============================================================
print("\nPass 2: Parsing heap dump segments...")

class_dumps = {}       # class_obj_id -> {field_name: (type, value_bytes)}
string_instances = {}  # obj_id -> (value_char_array_id, num_bytes)
char_arrays = {}       # array_obj_id -> string_value
stats = {}

# Value sizes by HPROF basic type
VALUE_SIZES = {2: id_size, 4: 1, 5: 2, 6: 4, 7: 8, 8: 1, 9: 2, 10: 4, 11: 8}

with open(filepath, 'rb') as f:
    # Skip header
    while True:
        b = f.read(1)
        if b == b'\x00':
            break
    f.read(4)  # id_size
    f.read(8)  # timestamp

    while True:
        tag_data = f.read(1)
        if not tag_data:
            break
        tag = tag_data[0]
        f.read(4)  # time
        length = struct.unpack('>I', f.read(4))[0]

        if tag not in (0x0C, 0x1C):  # Only process HEAP_DUMP / HEAP_DUMP_SEGMENT
            f.read(length)
            continue
        
        data = f.read(length)
        pos = 0

        while pos < len(data):
            if pos + 1 > len(data):
                break
            sub_tag = data[pos]; pos += 1
            stats[sub_tag] = stats.get(sub_tag, 0) + 1

            try:
                # ROOT records
                if sub_tag == 0x01:  # ROOT_JNI_GLOBAL
                    pos += id_size * 2
                elif sub_tag == 0x02:  # ROOT_JNI_LOCAL
                    pos += id_size + 4 + id_size
                elif sub_tag == 0x03:  # ROOT_JAVA_FRAME
                    pos += id_size + 4 + 4
                elif sub_tag == 0x04:  # ROOT_NATIVE_STACK
                    pos += id_size + 4
                elif sub_tag == 0x05:  # ROOT_STICKY_CLASS
                    pos += id_size
                elif sub_tag == 0x06:  # ROOT_THREAD_BLOCK
                    pos += id_size + 4
                elif sub_tag == 0x07:  # ROOT_MONITOR_USED
                    pos += id_size
                elif sub_tag == 0x08:  # ROOT_THREAD_OBJ
                    pos += id_size + 4 + 4
                elif sub_tag == 0xFF:  # ROOT_UNKNOWN
                    pos += id_size
                
                # CLASS_DUMP
                elif sub_tag == 0x20:
                    class_obj_id = struct.unpack('>I', data[pos:pos+4])[0]; pos += 4
                    pos += 4  # stack_trace_serial_number
                    pos += id_size  # superclass_class_object_id
                    pos += id_size  # class_loader_class_object_id
                    pos += id_size  # signers_class_object_id
                    pos += id_size  # protection_domain_class_object_id
                    pos += id_size * 2  # reserved1 + reserved2
                    pos += 4  # instance_size
                    
                    # Constant pool
                    cp_count = struct.unpack('>H', data[pos:pos+2])[0]; pos += 2
                    for _ in range(cp_count):
                        pos += 2 + id_size  # cp_index + value
                    
                    # Static fields
                    sf_count = struct.unpack('>H', data[pos:pos+2])[0]; pos += 2
                    static_fields = {}
                    for _ in range(sf_count):
                        name_id = struct.unpack('>I', data[pos:pos+id_size])[0]; pos += id_size
                        ftype = data[pos]; pos += 1
                        vsize = VALUE_SIZES.get(ftype, id_size)
                        fvalue = data[pos:pos+vsize]; pos += vsize
                        
                        name_str = strings.get(name_id, f"_{name_id:#x}")
                        static_fields[name_str] = (ftype, fvalue)
                    
                    # Instance field descriptors
                    if_count = struct.unpack('>H', data[pos:pos+2])[0]; pos += 2
                    pos += if_count * (id_size + 1)  # name_id + type
                    
                    # Check if this is one of our target classes
                    if class_obj_id in target_obj_ids:
                        class_name = target_obj_ids[class_obj_id]
                        class_dumps[class_name] = static_fields
                
                # INSTANCE_DUMP
                elif sub_tag == 0x21:
                    pos += 4  # stack_trace_serial_number
                    obj_id = struct.unpack('>I', data[pos:pos+4])[0]; pos += 4
                    pos += 4  # stack_trace_serial_number_2
                    cls_id = struct.unpack('>I', data[pos:pos+4])[0]; pos += 4
                    num_bytes = struct.unpack('>I', data[pos:pos+4])[0]; pos += 4
                    
                    instance_data = data[pos:pos+num_bytes]; pos += num_bytes
                    
                    # Check if this is a String instance
                    if cls_id == string_class_id and num_bytes >= 12:
                        # Standard Java String layout:
                        # Object header (klass ptr + monitor): id_size + 4 = 8 bytes
                        # String.value (char[]): id_size = 4 bytes
                        # String.hash (int): 4 bytes  
                        # String.coder (byte): 1 byte
                        # String.hashIsZero (boolean): 1 byte
                        # Note: in standard HPROF (after hprof-conv), the object header is included
                        # But hprof-conv strips the klass pointer, keeping only:
                        #   - monitor (4 bytes) ... actually standard HPROF keeps both
                        # Let's try both offsets
                        value_ref_offset_8 = 8   # After Object.klass(ID) + Object.monitor(u4)
                        value_ref_offset_4 = 4   # If klass is stripped
                        value_ref_offset_0 = 0   # If header fully stripped
                        
                        for offset in [value_ref_offset_8, value_ref_offset_4]:
                            if offset + 4 <= num_bytes:
                                vref = struct.unpack('>I', instance_data[offset:offset+4])[0]
                                if vref != 0:
                                    string_instances[obj_id] = (vref, num_bytes, offset, instance_data)
                                    break
                
                # OBJECT_ARRAY_DUMP
                elif sub_tag == 0x22:
                    pos += 4  # stack_trace_serial
                    pos += id_size  # array_object_id
                    pos += id_size  # class_object_id
                    count = struct.unpack('>I', data[pos:pos+4])[0]; pos += 4
                    pos += count * id_size
                
                # PRIMITIVE_ARRAY_DUMP
                elif sub_tag == 0x23:
                    pos += 4  # stack_trace_serial
                    arr_id = struct.unpack('>I', data[pos:pos+4])[0]; pos += 4
                    count = struct.unpack('>I', data[pos:pos+4])[0]; pos += 4
                    elem_type = data[pos]; pos += 1
                    
                    ELEM_SIZES = {4: 1, 5: 2, 6: 4, 7: 8, 8: 1, 9: 2, 10: 4, 11: 2}
                    esize = ELEM_SIZES.get(elem_type, 0)
                    
                    if elem_type == 5:  # char array (UTF-16 BE) — this is String.value
                        arr_data = data[pos:pos+count*esize]
                        try:
                            char_arrays[arr_id] = arr_data.decode('utf-16-be', errors='replace')
                        except:
                            pass
                    
                    pos += count * esize if esize else 0
                
                # HEAP_DUMP_INFO (standard format: 0xFE)
                elif sub_tag == 0xFE:
                    pos += 1 + id_size  # heap_type(u1) + heap_name_string_id(ID)
                
                else:
                    print(f"  Unknown sub-tag {sub_tag:#04x} at pos {pos-1}, remaining {len(data)-pos}")
                    break
            except (struct.error, IndexError) as e:
                print(f"  Parse error at sub-tag {sub_tag:#04x}, pos {pos}: {e}")
                break

print(f"\nSub-record stats:")
tag_names = {
    0x01: 'JNI_GLOBAL', 0x02: 'JNI_LOCAL', 0x03: 'JAVA_FRAME', 0x04: 'NATIVE_STACK',
    0x05: 'STICKY_CLASS', 0x06: 'THREAD_BLOCK', 0x07: 'MONITOR_USED', 0x08: 'THREAD_OBJ',
    0xFF: 'ROOT_UNKNOWN', 0x20: 'CLASS_DUMP', 0x21: 'INSTANCE_DUMP',
    0x22: 'OBJ_ARRAY_DUMP', 0x23: 'PRIM_ARRAY_DUMP',
    0xFE: 'HEAP_DUMP_INFO',
}
for tag, count in sorted(stats.items(), key=lambda x: -x[1]):
    name = tag_names.get(tag, f'UNKNOWN_{tag:#04x}')
    print(f"  {name} ({tag:#04x}): {count:,}")

print(f"\nFound {len(class_dumps)} target class dumps")
print(f"Found {len(string_instances):,} String instances")
print(f"Found {len(char_arrays):,} char arrays")

# ============================================================
# Resolve static String fields to actual string values
# ============================================================
print("\n" + "="*60)
print("Resolving static String fields...")
print("="*60)

results = {}
resolved = 0
unresolved = 0
null_fields = 0
non_string = 0

for class_name, fields in class_dumps.items():
    class_result = {}
    for field_name, (ftype, fvalue) in fields.items():
        if ftype == 2:  # Object reference — could be String
            str_obj_id = struct.unpack('>I', fvalue)[0]
            if str_obj_id == 0:
                class_result[field_name] = None
                null_fields += 1
            elif str_obj_id in string_instances:
                vref, nbytes, offset, inst_data = string_instances[str_obj_id]
                if vref in char_arrays:
                    class_result[field_name] = char_arrays[vref]
                    resolved += 1
                else:
                    class_result[field_name] = f"<char_arr_not_found:{vref:#x}>"
                    unresolved += 1
            else:
                # Check if this is actually a String by looking at the class
                class_result[field_name] = f"<obj_ref_not_found:{str_obj_id:#x}>"
                unresolved += 1
        elif ftype in (4, 5, 8, 9, 10, 11, 6, 7):
            # Primitive types
            vsize = VALUE_SIZES[ftype]
            if ftype == 4:  # boolean
                class_result[field_name] = bool(fvalue[0])
            elif ftype == 5:  # char
                class_result[field_name] = chr(struct.unpack('>H', fvalue)[0])
            elif ftype == 8:  # byte
                class_result[field_name] = fvalue[0]
            elif ftype == 9:  # short
                class_result[field_name] = struct.unpack('>h', fvalue)[0]
            elif ftype == 10:  # int
                class_result[field_name] = struct.unpack('>i', fvalue)[0]
            elif ftype == 11:  # long
                class_result[field_name] = struct.unpack('>q', fvalue)[0]
            elif ftype == 6:  # float
                class_result[field_name] = struct.unpack('>f', fvalue)[0]
            elif ftype == 7:  # double
                class_result[field_name] = struct.unpack('>d', fvalue)[0]
            non_string += 1
    
    if class_result:
        results[class_name] = class_result
        string_fields = {k: v for k, v in class_result.items() if isinstance(v, str) and v is not None and not v.startswith('<')}
        null_f = {k for k, v in class_result.items() if v is None}
        print(f"\n{class_name}:")
        print(f"  Resolved: {len(string_fields)}, Null: {len(null_f)}, Total: {len(class_result)}")
        for k, v in list(string_fields.items())[:3]:
            print(f"    {k} = {str(v)[:80]}")
        if len(string_fields) > 3:
            print(f"    ... +{len(string_fields)-3} more")

print(f"\n{'='*60}")
print(f"SUMMARY")
print(f"{'='*60}")
print(f"Classes resolved: {len(results)}/{len(target_names)}")
print(f"String fields resolved: {resolved}")
print(f"Unresolved references: {unresolved}")
print(f"Null fields: {null_fields}")
print(f"Non-string primitive fields: {non_string}")

# Check which target classes we're missing
found_classes = set(results.keys())
missing_classes = target_names - found_classes
if missing_classes:
    print(f"\nMissing {len(missing_classes)} classes (not in heap dump):")
    for c in sorted(missing_classes):
        print(f"  {c}")

# Save results
with open(output_file, 'w') as f:
    json.dump(results, f, indent=2, ensure_ascii=False)
print(f"\nResults saved to {output_file}")