# GenialTCG

A [libGDX](https://libgdx.com/) project generated with [gdx-liftoff](https://github.com/libgdx/gdx-liftoff).

This project was generated with a template including simple application launchers and an `ApplicationAdapter` extension that draws libGDX logo.

## Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3; was called 'desktop' in older docs.

## Gradle

This project uses [Gradle](https://gradle.org/) to manage dependencies.
The Gradle wrapper was included, so you can run Gradle tasks using `gradlew.bat` or `./gradlew` commands.
Useful Gradle tasks and flags:

- `--continue`: when using this flag, errors will not stop the tasks from running.
- `--daemon`: thanks to this flag, Gradle daemon will be used to run chosen tasks.
- `--offline`: when using this flag, cached dependency archives will be used.
- `--refresh-dependencies`: this flag forces validation of all dependencies. Useful for snapshot versions.
- `build`: builds sources and archives of every project.
- `cleanEclipse`: removes Eclipse project data.
- `cleanIdea`: removes IntelliJ project data.
- `clean`: removes `build` folders, which store compiled classes and built archives.
- `eclipse`: generates Eclipse project data.
- `idea`: generates IntelliJ project data.
- `lwjgl3:jar`: builds application's runnable jar, which can be found at `lwjgl3/build/libs`.
- `lwjgl3:run`: starts the application.
- `test`: runs unit tests (if any).

Note that most tasks that are not specific to a single project can be run with `name:` prefix, where the `name` should be replaced with the ID of a specific project.
For example, `core:clean` removes `build` folder only from the `core` project.

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
        "effets":      [ [degatAdverse, valeur], [degatBanc, valeur], [soinJeu, valeur], [soinBanc, valeur], [voleCredit, valeur], [pioche, valeur]]
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
        "effets":      [ [soinJeu, valeur], [soinBanc, valeur],["ChangementT", "Tropical"], [pioche, valeur],[bloquerOutilA, nbrTour], [echangeBanc, -], [echangeBancRandom, -]  ],
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
    ChangementT        | nomTerrain| Change le terrain actif
    

    Notes :
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


6. REGLES : 
 
    ## 1. Construction du deck
    
    Chaque joueur doit construire un deck contenant exactement **60 cartes**.
    
    Le deck doit respecter les contraintes suivantes :
    
    * Il doit contenir **au moins 5 carte Pays** dont le coût est inférieur à **200 crédits**.
    * Il doit contenir **10 carte Outil** et **10 carte Action**
  
    
    ## 2. Conditions de victoire
    
    Il existe deux façons de gagner une partie :
    
    ### Victoire aux points
    
    Un joueur gagne immédiatement lorsqu’il atteint **6 points de victoire** en éliminant des pays adverses.
    
    ### Victoire par forfait
    
    Un joueur remporte aussi la partie si son adversaire n’est plus capable de placer une carte Pays en jeu.
    
    
    ## 3. Les nations et le système de combat
    
    ### Attaques de base
    
    Chaque nation possède plusieurs statistiques offensives :
    
    * Puissance
    * Technologie
    * Ressource
    * Stabilité
    
    Lorsqu’une attaque de base est utilisée, le jeu compare la statistique choisie entre la nation attaquante et la nation adverse.
    Les dégâts infligés correspondent à la différence entre ces deux valeurs.
    
    ### Attaques spéciales

    Chaque pays dispose également d’attaques spéciales uniques.

    * infliger des dégâts au jeu et/ou à une carte du banc,
    * soigner en jeu et/ou à une carte du banc,
    * voler des crédits au joueur adverse 
    * piocher des cartes dans ton deck
    
    
    ## 4. Rang des nations et points de victoire
    
    Chaque nation possède un rang qui détermine le nombre de points accordés à l’adversaire lorsqu’elle est éliminée.
    
    ### Tableau des rangs
    
    * **Marginal** : rapporte 1 point
    * **Émergent** : rapporte 2 points
    * **Établi** : rapporte 3 points
    * **Dominant** : rapporte 4 points
    * **Hégémonie** : rapporte 5 points
    
    En règle générale :
    
    * les nations de rang élevé possèdent de meilleures statistiques,
    * mais elles donnent davantage de points lorsqu’elles sont vaincues.
    
    ---
    
    ## 5. Types de nations
    
    Chaque nation appartient à une catégorie stratégique parmi les suivantes :
    
    * Économique
    * Renseignement
    * Isolationniste
    * Militaire
    * Diplomatique
    
    Le type d’une nation peut interagir avec certains effets de cartes ou avec le terrain.
    
    ---
    
    ## 6. Système d’économie et crédits
    
    Chaque pays possède une statistique d’**Économie** qui génère des crédits au début de chaque tour.
    
    Le montant obtenu dépend de l’emplacement de la carte :
    
    * **Dans la main** : aucun revenu (0 %)
    * **Sur le banc** : 20 % de la statistique d’Économie
    * **Sur le poste actif** : 100 % de la statistique d’Économie
    
    ### Utilisation des crédits
    
    Les crédits servent notamment à :
    
    * poser des cartes sur le terrain,
    * placer des cartes sur le banc,
    * effectuer une retraite,
    * payer un effet spécial.
    
    ### Retraite
    
    Battre en retraite consiste à échanger la nation située sur le poste actif avec une nation du banc, en payant le coût requis.
    
    ---
    
    ## 7. Les cartes Outil et Action
    
    ### Cartes Outil
    
    Les cartes Outil peuvent être attachées à une nation :
    
    * sur le poste actif,
    * ou sur le banc.
    
    Elles augmentent généralement une ou plusieurs statistiques du pays équipé ou débuff le pays adverse. Les cartes outils ne font effet que quand elle rentre en jeu.
    
    ### Cartes Action
    
    Les cartes Action sont des consommables à effet immédiat.
    Elles permettent par exemple :
    
    * de piocher des cartes,
    * de soigner des nations en jeu ou sur le banc,
    * de modifier le terrain,
    * de bloquer les outils adverse pendant X tours,
    * de faire des echange gratuit, aléatoire ou pas en fonction de la carte.
    
    ---
    
    ## 8. Le terrain
    
    Au début d’une partie, le terrain actif est de type **Tempéré**.
    
    Le terrain reste identique pendant toute la partie sauf si une carte Action modifie son type.
    
    Chaque terrain applique des effets particuliers selon le type des nations présentes sur le terrain.
    Ces modificateurs s’appliquent temporairement aux statistiques de combat tant que le terrain est actif.
    
    ### Table des effets 

    | Terrain      | Militaire       | Économique       | Diplomatique     | Renseignement   | Isolationniste  | Technologique    |
    |--------------|-----------------|------------------|------------------|-----------------|-----------------|------------------|
    | Tempéré      | —               | —                | —                | —               | —               | —                |
    | Désertique   | +10 puissance   | −10 ressources   | −10 stabilité    | +10 technologie | +10 stabilité   | −10 économie     |
    | Tropical     | −10 puissance   | +15 ressources   | +10 stabilité    | +10 puissance   | +10 stabilité   | −10 technologie  |
    | Montagneux   | +15 puissance   | −10 économie     | −10 économie     | +10 puissance   | +15 stabilité   | +10 technologie  |
    | Glacial      | −10 stabilité   | −10 ressources   | −15 économie     | −10 puissance   | +15 stabilité   | +15 technologie  |
    | Océanique    | +10 puissance   | +15 économie     | +10 économie     | +10 puissance   | −15 stabilité   | +10 technologie  |
