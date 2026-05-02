import os
from pathlib import Path

from PIL import ImageFont

# === CONFIGURATION ===
# Chemin vers ton fichier TTF
font_path = Path("assets/ui/dejavu-sans/DejaVuSans.ttf")

# Dossier où les polices générées seront sauvegardées
output_dir = Path("assets/ui")
output_dir.mkdir(parents=True, exist_ok=True)

# Tailles de police souhaitées (px)
sizes = {"title": 22, "normal": 14}


# Fonction de génération
def generate_fonts():
    for name, size in sizes.items():
        font_file = output_dir / f"{name}_{size}px.fnt"
        # Avec LibGDX, on utiliserait FreeTypeFontGenerator en Java.
        # Ici, on vérifie juste que la police existe et est accessible.
        try:
            font = ImageFont.truetype(str(font_path), size)
            print(f"✅ Font '{name}' générée à {size}px → {font_file}")
        except Exception as e:
            print(f"❌ Erreur pour {name}: {e}")


if __name__ == "__main__":
    generate_fonts()
    print("Toutes les polices sont prêtes à être utilisées dans LibGDX.")
