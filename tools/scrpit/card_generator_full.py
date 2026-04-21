import json
import os
from io import BytesIO

import requests
from PIL import Image, ImageDraw, ImageFont, ImageOps

# --- CONFIGURATION DES CHEMINS ---
JSON_PATH = "/home/user-x/Documents/GitHub/projet-java/genialtcg/assets/JSON/pays.json"
SHAPES_CACHE_DIR = "tools/img/shapes_cache"
FLAGS_CACHE_DIR = "tools/img/flags_cache"
OUTPUT_DIR = "output_cards"  # Dossier pour les cartes individuelles

# --- CONFIGURATION DES CONSTANTES ---
SCALE = 2
CARD_WIDTH = 320 * SCALE
CARD_HEIGHT = 448 * SCALE
TEXT_COLOR = (0, 0, 0)
HEADER_COLOR = (0, 0, 0)

TEMPLATES = {
    "Économique": "tools/img/template/economique.png",
    "Renseignement": "tools/img/template/renseignement.png",
    "Isolationniste": "tools/img/template/isolationniste.png",
    "Militaire": "tools/img/template/militaire.png",
    "Diplomatique": "tools/img/template/diplomatique.png",
}

# --- INITIALISATION ET CACHE ---
if not os.path.exists(OUTPUT_DIR):
    os.makedirs(OUTPUT_DIR)

for folder in [SHAPES_CACHE_DIR, FLAGS_CACHE_DIR]:
    if not os.path.exists(folder):
        os.makedirs(folder)

templates_cache = {}
for k, path in TEMPLATES.items():
    if os.path.exists(path):
        templates_cache[k] = Image.open(path)

# --- POLICES ---
font_path = "/usr/share/fonts/truetype/dejavu/"
font_bold_big = ImageFont.truetype(font_path + "DejaVuSans-Bold.ttf", 24 * SCALE)
font_bold = ImageFont.truetype(font_path + "DejaVuSans-Bold.ttf", 18 * SCALE)
font_italic = ImageFont.truetype(font_path + "DejaVuSans-Oblique.ttf", 18 * SCALE)
font_small = ImageFont.truetype(font_path + "DejaVuSans.ttf", 12 * SCALE)
font_small_small_italic = ImageFont.truetype(
    font_path + "DejaVuSans-Oblique.ttf", 11 * SCALE
)
font_small_small_small_italic = ImageFont.truetype(
    font_path + "DejaVuSans-Oblique.ttf", 10 * SCALE
)
font_bold_small = ImageFont.truetype(font_path + "DejaVuSans-Bold.ttf", 12 * SCALE)
font_bold_italic_small = ImageFont.truetype(
    font_path + "DejaVuSans-BoldOblique.ttf", 12 * SCALE
)


# --- FONCTIONS ---
def get_country_shape(iso_code):
    if not iso_code:
        return None
    iso_code = iso_code.lower()
    local_file = os.path.join(SHAPES_CACHE_DIR, f"{iso_code}.png")
    if os.path.exists(local_file):
        return Image.open(local_file).convert("RGBA")
    url = f"https://raw.githubusercontent.com/djaiss/mapsicon/master/all/{iso_code}/256.png"
    try:
        r = requests.get(url, timeout=5)
        if r.status_code == 200:
            with open(local_file, "wb") as f:
                f.write(r.content)
            return Image.open(BytesIO(r.content)).convert("RGBA")
    except:
        return None
    return None


def get_country_flag(iso_code):
    if not iso_code:
        return None
    iso_code = iso_code.lower()
    local_file = os.path.join(FLAGS_CACHE_DIR, f"{iso_code}.png")
    if os.path.exists(local_file):
        return Image.open(local_file).convert("RGBA")
    url = f"https://flagcdn.com/w640/{iso_code}.png"
    try:
        r = requests.get(url, timeout=5)
        if r.status_code == 200:
            with open(local_file, "wb") as f:
                f.write(r.content)
            return Image.open(BytesIO(r.content)).convert("RGBA")
    except:
        return None
    return None


def wrap_text(draw, text, font, max_width):
    words = text.split()
    lines, current = [], ""
    for word in words:
        test = current + " " + word if current else word
        if draw.textbbox((0, 0), test, font=font)[2] <= max_width:
            current = test
        else:
            lines.append(current)
            current = word
    if current:
        lines.append(current)
    return lines


# --- GÉNÉRATION ---
with open(JSON_PATH, "r", encoding="utf-8") as file:
    pays = json.load(file)

print(f"Début de la génération de {len(pays)} cartes individuelles...")

