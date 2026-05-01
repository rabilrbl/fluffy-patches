import struct

XOR_KEY = bytes.fromhex("6a852fbd5ac3371c2c6848d6befb97a1")

def xor_decrypt(data, key):
    key_len = len(key)
    return bytes(b ^ key[i % key_len] for i, b in enumerate(data))

# The kEpRxMC field names
field_names = [
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
]

print(f"Total kEpRxMC fields: {len(field_names)}")

# Search ALL 22 VM bytecode files for these field names
assets_dir = "/tmp/pairip-dump/vm-bytecode/assets"
import os

for fname in sorted(os.listdir(assets_dir)):
    fpath = os.path.join(assets_dir, fname)
    with open(fpath, 'rb') as f:
        data = f.read()
    
    # Try both raw and decrypted
    for mode, search_data in [("raw", data), ("decrypted", data[:8] + xor_decrypt(data[8:], XOR_KEY))]:
        matches = []
        for field in field_names:
            field_bytes = field.encode('utf-8')
            idx = search_data.find(field_bytes)
            if idx >= 0:
                matches.append((field, idx))
        if matches:
            print(f"\n{fname} ({mode}): Found {len(matches)} field name matches:")
            for field, idx in matches:
                context = search_data[max(0,idx-10):idx+len(field)+20]
                ctx_str = ''.join(chr(b) if 32 <= b <= 126 else '.' for b in context)
                print(f"  '{field}' at offset {idx}: ...{ctx_str}...")

# Also check for "kEpRxMC" itself
for fname in sorted(os.listdir(assets_dir)):
    fpath = os.path.join(assets_dir, fname)
    with open(fpath, 'rb') as f:
        data = f.read()
    
    for mode, search_data in [("raw", data), ("decrypted", data[:8] + xor_decrypt(data[8:], XOR_KEY))]:
        for pattern in [b'kEpRxMC', b'Rso/', b'org/apache/commons']:
            idx = search_data.find(pattern)
            if idx >= 0:
                context = search_data[max(0,idx-20):idx+40]
                ctx_str = ''.join(chr(b) if 32 <= b <= 126 else '.' for b in context)
                print(f"\n{fname} ({mode}): '{pattern.decode()}' at offset {idx}: ...{ctx_str}...")
