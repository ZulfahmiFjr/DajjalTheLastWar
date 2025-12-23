package com.finpro7.oop.world.weapon;

import com.badlogic.gdx.Gdx; // perlu import ini buat cek input
import com.badlogic.gdx.Input; // ini juga
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public abstract class Firearm {
    public String name = "Firearm";
    public Array<String> mods = new Array<>();
    public ModelInstance viewModel;
    public Vector3 muzzlePoint = new Vector3();

    public float damage = 5.0f;
    public float knockback = 2f;
    public float reloadProgress = 0.0f;
    public boolean canScope = false;

    public float currentReloadTime = 0f;
    public float totalReloadDuration = 2.0f;

    public int ammoInClip;
    public int maxAmmoInClip;
    public int totalAmmo;
    public boolean isReloading = false;

    public float recoverySpeed = 8.0f;
    public float reloadSpeed = 1.0f;
    public float spread = 0.02f;

    public float scaleX = 1.0f, scaleY = 1.0f, scaleZ = 1.0f;
    public float aimSightRatio = 0.0f; // 0 = pinggang, 1 = depan mata
    public float aimSightY = -.4f;
    public float aimSightZ = -.75f;

    public float aimSightFov = 67f;

    public float recoveryTranslateZ = 0.125f;
    public float recoveryPitch = 10f;
    public float recoveryRoll = 20f;

    public float noAutoWaitTime = 0f;

    public Firearm(Object placeholder, AkRifle.Template template) {
        if (template != null) {
            this.viewModel = template.model.copy();
            this.muzzlePoint.set(template.muzzlePoint);
        }
    }

    public void updateModel() { }

    public void update(float delta) {
        // logika reload
        if (isReloading) {
            currentReloadTime += delta;
            if (currentReloadTime >= totalReloadDuration) {
                finishReload();
            }
        }

        // cek apakah tombol klik kanan ditahan dan senjata yg digunakan bisa scope
        boolean isRightClick = Gdx.input.isButtonPressed(Input.Buttons.RIGHT) && canScope;

        // kalo lagi reload, paksa aimnya lepas biar gak aneh
        if (isReloading || !isRightClick) {
            // kurangi ratio pelan-pelan, 5f kecepatan transisinya
            aimSightRatio = Math.max(0f, aimSightRatio - delta * 5f);
        } else {
            // nambah ratio pelan pelan, gerak ke depan mata
            aimSightRatio = Math.min(1f, aimSightRatio + delta * 5f);
        }
    }

    // method setView
    public void setView(Camera camera) {
        if (viewModel != null) {
            float delta = Gdx.graphics.getDeltaTime();
            // pake interpolasi pow2 biar gerakannya enak, cepet di awal, lambat di akhir
            final float ratio = Interpolation.pow2.apply(aimSightRatio);

            final float tx = MathUtils.lerp(0.45f, 0f, ratio);
            final float ty = MathUtils.lerp(-0.5f, aimSightY, ratio);
            final float tz = MathUtils.lerp(aimSightZ, aimSightZ, ratio) + recoveryTranslateZ;

            // logika zoom kamera (fov)
            if (camera instanceof PerspectiveCamera) {
                PerspectiveCamera pc = (PerspectiveCamera) camera;
                // kalo aimSightRatio naik, fov bakal turun ke aimSightFov (zoom in)
                pc.fieldOfView = MathUtils.lerp(67f, aimSightFov, ratio);
                pc.update();
            }

            viewModel.transform.set(camera.view).inv();

            // animasi reload & recoil
            if (isReloading) {
                float progress = currentReloadTime / totalReloadDuration;
                float progressCos = (1.0f - (float) Math.cos(progress * MathUtils.PI * 2.0f)) * 0.5f;
                float mundurReload = progressCos * -0.5f;
                float speedMultiplier = 720f;
                float putaran = (MathUtils.cos(progress * MathUtils.PI) * 0.5f - 0.5f) * speedMultiplier;

                viewModel.transform
                    .translate(tx, ty, tz + mundurReload)
                    .rotate(Vector3.X, putaran)
                    .scale(scaleX, scaleY, scaleZ);
            } else {
                viewModel.transform
                    .translate(tx, ty, tz)
                    .rotate(Vector3.X, recoveryPitch)
                    .scale(scaleX, scaleY, scaleZ);
            }

            recoveryTranslateZ = MathUtils.lerp(recoveryTranslateZ, 0, delta * recoverySpeed);
            recoveryPitch = MathUtils.lerp(recoveryPitch, 0, delta * recoverySpeed);
        }
    }

    public void shoot() {
        if (isReloading) return;

        if (ammoInClip <= 0) {
            if (totalAmmo > 0) {
                reload();
            }
            return;
        }

        ammoInClip--;
        recoveryTranslateZ = 0.15f;
        recoveryPitch = 10f;
    }

    public void reload() {
        if (isReloading || ammoInClip == maxAmmoInClip || totalAmmo <= 0) return;

        isReloading = true;
        currentReloadTime = 0f;
    }

    private void finishReload() {
        int butuh = maxAmmoInClip - ammoInClip;
        if (totalAmmo >= butuh) {
            totalAmmo -= butuh;
            ammoInClip = maxAmmoInClip;
        } else {
            ammoInClip += totalAmmo;
            totalAmmo = 0;
        }
        isReloading = false;
        currentReloadTime = 0f;
    }
}
