import json
import os
from io import BytesIO

import requests
from PIL import Image, ImageDraw, ImageFont

JSON_PATH = "/home/user-x/Documents/GitHub/projet-java/genialtcg/assets/JSON/pays.json"
SHAPES_CACHE_DIR = "tools/img/shapes_cache"
with open(JSON_PATH, "r", encoding="utf-8") as file:
    pays = json.load(file)

SCALE = 2

CARD_WIDTH = 320 * SCALE
CARD_HEIGHT = 448 * SCALE
PADDING = 20 * SCALE

COLUMN_COUNT = 10

TEXT_COLOR = (0, 0, 0)
HEADER_COLOR = (0, 0, 0)

TEMPLATES = {
    "Économique": "tools/img/template/economique.png",
    "Renseignement": "tools/img/template/renseignement.png",
    "Isolationniste": "tools/img/template/isolationniste.png",
    "Militaire": "tools/img/template/militaire.png",
    "Diplomatique": "tools/img/template/diplomatique.png",
}

if not os.path.exists(SHAPES_CACHE_DIR):
    os.makedirs(SHAPES_CACHE_DIR)
templates_cache = {}

for k, path in TEMPLATES.items():
    templates_cache[k] = Image.open(path)

font_bold_big = ImageFont.truetype(
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 24 * SCALE
)
font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 18 * SCALE)
font_bold = ImageFont.truetype(
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 18 * SCALE
)
font_italic = ImageFont.truetype(
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Oblique.ttf", 18 * SCALE
)
font_bold_italic = ImageFont.truetype(
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-BoldOblique.ttf", 18 * SCALE
)
font_small = ImageFont.truetype(
    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 12 * SCALE
)
font_small_small_italic = ImageFont.truetype(
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Oblique.ttf", 11 * SCALE
)
font_bold_small = ImageFont.truetype(
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 12 * SCALE
)
font_italic_small = ImageFont.truetype(
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Oblique.ttf", 18 * SCALE
)
font_bold_italic_small = ImageFont.truetype(
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-BoldOblique.ttf", 12 * SCALE
)


def get_country_shape(iso_code):
    """Récupère la silhouette PNG (noir/transparent) depuis MapsIcon ou le cache local."""
    if not iso_code:
        return None

    iso_code = iso_code.lower()
    local_file = os.path.join(SHAPES_CACHE_DIR, f"{iso_code}.png")

    # 1. Vérifier le cache local
    if os.path.exists(local_file):
        return Image.open(local_file).convert("RGBA")

    # 2. Sinon, télécharger depuis GitHub (MapsIcon)
    url = f"https://raw.githubusercontent.com/djaiss/mapsicon/master/all/{iso_code}/256.png"
    try:
        response = requests.get(url, timeout=5)
        if response.status_code == 200:
            with open(local_file, "wb") as f:
                f.write(response.content)
            return Image.open(BytesIO(response.content)).convert("RGBA")
    except Exception as e:
        print(f"Erreur téléchargement pour {iso_code}: {e}")

    return None


row_count = (len(pays) + COLUMN_COUNT - 1) // COLUMN_COUNT
img_width = COLUMN_COUNT * CARD_WIDTH + 2 * PADDING
img_height = row_count * CARD_HEIGHT + 2 * PADDING

img = Image.new("RGB", (img_width, img_height))
draw = ImageDraw.Draw(img)


def wrap_text(draw, text, font, max_width):
    words = text.split()
    lines = []
    current = ""

    for word in words:
        test = current + " " + word if current else word
        w = draw.textbbox((0, 0), test, font=font)[2]

        if w <= max_width:
            current = test
        else:
            lines.append(current)
            current = word

    if current:
        lines.append(current)

    return lines


