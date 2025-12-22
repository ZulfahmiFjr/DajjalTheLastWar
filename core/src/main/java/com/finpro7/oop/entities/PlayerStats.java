package com.finpro7.oop.entities;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;

public class PlayerStats {

    // ================= HEALTH =================
    public float maxHealth = 100f;
    public float health = 100f;

    // ================= STAMINA =================
    public float maxStamina = 100f;
    public float stamina = 100f;

    // ================= STAMINA CONFIG =================

    // drain
    public float staminaDrainSprint = 10f;   // SHIFT + WASD

    // regen
    public float staminaRegenWalk = 7f;      // jalan biasa
    public float staminaRegenIdle = 10f;     // diam

    // Delay system
    private boolean staminaLocked = false;
    private float staminaRegenDelay = 3f;
    private float staminaRegenTimer = 0f;

    public boolean isSprinting = false;

    // ================= UPDATE =================
    public void update(float delta, boolean isMoving) {

        boolean shiftPressed = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT);

        if (isMoving && shiftPressed && stamina > 0) {
            // SPRINT
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
                    // Jalan biasa → regen lambat
                    if (!staminaLocked) {
                        stamina += staminaRegenWalk * delta;
                    }
                } else {
                    // Diam → regen cepat
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

        stamina = MathUtils.clamp(stamina, 0f, maxStamina);
    }

    // ================= DAMAGE =================
    public void takeDamage(float dmg) {
        health -= dmg;
        if (health < 0) health = 0;
    }

    public boolean isDead() {
        return health <= 0;
    }
}
