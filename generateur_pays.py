import json
import random
import re

# ==========================================
# 1. FONCTIONS OUTILS
# ==========================================

def nettoyer_stat(valeur, defaut=30):
    """
    Vérifie si une statistique est un nombre. 
    Si c'est 'à remplir', retourne une valeur par défaut.
    """
    if isinstance(valeur, int):
        return valeur
    elif isinstance(valeur, str) and valeur.isdigit():
        return int(valeur)
    else:
        # Génère une statistique aléatoire entre 10 et 50 pour varier un peu
        return random.randint(10, 50)

def generer_id(nom_pays, index):
    """Génère un ID unique propre (ex: FR_001)."""
    # Prend les 2 premières lettres, met en majuscule, enlève les accents/espaces
    prefixe = re.sub(r'[^A-Z]', '', nom_pays.upper())[:2]
    if len(prefixe) < 2:
        prefixe = prefixe.ljust(2, 'X')
    return f"{prefixe}_{index:03d}"

# ==========================================
# 2. LOGIQUE PRINCIPALE
# ==========================================

def transformer_donnees():
    print("Début de la transformation des données...")

    # Chargement du fichier source
    try:
        with open('countries_cards_data.json', 'r', encoding='utf-8') as f:
            data_brute = json.load(f)
    except FileNotFoundError:
        print("Erreur : Le fichier 'countries_cards_data.json' est introuvable.")
        return

    pays_complets = []
    index = 1
    
    # Listes définies dans tes règles pour générer des données de base
    rangs = ["Marginal", "Émergent", "Établi", "Dominant", "Superpuissance"]
    types_pays = ["Économique", "Renseignement", "Isolationniste", "Militaire", "Diplomatique"]
    types_effets = ["Dégât sur l'état du pays adverse", "Soin sur l'état du pays allié", 
                    "debuff économique adverse", "buff économique allié", 
                    "debuff statistique(s) adverse", "buff statistique(s) allié", 
                    "Effet sur la pioche", "bloque le tour adverse"]

    # Parcours de chaque pays dans ton fichier incomplet
    for nom, stats in data_brute.items():
        # Nettoyage des statistiques
        puissance = nettoyer_stat(stats.get("military"))
        economie = nettoyer_stat(stats.get("gdp"))
        ressources = nettoyer_stat(stats.get("ressources"))
        techno = nettoyer_stat(stats.get("innovation"))
        stabilite = nettoyer_stat(stats.get("stability"))
        
        # Calculs selon tes règles
        somme_stats = puissance + economie + ressources + techno + stabilite
        etat_initial = somme_stats * 2  # /1000 selon tes règles, ajusté ici
        
        # Détermination du rang et du coût
        rang_choisi = random.choice(rangs)
        cout = 0
        if rang_choisi == "Marginal": cout = 150
        elif rang_choisi == "Émergent": cout = 350
        elif rang_choisi == "Établi": cout = 550
        elif rang_choisi == "Dominant": cout = 750
        else: cout = 900
        
        # Construction de l'objet Pays
        nouveau_pays = {
            "id": generer_id(nom, index),
            "nom": nom,
            "emoji": "🏳️", # L'emoji de base, tu pourras utiliser un dictionnaire pour les mapper plus tard
            "rang": rang_choisi,
            "type": random.choice(types_pays),
            "image": f"images/{generer_id(nom, index)}.png",
            "coût_de_pose_en_credits": cout,
            "etat_du_pays_initial": etat_initial,
            "statistiques": {
                "puissance": puissance,
                "économie": economie,
                "ressources": ressources,
                "technologie": techno,
                "stabilité": stabilite
            },
            "effet_spécial": {
                "type": random.choice(types_effets),
                "nom": "Capacité à définir",
                "coût_en_credit": int(cout / 3),
                "valeur": random.randint(10, 50),
                "lore/explication_effets": "Description de l'effet à rédiger."
            },
            "lore": "Histoire et contexte géopolitique du pays à rédiger."
        }
        
        pays_complets.append(nouveau_pays)
        index += 1

    # ==========================================
    # 3. SAUVEGARDE DU RESULTAT
    # ==========================================
    
    with open('pays_geopolitique_complet.json', 'w', encoding='utf-8') as f:
        json.dump(pays_complets, f, ensure_ascii=False, indent=4)
        
    print(f"Succès ! {len(pays_complets)} pays ont été générés dans 'pays_geopolitique_complet.json'.")

# Exécution du script
if __name__ == "__main__":
    transformer_donnees()
