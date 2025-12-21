package com.finpro7.oop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.*;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.*;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.finpro7.oop.world.Terrain;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.collision.Ray;
import com.finpro7.oop.world.weapon.Firearm;

public class GameScreen implements Screen {

    final Main game;
    private Stage stage;

    // Sistem 3D
    private PerspectiveCamera cam;
    private Environment env;
    private RenderContext renderContext;
    private ModelBatch modelBatch;

    // FPS WEAPON SYSTEM
    private SpriteBatch uiBatch;
    private Texture crosshairTex;
    private ShapeRenderer shapeRenderer;
    private Firearm playerWeapon;
    private Array<Firearm> inventory = new Array<>();

    // bullet tracer
    private Vector3 bulletOrigin = new Vector3();
    private Vector3 bulletDest = new Vector3();
    private float bulletTracerTimer = 0f;

    // Asset dan World
    private Terrain terrain;
    private PerlinNoise perlin;
    private Model treeModel;
    private Array<ModelInstance> treeInstances = new Array<>();

    // sistem dajjalnya
    private Model dajjalModel; // buat nampung data model 3D nya dari file g3db
    private DajjalEntity dajjal; // entity utamanya yg ngatur logika gerak sama animasinya

    // Kontrol Pemain
    private float yawDeg, pitchDeg;
    private float mouseSens = 0.14f;
    private float moveSpeed = 10f;
    private float sprintMul = 2.0f;
    private float eyeHeight = 2.0f;
    private float margin = 1.5f;

    // Fisika
    private float verticalVelocity = 0f;
    private float gravity = 30f;
    private float jumpForce = 15f;
    private boolean isGrounded = false;
//    private int skipMouseFrames = 3;

    // Sistem Kabut
    private Model fogModel;
    private Array<ModelInstance> fogInstances = new Array<>();
    private float fogSpeed = 2.0f;
    private int fogCount = 200;

    // Sistem UI & State
    private boolean isPaused = false;
    private Table pauseContainer;
    private Image overlay;

    // Variabel Data UI (Dummy Stats)
    private float missionTimer = 0f;
    private Label coordLabel;
    private Label timeLabel;

    public GameScreen(final Main game) {
        this.game = game;
        stage = new Stage(new ScreenViewport());

        setup3DWorld();
        setupPauseInterface();

        uiBatch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        crosshairTex = game.assets.get("textures/crosshair.png", Texture.class);

//        setPaused(false);
        Gdx.input.setCursorCatched(true);
        Gdx.input.setInputProcessor(null);

        inventory.add(com.finpro7.oop.world.weapon.AkRifle.generateDefault());
        inventory.add(com.finpro7.oop.world.weapon.Pistol.generateDefault());
        playerWeapon = inventory.get(0); // Set default ke AK
    }

