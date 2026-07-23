from __future__ import annotations

import math
import shutil
import subprocess
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


# 功能：程序化绘制 7.5 秒的 VSIE 系统一体化流程图动画。
ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "generated_media"
FRAME_DIR = OUT_DIR / "vsie_integrated_systems_frames"
OUTPUT = OUT_DIR / "vsie_integrated_systems_7_5s.mp4"
FONT_PATH = Path(r"C:\Windows\Fonts\msyh.ttc")

W, H = 1920, 1080
FPS = 30
DURATION = 7.5
SCALE = 2


SYSTEMS = [
    ("驾驶控制", "玩家下指令", "#2563eb", (205, 255), (350, 335)),
    ("推进器", "让船移动和转向", "#0f766e", (1460, 190), (1320, 335)),
    ("能源燃料", "给设备供电供油", "#ca8a04", (220, 775), (500, 730)),
    ("武器炮塔", "按目标一起开火", "#dc2626", (1485, 780), (1170, 730)),
    ("护盾识别", "保护自己，分清敌友", "#7c3aed", (450, 120), (500, 465)),
    ("屏幕雷达", "看见状态和目标", "#0891b2", (1330, 485), (1170, 465)),
    ("弹药存储", "把弹药送到武器", "#ea580c", (755, 890), (760, 805)),
    ("数据同步", "让各处状态一致", "#475569", (940, 140), (910, 260)),
]


def ease(x: float) -> float:
    x = max(0.0, min(1.0, x))
    return x * x * (3.0 - 2.0 * x)


def lerp(a: float, b: float, x: float) -> float:
    return a + (b - a) * x


def hex_to_rgb(value: str) -> tuple[int, int, int]:
    value = value.lstrip("#")
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4))


def with_alpha(color: str | tuple[int, int, int], alpha: int) -> tuple[int, int, int, int]:
    rgb = hex_to_rgb(color) if isinstance(color, str) else color
    return (*rgb, max(0, min(255, alpha)))


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(str(FONT_PATH), size=size, index=0)


F_TITLE = font(62)
F_SUBTITLE = font(34)
F_NODE = font(31)
F_SMALL = font(22)
F_TINY = font(18)


def draw_text_center(
    draw: ImageDraw.ImageDraw,
    xy: tuple[float, float],
    text: str,
    fnt: ImageFont.FreeTypeFont,
    fill: tuple[int, int, int, int],
) -> None:
    box = draw.textbbox((0, 0), text, font=fnt)
    draw.text((xy[0] - (box[2] - box[0]) / 2, xy[1] - (box[3] - box[1]) / 2), text, font=fnt, fill=fill)


def draw_arrow(
    draw: ImageDraw.ImageDraw,
    start: tuple[float, float],
    end: tuple[float, float],
    color: tuple[int, int, int, int],
    width: int,
    progress: float = 1.0,
) -> None:
    progress = max(0.0, min(1.0, progress))
    if progress <= 0:
        return
    sx, sy = start
    ex, ey = end
    px, py = sx + (ex - sx) * progress, sy + (ey - sy) * progress
    draw.line((sx, sy, px, py), fill=color, width=width)
    if progress < 0.12:
        return
    angle = math.atan2(py - sy, px - sx)
    head = 18 + width
    left = (px - math.cos(angle - 0.55) * head, py - math.sin(angle - 0.55) * head)
    right = (px - math.cos(angle + 0.55) * head, py - math.sin(angle + 0.55) * head)
    draw.polygon([(px, py), left, right], fill=color)


def draw_node(
    draw: ImageDraw.ImageDraw,
    center: tuple[float, float],
    title: str,
    detail: str,
    color: str,
    alpha: int,
    scale: float = 1.0,
) -> None:
    x, y = center
    rw, rh = 245 * scale, 94 * scale
    fill = with_alpha((255, 255, 255), alpha)
    border = with_alpha(color, alpha)
    shadow = with_alpha((15, 23, 42), int(alpha * 0.08))
    draw.rounded_rectangle((x - rw / 2 + 8, y - rh / 2 + 8, x + rw / 2 + 8, y + rh / 2 + 8), radius=16, fill=shadow)
    draw.rounded_rectangle((x - rw / 2, y - rh / 2, x + rw / 2, y + rh / 2), radius=16, fill=fill, outline=border, width=4)
    draw.ellipse((x - rw / 2 + 20, y - 13, x - rw / 2 + 46, y + 13), fill=with_alpha(color, alpha))
    draw.text((x - rw / 2 + 58, y - 32), title, font=F_NODE, fill=with_alpha((15, 23, 42), alpha))
    draw.text((x - rw / 2 + 58, y + 8), detail, font=F_SMALL, fill=with_alpha((71, 85, 105), alpha))


