"""Builds the offline country-boundary file used for on-device "am I in Canada?" checks.

Source: Natural Earth admin-0 countries (public domain), 1:10m for Canada and its neighbours (precise borders),
1:50m for everything else (only used to name the country you're in).

Output format (little-endian), read by core/.../geo/BorderIndex.kt:
    magic  b"EHB1"
    u16    country count
    per country:
        2 bytes  ISO 3166-1 alpha-2 code (ASCII)
        u16      ring count
        per ring:
            i32 x4   bounding box (minLat, minLon, maxLat, maxLon) in 1e-5 degrees
            u32      point count
            points   first point as two i32 (lat, lon), then zigzag-varint deltas (lat, lon), 1e-5 degrees

Usage: python -I tools/geodata/build_borders.py [output-path]
"""

import json
import os
import struct
import sys
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
CACHE = os.path.join(HERE, ".cache")
REPO = os.path.dirname(os.path.dirname(HERE))
DEFAULT_OUT = os.path.join(REPO, "composeApp", "src", "commonMain", "composeResources", "files", "borders.bin")

BASE = "https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/"
SOURCES = {
    "10m": BASE + "ne_10m_admin_0_countries.geojson",
    "50m": BASE + "ne_50m_admin_0_countries.geojson",
}
# Canada and every country it borders (by land or very close water) use the detailed 1:10m data.
DETAILED = {"CA", "US", "GL", "PM"}
SCALE = 100_000  # 1e-5 degrees


def download(name):
    os.makedirs(CACHE, exist_ok=True)
    path = os.path.join(CACHE, f"ne_{name}_admin_0_countries.geojson")
    if not os.path.exists(path):
        print(f"Downloading {SOURCES[name]}")
        urllib.request.urlretrieve(SOURCES[name], path)
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def iso_code(props):
    for key in ("ISO_A2_EH", "ISO_A2", "iso_a2"):
        code = props.get(key)
        if code and code not in ("-99", "-1") and len(code) == 2:
            return code.upper()
    return None


def tolerance(code, lat):
    """Simplification tolerance in degrees. Fine where Canada meets the US, coarse elsewhere."""
    if code in DETAILED:
        if lat < 49.5:
            return 0.002
        if lat < 60.0:
            return 0.004
        return 0.01
    return 0.02


def simplify(points, code):
    """Iterative Douglas-Peucker with a latitude-dependent tolerance. points: list of (lat, lon)."""
    n = len(points)
    if n <= 4:
        return points
    keep = [False] * n
    keep[0] = keep[-1] = True
    stack = [(0, n - 1)]
    while stack:
        start, end = stack.pop()
        if end <= start + 1:
            continue
        (y1, x1), (y2, x2) = points[start], points[end]
        dx, dy = x2 - x1, y2 - y1
        seg_len_sq = dx * dx + dy * dy
        best_dist, best_index = -1.0, -1
        for i in range(start + 1, end):
            y, x = points[i]
            if seg_len_sq == 0:
                dist = ((x - x1) ** 2 + (y - y1) ** 2) ** 0.5
            else:
                t = max(0.0, min(1.0, ((x - x1) * dx + (y - y1) * dy) / seg_len_sq))
                px, py = x1 + t * dx, y1 + t * dy
                dist = ((x - px) ** 2 + (y - py) ** 2) ** 0.5
            if dist > best_dist:
                best_dist, best_index = dist, i
        if best_dist > tolerance(code, points[best_index][0]):
            keep[best_index] = True
            stack.append((start, best_index))
            stack.append((best_index, end))
    # Rings that collapse below 4 points are islets smaller than the tolerance; the caller drops them.
    return [p for p, k in zip(points, keep) if k]


def rings_of(geometry):
    if geometry["type"] == "Polygon":
        polygons = [geometry["coordinates"]]
    elif geometry["type"] == "MultiPolygon":
        polygons = geometry["coordinates"]
    else:
        return []
    rings = []
    for polygon in polygons:
        for ring in polygon:
            rings.append([(lat, lon) for lon, lat in ring])
    return rings


def zigzag(value):
    return (value << 1) ^ (value >> 63)


def varint(value):
    out = bytearray()
    while True:
        byte = value & 0x7F
        value >>= 7
        if value:
            out.append(byte | 0x80)
        else:
            out.append(byte)
            return bytes(out)


def encode_ring(points):
    q = [(round(lat * SCALE), round(lon * SCALE)) for lat, lon in points]
    lats = [p[0] for p in q]
    lons = [p[1] for p in q]
    out = bytearray(struct.pack("<iiii", min(lats), min(lons), max(lats), max(lons)))
    out += struct.pack("<I", len(q))
    out += struct.pack("<ii", *q[0])
    prev_lat, prev_lon = q[0]
    for lat, lon in q[1:]:
        out += varint(zigzag(lat - prev_lat)) + varint(zigzag(lon - prev_lon))
        prev_lat, prev_lon = lat, lon
    return bytes(out)


def main():
    out_path = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_OUT
    countries = {}
    for scale in ("10m", "50m"):
        for feature in download(scale)["features"]:
            code = iso_code(feature["properties"])
            if code is None:
                continue
            detailed = code in DETAILED
            if (scale == "10m") != detailed or code in countries:
                continue
            countries[code] = rings_of(feature["geometry"])

    blob = bytearray(b"EHB1")
    blob += struct.pack("<H", len(countries))
    total_points = 0
    for code in sorted(countries):
        rings = [simplify(ring, code) for ring in countries[code]]
        rings = [ring for ring in rings if len(ring) >= 4]
        blob += code.encode("ascii") + struct.pack("<H", len(rings))
        for ring in rings:
            blob += encode_ring(ring)
            total_points += len(ring)
        if code in DETAILED:
            print(f"{code}: {len(rings)} rings, {sum(len(r) for r in rings)} points")

    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    with open(out_path, "wb") as f:
        f.write(blob)
    print(f"{len(countries)} countries, {total_points} points, {len(blob) / 1024:.0f} KiB -> {out_path}")


if __name__ == "__main__":
    main()
