import struct

XOR_KEY = bytes.fromhex("6a852fbd5ac3371c2c6848d6befb97a1")

def xor_decrypt(data, key):
    key_len = len(key)
    return bytes(b ^ key[i % key_len] for i, b in enumerate(data))

fpath = "/tmp/pairip-dump/vm-bytecode/assets/fS2NPonHRPamHuOE"
with open(fpath, 'rb') as f:
    data = f.read()

decrypted = data[:8] + xor_decrypt(data[8:], XOR_KEY)

# Known field name offsets: gEdg@30090, YVdiJ@48826, pQJMoMSoOUTIIG@103194
# Each is preceded by 0x00 byte
# Let's see what comes BEFORE and AFTER these strings more carefully

print("=== Analyzing context around each known field name ===")
for field_name, offset in [("gEdg", 30090), ("YVdiJ", 48826), ("pQJMoMSoOUTIIG", 103194)]:
    print(f"\n--- {field_name} at offset {offset} ---")
    
    # The 0x00 byte is at offset-1. Let's check bytes before that.
    # Show 40 bytes before and 40 bytes after
    start = max(0, offset - 40)
    end = min(len(decrypted), offset + len(field_name) + 40)
    
    for i in range(start, end, 16):
        chunk = decrypted[i:i+16]
        hex_str = ' '.join(f'{b:02x}' for b in chunk)
        ascii_str = ''.join(chr(b) if 32 <= b <= 126 else '.' for b in chunk)
        marker = " <-- FIELD START" if i <= offset < i+16 else ""
        print(f"  {i:06x}: {hex_str}  {ascii_str}{marker}")

# Now let's look at ALL 0x00-prefixed strings and see if they correspond to ANY 
# known identifier patterns. Also check for package/class paths
print("\n\n=== Looking for class path patterns ===")
# Look for strings like "org/apache/commons/net/ftp/Rso/kEpRxMC" 
# or patterns like "Lorg/apache/commons/net/ftp/Rso/kEpRxMC;"
for pattern in [b'org/apache', b'kEpRxMC', b'Rso/kEp', b'Ljava/lang/String']:
    idx = 0
    while True:
        idx = decrypted.find(pattern, idx)
        if idx == -1:
            break
        ctx = decrypted[max(0,idx-20):idx+40]
        ctx_str = ''.join(chr(b) if 32 <= b <= 126 else '.' for b in ctx)
        print(f"  Pattern '{pattern.decode()}' at {idx}: ...{ctx_str}...")
        idx += 1

# Now let's look at ALL VM bytecode files to understand their purpose
# Each one has a different header after the IAP header - could be a checksum/hash
print("\n\n=== VM File Header Comparison ===")
import os
assets_dir = "/tmp/pairip-dump/vm-bytecode/assets"
for fname in sorted(os.listdir(assets_dir)):
    fpath = os.path.join(assets_dir, fname)
    with open(fpath, 'rb') as f:
        fdata = f.read()
    
    fdec = fdata[:8] + xor_decrypt(fdata[8:], XOR_KEY)
    
    # Bytes 8-10 could be a section type or size
    # Let's also find what long strings are in each file
    strings = []
    i = 8
    while i < len(fdec) - 1:
        if fdec[i] == 0x00 and i + 1 < len(fdec):
            end = i + 1
            while end < len(fdec) and ((65 <= fdec[end] <= 90) or (97 <= fdec[end] <= 122)):
                end += 1
            s = fdec[i+1:end].decode('ascii', errors='replace')
            if len(s) >= 5:
                strings.append((i, s))
        i += 1
    
    # Find interesting strings (not random-looking)
    interesting = [s for _, s in strings if not all(c in 'ABCDEFGHIJKLMNOPQRSTUVWXYZ' for c in s) and len(s) >= 6]
    # Pick the most recognizable ones
    recognizable = [s for s in interesting if any(w in s.lower() for w in ['java', 'com/', 'org/', 'android', 'http', 'string', 'class', 'get', 'set', 'init', 'method', 'field'])]
    
    print(f"\n{fname} ({len(fdata)} bytes):")
    print(f"  Header after IAP: {fdata[8:16].hex()} / decrypted: {fdec[8:16].hex()}")
    print(f"  String count (>=5 chars): {len(strings)}")
    if recognizable:
        print(f"  Recognizable strings: {recognizable[:10]}")
    
    # Check for long meaningful strings
    long_strings = [(off, s) for off, s in strings if len(s) >= 8]
    if long_strings:
        for off, s in long_strings[:5]:
            print(f"    @{off}: '{s}'")

