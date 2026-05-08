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

    // Effets (pays : coup spécial — actions/outils : effets de la carte)
    public int      specialCost;
    public String[] specialEffectTypes;
    public int[]    specialEffectValues;

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

    private static final String SEP = "--------------------------------------------------";

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(SEP).append("\n");

        if (id != null && (id.startsWith("ACT-") || id.startsWith("OUT-"))) {
            sb.append(String.format("%-12s : %s\n", "ID",        id));
            sb.append(String.format("%-12s : %s\n", "Nom",       country));
            sb.append(String.format("%-12s : %s\n", "Condition", cond != null ? cond : "—"));
            sb.append(String.format("%-12s : %s\n", "Effets",    formatEffets(specialEffectTypes, specialEffectValues)));
        } else {
            sb.append(String.format("%-12s : %s\n",  "ID",          id));
            sb.append(String.format("%-12s : %s\n",  "Nom",         country));
            sb.append(String.format("%-12s : %s\n",  "Rang",        rank));
            sb.append(String.format("%-12s : %s\n",  "Type",        type));
            sb.append(String.format("%-12s : %d\n",  "Coût",        cost));
            sb.append(String.format("%-12s : %d\n",  "État",        pv));
            sb.append(String.format("%-12s : %d\n",  "Puissance",   stats != null && stats.length > 0 ? stats[0] : 0));
            sb.append(String.format("%-12s : %d\n",  "Économie",    stats != null && stats.length > 1 ? stats[1] : 0));
            sb.append(String.format("%-12s : %d\n",  "Ressources",  stats != null && stats.length > 2 ? stats[2] : 0));
            sb.append(String.format("%-12s : %d\n",  "Technologie", stats != null && stats.length > 3 ? stats[3] : 0));
            sb.append(String.format("%-12s : %d\n",  "Stabilité",   stats != null && stats.length > 4 ? stats[4] : 0));
            sb.append(String.format("%-12s : %s\n",  "Spécial",     specialName != null ? specialName : "—"));
            sb.append(String.format("%-12s : %s\n",  "Description", specialDescription != null ? specialDescription : "—"));
            sb.append(String.format("%-12s : %d\n",  "Coût spéc.", specialCost));
            sb.append(String.format("%-12s : %s\n",  "Effets",      formatEffets(specialEffectTypes, specialEffectValues)));
        }

        sb.append(SEP);
        return sb.toString();
    }

    private static String formatEffets(String[] types, int[] values) {
        if (types == null || types.length == 0) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < types.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append("[").append(types[i]);
            if (values != null && i < values.length && values[i] != 0)
                sb.append(", ").append(values[i]);
            sb.append("]");
        }
        sb.append("]");
        return sb.toString();
    }
}
