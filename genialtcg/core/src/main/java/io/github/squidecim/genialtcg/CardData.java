package io.github.squidecim.genialtcg;

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


    public CardData(String country, String id, String rank, String type, int cost, int pv, int[] stats){

        this.country = country;
        this.id = id;
        this.rank = rank;
        this.type = type;
        this.cost = cost;
        this.pv = pv;
        this.stats = stats;
        this.texture = new Texture("frontCardTexture.jpg");

    }

}
