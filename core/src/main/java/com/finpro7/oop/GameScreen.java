package com.finpro7.oop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.*;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
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

// Import Entities & Logics
import com.finpro7.oop.entities.BaseEnemy;
import com.finpro7.oop.entities.Coin;
import com.finpro7.oop.entities.EnemyFactory;
import com.finpro7.oop.logics.WaveManager;
import com.finpro7.oop.world.Terrain;
import com.finpro7.oop.world.weapon.Firearm;

public class GameScreen implements Screen {

    final Main game;
    private Stage stage;

    // Sistem 3D
    private PerspectiveCamera cam;
    private Environment env;
    private RenderContext renderContext;
    private ModelBatch modelBatch;

    private Array<Coin> activeCoins = new Array<>(); // List koin
    private Model coinModel; // Cetakan modelnya
    private int score = 0; // Skor player

    // FPS WEAPON SYSTEM (Punya Kamu)
    private SpriteBatch uiBatch;
    private Texture crosshairTex;
    private ShapeRenderer shapeRenderer;
    private Firearm playerWeapon;
    private Array<Firearm> inventory = new Array<>();
    private Vector3 bulletOrigin = new Vector3();
    private Vector3 bulletDest = new Vector3();
    private float bulletTracerTimer = 0f;

    // Asset dan World
    private Terrain terrain;
    private PerlinNoise perlin;
    private Model treeModel;
    private Array<ModelInstance> treeInstances = new Array<>();
    private final Vector3 tempHitCenter = new Vector3();

    // Entity Spesial (Dajjal)
    private Model dajjalModel;
    private DajjalEntity dajjal;

    // Entity Musuh Biasa (Yajuj Majuj - Punya Teman)
    private Array<BaseEnemy> activeEnemies = new Array<>();
    private EnemyFactory enemyFactory;
    private WaveManager waveManager;

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
    private final Vector3 tempPos = new Vector3(); // Helper vector

    // Sistem Kabut
    private Model fogModel;
    private Array<ModelInstance> fogInstances = new Array<>();
    private float fogSpeed = 2.0f;
    private int fogCount = 200;

    // Sistem UI & State
    private boolean isPaused = false;
    private Table pauseContainer;
    private Image overlay;

    // Variabel Data UI & HUD
    private float missionTimer = 0f;
    private Label coordLabel;
    private Label timeLabel;
    private Label stageLabel; // Info stage
    private Label enemyCountLabel; // Info sisa musuh
    private Label notificationLabel; // Notifikasi besar
    private Table hudTable;
    private float warningCooldown = 0f;
    private Label ammoLabel; // Label buat nampilin peluru
    // hit maker
    private float hitMarkerTimer = 0f; // timer buat nampilin tanda X
    private Texture hitMarkerTex; // gambar tanda X (pake crosshair aja diwarnain merah nanti)

    public GameScreen(final Main game) {
        this.game = game;
        stage = new Stage(new ScreenViewport());

        setup3DWorld();
        setupPauseInterface();
        setupHUD();

        // Setup FPS System
        uiBatch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        crosshairTex = game.assets.get("textures/crosshair.png", Texture.class);
        hitMarkerTex = crosshairTex; // kita pake gambar yg sama aja biar hemat, nanti ditint merah

        // Setup Weapon Inventory
        inventory.add(com.finpro7.oop.world.weapon.AkRifle.generateDefault());
        inventory.add(com.finpro7.oop.world.weapon.Pistol.generateDefault());
        playerWeapon = inventory.get(0);

        // Setup Enemy System (Punya Teman)
        Model yajujM = game.assets.get("models/yajuj/yajuj.g3db", Model.class);
        Model majujM = game.assets.get("models/majuj/majuj.g3db", Model.class);
        enemyFactory = new EnemyFactory(yajujM, majujM);

        waveManager = new WaveManager();
        waveManager.initLevelData(terrain);

        // Input Setup
        Gdx.input.setCursorCatched(true);
        Gdx.input.setInputProcessor(null);
    }

    private void setup3DWorld() {
        cam = new PerspectiveCamera(67f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.near = 0.1f;
        cam.far = 200f;

        env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.6f, 1f));
        env.add(new DirectionalLight().set(1f, 1f, 1f, -0.6f, -1f, -0.3f));
        env.set(new ColorAttribute(ColorAttribute.Fog, 0.08f, 0.1f, 0.14f, 1f));

