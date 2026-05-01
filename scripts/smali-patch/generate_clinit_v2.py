#!/usr/bin/env python3
"""
Generate and inject <clinit> blocks into smali files for pairip-encrypted classes.
v2: Reads actual field types from smali files to avoid type mismatches.

Usage:
  generate_clinit_v2.py <hprof_fields.json> <smali_root> [--dry-run]
"""

import json
import os
import re
import sys


def java_to_smali(java_name):
    return 'L' + java_name.replace('.', '/') + ';'


def escape_smali_string(s):
    """Escape a string for smali const-string instruction.
    Smali uses Java string literal syntax: \\n, \\t, \\r, \\\\, \\", \\uXXXX.
    Only these 6 escape sequences are valid in smali. Everything else
    must be passed through or converted to \\uXXXX."""
    result = []
    for c in s:
        if c == '\\': result.append('\\\\')
        elif c == '"': result.append('\\"')
        elif c == '\n': result.append('\\n')
        elif c == '\r': result.append('\\r')
        elif c == '\t': result.append('\\t')
        elif c == '\0': result.append('\\0')
        elif ord(c) < 0x20: result.append(f'\\u{ord(c):04x}')
        elif ord(c) > 0x7f: result.append(f'\\u{ord(c):04x}')
        else: result.append(c)
    return ''.join(result)


def find_smali_file(smali_root, java_cls):
    smali_path = java_cls.replace('.', '/')
    target_filename = smali_path.split('/')[-1] + '.smali'
    
    for root, dirs, files in os.walk(smali_root):
        for f in files:
            if f == target_filename:
                full = os.path.join(root, f)
                rel = os.path.relpath(full, smali_root)
                if rel.endswith(smali_path + '.smali'):
                    return full
    return None


def get_smali_string_fields(smali_path):
    """Read the smali file and return dict of field_name -> field_type for static String fields."""
    fields = {}
    with open(smali_path) as f:
        for line in f:
            # Match .field public static FIELDNAME:TYPE
            m = re.match(r'\.field\s+(public|private)?\s*static\s+(final\s+)?(\w+):(.+)', line.strip())
            if m:
                fname = m.group(3)
                ftype = m.group(4).strip()
                fields[fname] = ftype
    return fields


def generate_clinit_block(smali_class_ref, fields, smali_field_types):
    """Generate clinit only for fields that are actually String type in smali."""
    registers = 1
    lines = []
    lines.append('.method public static constructor <clinit>()V')
    lines.append(f'    .registers {registers}')
    lines.append('')
    
    string_fields = []
    for name, val in fields.items():
        if name.startswith('$'):
            continue
        # Only patch fields that are actually String type in the smali
        actual_type = smali_field_types.get(name, '')
        if actual_type != 'Ljava/lang/String;':
            continue  # Skip non-String fields
        
        if isinstance(val, str) and not val.startswith('<'):
            string_fields.append((name, val))
        elif val is None:
            string_fields.append((name, None))
        # Skip unresolved (<obj_not_found:...>, <arr_not_found:...>) - left null
    
    if not string_fields:
        return None  # Nothing to initialize
    
    for field_name, val in string_fields:
        if val is None:
            lines.append('    const/4 v0, 0x0')
        else:
            escaped = escape_smali_string(val)
            lines.append(f'    const-string v0, "{escaped}"')
        lines.append(f'    sput-object v0, {smali_class_ref}->{field_name}:Ljava/lang/String;')
        lines.append('')
    
    lines.append('    return-void')
    lines.append('.end method')
    
    return '\n'.join(lines)


def patch_smali_file(smali_path, smali_class_ref, fields, smali_field_types, dry_run=False):
    with open(smali_path) as f:
        content = f.read()
    
    new_clinit = generate_clinit_block(smali_class_ref, fields, smali_field_types)
    if new_clinit is None:
        return 'SKIPPED_NO_STRING_FIELDS'
    
    if '<clinit>' in content:
        pattern = r'\.method public static constructor <clinit>\(\)V.*?\.end method'
        new_content = re.sub(pattern, new_clinit, content, flags=re.DOTALL)
        action = 'REPLACED'
    else:
        field_lines = [m.start() for m in re.finditer(r'^\.field ', content, re.MULTILINE)]
        method_lines = [m.start() for m in re.finditer(r'^\.method ', content, re.MULTILINE)]
        
        if field_lines:
            last_field_start = max(field_lines)
            insert_pos = content.index('\n', last_field_start) + 1
            new_content = content[:insert_pos] + '\n' + new_clinit + '\n' + content[insert_pos:]
        elif method_lines:
            insert_pos = min(method_lines)
            new_content = content[:insert_pos] + '\n' + new_clinit + '\n\n' + content[insert_pos:]
        else:
            new_content = content + '\n\n' + new_clinit + '\n'
        action = 'ADDED'
    
    if not dry_run:
        with open(smali_path, 'w') as f:
            f.write(new_content)
    
    return action


def main():
    if len(sys.argv) < 3:
        print(f"Usage: {sys.argv[0]} <hprof_fields.json> <smali_root> [--dry-run]")
        sys.exit(1)
    
    fields_json = sys.argv[1]
    smali_root = sys.argv[2]
    dry_run = '--dry-run' in sys.argv
    
    # Restore smali files from apktool source first if needed
    with open(fields_json) as f:
        results = json.load(f)
    
    patched = 0
    skipped = 0
    fields_written = 0
    type_skipped = 0
    
    for java_cls, fields in results.items():
        smali_file = find_smali_file(smali_root, java_cls)
        if not smali_file:
            skipped += 1
            continue
        
        smali_field_types = get_smali_string_fields(smali_file)
        smali_class_ref = java_to_smali(java_cls)
        
        # Count how many fields will actually be patched
        actual_string_fields = 0
        for name, val in fields.items():
            if name.startswith('$'): continue
            if smali_field_types.get(name) == 'Ljava/lang/String;':
                if isinstance(val, str) and not val.startswith('<'):
                    actual_string_fields += 1
                elif val is None:
                    actual_string_fields += 1
        
        action = patch_smali_file(smali_file, smali_class_ref, fields, smali_field_types, dry_run)
        
        if 'SKIPPED' in action:
            skipped += 1
            continue
        
        # Check type mismatches
        for name, val in fields.items():
            if name.startswith('$'): continue
            actual_type = smali_field_types.get(name, 'NOT_IN_SMALI')
            if actual_type != 'Ljava/lang/String;' and actual_type != 'NOT_IN_SMALI':
                if isinstance(val, str) and not val.startswith('<'):
                    type_skipped += 1
        
        patched += 1
        fields_written += actual_string_fields
        status = 'ADDED' if 'ADDED' in action else 'REPLACED'
        print(f"  [{status}] {java_cls}: {actual_string_fields} String fields")
    
    print(f"\n{'DRY RUN: ' if dry_run else ''}Patched: {patched}, Skipped: {skipped}, Fields: {fields_written}")
    if type_skipped:
        print(f"Type-mismatch skipped: {type_skipped} (non-String fields in HPROF data)")


if __name__ == '__main__':
    main()