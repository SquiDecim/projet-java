import json
import os

from PIL import Image, ImageDraw, ImageFont

# --- CONFIGURATION DES CHEMINS ---
JSON_PATH = (
    "/home/user-x/Documents/GitHub/projet-java/genialtcg/assets/JSON/actions.json"
)
OUTPUT_DIR = "tools/img/output_cards_actions"

TEMPLATE_DIR = "tools/img/template"
TEMPLATE_PATH = os.path.join(TEMPLATE_DIR, "action.png")
LOGO_PATH = os.path.join(TEMPLATE_DIR, "action_logo.png")

# Templates spéciaux à partir de ACT-61
TEMPLATES_ACT_61 = [
    "désertique.png",
    "océanique.png",
    "montagneux.png",
    "tropical.png",
    "glacial.png",
]

# Coordonnées du carré blanc dans le template
WHITE_BOX = (40, 113, 285, 297)  # (left, top, right, bottom)
LOGO_PADDING = 10

if not os.path.exists(OUTPUT_DIR):
    os.makedirs(OUTPUT_DIR)

# --- CONFIGURATION DES CONSTANTES ---
CARD_WIDTH = 320
CARD_HEIGHT = 448
TEXT_COLOR = (0, 0, 0)

# --- POLICES ---
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


def get_act_number(act_id: str) -> int:
    # "ACT-61" -> 61
    return int(act_id.split("-")[1])


with open(JSON_PATH, "r", encoding="utf-8") as file:
    actions = json.load(file)

logo_raw = Image.open(LOGO_PATH).convert("RGBA")

print(f"Génération de {len(actions)} cartes actions...")

for p in actions:
    act_number = get_act_number(p["id"])

    # --- CHOIX DU TEMPLATE ---
    if act_number >= 61:
        index = (act_number - 61) % len(TEMPLATES_ACT_61)
        template_file = os.path.join(TEMPLATE_DIR, TEMPLATES_ACT_61[index])
    else:
        template_file = TEMPLATE_PATH

    template_img = Image.open(template_file).convert("RGBA")
    card_img = template_img.copy()

    # --- LOGO CENTRÉ ---
    box_w = WHITE_BOX[2] - WHITE_BOX[0] - LOGO_PADDING * 2
    box_h = WHITE_BOX[3] - WHITE_BOX[1] - LOGO_PADDING * 2

    logo_resized = logo_raw.copy()
    logo_resized.thumbnail((box_w, box_h), Image.LANCZOS)

    logo_x = WHITE_BOX[0] + LOGO_PADDING + (box_w - logo_resized.width) // 2
    logo_y = WHITE_BOX[1] + LOGO_PADDING + (box_h - logo_resized.height) // 2

    if act_number < 61:
        card_img.paste(logo_resized, (logo_x, logo_y), logo_resized)

    draw = ImageDraw.Draw(card_img)

    # --- TITRE ---
    draw.text((40, 75), p["nom"], font=font_bold, fill=TEXT_COLOR)

    # --- DESCRIPTION ---
    desc_text = p["description"]
    lines = wrap_text(desc_text, font_italic, 240, draw)
    y_offset = 308
    for line in lines:
        draw.text((40, y_offset), line, font=font_italic, fill=TEXT_COLOR)
        y_offset += 18

    # --- SAUVEGARDE ---
    clean_name = p["nom"].replace(" ", "_").lower()
    file_name = f"{p['id']}_{clean_name}.png"
    card_img.convert("RGB").save(os.path.join(OUTPUT_DIR, file_name))

print(f"Terminé ! Les cartes sont dans le dossier '{OUTPUT_DIR}'.")
