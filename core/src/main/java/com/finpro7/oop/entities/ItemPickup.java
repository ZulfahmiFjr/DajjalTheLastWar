package com.finpro7.oop.entities;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

public class ItemPickup {

    // Kita kasih label biar tau ini barang apaan
    public enum Type {
        COIN,
        HEALTH,
        AMMO
    }

    public ModelInstance instance;
    public Vector3 position = new Vector3();
    public Type type;
    public int value; // Nilainya (misal: 10 koin, 20 darah, atau 30 peluru)

    private float originalY;
    private float angle = 0f;
    private float time = 0f;

    public ItemPickup(Model model, float x, float y, float z, Type type, int value) {
        this.instance = new ModelInstance(model);
        this.position.set(x, y, z);
        this.originalY = y;
        this.type = type;
        this.value = value;

        // Sedikit random biar gak sinkron semua muternya (biar natural)
        this.time = MathUtils.random(0f, 10f);
    }

    public void update(float delta) {
        time += delta;
        angle += 100f * delta; // Kecepatan muter

        // Efek naik turun (Bobbing)
        float bobOffset = MathUtils.sin(time * 3f) * 0.25f;

        this.instance.transform.idt();
        this.instance.transform.translate(position.x, originalY + bobOffset, position.z);
        this.instance.transform.rotate(Vector3.Y, angle);

        // Khusus koin biasanya gepeng berdiri, kalo kotak (ammo/health) biarin datar aja
        if (type == Type.COIN) {
            this.instance.transform.rotate(Vector3.Z, 90f);
            this.instance.transform.scale(0.5f, 0.5f, 0.5f);
        } else if (type == Type.AMMO) {
            float s = 0.0015f;
            this.instance.transform.scale(s, s, s);
        } else {
            // Kalo health/ammo ukurannya seseuaiin dikit
//            this.instance.transform.scale(0.4f, 0.4f, 0.4f);
            float s = 0.00035f;
            this.instance.transform.scale(s, s, s);
        }
    }
}
