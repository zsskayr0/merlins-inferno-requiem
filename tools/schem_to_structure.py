"""Converts a WorldEdit .schem (Sponge v3) into a vanilla structure .nbt and wires up the chest loot tables.

  python tools/schem_to_structure.py <in.schem> <out.nbt>

Chest -> loot table, decided by geometry so it survives re-exporting an edited schematic:
  * chests on the lowest chest layer (the double chests in the skull chamber under the dome) -> andras_vault
  * the single chest nearest the sword's axis (the tallest basalt column)                    -> andras_sword_chest
  * every other chest (rubble piles around the battlefield)                                  -> andras_ruins
Air is only kept when it sits inside a column's solid span (chambers, gaps), so pasting carves the interior but
leaves the surrounding Nether alone. Spawners are rebuilt with proper NBT types (a .schem read loses short/int).
"""
import gzip
import re
import struct
import sys

NS = "merlins_inferno:chests/"


# ---------------------------------------------------------------- NBT reader (types collapsed - only used for the schem)
def read_schem(path):
    d = gzip.open(path).read()
    p = [0]

    def u(fmt, n):
        v = struct.unpack(fmt, d[p[0]:p[0] + n])[0]
        p[0] += n
        return v

    def rd(t):
        if t == 1: return u('>b', 1)
        if t == 2: return u('>h', 2)
        if t == 3: return u('>i', 4)
        if t == 4: return u('>q', 8)
        if t == 5: p[0] += 4; return 0
        if t == 6: p[0] += 8; return 0
        if t == 7:
            n = u('>i', 4); v = d[p[0]:p[0] + n]; p[0] += n; return v
        if t == 8:
            n = u('>H', 2); v = d[p[0]:p[0] + n].decode(); p[0] += n; return v
        if t == 9:
            et = d[p[0]]; p[0] += 1; n = u('>i', 4)
            return [rd(et) for _ in range(n)]
        if t == 10:
            r = {}
            while True:
                tt = d[p[0]]; p[0] += 1
                if tt == 0: break
                n = u('>H', 2); k = d[p[0]:p[0] + n].decode(); p[0] += n
                r[k] = rd(tt)
            return r
        if t == 11:
            n = u('>i', 4); v = struct.unpack('>%di' % n, d[p[0]:p[0] + 4 * n]); p[0] += 4 * n; return v
        if t == 12:
            n = u('>i', 4); p[0] += 8 * n; return 0
        raise ValueError(t)

    n = struct.unpack('>H', d[1:3])[0]
    p[0] = 3 + n
    root = rd(10)
    return root.get('Schematic', root)


# ---------------------------------------------------------------- NBT writer (explicit types)
class T:
    def __init__(self, t, v):
        self.t, self.v = t, v


def short(v): return T(2, v)
def int_(v): return T(3, v)
def string(v): return T(8, v)
def lst(et, items): return T(9, (et, items))
def comp(d): return T(10, d)
def ints(v): return T(11, list(v))


def w_payload(out, t):
    k, v = t.t, t.v
    if k == 2: out += struct.pack('>h', v)
    elif k == 3: out += struct.pack('>i', v)
    elif k == 8:
        b = v.encode(); out += struct.pack('>H', len(b)) + b
    elif k == 9:
        et, items = v
        out += struct.pack('>bi', et, len(items))
        for i in items: w_payload(out, i)
    elif k == 10:
        for name, val in v.items():
            b = name.encode()
            out += struct.pack('>bH', val.t, len(b)) + b
            w_payload(out, val)
        out += b'\x00'
    elif k == 11:
        out += struct.pack('>i', len(v))
        out += struct.pack('>%di' % len(v), *v)


def write_nbt(path, root):
    out = bytearray(b'\x0a\x00\x00')
    w_payload(out, root)
    with open(path, 'wb') as f:
        f.write(gzip.compress(bytes(out), 9, mtime=0))


# ---------------------------------------------------------------- conversion
def parse_state(s):
    m = re.match(r'([^\[]+)(?:\[(.*)\])?$', s)
    props = dict(kv.split('=') for kv in m.group(2).split(',')) if m.group(2) else {}
    return m.group(1), props


