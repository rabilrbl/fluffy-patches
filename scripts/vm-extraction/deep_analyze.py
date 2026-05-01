import struct

XOR_KEY = bytes.fromhex("6a852fbd5ac3371c2c6848d6befb97a1")

def xor_decrypt(data, key):
    key_len = len(key)
    return bytes(b ^ key[i % key_len] for i, b in enumerate(data))

# Analyze the big file more carefully
fpath = "/tmp/pairip-dump/vm-bytecode/assets/fS2NPonHRPamHuOE"
with open(fpath, 'rb') as f:
    data = f.read()

header = data[:8]
body = data[8:]
decrypted = header + xor_decrypt(body, XOR_KEY)

# Find all long readable strings in decrypted data
strings_found = []
current = []
current_start = 0
for i, b in enumerate(decrypted[8:]):
    if 32 <= b <= 126:
        if not current:
            current_start = i + 8
        current.append(chr(b))
    else:
        if len(current) >= 8:
            strings_found.append((current_start, len(current), ''.join(current)))
        current = []

if len(current) >= 8:
    strings_found.append((current_start, len(current), ''.join(current)))

print(f"Total strings >= 8 chars: {len(strings_found)}")
for offset, length, s in strings_found:
    print(f"  @{offset:6d} (len={length:3d}): {s[:120]}")

# Look for patterns around "org" "kEpRxMC" "Rso" etc.
for pattern in [b'org/', b'kEp', b'Rso', b'ftp', b'pairip', b'execute', b'set', b'get', b'init', b'clinit', b'field', b'class', b'method', b'invoke', b'static', b'Ljava', b'String', b'Ljava/lang/String', b'put', b'label']:
    for i in range(len(decrypted) - len(pattern)):
        if decrypted[i:i+len(pattern)] == pattern:
            context = decrypted[max(0,i-10):i+60]
            # Only printable context
            ctx_str = ''.join(chr(b) if 32 <= b <= 126 else '.' for b in context)
            print(f"\nPattern '{pattern.decode('ascii', errors='replace')}' at offset {i}: ...{ctx_str}...")
            break  # just show first occurrence
