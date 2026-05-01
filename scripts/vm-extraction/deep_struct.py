import struct, os

XOR_KEY = bytes.fromhex("6a852fbd5ac3371c2c6848d6befb97a1")

def xor_decrypt(data, key):
    key_len = len(key)
    return bytes(b ^ key[i % key_len] for i, b in enumerate(data))

# Look deeper at the fS2NPonHRPamHuOE file structure
fpath = "/tmp/pairip-dump/vm-bytecode/assets/fS2NPonHRPamHuOE"
with open(fpath, 'rb') as f:
    data = f.read()

decrypted = data[:8] + xor_decrypt(data[8:], XOR_KEY)

# The header seems to be: 00 49 41 50 02 00 00 00 (8 bytes)
# Then bytes 8-9 seem to be some kind of block marker
# Let's analyze the structure more carefully

print("=== HEADER ANALYSIS ===")
print(f"Magic: {data[:4].hex()} = {repr(data[:4])}")  # 00494150 = NUL IAP
print(f"Version: {struct.unpack_from('<I', data, 4)[0]}")  # = 2

# After 8-byte header, look for block/section markers
# Check bytes 8-10 more carefully
print(f"\nBytes 8-16 (raw):     {data[8:16].hex()}")
print(f"Bytes 8-16 (decrypted): {decrypted[8:16].hex()}")
# 0805 looks like it could be a section type identifier  
# After decryption it becomes 6280

# Let's look for structure in the first 256 bytes of decrypted data
print(f"\nFirst 256 bytes of decrypted data (hex):")
for i in range(0, 256, 16):
    hex_str = ' '.join(f'{b:02x}' for b in decrypted[i:i+16])
    ascii_str = ''.join(chr(b) if 32 <= b <= 126 else '.' for b in decrypted[i:i+16])
    print(f"  {i:04x}: {hex_str}  {ascii_str}")

# Look for null-terminated strings in the decrypted data
print("\n\n=== NULL-TERMINATED STRINGS ===")
i = 8
strings_by_offset = {}
while i < len(decrypted):
    # Find next null byte
    end = decrypted.find(b'\x00', i)
    if end == -1:
        break
    s = decrypted[i:end]
    if len(s) >= 4 and all(32 <= b <= 126 for b in s):
        strings_by_offset[i] = s.decode('ascii')
        if len(s) >= 6:
            print(f"  @{i:6d} (len={len(s):3d}): {s.decode('ascii')[:100]}")
    i = end + 1

# Also look for the "short string" encoding where strings are preceded by a length byte
print("\n\n=== LENGTH-PREFIXED STRINGS (length byte before string) ===")
for i in range(8, min(len(decrypted)-100, 151000)):
    length = decrypted[i]
    if 4 < length < 60 and i + length + 1 < len(decrypted):
        potential_str = decrypted[i+1:i+1+length]
        if all(32 <= b <= 126 for b in potential_str):
            # Check if it's preceded by something that looks like a field reference
            print(f"  @{i:6d} len={length:2d}: '{potential_str.decode('ascii')}'")

# Search for all occurrences of kEpRxMC field names in the context of the VM structure
print("\n\n=== FIELD NAME CONTEXT (larger window) ===")
field_names = ["YVdiJ", "gEdg", "pQJMoMSoOUTIIG"]
for field in field_names:
    field_bytes = field.encode('utf-8')
    idx = decrypted.find(field_bytes)
    if idx >= 0:
        # Show 80 bytes before and after
        start = max(0, idx - 80)
        end = min(len(decrypted), idx + len(field_bytes) + 80)
        context = decrypted[start:end]
        print(f"\n  Field '{field}' at offset {idx}:")
        for j in range(0, len(context), 16):
            offset = start + j
            chunk = context[j:j+16]
            hex_str = ' '.join(f'{b:02x}' for b in chunk)
            ascii_str = ''.join(chr(b) if 32 <= b <= 126 else '.' for b in chunk)
            print(f"    {offset:06x}: {hex_str}  {ascii_str}")
