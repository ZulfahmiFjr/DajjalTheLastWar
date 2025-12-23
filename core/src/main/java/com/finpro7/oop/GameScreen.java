package com.finpro7.oop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.*;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.utils.Array;

import com.finpro7.oop.entities.*;
import com.finpro7.oop.managers.*;
import com.finpro7.oop.world.PerlinNoise;
import com.finpro7.oop.world.weapon.*;
import com.finpro7.oop.logics.WaveManager;
import com.finpro7.oop.world.Terrain;

public class GameScreen implements Screen {

    final Main game;

    // --- managers & controllers ---
    private GameHUD hud;
    private PlayerController playerController;
    private ItemManager itemManager;
    private WaveManager waveManager;
    private EnemyFactory enemyFactory;

    // --- player stats & weapon ---
    private PlayerStats playerStats;
    private Firearm playerWeapon;
    private Array<Firearm> inventory = new Array<>();

    // --- 3d rendering core ---
    private PerspectiveCamera cam;
    private Environment env;
    private RenderContext renderContext;
    private ModelBatch modelBatch;
    private DirectionalLight dirLight;

    // --- world objects ---
    private Terrain terrain;
    private PerlinNoise perlin;
    private Model treeModel;
    private Array<ModelInstance> treeInstances = new Array<>();
    private Model fogModel;
    private Array<ModelInstance> fogInstances = new Array<>();

    // --- entities ---
    private Array<BaseEnemy> activeEnemies = new Array<>();
    private DajjalEntity dajjal;

    // --- game state ---
    private boolean isPaused = false;
    private boolean isGameOver = false;
    private boolean isGameWon = false;

    // --- timers & cooldowns ---
    private float missionTimer = 0f;
    private float victoryTimer = 0f;
    private float bossSpawnTimer = 0f;
    private boolean bossSpawnSequenceStarted = false;

    // --- atmosfer dajjal ---
    private final Color BASE_DARK = new Color(0.01f, 0.01f, 0.02f, 1f);
    private final Color LIGHTNING_COLOR = new Color(0.8f, 0.85f, 1f, 1f);
    private final Color NORMAL_AMBIENT = new Color(0.6f, 0.6f, 0.6f, 1f);
    private final Color NORMAL_FOG = new Color(0.08f, 0.1f, 0.14f, 1f);
    private float lightningTimer = 0f;
    private float flashIntensity = 0f;
    private final Color currentSkyColor = new Color();

    // --- shooting logic ---
    private ShapeRenderer shapeRenderer; // buat gambar garis peluru
    private Vector3 bulletOrigin = new Vector3();
    private Vector3 bulletDest = new Vector3();
    private float bulletTracerTimer = 0f;
    private final Vector3 tempHitCenter = new Vector3();
    private final Vector3 tmpExactHit = new Vector3();
    private final Vector3 lastHitPos = new Vector3();

    // --- sistem Kabut ---
    private float fogSpeed = 2.0f;
    private int fogCount = 200;

    // --- player stats ---
    private int score = 0;

