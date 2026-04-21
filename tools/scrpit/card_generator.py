import json
import os
from io import BytesIO

import requests
from PIL import Image, ImageDraw, ImageFont, ImageOps

# chemin d'accès aux fichiers
JSON_PATH = "/home/user-x/Documents/GitHub/projet-java/genialtcg/assets/JSON/pays.json"
SHAPES_CACHE_DIR = "tools/img/shapes_cache"
FLAGS_CACHE_DIR = "tools/img/flags_cache"
OUTPUT_PATH = "genialtcg/assets/cards/atlas_pays.png"

# configuration des constantes
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

# Chargement des modèles et des caches
templates_cache = {}
for k, path in TEMPLATES.items():
    if os.path.exists(path):
        templates_cache[k] = Image.open(path)

for folder in [SHAPES_CACHE_DIR, FLAGS_CACHE_DIR]:
    if not os.path.exists(folder):
        os.makedirs(folder)

# Polices
font_path = "/usr/share/fonts/truetype/dejavu/"
font_bold_big = ImageFont.truetype(font_path + "DejaVuSans-Bold.ttf", 24 * SCALE)
font = ImageFont.truetype(font_path + "DejaVuSans.ttf", 18 * SCALE)
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


# fonction pour obtenir l'image de la forme d'un pays
def get_country_shape(iso_code):
    if not iso_code:
        return None
    iso_code = iso_code.lower()
    local_file = os.path.join(SHAPES_CACHE_DIR, f"{iso_code}.png")
    if os.path.exists(local_file):
        return Image.open(local_file).convert("RGBA")
    else:
        print(f"le code ISO {iso_code} n'a pas de shape")
    url = f"https://raw.githubusercontent.com/djaiss/mapsicon/master/all/{iso_code}/256.png"
    try:
        r = requests.get(url, timeout=5)
        if r.status_code == 200:
            with open(local_file, "wb") as f:
                f.write(r.content)
            return Image.open(BytesIO(r.content)).convert("RGBA")
    except:
        print(f"pas de forme pour {iso_code}")
    return None


# fonction pour obtenir l'image du drapeau d'un pays
def get_country_flag(iso_code):
    if not iso_code:
        return None
    iso_code = iso_code.lower()
    local_file = os.path.join(FLAGS_CACHE_DIR, f"{iso_code}.png")
    if os.path.exists(local_file):
        return Image.open(local_file).convert("RGBA")
    else:
        print(f"le code ISO {iso_code} n'a pas de drapeau")
    url = f"https://flagcdn.com/w640/{iso_code}.png"
    try:
        r = requests.get(url, timeout=5)
        if r.status_code == 200:
            with open(local_file, "wb") as f:
                f.write(r.content)
            return Image.open(BytesIO(r.content)).convert("RGBA")
    except:
        print(f"pas de drapeau pour {iso_code}")
    return None


# fonction pour écrire du texte sur l'image avec retour à la ligne quand le texte dépasse la largeur autorisée
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


# chargement des pays depuis le fichier JSON
with open(JSON_PATH, "r", encoding="utf-8") as file:
    pays = json.load(file)

row_count = (len(pays) + COLUMN_COUNT - 1) // COLUMN_COUNT
img_width = COLUMN_COUNT * CARD_WIDTH + 2 * PADDING
img_height = row_count * CARD_HEIGHT + 2 * PADDING
img = Image.new("RGB", (img_width, img_height), (255, 255, 255))
draw = ImageDraw.Draw(img)


