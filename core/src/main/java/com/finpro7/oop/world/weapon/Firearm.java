package com.finpro7.oop.world.weapon;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.finpro7.oop.Main;

public abstract class Firearm {
    public String name = "Firearm";
    public Array<String> mods = new Array<>();
    public ModelInstance viewModel;
    public Vector3 muzzlePoint = new Vector3();

    public float damage = 5.0f;
    public float knockback = 2f;
    public float knockForward = -.375f;

    public int ammoInClip = 30;
    public int maxAmmoInClip = 30;
    public int clips = 3;
    public float recoverySpeed = 8.0f;
    public float reloadSpeed = 1.0f;
    public float spread = 0.02f;

    // Untuk animasi & posisi
    public float scaleX = 1.0f, scaleY = 1.0f, scaleZ = 1.0f;
    public float aimSightRatio = 0.0f;
    public float aimSightFov = 70f;
    public float aimSightY = -.4f;
    public float aimSightZ = -.75f;
    public float recoveryTranslateZ = 0.125f;
    public float recoveryPitch = 10f;
    public float recoveryRoll = 20f;

    // Status Senjata
    public boolean isAuto = true;
    public int burstCount = 1;
    public float noAutoWaitTime = 0f;
    public float soundPitchBase = 1.0f;
    public float soundVolume = 1.0f;

    public Firearm(Object placeholder, AkRifle.Template template) {
        if (template != null) {
            this.viewModel = template.model.copy();
            this.muzzlePoint.set(template.muzzlePoint);
        }
    }

    public void updateModel() {

    }

    public void update(float delta) {

    }

    // Mengatur posisi senjata agar mengikuti kamera (First Person View)
    public void setView(Camera camera) {
        // Di dalam method setView(Camera camera)
        if (viewModel != null) {
            float delta = com.badlogic.gdx.Gdx.graphics.getDeltaTime(); // Ambil waktu antar frame
            final float ratio = Interpolation.pow2.apply(aimSightRatio);

            // Sesuaikan posisi agar lebih pas di layar
            final float tx = MathUtils.lerp(0.45f, 0f, ratio);
            final float ty = MathUtils.lerp(-0.5f, aimSightY, ratio);
            // tz ditambah recoveryTranslateZ agar senjata mundur saat menembak
            final float tz = MathUtils.lerp(aimSightZ, aimSightZ, ratio) + recoveryTranslateZ;

            float finalScaleX = scaleX;
            float finalScaleY = scaleY;
            float finalScaleZ = scaleZ;
            // Bikin model 3D ngikutin gerakan kamera secara real-time
            viewModel.transform.set(camera.view).inv();

            viewModel.transform
                .translate(tx, ty, tz)   // Geser ke kanan/bawah biar gak nutupin mata
                .rotate(Vector3.X, recoveryPitch) // rotasi ke atas saat menembak
                .scale(finalScaleX, finalScaleY, finalScaleZ);

            // Logika pemulihan (recovery) agar senjata balik lagi ke posisi awal
            recoveryTranslateZ = MathUtils.lerp(recoveryTranslateZ, 0, delta * recoverySpeed);
            recoveryPitch = MathUtils.lerp(recoveryPitch, 0, delta * recoverySpeed);
        }
    }

    public void shoot() {
        // Fungsi saat tombol tembak ditekan
        recoveryTranslateZ = 0.15f;
        recoveryPitch = 10f;
    }
}
