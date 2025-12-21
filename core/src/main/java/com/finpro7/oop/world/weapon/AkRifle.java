package com.finpro7.oop.world.weapon;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.finpro7.oop.Main;

public class AkRifle extends Firearm {

    // Helper class untuk menampung data awal senjata
    public static class Template {
        public ModelInstance model;
        public Vector3 muzzlePoint;
        public Sound shootSound;
        public Sound reloadSound;
        public Template(ModelInstance m, Vector3 muz, Sound s, Sound r) {
            this.model = m;
            this.muzzlePoint = muz;
            this.shootSound = s;
            this.reloadSound = r;
        }
    }

    Array<String> meshNames = new Array<>(16);
    String magName = "default-mag";

    public AkRifle(Object player) {
        // Memanggil constructor Firearm
        super(player, com.finpro7.oop.Main.autoRifleTemplate);
        this.name = "Assault Rifle";
        this.meshNames.add("ak"); // Nama mesh dasar di file 3D

        this.aimSightY = -0.4f; // Mengatur tinggi (semakin kecil/minus semakin turun)
        this.aimSightZ = -0.5f;  // Mengatur Mundur/Maju
    }

    public static AkRifle generateDefault() {
        Array<WeaponMod> modsToApply = new Array<>();
        return generateFrom(false, modsToApply);
    }

    public static AkRifle generateFrom(boolean isM4, Array<WeaponMod> modsToApply) {
        AkRifle weapon = new AkRifle(null);
        if (isM4) {
            weapon.meshNames.set(0, "M4A1"); // Ganti model dasar ke M4
            weapon.mods.add("M4A1");
        } else {
            weapon.spread *= 2f;
            weapon.mods.add("AK");
        }

        for (WeaponMod mod : modsToApply) {
            if (mod != null) mod.applyMod(weapon);
        }

        weapon.updateModel();
        return weapon;
    }

    public void updateModel() {
        if (Main.weaponsModel != null) {
            // Hanya memunculkan bagian "ak" saja (atau "M4A1" sesuai pilihan random)
            viewModel = new ModelInstance(Main.weaponsModel, meshNames);
        }
    }

    // DAFTAR MODIFIKASI

    static WeaponMod[] stockMods = {
        new WeaponMod("Woodstock", 100f, true) {
            @Override
            void mod(Firearm weapon) {
                weapon.knockback = 1.5f;
                ((AkRifle)weapon).meshNames.add(((AkRifle)weapon).meshNames.get(0) + "-woodstock");
            }
        },
        new WeaponMod("NormalStock", 100f, true) {
            @Override
            void mod(Firearm weapon) {
                weapon.knockback = 1f;
                ((AkRifle)weapon).meshNames.add(((AkRifle)weapon).meshNames.get(0) + "-stock");
            }
        },
    };

    static WeaponMod[] magMods = {
        new WeaponMod("ExtendedMag", 100f, true) {
            @Override
            void mod(Firearm weapon) {
                weapon.name = "Extended " + weapon.name;
                weapon.maxAmmoInClip += 10;
                weapon.ammoInClip += 10;
                weapon.reloadSpeed *= .85f;
                ((AkRifle)weapon).magName = "ext-mag";
            }
        },
        new WeaponMod("DrumMag", 50f, true) {
            @Override
            void mod(Firearm weapon) {
                weapon.name = "Drum-Mag " + weapon.name;
                weapon.maxAmmoInClip = 90;
                weapon.ammoInClip = 90;
                weapon.reloadSpeed *= .4f;
                ((AkRifle)weapon).magName = "drum-mag";
            }
        },
    };

    static WeaponMod[] weaponMods = {
        new WeaponMod("Zippy", 100f, false) {
            @Override
            void mod(Firearm weapon) {
                weapon.name = "Zippy " + weapon.name;
                weapon.recoverySpeed *= 1.25f;
                weapon.scaleZ *= .5f;
                weapon.spread *= 1.5f;
            }
        },
        new WeaponMod("Chunky", 100f, false) {
            @Override
            void mod(Firearm weapon) {
                weapon.name = "Chunky " + weapon.name;
                weapon.damage *= 1.35f;
                weapon.scaleX *= 2f;
                weapon.recoverySpeed *= .85f;
            }
        },
        new WeaponMod("LongBarrel", 100f, false) {
            @Override
            void mod(Firearm weapon) {
                weapon.name = "Long-barrel " + weapon.name;
                weapon.scaleZ *= 1.5f;
                weapon.damage *= 2f;
                weapon.spread = .0f;
            }
        }
    };
}
