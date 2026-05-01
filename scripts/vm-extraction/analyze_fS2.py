import struct

XOR_KEY = bytes.fromhex("6a852fbd5ac3371c2c6848d6befb97a1")

def xor_decrypt(data, key):
    key_len = len(key)
    return bytes(b ^ key[i % key_len] for i, b in enumerate(data))

fpath = "/tmp/pairip-dump/vm-bytecode/assets/fS2NPonHRPamHuOE"
with open(fpath, 'rb') as f:
    data = f.read()

header = data[:8]
body = data[8:]
decrypted = header + xor_decrypt(body, XOR_KEY)

print(f"Total size: {len(data)} bytes")
print(f"Header: {header.hex()}")
print(f"Magic: {header[:4]} ({header[:4].hex()})")
print(f"Version: {struct.unpack_from('<I', data, 4)[0]}")

# After header, look at bytes 8-12 for potential structure info
print(f"\nBytes 8-16 (raw): {data[8:16].hex()}")
print(f"Bytes 8-16 (decrypted): {decrypted[8:16].hex()}")

# Look for long ASCII strings in the decrypted content
strings_found = []
current = []
for i, b in enumerate(decrypted[8:]):
    if 32 <= b <= 126:
        current.append(chr(b))
    else:
        if len(current) >= 6:
            strings_found.append((i - len(current) + 8, ''.join(current)))
        current = []
if len(current) >= 6:
    strings_found.append((len(decrypted) - len(current), ''.join(current)))

print(f"\nLong strings (>=6 chars) found at offsets:")
for offset, s in strings_found:
    print(f"  offset {offset}: '{s}'")

# Also look for "java", "org", "android", "com/" patterns in decrypted data
for pattern in [b'java', b'org/', b'com/', b'android', b'kEpRxMC', b'Rso', b'ftp', b'commons', b'http', b'https', b'string', b'String', b'class']:
    idx = decrypted.find(pattern)
    if idx >= 0:
        context = decrypted[max(0,idx-20):idx+40]
        print(f"\nFound '{pattern.decode('ascii', errors='replace')}' at offset {idx}:")
        print(f"  Context: {context}")
