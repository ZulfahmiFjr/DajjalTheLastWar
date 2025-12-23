package com.finpro7.oop.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.finpro7.oop.entities.BaseEnemy;
import com.finpro7.oop.world.PerlinNoise;
import com.finpro7.oop.world.Terrain;
import com.finpro7.oop.world.weapon.Firearm;

public class WorldRenderer implements Disposable {

    // kamera kita taro sini biar renderer yang ngatur viewnya
    public PerspectiveCamera cam;

    // alat alat buat gambar 3d
    private Environment env;
    private RenderContext renderContext;
    private ModelBatch modelBatch;
    private DirectionalLight dirLight;

    // buat gambar garis peluru
    private ShapeRenderer shapeRenderer;

    // logic kabut atau fog visual
    private Model fogModel;
    private Array<ModelInstance> fogInstances = new Array<>();
    private float fogSpeed = 2.0f;
    private int fogCount = 200;

    // settingan warna atmosfer
    private final Color NORMAL_AMBIENT = new Color(0.6f, 0.6f, 0.6f, 1f);
    private final Color NORMAL_FOG = new Color(0.08f, 0.1f, 0.14f, 1f);

    public WorldRenderer() {
        initCamera();
        initEnvironment();
        initRendererTools();
    }

    // inisialisasi kamera
    private void initCamera() {
        cam = new PerspectiveCamera(67f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.near = 0.1f;
        cam.far = 200f;
    }

    // setting lampu dan kabut dasar
    private void initEnvironment() {
        env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, NORMAL_AMBIENT));
        dirLight = new DirectionalLight().set(1f, 1f, 1f, -0.6f, -1f, -0.3f);
        env.add(dirLight);
        env.set(new ColorAttribute(ColorAttribute.Fog, NORMAL_FOG));
    }

    // siapin batch dan context buat rendering
    private void initRendererTools() {
        renderContext = new RenderContext(new DefaultTextureBinder(DefaultTextureBinder.LRU, 1));
        modelBatch = new ModelBatch();
        shapeRenderer = new ShapeRenderer();
    }

    // method utama buat ngegambar semuanya, dipanggil dari gamescreen
    public void render(float delta, Terrain terrain, Array<ModelInstance> trees,
                       Array<BaseEnemy> enemies, ItemManager itemManager,
                       Firearm weapon, Vector3 bulletStart, Vector3 bulletEnd, float tracerTimer, ParticleSystem particleSystem) {

        // bersihin layar dulu pake warna gelap
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.08f, 0.1f, 0.14f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);

        // update posisi kabut biar gerak
        updateFog(delta, terrain);

        // mulai gambar terrain
        renderContext.begin();
        terrain.render(cam, renderContext);
        renderContext.end();

        // mulai gambar objek objek 3d
        modelBatch.begin(cam);

        // gambar pohon
        for (ModelInstance tree : trees) modelBatch.render(tree, env);

        // gambar musuh
        for (BaseEnemy enemy : enemies) modelBatch.render(enemy.modelInstance, env);

        // gambar partikel darah ambil dari object pool
        if (particleSystem != null) {
            particleSystem.render(modelBatch);
        }

        // gambar item yang jatoh
        itemManager.render(modelBatch, env);

        // gambar senjata di tangan player
        if (weapon != null && weapon.viewModel != null) {
            weapon.setView(cam);
            modelBatch.render(weapon.viewModel);
        }

        // flush biar kegambar semua
        modelBatch.flush();

        // gambar kabut (transparan jadi depth mask dimatiin dulu)
        Gdx.gl.glDepthMask(false);
        for (ModelInstance fog : fogInstances) modelBatch.render(fog);
        Gdx.gl.glDepthMask(true);

        modelBatch.end();

        // gambar garis peluru kalo lagi nembak
        if (tracerTimer > 0) {
            shapeRenderer.setProjectionMatrix(cam.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.line(
                bulletStart.x, bulletStart.y, bulletStart.z,
                bulletEnd.x, bulletEnd.y, bulletEnd.z,
                Color.WHITE,
                Color.YELLOW
            );
            shapeRenderer.end();
        }
    }

    // update viewport pas layar diubah ukurannya
    public void resize(int width, int height) {
        cam.viewportWidth = width;
        cam.viewportHeight = height;
        cam.update();
    }

    // method buat inisialisasi kabut prosedural (dipindahin dari gamescreen)
    public void createFogSystem(Terrain terrain) {
        ModelBuilder modelBuilder = new ModelBuilder();
        Texture kabutTex = createProceduralFogTexture(128);
        Material fogMat = new Material(
            TextureAttribute.createDiffuse(kabutTex),
            new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA),
            IntAttribute.createCullFace(GL20.GL_NONE)
        );
        long attr = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates;
        modelBuilder.begin();
        MeshPartBuilder meshBuilder = modelBuilder.part("fog_cluster", GL20.GL_TRIANGLES, attr, fogMat);

        // bikin partikel kabut kotak kotak
        int planesCount = 1;
        float baseSize = 10f;
        for (int i = 0; i < planesCount; i++) {
            Vector3 axis = new Vector3(MathUtils.random(-1f, 1f), MathUtils.random(-1f, 1f), MathUtils.random(-1f, 1f)).nor();
            float angle = MathUtils.random(0f, 360f);
            Vector3 offset = new Vector3(MathUtils.random(-1.5f, 1.5f), MathUtils.random(-1.5f, 1.5f), MathUtils.random(-1.5f, 1.5f));
            Vector3 p1 = new Vector3(-baseSize, -baseSize, 0);
            Vector3 p2 = new Vector3(baseSize, -baseSize, 0);
            Vector3 p3 = new Vector3(baseSize, baseSize, 0);
            Vector3 p4 = new Vector3(-baseSize, baseSize, 0);
            Vector3 normal = new Vector3(0, 0, 1);
            p1.rotate(axis, angle).add(offset);
            p2.rotate(axis, angle).add(offset);
            p3.rotate(axis, angle).add(offset);
            p4.rotate(axis, angle).add(offset);
            normal.rotate(axis, angle);
            meshBuilder.rect(p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, p3.x, p3.y, p3.z, p4.x, p4.y, p4.z, normal.x, normal.y, normal.z);
        }
        fogModel = modelBuilder.end();

        // sebar kabutnya random di map
        for (int i = 0; i < fogCount; i++) {
            ModelInstance fog = new ModelInstance(fogModel);
            float x = MathUtils.random(-160f, 160f);
            float z = MathUtils.random(-160f, 160f);
            float yT = terrain.getHeight(x, z);
            float y = MathUtils.random(yT, yT + 5f);
            fog.transform.setToTranslation(x, y + MathUtils.random(1f, 5f), z);
            fog.transform.rotate(Vector3.Y, MathUtils.random(0f, 360f));
            float randomScale = MathUtils.random(1.5f, 5.0f);
            fog.transform.scale(randomScale, randomScale * 0.6f, randomScale);
            fogInstances.add(fog);
        }
    }

    // helper bikin tekstur noise buat kabut
    private Texture createProceduralFogTexture(int size) {
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(size, size, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        PerlinNoise texNoise = new PerlinNoise();
        texNoise.frequencyX = 0.07f;
        texNoise.frequencyZ = 0.07f;
        texNoise.offsetX = MathUtils.random(0, 1000f);
        texNoise.offsetZ = MathUtils.random(0, 1000f);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                // bikin pinggirannya transparan biar gak kotak banget
                if (x == 0 || x == size - 1 || y == 0 || y == size - 1) {
                    pixmap.setColor(0f, 0f, 0f, 0f);
                    pixmap.drawPixel(x, y);
                    continue;
                }
                float noiseVal = texNoise.getHeight(x, y);
                float dx = x - size / 2f;
                float dy = y - size / 2f;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                float sphereMask = Math.max(0, 1.0f - (dist / (size / 2f)));
                sphereMask = (float) Math.pow(sphereMask, 3.5f);
                float alpha = Math.min(noiseVal * sphereMask * 1.3f, 1.0f);
                pixmap.setColor(0.92f, 0.96f, 1f, alpha);
                pixmap.drawPixel(x, y);
            }
        }
        Texture t = new Texture(pixmap);
        pixmap.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    // logika gerakin kabut biar dinamis
    private void updateFog(float delta, Terrain terrain) {
        for (ModelInstance fog : fogInstances) {
            Vector3 pos = fog.transform.getTranslation(new Vector3());
            pos.x += fogSpeed * delta;
            // kalo kabut lewat batas map, lempar lagi ke ujung satunya
            if (pos.x > 160f) {
                pos.x = -160f;
                pos.z = MathUtils.random(-160f, 160f);
                pos.y = terrain.getHeight(pos.x, pos.z) + MathUtils.random(1f, 5f);
                fog.transform.idt().setToTranslation(pos).rotate(Vector3.Y, MathUtils.random(0f, 360f));
                float s = MathUtils.random(1.5f, 5.0f);
                fog.transform.scale(s, s * 0.6f, s);
            } else fog.transform.setTranslation(pos);
        }
    }

    // akses ke environment buat efek petir dajjal
    public Environment getEnvironment() {
        return env;
    }

    // akses ke lampu buat diwarnain pas boss fight
    public DirectionalLight getDirectionalLight() {
        return dirLight;
    }

    @Override
    public void dispose() {
        if (modelBatch != null) modelBatch.dispose();
        if (fogModel != null) fogModel.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        // render context gak perlu didispose di libgdx versi baru biasanya
    }
}
