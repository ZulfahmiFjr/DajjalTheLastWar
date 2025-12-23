package com.finpro7.oop.managers;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.finpro7.oop.entities.Particle;

public class ParticleSystem {

    // ini dia implementasi object pool-nya!
    private final Pool<Particle> particlePool;
    private final Array<Particle> activeParticles = new Array<>();
    private Model particleModel;

    public ParticleSystem() {
        // bikin model kotak simpel buat darah
        ModelBuilder mb = new ModelBuilder();
        particleModel = mb.createBox(1f, 1f, 1f,
            new Material(ColorAttribute.createDiffuse(Color.RED)),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        // inisialisasi pool
        particlePool = new Pool<Particle>(50, 200) { // start 50, max 200
            @Override
            protected Particle newObject() {
                // ini cuma dipanggil kalo pool kosong (bikin baru)
                return new Particle(particleModel);
            }
        };
    }

    public void spawnBloodEffect(float x, float y, float z) {
        // spawn 8 butir darah setiap kali panggil
        for(int i=0; i<8; i++){
            // ambil dari pool (reuse objek lama kalo ada)
            Particle p = particlePool.obtain();
            p.init(x, y, z);
            activeParticles.add(p);
        }
    }

    public void update(float delta) {
        // loop terbalik biar aman pas ngehapus
        for (int i = activeParticles.size - 1; i >= 0; i--) {
            Particle p = activeParticles.get(i);
            p.update(delta);

            if (!p.active) {
                // balikin ke pool biar bisa dipake lagi nanti (recycle)
                activeParticles.removeIndex(i);
                particlePool.free(p);
            }
        }
    }

    public void render(ModelBatch batch) {
        for (Particle p : activeParticles) {
            if(p.active) {
                batch.render(p.instance);
            }
        }
    }

    public void dispose() {
        particleModel.dispose();
    }
}
