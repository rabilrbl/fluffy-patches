import os
import struct
import hashlib

XOR_KEY_HEX = "6a852fbd5ac3371c2c6848d6befb97a1"
XOR_KEY = bytes.fromhex(XOR_KEY_HEX)
print(f"XOR key length: {len(XOR_KEY)} bytes")

def xor_decrypt(data, key):
    key_len = len(key)
    return bytes(b ^ key[i % key_len] for i, b in enumerate(data))

assets_dir = "/tmp/pairip-dump/vm-bytecode/assets"
files = sorted(os.listdir(assets_dir))

for fname in files:
    fpath = os.path.join(assets_dir, fname)
    with open(fpath, 'rb') as f:
        data = f.read()
    
    size = len(data)
    magic = data[:4]
    version = struct.unpack_from('<I', data, 4)[0] if size > 7 else 0
    
    # Decrypt everything after the 8-byte header
    header = data[:8]
    encrypted_body = data[8:]
    decrypted_body = xor_decrypt(encrypted_body, XOR_KEY)
    decrypted = header + decrypted_body
    
    print(f"\n{'='*80}")
    print(f"File: {fname} (size: {size} bytes)")
    print(f"Magic: {magic} (hex: {magic.hex()})")
    print(f"Version: {version}")
    print(f"Decrypted header (first 64 bytes):")
    print(' '.join(f'{b:02x}' for b in decrypted[:64]))
    print(f"ASCII: {repr(decrypted[:64])}")
    
    # Try to find readable strings in the decrypted content
    strings_found = []
    current = []
    for b in decrypted_body:
        if 32 <= b <= 126:
            current.append(chr(b))
        else:
            if len(current) >= 4:
                strings_found.append(''.join(current))
            current = []
    if len(current) >= 4:
        strings_found.append(''.join(current))
    
    if strings_found:
        print(f"Readable strings ({len(strings_found)}):")
        for s in strings_found[:50]:
            print(f"  '{s}'")
    else:
        print("No readable strings found in decrypted body")
