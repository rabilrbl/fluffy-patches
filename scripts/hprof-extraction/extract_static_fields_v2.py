#!/usr/bin/env python3
"""
HPROF heap dump parser — extracts static String field values from target classes.

Handles ART-specific sub-records:
  0xFE = HEAP_DUMP_INFO (u1 heap_type + ID heap_name)
  0x89 = ROOT_INTERNED_STRING (ID object_id)
  0x8B = ROOT_DEBUGGER (ID object_id)
  0x8D = ROOT_VM_INTERNAL (ID object_id)
  0x8E = ROOT_JNI_MONITOR (ID object_id + u4 thread_serial)
  0xC3 = PRIM_ARRAY_NODATA_DUMP
"""
import struct
import json
import sys

filepath = '/tmp/pairip-dump/jiotv-heap.hprof'
targets_file = '/tmp/pairip-dump/classes_to_dump.json'
output_file = '/tmp/pairip-dump/hprof_static_fields.json'
id_size = 4

with open(targets_file) as f:
    targets = json.load(f)
target_names = set(targets.keys())

# Pass 1: collect STRING and LOAD_CLASS records
strings = {}
load_class = {}
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
        data = f.read(length)

        if tag == 0x01:  # STRING
            sid = struct.unpack('>I', data[:id_size])[0]
            s = data[id_size:]
            try:
                strings[sid] = s.decode('utf-8', errors='replace')
            except:
                strings[sid] = s.hex()
        elif tag == 0x02:  # LOAD_CLASS
            cs = struct.unpack('>I', data[:4])[0]
            coid = struct.unpack('>I', data[4:8])[0]
            st = struct.unpack('>I', data[8:12])[0]
            cnid = struct.unpack('>I', data[12:16])[0]
            load_class[coid] = cnid
            if cnid in strings and strings[cnid] == 'java.lang.String':
                string_class_id = coid

print(f"  Strings: {len(strings)}, Load classes: {len(load_class)}")
print(f"  String class ID: {string_class_id:#010x}" if string_class_id else "  String class NOT FOUND!")

# Build target class -> obj_id mapping
target_obj_ids = {}
for coid, cnid in load_class.items():
    if cnid in strings:
        name = strings[cnid]
        if name in target_names:
            target_obj_ids[coid] = name
            target_obj_ids_rev = {}  # will build below
# Reverse mapping
target_obj_ids_rev = {name: coid for coid, name in target_obj_ids.items()}
print(f"  Found {len(target_obj_ids)}/{len(target_names)} target classes")

# Pass 2: parse heap dump sub-records
print("\nPass 2: Parsing heap dump...")

class_dumps = {}       # class_obj_id -> {field_name: value}
string_instances = {}  # obj_id -> (char_array_ref, num_bytes, instance_data)
char_arrays = {}       # array_id -> string_value