def draw_grid(draw: ImageDraw.ImageDraw) -> None:
    for x in range(0, W * SCALE, 80 * SCALE):
        draw.line((x, 0, x, H * SCALE), fill=(226, 232, 240, 70), width=1)
    for y in range(0, H * SCALE, 80 * SCALE):
        draw.line((0, y, W * SCALE, y), fill=(226, 232, 240, 70), width=1)


def draw_core(draw: ImageDraw.ImageDraw, alpha: int, t: float) -> None:
    cx, cy = 960 * SCALE, 540 * SCALE
    pulse = 1.0 + 0.03 * math.sin(t * 5.5)
    w, h = 430 * SCALE * pulse, 190 * SCALE * pulse
    draw.rounded_rectangle((cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2), radius=28 * SCALE,
                           fill=with_alpha((255, 255, 255), alpha), outline=with_alpha("#0f172a", alpha), width=5 * SCALE)
    draw_text_center(draw, (cx, cy - 30 * SCALE), "VSIE 整船系统", F_SUBTITLE, with_alpha((15, 23, 42), alpha))
    draw_text_center(draw, (cx, cy + 28 * SCALE), "一起工作", F_NODE, with_alpha("#2563eb", alpha))
    draw.rounded_rectangle((cx - 170 * SCALE, cy + 66 * SCALE, cx + 170 * SCALE, cy + 98 * SCALE),
                           radius=16 * SCALE, fill=with_alpha("#dbeafe", int(alpha * 0.95)))
    draw_text_center(draw, (cx, cy + 82 * SCALE), "驾驶 → 供能 → 推进 / 开火 → 回传状态", F_TINY, with_alpha((30, 64, 175), alpha))


def draw_vehicle_outline(draw: ImageDraw.ImageDraw, alpha: int) -> None:
    cx, cy = 960 * SCALE, 540 * SCALE
    outline = [
        (cx - 510 * SCALE, cy + 10 * SCALE),
        (cx - 330 * SCALE, cy - 190 * SCALE),
        (cx + 270 * SCALE, cy - 210 * SCALE),
        (cx + 520 * SCALE, cy + 15 * SCALE),
        (cx + 265 * SCALE, cy + 225 * SCALE),
        (cx - 325 * SCALE, cy + 210 * SCALE),
    ]
    draw.polygon(outline, fill=with_alpha("#eff6ff", int(alpha * 0.8)), outline=with_alpha("#2563eb", alpha))
    draw.line(outline + [outline[0]], fill=with_alpha("#2563eb", alpha), width=5 * SCALE)
    draw.rounded_rectangle((cx - 230 * SCALE, cy - 70 * SCALE, cx + 230 * SCALE, cy + 80 * SCALE),
                           radius=26 * SCALE, fill=with_alpha((255, 255, 255), int(alpha * 0.96)),
                           outline=with_alpha("#0f172a", alpha), width=4 * SCALE)
    draw_text_center(draw, (cx, cy - 18 * SCALE), "不是零件堆", F_SUBTITLE, with_alpha((15, 23, 42), alpha))
    draw_text_center(draw, (cx, cy + 38 * SCALE), "是一艘完整的载具", F_NODE, with_alpha("#2563eb", alpha))


