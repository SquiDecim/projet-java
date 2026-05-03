import json
import os

from PIL import Image, ImageDraw, ImageFont

# --- CONFIGURATION DES CHEMINS ---
JSON_PATH = (
    "/home/user-x/Documents/GitHub/projet-java/genialtcg/assets/JSON/outils.json"
)
OUTPUT_DIR = "tools/img/output_cards_outils"
TEMPLATE_PATH = "tools/img/template/outils.png"

if not os.path.exists(OUTPUT_DIR):
    os.makedirs(OUTPUT_DIR)

# --- CONFIGURATION DES CONSTANTES ---
CARD_WIDTH = 320
CARD_HEIGHT = 448
TEXT_COLOR = (0, 0, 0)

# --- POLICES ---
# Note : Assure-toi que ces chemins sont corrects sur ton système (Linux/Windows)
font_path = "/usr/share/fonts/truetype/dejavu/"
font_bold_big = ImageFont.truetype(font_path + "DejaVuSans-Bold.ttf", 24)
font_bold = ImageFont.truetype(font_path + "DejaVuSans-Bold.ttf", 18)
font_italic = ImageFont.truetype(font_path + "DejaVuSans-Oblique.ttf", 18)
font_small = ImageFont.truetype(font_path + "DejaVuSans.ttf", 12)
font_small_small_italic = ImageFont.truetype(font_path + "DejaVuSans-Oblique.ttf", 11)
font_small_small_small_italic = ImageFont.truetype(
    font_path + "DejaVuSans-Oblique.ttf", 10
)
font_bold_small = ImageFont.truetype(font_path + "DejaVuSans-Bold.ttf", 12)
font_bold_italic_small = ImageFont.truetype(
    font_path + "DejaVuSans-BoldOblique.ttf", 12
)


def wrap_text(text, font, max_width, draw):
    words = text.split()
    lines = []
    current_line = ""
    for word in words:
        test_line = current_line + " " + word if current_line else word
        bbox = draw.textbbox((0, 0), test_line, font=font)
        if bbox[2] <= max_width:
            current_line = test_line
        else:
            lines.append(current_line)
            current_line = word
    lines.append(current_line)
    return lines


with open(JSON_PATH, "r", encoding="utf-8") as file:
    outils = json.load(file)

template_img = Image.open(TEMPLATE_PATH).convert("RGBA")

print(f"Génération de {len(outils)} cartes outils...")

for p in outils:
    card_img = template_img.copy()
    draw = ImageDraw.Draw(card_img)

    draw.text((40, 75), p["nom"], font=font_bold, fill=TEXT_COLOR)

    desc_text = p["description"]
    lines = wrap_text(desc_text, font_italic, 240, draw)
    y_offset = 308
    for line in lines:
        draw.text((40, y_offset), line, font=font_italic, fill=TEXT_COLOR)
        y_offset += 18

    clean_name = p["nom"].replace(" ", "_").lower()
    file_name = f"{p['id']}_{clean_name}.png"
    card_img.convert("RGB").save(os.path.join(OUTPUT_DIR, file_name))

print(f"Terminé ! Les cartes sont dans le dossier '{OUTPUT_DIR}'.")