with open(filepath, 'rb') as f:
    # Skip header
    while True:
        b = f.read(1)
        if b == b'\x00':
            break
    f.read(4)
    f.read(8)

    while True:
        tag_data = f.read(1)
        if not tag_data:
            break
        tag = tag_data[0]
        f.read(4)  # time
        length = struct.unpack('>I', f.read(4))[0]
        data = f.read(length)

        if tag not in (0x0C, 0x1C):  # HEAP_DUMP or HEAP_DUMP_SEGMENT
            continue

        pos = 0
        stats = {}
        while pos < len(data):
            if pos >= len(data) - 1:
                break
            sub_tag = data[pos]
            pos += 1
            stats[sub_tag] = stats.get(sub_tag, 0) + 1

            try:
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
                # ART-specific roots
                elif sub_tag == 0x89:  # ROOT_INTERNED_STRING
                    pos += id_size
                elif sub_tag == 0x8B:  # ROOT_DEBUGGER
                    pos += id_size
                elif sub_tag == 0x8D:  # ROOT_VM_INTERNAL
                    pos += id_size
                elif sub_tag == 0x8E:  # ROOT_JNI_MONITOR
                    pos += id_size + 4
                # HEAP_DUMP_INFO
                elif sub_tag == 0xFE:  # HEAP_DUMP_INFO
                    pos += 1 + id_size  # u1 heap_type + ID heap_name_string_id
                # CLASS_DUMP
                elif sub_tag == 0x20:
                    class_obj_id = struct.unpack('>I', data[pos:pos+4])[0]; pos += 4
                    pos += 4  # stack_trace_serial
                    pos += id_size  # superclass_id
                    pos += id_size * 3  # class_loader_id, signers_id, prot_domain_id
                    pos += id_size * 2  # reserved
                    pos += 4  # instance_size
                    
                    # constant_pool: u2 count + count*(u2+ID)
                    cp_count = struct.unpack('>H', data[pos:pos+2])[0]; pos += 2
                    pos += cp_count * (2 + id_size)
                    
                    # static_fields: u2 count + count*(ID name + u1 type + value)
                    sf_count = struct.unpack('>H', data[pos:pos+2])[0]; pos += 2
                    
                    value_sizes = {2: id_size, 4: 1, 5: 2, 6: 4, 7: 8, 8: 1, 9: 2, 10: 4, 11: 8}
                    static_fields = {}
                    for _ in range(sf_count):
                        name_id = struct.unpack('>I', data[pos:pos+id_size])[0]; pos += id_size
                        ftype = data[pos]; pos += 1
                        vsize = value_sizes.get(ftype, id_size)
                        fvalue = data[pos:pos+vsize]; pos += vsize
                        
                        name_str = strings.get(name_id, f"unknown_{name_id:#x}")
                        static_fields[name_str] = (ftype, fvalue)
                    
                    # instance_fields: u2 count + count*(ID name + u1 type)
                    if_count = struct.unpack('>H', data[pos:pos+2])[0]; pos += 2
                    pos += if_count * (id_size + 1)
                    
                    # Store if this is a target class
                    if class_obj_id in target_obj_ids:
                        class_name = target_obj_ids[class_obj_id]
                        class_dumps[class_name] = static_fields
                
                # INSTANCE_DUMP
                elif sub_tag == 0x21:
                    pos += 4  # stack_trace_serial
                    obj_id = struct.unpack('>I', data[pos:pos+4])[0]; pos += 4
                    pos += 4  # stack_trace_serial2
                    cls_id = struct.unpack('>I', data[pos:pos+4])[0]; pos += 4
                    num_bytes = struct.unpack('>I', data[pos:pos+4])[0]; pos += 4
                    
                    if cls_id == string_class_id and num_bytes >= 8:
                        instance_data = data[pos:pos+num_bytes]
                        # ART String instance: first id_size bytes = char[] reference for value field
                        # But actually in ART's hprof, it includes Object header fields
                        # ART String: shadow$_klass_(ID) + shadow$_monitor_(4) = id_size+4
                        # Then: value(char[])=ID, hash(int)=4, coder(byte)=1, hashIsZero(boolean)=1
                        # Total Object header: id_size + 4 = 8 bytes (for id_size=4)
                        # String instance data in hprof EXCLUDES the Object header
                        # Wait - hprof INCLUDES the header? Let me check...
                        # Actually hprof INSTANCE_DUMP data includes ALL instance fields including super
                        # Object fields: shadow$_klass_(ID) + shadow$_monitor_(u4) = 8 bytes
                        # String fields: value(Reference=ID), hash(u4), hashIsZero(boolean=1), coder(byte=1)
                        # Total = 8 + 4 + 4 + 1 + 1 = 18, aligned to 20 (with 2 padding)
                        # So: offset 0: shadow$_klass_ (4), offset 4: shadow$_monitor_ (4),
                        #     offset 8: String.value (4), offset 12: String.hash (4),
                        #     offset 16: String.hashIsZero (1), offset 17: String.coder (1)
                        #     offset 18-19: padding
                        
                        if num_bytes >= 12:
                            # value field at offset 8 (after Object.klass + Object.monitor)
                            value_ref = struct.unpack('>I', instance_data[8:12])[0]
                            string_instances[obj_id] = (value_ref, num_bytes, instance_data)
                    
                    pos += num_bytes
                
                # OBJ_ARRAY_DUMP
                elif sub_tag == 0x22:
                    pos += 4  # stack_trace
                    pos += id_size  # array_object_id
                    pos += id_size  # class_object_id
                    count = struct.unpack('>I', data[pos:pos+4])[0]; pos += 4
                    pos += count * id_size
                
                # PRIMITIVE_ARRAY_DUMP
                elif sub_tag == 0x23:
                    pos += 4  # stack_trace
                    arr_id = struct.unpack('>I', data[pos:pos+4])[0]; pos += 4
                    count = struct.unpack('>I', data[pos:pos+4])[0]; pos += 4
                    elem_type = data[pos]; pos += 1
                    
                    elem_sizes = {4: 1, 5: 2, 6: 2, 7: 4, 8: 4, 9: 8, 10: 1, 11: 2}
                    esize = elem_sizes.get(elem_type, 0)
                    
                    if elem_type == 5:  # char array (UTF-16 BE)
                        arr_data = data[pos:pos+count*esize]
                        try:
                            chars = arr_data.decode('utf-16-be', errors='replace')
                            char_arrays[arr_id] = chars
                        except:
                            pass
                    
                    pos += count * esize
                
                # PRIM_ARRAY_NODATA_DUMP (ART-specific)
                elif sub_tag == 0xC3:
                    pos += 4 + id_size + 4 + 1  # stack_trace + array_id + count + type
                    # No data payload for this tag
                
                else:
                    print(f"  Unknown sub-tag {sub_tag:#04x} at offset {pos-1}, stopping segment")
                    break
            except (struct.error, IndexError) as e:
                print(f"  Parse error at sub-tag {sub_tag:#04x}, pos {pos}: {e}")
                break