def make_frame(frame_index: int) -> Image.Image:
    t = frame_index / FPS
    img = Image.new("RGBA", (W * SCALE, H * SCALE), (248, 250, 252, 255))
    draw = ImageDraw.Draw(img)
    draw_grid(draw)

    title_alpha = int(255 * (1.0 - ease((t - 5.7) / 0.8)))
    if title_alpha > 0:
        draw_text_center(draw, (960 * SCALE, 76 * SCALE), "VSIE：把各系统合成一艘船", F_TITLE, with_alpha((15, 23, 42), title_alpha))
        draw_text_center(draw, (960 * SCALE, 135 * SCALE), "驾驶、动力、能源、武器、护盾、雷达互相配合", F_SUBTITLE, with_alpha((71, 85, 105), title_alpha))

    node_move = ease((t - 1.1) / 1.7)
    node_alpha = int(255 * ease(t / 0.7) * (1.0 - 0.55 * ease((t - 5.2) / 1.3)))
    ring_alpha = int(255 * ease((t - 2.25) / 0.8) * (1.0 - 0.45 * ease((t - 5.2) / 1.3)))
    core_alpha = int(255 * ease((t - 2.05) / 0.8) * (1.0 - 0.25 * ease((t - 5.7) / 0.8)))
    final_alpha = int(255 * ease((t - 5.25) / 0.95))

    positions: list[tuple[float, float, str, str, str]] = []
    for title, detail, color, start, end in SYSTEMS:
        px = lerp(start[0], end[0], node_move) * SCALE
        py = lerp(start[1], end[1], node_move) * SCALE
        positions.append((px, py, title, detail, color))

    if t < 2.55:
        bad_alpha = int(190 * (1.0 - ease((t - 1.45) / 0.7)) * ease(t / 0.5))
        if bad_alpha > 0:
            draw_text_center(draw, (960 * SCALE, 990 * SCALE), "不是把零件硬拼在一起", F_SUBTITLE, with_alpha((100, 116, 139), bad_alpha))

    cx, cy = 960 * SCALE, 540 * SCALE
    if ring_alpha > 0:
        draw.ellipse((cx - 475 * SCALE, cy - 320 * SCALE, cx + 475 * SCALE, cy + 320 * SCALE),
                     outline=with_alpha("#93c5fd", int(ring_alpha * 0.7)), width=4 * SCALE)
        draw_text_center(draw, (960 * SCALE, 930 * SCALE), "每个模块都听同一套指令", F_SUBTITLE, with_alpha("#1d4ed8", ring_alpha))

    arrow_progress = ease((t - 2.6) / 1.5)
    flow_phase = (t * 0.7) % 1.0
    for px, py, title, detail, color in positions:
        if arrow_progress > 0:
            draw_arrow(draw, (px, py), (cx, cy), with_alpha(color, int(180 * arrow_progress)), 5 * SCALE, arrow_progress)
            dot_x = lerp(px, cx, flow_phase)
            dot_y = lerp(py, cy, flow_phase)
            draw.ellipse((dot_x - 7 * SCALE, dot_y - 7 * SCALE, dot_x + 7 * SCALE, dot_y + 7 * SCALE),
                         fill=with_alpha(color, int(210 * arrow_progress)))
        draw_node(draw, (px, py), title, detail, color, node_alpha, scale=1.0)

    if core_alpha > 0:
        draw_core(draw, core_alpha, t)

    if t > 4.05:
        good_alpha = int(255 * ease((t - 4.05) / 0.65) * (1.0 - 0.35 * ease((t - 6.2) / 1.0)))
        draw_text_center(draw, (960 * SCALE, 995 * SCALE), "结果：像一艘船一样行动", F_SUBTITLE, with_alpha((15, 23, 42), good_alpha))

    if final_alpha > 0:
        draw_vehicle_outline(draw, final_alpha)

    progress = frame_index / max(1, int(FPS * DURATION) - 1)
    draw.rounded_rectangle((80 * SCALE, 1015 * SCALE, 1840 * SCALE, 1030 * SCALE), radius=8 * SCALE, fill=(226, 232, 240, 255))
    draw.rounded_rectangle((80 * SCALE, 1015 * SCALE, (80 + 1760 * progress) * SCALE, 1030 * SCALE),
                           radius=8 * SCALE, fill=(37, 99, 235, 255))

    return img.resize((W, H), Image.Resampling.LANCZOS).convert("RGB")


def main() -> None:
    OUT_DIR.mkdir(exist_ok=True)
    if FRAME_DIR.exists():
        shutil.rmtree(FRAME_DIR)
    FRAME_DIR.mkdir()

    total_frames = int(FPS * DURATION)
    for frame_index in range(total_frames):
        make_frame(frame_index).save(FRAME_DIR / f"frame_{frame_index:04d}.png", optimize=True)

    cmd = [
        "ffmpeg",
        "-y",
        "-framerate",
        str(FPS),
        "-i",
        str(FRAME_DIR / "frame_%04d.png"),
        "-c:v",
        "libx264",
        "-pix_fmt",
        "yuv420p",
        "-movflags",
        "+faststart",
        "-t",
        str(DURATION),
        str(OUTPUT),
    ]
    subprocess.run(cmd, check=True)
    print(OUTPUT)


if __name__ == "__main__":
    main()
