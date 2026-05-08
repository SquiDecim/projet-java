package io.github.squidecim.genialtcg.model;

import com.badlogic.gdx.graphics.Texture;

public class CardData {

    // Informations de base
    public String country; // Correspond à "nom" dans le JSON
    public String id; // Correspond à "id"
    public String rank; // Correspond à "rang"
    public String type; // Correspond à "type"
    public int cost; // Correspond à "cout"
    public int pv; // Correspond à "etat"
    public int revocation; //Correspond à "retrait"

    // Statistiques converties en tableau int[]
    // Ordre : [puissance, economie, ressources, technologie, stabilite]
    public int[] stats;

    // Effets du coup spécial (pays uniquement — format [type, valeur])
    public int      specialCost;
    public String[] specialEffectTypes;
    public int[]    specialEffectValues;

    // Effets des cartes actions/outils (ancien format cibles/variables/valeurs)
    public String[] specialTargets;
    public String[] specialVariables;
    public Object[] specialValues;

    // Condition d'activation (actions/outils uniquement) — affichage
    public String cond;

    // Conditions structurées (évaluation en jeu)
    public String[] condTypes;       // votre pays actif doit être de ce type
    public String[] condTerrains;    // le terrain actif doit être ce terrain
    public String[] condRangs;       // votre pays actif doit avoir ce rang
    public int condEtatMin;          // PV minimum requis (0 = ignoré)
    public int condEtatMax;          // PV maximum requis (0 = ignoré)
    public String condStatMinKey;    // stat à vérifier (min), null = ignoré
    public int condStatMinVal;       // seuil minimum de la stat
    public String condStatMaxKey;    // stat à vérifier (max), null = ignoré
    public int condStatMaxVal;       // seuil maximum de la stat
    public String specialName;
    public String specialDescription;

    public Texture texture;

    public CardData(
        String country,
        String id,
        String rank,
        String type,
        int cost,
        int pv,
        int[] stats,
        int specialCout,
        String specialNom,
        String specialDescription
    ) {
        this.country = country;
        this.id = id;
        this.rank = rank;
        this.type = type;
        this.cost = cost;
        this.pv = pv;
        this.stats = stats;
        this.specialCost = specialCout;
        this.specialName = specialNom;
        this.specialDescription = specialDescription;
        this.revocation = cost/2;
    }

    public String getAtlasRegionName() {
        if (id != null && (id.startsWith("ACT-") || id.startsWith("OUT-"))) {
            return id + "_" + country.toLowerCase().replace(" ", "_");
        }
        return country.replace(" ", "_");
    }

    @Override
    public String toString() {
        if (id != null && (id.startsWith("ACT-") || id.startsWith("OUT-"))) {
            return String.format(
                "Nom       : %s\n" +
                "\tCondition : %s\n" +
                "\tCibles    : %s\n" +
                "\tVariables : %s\n" +
                "\tValeurs   : %s",
                country,
                (cond != null && !cond.equals("—")) ? cond : "—",
                java.util.Arrays.toString(specialTargets),
                java.util.Arrays.toString(specialVariables),
                formatValeurs(specialValues)
            );
        }
        return String.format(
            "Nom  : %s\n" +
            "\tRang : %s | Type : %s\n" +
            "\tCoût : %d  | État : %d\n" +
            "\tStatistiques :\n" +
            "\t\tpuissance   : %d\n" +
            "\t\téconomie    : %d\n" +
            "\t\tressources  : %d\n" +
            "\t\ttechnologie : %d\n" +
            "\t\tstabilité   : %d\n" +
            "\tSpécial :\n" +
            "\tNom spécial : %s\n" +
            "\tDescription : %s\n" +
            "\t\tcoût   : %d\n" +
            "\t\teffets : %s",

            country, rank, type, cost, pv,
            stats != null && stats.length > 0 ? stats[0] : 0,
            stats != null && stats.length > 1 ? stats[1] : 0,
            stats != null && stats.length > 2 ? stats[2] : 0,
            stats != null && stats.length > 3 ? stats[3] : 0,
            stats != null && stats.length > 4 ? stats[4] : 0,
            specialName,
            specialDescription,
            specialCost,
            formatEffets(specialEffectTypes, specialEffectValues)
        );
    }

    private static String formatEffets(String[] types, int[] values) {
        if (types == null || types.length == 0) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < types.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append("[").append(types[i]).append(", ").append(values[i]).append("]");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String formatValeurs(Object[] vals) {
        if (vals == null || vals.length == 0) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vals.length; i++) {
            if (i > 0) sb.append(", ");
            if (vals[i] instanceof String[]) {
                sb.append(java.util.Arrays.toString((String[]) vals[i]));
            } else {
                sb.append(vals[i]);
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