print(f"\nSub-record stats:")
tag_names = {
    0x01: 'JNI_GLOBAL', 0x02: 'JNI_LOCAL', 0x03: 'JAVA_FRAME', 0x04: 'NATIVE_STACK',
    0x05: 'STICKY_CLASS', 0x06: 'THREAD_BLOCK', 0x07: 'MONITOR_USED', 0x08: 'THREAD_OBJ',
    0x20: 'CLASS_DUMP', 0x21: 'INSTANCE_DUMP', 0x22: 'OBJ_ARRAY_DUMP', 0x23: 'PRIM_ARRAY_DUMP',
    0x89: 'INTERNED_STRING', 0x8B: 'DEBUGGER', 0x8D: 'VM_INTERNAL', 0x8E: 'JNI_MONITOR',
    0xFE: 'HEAP_DUMP_INFO', 0xC3: 'PRIM_NODATA',
}
for tag, count in sorted(stats.items(), key=lambda x: -x[1]):
    name = tag_names.get(tag, f'UNKNOWN_{tag:#04x}')
    print(f"  {name} ({tag:#04x}): {count}")

print(f"\nFound {len(class_dumps)} target class dumps")
print(f"Found {len(string_instances)} String instances")
print(f"Found {len(char_arrays)} char arrays")

# Resolve static String fields
results = {}
unresolved = 0
resolved = 0
null_fields = 0

for class_name, fields in class_dumps.items():
    class_result = {}
    for field_name, (ftype, fvalue) in fields.items():
        if ftype == 2:  # Object reference (String)
            str_obj_id = struct.unpack('>I', fvalue)[0]
            if str_obj_id == 0:
                class_result[field_name] = None
                null_fields += 1
            elif str_obj_id in string_instances:
                value_ref, num_bytes, inst_data = string_instances[str_obj_id]
                if value_ref in char_arrays:
                    class_result[field_name] = char_arrays[value_ref]
                    resolved += 1
                else:
                    class_result[field_name] = f"<char_arr_{value_ref:#x}_not_found>"
                    unresolved += 1
            else:
                # Try to find string via STRING records (some strings are interned)
                class_result[field_name] = f"<str_obj_{str_obj_id:#x}_not_found>"
                unresolved += 1
        elif ftype == 4:  # boolean
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
    
    if class_result:
        results[class_name] = class_result
        string_fields = {k: v for k, v in class_result.items() if isinstance(v, str) and v is not None and not v.startswith('<')}
        print(f"\n{class_name}: {len(string_fields)} resolved string fields out of {len(class_result)} total")
        for k, v in list(string_fields.items())[:5]:
            v_str = str(v)[:80]
            print(f"  {k} = {v_str}")
        if len(string_fields) > 5:
            print(f"  ... and {len(string_fields)-5} more")

print(f"\n=== SUMMARY ===")
print(f"Resolved String fields: {resolved}")
print(f"Unresolved references: {unresolved}")
print(f"Null fields: {null_fields}")
print(f"Total classes with data: {len(results)}")

# Save results
with open(output_file, 'w') as f:
    json.dump(results, f, indent=2, ensure_ascii=False)
print(f"\nSaved to {output_file}")