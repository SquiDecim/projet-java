import requests
import math
import json
import os
import unicodedata

COUNTRIES_API_URL = "https://iso.lahrim.fr/countries"
REST_COUNTRIES_URL = "https://restcountries.com/v3.1/all?fields=cca2,flags"
WORLD_BANK_BASE = "https://api.worldbank.org/v2/country/all/indicator/{}?date={}&format=json&per_page=20000"

INDICATORS = {
    "gdp": ("NY.GDP.MKTP.CD", 2023),
    "military": ("MS.MIL.XPND.CD", 2023),
    "stability": ("PV.EST", 2023),
    "ressources": ("NY.GDP.TOTL.RT.ZS", 2021),
    "innovation": ("GB.XPD.RSDV.GD.ZS", 2022)
}

LOG_KEYS = {"gdp", "military"}

ALLOWED_COUNTRIES = """Afghanistan, Afrique du Sud, Albanie, Algérie, Allemagne, Andorre, Angola, Antigua-et-Barbuda, Saudi Arabia, Argentina, Arménie, Australie, Autriche, Azerbaïdjan, Bahamas, Bahreïn, Bangladesh, Barbade, Belgique, Belize, Bénin, Bhoutan, Bélarus, Myanmar, Bolivie, Bosnie-Herzégovine, Botswana, Brésil, Brunei, Bulgarie, Burkina Faso, Burundi, Cambodge, Cameroun, Canada, Cap-Vert, Chili, Chine, Chypre, Colombie, Comores, Corée du Nord, Corée du Sud, Costa Rica, Côte d'Ivoire, Croatie, Cuba, Danemark, Djibouti, Dominique, Egypt, United Arab Emirates, Equateur, Erythrée, Spain, Eswatini, Estonie, États-Unis, Éthiopie, Fidji, Finlande, France, Gabon, Gambie, Géorgie, Ghana, Grèce, Grenade, Guatemala, Guinée, Guinée-Bissau, Guinée équatoriale, Guyana, Haïti, Honduras, Hongrie, Îles Marshall, Îles Salomon, Inde, Indonésie, Irak, Iran, Irlande, Islande, Israël, Italie, Jamaïque, Japon, Jordanie, Kazakhstan, Kenya, Kyrgyzstan, Kiribati, Koweït, Laos, Lesotho, Lettonie, Liban, Libéria, Libye, Liechtenstein, Lituanie, Luxembourg, Macédoine du Nord, Madagascar, Malaisie, Malawi, Maldives, Mali, Malte, Maroc, Maurice, Mauritanie, Mexique, Micronésie (États fédérés de), Moldavie, Monaco, Mongolie, Monténégro, Mozambique, Namibie, Nauru, Népal, Nicaragua, Niger, Nigeria, Norvège, Nouvelle-Zélande, Oman, Ouganda, Ouzbékistan, Pakistan, Palau, Palestine, Panama, Papua New Guinea, Paraguay, Pays-Bas, Peru, Philippines, Pologne, Portugal, Qatar, République centrafricaine, République démocratique du Congo, République dominicaine, Congo, République tchèque, România, United Kingdom, Russie, Rwanda, Saint-Kitts-et-Nevis, Sainte-Lucie, Saint-Marin, Saint-Vincent-et-les-Grenadines, El Salvador, Samoa américaines, São Tomé and Príncipe, Sénégal, Serbie, Seychelles, Sierra Leone, Singapour, Slovaquie, Slovénie, Somalie, Soudan, South Sudan, Sri Lanka, Suède, Switzerland, Suriname, Syria, Tadjikistan, Tanzanie, Taiwan, Chad, Thailand, Timor-Leste, Togo, Tonga, Trinidad et Tobago, Tunisia, Turkmenistan, Turquie, Tuvalu, Ukraine, Uruguay, Vanuatu, Vatican, Venezuela, Viêt Nam, Yémen, Zambie, Zimbabwe"""



def normalize_name(name):
    name = name.lower()
    name = ''.join(c for c in unicodedata.normalize('NFD', name) if unicodedata.category(c) != 'Mn')
    name = ''.join(c for c in name if c.isalnum())
    return name

ALLOWED_SET = set(normalize_name(c) for c in ALLOWED_COUNTRIES.split(", "))


print("Récupération des noms de pays...")
country_names = {}
resp = requests.get(COUNTRIES_API_URL).json()
for c in resp["data"]:
    country_names[c["alpha2"]] = c["name_fr"]
    print(f"  ✅ {c['name_fr']} ajouté")
print(f"Total pays récupérés : {len(country_names)}\n")


print("Récupération des drapeaux...")
flags = {}
resp = requests.get(REST_COUNTRIES_URL).json()
for c in resp:
    if "cca2" in c and "flags" in c:
        flags[c["cca2"]] = c["flags"]["png"]
        print(f"  🏳️ Drapeau de {c['cca2']} récupéré")
print(f"Total drapeaux récupérés : {len(flags)}\n")


countries = {}
for iso2 in country_names.keys():
    countries[iso2] = {k: "à remplir" for k in INDICATORS.keys()}


for key, (indicator, year) in INDICATORS.items():
    print(f"Récupération de {key} ({indicator}) année {year}...")
    url = WORLD_BANK_BASE.format(indicator, year)
    response = requests.get(url).json()
    count = 0
    for entry in response[1]:
        iso2 = entry["country"]["id"]
        value = entry["value"]
        if iso2 in countries and value is not None:
            countries[iso2][key] = value
            count += 1
    print(f"  ✅ {count} pays mis à jour pour {key}\n")


print("Calcul des min/max...")
stats = {}
for key in INDICATORS.keys():
    values = [v[key] for v in countries.values() if isinstance(v[key], (int,float))]
    if values:
        stats[key] = (min(values), max(values))
        print(f"  {key}: min={stats[key][0]}, max={stats[key][1]}")
print()


print("Normalisation...")
def normalize_log(value, min_v, max_v):
    if min_v == max_v: return 50
    return round(10 + (math.log(value+1)-math.log(min_v+1))/(math.log(max_v+1)-math.log(min_v+1))*80)

def normalize_linear(value, min_v, max_v):
    if min_v == max_v: return 50
    return round(10 + (value - min_v)/(max_v - min_v)*80)

for iso2, data in countries.items():
    for key in INDICATORS.keys():
        value = data[key]
        if isinstance(value,(int,float)):
            min_v,max_v = stats[key]
            data[key] = normalize_log(value,min_v,max_v) if key in LOG_KEYS else normalize_linear(value,min_v,max_v)
print("✅ Normalisation terminée\n")


print("Création du dictionnaire final filtré...")
final_countries = {}
for iso2, data in countries.items():
    name = country_names.get(iso2)
    if not name: continue
    if normalize_name(name) not in ALLOWED_SET:
        continue
    final_countries[name] = {**data, "flag": flags.get(iso2,"à remplir")}
print(f"Total pays gardés après filtrage : {len(final_countries)}\n")


base_dir = os.path.dirname(__file__)
destination = os.path.join(base_dir,"countries_cards_data.json")

with open(destination,"w",encoding="utf-8") as f:
    json.dump(final_countries,f,ensure_ascii=False,indent=4,sort_keys=True)

print(len(final_countries))
print(f"Données enregistrées dans {destination}")