package com.finpro7.oop.entities;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.math.MathUtils;

public class PlayerStats {

    // --- stat player ---
    public float maxHealth = 100f;
    public float health = 100f;
    public float maxStamina = 100f;
    public float stamina = 100f;
    public int currentCoins = 0;

    // --- stamina update pas udah mulai ---
    // ini buat ngurangin stamina pas lari
    public float staminaDrainSprint = 10f; // SHIFT + WASD / pas lari

    // kalo ini buat ngisi lagi staminanya
    public float staminaRegenWalk = 7f; // jalan biasa
    public float staminaRegenIdle = 10f; // diam

    // pake jeda biar gak langsung penuh
    private boolean staminaLocked = false;
    private float staminaRegenDelay = 3f;
    private float staminaRegenTimer = 0f;
    public boolean isSprinting = false;

    public interface DamageListener {
        void onDamageTaken();
    }

    private DamageListener damageListener;

    // konstruktor buat muat data koin pas awal game jalan
    public PlayerStats() {
        Preferences prefs = Gdx.app.getPreferences("UserSession");
        // ambil data koin terakhir, kalo gak ada anggap aja 0
        this.currentCoins = prefs.getInteger("total_coins", 0);
    }

    // update stats pas lagi main
    public void update(float delta, boolean isMoving) {

        boolean shiftPressed = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT);

        if (isMoving && shiftPressed && stamina > 0) {
            // logika lari ngabisin stamina
            isSprinting = true;
            stamina -= staminaDrainSprint * delta;
            staminaRegenTimer = 0f;

            if (stamina <= 0) {
                stamina = 0;
                staminaLocked = true;
            }

        } else {
            isSprinting = false;

            if (!shiftPressed) {
                if (isMoving) {
                    // jalan biasa isinya pelan
                    if (!staminaLocked) {
                        stamina += staminaRegenWalk * delta;
                    }
                } else {
                    // kalo diem isinya ngebut
                    if (staminaLocked) {
                        staminaRegenTimer += delta;
                        if (staminaRegenTimer >= staminaRegenDelay) {
                            staminaLocked = false;
                        }
                    } else {
                        stamina += staminaRegenIdle * delta;
                    }
                }
            }
        }

        // pastiin stamina gak minus atau kelebihan
        stamina = MathUtils.clamp(stamina, 0f, maxStamina);
    }

    // method baru buat nambah koin dan save otomatis
    public void addCoins(int amount) {
        // update di memori
        this.currentCoins += amount;

        // langsung simpen ke lokal biar kalo crash atau offline tetep aman
        Preferences prefs = Gdx.app.getPreferences("UserSession");
        prefs.putInteger("total_coins", this.currentCoins);
        prefs.flush(); // wajib flush biar beneran kesimpen di harddisk/hp
    }

    // method buat masang listener
    public void setListener(DamageListener listener) {
        this.damageListener = listener;
    }

    // bagian damage player
    public void takeDamage(float dmg) {
        health -= dmg;
        if (health < 0) health = 0;
        if (damageListener != null) {
            damageListener.onDamageTaken();
        }
    }

    public boolean isDead() {
        return health <= 0;
    }
}
