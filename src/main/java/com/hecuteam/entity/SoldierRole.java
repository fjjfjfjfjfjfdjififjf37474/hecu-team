package com.hecuteam.entity;

public enum SoldierRole {
    SHOTGUNNER("shotgunner", 8, 7.0, 1.25),
    MACHINE_GUNNER("machine_gunner", 50, 12.0, 1.35),
    STORMTROOPER("stormtrooper", 50, 10.0, 1.3),
    SQUAD_LEADER("squad_leader", 8, 8.0, 1.2),
    MEDIC("medic", 30, 7.0, 1.2);
    public final String id;
    public final int magazine;
    public final double range;
    public final double damage;
    SoldierRole(String id, int magazine, double range, double damage) {
        this.id = id; this.magazine = magazine; this.range = range; this.damage = damage;
    }
}
