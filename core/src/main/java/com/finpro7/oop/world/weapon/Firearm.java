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
    public float reloadProgress = 0.0f;

    public float currentReloadTime = 0f;
    public float totalReloadDuration = 2.0f;

    public int ammoInClip;      // peluru yang ada di dalam pistol/ak sekarang
    public int maxAmmoInClip;   // kapasitas maksimal magazine (7 atau 20)
    public int totalAmmo;       // cadangan peluru keseluruhan (30 atau 60)
    public boolean isReloading = false; // penanda lagi ganti magazine

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


    public Firearm(Object placeholder, AkRifle.Template template) {
        if (template != null) {
            this.viewModel = template.model.copy();
            this.muzzlePoint.set(template.muzzlePoint);
        }
    }

    public void updateModel() {

    }

    public void update(float delta) {
        if (isReloading) {
            // Jalankan timer reload
            currentReloadTime += delta;

            // Cek apakah reload sudah selesai?
            if (currentReloadTime >= totalReloadDuration) {
                finishReload();
            }
        }
    }

    // Mengatur posisi senjata agar mengikuti kamera (First Person View)
    // --- 3. ANIMASI PUTARAN CEPAT DI SINI ---
    public void setView(Camera camera) {
        if (viewModel != null) {
            float delta = com.badlogic.gdx.Gdx.graphics.getDeltaTime();
            final float ratio = Interpolation.pow2.apply(aimSightRatio);

            final float tx = MathUtils.lerp(0.45f, 0f, ratio);
            final float ty = MathUtils.lerp(-0.5f, aimSightY, ratio);

            // Logika recoil mundur
            final float tz = MathUtils.lerp(aimSightZ, aimSightZ, ratio) + recoveryTranslateZ;

            viewModel.transform.set(camera.view).inv();

            // === BAGIAN UTAMA ANIMASI ===
            if (isReloading) {
                // Hitung progress (0.0 sampai 1.0)
                float progress = currentReloadTime / totalReloadDuration;

                // Rumus Cosine agar gerakan halus (slow in - slow out)
                float progressCos = (1.0f - (float) Math.cos(progress * MathUtils.PI * 2.0f)) * 0.5f;

                // 1. Mundurkan senjata sedikit saat reload
                float mundurReload = progressCos * -0.5f;

                // 2. LOGIKA MUTAR CEPAT
                // Kita ganti 360f menjadi 720f (2 putaran penuh) atau 1080f biar makin ngebut!
                // Angka ini menentukan seberapa banyak dia berputar.
                float speedMultiplier = 720f;
                float putaran = (MathUtils.cos(progress * MathUtils.PI) * 0.5f - 0.5f) * speedMultiplier;

                viewModel.transform
                    .translate(tx, ty, tz + mundurReload) // Posisi + Mundur dikit
                    .rotate(Vector3.X, putaran)           // ROTASI SUMBU X (Gasing)
                    .scale(scaleX, scaleY, scaleZ);
            } else {
                // Posisi Normal (Tidak Reload)
                viewModel.transform
                    .translate(tx, ty, tz)
                    .rotate(Vector3.X, recoveryPitch)     // Rotasi recoil biasa
                    .scale(scaleX, scaleY, scaleZ);
            }

            // Kembalikan recoil ke 0 pelan-pelan
            recoveryTranslateZ = MathUtils.lerp(recoveryTranslateZ, 0, delta * recoverySpeed);
            recoveryPitch = MathUtils.lerp(recoveryPitch, 0, delta * recoverySpeed);
        }
    }

    public void shoot() {
        // Fungsi saat tombol tembak ditekan
        if (isReloading) return; // kalau lagi reload gak bisa nembak
        if (isReloading) {
            System.out.println("Memasukkan peluru...");
            return;
        }

        if (ammoInClip <= 0) {
            System.out.println("Klik! Peluru habis. Tekan R untuk reload!");
            return;
        }

        ammoInClip--;
        recoveryTranslateZ = 0.15f;
        recoveryPitch = 10f;
    }

    public void reload() {
        // Cek syarat reload
        if (isReloading || ammoInClip == maxAmmoInClip || totalAmmo <= 0) return;

        isReloading = true;
        currentReloadTime = 0f; // Reset timer ke 0
        System.out.println("Reloading... (Mulai Animasi)");

        // Timer.schedule DIHAPUS karena kita pakai update(delta)
    }

    // Helper untuk menyelesaikan reload
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
        System.out.println("Reload Selesai!");
    }
}
