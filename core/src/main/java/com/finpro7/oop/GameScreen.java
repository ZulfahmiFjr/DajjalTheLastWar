package com.finpro7.oop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
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
import com.finpro7.oop.world.Terrain;
import com.finpro7.oop.world.weapon.*;
import com.finpro7.oop.logics.WaveManager;

public class GameScreen implements Screen {

    final Main game;

    // --- managers & controllers ---
    private GameHUD hud;
    private PlayerController playerController;
    private ItemManager itemManager;
    private WaveManager waveManager;
    private EnemyFactory enemyFactory;

    // ini facade kita buat urusan gambar
    private WorldRenderer worldRenderer;

    // --- player stats & weapon ---
    private PlayerStats playerStats;
    private Firearm playerWeapon;
    private Array<Firearm> inventory = new Array<>();

    // --- world objects ---
    private Terrain terrain;
    private PerlinNoise perlin;
    private Model treeModel;
    private Array<ModelInstance> treeInstances = new Array<>();

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
    private float lightningTimer = 0f;
    private float flashIntensity = 0f;
    private final Color currentSkyColor = new Color();

    // --- shooting logic ---
    // variabel perhitungan peluru tetep di sini karena ini logic
    private Vector3 bulletOrigin = new Vector3();
    private Vector3 bulletDest = new Vector3();
    private float bulletTracerTimer = 0f;
    private final Vector3 tempHitCenter = new Vector3();
    private final Vector3 tmpExactHit = new Vector3();
    private final Vector3 lastHitPos = new Vector3();

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

        // inisialisasi renderer sebelum setup world
        worldRenderer = new WorldRenderer();

        // setup dunia 3d dan objek objeknya
        setup3DWorld();

        // inisialisasi semua manager
        hud = new GameHUD(game);
        // player controller butuh kamera dari renderer
        playerController = new PlayerController(worldRenderer.cam, playerStats);

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

