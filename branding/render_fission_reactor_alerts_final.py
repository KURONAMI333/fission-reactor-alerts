#!/usr/bin/env python3
"""Render the approved Fission Reactor Alerts icon at distribution sizes.

Owner-selected design (2026-09-27, candidate A of the icon review):
Mekanism fission reactor port (input face, teal glowing ring) rendered
isometrically at vanilla item angle (yaw 225 / pitch 30) over a full-bleed
yellow/black 45-degree hazard-stripe field with a soft centre vignette.

Subject: branding/_ref/subject_port_input.png is an isometric render of
Mekanism Generators' real `fission_reactor_port_input` block texture,
produced from mekanismgenerators-268566-7904061.jar (dev dependency,
Gradle cache) via MineTexture's block_renderer. Third-party asset used as
the visual basis for this addon icon; kept under _ref/ (untracked).
Regenerate it with branding/render_fission_reactor_alerts_subjects.py.

Outputs:
  branding/fission_reactor_alerts_icon_{512,256,128,64}.png  publish masters
  src/main/resources/logo.png                                in-jar logo (256)

SS=4 (2048 -> 512 LANCZOS).
"""
import math
import shutil
from pathlib import Path

from PIL import Image, ImageDraw
from PIL.ImageFilter import GaussianBlur

ROOT = Path(__file__).resolve().parent.parent
BRAND = ROOT / "branding"
SUBJECT = BRAND / "_ref" / "subject_port_input.png"
SIZE, SS = 512, 4
S = SIZE * SS


def diag_hazard(d: ImageDraw.ImageDraw, c2, band: int, angle: float = 45.0):
    """Diagonal stripes covering the canvas (warning tape).
    Dark bands are `band` wide, spaced `2*band` apart so the base shows."""
    ca, sa = math.cos(math.radians(angle)), math.sin(math.radians(angle))
    diag = int(abs(S * ca) + abs(S * sa)) + band * 2
    for i in range(-diag // band - 1, diag // band + 1):
        off = i * band * 2
        cx = S / 2 + off * ca
        cy = S / 2 - off * sa
        d.line(
            [(cx - diag * sa, cy - diag * ca), (cx + diag * sa, cy + diag * ca)],
            fill=c2, width=band,
        )


def place_subject(img: Image.Image, subject_png: Path, frac: float):
    """Paste isometric subject scaled to `frac` of canvas, centred."""
    sub = Image.open(subject_png).convert("RGBA")
    bb = sub.getbbox()
    sub = sub.crop(bb)
    target = int(S * frac)
    scale = target / max(sub.size)
    sub = sub.resize((int(sub.width * scale), int(sub.height * scale)),
                     Image.LANCZOS)
    sh = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    sd = ImageDraw.Draw(sh)
    cx = S / 2
    cy = (S - sub.height) / 2 + sub.height * 0.92
    sd.ellipse([cx - sub.width * 0.40, cy - sub.height * 0.05,
                cx + sub.width * 0.40, cy + sub.height * 0.05],
               fill=(0, 0, 0, 110))
    sh = sh.filter(GaussianBlur(S * 0.012))
    img.alpha_composite(sh)
    img.alpha_composite(sub, (int((S - sub.width) / 2),
                              int((S - sub.height) / 2)))


def final_icon() -> Image.Image:
    if not SUBJECT.exists():
        raise SystemExit(
            f"missing subject {SUBJECT} — regenerate with "
            "branding/render_fission_reactor_alerts_subjects.py"
        )
    img = Image.new("RGBA", (S, S), (243, 167, 26, 255))
    d = ImageDraw.Draw(img)
    diag_hazard(d, (32, 28, 22, 255), band=int(S * 0.0625), angle=45)
    vig = Image.new("L", (S, S), 0)
    vd = ImageDraw.Draw(vig)
    vd.ellipse([S * 0.16, S * 0.14, S * 0.84, S * 0.86], fill=110)
    vig = vig.filter(GaussianBlur(80))
    dark = Image.new("RGBA", (S, S), (24, 18, 12, 105))
    img = Image.composite(dark, img, vig)
    place_subject(img, SUBJECT, frac=0.62)
    return img.resize((SIZE, SIZE), Image.LANCZOS)


if __name__ == "__main__":
    final = final_icon()
    for size in (512, 256, 128, 64):
        out = BRAND / f"fission_reactor_alerts_icon_{size}.png"
        final.resize((size, size), Image.LANCZOS).save(out)
        print("wrote", out)
    logo = ROOT / "src" / "main" / "resources" / "logo.png"
    shutil.copyfile(BRAND / "fission_reactor_alerts_icon_256.png", logo)
    print("wrote", logo)
