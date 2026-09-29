#!/usr/bin/env python3
"""Builds data/merlins_inferno/structure/oblivion_hub.nbt - the structure a player arrives at when entering Oblivion.

Source: a classic (MCEdit) `.schematic` of a floating gothic chapel, plus procedural additions:
  * the four wings (the thin spikes around the island) and everything above y = 46 (the pointed spire) are cut away;
  * a portal chamber is carved into the middle: a Void Block frame against the south wall (the portal faces north),
    a doorway in the north wall, mossy stone bricks, leaves (persistent), moss and a few hanging soul lanterns;
  * broken paths float out from the island in all four directions, with small floating islets between them.

The legacy id/data -> modern block state table comes from the game's own data fixer (BlockStateData), dumped one
line per state as `id data name prop=value,prop=value,` (see --legacy-map).

Usage: build_oblivion_hub.py --schematic gothicchapelsky.schematic --legacy-map legacy_map.txt --out oblivion_hub.nbt
The layout constants printed at the end are the ones OblivionDimension.java relies on.
"""
import argparse
import gzip
import math
import random
import struct

# ------------------------------------------------------------------ minimal NBT reader / writer


class R:
    def __init__(self, b):
        self.b, self.i = b, 0

    def u(self, f, n):
        v = struct.unpack_from('>' + f, self.b, self.i)[0]
        self.i += n
        return v

    def s(self):
        n = self.u('H', 2)
        v = self.b[self.i:self.i + n].decode('utf8', 'replace')
        self.i += n
        return v

    def payload(self, t):
        if t == 1: return self.u('b', 1)
        if t == 2: return self.u('h', 2)
        if t == 3: return self.u('i', 4)
        if t == 4: return self.u('q', 8)
        if t == 5: return self.u('f', 4)
        if t == 6: return self.u('d', 8)
        if t == 7:
            n = self.u('i', 4); v = self.b[self.i:self.i + n]; self.i += n; return v
        if t == 8: return self.s()
        if t == 9:
            it = self.u('b', 1); n = self.u('i', 4)
            return [self.payload(it) for _ in range(n)]
        if t == 10:
            d = {}
            while True:
                tt = self.u('b', 1)
                if tt == 0: return d
                k = self.s(); d[k] = self.payload(tt)
        if t == 11:
            n = self.u('i', 4); v = list(struct.unpack_from('>%di' % n, self.b, self.i)); self.i += 4 * n; return v
        if t == 12:
            n = self.u('i', 4); v = list(struct.unpack_from('>%dq' % n, self.b, self.i)); self.i += 8 * n; return v
        raise ValueError(t)


def read_nbt(path):
    r = R(gzip.open(path).read())
    r.u('b', 1); r.s()
    return r.payload(10)


def w_str(out, s):
    b = s.encode('utf8'); out += struct.pack('>H', len(b)) + b


def w_int_list(out, ints):
    out += b'\x03' + struct.pack('>i', len(ints))
    for v in ints: out += struct.pack('>i', v)


def write_structure(path, size, palette, blocks):
    """palette: list of (name, {prop: value}); blocks: list of ((x, y, z), state_index). Positions are int LISTS, as vanilla reads."""
    out = bytearray(b'\x0a\x00\x00')
    out += b'\x03'; w_str(out, 'DataVersion'); out += struct.pack('>i', 3955)
    out += b'\x09'; w_str(out, 'size'); w_int_list(out, list(size))
    out += b'\x09'; w_str(out, 'palette'); out += b'\x0a' + struct.pack('>i', len(palette))
    for name, props in palette:
        out += b'\x08'; w_str(out, 'Name'); w_str(out, name)
        if props:
            out += b'\x0a'; w_str(out, 'Properties')
            for k, v in props.items():
                out += b'\x08'; w_str(out, k); w_str(out, v)
            out += b'\x00'
        out += b'\x00'
    out += b'\x09'; w_str(out, 'blocks'); out += b'\x0a' + struct.pack('>i', len(blocks))
    for pos, state in blocks:
        out += b'\x09'; w_str(out, 'pos'); w_int_list(out, list(pos))
        out += b'\x03'; w_str(out, 'state'); out += struct.pack('>i', state)
        out += b'\x00'
    out += b'\x09'; w_str(out, 'entities'); out += b'\x0a' + struct.pack('>i', 0)
    out += b'\x00'
    with open(path, 'wb') as f:
        f.write(gzip.compress(bytes(out), 9, mtime=0))


