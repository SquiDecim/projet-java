from PIL import Image, ImageDraw, ImageFont, ImageOps
import urllib.request
from io import BytesIO
import os

def around_number(n):
    n=float(n)
    if n >= 1_000_000_000_000:
        return f"{n // 1_000_000_000_000}T"
    elif n >= 1_000_000_000:
        return f"{n // 1_000_000_000}B"
    elif n >= 1_000_000:
        return f"{n // 1_000_000}M"
    elif n >= 1_000:
        return f"{n // 1_000}k"
    else:
        return str(n)

def create_country_card(country):

    #Duplication du template nation
    iso2 = country["iso2"]
    base_dir = os.path.dirname(__file__)
    source = os.path.join(base_dir, "img/template/nation_template.png")
    destination = os.path.join(base_dir, f"img/countries_cards/nation_{iso2}.png")
    
    #Récupération des données du pays
    name = country["name"]
    population = around_number(country["population"])
    pib = around_number(country["pib_usd"])
    territory = os.path.join(base_dir, f"img/countries_territories/{iso2.lower()}/512.png")
    flag_url = country["flag"]

    #Création de la carte
    card_image = Image.open(source)
    width, height = card_image.size
    draw = ImageDraw.Draw(card_image)
    fontTitlePath = os.path.join(base_dir, "font/OpenSans-Bold.ttf")
    fontStatsPath = os.path.join(base_dir, "font/OpenSans-Light.ttf")

    fontTitle = ImageFont.truetype(fontTitlePath, size = 75)
    fontStats = ImageFont.truetype(fontStatsPath, size = 50)

    draw.text((width//2, 135), name.upper(), font = fontTitle, fill=(82, 84, 87), align="center", anchor="mm") #Titre

    draw.text((70, 710), f"Population : {population}", font = fontStats, fill=(82, 84, 87), align="center", anchor="lm")
    draw.text((70, 780), f"PIB : {pib} $", font = fontStats, fill=(82, 84, 87), align="center", anchor="lm")


    territory_image = Image.open(territory).convert("RGBA")
    flag_image = Image.open(BytesIO(urllib.request.urlopen(flag_url).read())).convert("RGBA")

    alpha = territory_image.getchannel("A")
    flag_image = ImageOps.fit(flag_image, territory_image.size, method=Image.LANCZOS)

    illustration_image = Image.new("RGBA", territory_image.size)
    illustration_image.paste(flag_image, (0,0), alpha)
    illustration_image = illustration_image.resize((int(illustration_image.size[0]*0.78), int(illustration_image.size[1]*0.78)))

    card_image.paste(illustration_image, (width//2 - illustration_image.size[0]//2 ,225), mask = illustration_image)

    card_image.save(destination)
    card_image.show()



country = {
        "name": "France",
        "iso2": "FR",
        "population": 66351959,
        "pib_usd": 3160442622465.08,
        "military_expenditure_usd": 64675015306.1997,
        "flag": "https://flagcdn.com/w320/fr.png"
    }
create_country_card(country)