def main(src, dst):
    s = read_schem(src)
    W, H, L = s['Width'], s['Height'], s['Length']
    b = s['Blocks']
    pal = {v: k for k, v in b['Palette'].items()}
    data = b['Data']
    arr, i = [], 0
    while i < len(data):
        v = sh = 0
        while True:
            c = data[i]; i += 1
            v |= (c & 0x7f) << sh; sh += 7
            if not c & 0x80: break
        arr.append(v)

    def name(x, y, z): return pal[arr[(y * L + z) * W + x]]
    def isair(n): return n.split('[')[0] in ('minecraft:air', 'minecraft:cave_air', 'minecraft:void_air')

    chests = {tuple(be['Pos']) for be in b['BlockEntities'] if be['Id'] == 'minecraft:chest'}
    spawners = {tuple(be['Pos']): be['Data'] for be in b['BlockEntities'] if be['Id'] == 'minecraft:mob_spawner'}
    vault_y = min(c[1] for c in chests)
    col = {}
    for y in range(H):
        for z in range(L):
            for x in range(W):
                if name(x, y, z).startswith('minecraft:basalt'):
                    col[(x, z)] = col.get((x, z), 0) + 1
    sx, sz = max(col, key=col.get)
    singles = [c for c in chests if 'type=single' in name(*c)]
    sword_chest = min(singles, key=lambda c: (c[0] - sx) ** 2 + (c[2] - sz) ** 2)

    def loot_for(c):
        if c[1] == vault_y: return 'andras_vault'
        if c == sword_chest: return 'andras_sword_chest'
        return 'andras_ruins'

    palette, index = [], {}

    def pidx(state):
        if state not in index:
            n, props = parse_state(state)
            e = {'Name': string(n)}
            if props: e['Properties'] = comp({k: string(v) for k, v in props.items()})
            index[state] = len(palette)
            palette.append(comp(e))
        return index[state]

    blocks, stats = [], {}
    for z in range(L):
        for x in range(W):
            solid = [y for y in range(H) if not isair(name(x, y, z))]
            if not solid: continue
            for y in range(solid[0], solid[-1] + 1):
                e = {'pos': ints((x, y, z)), 'state': int_(pidx(name(x, y, z)))}
                if (x, y, z) in spawners:
                    sd = spawners[(x, y, z)]
                    ent = sd['SpawnData']['entity']['id']
                    e['nbt'] = comp({
                        'id': string('minecraft:mob_spawner'),
                        'Delay': short(-1), 'MinSpawnDelay': short(sd['MinSpawnDelay']), 'MaxSpawnDelay': short(sd['MaxSpawnDelay']),
                        'SpawnCount': short(sd['SpawnCount']), 'MaxNearbyEntities': short(sd['MaxNearbyEntities']),
                        'RequiredPlayerRange': short(sd['RequiredPlayerRange']), 'SpawnRange': short(sd['SpawnRange']),
                        'SpawnData': comp({'entity': comp({'id': string(ent)})}),
                        'SpawnPotentials': lst(10, [comp({'weight': int_(1), 'data': comp({'entity': comp({'id': string(ent)})})})]),
                    })
                elif (x, y, z) in chests:
                    lt = loot_for((x, y, z))
                    stats[lt] = stats.get(lt, 0) + 1
                    e['nbt'] = comp({'id': string('minecraft:chest'), 'LootTable': string(NS + lt)})
                blocks.append(comp(e))

    root = comp({'DataVersion': int_(s['DataVersion']), 'size': lst(3, [int_(W), int_(H), int_(L)]),
                 'palette': lst(10, palette), 'blocks': lst(10, blocks), 'entities': lst(10, [])})
    write_nbt(dst, root)
    print('size %dx%dx%d, %d blocks, %d palette entries' % (W, H, L, len(blocks), len(palette)))
    print('sword axis', (sx, sz), 'sword chest', sword_chest, 'vault layer y', vault_y)
    print('chest blocks by loot table:', stats)


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