    public GameScreen(final Main game) {
        this.game = game;

        // inisialisasi statistik player
        playerStats = new PlayerStats();
        playerStats.staminaDrainSprint = 12f;
        playerStats.staminaRegenWalk = 6f;
        playerStats.staminaRegenIdle = 10f;

        // setup senjata awal
        setupWeapons();

        // setup dunia 3d dan objek objeknya
        setup3DWorld();

        // inisialisasi semua manager
        hud = new GameHUD(game);
        playerController = new PlayerController(cam, playerStats);

        // listener buat nyambungin tombol resume di HUD ke logic pause di sini
        hud.getResumeButton().addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                togglePause(); // panggil method pause yg ada di GameScreen ini
            }
        });

        // listener buat notifikasi barrier dari player controller
        playerController.setWarningListener(new PlayerController.WarningListener() {
            @Override
            public void onBarrierHit() {
                hud.showBarrierWarning(waveManager.getRemainingEnemies());
            }
        });

        // setup enemy system
        Model yajujM = game.assets.get("models/yajuj/yajuj.g3db", Model.class);
        Model majujM = game.assets.get("models/majuj/majuj.g3db", Model.class);
        Model dajjalM = game.assets.get("models/dajjal.g3db", Model.class);
        enemyFactory = new EnemyFactory(yajujM, majujM, dajjalM);

        waveManager = new WaveManager();
        waveManager.initLevelData(terrain);

        // setup item manager dan listener duitnya
        setupItemSystem();

        // setup rendering tambahan
        shapeRenderer = new ShapeRenderer();

        // lock cursor biar gak lari kemana mana
        Gdx.input.setCursorCatched(true);
        Gdx.input.setInputProcessor(null);
    }

    // misahin setup senjata biar rapi
    private void setupWeapons() {
//        inventory.add(com.finpro7.oop.world.weapon.Pistol.generateDefault());
//        inventory.add(com.finpro7.oop.world.weapon.Pistol.generateSniperPistol());
        com.badlogic.gdx.Preferences p = Gdx.app.getPreferences("UserSession");
        int pistolId = p.getInteger("equipped_pistol_id", 0);

        // rakit pistolnya
        inventory.add(com.finpro7.oop.world.weapon.Pistol.assembleType(pistolId));

        // cek ak-47
        if (p.getBoolean("has_ak", false)) {
            inventory.add(AkRifle.generateDefault());
        }
        playerWeapon = inventory.get(0);
    }

    // misahin setup item manager
    private void setupItemSystem() {
        // load model item
        Model coinM = new ModelBuilder().createCylinder(1f, 0.1f, 1f, 20,
            new Material(ColorAttribute.createDiffuse(Color.GOLD)),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        Model healthM = game.assets.get("models/medkit.g3db", Model.class);
        Model ammoM = game.assets.get("models/ammo.g3db", Model.class);

        itemManager = new ItemManager(coinM, healthM, ammoM, playerStats);

        // listener update ui pas dapet item
        itemManager.setListener(new ItemManager.ItemListener() {
            @Override
            public void onCoinCollected(int amount) {}
            @Override
            public void onHealthCollected(int amount) {}
            @Override
            public void onAmmoCollected(int amount) {}
        });

        // balikin score dari memori pas awal main
        com.badlogic.gdx.Preferences prefs = Gdx.app.getPreferences("UserSession");
        this.score = prefs.getInteger("total_coins", 0);
    }

    private void setup3DWorld() {
        cam = new PerspectiveCamera(67f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.near = 0.1f;
        cam.far = 200f;

        env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, NORMAL_AMBIENT));
        dirLight = new DirectionalLight().set(1f, 1f, 1f, -0.6f, -1f, -0.3f);
        env.add(dirLight);
        env.set(new ColorAttribute(ColorAttribute.Fog, NORMAL_FOG));

        renderContext = new RenderContext(new DefaultTextureBinder(DefaultTextureBinder.LRU, 1));
        modelBatch = new ModelBatch();

        perlin = new PerlinNoise();
        perlin.amplitude = 80f;
        perlin.frequencyX = 0.08f;
        perlin.frequencyZ = 0.08f;
        perlin.offsetX = MathUtils.random(0f, 999f);
        perlin.offsetZ = MathUtils.random(0f, 999f);

        treeModel = game.assets.get("models/pohon.g3dj", Model.class);

        terrain = new Terrain(env, perlin, 254, 254, 320f, 320f);
        createFogSystem(terrain); // fog dipisah di helper method bawah

        // atur transparansi pohon
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
        cam.position.set(startPos.x + 5.0f, startPos.y + 2.0f, startPos.z + 5.0f);

        // posisi start debug di stage 6
//        float debugX = 40f;
//        float debugZ = 0f;
//        float debugY = terrain.getHeight(debugX, debugZ);
//        cam.position.set(debugX, debugY + 2.0f, debugZ);

        Vector3 lookTarget = new Vector3();
        terrain.getRoadLookAtPos(lookTarget);
        cam.direction.set(lookTarget.sub(cam.position)).nor();
        cam.up.set(Vector3.Y);
        cam.update();
    }

    // ini helper buat create fog (sama kyak sebelumnya)
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

    private Texture createProceduralFogTexture(int size) {
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(size, size, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        PerlinNoise texNoise = new PerlinNoise();
        texNoise.frequencyX = 0.07f;
        texNoise.frequencyZ = 0.07f;
        texNoise.offsetX = MathUtils.random(0, 1000f);
        texNoise.offsetZ = MathUtils.random(0, 1000f);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
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

    // helper update fog
    private void updateFog(float delta) {
        for (ModelInstance fog : fogInstances) {
            Vector3 pos = fog.transform.getTranslation(new Vector3());
            pos.x += fogSpeed * delta;
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

    @Override
    public void render(float delta) {
        // handle tombol escape buat pause
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) togglePause();

        if (!isPaused) {
            // update semua logika game
            updateGameLogic(delta);
        } else {
            // kalo pause cuma update info di menu
            hud.updatePauseInfo(missionTimer, cam.position);
        }

        // render scene 3d
        render3DScene();

        // render ui 2d
        renderUI(delta);
    }

    // misahin logika update biar render gak penuh sesak
    private void updateGameLogic(float delta) {
        // update player controller
        playerController.update(delta, terrain, treeInstances, waveManager, isPaused);

        // cek kondisi game over
        if (playerStats.isDead() && !isGameOver) {
            triggerGameOver();
        }

        // update items
        itemManager.update(delta, cam.position, playerWeapon);

        // update wave dan stage
        waveManager.update(delta, cam.position, terrain, enemyFactory, activeEnemies);
        if (waveManager.justChangedStage) {
            hud.showStageNotification(waveManager.getCurrentStageNum());
            waveManager.justChangedStage = false;
        }

        // logic spawn dajjal
        handleBossSpawn(delta);

        // logic boss fight (atmosfer & win condition)
        handleBossFightLogic(delta);

        cam.update();
        updateFog(delta);
        missionTimer += delta;

        // update musuh
        updateEnemies(delta);

        // update senjata
        updateWeapon(delta);
    }

    // logika spawn boss yang dramatis
    private void handleBossSpawn(float delta) {
        if (waveManager.getCurrentStageNum() > 6) {
            if (dajjal == null) {
                if (!bossSpawnSequenceStarted) {
                    System.out.println("WARNING: BOSS INCOMING...");
                    hud.getNotificationLabel().setText("WARNING: HUGE ENERGY DETECTED!\nFINAL BOSS APPROACHING...");
                    hud.getNotificationLabel().setColor(Color.RED);
                    hud.getNotificationLabel().clearActions();
                    hud.getNotificationLabel().addAction(Actions.sequence(
                        Actions.fadeIn(0.5f),
                        Actions.delay(4.0f),
                        Actions.fadeOut(1.0f)
                    ));

                    bossSpawnSequenceStarted = true;
                    bossSpawnTimer = 4.0f;
                }

                if (bossSpawnTimer > 0) {
                    bossSpawnTimer -= delta;
                    if (bossSpawnTimer < 2.0f) {
                        cam.position.add(MathUtils.random(-0.1f, 0.1f), MathUtils.random(-0.1f, 0.1f), 0);
                    }
                } else {
                    float bossX = 0f;
                    float bossZ = 0f;
                    dajjal = (DajjalEntity) enemyFactory.spawnDajjal(bossX, bossZ, terrain);
                    activeEnemies.add(dajjal);

                    hud.getNotificationLabel().setText("DAJJAL HAS ARRIVED!");
                    hud.getNotificationLabel().setColor(Color.RED);
                    hud.getNotificationLabel().clearActions();
                    hud.getNotificationLabel().addAction(Actions.sequence(Actions.fadeIn(0.1f), Actions.delay(2f), Actions.fadeOut(1f)));
                }
            }
        }
    }

    // logika pas lawan boss
    private void handleBossFightLogic(float delta) {
        if (dajjal != null) {
            if (dajjal.isDead && !isGameWon) {
                victoryTimer += delta;
                hud.getBossUiTable().setVisible(false);
                if (victoryTimer > 2.0f) {
                    triggerVictory();
                }
            } else if (!dajjal.isDead) {
                hud.getBossUiTable().setVisible(true);
                float hpPercent = dajjal.health / dajjal.maxHealth;
                hud.getBossHealthBar().setValue(hpPercent);

                // efek petir
                if (flashIntensity > 0) {
                    flashIntensity -= delta * 2.5f;
                    if (MathUtils.randomBoolean(0.3f)) flashIntensity = MathUtils.random(0.5f, 1.0f);
                    if (flashIntensity < 0) flashIntensity = 0;
                }
                lightningTimer -= delta;
                if (lightningTimer <= 0) {
                    lightningTimer = MathUtils.random(2.0f, 8.0f);
                    flashIntensity = 1.0f;
                }
                currentSkyColor.set(BASE_DARK).lerp(LIGHTNING_COLOR, flashIntensity);
                env.set(new ColorAttribute(ColorAttribute.AmbientLight, currentSkyColor));
                env.set(new ColorAttribute(ColorAttribute.Fog, currentSkyColor));
                if (dirLight != null) dirLight.color.set(currentSkyColor);
            }
        } else {
            // mode normal
            env.set(new ColorAttribute(ColorAttribute.AmbientLight, NORMAL_AMBIENT));
            env.set(new ColorAttribute(ColorAttribute.Fog, NORMAL_FOG));
            if (dirLight != null) dirLight.color.set(1f, 1f, 1f, 1f);
            hud.getBossUiTable().setVisible(false);
        }
    }

    // update semua musuh
    private void updateEnemies(float delta) {
        for (int i = activeEnemies.size - 1; i >= 0; i--) {
            BaseEnemy enemy = activeEnemies.get(i);
            enemy.update(delta, cam.position, terrain, treeInstances, activeEnemies, playerStats);

            if (enemy.isDead && !enemy.countedAsDead) {
                waveManager.reportEnemyDeath();
                enemy.countedAsDead = true;

                // spawn item pake manager baru
                itemManager.spawnItem(enemy.position.x, enemy.position.y + 1.0f, enemy.position.z);
            }

            if (enemy.isReadyToRemove) {
                activeEnemies.removeIndex(i);
            }
        }
    }

    // update logika senjata
    private void updateWeapon(float delta) {
        // ganti senjata
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) playerWeapon = inventory.get(0);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2) && inventory.size > 1) playerWeapon = inventory.get(1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.R) && playerWeapon != null) playerWeapon.reload();

        if (playerWeapon != null) {
            if (playerWeapon.noAutoWaitTime > 0) playerWeapon.noAutoWaitTime -= delta;

            boolean mauNembak = false;
            if (playerWeapon instanceof com.finpro7.oop.world.weapon.AkRifle) {
                if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) mauNembak = true;
            } else {
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) mauNembak = true;
            }

            if (mauNembak && playerWeapon.noAutoWaitTime <= 0) {
                shoot();
                playerWeapon.noAutoWaitTime = (playerWeapon instanceof AkRifle) ? 0.1f : 0.2f;
            }
            playerWeapon.update(delta);
        }

        if (bulletTracerTimer > 0) bulletTracerTimer -= delta;
    }

    // render dunia 3d
    private void render3DScene() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.08f, 0.1f, 0.14f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);

        renderContext.begin();
        terrain.render(cam, renderContext);
        renderContext.end();

        modelBatch.begin(cam);
        for (ModelInstance tree : treeInstances) modelBatch.render(tree, env);
        for (BaseEnemy enemy : activeEnemies) modelBatch.render(enemy.modelInstance, env);

        // render item pake manager
        itemManager.render(modelBatch, env);

        if (playerWeapon != null && playerWeapon.viewModel != null) {
            playerWeapon.setView(cam);
            modelBatch.render(playerWeapon.viewModel);
        }
        modelBatch.flush();

        // render fog
        Gdx.gl.glDepthMask(false);
        for (ModelInstance fog : fogInstances) modelBatch.render(fog);
        Gdx.gl.glDepthMask(true);
        modelBatch.end();

        // render garis peluru
        if (bulletTracerTimer > 0) {
            shapeRenderer.setProjectionMatrix(cam.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.line(
                bulletOrigin.x, bulletOrigin.y, bulletOrigin.z,
                bulletDest.x, bulletDest.y, bulletDest.z,
                Color.WHITE,
                Color.YELLOW
            );
            shapeRenderer.end();
        }
    }

    // render ui terakhir
    private void renderUI(float delta) {
        String ammoText = "AMMO: -- / --";
        if (playerWeapon != null) {
            ammoText = "AMMO: " + playerWeapon.ammoInClip + " / " + playerWeapon.totalAmmo;
            if (playerWeapon.ammoInClip == 0) hud.setAmmoColor(Color.RED);
            else if (playerWeapon.isReloading) {
                ammoText = "RELOADING...";
                hud.setAmmoColor(Color.YELLOW);
            } else hud.setAmmoColor(Color.LIGHT_GRAY);
        }

        hud.update(delta, waveManager.getCurrentStageNum(), waveManager.getRemainingEnemies(), waveManager.getTotalEnemiesInStage(), playerStats.currentCoins, ammoText);
        hud.render(playerStats, cam, delta);
    }

    // logika tembak menembak
    private void shoot() {
        if (playerWeapon == null || playerWeapon.isReloading) return;
        playerWeapon.shoot();
        cam.update();

        Ray ray = cam.getPickRay(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f);
        bulletDest.set(ray.direction).scl(100f).add(ray.origin);

        BaseEnemy hitEnemy = null;
        float closestDist = Float.MAX_VALUE;

        for (BaseEnemy enemy : activeEnemies) {
            if (enemy.isDead) continue;
            float radiusHit = (enemy instanceof DajjalEntity) ? 4.0f : 0.8f;
            float heightOffset = (enemy instanceof DajjalEntity) ? 6.0f : 2.8f;

            tempHitCenter.set(enemy.position);
            tempHitCenter.y += heightOffset;

            if (Intersector.intersectRaySphere(ray, tempHitCenter, radiusHit, tmpExactHit)) {
                float dist = cam.position.dst(tmpExactHit);
                if (dist < closestDist) {
                    closestDist = dist;
                    hitEnemy = enemy;
                    lastHitPos.set(tmpExactHit);
                }
            }
        }

        if (hitEnemy != null) {
            float damage = (playerWeapon instanceof AkRifle) ? 15f : 10f;
            hitEnemy.takeDamage(damage, terrain);
            ray.getEndPoint(lastHitPos, closestDist);

            // geser hit marker dikit ke arah player biar gak mendem
            Vector3 spawnPos = new Vector3(lastHitPos);
            spawnPos.mulAdd(cam.direction, -0.5f);
            hud.addHitMarker(spawnPos);

            bulletDest.set(ray.direction).scl(closestDist).add(ray.origin);
        }

        // setup tracer
        Vector3 camRight = cam.direction.cpy().crs(cam.up).nor();
        Vector3 camDown = camRight.cpy().crs(cam.direction).nor().scl(-1f);
        bulletOrigin.set(cam.position);
        float fwd = (playerWeapon instanceof AkRifle) ? 2.15f : 1.35f;
        float side = 0.52f;
        float down = (playerWeapon instanceof AkRifle) ? 0.3f : 0.38f;

        bulletOrigin.add(camRight.scl(side));
        bulletOrigin.add(camDown.scl(down));
        bulletOrigin.add(new Vector3(cam.direction).scl(fwd));
        bulletTracerTimer = 0.03f;
    }

    private void togglePause() {
        isPaused = !isPaused;
        hud.setPauseVisible(isPaused);
        if (isPaused) {
            Gdx.input.setCursorCatched(false);
            Gdx.input.setInputProcessor(hud.stage); // pake stage nya hud
        } else {
            Gdx.input.setCursorCatched(true);
            Gdx.input.setInputProcessor(null);
        }
    }

    private void triggerVictory() {
        if (isPaused || isGameWon) return;
        isGameWon = true;
        isPaused = true;

        playerStats.addCoins(100);

        Gdx.input.setCursorCatched(false);
        Gdx.input.setInputProcessor(hud.stage);
        hud.showVictory();
    }

    private void triggerGameOver() {
        if (isPaused) return;
        isPaused = true;
        isGameOver = true;

        Gdx.input.setCursorCatched(false);
        Gdx.input.setInputProcessor(hud.stage);
        hud.showGameOver();
    }

    @Override
    public void resize(int width, int height) {
        cam.viewportWidth = width;
        cam.viewportHeight = height;
        cam.update();
        hud.resize(width, height);
    }

    @Override
    public void show() {
        com.badlogic.gdx.Preferences prefs = Gdx.app.getPreferences("UserSession");
        this.score = prefs.getInteger("total_coins", 0);
        Gdx.input.setInputProcessor(null);
        Gdx.input.setCursorCatched(true);
    }

    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void dispose() {
        if (terrain != null) terrain.dispose();
        if (modelBatch != null) modelBatch.dispose();
        if (fogModel != null) fogModel.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (hud != null) hud.dispose();
        if (itemManager != null) itemManager.dispose();
    }
}
