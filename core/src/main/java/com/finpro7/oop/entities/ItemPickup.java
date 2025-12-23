package com.finpro7.oop.entities;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

public class ItemPickup {

    // label buat nandain ini jenis item apa
    public enum Type {
        COIN,
        HEALTH,
        AMMO
    }

    public ModelInstance instance;
    public Vector3 position = new Vector3();
    public Type type;
    public int value; // isi itemnya misal 10 koin

    private float originalY;
    private float angle = 0f;
    private float time = 0f;

    public ItemPickup(Model model, float x, float y, float z, Type type, int value) {
        this.instance = new ModelInstance(model);
        this.position.set(x, y, z);
        this.originalY = y;
        this.type = type;
        this.value = value;

        // kasih waktu random biar muternya gak barengan semua
        this.time = MathUtils.random(0f, 10f);
    }

    public void update(float delta) {
        time += delta;
        angle += 100f * delta; // kecepatan putaran item

        // efek ngambang naik turun
        float bobOffset = MathUtils.sin(time * 3f) * 0.25f;

        this.instance.transform.idt();
        this.instance.transform.translate(position.x, originalY + bobOffset, position.z);
        this.instance.transform.rotate(Vector3.Y, angle);

        // khusus koin kita puter lagi biar berdiri tegak
        if (type == Type.COIN) {
            this.instance.transform.rotate(Vector3.Z, 90f);
            this.instance.transform.scale(0.5f, 0.5f, 0.5f);
        } else if (type == Type.AMMO) {
            float s = 0.0015f;
            this.instance.transform.scale(s, s, s);
        } else {
            float s = 0.00035f;
            this.instance.transform.scale(s, s, s);
        }
    }
}
