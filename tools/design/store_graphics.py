"""Builds Google Play store graphics from the brand mark and raw emulator screenshots.

Usage (needs Pillow: `pip install pillow`):
    python tools/design/store_graphics.py icon
    python tools/design/store_graphics.py feature
    python tools/design/store_graphics.py frame <raw.png> <out.png> "<caption>"

The leaf geometry matches core/.../brand/LeafShape.kt and the launcher icon vector.
Font: Roboto (SIL Open Font License, tools/design/fonts/OFL.txt).
"""
import math
import os
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(HERE))
PLAY = os.path.join(REPO, "androidApp", "src", "main", "play", "listings")
FONT = os.path.join(HERE, "fonts", "Roboto.ttf")

MAPLE = (214, 40, 40)
PAPER = (255, 248, 245)
INK = (35, 25, 23)
LEAF = [(50, 4), (57, 18), (65, 14), (62, 36), (80, 22), (84, 32), (96, 30), (88, 48), (94, 54), (70, 70), (73, 80),
        (53, 77), (53, 97), (47, 97), (47, 77), (27, 80), (30, 70), (6, 54), (12, 48), (4, 30), (16, 32), (20, 22),
        (38, 36), (35, 14), (43, 18)]


def font(size, weight="Bold"):
    f = ImageFont.truetype(FONT, size)
    f.set_variation_by_name(weight)
    return f


def draw_mark(img, cx, cy, size, color=(255, 255, 255)):
    """The ring + tilted rounded leaf, `size` = ring diameter in pixels (supersampled by the caller)."""
    d = ImageDraw.Draw(img)
    r = size / 2
    width = max(2, int(size * 0.069))
    box = [cx - r, cy - r, cx + r, cy + r]
    d.arc(box, start=-50, end=210, fill=color, width=width)
    # Round caps on the ring.
    for angle in (-50, 210):
        a = math.radians(angle)
        x, y = cx + (r - width / 2) * math.cos(a), cy + (r - width / 2) * math.sin(a)
        d.ellipse([x - width / 2, y - width / 2, x + width / 2, y + width / 2], fill=color)
    k = size / 58 * 0.40  # Leaf scale relative to the 108-unit icon, where the ring diameter is 58.
    tilt = math.radians(-12)
    points = []
    for x, y in LEAF:
        dx, dy = (x - 50) * k, (y - 50.5) * k
        points.append((cx + dx * math.cos(tilt) - dy * math.sin(tilt), cy + 1 * k + dx * math.sin(tilt) + dy * math.cos(tilt)))
    d.polygon(points, fill=color)
    join = max(2, int(6 * k))
    d.line(points + [points[0]], fill=color, width=join, joint="curve")
    for x, y in points:
        d.ellipse([x - join / 2, y - join / 2, x + join / 2, y + join / 2], fill=color)


def icon(out=None):
    scale = 4
    img = Image.new("RGB", (512 * scale, 512 * scale), MAPLE)
    draw_mark(img, 256 * scale, 256 * scale, int(512 * 58 / 108) * scale)
    img = img.resize((512, 512), Image.LANCZOS)
    for lang in ("en-US", "fr-CA"):
        path = out or os.path.join(PLAY, lang, "graphics", "icon", "icon.png")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        img.save(path)
    print("icon written")


def feature():
    taglines = {
        "en-US": "Your countdown to Canadian citizenship",
        "fr-CA": "Votre décompte vers la citoyenneté canadienne",
    }
    for lang, tagline in taglines.items():
        scale = 2
        w, h = 1024 * scale, 500 * scale
        img = Image.new("RGB", (w, h), MAPLE)
        draw_mark(img, int(250 * scale), h // 2, int(300 * scale))
        d = ImageDraw.Draw(img)
        d.text((470 * scale, 150 * scale), "Eh-ligible", font=font(104 * scale, "Black"), fill=(255, 255, 255))
        tag_font = font(38 * scale, "Medium")
        lines = wrap(d, tagline, tag_font, 500 * scale)
        y = 290 * scale
        for line in lines:
            d.text((474 * scale, y), line, font=tag_font, fill=(255, 235, 232))
            y += 50 * scale
        img = img.resize((1024, 500), Image.LANCZOS)
        path = os.path.join(PLAY, lang, "graphics", "feature-graphic", "feature.png")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        img.save(path)
    print("feature graphics written")


def wrap(draw, text, fnt, max_width):
    words, lines, line = text.split(), [], ""
    for word in words:
        candidate = f"{line} {word}".strip()
        if draw.textlength(candidate, font=fnt) <= max_width:
            line = candidate
        else:
            lines.append(line)
            line = word
    lines.append(line)
    return lines


def frame(raw_path, out_path, caption):
    """A 1080×1920 store screenshot: caption band on top, the phone screen below with rounded corners."""
    W, H = 1080, 1920
    canvas = Image.new("RGB", (W, H), PAPER)
    d = ImageDraw.Draw(canvas)
    band = 430
    cap_font = font(68, "Bold")
    lines = wrap(d, caption, cap_font, W - 140)
    screen = Image.open(raw_path).convert("RGB")
    target_h = H - band + 260
    target_w = int(screen.width * target_h / screen.height)
    if target_w > W - 120:
        target_w = W - 120
        target_h = int(screen.height * target_w / screen.width)
    screen = screen.resize((target_w, target_h), Image.LANCZOS)
    radius = 44
    mask = Image.new("L", screen.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, screen.width, screen.height], radius=radius, fill=255)
    x = (W - target_w) // 2
    top = band - 200
    shadow = Image.new("L", (W, H), 0)
    ImageDraw.Draw(shadow).rounded_rectangle([x, top + 12, x + target_w, top + target_h + 12], radius=radius, fill=110)
    shadow = shadow.filter(ImageFilter.GaussianBlur(22))
    canvas.paste(Image.new("RGB", (W, H), (60, 20, 20)), (0, 0), shadow)
    d = ImageDraw.Draw(canvas)
    d.rectangle([0, 0, W, band], fill=MAPLE)
    # Caption in the upper part of the band; the phone screen overlaps the band's lower part.
    y = (band - len(lines) * 84) // 2 - 114
    for line in lines:
        width = d.textlength(line, font=cap_font)
        d.text(((W - width) / 2, y), line, font=cap_font, fill=(255, 255, 255))
        y += 84
    canvas.paste(screen, (x, top), mask)
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    canvas.save(out_path)
    print("framed", out_path)


if __name__ == "__main__":
    command = sys.argv[1] if len(sys.argv) > 1 else ""
    if command == "icon":
        icon()
    elif command == "feature":
        feature()
    elif command == "frame":
        frame(sys.argv[2], sys.argv[3], sys.argv[4])
    else:
        print(__doc__)
        sys.exit(1)
