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
        public Template(ModelInstance m, Vector3 muz) {
            this.model = m;
            this.muzzlePoint = muz;
        }
    }

    Array<String> meshNames = new Array<>(16);
    String magName = "default-mag";

    public AkRifle(Object player) {
        // Memanggil constructor Firearm
        super(player, com.finpro7.oop.Main.autoRifleTemplate);
        this.name = "Assault Rifle";
        this.maxAmmoInClip = 20;  // isi magazine 20
        this.ammoInClip = 20;     // mulai penuh
        this.totalAmmo = 400;      // cadangan peluru 400
        this.damage = 3f;
        this.meshNames.add("ak");

        this.aimSightY = -0.4f; // Mengatur tinggi
        this.aimSightZ = -0.5f;  // Mengatur Mundur/Maju
    }

    // Fungsi buat bikin AK standar
    public static AkRifle generateDefault() {
        AkRifle weapon = new AkRifle(null);
        weapon.updateModel();
        return weapon;
    }

    // Rakit model 3D berdasarkan meshNames yang di-add tadi
    @Override
    public void updateModel() {
        if (Main.weaponsModel != null) {
            viewModel = new ModelInstance(Main.weaponsModel, meshNames);
        }
    }
}