# ------------------------------------------------------------------ layout (template-local coordinates, after the shift)

CENTER = (30, 30)          # island centre (x, z)
SHIFT = (11, 16)           # schematic (19, 14) -> (30, 30)
SURFACE_Y = 28             # top layer of the island disc
CHAMBER = (25, 35)         # chamber walls at x/z = 25 and 35; interior 26..34
ROOF_Y = 38
PORTAL_Z = 33              # the frame's plane; the portal faces north
FRAME_X = (28, 32)         # frame columns; the interior is 29..31 (3 wide)
FRAME_Y = (28, 33)         # frame rows; the interior is 29..32 (4 tall)
ARRIVAL = (30, 29, 29)     # where a traveller stands, north of the portal

BODY_RADIUS = 9.6
SPIRE_Y = 47


def S(name, **props):
    return ('minecraft:' + name if ':' not in name else name, tuple(sorted((k, str(v).lower() if isinstance(v, bool) else str(v)) for k, v in props.items())))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--schematic', required=True)
    ap.add_argument('--legacy-map', required=True)
    ap.add_argument('--out', required=True)
    ap.add_argument('--seed', type=int, default=20260928)
    args = ap.parse_args()
    rng = random.Random(args.seed)

    legacy = {}
    for line in open(args.legacy_map, encoding='utf-8'):
        parts = line.rstrip('\n').split(' ')
        if len(parts) < 3: continue
        props = {}
        if len(parts) > 3 and parts[3]:
            for kv in parts[3].split(','):
                if kv:
                    k, v = kv.split('='); props[k] = v
        legacy[(int(parts[0]), int(parts[1]))] = (parts[2], tuple(sorted(props.items())))

    sch = read_nbt(args.schematic)
    W, H, L = sch['Width'], sch['Height'], sch['Length']
    blocks, data = sch['Blocks'], sch['Data']

    g = {}   # (x, y, z) -> state; template-local, already shifted

    # ---- 1. convert, cutting the wings and the spire
    for y in range(H):
        for z in range(L):
            for x in range(W):
                i = (y * L + z) * W + x
                bid = blocks[i] & 0xFF
                if bid == 0: continue
                if y >= SPIRE_Y: continue
                if y >= 24 and (x - 19) ** 2 + (z - 14) ** 2 > BODY_RADIUS ** 2: continue
                # what is left of the chapel's own walls around the chamber is only ragged fragments (the radius cuts
                # through them) and it walls the exits in: above the island keep nothing outside the chamber's footprint
                if y > SURFACE_Y and not (CHAMBER[0] <= x + SHIFT[0] <= CHAMBER[1] and CHAMBER[0] <= z + SHIFT[1] <= CHAMBER[1]): continue
                st = legacy.get((bid, data[i] & 15))
                if st is None or st[0] == 'minecraft:air': continue
                g[(x + SHIFT[0], y, z + SHIFT[1])] = st

    def brick():
        r = rng.random()
        if r < 0.50: return S('stone_bricks')
        if r < 0.72: return S('mossy_stone_bricks')
        if r < 0.90: return S('cracked_stone_bricks')
        if r < 0.95: return S('cobblestone')
        return S('mossy_cobblestone')

    def leaves(persistent=True):
        return S(rng.choice(['oak_leaves', 'oak_leaves', 'azalea_leaves', 'flowering_azalea_leaves']), distance=7, persistent=persistent, waterlogged=False)

    def solid_below(x, y, z):
        return (x, y - 1, z) in g

    # ---- 2. the portal chamber
    x0, x1 = CHAMBER
    for x in range(x0, x1 + 1):
        for z in range(x0, x1 + 1):
            for y in range(SURFACE_Y + 1, ROOF_Y + 1):
                g.pop((x, y, z), None)
            g[(x, SURFACE_Y, z)] = brick()
            g[(x, ROOF_Y, z)] = brick()
            for y in range(SURFACE_Y - 2, SURFACE_Y):   # keep the floor solid under the chamber
                g.setdefault((x, y, z), S('stone'))
            if x in (x0, x1) or z in (x0, x1):
                for y in range(SURFACE_Y + 1, ROOF_Y):
                    g[(x, y, z)] = brick()
    # windows in the side walls
    for xw in (x0, x1):
        for z in range(29, 32):
            for y in range(SURFACE_Y + 3, SURFACE_Y + 8):
                g[(xw, y, z)] = S('gray_stained_glass')
    # north doorway (the way out), 3 wide, 4 tall, with a stair arch
    for x in range(29, 32):
        for y in range(SURFACE_Y + 1, SURFACE_Y + 5):
            g.pop((x, y, x0), None)
    g[(28, SURFACE_Y + 4, x0)] = S('stone_brick_stairs', facing='east', half='top', shape='straight', waterlogged=False)
    g[(32, SURFACE_Y + 4, x0)] = S('stone_brick_stairs', facing='west', half='top', shape='straight', waterlogged=False)
    for x in range(29, 32):
        g[(x, SURFACE_Y + 5, x0)] = S('chiseled_stone_bricks')
    # the chapel's own outer wall (3 thick, up to y = 36) still stands a block north of the door and would wall the
    # exit in: cut a doorway-sized porch through it out to the north path (no rng, so the rest stays as it was)
    for x in range(29, 32):
        for z in range(CENTER[1] - 14, x0):
            for y in range(SURFACE_Y + 1, SURFACE_Y + 5):
                g.pop((x, y, z), None)
            if z >= CENTER[1] - 10:
                g[(x, SURFACE_Y, z)] = S('mossy_stone_bricks') if (x + z) % 3 == 0 else S('stone_bricks')
    # the Void Block frame, in the X-Y plane at z = PORTAL_Z
    for x in range(FRAME_X[0], FRAME_X[1] + 1):
        for y in range(FRAME_Y[0], FRAME_Y[1] + 1):
            if x in FRAME_X or y in FRAME_Y:
                g[(x, y, PORTAL_Z)] = S('merlins_inferno:void_block')
    for x in range(FRAME_X[0] + 1, FRAME_X[1]):
        for y in range(FRAME_Y[0] + 1, FRAME_Y[1]):
            g.pop((x, y, PORTAL_Z), None)
    # corner posts and a raised step before the portal
    for cx_, cz_ in ((x0 + 1, x0 + 1), (x1 - 1, x0 + 1), (x0 + 1, x1 - 1), (x1 - 1, x1 - 1)):
        for y in range(SURFACE_Y + 1, SURFACE_Y + 6):
            g[(cx_, y, cz_)] = S('stone_brick_wall', up=True, north=False, south=False, east=False, west=False, waterlogged=False)
        g[(cx_, SURFACE_Y + 6, cz_)] = S('chiseled_stone_bricks')
    for x in range(FRAME_X[0] - 1, FRAME_X[1] + 2):
        g[(x, SURFACE_Y + 1, PORTAL_Z - 1)] = S('stone_brick_slab', type='bottom', waterlogged=False)
    # lanterns, moss and leaves inside
    for lx, lz in ((27, 27), (33, 27), (27, 33), (33, 33), (30, 30)):
        g[(lx, ROOF_Y - 1, lz)] = S('soul_lantern', hanging=True, waterlogged=False)
    for x in range(x0 + 1, x1):
        for z in range(x0 + 1, x1):
            if (x, SURFACE_Y + 1, z) in g: continue
            r = rng.random()
            if r < 0.14:
                g[(x, SURFACE_Y, z)] = S('moss_block')
                if rng.random() < 0.6:
                    g[(x, SURFACE_Y + 1, z)] = S('moss_carpet')
    for lx, lz in ((x0 + 1, x0 + 2), (x1 - 1, x1 - 2), (x0 + 2, x1 - 1), (x1 - 2, x0 + 1)):
        for dx, dy, dz in ((0, 1, 0), (1, 1, 0), (0, 1, 1), (0, 2, 0)):
            if (lx + dx, SURFACE_Y + dy, lz + dz) not in g and PORTAL_Z != lz + dz:
                g[(lx + dx, SURFACE_Y + dy, lz + dz)] = leaves()

    # ---- 3. the island: moss, mossy stone, bushes
    def chamber_area(x, z):
        return x0 - 1 <= x <= x1 + 1 and x0 - 1 <= z <= x1 + 1

    for (x, y, z), st in list(g.items()):
        if y != SURFACE_Y or chamber_area(x, z): continue
        if st[0] in ('minecraft:grass_block', 'minecraft:dirt', 'minecraft:coarse_dirt', 'minecraft:podzol'):
            r = rng.random()
            if r < 0.30:
                g[(x, y, z)] = S('moss_block')
                if (x, y + 1, z) not in g and rng.random() < 0.5:
                    g[(x, y + 1, z)] = S('moss_carpet')
        elif st[0] == 'minecraft:stone_bricks' and rng.random() < 0.4:
            g[(x, y, z)] = S(rng.choice(['mossy_stone_bricks', 'cracked_stone_bricks']))
    for (x, y, z), st in list(g.items()):
        if st[0] in ('minecraft:stone_bricks',) and rng.random() < 0.25 and not chamber_area(x, z):
            g[(x, y, z)] = S(rng.choice(['mossy_stone_bricks', 'cracked_stone_bricks', 'mossy_cobblestone']))
    rim = [(x, z) for (x, y, z) in g if y == SURFACE_Y and not chamber_area(x, z) and (x - CENTER[0]) ** 2 + (z - CENTER[1]) ** 2 > 5 ** 2 and (x, y + 1, z) not in g]
    rng.shuffle(rim)
    for x, z in rim[:6]:      # bushes
        for dx, dy, dz in ((0, 1, 0), (1, 1, 0), (-1, 1, 0), (0, 1, 1), (0, 1, -1), (0, 2, 0)):
            if (x + dx, SURFACE_Y + dy, z + dz) not in g and not chamber_area(x + dx, z + dz):
                g[(x + dx, SURFACE_Y + dy, z + dz)] = leaves()
    for x, z in rim[6:14]:    # loose stones
        g[(x, SURFACE_Y + 1, z)] = S(rng.choice(['mossy_cobblestone', 'stone', 'andesite', 'mossy_stone_bricks']))

    # ---- 4. floating broken paths and islets
    def stalactite(x, y, z, chance):
        if rng.random() < chance:
            for d in range(1, rng.randint(2, 4)):
                if (x, y - d, z) not in g:
                    g[(x, y - d, z)] = S(rng.choice(['stone', 'andesite', 'cobblestone', 'mossy_cobblestone']))

    def inside(x, y, z):
        return 1 <= x <= 2 * CENTER[0] - 1 and 1 <= z <= 2 * CENTER[1] - 1 and y >= 1

    def build_path(dirx, dirz, length, y0, rise, phase):
        px, pz = -dirz, dirx     # perpendicular
        for t in range(length):
            along = 10 + t
            drift = int(round(2.3 * math.sin(t * 0.30 + phase)))
            y = y0 + int(round(t * rise))
            chunk = t // 4
            gap = random.Random(args.seed * 31 + chunk * 7 + dirx * 3 + dirz * 5).random()
            keep = 0.97 if t < 5 else max(0.55, 0.95 - t * 0.022)
            if t >= 7 and gap < 0.22:
                keep = 0.12     # a whole missing stretch
            for w in (-1, 0, 1):
                bx = CENTER[0] + dirx * along + px * (drift + w)
                bz = CENTER[1] + dirz * along + pz * (drift + w)
                if not inside(bx, y, bz) or rng.random() > keep: continue
                if w != 0 and rng.random() < 0.35:
                    g[(bx, y, bz)] = S('stone_brick_slab', type='bottom', waterlogged=False)
                else:
                    g[(bx, y, bz)] = brick()
                    stalactite(bx, y, bz, 0.22)
                r = rng.random()
                if r < 0.10:
                    g.setdefault((bx, y + 1, bz), S('moss_carpet'))
                elif r < 0.15 and w != 0:
                    for hy in range(1, rng.randint(2, 3)):
                        g.setdefault((bx, y + hy, bz), S('mossy_cobblestone_wall', up=True, north=False, south=False, east=False, west=False, waterlogged=False))
                elif r < 0.19:
                    for dx, dy, dz in ((0, 1, 0), (1, 1, 0), (0, 1, 1), (0, 2, 0)):
                        if inside(bx + dx, y + dy, bz + dz):
                            g.setdefault((bx + dx, y + dy, bz + dz), leaves())

    build_path(0, -1, 19, SURFACE_Y, 0.10, 0.0)     # north: the way out of the chamber, the longest and best kept
    build_path(1, 0, 15, SURFACE_Y, -0.10, 1.7)
    build_path(0, 1, 15, SURFACE_Y, 0.12, 3.1)
    build_path(-1, 0, 15, SURFACE_Y, -0.08, 4.6)

    def islet(cx_, cy, cz_, radius):
        for dy in range(0, 4):
            r = radius - dy if dy < 3 else max(0, radius - 3)
            if r < 0: break
            for dx in range(-r, r + 1):
                for dz in range(-r, r + 1):
                    if dx * dx + dz * dz > r * r + 1: continue
                    p = (cx_ + dx, cy - dy, cz_ + dz)
                    if not inside(*p): continue
                    if dy == 0:
                        g[p] = S(rng.choice(['moss_block', 'moss_block', 'grass_block', 'mossy_stone_bricks']))
                    else:
                        g[p] = S(rng.choice(['stone', 'andesite', 'cobblestone', 'mossy_cobblestone', 'dirt']))
        top = cy + 1
        for dx in range(-radius, radius + 1):
            for dz in range(-radius, radius + 1):
                if dx * dx + dz * dz <= radius * radius and inside(cx_ + dx, top, cz_ + dz):
                    r = rng.random()
                    if r < 0.30: g[(cx_ + dx, top, cz_ + dz)] = S('moss_carpet')
                    elif r < 0.36: g[(cx_ + dx, top, cz_ + dz)] = S('mossy_cobblestone')
        if radius >= 2 and rng.random() < 0.8:
            for dx, dy, dz in ((0, 1, 0), (1, 1, 0), (-1, 1, 0), (0, 1, 1), (0, 1, -1), (0, 2, 0), (0, 3, 0)):
                if inside(cx_ + dx, top + dy - 1, cz_ + dz):
                    g[(cx_ + dx, top + dy - 1 + 1, cz_ + dz)] = leaves()

    for k in range(8):
        ang = (k + rng.random() * 0.6) * (math.tau / 8)
        rad = rng.randint(20, 27)
        ix = CENTER[0] + int(round(math.cos(ang) * rad))
        iz = CENTER[1] + int(round(math.sin(ang) * rad))
        islet(ix, rng.randint(SURFACE_Y - 6, SURFACE_Y + 12), iz, rng.randint(1, 3))

    # ---- write
    palette, index, out_blocks = [], {}, []
    max_y = 0
    for (x, y, z), st in sorted(g.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
        if x < 0 or y < 0 or z < 0: continue
        if st not in index:
            index[st] = len(palette)
            palette.append((st[0], dict(st[1])))
        out_blocks.append(((x, y, z), index[st]))
        max_y = max(max_y, y)
    size = (2 * CENTER[0] + 1, max_y + 1, 2 * CENTER[1] + 1)
    write_structure(args.out, size, palette, out_blocks)
    print('blocks', len(out_blocks), 'palette', len(palette), 'size', size)
    print('portal frame x', FRAME_X, 'y', FRAME_Y, 'z', PORTAL_Z, 'interior bottom-left', (FRAME_X[0] + 1, FRAME_Y[0] + 1, PORTAL_Z), 'arrival', ARRIVAL)


if __name__ == '__main__':
    main()
