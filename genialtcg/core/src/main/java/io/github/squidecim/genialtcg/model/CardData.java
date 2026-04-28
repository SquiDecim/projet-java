package io.github.squidecim.genialtcg.model;

import com.badlogic.gdx.graphics.Texture;
import java.util.Arrays;

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

    @Override
    public String toString() {
        return String.format(
            "Nom : %s\n" +
                "\tID : [%s]\n" +
                "\tRang : %s\n" +
                "\tType : %s\n" +
                "\tCoût : %d\n" +
                "\tÉtat (PV) : %d\n" +
                "\tStats : %s\n" +
                "\tSpécial : {\n" +
                "\t\tcout : %d,\n" +
                "\t\tcibles : %s,\n" +
                "\t\tvariables : %s,\n" +
                "\t\tvaleurs : %s\n" +
                "\t}",
            country,
            id,
            rank,
            type,
            cost,
            pv,
            java.util.Arrays.toString(stats),
            specialCout,
            java.util.Arrays.toString(specialCibles),
            java.util.Arrays.toString(specialVariables),
            java.util.Arrays.deepToString(specialValeurs)
        );
    }
}
