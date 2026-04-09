from PIL import Image, ImageDraw, ImageFont
import json

JSON_PATH = "C:\\Users\\ahamelin\\Documents\\GitHub\\projet-java\\data\\JSON\\pays.json"
with open(JSON_PATH, 'r', encoding='utf-8') as file:
    pays = json.load(file)

# Config
font = ImageFont.truetype("arial.ttf", 18)
font_small = ImageFont.truetype("arial.ttf", 14)
TEXT_COLOR = (0, 0, 0)
HEADER_COLOR = (0, 0, 0)
TYPE_COLORS = {
    "Isolationniste": (255, 0, 0),
    "Économique": (255, 255, 0),
    "Militaire": (107, 142, 35),
    "Renseignement": (100, 100, 100),
    "Diplomatique": (0, 102, 204)
}

CARD_WIDTH = 320
CARD_HEIGHT = 448
PADDING = 20
COLUMN_COUNT = 10  

row_count = (len(pays) + COLUMN_COUNT - 1) // COLUMN_COUNT
img_width = COLUMN_COUNT * CARD_WIDTH + (COLUMN_COUNT-1)
img_height = row_count * CARD_HEIGHT + (row_count-1)

img = Image.new('RGB', (img_width, img_height), (255, 255, 255))
draw = ImageDraw.Draw(img)


# Fonction wrap texte
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

    # Fond coloré selon le type
    bg_color = TYPE_COLORS.get(p['type'], (240, 240, 240))
    draw.rectangle([(current_x, current_y),(current_x + CARD_WIDTH, current_y + CARD_HEIGHT)],fill=bg_color,width=2)

    padding_inside = 10
    x_text = current_x + padding_inside
    y_text = current_y + padding_inside

    # Nom centré
    name = p['nom']
    w = draw.textbbox((0, 0), name, font=font)[2]
    draw.text(
        (current_x + (CARD_WIDTH - w)//2, y_text),
        name,
        fill=HEADER_COLOR,
        font=font
    )

    y_text += 40

    # Rang / Type 
    draw.text((x_text, y_text), f"Rang: {p['rang']}", fill=TEXT_COLOR, font=font_small)
    draw.text((x_text, y_text + 20), f"Type: {p['type']}", fill=TEXT_COLOR, font=font_small)
    y_text += 50

    # Stats 
    stats = p['statistiques']
    stats_labels = [
        ("Puissance", stats['puissance']),
        ("Économie", stats['economie']),
        ("Ressources", stats['ressources']),
        ("Technologie", stats['technologie']),
        ("Stabilité", stats['stabilite'])
    ]

    for i, (label, value) in enumerate(stats_labels):
        y = y_text + i*20
        draw.text((x_text, y), f"{label}: {value}", fill=TEXT_COLOR, font=font_small)
    y_text += 120

    # Lore
    lore_lines = wrap_text(draw, "Lore: " + p['lore'], font_small, CARD_WIDTH - 2*padding_inside)
    for line in lore_lines:
        draw.text((x_text, y_text), line, fill=TEXT_COLOR, font=font_small)
        y_text += 18


img.save("cartes_pays.png")
print(" Image générée ")