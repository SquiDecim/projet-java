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

    // Statistiques converties en tableau int[]
    // Ordre : [puissance, economie, ressources, technologie, stabilite]
    public int[] stats;

    // Informations Spéciales
    public int specialCout;
    public String[] specialCibles;
    public String[] specialVariables;
    public Object[] specialValeurs; // Object[] car peut contenir des int ou des int[] (ex: [1, "main"])

    // Condition d'activation (actions/outils uniquement)
    public String cond;

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
        String[] specialCibles,
        String[] specialVariables,
        Object[] specialValeurs
    ) {
        this.country = country;
        this.id = id;
        this.rank = rank;
        this.type = type;
        this.cost = cost;
        this.pv = pv;
        this.stats = stats;
        this.specialCout = specialCout;
        this.specialCibles = specialCibles;
        this.specialVariables = specialVariables;
        this.specialValeurs = specialValeurs;
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
                java.util.Arrays.toString(specialCibles),
                java.util.Arrays.toString(specialVariables),
                formatValeurs(specialValeurs)
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
            "\t\tcoût      : %d\n" +
            "\t\tcibles    : %s\n" +
            "\t\tvariables : %s\n" +
            "\t\tvaleurs   : %s",
            country, rank, type, cost, pv,
            stats != null && stats.length > 0 ? stats[0] : 0,
            stats != null && stats.length > 1 ? stats[1] : 0,
            stats != null && stats.length > 2 ? stats[2] : 0,
            stats != null && stats.length > 3 ? stats[3] : 0,
            stats != null && stats.length > 4 ? stats[4] : 0,
            specialCout,
            java.util.Arrays.toString(specialCibles),
            java.util.Arrays.toString(specialVariables),
            formatValeurs(specialValeurs)
        );
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
