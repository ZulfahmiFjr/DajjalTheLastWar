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
        mod(firearm); // Ubah statistik senjatanya
        firearm.mods.add(hashName);
    }
    // Detail apa yang diubah (damage, akurasi, dll) diatur di sini
    abstract void mod(Firearm firearm);
}
