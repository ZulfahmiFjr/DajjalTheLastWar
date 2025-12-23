package com.finpro7.oop.world;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;

public class SpatialGrid {

    // ukuran per kotaknya, 10 meter udah cukup lega
    private float cellSize;

    // ini map buat nyimpen daftar pohon di setiap kotak grid
    // kuncinya pake integer (id kotak), isinya list pohon
    private IntMap<Array<ModelInstance>> grid;

    // vektor sementara buat ngitung posisi biar gak new new terus
    private final Vector3 tmpPos = new Vector3();

    public SpatialGrid(float cellSize) {
        this.cellSize = cellSize;
        this.grid = new IntMap<>();
    }

    // method buat masukin pohon ke dalem kotak yang sesuai
    public void addTree(ModelInstance tree) {
        tree.transform.getTranslation(tmpPos);
        int key = getKey(tmpPos.x, tmpPos.z);

        // cek dulu kotaknya udah ada belum, kalo belum kita bikin list baru
        if (!grid.containsKey(key)) {
            grid.put(key, new Array<ModelInstance>());
        }
        grid.get(key).add(tree);
    }

    // method buat dapetin pohon pohon di sekitar player aja
    // jadi gak usah cek ribuan pohon, cukup yang di kotak ini dan sekitarnya
    public void getNearbyTrees(float x, float z, Array<ModelInstance> outResult) {
        outResult.clear();

        // kita ambil index kotak tempat player berdiri
        int centerX = MathUtils.floor(x / cellSize);
        int centerZ = MathUtils.floor(z / cellSize);

        // loop 3x3 kotak di sekitar player (biar kalo pas di pinggir garis grid tetep keambil)
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int key = getHashKey(centerX + i, centerZ + j);
                Array<ModelInstance> treesInCell = grid.get(key);

                // kalo di kotak itu ada pohon, masukin ke hasil
                if (treesInCell != null) {
                    outResult.addAll(treesInCell);
                }
            }
        }
    }

    // rumus matematika buat bikin id unik setiap kotak
    // x kita geser bit-nya biar gak tabrakan sama z
    private int getKey(float x, float z) {
        int ix = MathUtils.floor(x / cellSize);
        int iz = MathUtils.floor(z / cellSize);
        return getHashKey(ix, iz);
    }

    // helper buat generate key dari koordinat grid
    private int getHashKey(int x, int z) {
        // pake rumus simpel aja, x dikali angka gede tambah z
        // 31 itu angka prima biar sebarannya bagus
        return x * 73856093 ^ z * 19349663;
    }
}
