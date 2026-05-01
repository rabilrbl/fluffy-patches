import struct, os

XOR_KEY = bytes.fromhex("6a852fbd5ac3371c2c6848d6befb97a1")

def xor_decrypt(data, key):
    key_len = len(key)
    return bytes(b ^ key[i % key_len] for i, b in enumerate(data))

fpath = "/tmp/pairip-dump/vm-bytecode/assets/fS2NPonHRPamHuOE"
with open(fpath, 'rb') as f:
    data = f.read()

decrypted = data[:8] + xor_decrypt(data[8:], XOR_KEY)

# Field names found: gEdg @ 30090, YVdiJ @ 48826, pQJMoMSoOUTIIG @ 103194
# All preceded by 0x00 byte. Let's look at all occurrences of 0x00 followed by ASCII strings (4+ chars)

print("=== 0x00-prefixed strings (potential field references) ===")
field_names = set([
    "AdeZcWTSZwWrYxd", "ApHXiDnrvirY", "CLjObgw", "EVmBSZGEDSoIw", "Epf",
    "EubSZZYBfnRql", "FGdYpdxF", "GFKxAYmlluM", "GIo", "JdFCpvjTrNIXApx",
    "LXFP", "LvVgVLJFuFF", "OkjiTV", "Omplm", "RSvaCsz", "RXffObjzi",
    "UeMSQBr", "Uts", "VuAmicHyJVUhXy", "YVdiJ", "YzkPCeZOIo",
    "ZRPAl", "aKPJrxNCR", "dZml", "dvytFeRnr", "eWkfCJq", "gEdg",
    "gGhZtVN", "irZoovQ", "jeddv", "jktPZJWXNG", "lLgqyuOjQPVFhoU",
    "mJnmqQCTGwWbJgQ", "mbEaYftZg", "nZMOBr", "pQJMoMSoOUTIIG",
    "ppmhkinKiIeecmq", "rGEUEJREauKidc", "tijqIH", "ttWhLxpgFPip",
    "uryINO", "vNbvgZz", "wzYgVoOQMiBZCTG", "xDAMRXSa",
    "xoSdzXezvF", "xyjSfoP", "ypmscfT", "yrfcbcLrPQFd"
])

# Scan for null-terminated strings preceded by 0x00
i = 8
found_strings = []
while i < len(decrypted) - 1:
    if decrypted[i] == 0x00 and 65 <= decrypted[i+1] <= 122:  # null followed by letter
        # Find end of this string
        end = i + 1
        while end < len(decrypted) and ((65 <= decrypted[end] <= 90) or (97 <= decrypted[end] <= 122) or decrypted[end] in (ord('_'), ord('$'))):
            end += 1
        s = decrypted[i+1:end].decode('ascii', errors='replace')
        if len(s) >= 2:
            is_field = s in field_names
            found_strings.append((i, s, is_field))
    i += 1

print(f"Found {len(found_strings)} null-prefixed identifier-like strings")
for offset, s, is_field in found_strings:
    marker = " *** FIELD ***" if is_field else ""
    print(f"  @{offset:6d}: '{s}'{marker}")

# Now let's also look specifically at the structure around field name references
# We saw that field names are preceded by 0x00 and some bytes before that
# Let's check what precedes them more carefully
print("\n\n=== Structure around known field references ===")
for field in ["gEdg", "YVdiJ", "pQJMoMSoOUTIIG"]:
    field_bytes = field.encode('utf-8')
    idx = decrypted.find(field_bytes)
    if idx > 0:
        # The 0x00 byte seems to be a string delimiter
        # Let's see what's ~20 bytes before each field name
        start = max(0, idx - 30)
        context = decrypted[start:idx+len(field_bytes)+30]
        print(f"\n  Field '{field}' at offset {idx}:")
        for j in range(0, len(context), 16):
            off = start + j
            chunk = context[j:j+16]
            hex_str = ' '.join(f'{b:02x}' for b in chunk)
            ascii_str = ''.join(chr(b) if 32 <= b <= 126 else '.' for b in chunk)
            print(f"    {off:06x}: {hex_str}  {ascii_str}")

