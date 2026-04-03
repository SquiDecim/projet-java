from PIL import Image, ImageDraw, ImageFont
import json
import textwrap

# Chemin absolu vers le fichier JSON
JSON_PATH = "/home/user-x/Documents/GitHub/projet-java/data/JSON/pays.json"

# Charger les données des pays
with open(JSON_PATH, 'r', encoding='utf-8') as file:
    pays = json.load(file)

# Polices (remplace par le chemin vers tes fichiers .ttf si nécessaire)
try:
    font = ImageFont.truetype("/home/user-x/Documents/GitHub/projet-java/cards_generation/font/NotoSans-Bold.ttf", 16)
    font_small = ImageFont.truetype("/home/user-x/Documents/GitHub/projet-java/cards_generation/font/NotoSans-Regular.ttf", 12)
    emoji_font = ImageFont.truetype("/home/user-x/Documents/GitHub/projet-java/cards_generation/font/NotoColorEmoji.ttf", 24)
except:
    print("Police Noto non trouvée. Utilisation de la police par défaut.")
    font = ImageFont.load_default()
    font_small = ImageFont.load_default()
    emoji_font = ImageFont.load_default()

# Taille de la carte (ajustée pour contenir toutes les infos)
CARD_WIDTH = 300
CARD_HEIGHT = 10000  # Hauteur suffisante pour tous les pays

# Couleurs
BACKGROUND_COLOR = (255, 255, 255)
BORDER_COLOR = (0, 0, 0)
TEXT_COLOR = (0, 0, 0)
HEADER_COLOR = (100, 100, 200)  # Couleur pour l'en-tête

TYPE_COLORS = {
    "Isolationniste": (200, 200, 200),
    "Économique": (100, 200, 100),
    "Militaire": (200, 100, 100),
    "Dominant": (100, 100, 200),
    "Émergent": (200, 200, 100)
}

def draw_text_with_wrap(draw, text, x, y, font, max_width, line_spacing=20):
    """Dessine du texte avec retour à la ligne automatique."""
    lines = textwrap.wrap(text, width=max_width)
    for i, line in enumerate(lines):
        draw.text((x, y + i * line_spacing), line, fill=TEXT_COLOR, font=font)

# Créer une seule image pour tous les pays
img = Image.new('RGB', (CARD_WIDTH, CARD_HEIGHT), BACKGROUND_COLOR)
draw = ImageDraw.Draw(img)

# Dessiner la bordure
draw.rectangle([(0, 0), (CARD_WIDTH, CARD_HEIGHT)], outline=BORDER_COLOR, width=3)

# Position initiale
current_y = 20

# Pour chaque pays, ajouter ses infos sur la carte
for pays_data in pays:
    # En-tête : Emoji + Nom en couleur
    draw.text((20, current_y), pays_data['emoji'], fill=HEADER_COLOR, font=emoji_font)
    draw.text((50, current_y), f" {pays_data['nom']}", fill=HEADER_COLOR, font=font)
    current_y += 40

    # Rang et Type
    draw.text((20, current_y), f"Rang: {pays_data['rang']}", fill=TEXT_COLOR, font=font_small)
    draw.text((20, current_y + 20), f"Type: {pays_data['type']}", fill=TYPE_COLORS.get(pays_data['type'], TEXT_COLOR), font=font_small)
    current_y += 50

    # Statistiques
    stats = pays_data['statistiques']
    draw.text((20, current_y), "Statistiques:", fill=TEXT_COLOR, font=font_small)
    draw.text((20, current_y + 20), f"Puissance: {stats['puissance']}", fill=TEXT_COLOR, font=font_small)
    draw.text((20, current_y + 40), f"Économie: {stats['economie']}", fill=TEXT_COLOR, font=font_small)
    draw.text((20, current_y + 60), f"Ressources: {stats['ressources']}", fill=TEXT_COLOR, font=font_small)
    draw.text((20, current_y + 80), f"Technologie: {stats['technologie']}", fill=TEXT_COLOR, font=font_small)
    draw.text((20, current_y + 100), f"Stabilité: {stats['stabilite']}", fill=TEXT_COLOR, font=font_small)
    current_y += 140

    # Lore : Découpage automatique si trop long
    lore = pays_data['lore']
    draw_text_with_wrap(draw, f"Lore: {lore}", 20, current_y, font_small, 25)
    current_y += 100  # Espacement après le lore

    # Ligne de séparation entre chaque pays
    draw.line([(0, current_y), (CARD_WIDTH, current_y)], fill=BORDER_COLOR, width=1)
    current_y += 20

# Sauvegarder la carte unifiée
img.save("carte_unifiee.png")
print("Carte unifiée générée : carte_unifiee.png")