        renderContext = new RenderContext(new DefaultTextureBinder(DefaultTextureBinder.LRU, 1));
        modelBatch = new ModelBatch();

        perlin = new PerlinNoise();
        perlin.amplitude = 80f;
        perlin.frequencyX = 0.08f;
        perlin.frequencyZ = 0.08f;
        perlin.offsetX = MathUtils.random(0f, 999f);
        perlin.offsetZ = MathUtils.random(0f, 999f);
        ModelBuilder mb = new ModelBuilder();
        // Bikin silinder gepeng warna Emas (Gold)
        coinModel = mb.createCylinder(1f, 0.1f, 1f, 20,
            new Material(ColorAttribute.createDiffuse(Color.GOLD)),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        treeModel = game.assets.get("models/pohon.g3dj", Model.class);
        dajjalModel = game.assets.get("models/dajjal.g3db", Model.class); // Pastikan ini benar (bukan yajuj)

        terrain = new Terrain(env, perlin, 254, 254, 320f, 320f);
        createFogSystem(terrain);

        for(Material mat : treeModel.materials){
            if(treeModel.materials.indexOf(mat, true) == 1){
                mat.set(
                    new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA),
                    FloatAttribute.createAlphaTest(0.25f),
                    IntAttribute.createCullFace(GL20.GL_NONE)
                );
            }
        }
        terrain.generateTrees(treeModel, treeInstances, 600);

        Vector3 startPos = new Vector3();
        terrain.getRoadStartPos(startPos);
        cam.position.set(startPos.x + 5.0f, startPos.y + eyeHeight, startPos.z + 5.0f);

        // Setup Dajjal Entity (Punya Kamu)
        float spawnX = startPos.x + 15.0f;
        float spawnZ = startPos.z + 15.0f;
        float spawnY = terrain.getHeight(spawnX, spawnZ);
//        dajjal = new DajjalEntity(dajjalModel, spawnX, spawnY, spawnZ);

