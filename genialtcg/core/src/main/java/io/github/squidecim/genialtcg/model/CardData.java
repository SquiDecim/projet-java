package io.github.squidecim.genialtcg.model;

import com.badlogic.gdx.graphics.Texture;
import java.util.Arrays;

public class CardData {

    public String country;
    public String id;
    public String rank;
    public String type;
    public int cost;
    public int pv;
    public int[] stats;

    // Nouveaux champs pour le Spécial
    public int specialCout;
    public String[] specialCibles;
    public String[] specialVariables;
    public int[] specialValeurs;

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
        int[] specialValeurs
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
            "Country: %s, id: %s, rank: %s, type: %s, cost: %d, pv: %d, stats: %s, specialCout: %d, cibles: %s, vars: %s, vals: %s",
            this.country,
            this.id,
            this.rank,
            this.type,
            this.cost,
            this.pv,
            Arrays.toString(this.stats),
            this.specialCout,
            Arrays.toString(this.specialCibles),
            Arrays.toString(this.specialVariables),
            Arrays.toString(this.specialValeurs)
        );
    }
}
