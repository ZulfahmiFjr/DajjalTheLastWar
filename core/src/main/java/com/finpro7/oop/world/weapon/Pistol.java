package com.finpro7.oop.world.weapon;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.utils.Array;
import com.finpro7.oop.Main;

public class Pistol extends Firearm {
    // buat nyimpen bagian-bagian model pistol
    Array<String> meshNames = new Array<>(16);

    public Pistol(Object player) {
        super(player, com.finpro7.oop.Main.autoRifleTemplate);

        // set spek dasar pistolnya
        this.name = "Pistol";
        this.scaleX = 1.0f;
        this.scaleY = 1.0f;
        this.scaleZ = 1.0f;
        this.aimSightY = -0.5f;
        this.aimSightZ = -0.7f;
        this.ammoInClip = 10; // peluru dikit khas pistol
        this.maxAmmoInClip = 10;
        this.totalAmmo = 250;
        this.reloadSpeed = 2.0f; // reload agak santai
        this.damage = 6f; // damage per peluru lebih gede dari rifle buat balancing

        // set efek visual pas nembak (animasi rekoil)
        this.recoveryTranslateZ = 0.125f; // mundurin pistol dikit pas nembak
        this.recoveryRoll = 20f; // agak miring dikit pas hentakan
        this.recoveryPitch = 30f; // moncong naik ke atas
        this.recoverySpeed = 4.0f; // kecepatan balik ke posisi normal
        this.knockback = 2f; // dorongan ke musuh
        this.meshNames.clear();
        this.meshNames.add("pistol");
        this.mods.add("Pistol");
    }

    // fungsi buat nge-generate pistol dengan modifikasi acak
    public static Pistol generateDefault() {
        Array<WeaponMod> modsToApply = new Array<>();
        return generateFrom(modsToApply);
    }

    // fungsi pembantu buat nerapin daftar mod ke objek pistol baru
    public static Pistol generateFrom(Array<WeaponMod> modsToApply) {
        Pistol pistol = new Pistol(null);
        for (WeaponMod mod : modsToApply) {
            if (mod != null) mod.applyMod(pistol);
        }
        pistol.updateModel(); // rakit model 3D-nya setelah semua mod kepasang
        return pistol;
    }

    public void updateModel() {
        if (Main.weaponsModel != null) {
            viewModel = new ModelInstance(Main.weaponsModel, meshNames);
        }
    }

    // modifikasi bagian magazine
    static WeaponMod[] magMods = {
        new WeaponMod("ExtendedMag", 100f, true) {
            @Override
            void mod(Firearm weapon) {
                weapon.name = "Extended " + weapon.name;
                weapon.maxAmmoInClip += 5; // nambah 5 peluru
                weapon.ammoInClip += 5;
                weapon.reloadSpeed *= .85f; // reload jadi rada lambat dikit karna mag panjang
                ((Pistol)weapon).meshNames.add("pistol-ext-mag"); // pasang mesh mag panjang
            }
        },
        new WeaponMod("DrumMag", 100f, true) {
            @Override
            void mod(Firearm weapon) {
                weapon.name = "Drum-Mag " + weapon.name;
                weapon.maxAmmoInClip = 20; // jadi banyak banget pelurunya
                weapon.ammoInClip = 20;
                weapon.reloadSpeed *= .5f; // tapi reloadnya jadi berat/lama
                ((Pistol)weapon).meshNames.add("pistol-drum-mag"); // pasang mesh mag bunder
            }
        },
    };

    // modifikasi badan & aksesoris
    static WeaponMod[] weaponMods = {
        new WeaponMod("Zippy", 100f, false) {
            @Override
            void mod(Firearm weapon) {
                weapon.name = "Zippy " + weapon.name;
                weapon.recoverySpeed *= 1.25f; // nembak jadi lebih cepet balik rekoilnya
                weapon.scaleZ *= .5f; // pistolnya jadi kuntet/pendek
                weapon.spread *= 1.7f; // tapi jadi ga akurat
            }
        },
        new WeaponMod("Chunky", 100f, false) {
            @Override
            void mod(Firearm weapon) {
                weapon.name = "Chunky " + weapon.name;
                weapon.damage *= 1.25f; // damage makin sakit
                weapon.scaleX *= 2f; // pistolnya jadi tebel/gemuk
                weapon.recoverySpeed *= .85f; // hentakannya lebih berasa
            }
        },
        new WeaponMod("LongBarrel", 100f, false) {
            @Override
            void mod(Firearm weapon) {
                weapon.name = "Long-barrel " + weapon.name;
                weapon.scaleZ *= 1.5f; // moncong pistol jadi panjang
                weapon.damage *= 1.5f;
                weapon.spread = .0f; // jadi akurat parah kyak sniper
            }
        },
        new WeaponMod("Scoped", 100f, true) {
            @Override
            void mod(Firearm weapon) {
                weapon.name = "Scoped " + weapon.name;
                weapon.aimSightFov = 30f; // zoomnya jadi jauh pas bidik
                weapon.aimSightY = -.315f;
                weapon.aimSightZ = -.5f;
                ((Pistol)weapon).meshNames.add("pistol-scope"); // tambahin model scope di atasnya
            }
        },
    };
}
