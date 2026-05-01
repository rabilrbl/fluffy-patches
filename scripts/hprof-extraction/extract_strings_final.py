#!/usr/bin/env python3
"""
HPROF string field extractor — FINAL production version.
Successfully parses hprof-conv'd HPROF dumps to extract static String field values
from target classes.

Usage:
  hprof-conv input.hprof output-std.hprof   # Convert ART format first
  python3 extract_strings_final.py output-std.hprof classes_to_dump.json output.json
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
        return {4:1, 5:2, 6:4, 7:8, 8:1, 9:2, 10:4, 11:8}.get(ty)

    hprof = open(filepath, 'rb')

    # Read header
    version = b''
    while True:
        c = hprof.read(1)
        if c == b'\x00':
            break
        version += c
    idsize = struct.unpack('>I', hprof.read(4))[0]
    hprof.read(8)
    print(f"ID Size: {idsize}")

    def readID():
        if idsize == 4: return struct.unpack('>I', hprof.read(4))[0]
        return struct.unpack('>Q', hprof.read(8))[0]
    def readu1(): return struct.unpack('>B', hprof.read(1))[0]
    def readu2(): return struct.unpack('>H', hprof.read(2))[0]
    def readu4(): return struct.unpack('>I', hprof.read(4))[0]

    # Phase 1: Strings, Load classes, and all heap records in single pass
    strings = {}
    load_classes = {}
    string_class_id = None
    target_class_ids = {}
    class_dumps = {}
    string_instances = {}
    char_arrays = {}

    total_seg = 0
    total_class_dump = 0
    total_instance = 0
    total_prim = 0

    while True:
        tag_data = hprof.read(1)
        if not tag_data:
            break
        tag = tag_data[0]
        time_val = readu4()
        length = readu4()
        
        if tag == 0x01:  # STRING
            sid = readID()
            s = hprof.read(length - idsize)
            strings[sid] = s.decode('utf-8', errors='replace')
        elif tag == 0x02:  # LOAD_CLASS
            serial = readu4()
            classobj = readID()
            stack = readu4()
            classname = readID()
            load_classes[classobj] = classname
            if classname in strings and strings[classname] == 'java.lang.String':
                string_class_id = classobj
        elif tag in (0x0C, 0x1C):  # HEAP_DUMP / HEAP_DUMP_SEGMENT
            total_seg += 1
            remaining = length
            
            while remaining > 0:
                subtag = readu1()
                remaining -= 1
                
                if subtag == 0xFF: readID(); remaining -= idsize
                elif subtag == 0x01: readID(); readID(); remaining -= idsize*2
                elif subtag == 0x02: readID(); readu4(); readu4(); remaining -= idsize+8
                elif subtag == 0x03: readID(); readu4(); readu4(); remaining -= idsize+8
                elif subtag == 0x04: readID(); readu4(); remaining -= idsize+4
                elif subtag == 0x05: readID(); remaining -= idsize
                elif subtag == 0x06: readID(); readu4(); remaining -= idsize+4
                elif subtag == 0x07: readID(); remaining -= idsize
                elif subtag == 0x08: readID(); readu4(); readu4(); remaining -= idsize+8
                elif subtag == 0x89: readID(); remaining -= idsize
                elif subtag == 0x8B: readID(); remaining -= idsize
                elif subtag == 0x8D: readID(); remaining -= idsize
                elif subtag == 0x8E: readID(); readu4(); remaining -= idsize+4
                elif subtag == 0xFE: readu4(); readID(); remaining -= 4+idsize
                
                elif subtag == 0x20:  # CLASS_DUMP
                    total_class_dump += 1
                    class_obj_id = readID(); remaining -= idsize
                    readu4(); remaining -= 4
                    readID(); remaining -= idsize  # superclass
                    readID(); remaining -= idsize  # loader
                    readID(); remaining -= idsize  # signers
                    readID(); remaining -= idsize  # prot_domain
                    readID(); remaining -= idsize  # reserved1
                    readID(); remaining -= idsize  # reserved2
                    readu4(); remaining -= 4  # instance_size
                    
                    pool_count = readu2(); remaining -= 2
                    for _ in range(pool_count):
                        readu2(); remaining -= 2
                        ty = readu1(); remaining -= 1
                        if ty == TY_OBJECT: readID(); remaining -= idsize
                        else:
                            vs = valsize(ty)
                            if vs is None: remaining = -999; break
                            hprof.read(vs); remaining -= vs
                    if remaining < 0: break
                    
                    sf_count = readu2(); remaining -= 2
                    static_fields = {}
                    for _ in range(sf_count):
                        name_id = readID(); remaining -= idsize
                        ty = readu1(); remaining -= 1
                        if ty == TY_OBJECT:
                            val = readID(); remaining -= idsize
                        else:
                            vs = valsize(ty)
                            if vs is None: remaining = -999; break
                            if vs == 1: val = readu1()
                            elif vs == 2: val = readu2()
                            elif vs == 4: val = readu4()
                            elif vs == 8: val = struct.unpack('>Q', hprof.read(8))[0]
                            remaining -= vs
                        name_str = strings.get(name_id, f"_{name_id:#x}")
                        static_fields[name_str] = (ty, val)
                    if remaining < 0: break
                    
                    if_count = readu2(); remaining -= 2
                    hprof.read(if_count * (idsize + 1)); remaining -= if_count * (idsize + 1)
                    
                    cname = strings.get(load_classes.get(class_obj_id, 0), '')
                    if cname in target_names:
                        target_class_ids[class_obj_id] = cname
                        class_dumps[cname] = static_fields
                
                elif subtag == 0x21:  # INSTANCE_DUMP
                    total_instance += 1
                    obj_id = readID(); remaining -= idsize
                    stack = readu4(); remaining -= 4
                    cls_id = readID(); remaining -= idsize
                    num_bytes = readu4(); remaining -= 4
                    
                    if cls_id == string_class_id and string_class_id is not None:
                        instance_data = hprof.read(num_bytes)
                        remaining -= num_bytes
                        # ART String instance data layout (after hprof-conv, compact strings):
                        # offset 0: shadow$_klass_ (actually some monitor/flags value)
                        # offset 4: hash (u4)
                        # offset 8: value (byte[]/char[] reference, ID)
                        # offset 12: shadow$_klass_ or class ref
                        # offset 16: coder (u1) + hashIsZero (u1) + padding
                        # coder: 0 = LATIN1 (byte[] as UTF-8), 1 = UTF16 (char[] as UTF-16BE)
                        if num_bytes >= 8 + idsize:
                            val_ref = struct.unpack('>I', instance_data[8:8+idsize])[0]
                            coder = instance_data[16] if num_bytes > 16 else 0
                            if val_ref != 0:
                                string_instances[obj_id] = (val_ref, coder)
                    else:
                        hprof.read(num_bytes)
                        remaining -= num_bytes
                
                elif subtag == 0x22:  # OBJ_ARRAY_DUMP
                    total_class_dump += 0  # don't count these
                    readID(); remaining -= idsize  # array_id
                    readu4(); remaining -= 4  # stack_trace
                    count = readu4(); remaining -= 4  # num_elements
                    readID(); remaining -= idsize  # element_class_id
                    hprof.read(idsize * count); remaining -= idsize * count
                
                elif subtag == 0x23:  # PRIM_ARRAY_DUMP
                    total_prim += 1
                    arr_id = readID(); remaining -= idsize
                    readu4(); remaining -= 4  # stack_trace
                    count = readu4(); remaining -= 4
                    ty = readu1(); remaining -= 1
                    esz = idsize if ty == TY_OBJECT else valsize(ty)
                    if esz is None or esz == 0: remaining = -999; break
                    arr_data = hprof.read(esz * count); remaining -= esz * count
                    
                    if ty == 5:  # char array (UTF-16)
                        try:
                            char_arrays[arr_id] = (5, arr_data.decode('utf-16-be', errors='replace'))
                        except:
                            char_arrays[arr_id] = (5, '')
                    elif ty == 8:  # byte array (LATIN1/UTF-8 for compact strings)
                        try:
                            char_arrays[arr_id] = (8, arr_data.decode('utf-8', errors='replace'))
                        except:
                            char_arrays[arr_id] = (8, arr_data.decode('latin-1'))
                
                elif subtag == 0xC3:  # PRIM_ARRAY_NODATA
                    readID(); readu4(); readu4(); readu1(); remaining -= idsize + 9
                
                else:
                    print(f"  Unknown sub-tag {subtag:#04x} at remaining={remaining}")
                    remaining = -1
                    break
                
                if remaining < 0:
                    print(f"  Remaining went negative: {remaining}")
                    break
        else:
            hprof.read(length)

    hprof.close()

    print(f"\nStrings: {len(strings):,}")
    print(f"Load classes: {len(load_classes):,}")
    print(f"Heap segments: {total_seg}")
    print(f"CLASS_DUMP: {total_class_dump:,}")
    print(f"INSTANCE_DUMP: {total_instance:,}")
    print(f"PRIM_ARRAY_DUMP: {total_prim:,}")
    print(f"String class ID: {string_class_id:#010x}" if string_class_id else "String class NOT FOUND!")
    print(f"String instances: {len(string_instances):,}")
    print(f"Char arrays: {len(char_arrays):,}")
    print(f"Target classes found: {len(target_class_ids)}/{len(target_names)}")

    # Resolve static fields
    print("\n=== Resolving String fields ===")
    results = {}
    resolved = 0
    unresolved = 0
    null_fields = 0

    for class_name, fields in class_dumps.items():
        class_result = {}
        for field_name, (fty, fval) in fields.items():
            if fty == TY_OBJECT:
                if fval == 0:
                    class_result[field_name] = None
                    null_fields += 1
                elif fval in string_instances:
                    arr_ref, coder = string_instances[fval]
                    if arr_ref in char_arrays:
                        arr_ty, arr_str = char_arrays[arr_ref]
                        # Verify coder matches array type (best-effort)
                        class_result[field_name] = arr_str
                        resolved += 1
                    else:
                        class_result[field_name] = f"<arr_not_found:{arr_ref:#x}>"
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
                import struct as st
                class_result[field_name] = st.unpack('>f', st.pack('>I', fval))[0]
            elif fty == TY_DOUBLE:
                import struct as st
                class_result[field_name] = st.unpack('>d', st.pack('>Q', fval))[0]
        
        if class_result:
            results[class_name] = class_result
            str_f = {k: v for k, v in class_result.items() if isinstance(v, str) and not v.startswith('<')}
            null_f = sum(1 for v in class_result.values() if v is None)
            print(f"\n  {class_name}:")
            print(f"    Resolved: {len(str_f)}, Null: {null_f}, Total: {len(class_result)}")
            for k, v in list(str_f.items())[:5]:
                v_str = str(v)[:100]
                print(f"    {k} = {v_str}")
            if len(str_f) > 5:
                print(f"    ... +{len(str_f)-5} more")

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
        if len(missing) > 10:
            print(f"  ... +{len(missing)-10} more")

    with open(output_file, 'w') as f:
        json.dump(results, f, indent=2, ensure_ascii=False)
    print(f"\nResults saved to {output_file}")

if __name__ == '__main__':
    main()