# Ecriture sur l'atlas carte par carte
for idx, p in enumerate(pays):
    col_index = idx % COLUMN_COUNT
    row_index = idx // COLUMN_COUNT
    current_x = PADDING + col_index * CARD_WIDTH
    current_y = PADDING + row_index * CARD_HEIGHT

    # Image pays sans drapeau
    type_p = p.get("type")
    if type_p in templates_cache:
        t_img = templates_cache[type_p].resize(
            (CARD_WIDTH, CARD_HEIGHT), Image.Resampling.LANCZOS
        )
        img.paste(t_img, (current_x, current_y))

        # Image pays avec drapeau
        iso_code = p.get("id").lower()  # On s'assure d'être en minuscules
        shape_img = get_country_shape(iso_code)
        flag_img = get_country_flag(iso_code)

        if shape_img and flag_img:
            target_size = 180 * SCALE
            w, h = shape_img.size
            ratio = min(target_size / w, target_size / h)
            new_size = (int(w * ratio), int(h * ratio))

            # --- GESTION CONDITIONNELLE DES MASQUES ---

            # Liste des pays "problématiques" (fonds blancs opaques)
            special_cases = ["xk", "fm", "ps", "tv"]

            if iso_code in special_cases:
                # MÉTHODE A : Pour les fichiers opaques (noir sur blanc)
                # On force le lissage en convertissant proprement
                mask_temp = shape_img.convert("RGBA").convert("L")
                # On inverse pour que le noir devienne la zone de collage
                mask = ImageOps.invert(mask_temp)
                # On redimensionne avec LANCZOS pour essayer de garder un peu de douceur
                mask = mask.resize(new_size, Image.Resampling.LANCZOS)
            else:
                # MÉTHODE B : Pour les fichiers normaux avec transparence (ex: fr.png)
                if "A" in shape_img.getbands():
                    mask = shape_img.getchannel("A")
                else:
                    mask = shape_img.convert("L")
                mask = mask.resize(new_size, Image.Resampling.LANCZOS)

            # --- COLLAGE COMMUN ---

            # On prépare le drapeau
            flag_resized = ImageOps.fit(
                flag_img, new_size, method=Image.Resampling.LANCZOS
            )

            # Création de l'image découpée
            combined = Image.new("RGBA", new_size, (0, 0, 0, 0))
            combined.paste(flag_resized, (0, 0), mask=mask)

            # Collage final sur la carte
            s_w, s_h = combined.size
            img.paste(
                combined,
                (current_x + (CARD_WIDTH - s_w) // 2, current_y + 90 * SCALE),
                combined,
            )
    # TEXTE
    padding_inside = 10 * SCALE
    x_text = current_x + padding_inside
    y_text = current_y + padding_inside

    # Nom
    draw.text(
        (current_x + 10 * SCALE, y_text + 4 * SCALE),
        p["nom"],
        fill=HEADER_COLOR,
        font=font_bold,
    )

    y_text += 45 * SCALE
    # Rang
    draw.text(
        (x_text - 3 * SCALE, y_text),
        str(p["rang"]),
        fill=TEXT_COLOR,
        font=font_italic,
    )

    y_text += 307 * SCALE

    # Special
    special = p["special"]
    draw.text(
        (x_text + 30 * SCALE, y_text),
        special["nom"],
        fill=TEXT_COLOR,
        font=font_bold_italic_small,
    )

    y_text += 15 * SCALE

    desc_lines = wrap_text(
        draw,
        special["description"],
        font_small,
        (CARD_WIDTH - 2 * padding_inside) - (35 * SCALE),
    )
    for line in desc_lines:
        draw.text(
            (x_text + 30 * SCALE, y_text),
            line,
            fill=TEXT_COLOR,
            font=font_small_small_italic,
        )
        y_text += 16 * SCALE

    # Cout carte
    y_text += 9 * SCALE

    # Lore carte
    lore_lines = wrap_text(
        draw, p["lore"], font_small, (CARD_WIDTH - 2 * padding_inside) - (50 * SCALE)
    )
    for line in lore_lines:
        draw.text(
            (x_text + 95 * SCALE, y_text + 2 * SCALE),
            line,
            fill=TEXT_COLOR,
            font=font_small_small_small_italic,
        )
        y_text += 11 * SCALE

img.save(OUTPUT_PATH, dpi=(300, 300))
print("Atlas généré")
