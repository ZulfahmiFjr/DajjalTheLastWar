package com.finpro7.oop.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Pool;

// implement poolable biar bisa direcycle
public class Particle implements Pool.Poolable {

    public ModelInstance instance;
    public boolean active = false;

    // vektor gerak partikelnya
    private Vector3 velocity = new Vector3();
    private float lifeTime = 0f;
    private float maxLife = 1.0f; // partikel cuma idup 1 detik

    public Particle(Model model) {
        // kita bikin model kotak kecil warna merah
        instance = new ModelInstance(model);

        // set warna merah darah
        Material mat = instance.materials.get(0);
        mat.set(ColorAttribute.createDiffuse(Color.RED));

        // kecilin ukurannya
        float s = 0.15f;
        instance.transform.scale(s, s, s);
    }

    // method buat inisialisasi ulang pas diambil dari pool (kyak reinkarnasi)
    public void init(float x, float y, float z) {
        active = true;
        lifeTime = maxLife;

        // reset posisi
        instance.transform.setToTranslation(x, y, z);
        float s = 0.15f;
        instance.transform.scale(s, s, s);

        // kasih ledakan arah random biar muncrat
        velocity.set(
            MathUtils.random(-3f, 3f),
            MathUtils.random(2f, 5f), // agak ke atas dikit
            MathUtils.random(-3f, 3f)
        );
    }

    public void update(float delta) {
        if (!active) return;

        // gerakin partikel
        float posX = instance.transform.getTranslation(new Vector3()).x;
        float posY = instance.transform.getTranslation(new Vector3()).y;
        float posZ = instance.transform.getTranslation(new Vector3()).z;

        // gravitasi biar jatoh
        velocity.y -= 15f * delta;

        instance.transform.setTranslation(
            posX + velocity.x * delta,
            posY + velocity.y * delta,
            posZ + velocity.z * delta
        );

        // kurangi umur partikel
        lifeTime -= delta;
        if (lifeTime <= 0) {
            active = false; // mati, siap dikembaliin ke pool
        }
    }

    @Override
    public void reset() {
        // method wajib dari interface poolable buat bersihin data lama
        active = false;
        lifeTime = 0;
        velocity.set(0,0,0);
        // balikin posisi ke 0 biar aman
        instance.transform.idt();
    }
}