    private void setup3DWorld() {
        // setup kamera perspektif biar kyak mata manusia ada jauh dekatnya
        cam = new PerspectiveCamera(67f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.near = 0.1f; // jarak pandang terdekat
        cam.far = 200f; // jarak pandang terjauh, dipendekin jadi 100 buat efek kabut
        // buat setup pencahayaan biar gak gelap gulita
        env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.6f, 1f)); // cahaya dasar
        env.add(new DirectionalLight().set(1f, 1f, 1f, -0.6f, -1f, -0.3f)); // matahari
        env.set(new ColorAttribute(ColorAttribute.Fog, 0.08f, 0.1f, 0.14f, 1f)); // tambahin atribut kabut ke env
        renderContext = new RenderContext(new DefaultTextureBinder(DefaultTextureBinder.LRU, 1));
        modelBatch = new ModelBatch(); // inisialisasi ModelBatch buat model
        // buat setup generator perlin noisenya biar gunungnya random tiap kali playy
        perlin = new PerlinNoise();
//        perlin.terrainHeight = 6f;
        perlin.amplitude = 80f; // tinggi maksimal gunung
        perlin.frequencyX = 0.08f;
        perlin.frequencyZ = 0.08f;
        perlin.offsetX = MathUtils.random(0f, 999f); // geser seed random
        perlin.offsetZ = MathUtils.random(0f, 999f);
        // ambil model buat pohon, dllnya
        treeModel = game.assets.get("models/pohon.g3dj", Model.class);
        dajjalModel = game.assets.get("models/yajuj/yajuj.g3db", Model.class);
        terrain = new Terrain(env, perlin, 254, 254, 320f, 320f);
        createFogSystem(terrain);
        // biar daunnya transparan & keliatan dari dua sisi
        for(Material mat : treeModel.materials){
            // cek material daun index 1
            if(treeModel.materials.indexOf(mat, true) == 1){
                mat.set(
                    new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA),
                    FloatAttribute.createAlphaTest(0.25f),
                    IntAttribute.createCullFace(GL20.GL_NONE)
                );
            }
        }
        terrain.generateTrees(treeModel, treeInstances, 600); // generate pohon pohonnya di terrain sebanyak 600 secara random
        // buat ngatur spawn playernya
        Vector3 startPos = new Vector3();
        terrain.getRoadStartPos(startPos); // minta koordinat start, biar di awal jalan spiral startnya
        cam.position.set(startPos.x + 5.0f, startPos.y + eyeHeight, startPos.z + 5.0f); // buat set posisi kamera
        float spawnX = startPos.x + 15.0f;
        float spawnZ = startPos.z + 15.0f;
        float spawnY = terrain.getHeight(spawnX, spawnZ); // tinggi dari tinggi terrain di titik  x z itu
        // manggil class DajjalEntity
        dajjal = new DajjalEntity(dajjalModel, spawnX, spawnY, spawnZ);
        Vector3 lookTarget = new Vector3();
        // Panggil methodnya aja, jangan itung manual di sini!
        terrain.getRoadLookAtPos(lookTarget);
        cam.direction.set(lookTarget.sub(cam.position)).nor();
        cam.up.set(Vector3.Y);
        cam.update();
    }

    private void setupPauseInterface() {
        // 1. Overlay Fullscreen Hitam Transparan
        overlay = new Image(Main.skin.newDrawable("white", new Color(0.05f, 0.05f, 0.08f, 0.85f)));
        overlay.setFillParent(true);
        overlay.setVisible(false);
        stage.addActor(overlay);

        // 2. Container Utama (Menggunakan Table)
        pauseContainer = new Table();
        pauseContainer.setFillParent(true);
        pauseContainer.setVisible(false);

        // -- Bagian Kiri: Info Panel (Stats) --
        Table infoTable = new Table();
        infoTable.setBackground(Main.skin.newDrawable("white", new Color(0f, 0f, 0f, 0.5f)));
        infoTable.pad(30);

        Label sysTitle = new Label("SYSTEM PAUSED", Main.skin, "title");
        sysTitle.setFontScale(0.8f);
        sysTitle.setColor(Color.ORANGE);

        Label missionText = new Label("CURRENT OP: RECON", Main.skin, "text");

        // Label dinamis untuk data
        timeLabel = new Label("T+ 00:00:00", Main.skin, "subtitle");
        coordLabel = new Label("LOC: 000, 000", Main.skin, "text");
        coordLabel.setColor(Color.LIGHT_GRAY);

        infoTable.add(sysTitle).left().padBottom(10).row();
        infoTable.add(missionText).left().padBottom(30).row();
        infoTable.add(timeLabel).left().padBottom(5).row();
        infoTable.add(coordLabel).left().expandY().top();

        // -- Bagian Kanan: Menu Buttons --
        Table buttonTable = new Table();

        TextButton btnResume = createStyledButton("RESUME MISSION", () -> togglePause());
        TextButton btnRestart = createStyledButton("RESTART SECTOR", () -> game.setScreen(new GameScreen(game)));
        TextButton btnExit = createStyledButton("ABORT OPERATION", () -> {
            game.setScreen(new MenuScreen(game));
            dispose();
        });

        buttonTable.add(btnResume).width(300).height(60).padBottom(15).right().row();
        buttonTable.add(btnRestart).width(300).height(60).padBottom(15).right().row();
        buttonTable.add(btnExit).width(300).height(60).right().row();

        // Menyusun layout kiri dan kanan
        // Expand X agar mereka terpisah ke ujung layar
        pauseContainer.add(infoTable).width(400).expandY().fillY().left();
        pauseContainer.add(buttonTable).expand().bottom().right().pad(50);

        stage.addActor(pauseContainer);
    }

    private TextButton createStyledButton(String text, Runnable action) {
        TextButton btn = new TextButton(text, Main.skin, "btn-main");
        btn.setTransform(true);
        btn.setOrigin(Align.right); // Pivot di kanan untuk efek scaling

        btn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                // Efek hover: geser sedikit ke kiri dan membesar
                if(pointer == -1) {
                    btn.addAction(Actions.parallel(
                        Actions.scaleTo(1.05f, 1.05f, 0.1f),
                        Actions.moveBy(-10f, 0f, 0.1f)
                    ));
                }
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if(pointer == -1) {
                    btn.addAction(Actions.parallel(
                        Actions.scaleTo(1f, 1f, 0.1f),
                        Actions.moveBy(10f, 0f, 0.1f)
                    ));
                }
            }
        });
        return btn;
    }

    public void togglePause() {
        isPaused = !isPaused;

        if (isPaused) {
            Gdx.input.setCursorCatched(false);
            Gdx.input.setInputProcessor(stage);

            // Tampilkan Overlay dengan Fade In
            overlay.setVisible(true);
            overlay.getColor().a = 0f;
            overlay.addAction(Actions.fadeIn(0.2f));

            // Animasi Menu Slide In dari bawah
            pauseContainer.setVisible(true);
            pauseContainer.setPosition(0, -50); // Mulai sedikit di bawah
            pauseContainer.getColor().a = 0f;

            pauseContainer.addAction(Actions.parallel(
                Actions.fadeIn(0.25f, Interpolation.fade),
                Actions.moveTo(0, 0, 0.4f, Interpolation.circleOut)
            ));
        } else {
            Gdx.input.setCursorCatched(true);
            Gdx.input.setInputProcessor(null);
            overlay.setVisible(false);
            pauseContainer.setVisible(false);
        }
    }

    public void setPaused(boolean v) {
        if (isPaused != v) togglePause();
    }

    private void updateUI() {
        if (!isPaused) return;

        // Update timer format MM:SS
        int minutes = (int)missionTimer / 60;
        int seconds = (int)missionTimer % 60;
        timeLabel.setText(String.format("T+ %02d:%02d", minutes, seconds));

        // Update koordinat player
        coordLabel.setText(String.format("LOC: %d, %d, %d",
            (int)cam.position.x, (int)cam.position.y, (int)cam.position.z));
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            togglePause();
        }

        if (!isPaused) {
            updateMouseLook();
            updateMovement(delta);
            clampAndStickToTerrain(delta);
            cam.update();
            updateFog(delta);
            missionTimer += delta;
            // kalo dajjalnya udh keload, suruh dia update logika, animasi, sama ngejar posisi kita
            if(dajjal != null) dajjal.update(delta, cam.position, terrain);
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) playerWeapon = inventory.get(0);
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) playerWeapon = inventory.get(1);
            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) { // Shooting input
                shoot();
            }
            if (bulletTracerTimer > 0) { // update bullet tracer timer
                bulletTracerTimer -= delta;
            }
        } else {
            // Update data UI saat pause aktif
            updateUI();
        }

        cam.update();

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.08f, 0.1f, 0.14f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);

        renderContext.begin();
        terrain.render(cam, renderContext);
        renderContext.end();
        modelBatch.begin(cam);
        for(ModelInstance tree : treeInstances) modelBatch.render(tree, env);
        if(dajjal != null) modelBatch.render(dajjal.badanDajjal, env);

        if (playerWeapon != null && playerWeapon.viewModel != null) {
            playerWeapon.setView(cam); // Biar nempel di kamera
            modelBatch.render(playerWeapon.viewModel); // Tidak pakai env biar tidak gelap/kena fog
        }
        // suruh GPU selesaikan gambar pohon & dajjal dulu
        modelBatch.flush();
