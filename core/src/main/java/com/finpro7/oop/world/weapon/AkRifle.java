package com.finpro7.oop.world.weapon;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.finpro7.oop.Main;

public class AkRifle extends Firearm {

    // helper buat nyimpen data awal model 3d
    public static class Template {
        public ModelInstance model;
        public Vector3 muzzlePoint;
        public Template(ModelInstance m, Vector3 muz) {
            this.model = m;
            this.muzzlePoint = muz;
        }
    }

    Array<String> meshNames = new Array<>(16);

    public AkRifle(Object player) {
        super(player, Main.autoRifleTemplate);
        this.name = "Assault Rifle";
        this.maxAmmoInClip = 20;
        this.ammoInClip = 20;
        this.totalAmmo = 400;
        this.damage = 3f;
        this.meshNames.add("ak");

        this.aimSightY = -0.4f;
        this.aimSightZ = -0.5f;
    }

    // bikin ak standar tanpa modifikasi
    public static AkRifle generateDefault() {
        AkRifle weapon = new AkRifle(null);
        weapon.updateModel();
        return weapon;
    }

    // rakit modelnya dari potongan mesh yang ada
    @Override
    public void updateModel() {
        if (Main.weaponsModel != null) {
            viewModel = new ModelInstance(Main.weaponsModel, meshNames);
        }
    }
}