        Vector3 lookTarget = new Vector3();
        terrain.getRoadLookAtPos(lookTarget);
        cam.direction.set(lookTarget.sub(cam.position)).nor();
        cam.up.set(Vector3.Y);
        cam.update();
    }

    // --- SETUP UI & HUD (Gabungan style kamu & logic teman) ---
    private void setupHUD(){
        hudTable = new Table();
        hudTable.top().left();
        hudTable.setFillParent(true);
        hudTable.pad(20);

        stageLabel = new Label("STAGE 1", Main.skin, "title");
        stageLabel.setFontScale(1.4f);
        stageLabel.setColor(Color.GOLD);

        enemyCountLabel = new Label("ENEMIES: 0", Main.skin, "subtitle");
        enemyCountLabel.setColor(Color.WHITE);
        ammoLabel = new Label("AMMO: -- / --", Main.skin, "subtitle");
        ammoLabel.setColor(Color.LIGHT_GRAY); // Warnanya abu-abu biar beda dikit

        hudTable.add(stageLabel).left().row();
        hudTable.add(enemyCountLabel).left().padTop(5).row();
        hudTable.add(ammoLabel).left().padTop(5);
        stage.addActor(hudTable);

        notificationLabel = new Label("", Main.skin, "title");
        notificationLabel.setFontScale(1.5f);
        notificationLabel.setColor(Color.RED);
        notificationLabel.setAlignment(Align.center);
        notificationLabel.setPosition(0, Gdx.graphics.getHeight() / 2f + 50f);
        notificationLabel.setWidth(Gdx.graphics.getWidth());
        notificationLabel.getColor().a = 0f;
        stage.addActor(notificationLabel);
    }

    private void showStageNotification(int stageNum){
        notificationLabel.setText("ENTERING STAGE " + stageNum);
        notificationLabel.getColor().a = 0f;
        notificationLabel.clearActions();
        notificationLabel.addAction(Actions.sequence(
            Actions.fadeIn(0.5f),
            Actions.delay(2.0f),
            Actions.fadeOut(1.0f)
        ));
    }

    private void showBarrierWarning(){
        if(warningCooldown > 0) return;
        warningCooldown = 1.5f;
        int enemiesLeft = waveManager.getRemainingEnemies();
        notificationLabel.setText("ELIMINATE " + enemiesLeft + " REMAINING HOSTILES\nTO PROCEED!");
        notificationLabel.setColor(Color.RED);
        notificationLabel.setFontScale(1.2f);
        notificationLabel.clearActions();
        notificationLabel.addAction(Actions.sequence(
            Actions.fadeIn(0.1f),
            Actions.delay(1.0f),
            Actions.fadeOut(0.5f)
        ));
    }

    private void setupPauseInterface() {
        overlay = new Image(Main.skin.newDrawable("white", new Color(0.05f, 0.05f, 0.08f, 0.85f)));
        overlay.setFillParent(true);
        overlay.setVisible(false);
        stage.addActor(overlay);

        pauseContainer = new Table();
        pauseContainer.setFillParent(true);
        pauseContainer.setVisible(false);

        Table infoTable = new Table();
        infoTable.setBackground(Main.skin.newDrawable("white", new Color(0f, 0f, 0f, 0.5f)));
        infoTable.pad(30);

        Label sysTitle = new Label("SYSTEM PAUSED", Main.skin, "title");
        sysTitle.setFontScale(0.8f);
        sysTitle.setColor(Color.ORANGE);

        Label missionText = new Label("CURRENT OP: RECON", Main.skin, "text");
        timeLabel = new Label("T+ 00:00:00", Main.skin, "subtitle");
        coordLabel = new Label("LOC: 000, 000", Main.skin, "text");
        coordLabel.setColor(Color.LIGHT_GRAY);

        infoTable.add(sysTitle).left().padBottom(10).row();
        infoTable.add(missionText).left().padBottom(30).row();
        infoTable.add(timeLabel).left().padBottom(5).row();
        infoTable.add(coordLabel).left().expandY().top();

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

        pauseContainer.add(infoTable).width(400).expandY().fillY().left();
        pauseContainer.add(buttonTable).expand().bottom().right().pad(50);

        stage.addActor(pauseContainer);
    }

    private TextButton createStyledButton(String text, Runnable action) {
        TextButton btn = new TextButton(text, Main.skin, "btn-main");
        btn.setTransform(true);
        btn.setOrigin(Align.right);
        btn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) { action.run(); }
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if(pointer == -1) btn.addAction(Actions.parallel(Actions.scaleTo(1.05f, 1.05f, 0.1f), Actions.moveBy(-10f, 0f, 0.1f)));
            }
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if(pointer == -1) btn.addAction(Actions.parallel(Actions.scaleTo(1f, 1f, 0.1f), Actions.moveBy(10f, 0f, 0.1f)));
            }
        });
        return btn;
    }

    public void togglePause() {
        isPaused = !isPaused;
        if(hudTable != null) hudTable.setVisible(!isPaused);
        if (isPaused) {
            Gdx.input.setCursorCatched(false);
            Gdx.input.setInputProcessor(stage);
            overlay.setVisible(true);
            overlay.getColor().a = 0f;
            overlay.addAction(Actions.fadeIn(0.2f));
            pauseContainer.setVisible(true);
            pauseContainer.setPosition(0, -50);
            pauseContainer.getColor().a = 0f;
            pauseContainer.addAction(Actions.parallel(Actions.fadeIn(0.25f, Interpolation.fade), Actions.moveTo(0, 0, 0.4f, Interpolation.circleOut)));
        } else {
            Gdx.input.setCursorCatched(true);
            Gdx.input.setInputProcessor(null);
            overlay.setVisible(false);
            pauseContainer.setVisible(false);
        }
    }

    private void updateUI() {
        if (!isPaused) return;
        int minutes = (int)missionTimer / 60;
        int seconds = (int)missionTimer % 60;
        timeLabel.setText(String.format("T+ %02d:%02d", minutes, seconds));
        coordLabel.setText(String.format("LOC: %d, %d, %d", (int)cam.position.x, (int)cam.position.y, (int)cam.position.z));
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) togglePause();

        if (!isPaused) {
            updateMouseLook();
            if(warningCooldown > 0) warningCooldown -= delta;
            updateMovement(delta);
            clampAndStickToTerrain(delta);
            cam.update();
            updateFog(delta);
            missionTimer += delta;

            // UPDATE LOGIC MUSUH & WAVE (Punya Teman)
            // 1. Dajjal Logic
            if(dajjal != null) dajjal.update(delta, cam.position, terrain);
            // 2. Wave Manager Logic
            waveManager.update(delta, cam.position, terrain, enemyFactory, activeEnemies);

            if(waveManager.justChangedStage){
                showStageNotification(waveManager.getCurrentStageNum());
                waveManager.justChangedStage = false;
            }

            // 3. Update Text UI
            stageLabel.setText("STAGE " + waveManager.getCurrentStageNum());
            int totalJatahStage = waveManager.getTotalEnemiesInStage();
            int sisaGlobal = waveManager.getRemainingEnemies();
            enemyCountLabel.setText("ENEMIES: " + sisaGlobal + " / " + totalJatahStage);
            if(sisaGlobal <= 3) enemyCountLabel.setColor(Color.RED);
            else enemyCountLabel.setColor(Color.WHITE);
            if (playerWeapon != null) {
                // Format: AMMO: [Peluru di Magazine] / [Sisa di Tas]
                String ammoText = "AMMO: " + playerWeapon.ammoInClip + " / " + playerWeapon.totalAmmo;
                ammoLabel.setText(ammoText);
                // Fitur Pemanis: Kalau peluru di mag abis (0), warnanya jadi Merah biar panik dikit
                if (playerWeapon.ammoInClip == 0) {
                    ammoLabel.setColor(Color.RED);
                } else if (playerWeapon.isReloading) {
                    ammoLabel.setText("RELOADING..."); // Biar keliatan lagi reload
                    ammoLabel.setColor(Color.YELLOW);
                } else {
                    ammoLabel.setColor(Color.LIGHT_GRAY);
                }
            }

            for(int i = activeEnemies.size - 1; i >= 0; i--){
                BaseEnemy enemy = activeEnemies.get(i);

                enemy.update(delta, cam.position, terrain, treeInstances, activeEnemies);

                // 1. CEK MATI (Coin Spawn Pindah Sini)
                if(enemy.isDead && !enemy.countedAsDead){
                    waveManager.reportEnemyDeath();
                    enemy.countedAsDead = true;

                    // Jadi pas nyawanya abis, koin langsung njeblug keluar, gak nunggu mayat ilang.
                    // Tinggi spawnnya (y) kita ambil dari posisi musuh + 1.0f biar gak kelelep tanah
                    Coin c = new Coin(coinModel, enemy.position.x, enemy.position.y + 1.0f, enemy.position.z);
                    activeCoins.add(c);

                }

                // 2. CEK HAPUS MEMORI (Mayat udah selesai animasi tenggelam)
                if(enemy.isReadyToRemove){
                    // Hapus kodingan spawn coin yang lama di sini, biar gak dobel
                    activeEnemies.removeIndex(i);
                    continue;
                }
            }

            for(int i = activeCoins.size - 1; i >= 0; i--) {
                Coin c = activeCoins.get(i);
                c.update(delta); // Muterin koin

                // Cek Jarak Player ke Koin (1.5 meter)
                if (cam.position.dst(c.position) < 1.5f) {
                    activeCoins.removeIndex(i); // Hapus koin
                    score += 10; // Nambah skor
                    // Update UI Skor disini kalo mau, misal:
                    // enemyCountLabel.setText("LEFT: " + ... + " | SCORE: " + score);
                    System.out.println("SCORE: " + score);
                }
            }

            // WEAPON INPUT (Punya Kamu)
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) playerWeapon = inventory.get(0);
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) playerWeapon = inventory.get(1);
            if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                if (playerWeapon != null) {
                    playerWeapon.reload();
                }
            }
            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) shoot();
            if (playerWeapon != null) {
                // Ini "baterai"-nya. Biar waktu reload-nya jalan.
                playerWeapon.update(delta);
            }


            if (bulletTracerTimer > 0) bulletTracerTimer -= delta;

        } else {
            updateUI();
        }

        cam.update();

        if(hitMarkerTimer > 0) hitMarkerTimer -= delta;

        // RENDER START
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

        // RENDER MUSUH (Yajuj Majuj)
        for(BaseEnemy enemy : activeEnemies) modelBatch.render(enemy.modelInstance, env);
        for(Coin c : activeCoins) {
            modelBatch.render(c.instance, env);
        }

        // Render Senjata (ViewModel)
        if (playerWeapon != null && playerWeapon.viewModel != null) {
            playerWeapon.setView(cam);
            modelBatch.render(playerWeapon.viewModel);
        }

        modelBatch.flush();

        // Render Fog (Transparent)
        Gdx.gl.glDepthMask(false);
        for(ModelInstance fog : fogInstances) modelBatch.render(fog);
        Gdx.gl.glDepthMask(true);

        modelBatch.end();



        // Render Bullet Tracer
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


        // Render UI
        stage.act(delta);

        // Crosshair
        uiBatch.begin();
        uiBatch.setColor(Color.WHITE);
        uiBatch.draw(crosshairTex, Gdx.graphics.getWidth() / 2f - 16, Gdx.graphics.getHeight() / 2f - 16, 32, 32);
        if(hitMarkerTimer > 0){
            uiBatch.setColor(Color.RED); // Ubah warna jadi merah
            // Gambar agak miring atau lebih kecil di tengah crosshair
            uiBatch.draw(hitMarkerTex, Gdx.graphics.getWidth()/2f - 12, Gdx.graphics.getHeight()/2f - 12, 24, 24);
            uiBatch.setColor(Color.WHITE); // Balikin warna normal
        }
        uiBatch.end();

        stage.draw();
    }

    private void updateMouseLook(){
        if(isPaused || !Gdx.input.isCursorCatched()) return;
        int dx = Gdx.input.getDeltaX();
        int dy = Gdx.input.getDeltaY();
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
                float nextX = cam.position.x + stepX;
                float nextZ = cam.position.z + stepZ;

                // LOGIKA BARRIER (Punya Teman)
                if(!waveManager.isStageCleared()){
                    float baseAngle = waveManager.getPlayerCurrentAngle();
                    float currentRaw = MathUtils.atan2(cam.position.z, cam.position.x);
                    float nextRaw = MathUtils.atan2(nextZ, nextX);
                    float deltaAngle = nextRaw - currentRaw;
                    if(deltaAngle < -MathUtils.PI) deltaAngle += MathUtils.PI2;
                    else if(deltaAngle > MathUtils.PI) deltaAngle -= MathUtils.PI2;
                    float predictedTotalAngle = baseAngle + deltaAngle;
                    float barrier = waveManager.getAngleBarrier();

                    // Cek tabrak barrier
                    if(predictedTotalAngle > barrier && deltaAngle > 0.0001f){
                        showBarrierWarning();
                        break;
                    }
                }

                // Logika Tabrakan Terrain & Pohon
                float probeFar = 0.6f;
                float r = (float)Math.sqrt(cam.position.x * cam.position.x + cam.position.z * cam.position.z);
                float slopeLimit = (r < 80f) ? 1.4f : 0.6f;
                float currentY = terrain.getHeight(cam.position.x, cam.position.z);
                boolean safeX = true;
                float dirX = Math.signum(move.x);
                float probeX_Far = cam.position.x + dirX * probeFar;
                if(terrain.getHeight(probeX_Far, cam.position.z) - currentY > slopeLimit) safeX = false;
                if(cekNabrakPohon(cam.position.x + stepX, cam.position.z)) safeX = false;
                if(safeX) cam.position.x += stepX;

                currentY = terrain.getHeight(cam.position.x, cam.position.z);
                boolean safeZ = true;
                float dirZ = Math.signum(move.z);
                float probeZ_Far = cam.position.z + dirZ * probeFar;
                if(terrain.getHeight(cam.position.x, probeZ_Far) - currentY > slopeLimit) safeZ = false;
                if(cekNabrakPohon(cam.position.x, cam.position.z + stepZ)) safeZ = false;
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

    private boolean cekNabrakPohon(float x, float z){
        float radiusPlayer = 0.5f;
        float radiusPohon = 0.8f;
        float jarakMinimal = radiusPlayer + radiusPohon;
        float jarakMinimalKuadrat = jarakMinimal * jarakMinimal;
        for (ModelInstance tree : treeInstances){
            tree.transform.getTranslation(tempPos);
            float dx = x - tempPos.x;
            float dz = z - tempPos.z;
            if(dx * dx + dz * dz < jarakMinimalKuadrat) return true;
        }
        return false;
    }

    private void createFogSystem(Terrain terrain) {
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
        int planesCount = 1;
        float baseSize = 10f;
        for(int i = 0; i < planesCount; i++){
            Vector3 axis = new Vector3(MathUtils.random(-1f, 1f), MathUtils.random(-1f, 1f), MathUtils.random(-1f, 1f)).nor();
            float angle = MathUtils.random(0f, 360f);
            Vector3 offset = new Vector3(MathUtils.random(-1.5f, 1.5f), MathUtils.random(-1.5f, 1.5f), MathUtils.random(-1.5f, 1.5f));
            Vector3 p1 = new Vector3(-baseSize, -baseSize, 0);
            Vector3 p2 = new Vector3( baseSize, -baseSize, 0);
            Vector3 p3 = new Vector3( baseSize,  baseSize, 0);
            Vector3 p4 = new Vector3(-baseSize,  baseSize, 0);
            Vector3 normal = new Vector3(0, 0, 1);
            p1.rotate(axis, angle).add(offset);
            p2.rotate(axis, angle).add(offset);
            p3.rotate(axis, angle).add(offset);
            p4.rotate(axis, angle).add(offset);
            normal.rotate(axis, angle);
            meshBuilder.rect(p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, p3.x, p3.y, p3.z, p4.x, p4.y, p4.z, normal.x, normal.y, normal.z);
        }
        fogModel = modelBuilder.end();
        for(int i = 0; i < fogCount; i++){
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

    // --- LOGIKA SHOOTING & DAMAGE ---
    private void shoot() {
        if (playerWeapon == null) return;
        if (playerWeapon.isReloading) {
            System.out.println("Tahan bos, lagi isi peluru!");
            return;
        }
        // Kalau peluru di magazine abis
        if (playerWeapon.ammoInClip <= 0) {
            System.out.println("Klik! Peluru abis. Tekan R buat reload!");
            // Opsional: Play sound "klik" kosong disini
            return;
        }
        // Kurangi peluru (Penting!)
//        playerWeapon.ammoInClip--;
        playerWeapon.shoot();
        cam.update();

        // 1. Setup Raycast dari tengah layar
        Ray ray = cam.getPickRay(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f);

        // 2. Setup Visual Tracer (ke jarak jauh dulu defaultnya)
        bulletDest.set(ray.direction).scl(100f).add(ray.origin);

        // 3. LOGIKA HIT SCAN
        BaseEnemy hitEnemy = null;
        float closestDist = Float.MAX_VALUE;

        // Loop semua musuh aktif (Yajuj Majuj)
        for(BaseEnemy enemy : activeEnemies){
            if(enemy.isDead) continue;
            tempHitCenter.set(enemy.position);
            tempHitCenter.y += 2.8f;

            Vector3 enemyPos = enemy.position;
            float radius = 1.0f; // Bounding sphere sederhana

            if (Intersector.intersectRaySphere(ray, tempHitCenter, 0.8f, null)) {
                float dist = cam.position.dst(enemy.position);
                if(dist < closestDist){
                    closestDist = dist;
                    hitEnemy = enemy;
                }
            }
        }


        // Cek kena Dajjal juga (Opsional, kalau Dajjal ada bounding boxnya)
        if(dajjal != null) {
            // Logika hit dajjal bisa ditambahkan di sini mirip kayak musuh biasa
            // Misalnya: if (Intersector.intersectRaySphere(ray, dajjal.position, 5.0f, null)) { ... }
        }

        // Kalau ada yang kena
        if(hitEnemy != null){
            float damage = (playerWeapon instanceof com.finpro7.oop.world.weapon.AkRifle) ? 15f : 10f;

            // 1. Panggil fungsi sakit
            hitEnemy.takeDamage(damage, terrain);

            // 2. Trigger Hit Marker UI
            hitMarkerTimer = 0.1f; // Munculin tanda X selama 0.1 detik

            // 3. Update Tracer
            bulletDest.set(ray.direction).scl(closestDist).add(ray.origin);

            // 4. Lapor kematian
//            if(hitEnemy.isDead && !hitEnemy.countedAsDead){ // Cek flag dead dari BaseEnemy
//                waveManager.reportEnemyDeath();
//                hitEnemy.countedAsDead = true;
//            }
        }

        // Setup origin tracer dari senjata
        Vector3 camRight = cam.direction.cpy().crs(cam.up).nor();
        Vector3 camDown = camRight.cpy().crs(cam.direction).nor().scl(-1f);
        bulletOrigin.set(cam.position);
        float fwd, side, down;
        if (playerWeapon instanceof com.finpro7.oop.world.weapon.AkRifle) {
            fwd = 2.15f; side = 0.52f; down = 0.3f;
        } else {
            fwd = 1.35f; side = 0.52f; down = 0.38f;
        }
        bulletOrigin.add(camRight.scl(side));
        bulletOrigin.add(camDown.scl(down));
        bulletOrigin.add(new Vector3(cam.direction).scl(fwd));
        bulletTracerTimer = 0.03f;
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
        if(coinModel != null) coinModel.dispose();
    }
}
