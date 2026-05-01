#!/usr/bin/env python3
"""
Generate and inject <clinit> blocks into smali files for pairip-encrypted classes.
Reads the HPROF-extracted static field values and patches each smali file to 
initialize the encrypted static String fields with their runtime values.

Usage:
  generate_clinit.py <hprof_fields.json> <smali_root> [--dry-run]

The hprof_static_fields.json has class names in Java dot format mapping to 
field name → value dicts. String values get const-string/sput-object pairs.
Null fields get const/4 v0, 0x0 / sput-object.
Unresolved fields are skipped (left null).
"""

import json
import os
import re
import sys


def java_to_smali(java_name):
    """Convert java.dot.ClassName to Ldot/slash/ClassName;"""
    return 'L' + java_name.replace('.', '/') + ';'


def escape_smali_string(s):
    """Escape a string for smali const-string instruction."""
    # Smali uses Java-style string escaping
    result = []
    for c in s:
        if c == '\\':
            result.append('\\\\')
        elif c == '"':
            result.append('\\"')
        elif c == '\n':
            result.append('\\n')
        elif c == '\r':
            result.append('\\r')
        elif c == '\t':
            result.append('\\t')
        elif c == '\0':
            result.append('\\0')
        elif ord(c) < 0x20:
            result.append(f'\\u{ord(c):04x}')
        else:
            result.append(c)
    return ''.join(result)


def find_smali_file(smali_root, java_cls):
    """Find the .smali file for a Java class name."""
    smali_path = java_cls.replace('.', '/')
    target_filename = smali_path.split('/')[-1] + '.smali'
    
    for root, dirs, files in os.walk(smali_root):
        for f in files:
            if f == target_filename:
                full = os.path.join(root, f)
                # Verify the class path matches
                rel = os.path.relpath(full, smali_root)
                # Handle smali_classesX subdirs
                if rel.endswith(smali_path + '.smali'):
                    return full
    return None


def generate_clinit_block(smali_class_ref, fields):
    """Generate a <clinit> method block for the given fields.
    
    fields: dict of field_name → value (str, None, int, etc.)
    Returns smali code string.
    """
    # Count registers needed: 1 for string refs, maybe 1 for null
    registers = 1
    
    lines = []
    lines.append('.method public static constructor <clinit>()V')
    lines.append(f'    .registers {registers}')
    lines.append('')
    
    # Filter to only string fields that need initialization
    string_fields = []
    for name, val in fields.items():
        if name.startswith('$'):
            continue  # Skip $class$ metadata
        if isinstance(val, str) and not val.startswith('<'):
            string_fields.append((name, val))
        elif val is None:
            # Explicitly set to null
            string_fields.append((name, None))
        # Skip unresolved (<obj_not_found:...>, <arr_not_found:...>) - leave as null
    
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


def patch_smali_file(smali_path, smali_class_ref, fields, dry_run=False):
    """Patch a smali file to add or replace <clinit> method."""
    with open(smali_path) as f:
        content = f.read()
    
    new_clinit = generate_clinit_block(smali_class_ref, fields)
    
    if '<clinit>' in content:
        # Replace existing <clinit>
        # Pattern: .method public static constructor <clinit>()V ... .end method
        pattern = r'\.method public static constructor <clinit>\(\)V.*?\.end method'
        new_content = re.sub(pattern, new_clinit, content, flags=re.DOTALL)
        action = 'REPLACED'
    else:
        # Insert <clinit> after the last .field line, before any .method
        # Find the last .field line
        field_lines = [m.start() for m in re.finditer(r'^\.field ', content, re.MULTILINE)]
        method_lines = [m.start() for m in re.finditer(r'^\.method ', content, re.MULTILINE)]
        
        if field_lines:
            # Find the end of the last .field line
            last_field_start = max(field_lines)
            # Find next newline after last field
            insert_pos = content.index('\n', last_field_start) + 1
            # Add blank line separator before clinit
            new_content = content[:insert_pos] + '\n' + new_clinit + '\n' + content[insert_pos:]
        elif method_lines:
            # Insert before first method
            insert_pos = min(method_lines)
            new_content = content[:insert_pos] + '\n' + new_clinit + '\n\n' + content[insert_pos:]
        else:
            # Append at end
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
    
    with open(fields_json) as f:
        results = json.load(f)
    
    patched = 0
    skipped = 0
    fields_written = 0
    missing_files = []
    
    for java_cls, fields in results.items():
        # Count resolvable string fields (skip $class$ metadata and unresolved)
        resolvable = {k: v for k, v in fields.items()
                      if not k.startswith('$')
                      and (v is None or (isinstance(v, str) and not v.startswith('<')))}
        
        if not resolvable:
            skipped += 1
            continue
        
        smali_file = find_smali_file(smali_root, java_cls)
        if not smali_file:
            missing_files.append(java_cls)
            skipped += 1
            continue
        
        smali_class_ref = java_to_smali(java_cls)
        action = patch_smali_file(smali_file, smali_class_ref, fields, dry_run)
        patched += 1
        fields_written += len(resolvable)
        print(f"  [{action}] {java_cls}: {len(resolvable)} fields")
    
    print(f"\n{'DRY RUN: ' if dry_run else ''}Patched: {patched}, Skipped: {skipped}, Fields: {fields_written}")
    if missing_files:
        print(f"Missing smali files: {missing_files}")


if __name__ == '__main__':
    main()