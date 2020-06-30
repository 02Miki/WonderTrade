package com.mcsimonflash.wondertrade.sponge.data;


public enum BroadcastTypes {
    SHINY("Shiny"),
    ULTRABEAST("UltraBeast"),
    LEGENDARY("Legendary"),
    SPECIALTEXTURE("SpecialTexture"),
    CUSTOMTEXTURE("CustomTexture"),
    AURA("Aura");

    public String name;

    BroadcastTypes(String name) {
        this.name = name;
    }

}
