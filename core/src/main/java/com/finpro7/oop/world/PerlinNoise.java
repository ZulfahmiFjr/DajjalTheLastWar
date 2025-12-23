package com.finpro7.oop.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public class PerlinNoise {

    // buat ngatur seberapa rapet gelombang noisenya
    public float frequencyX = 0.125f;
    public float frequencyZ = 0.125f;
    // buat ngatur geseran awalnya
    public float offsetX = 0.0f;
    public float offsetZ = 0.0f;
    public float amplitude = 4.0f; // buat ngatur tinggi gelombangnya

    // array arah vektor buat kombinasi 8 arah mata angin
    private final Vector2[] COMPASS_DIRS = new Vector2[] {
        new Vector2(1, 1),   new Vector2(-1, 1),
        new Vector2(1, -1),  new Vector2(-1, -1),
        new Vector2(1, 0),   new Vector2(-1, 0),
        new Vector2(0, 1),   new Vector2(0, -1),
    };

    // tabel permutasi buat ngacak
    private final int[] HASH_MASK = new int[]{
        0, 3, 2, 7, 2, 1, 6, 3, 7, 4, 5, 1, 5, 6, 4, 0
    };

    // buat milih vektor acak
    private Vector2 getPseudoRandomDir(int x, int y){
        int hx = Math.abs(x) % HASH_MASK.length;
        int hy = Math.abs(y + HASH_MASK[hx]) % HASH_MASK.length;
        return COMPASS_DIRS[HASH_MASK[hy]];
    }

    public float getHeight(float x, float z){
        // pake scaling dan offset
        x = frequencyX * x + offsetX;
        z = frequencyZ * z + offsetZ;
        // pake grid cell kotak dimana titik ini berada
        final float gridXVal = MathUtils.floor(x);
        final float gridZVal = MathUtils.floor(z);
        // ngitung posisi relatif titik di dalam kotak 0.0 s/d 1.0
        final float fracX = x - gridXVal;
        final float fracZ = z - gridZVal;
        // ngonversi ke integer buat akses array hashnya
        final int cellX = (int)(gridXVal);
        final int cellZ = (int)(gridZVal);
        // ambil vektor gradien di 4 sudut kotak
        final Vector2 vecTopLeft  = getPseudoRandomDir(cellX, cellZ);
        final Vector2 vecTopRight = getPseudoRandomDir(cellX, cellZ + 1);
        final Vector2 vecBotLeft  = getPseudoRandomDir(cellX + 1, cellZ);
        final Vector2 vecBotRight = getPseudoRandomDir(cellX + 1, cellZ + 1);
        // ngitung dot product buat nyari jarak titik ke arah gradien
        final float dotTL = vecTopLeft.dot(fracX, fracZ);
        final float dotTR = vecTopRight.dot(fracX, fracZ - 1.0f);
        final float dotBL = vecBotLeft.dot(fracX - 1.0f, fracZ);
        final float dotBR = vecBotRight.dot(fracX - 1.0f, fracZ - 1.0f);
        // ngitung kurva fading biar smooth
        final float fadeX = fracX * fracX * fracX * (fracX * (fracX * 6.0f - 15.0f) + 10.0f);
        final float fadeZ = fracZ * fracZ * fracZ * (fracZ * (fracZ * 6.0f - 15.0f) + 10.0f);
        // nyampurin sumbu X dulu baru sumbu Z biar haluss
        float lerpX1 = MathUtils.lerp(dotTL, dotBL, fadeX);
        float lerpX2 = MathUtils.lerp(dotTR, dotBR, fadeX);
        float finalValue = MathUtils.lerp(lerpX1, lerpX2, fadeZ);
        // normalisasi hasil akhir ke range 0..1 karna output asli perlin itu -1..1
        return (finalValue + 1.0f) / 2.0f;
    }
}
