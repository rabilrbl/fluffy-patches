#!/usr/bin/env python3
"""
HPROF string field extractor — v6 with alignment verification.
Based on careful study of AOSP hprofdump.py parsing logic.

Key insight: after hprof-conv, the CLASS_DUMP format may differ from what
we expect. We validate at runtime by checking that remaining >= 0 after
each sub-record.
"""
import struct
import json
import sys
import traceback

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
        return {TY_BOOLEAN: 1, TY_CHAR: 2, TY_FLOAT: 4, TY_DOUBLE: 8,
                TY_BYTE: 1, TY_SHORT: 2, TY_INT: 4, TY_LONG: 8}.get(ty)

    hprof = open(filepath, 'rb')

    # Read header
    version = b''
    while True:
        c = hprof.read(1)
        if c == b'\x00':
            break
        version += c
    
    idsize = struct.unpack('>I', hprof.read(4))[0]
    print(f"Version: {version.decode('ascii', errors='replace')}")
    print(f"ID Size: {idsize} bytes")

    def readID():
        if idsize == 4:
            return struct.unpack('>I', hprof.read(4))[0]
        return struct.unpack('>Q', hprof.read(8))[0]

    def readu1(): return struct.unpack('>B', hprof.read(1))[0]
    def readu2(): return struct.unpack('>H', hprof.read(2))[0]
    def readu4(): return struct.unpack('>I', hprof.read(4))[0]

    def readval(ty):
        if ty == TY_OBJECT:
            return readID()
        vs = valsize(ty)
        if vs == 1: return readu1()
        if vs == 2: return readu2()
        if vs == 4: return readu4()
        if vs == 8: return struct.unpack('>Q', hprof.read(8))[0]

    # Skip timestamp
    hprof.read(8)

    # Single-pass: collect everything
    strings = {}
    load_classes = {}
    string_class_id = None
    class_dumps = {}      # class_obj_id -> {field_name: (type, value)}
    string_instances = {} # obj_id -> char_array_id
    char_arrays = {}       # array_id -> decoded_string
    target_class_ids = {}
    stats = {}

    total_segs = 0
    total_class_dump = 0
    total_instance_dump = 0
    total_prim_array = 0

    while True:
        tag_data = hprof.read(1)
        if not tag_data:
            break
        tag = tag_data[0]
        time = readu4()
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
            total_segs += 1
            remaining = length
            seg_class_dump = 0
            seg_instance_dump = 0
            
            while remaining > 0:
                pos_before = hprof.tell()
                subtag = readu1()
                remaining -= 1
                stats[subtag] = stats.get(subtag, 0) + 1
                
                try:
                    if subtag == 0xFF:  # ROOT_UNKNOWN
                        readID(); remaining -= idsize
                    elif subtag == 0x01:  # ROOT_JNI_GLOBAL
                        readID(); readID(); remaining -= idsize * 2
                    elif subtag == 0x02:  # ROOT_JNI_LOCAL
                        readID(); readu4(); readu4(); remaining -= idsize + 8
                    elif subtag == 0x03:  # ROOT_JAVA_FRAME
                        readID(); readu4(); readu4(); remaining -= idsize + 8
                    elif subtag == 0x04:  # ROOT_NATIVE_STACK
                        readID(); readu4(); remaining -= idsize + 4
                    elif subtag == 0x05:  # ROOT_STICKY_CLASS
                        readID(); remaining -= idsize
                    elif subtag == 0x06:  # ROOT_THREAD_BLOCK
                        readID(); readu4(); remaining -= idsize + 4
                    elif subtag == 0x07:  # ROOT_MONITOR_USED
                        readID(); remaining -= idsize
                    elif subtag == 0x08:  # ROOT_THREAD_OBJ
                        readID(); readu4(); readu4(); remaining -= idsize + 8
                    elif subtag == 0x89:  # ROOT_INTERNED_STRING
                        _ = readID(); remaining -= idsize
                    elif subtag == 0x8B:  # ROOT_DEBUGGER
                        readID(); remaining -= idsize
                    elif subtag == 0x8D:  # ROOT_VM_INTERNAL
                        readID(); remaining -= idsize
                    elif subtag == 0x8E:  # ROOT_JNI_MONITOR
                        readID(); readu4(); remaining -= idsize + 4
                    elif subtag == 0xFE:  # HEAP_DUMP_INFO
                        _htype = readu4(); _hnameid = readID()
                        remaining -= 4 + idsize
                    elif subtag == 0x20:  # CLASS_DUMP
                        seg_class_dump += 1
                        total_class_dump += 1
                        class_obj_id = readID(); remaining -= idsize
                        readu4(); remaining -= 4  # stack_trace
                        readID(); remaining -= idsize  # superclass
                        readID(); remaining -= idsize  # loader
                        readID(); remaining -= idsize  # signers
                        readID(); remaining -= idsize  # prot_domain
                        readID(); remaining -= idsize  # reserved1
                        readID(); remaining -= idsize  # reserved2
                        _inst_size = readu4(); remaining -= 4
                        
                        # Constant pool
                        pool_count = readu2(); remaining -= 2
                        for _ in range(pool_count):
                            readu2(); remaining -= 2  # index
                            ty = readu1(); remaining -= 1
                            if ty == TY_OBJECT:
                                readID(); remaining -= idsize
                            else:
                                vs = valsize(ty)
                                hprof.read(vs); remaining -= vs
                        
                        # Static fields — key data!
                        sf_count = readu2(); remaining -= 2
                        static_fields = {}
                        for _ in range(sf_count):
                            name_id = readID(); remaining -= idsize
                            ty = readu1(); remaining -= 1
                            val = readval(ty)
                            remaining -= idsize if ty == TY_OBJECT else valsize(ty)
                            name_str = strings.get(name_id, f"_{name_id:#x}")
                            static_fields[name_str] = (ty, val)
                        
                        # Instance field descriptors
                        if_count = readu2(); remaining -= 2
                        for _ in range(if_count):
                            readID(); remaining -= idsize  # name
                            readu1(); remaining -= 1  # type
                        
                        # Check if this is a target class
                        cname = strings.get(load_classes.get(class_obj_id, 0), '')
                        if cname in target_names:
                            target_class_ids[class_obj_id] = cname
                            class_dumps[cname] = static_fields

                    elif subtag == 0x21:  # INSTANCE_DUMP
                        seg_instance_dump += 1
                        total_instance_dump += 1
                        obj_id = readID(); remaining -= idsize
                        readu4(); remaining -= 4  # stack_trace
                        cls_id = readID(); remaining -= idsize
                        num_bytes = readu4(); remaining -= 4
                        instance_data = hprof.read(num_bytes); remaining -= num_bytes
                        
                        if cls_id == string_class_id and string_class_id is not None:
                            # Try multiple offsets for String.value (char[] reference)
                            for offset in [4, 8, 0]:  # After shadow$_monitor_, after shadow$_klass_+monitor_, no header
                                if offset + idsize <= num_bytes:
                                    val_ref = struct.unpack('>I', instance_data[offset:offset+idsize])[0]
                                    if val_ref != 0:
                                        string_instances[obj_id] = val_ref
                                        break
                    
                    elif subtag == 0x22:  # OBJ_ARRAY_DUMP
                        readID(); remaining -= idsize
                        readu4(); remaining -= 4
                        readID(); remaining -= idsize
                        count = readu4(); remaining -= 4
                        hprof.read(idsize * count); remaining -= idsize * count
                    
                    elif subtag == 0x23:  # PRIM_ARRAY_DUMP
                        arr_id = readID(); remaining -= idsize
                        readu4(); remaining -= 4  # stack_trace
                        count = readu4(); remaining -= 4
                        ty = readu1(); remaining -= 1
                        
                        esz = idsize if ty == TY_OBJECT else (valsize(ty) if valsize(ty) else 0)
                        if esz == 0:
                            # Unknown type — can't determine size, abort segment
                            print(f"  Unknown prim type {ty} in PRIM_ARRAY_DUMP, skipping segment")
                            if remaining > 0:
                                hprof.read(remaining)
                                remaining = 0
                            break
                        
                        arr_data = hprof.read(esz * count); remaining -= esz * count
                        
                        if ty == TY_CHAR:  # char array = String.value
                            char_arrays[arr_id] = arr_data.decode('utf-16-be', errors='replace')
                    
                    elif subtag == 0xC3:  # PRIM_ARRAY_NODATA
                        readID(); remaining -= idsize
                        readu4(); readu4(); readu1(); remaining -= 4 + 4 + 1 + idsize
                    
                    else:
                        print(f"  Unknown sub-tag {subtag:#04x} at offset {pos_before}, remaining={remaining}, seg={total_segs}")
                        if remaining > 0:
                            hprof.read(remaining)
                            remaining = 0
                        break
                
                except Exception as e:
                    print(f"  Parse error at sub-tag {subtag:#04x}, seg={total_segs}: {e}")
                    traceback.print_exc()
                    # Seek to end of this segment
                    if remaining > 0:
                        hprof.read(remaining)
                        remaining = 0
                    break
                
                if remaining < 0:
                    print(f"  WARNING: remaining={remaining} after sub-tag {subtag:#04x} at offset {hprof.tell()}, seg={total_segs}")
                    break
            
            if seg_class_dump > 0 or seg_instance_dump > 0:
                if total_segs % 5000 == 0:
                    print(f"  Progress: seg {total_segs}, classes={total_class_dump}, instances={total_instance_dump}")
        else:
            hprof.read(length)

    hprof.close()

    # Build target lookup
    for classobj, classname in load_classes.items():
        if classname in strings:
            name = strings[classname]
            if name in target_names and classobj not in target_class_ids:
                target_class_ids[classobj] = name

    print(f"\nStrings: {len(strings):,}")
    print(f"Load classes: {len(load_classes):,}")
    print(f"String class ID: {string_class_id:#010x}" if string_class_id else "String class NOT FOUND!")
    print(f"Target classes found: {len(target_class_ids)}/{len(target_names)}")
    print(f"\nSub-record stats:")
    tag_names = {0x01:'JNI_GLOBAL',0x02:'JNI_LOCAL',0x03:'JAVA_FRAME',0x04:'NATIVE_STACK',
        0x05:'STICKY_CLASS',0x06:'THREAD_BLOCK',0x07:'MONITOR_USED',0x08:'THREAD_OBJ',
        0xFF:'ROOT_UNKNOWN',0x20:'CLASS_DUMP',0x21:'INSTANCE_DUMP',
        0x22:'OBJ_ARRAY_DUMP',0x23:'PRIM_ARRAY_DUMP',
        0x89:'INTERNED_STRING',0x8B:'DEBUGGER',0x8D:'VM_INTERNAL',0x8E:'JNI_MONITOR',
        0xFE:'HEAP_DUMP_INFO',0xC3:'PRIM_NODATA'}
    for tag, count in sorted(stats.items(), key=lambda x: -x[1]):
        name = tag_names.get(tag, f'UNKNOWN_{tag:#04x}')
        print(f"  {name} ({tag:#04x}): {count:,}")
    print(f"\nTotal CLASSs: {total_class_dump}, INSTANCEs: {total_instance_dump}")
    print(f"Target class dumps: {len(class_dumps)}")
    print(f"String instances: {len(string_instances):,}")
    print(f"Char arrays: {len(char_arrays):,}")

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
        
        if class_result:
            results[class_name] = class_result
            str_f = {k: v for k, v in class_result.items() if isinstance(v, str) and not v.startswith('<')}
            null_f = sum(1 for v in class_result.values() if v is None)
            print(f"\n  {class_name}:")
            print(f"    Resolved: {len(str_f)}, Null: {null_f}, Total: {len(class_result)}")
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