package com.finpro7.oop.world.weapon;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.utils.Array;
import com.finpro7.oop.Main;

public class Pistol extends Firearm {

    Array<String> meshNames = new Array<>(16);

    public Pistol(Object player) {
        super(player, Main.autoRifleTemplate);

        // spek dasar pistol regular
        this.name = "Regular Pistol";
        this.scaleX = 1.0f;
        this.scaleY = 1.0f;
        this.scaleZ = 1.0f;
        this.aimSightY = -0.5f;
        this.aimSightZ = -0.7f;
        this.ammoInClip = 10;
        this.maxAmmoInClip = 10;
        this.totalAmmo = 250;
        this.reloadSpeed = 2.0f;
        this.damage = 6f;

        // efek visual
        this.recoveryTranslateZ = 0.125f;
        this.recoveryRoll = 20f;
        this.recoveryPitch = 30f;
        this.recoverySpeed = 4.0f;
        this.knockback = 2f;

        this.meshNames.clear();
        this.meshNames.add("pistol");
        this.mods.add("Pistol");
    }

    // method utama buat bikin varian pistol sesuai id
    // 0=regular, 1=zippy, 2=chunky, 3=long, 4=scoped
    public static Pistol assembleType(int typeId) {
        Array<WeaponMod> myMods = new Array<>();

        switch (typeId) {
            case 1: myMods.add(weaponMods[0]); break; // zippy
            case 2: myMods.add(weaponMods[1]); break; // chunky
            case 3: myMods.add(weaponMods[2]); break; // long
            case 4: myMods.add(weaponMods[3]); break; // scoped
            default: break; // 0 regular gak pake mod
        }

        return generateFrom(myMods);
    }

    public static Pistol generateFrom(Array<WeaponMod> modsToApply) {
        Pistol pistol = new Pistol(null);
        for (WeaponMod mod : modsToApply) {
            if (mod != null) mod.applyMod(pistol);
        }
        pistol.updateModel();
        return pistol;
    }

    public void updateModel() {
        if (Main.weaponsModel != null) {
            viewModel = new ModelInstance(Main.weaponsModel, meshNames);
        }
    }

    // daftar varian pistol (mod bodi)
    public static WeaponMod[] weaponMods = {
        new WeaponMod("Zippy", 100f, false) {
            @Override
            void mod(Firearm weapon) {
                weapon.name = "Zippy Pistol";
                weapon.recoverySpeed *= 1.25f;
                weapon.scaleZ *= .5f;
                weapon.spread *= 1.7f;
            }
        },
        new WeaponMod("Chunky", 100f, false) {
            @Override
            void mod(Firearm weapon) {
                weapon.name = "Chunky Pistol";
                weapon.damage *= 1.25f;
                weapon.scaleX *= 2f;
                weapon.recoverySpeed *= .85f;
            }
        },
        new WeaponMod("LongBarrel", 100f, false) {
            @Override
            void mod(Firearm weapon) {
                weapon.name = "Long-Barrel Pistol";
                weapon.scaleZ *= 1.5f;
                weapon.damage *= 1.5f;
                weapon.spread = .0f;
            }
        },
        new WeaponMod("Scoped", 100f, true) {
            @Override
            void mod(Firearm weapon) {
                weapon.name = "Scoped Pistol";
                weapon.canScope = true;
                weapon.aimSightFov = 30f;
                weapon.aimSightY = -.315f;
                weapon.aimSightZ = -.5f;
                ((Pistol)weapon).meshNames.add("pistol-scope");
            }
        },
    };
}
