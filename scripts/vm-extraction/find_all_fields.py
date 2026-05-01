import struct, os

XOR_KEY = bytes.fromhex("6a852fbd5ac3371c2c6848d6befb97a1")

def xor_decrypt(data, key):
    key_len = len(key)
    return bytes(b ^ key[i % key_len] for i, b in enumerate(data))

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

# Also look for the package + class reference patterns
search_patterns = [f.encode('utf-8') for f in field_names] + [
    b'kEpRxMC', b'org/apache/commons/net/ftp/Rso',
    b'sput-object', b'iget-object', b'sget-object',
    b'Rso/kEpRxMC', b'Lorg/apache/commons/net/ftp/Rso/kEpRxMC',
    b'Ljava/lang/String;', b'init', b'clinit'
]

assets_dir = "/tmp/pairip-dump/vm-bytecode/assets"
for fname in sorted(os.listdir(assets_dir)):
    fpath = os.path.join(assets_dir, fname)
    with open(fpath, 'rb') as f:
        data = f.read()
    
    decrypted = data[:8] + xor_decrypt(data[8:], XOR_KEY)
    
    found = []
    for pattern in search_patterns:
        idx = 0
        while True:
            idx = decrypted.find(pattern, idx)
            if idx == -1:
                break
            pname = pattern.decode('utf-8', errors='replace')
            # Show context
            start = max(0, idx - 5)
            end = min(len(decrypted), idx + len(pattern) + 20)
            context = decrypted[start:end]
            ctx_hex = ' '.join(f'{b:02x}' for b in context)
            found.append((idx, pname, ctx_hex))
            idx += len(pattern)
    
    if found:
        print(f"\n=== {fname} (size={len(data)}) ===")
        for idx, pname, ctx_hex in sorted(found):
            print(f"  @{idx:6d}: {pname:40s}  hex: {ctx_hex[:80]}")

