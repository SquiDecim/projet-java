# projet-java
Projet Java L2 Info 2026 - Application d'un jeu de cartes stratégique (TCG)

Concept :
Créer une application graphique permettant de jouer en solo ou à plusieurs à un jeu de cartes de type Pokémon.
Chaque carte possède des caractéristiques (attaque, défense, énergie, …) et des effets spéciaux (soin, buff, debuff, combo, etc).
L’application gère les règles du jeu, les interactions complexes entre les cartes et l’affichage du plateau et des cartes.

1. PAYS :

    Structure :
    {
        "id":    "CODE ISO",
        "nom":   "Nom du pays",
        "rang":  "Marginal | Émergent | Établi | Dominant | Hégémonie",
        "type":  "Militaire | Économique | Diplomatique | Renseignement | Isolationniste | Technologique",
        "cout":  150 | 350 | 550 | 750 | 850,
        "etat":  N / 1000,
        "statistiques": {
        "puissance": N / 100, "economie": N / 100, "ressources": N /100,
        "technologie": N / 100, "stabilite": N / 100
        },
        "special": {
        "nom":         "Nom du coup spécial",
        "cout":        N,
        "description": "Texte narratif expliquant l'effet spécial.",
        "effets":      [ [degatAdverse, valeur], [degatBanc, valeur], [soinJeu, valeur], [soinJeu, valeur], [voleCredit, valeur], [pioche, valeur]]
        },
        "lore": "Texte narratif."
    }

    Effets disponibles possible du spécial :
    Type           | Paramètre | Cible                         | Effet
    ---------------|-----------|-------------------------------|--------------------------------
    degatAdverse   | N         | Pays adverse en jeu           | Inflige N PV de dégâts
    degatBanc      | N         | Choisi par le joueur          | Inflige N PV de dégâts
    soinJeu        | N         | Votre pays en jeu             | Restaure N PV
    soinBanc       | N         | Choisi par le joueur          | Restaure N PV
    voleCredit     | N         | Crédits adverses              | Vole N crédits (peut aller en négatif)
    pioche         | N         | Votre deck                    | Pioche N cartes (stop si deck vide)


2. ACTIONS :

    Structure :
    {
        "id":          "ACT-NNN",
        "nom":         "nom",
        "cond":        null | { ... },
        "effets":      [ [soinJeu, valeur], [soinBanc, valeur], [pioche, valeur],[bloquerOutilA, nbrTour], [echangeBanc, -], [echangeBancRandom, -], [soin, valeur]  ],
        "description": "Texte narratif."
    }

    Effets disponibles (actions) :
    Type               | Paramètre | Effet
    ------------------- ----------- -----------------------------------------------
    soinJeu            | N         | Soigne votre pays actif de N PV
    soinBanc           | N         | Soigne un pays de votre banc de N PV
    pioche             | N         | Pioche N cartes
    bloquerOutilA      | N         | Bloque la carte outil adverse N tours
    echangeBanc        | —         | Échange votre pays actif avec un pays du banc (au choix)
    echangeBancRandom  | —         | Échange votre pays actif avec un pays du banc (aléatoire)
    soin               | N         | Soigne le pays entrant après un échange de N PV

    Notes :
    - "soin" s'utilise toujours après "echangeBanc" ou "echangeBancRandom".
    - "echangeBanc" laisse le joueur choisir la carte du banc remplaçante.
    - "echangeBancRandom" tire une carte du banc au hasard.


3. OUTILS :

    Structure :
    {
        "id":          "OUT-NNN",
        "nom":         "nom",
        "cond":        null | { ... },
        "effets":      [ ["puissance", 0], ["puissanceA", 0], ["economie", 0], ["economieA", 0], ["ressources", 0], ["ressourcesA", 0], ["technologie", 0], ["technologieA", 0], ["stabilite", 0], ["stabiliteA", 0], ["CoutES", 0], ["CoutESA", 0], ["CoutR", 0], ["CoutRA", 0]  ],
        "description": "Texte narratif."
    }

    Effets disponibles (outils) — s'appliquent à chaque tour tant que la carte est active (non bloquée) :
    | Type          | Paramètre | Effet
    ---------------- ----------- ----------------------------------------------------------
    puissance       | N         | Ajoute N en puissance tant que l’outil est actif
    puissanceA      | N         | Modifie la puissance adverse de N
    economie        | N         | Ajoute N en économie tant que l’outil est actif
    economieA       | N         | Modifie l’économie adverse de N
    ressources      | N         | Ajoute N en ressources tant que l’outil est actif
    ressourcesA     | N         | Modifie les ressources adverses de N
    technologie     | N         | Ajoute N en technologie tant que l’outil est actif
    technologieA    | N         | Modifie la technologie adverse de N
    stabilite       | N         | Ajoute N en stabilité tant que l’outil est actif
    stabiliteA      | N         | Modifie la stabilité adverse de N
    CoutES          | N         | Modifie le coût d’activation de votre effet spécial
    CoutESA         | N         | Modifie le coût d’activation de l’effet spécial adverse
    CoutR           | N         | Modifie le coût de retrait de vos pays
    CoutRA          | N         | Modifie le coût de retrait adverse


4. CONDITION :

    Clé        | Type de valeur | Description
    -----------|----------------|--------------------------------------------------
    "type"     | ["...", ...]   | Votre pays actif doit appartenir à l'un des types listés
    "rang"     | ["...", ...]   | Votre pays actif doit avoir l'un des rangs listés
    "terrain"  | ["...", ...]   | Le terrain de la partie doit être dans la liste
    "etatMax"  | N              | Votre pays actif doit avoir ≤ N PV restants
    "etatMin"  | N              | Votre pays actif doit avoir ≥ N PV restants
    "statMin"  | {"stat": N}    | La stat indiquée de votre pays actif doit être ≥ N

    Valeurs possibles — "type" :
        Militaire | Économique | Diplomatique | Renseignement | Isolationniste | Technologique

    Valeurs possibles — "rang" (ordre croissant) :
        Marginal | Émergent | Établi | Dominant | Hégémonie

    Valeurs possibles — "terrain" :
        Tempéré | Désertique | Tropical | Montagneux | Glacial | Océanique

    Valeurs possibles — "statMin" (clés) :
        puissance | economie | ressources | technologie | stabilite


5. TABLES DES TYPES :

    Économique : 50  contre Militaire, 25 contre Diplomatie, 0 contre lui-même, -15 contre Renseignement, -25 contre Isolationniste


    Renseignement : 50  contre Isolationniste, 25 contre Economique, 0 contre lui-même, -15 contre Militaire, -25 contre Diplomatie


    Isolationniste : 50  contre Economique, 25 contre Militaire, 0 contre lui-même, -15 contre Diplomatie, -25 contre Renseignement


    Militaire : 50 contre Diplomatie, 25 contre Renseignement, 0 contre lui-même, -15 contre Isolationniste, -25 contre Economique


    Diplomatique : 50  contre Renseignement, 25 contre Isolationniste, 0 contre lui-même, -15 contre Economique, -25 contre Militaire