        // lock cursor biar gak lari kemana mana
        Gdx.input.setCursorCatched(true);
        Gdx.input.setInputProcessor(null);
    }

    private void setupWeapons() {
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

        com.badlogic.gdx.Preferences prefs = Gdx.app.getPreferences("UserSession");
        this.score = prefs.getInteger("total_coins", 0);
    }

    private void setup3DWorld() {
        // setup noise buat terrain
        perlin = new PerlinNoise();
        perlin.amplitude = 80f;
        perlin.frequencyX = 0.08f;
        perlin.frequencyZ = 0.08f;
        perlin.offsetX = MathUtils.random(0f, 999f);
        perlin.offsetZ = MathUtils.random(0f, 999f);

        treeModel = game.assets.get("models/pohon.g3dj", Model.class);

        // bikin terrain, environmentnya ambil dari renderer
        terrain = new Terrain(worldRenderer.getEnvironment(), perlin, 254, 254, 320f, 320f);

        // bikin sistem kabut lewat renderer
        worldRenderer.createFogSystem(terrain);

        // atur transparansi pohon
        for(com.badlogic.gdx.graphics.g3d.Material mat : treeModel.materials){
            // logic cari material index 1 (daun)
            // di sini disederhanakan accessnya
            mat.set(
                new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA),
                FloatAttribute.createAlphaTest(0.25f),
                IntAttribute.createCullFace(GL20.GL_NONE)
            );
        }
        terrain.generateTrees(treeModel, treeInstances, 600);

        Vector3 startPos = new Vector3();
        terrain.getRoadStartPos(startPos);
        worldRenderer.cam.position.set(startPos.x + 5.0f, startPos.y + 2.0f, startPos.z + 5.0f);

        Vector3 lookTarget = new Vector3();
        terrain.getRoadLookAtPos(lookTarget);
        worldRenderer.cam.direction.set(lookTarget.sub(worldRenderer.cam.position)).nor();
        worldRenderer.cam.up.set(Vector3.Y);
        worldRenderer.cam.update();
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
            hud.updatePauseInfo(missionTimer, worldRenderer.cam.position);
        }

        // panggil renderer facade buat gambar semuanya
        // parameternya emang agak banyak tapi cuma satu baris ini doang
        worldRenderer.render(
            delta, terrain, treeInstances, activeEnemies,
            itemManager, playerWeapon,
            bulletOrigin, bulletDest, bulletTracerTimer
        );

        // render ui 2d tetep di gamescreen karena hud sifatnya overlay
        renderUI(delta);
    }

    private void updateGameLogic(float delta) {
        // update player controller
        playerController.update(delta, terrain, treeInstances, waveManager, isPaused);

        // cek kondisi game over
        if (playerStats.isDead() && !isGameOver) {
            triggerGameOver();
        }

        // update items, butuh posisi kamera
        itemManager.update(delta, worldRenderer.cam.position, playerWeapon);

        // update wave dan stage
        waveManager.update(delta, worldRenderer.cam.position, terrain, enemyFactory, activeEnemies);
        if (waveManager.justChangedStage) {
            hud.showStageNotification(waveManager.getCurrentStageNum());
            waveManager.justChangedStage = false;
        }

        // logic spawn dajjal
        handleBossSpawn(delta);

        // logic boss fight (atmosfer & win condition)
        handleBossFightLogic(delta);

        worldRenderer.cam.update();
        missionTimer += delta;

        // update musuh
        updateEnemies(delta);

        // update senjata
        updateWeapon(delta);
    }

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
                        // efek kamera goyang dikit
                        worldRenderer.cam.position.add(MathUtils.random(-0.1f, 0.1f), MathUtils.random(-0.1f, 0.1f), 0);
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

                // efek petir dengan ngubah warna di renderer
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

                // update environment di renderer
                Environment env = worldRenderer.getEnvironment();
                env.set(new ColorAttribute(ColorAttribute.AmbientLight, currentSkyColor));
                env.set(new ColorAttribute(ColorAttribute.Fog, currentSkyColor));
                DirectionalLight light = worldRenderer.getDirectionalLight();
                if (light != null) light.color.set(currentSkyColor);
            }
        } else {
            // mode normal balikin environment default
            // ini sebenernya agak redudant karena di init udah diset, tapi jaga jaga
            // lebih rapih kalau ada method resetEnvironment() di renderer
        }
    }

    private void updateEnemies(float delta) {
        for (int i = activeEnemies.size - 1; i >= 0; i--) {
            BaseEnemy enemy = activeEnemies.get(i);
            // oper posisi kamera dari renderer
            enemy.update(delta, worldRenderer.cam.position, terrain, treeInstances, activeEnemies, playerStats);

            if (enemy.isDead && !enemy.countedAsDead) {
                waveManager.reportEnemyDeath();
                enemy.countedAsDead = true;

                itemManager.spawnItem(enemy.position.x, enemy.position.y + 1.0f, enemy.position.z);
            }

            if (enemy.isReadyToRemove) {
                activeEnemies.removeIndex(i);
            }
        }
    }

    private void updateWeapon(float delta) {
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
        hud.render(playerStats, worldRenderer.cam, delta);
    }

    private void shoot() {
        if (playerWeapon == null || playerWeapon.isReloading) return;
        playerWeapon.shoot();
        worldRenderer.cam.update();

        Ray ray = worldRenderer.cam.getPickRay(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f);
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
                float dist = worldRenderer.cam.position.dst(tmpExactHit);
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

            Vector3 spawnPos = new Vector3(lastHitPos);
            spawnPos.mulAdd(worldRenderer.cam.direction, -0.5f);
            hud.addHitMarker(spawnPos);

            bulletDest.set(ray.direction).scl(closestDist).add(ray.origin);
        }

        Vector3 camRight = worldRenderer.cam.direction.cpy().crs(worldRenderer.cam.up).nor();
        Vector3 camDown = camRight.cpy().crs(worldRenderer.cam.direction).nor().scl(-1f);
        bulletOrigin.set(worldRenderer.cam.position);
        float fwd = (playerWeapon instanceof AkRifle) ? 2.15f : 1.35f;
        float side = 0.52f;
        float down = (playerWeapon instanceof AkRifle) ? 0.3f : 0.38f;

        bulletOrigin.add(camRight.scl(side));
        bulletOrigin.add(camDown.scl(down));
        bulletOrigin.add(new Vector3(worldRenderer.cam.direction).scl(fwd));
        bulletTracerTimer = 0.03f;
    }

    private void togglePause() {
        isPaused = !isPaused;
        hud.setPauseVisible(isPaused);
        if (isPaused) {
            Gdx.input.setCursorCatched(false);
            Gdx.input.setInputProcessor(hud.stage);
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
        worldRenderer.resize(width, height);
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
        // renderer facade yang handle dispose batch dll
        if (worldRenderer != null) worldRenderer.dispose();
        if (hud != null) hud.dispose();
        if (itemManager != null) itemManager.dispose();
    }
}