//        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.gl.glDepthMask(false);
        for(ModelInstance fog : fogInstances) modelBatch.render(fog); // baru render kabut tanpa env biar terang
        Gdx.gl.glDepthMask(true);
//        Gdx.gl.glEnable(GL20.GL_CULL_FACE);

        modelBatch.end();
        if (bulletTracerTimer > 0) {
            shapeRenderer.setProjectionMatrix(cam.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.line(
                bulletOrigin.x, bulletOrigin.y, bulletOrigin.z,
                bulletDest.x,   bulletDest.y,   bulletDest.z,
                Color.WHITE,
                Color.YELLOW
            );
            shapeRenderer.end();
        }
        stage.act(delta);
        uiBatch.begin();
        uiBatch.draw(
            crosshairTex,
            Gdx.graphics.getWidth() / 2f - 16,
            Gdx.graphics.getHeight() / 2f - 16,
            32, 32
        );
        uiBatch.end();
        stage.draw();
    }

    private void updateMouseLook(){
        if(isPaused || !Gdx.input.isCursorCatched()) return;

        int dx = Gdx.input.getDeltaX();
        int dy = Gdx.input.getDeltaY();

//        if(skipMouseFrames > 0){ skipMouseFrames--; return; }

        yawDeg -= dx * mouseSens;
        pitchDeg -= dy * mouseSens;
        pitchDeg = MathUtils.clamp(pitchDeg, -89f, 89f);

        float yawRad = yawDeg * MathUtils.degreesToRadians;
        float pitchRad = pitchDeg * MathUtils.degreesToRadians;
        cam.direction.set(MathUtils.sin(yawRad) * MathUtils.cos(pitchRad), MathUtils.sin(pitchRad), MathUtils.cos(yawRad) * MathUtils.cos(pitchRad)).nor();
    }

    private void updateMovement(float delta){
        float speed = moveSpeed;
        if(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) speed *= sprintMul;

        Vector3 forward = new Vector3(cam.direction.x, 0f, cam.direction.z).nor();
        Vector3 right = new Vector3(forward).crs(Vector3.Y).nor();
        Vector3 move = new Vector3();

        if(Gdx.input.isKeyPressed(Input.Keys.W)) move.add(forward);
        if(Gdx.input.isKeyPressed(Input.Keys.S)) move.sub(forward);
        if(Gdx.input.isKeyPressed(Input.Keys.D)) move.add(right);
        if(Gdx.input.isKeyPressed(Input.Keys.A)) move.sub(right);

        if(move.len2() > 0){
            move.nor();
            int substeps = 4;
            float subDelta = delta / substeps;
            for(int i = 0; i < substeps; i++){
                float stepX = move.x * speed * subDelta;
                float stepZ = move.z * speed * subDelta;

                float probeFar = 0.6f;
                float r = (float)Math.sqrt(cam.position.x * cam.position.x + cam.position.z * cam.position.z);
                float slopeLimit = (r < 80f) ? 1.4f : 0.6f;

                float currentY = terrain.getHeight(cam.position.x, cam.position.z);

                boolean safeX = true;
                float dirX = Math.signum(move.x);
                float probeX_Far = cam.position.x + dirX * probeFar;
                if(terrain.getHeight(probeX_Far, cam.position.z) - currentY > slopeLimit) safeX = false;

                if(safeX) cam.position.x += stepX;
                currentY = terrain.getHeight(cam.position.x, cam.position.z);

                boolean safeZ = true;
                float dirZ = Math.signum(move.z);
                float probeZ_Far = cam.position.z + dirZ * probeFar;
                if(terrain.getHeight(cam.position.x, probeZ_Far) - currentY > slopeLimit) safeZ = false;

                if(safeZ) cam.position.z += stepZ;
            }
        }

        if(Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && isGrounded){
            verticalVelocity = jumpForce;
            isGrounded = false;
        }
    }

    private void clampAndStickToTerrain(float delta){
        cam.position.x = terrain.clampX(cam.position.x, margin);
        cam.position.z = terrain.clampZ(cam.position.z, margin);

        verticalVelocity -= gravity * delta;
        cam.position.y += verticalVelocity * delta;

        float groundHeight = terrain.getHeight(cam.position.x, cam.position.z);
        float minHeight = groundHeight + eyeHeight;

        if(cam.position.y < minHeight){
            cam.position.y = minHeight;
            verticalVelocity = 0f;
            isGrounded = true;
        }else isGrounded = false;
    }

    private void createFogSystem(Terrain terrain) {
        ModelBuilder modelBuilder = new ModelBuilder();
        // bikin tekstur kabut procedural 128x128 biar lumayan tajem
        Texture kabutTex = createProceduralFogTexture(128);
        Material fogMat = new Material(
            TextureAttribute.createDiffuse(kabutTex),
            // atur blending biar transparan kyak asep beneran
            new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA),
            IntAttribute.createCullFace(GL20.GL_NONE)
        );
        long attr = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates;
        modelBuilder.begin();
        MeshPartBuilder meshBuilder = modelBuilder.part("fog_cluster", GL20.GL_TRIANGLES, attr, fogMat);
        // loop buat numpuk beberapa plane biar jadi gumpalan yg bervolume
        int planesCount = 1;
        float baseSize = 10f; // ukuran dasar per lembarnya
        for(int i = 0; i < planesCount; i++){
            // cari sumbu putar acak
            Vector3 axis = new Vector3(MathUtils.random(-1f, 1f), MathUtils.random(-1f, 1f), MathUtils.random(-1f, 1f)).nor();
            float angle = MathUtils.random(0f, 360f); // sudut putar random
            // geser dikit posisinya biar ga numpuk pas di tengah
            Vector3 offset = new Vector3(MathUtils.random(-1.5f, 1.5f), MathUtils.random(-1.5f, 1.5f), MathUtils.random(-1.5f, 1.5f));
            // tentuin 4 titik sudut plane secara manual, anggep awalnya madep depan
            Vector3 p1 = new Vector3(-baseSize, -baseSize, 0); // kiri bawah
            Vector3 p2 = new Vector3( baseSize, -baseSize, 0); // kanan bawah
            Vector3 p3 = new Vector3( baseSize,  baseSize, 0); // kanan atas
            Vector3 p4 = new Vector3(-baseSize,  baseSize, 0); // kiri atas
            Vector3 normal = new Vector3(0, 0, 1); // arah normal
            // terapin rotasi sama offset ke titik titik tadi
            p1.rotate(axis, angle).add(offset);
            p2.rotate(axis, angle).add(offset);
            p3.rotate(axis, angle).add(offset);
            p4.rotate(axis, angle).add(offset);
            normal.rotate(axis, angle);
            // masukin data titiknya ke mesh builder
            meshBuilder.rect(
                p1.x, p1.y, p1.z,
                p2.x, p2.y, p2.z,
                p3.x, p3.y, p3.z,
                p4.x, p4.y, p4.z,
                normal.x, normal.y, normal.z
            );
        }
        fogModel = modelBuilder.end();
        // bikin terrainnya grid 254x254, ukuran worldnya 320x320 meter
        terrain = new Terrain(env, perlin, 254, 254, 320f, 320f);
        // sebar objek kabutnya ke seluruh map
        for(int i = 0; i < fogCount; i++){
            ModelInstance fog = new ModelInstance(fogModel);
            // random posisi X sama Z nyaa
            float x = MathUtils.random(-160f, 160f);
            float z = MathUtils.random(-160f, 160f);
            float yT = terrain.getHeight(x, z); // ambil tinggi tanah di koordinat itu biar kabutnya napak
            float y = MathUtils.random(yT, yT + 5f); // posisi y kabut random minimal sesuai tinggi tanah
            fog.transform.setToTranslation(x, y + MathUtils.random(1f, 5f), z); // set posisi awalnya, y ditambah dikit biar ngambang
            fog.transform.rotate(Vector3.Y, MathUtils.random(0f, 360f)); // rotasi acak biar ga keliatan seragam madepnya
            float randomScale = MathUtils.random(1.5f, 5.0f); // random ukurannya biar variatif, ada yg gede ada yg kecil
            fog.transform.scale(randomScale, randomScale * 0.6f, randomScale); // skala Y dipenyetin dikit biar kyak lapisan tipis
            fogInstances.add(fog);
        }
    }

    private void updateFog(float delta){
        for(ModelInstance fog : fogInstances){
            Vector3 pos = fog.transform.getTranslation(new Vector3());
            pos.x += fogSpeed * delta;
            if(pos.x > 160f){
                pos.x = -160f;
                pos.z = MathUtils.random(-160f, 160f);
                pos.y = terrain.getHeight(pos.x, pos.z) + MathUtils.random(1f, 5f);
                fog.transform.idt().setToTranslation(pos).rotate(Vector3.Y, MathUtils.random(0f, 360f));
                float s = MathUtils.random(1.5f, 5.0f);
                fog.transform.scale(s, s * 0.6f, s);
            }else fog.transform.setTranslation(pos);
        }
    }

    private Texture createProceduralFogTexture(int size){
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(size, size, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        PerlinNoise texNoise = new PerlinNoise();
        texNoise.frequencyX = 0.07f; texNoise.frequencyZ = 0.07f;
        texNoise.offsetX = MathUtils.random(0, 1000f); texNoise.offsetZ = MathUtils.random(0, 1000f);

        for(int x = 0; x < size; x++){
            for(int y = 0; y < size; y++){
                if(x == 0 || x == size - 1 || y == 0 || y == size - 1){
                    pixmap.setColor(0f, 0f, 0f, 0f); pixmap.drawPixel(x, y); continue;
                }
                float noiseVal = texNoise.getHeight(x, y);
                float dx = x - size/2f; float dy = y - size/2f;
                float dist = (float)Math.sqrt(dx*dx + dy*dy);
                float sphereMask = Math.max(0, 1.0f - (dist / (size/2f)));
                sphereMask = (float)Math.pow(sphereMask, 3.5f);
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

    @Override
    public void resize(int width, int height) {
        cam.viewportWidth = width;
        cam.viewportHeight = height;
        cam.update();
        stage.getViewport().update(width, height, true);
    }

    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void dispose() {
        if(terrain != null) terrain.dispose();
        if(modelBatch != null) modelBatch.dispose();
        if(fogModel != null) fogModel.dispose();
        if(stage != null) stage.dispose();
        if(uiBatch != null) uiBatch.dispose();
        if(shapeRenderer != null) shapeRenderer.dispose();
    }

    private void shoot() {
        if (playerWeapon == null) return;
        playerWeapon.shoot(); // Jalankan animasi rekoil
        cam.update();

        // ngambil tujuan (tengah layar)
        Ray ray = cam.getPickRay(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f);
        bulletDest.set(ray.direction).scl(100f).add(ray.origin);

        // Hitung titik keluar peluru (Muzzle)
        Vector3 camRight = cam.direction.cpy().crs(cam.up).nor();
        Vector3 camDown = camRight.cpy().crs(cam.direction).nor().scl(-1f);
        bulletOrigin.set(cam.position);

        float fwd, side, down;
        if (playerWeapon instanceof com.finpro7.oop.world.weapon.AkRifle) {
            // Untuk ak
            fwd = 2.15f;  // Jarak peluru ke depan
            side = 0.52f; // Geser kiri(-) atau kanan(+)
            down = 0.3f; // Geser atas(-) atau bawah(+)
        } else {
            // Untuk pistol
            fwd = 1.35f;
            side = 0.52f;
            down = 0.38f;
        }

        bulletOrigin.add(camRight.scl(side));
        bulletOrigin.add(camDown.scl(down));
        bulletOrigin.add(new Vector3(cam.direction).scl(fwd));
        bulletTracerTimer = 0.03f;
    }

}
