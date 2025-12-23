package com.finpro7.oop.world.weapon;

public abstract class WeaponMod {
    public float weight;
    public String hashName;
    public boolean once;

    public WeaponMod(String hashName, float weight, boolean once) {
        this.hashName = hashName;
        this.weight = weight;
        this.once = once;
    }

    public void applyMod(Firearm firearm) {
        mod(firearm); // nerapin perubahan status senjatanya
        firearm.mods.add(hashName);
    }

    // logic detail perubahannya misal damage nambah, akurasi turun, ditulis di sini
    abstract void mod(Firearm firearm);
}