for idx, p in enumerate(pays):
    col_index = idx % COLUMN_COUNT
    row_index = idx // COLUMN_COUNT

    current_x = PADDING + col_index * CARD_WIDTH
    current_y = PADDING + row_index * CARD_HEIGHT

    type_pays = p.get("type")
    template_img = templates_cache[type_pays].resize(
        (CARD_WIDTH, CARD_HEIGHT), Image.Resampling.LANCZOS
    )
    img.paste(template_img, (current_x, current_y))

    iso_code = p.get("id")
    shape_img = get_country_shape(iso_code)

    if shape_img:
        shape_max_size = 140 * SCALE
        shape_img.thumbnail((shape_max_size, shape_max_size), Image.Resampling.LANCZOS)

        # Centrage horizontal et positionnement vertical au milieu
        s_w, s_h = shape_img.size
        shape_x = current_x + (CARD_WIDTH - s_w) // 2
        shape_y = current_y + 110 * SCALE  # Positionnée dans la zone vide centrale

        # Collage avec le canal Alpha (transparence)
        img.paste(shape_img, (shape_x, shape_y), shape_img)

    # TEXT
    padding_inside = 10 * SCALE
    x_text = current_x + padding_inside
    y_text = current_y + padding_inside

    draw.text(
        (current_x + 250 * SCALE, y_text),
        str(p["etat"]),
        fill=HEADER_COLOR,
        font=font_bold_big,
    )

    draw.text(
        (current_x + 10 * SCALE, y_text + 10),
        p["nom"],
        fill=HEADER_COLOR,
        font=font_bold,
    )

    y_text += 45 * SCALE

    draw.text((x_text, y_text), str(p["rang"]), fill=TEXT_COLOR, font=font_italic)

    y_text += 228 * SCALE

    stats = p["statistiques"]

    draw.text(
        (x_text + 120 * SCALE, y_text),
        str(stats["puissance"]),
        fill=TEXT_COLOR,
        font=font_small,
    )

    draw.text(
        (x_text + 255 * SCALE, y_text),
        str(stats["ressources"]),
        fill=TEXT_COLOR,
        font=font_small,
    )

    y_text += 28 * SCALE

    draw.text(
        (x_text + 120 * SCALE, y_text),
        str(stats["technologie"]),
        fill=TEXT_COLOR,
        font=font_small,
    )

    draw.text(
        (x_text + 255 * SCALE, y_text),
        str(stats["stabilite"]),
        fill=TEXT_COLOR,
        font=font_small,
    )

    y_text += 28 * SCALE

    draw.text(
        (x_text + 190 * SCALE, y_text),
        str(stats["economie"]),
        fill=TEXT_COLOR,
        font=font_small,
    )

    y_text += 23 * SCALE

    special = p["special"]

    draw.text(
        (x_text + 30 * SCALE, y_text),
        special["nom"],
        fill=TEXT_COLOR,
        font=font_bold_italic_small,
    )

    draw.text(
        (x_text + 247 * SCALE, y_text),
        str(special["cout"]),
        fill=TEXT_COLOR,
        font=font_bold_small,
    )

    y_text += 15 * SCALE

    desc_lines = wrap_text(
        draw,
        special["description"],
        font_small,
        (CARD_WIDTH - 2 * padding_inside) - 76,
    )

    for line in desc_lines:
        draw.text(
            (x_text + 30 * SCALE, y_text),
            line,
            fill=TEXT_COLOR,
            font=font_small_small_italic,
        )
        y_text += 16 * SCALE

    y_text += 9 * SCALE

    draw.text(
        (x_text + 37 * SCALE, y_text),
        str(p["cout"]),
        fill=TEXT_COLOR,
        font=font_small,
    )

    draw.text(
        (x_text + 56 * SCALE, y_text + 12 * SCALE),
        str(p["cout"] // 2),
        fill=TEXT_COLOR,
        font=font_small,
    )

    lore_lines = wrap_text(
        draw, p["lore"], font_small, (CARD_WIDTH - 2 * padding_inside) - 153
    )

    for line in lore_lines:
        draw.text(
            (x_text + 95 * SCALE, y_text),
            line,
            fill=TEXT_COLOR,
            font=font_small_small_italic,
        )
        y_text += 11 * SCALE


img.save("genialtcg/assets/cards/atlas_pays.png", dpi=(300, 300))

print("atlas généré")
