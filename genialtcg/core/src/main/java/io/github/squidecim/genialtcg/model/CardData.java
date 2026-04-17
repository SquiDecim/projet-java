package io.github.squidecim.genialtcg.model;

import com.badlogic.gdx.graphics.Texture;

public class CardData {

    public String country;
    public String id;
    public String rank;
    public String type;
    public int cost;
    public int pv;
    public int[] stats;
    public Texture texture;

    public CardData(
        String country,
        String id,
        String rank,
        String type,
        int cost,
        int pv,
        int[] stats
    ) {
        this.country = country;
        this.id = id;
        this.rank = rank;
        this.type = type;
        this.cost = cost;
        this.pv = pv;
        this.stats = stats;
        this.texture = new Texture("cards/frontCardTexture.jpg");
    }

    @Override
    public String toString() {
        return String.format(
            "Country: %s, id: %s, rank: %s, type: %s, cost: %d, pv: %d, statistics: XXXX",
            this.country,
            this.id,
            this.rank,
            this.type,
            this.cost,
            this.pv
        ); //statistics à compléter
    }
}
