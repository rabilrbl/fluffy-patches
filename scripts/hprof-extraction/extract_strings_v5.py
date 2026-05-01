#!/usr/bin/env python3
"""
HPROF string field extractor — based on AOSP's hprofdump.py format parsing.

Extracts static String field values from target classes in an HPROF heap dump.

Usage:
  hprof-conv input.hprof output-std.hprof   # Convert ART format first
  python3 extract_strings_v5.py output-std.hprof classes_to_dump.json output.json
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

    # Type constants (from HPROF spec / AOSP)
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
        m = {TY_BOOLEAN: 1, TY_CHAR: 2, TY_FLOAT: 4, TY_DOUBLE: 8,
             TY_BYTE: 1, TY_SHORT: 2, TY_INT: 4, TY_LONG: 8}
        if ty in m:
            return m[ty]
        return None  # TY_OBJECT — size depends on idsize

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

    # ---- Pass 1: Strings and Load Classes ----
    strings = {}
    load_classes = {}
    string_class_id = None

    print("Pass 1: Collecting STRING and LOAD_CLASS...")
    while True:
        pos = hprof.tell()
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
        elif tag in (0x0C, 0x1C):
            # Save position for Pass 2
            hprof.seek(length, 1)
        else:
            hprof.read(length)

    print(f"  Strings: {len(strings):,}")
    print(f"  Load classes: {len(load_classes):,}")
    print(f"  String class ID: {string_class_id:#010x}" if string_class_id else "  String class NOT FOUND!")

    target_class_ids = {}
    for classobj, classname in load_classes.items():
        if classname in strings:
            name = strings[classname]
            if name in target_names:
                target_class_ids[classobj] = name
    print(f"  Found {len(target_class_ids)}/{len(target_names)} target classes")

    # ---- Pass 2: Heap dump sub-records ----
    print("\nPass 2: Parsing heap dump...")
    hprof.seek(pos)  # Back to start of heap records

    class_dumps = {}       # class_name -> {field_name: (type, value)}
    string_instances = {}  # obj_id -> char_array_id
    char_arrays = {}       # array_id -> decoded_string
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
                readID(); remaining -= idsize
            elif subtag == 0x8B:  # ROOT_DEBUGGER
                readID(); remaining -= idsize
            elif subtag == 0x8D:  # ROOT_VM_INTERNAL
                readID(); remaining -= idsize
            elif subtag == 0x8E:  # ROOT_JNI_MONITOR
                readID(); readu4(); remaining -= idsize + 4
            elif subtag == 0xFE:  # HEAP_DUMP_INFO
                readu4(); readID(); remaining -= 4 + idsize
            elif subtag == 0x20:  # CLASS_DUMP
                class_obj_id = readID(); remaining -= idsize
                readu4(); remaining -= 4  # stack_trace
                readID(); remaining -= idsize  # superclass
                readID(); remaining -= idsize  # loader
                readID(); remaining -= idsize  # signers
                readID(); remaining -= idsize  # prot_domain
                readID(); remaining -= idsize  # reserved1
                readID(); remaining -= idsize  # reserved2
                readu4(); remaining -= 4  # instance_size
                
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
                
                # Static fields — this is what we want!
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
                
                if class_obj_id in target_class_ids:
                    class_name = target_class_ids[class_obj_id]
                    class_dumps[class_name] = static_fields

            elif subtag == 0x21:  # INSTANCE_DUMP
                obj_id = readID(); remaining -= idsize
                readu4(); remaining -= 4  # stack_trace
                cls_id = readID(); remaining -= idsize
                num_bytes = readu4(); remaining -= 4
                instance_data = hprof.read(num_bytes); remaining -= num_bytes
                
                if cls_id == string_class_id:
                    # String instance data layout (after hprof-conv):
                    # Standard HPROF includes Object header BUT hprof-conv from ART
                    # strips the klass pointer. The remaining layout is:
                    # shadow$_monitor_ (4 bytes) | value (ID) | hash (4) | coder (1) | hashIsZero (1)
                    # So value is at offset 4 for idsize=4
                    # BUT — hprof-conv may or may not strip klass. Let me try both offsets.
                    if num_bytes >= 4 + idsize:
                        # Try offset 0 (no header at all)
                        val_off0 = struct.unpack('>I', instance_data[0:idsize])[0] if idsize == 4 else struct.unpack('>Q', instance_data[0:8])[0]
                        # Try offset 4 (after monitor only)
                        val_off4 = struct.unpack('>I', instance_data[4:4+idsize])[0] if idsize == 4 else struct.unpack('>Q', instance_data[4:4+8])[0]
                        # Try offset 8 (after klass + monitor)
                        val_off8 = struct.unpack('>I', instance_data[8:8+idsize])[0] if idsize == 4 and num_bytes >= 8+idsize else 0
                        
                        # Use whichever offset gives a non-zero valid char[] reference
                        for val_ref in [val_off4, val_off8, val_off0]:
                            if val_ref != 0:
                                string_instances[obj_id] = val_ref
                                break

            elif subtag == 0x22:  # OBJ_ARRAY_DUMP
                readID(); remaining -= idsize  # array_id
                readu4(); remaining -= 4  # stack_trace
                readID(); remaining -= idsize  # class_id
                count = readu4(); remaining -= 4
                hprof.read(idsize * count); remaining -= idsize * count

            elif subtag == 0x23:  # PRIM_ARRAY_DUMP
                arr_id = readID(); remaining -= idsize
                readu4(); remaining -= 4  # stack_trace
                count = readu4(); remaining -= 4
                ty = readu1(); remaining -= 1
                
                if ty == TY_OBJECT:
                    esz = idsize
                else:
                    esz = valsize(ty)
                    if esz is None:
                        esz = 0
                
                arr_data = hprof.read(esz * count); remaining -= esz * count
                
                if ty == TY_CHAR and count > 0:  # char array = String.value
                    char_arrays[arr_id] = arr_data.decode('utf-16-be', errors='replace')

            elif subtag == 0xC3:  # PRIM_ARRAY_NODATA
                readID(); remaining -= idsize
                readu4(); remaining -= 4
                readu4(); remaining -= 4
                readu1(); remaining -= 1

            else:
                print(f"  Unknown sub-tag {subtag:#04x} (remaining={remaining}), seeking to end of segment")
                if remaining > 0:
                    hprof.read(remaining)
                    remaining = 0
                break

    hprof.close()

    # Stats
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
                    # Not a String instance or not in heap
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