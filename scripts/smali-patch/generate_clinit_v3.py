#!/usr/bin/env python3
"""
Generate and inject <clinit> blocks into smali files for pairip-encrypted classes.
v3: Bulletproof string escaping for smali.

Usage:
  generate_clinit_v3.py <hprof_fields.json> <smali_root> [--dry-run]
"""

import json
import os
import re
import sys


def java_to_smali(java_name):
    return 'L' + java_name.replace('.', '/') + ';'


def escape_smali_string(s):
    """Escape a string for smali const-string instruction.
    
    Smali supports these escape sequences inside string literals:
      \\n \\t \\r \\\\ \\\" \\0 \\uXXXX
    
    Any other \\X is INVALID and will cause a smali compile error.
    We build the output character-by-character as raw bytes to avoid
    any Python string interpretation issues.
    """
    out = []
    for c in s:
        cp = ord(c)
        if cp == 0x5c:        # backslash \
            out.extend([0x5c, 0x5c])  # -> \\
        elif cp == 0x22:      # double quote "
            out.extend([0x5c, 0x22])  # -> \\"
        elif cp == 0x0a:       # newline
            out.extend([0x5c, 0x6e])  # -> \\n
        elif cp == 0x0d:       # carriage return
            out.extend([0x5c, 0x72])  # -> \\r
        elif cp == 0x09:       # tab
            out.extend([0x5c, 0x74])  # -> \\t
        elif cp == 0x00:       # null
            out.extend([0x5c, 0x30])  # -> \\0
        elif cp < 0x20:        # other control chars
            for b in f'\\u{cp:04x}'.encode('ascii'):
                out.append(b)
        elif cp > 0x7f:        # non-ASCII
            for b in f'\\u{cp:04x}'.encode('ascii'):
                out.append(b)
        elif cp < 0x80:        # normal printable ASCII
            out.append(cp)
        else:
            # Fallback for any missed cases
            for b in c.encode('utf-8'):
                out.append(b)
    return bytes(out)


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
            m = re.match(r'\.field\s+(public|private)?\s*static\s+(final\s+)?(\w+):(.+)', line.strip())
            if m:
                fname = m.group(3)
                ftype = m.group(4).strip()
                fields[fname] = ftype
    return fields


def generate_clinit_block(smali_class_ref, fields, smali_field_types):
    """Generate clinit only for fields that are actually String type in smali.
    
    Returns bytes to avoid any Python string escape interpretation issues.
    """
    string_fields = []
    for name, val in fields.items():
        if name.startswith('$'):
            continue
        actual_type = smali_field_types.get(name, '')
        if actual_type != 'Ljava/lang/String;':
            continue
        if isinstance(val, str) and not val.startswith('<'):
            string_fields.append((name, val))
        elif val is None:
            string_fields.append((name, None))
    
    if not string_fields:
        return None
    
    # Build as bytes
    parts = []
    parts.append(b'.method public static constructor <clinit>()V\n')
    parts.append(b'    .registers 1\n\n')
    
    for field_name, val in string_fields:
        if val is None:
            parts.append(b'    const/4 v0, 0x0\n')
        else:
            escaped = escape_smali_string(val)
            # Use const-string/jumbo to support string tables >65K entries
            parts.append(b'    const-string/jumbo v0, "')
            parts.append(escaped)
            parts.append(b'"\n')
        parts.append(f'    sput-object v0, {smali_class_ref}->{field_name}:Ljava/lang/String;\n'.encode())
        parts.append(b'\n')
    
    parts.append(b'    return-void\n')
    parts.append(b'.end method')
    
    return b''.join(parts)


def patch_smali_file(smali_path, smali_class_ref, fields, smali_field_types, dry_run=False):
    with open(smali_path, 'rb') as f:
        content = f.read()
    
    new_clinit = generate_clinit_block(smali_class_ref, fields, smali_field_types)
    if new_clinit is None:
        return 'SKIPPED_NO_STRING_FIELDS'
    
    if b'<clinit>' in content:
        # Manual find-replace (re.sub corrupts binary data with control chars)
        start_marker = b'.method public static constructor <clinit>()V'
        end_marker = b'.end method'
        start_idx = content.find(start_marker)
        if start_idx >= 0:
            search_from = start_idx + len(start_marker)
            end_idx = content.find(end_marker, search_from)
            if end_idx >= 0:
                end_idx += len(end_marker)
                new_content = content[:start_idx] + new_clinit + content[end_idx:]
            else:
                new_content = content  # fallback
        else:
            new_content = content  # fallback
        action = 'REPLACED'
    else:
        field_lines = [m.start() for m in re.finditer(rb'^\.field ', content, re.MULTILINE)]
        method_lines = [m.start() for m in re.finditer(rb'^\.method ', content, re.MULTILINE)]
        
        if field_lines:
            last_field_start = max(field_lines)
            insert_pos = content.index(b'\n', last_field_start) + 1
            new_content = content[:insert_pos] + b'\n' + new_clinit + b'\n' + content[insert_pos:]
        elif method_lines:
            insert_pos = min(method_lines)
            new_content = content[:insert_pos] + b'\n' + new_clinit + b'\n\n' + content[insert_pos:]
        else:
            new_content = content + b'\n\n' + new_clinit + b'\n'
        action = 'ADDED'
    
    if not dry_run:
        with open(smali_path, 'wb') as f:
            f.write(new_content)
    
    return action


def main():
    if len(sys.argv) < 3:
        print(f"Usage: {sys.argv[0]} <hprof_fields.json> <smali_root> [--dry-run]")
        sys.exit(1)
    
    fields_json = sys.argv[1]
    smali_root = sys.argv[2]
    dry_run = '--dry-run' in sys.argv
    
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