for p in pays:
    # Canevas de la carte
    card_img = Image.new("RGB", (CARD_WIDTH, CARD_HEIGHT), (255, 255, 255))
    draw = ImageDraw.Draw(card_img)

    # 1. Template
    type_p = p.get("type")
    if type_p in templates_cache:
        t_img = templates_cache[type_p].resize(
            (CARD_WIDTH, CARD_HEIGHT), Image.Resampling.LANCZOS
        )
        card_img.paste(t_img, (0, 0))

        # 2. Forme + Drapeau
        iso_code = p.get("id").lower()
        shape_img = get_country_shape(iso_code)
        flag_img = get_country_flag(iso_code)

        if shape_img and flag_img:
            target_size = 180 * SCALE
            w, h = shape_img.size
            ratio = min(target_size / w, target_size / h)
            new_size = (int(w * ratio), int(h * ratio))

            special_cases = ["xk", "fm", "ps", "tv"]
            if iso_code in special_cases:
                mask = ImageOps.invert(shape_img.convert("RGBA").convert("L")).resize(
                    new_size, Image.Resampling.LANCZOS
                )
            else:
                mask = (
                    shape_img.getchannel("A").resize(new_size, Image.Resampling.LANCZOS)
                    if "A" in shape_img.getbands()
                    else shape_img.convert("L").resize(
                        new_size, Image.Resampling.LANCZOS
                    )
                )

            flag_resized = ImageOps.fit(
                flag_img, new_size, method=Image.Resampling.LANCZOS
            )
            combined = Image.new("RGBA", new_size, (0, 0, 0, 0))
            combined.paste(flag_resized, (0, 0), mask=mask)

            card_img.paste(
                combined, ((CARD_WIDTH - new_size[0]) // 2, 90 * SCALE), combined
            )

    # 3. Textes et Stats
    padding_inside = 10 * SCALE
    y_start = padding_inside

    # État
    draw.text(
        (250 * SCALE, y_start), str(p["etat"]), fill=HEADER_COLOR, font=font_bold_big
    )
    # Nom
    draw.text(
        (10 * SCALE, y_start + 4 * SCALE), p["nom"], fill=HEADER_COLOR, font=font_bold
    )
    # Rang
    draw.text(
        (padding_inside - 3 * SCALE, y_start + 45 * SCALE),
        str(p["rang"]),
        fill=TEXT_COLOR,
        font=font_italic,
    )

    # Stats
    stats_y = y_start + 273 * SCALE
    stats = p["statistiques"]
    draw.text(
        (padding_inside + 120 * SCALE, stats_y),
        str(stats["puissance"]),
        fill=TEXT_COLOR,
        font=font_small,
    )
    draw.text(
        (padding_inside + 255 * SCALE, stats_y),
        str(stats["ressources"]),
        fill=TEXT_COLOR,
        font=font_small,
    )

    stats_y += 28 * SCALE
    draw.text(
        (padding_inside + 120 * SCALE, stats_y),
        str(stats["technologie"]),
        fill=TEXT_COLOR,
        font=font_small,
    )
    draw.text(
        (padding_inside + 255 * SCALE, stats_y),
        str(stats["stabilite"]),
        fill=TEXT_COLOR,
        font=font_small,
    )

    stats_y += 27 * SCALE
    draw.text(
        (padding_inside + 190 * SCALE, stats_y),
        str(stats["economie"]),
        fill=TEXT_COLOR,
        font=font_small,
    )

    # Special
    special_y = stats_y + 24 * SCALE
    special = p["special"]
    draw.text(
        (padding_inside + 30 * SCALE, special_y),
        special["nom"],
        fill=TEXT_COLOR,
        font=font_bold_italic_small,
    )

    cout_x = padding_inside + (247 * SCALE if special["cout"] > 100 else 252 * SCALE)
    draw.text(
        (cout_x, special_y), str(special["cout"]), fill=TEXT_COLOR, font=font_bold_small
    )

    desc_y = special_y + 15 * SCALE
    desc_lines = wrap_text(
        draw,
        special["description"],
        font_small,
        (CARD_WIDTH - 2 * padding_inside) - (35 * SCALE),
    )
    for line in desc_lines:
        draw.text(
            (padding_inside + 30 * SCALE, desc_y),
            line,
            fill=TEXT_COLOR,
            font=font_small_small_italic,
        )
        desc_y += 16 * SCALE

    # Coûts et Lore
    bottom_y = desc_y + 9 * SCALE  # Note: ajusté dynamiquement selon la desc
    if bottom_y < 390 * SCALE:
        bottom_y = 398 * SCALE  # Sécurité alignement

    draw.text(
        (padding_inside + 37 * SCALE, bottom_y),
        str(p["cout"]),
        fill=TEXT_COLOR,
        font=font_small,
    )
    draw.text(
        (padding_inside + 56 * SCALE, bottom_y + 12 * SCALE),
        str(p["cout"] // 2),
        fill=TEXT_COLOR,
        font=font_small,
    )

    lore_lines = wrap_text(
        draw, p["lore"], font_small, (CARD_WIDTH - 2 * padding_inside) - (50 * SCALE)
    )
    for i, line in enumerate(lore_lines):
        draw.text(
            (padding_inside + 95 * SCALE, bottom_y + 2 * SCALE + (i * 11 * SCALE)),
            line,
            fill=TEXT_COLOR,
            font=font_small_small_small_italic,
        )

    # 4. Sauvegarde
    file_name = f"{p.get('id').lower()}.png"
    card_img.save(os.path.join(OUTPUT_DIR, file_name))

print(f"Terminé ! {len(pays)} cartes exportées dans '{OUTPUT_DIR}'.")
