package com.finpro7.oop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.math.MathUtils;
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

public class MenuScreen implements Screen {

    final Main game;
    private Stage stage;

    private PerspectiveCamera cam;
    private Environment env;
    private ModelBatch modelBatch;
    private RenderContext renderContext;
    private Terrain terrain;
    private PerlinNoise perlin;
    private Model treeModel;
    private Array<ModelInstance> treeInstances = new Array<>();
    private float camTimer = 0f;
    private final Color SKY_COLOR = new Color(0.5f, 0.6f, 0.7f, 1f);

    public MenuScreen(final Main game) {
        this.game = game;

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        setupBackgroundWorld();
        setupUI();
    }

    private void setupUI() {
        Table rootTable = new Table();
        rootTable.setFillParent(true);

        com.badlogic.gdx.Preferences prefs = Gdx.app.getPreferences("UserSession");
        String username = prefs.getString("current_user", "UNKNOWN");
        int totalCoins = prefs.getInteger("total_coins", 0);

        TextureRegionDrawable panelDrawable = createColorDrawable(new Color(0f, 0f, 0f, 0.75f));
        TextureRegionDrawable lineDrawable = createColorDrawable(new Color(1f, 1f, 1f, 0.5f));

        // --- 1. TABLE JUDUL (DAJJAL) ---
        Table titleTable = new Table();
        titleTable.setBackground(panelDrawable);
        titleTable.pad(30);

        Label title = new Label("DAJJAL", Main.skin, "title");
        title.setFontScale(6.0f);
        Label subtitle = new Label("THE LAST WAR", Main.skin, "subtitle");
        subtitle.setColor(Color.ORANGE);
        subtitle.setFontScale(1.5f);
        Image separator = new Image(lineDrawable);
        Label deptLabel = new Label("DEPARTEMEN TEKNIK KOMPUTER\nFAKULTAS TEKNIK UI", Main.skin, "text");
        deptLabel.setColor(Color.CYAN);

        titleTable.add(title).left().row();
        titleTable.add(subtitle).left().padBottom(10).row();
        titleTable.add(separator).growX().height(3).padBottom(10).row();
        titleTable.add(deptLabel).left();

        // --- 2. TABLE INFO PLAYER (DI BAWAH JUDUL - SUDAH DIPERBESAR) ---
        Table playerInfoTable = new Table();
        playerInfoTable.setBackground(panelDrawable);
        playerInfoTable.pad(20); // Pad-nya dibesarin biar kotak info lebih luas

        // Username pake warna putih biar bersih
        Label nameLabel = new Label("USERNAME: " + username.toUpperCase(), Main.skin, "subtitle");
        nameLabel.setFontScale(1.2f); // Digedein dari 0.8f ke 1.2f
        nameLabel.setColor(Color.WHITE);

        // Koin pake warna Emas biar mewah
        Label coinsLabel = new Label("WALLET: " + totalCoins + " COINS", Main.skin, "subtitle");
        coinsLabel.setFontScale(1.2f); // Digedein biar lurus sama username
        coinsLabel.setColor(Color.GOLD);

        playerInfoTable.add(nameLabel).left().row();
        playerInfoTable.add(coinsLabel).left().padTop(8);

        // --- 3. TABLE MENU (KANAN BAWAH) ---
        Table menuTable = new Table();
        menuTable.setBackground(panelDrawable);
        menuTable.pad(40);
        // ... sisa tombol tetep sama kyak punya abang ...
        TextButton btnStart = new TextButton("DEPLOY MISSION", Main.skin, "btn-main");
        TextButton btnShop = new TextButton("BLACK MARKET", Main.skin, "btn-main");
        TextButton btnLogout = new TextButton("LOGOUT", Main.skin, "btn-main");
        TextButton btnExit = new TextButton("EXIT GAME", Main.skin, "btn-main");
        menuTable.add(btnStart).width(300).height(60).padBottom(10).row();
        menuTable.add(btnShop).width(300).height(60).padBottom(10).row();
        menuTable.add(btnLogout).width(300).height(60).padBottom(10).row();
        menuTable.add(btnExit).width(300).height(60).row();

        // --- 4. TABLE FOOTER ---
        Table footerTable = new Table();
        footerTable.setBackground(panelDrawable);
        footerTable.pad(10);
        Label verLabel = new Label("VERSION: 1.2.0 STABLE", Main.skin, "text");
        Label copyLabel = new Label(" | (C) 2025 KELOMPOK 7 OOP", Main.skin, "text");
        verLabel.setColor(Color.GRAY);
        footerTable.add(verLabel);
        footerTable.add(copyLabel);

        // ================= SUSUN ULANG ROOT TABLE =================
        rootTable.add(titleTable).top().left().pad(50).row();

        // Info Player ditaruh di bawah judul, sejajar padLeft-nya
        rootTable.add(playerInfoTable).left().padLeft(50).padTop(-20).row();

        // Footer kiri, Menu kanan
        rootTable.add(footerTable).expand().bottom().left().pad(20);
        rootTable.add(menuTable).bottom().right().pad(50);

        stage.addActor(rootTable);

        // --- LISTENER TOMBOL (Tetep sama kyak punya abang) ---
        btnStart.addListener(new ClickListener(){
            @Override public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new GameScreen(game));
            }
        });
        btnShop.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                openShopUI(); // Kita bikin fungsi ini di bawah
            }
        });
        btnLogout.addListener(new ClickListener(){
            @Override public void clicked(InputEvent event, float x, float y) { game.setScreen(new LoginScreen(game)); }
        });
        btnExit.addListener(new ClickListener(){
            @Override public void clicked(InputEvent event, float x, float y) { Gdx.app.exit(); }
        });
    }

    private void openShopUI() {
        final Table shopTable = new Table();
        shopTable.setFillParent(true);
        // Background semi-transparan gelap
        shopTable.setBackground(createColorDrawable(new Color(0, 0, 0, 0.95f)));

        // Ambil data terbaru dari memori
        com.badlogic.gdx.Preferences prefs = Gdx.app.getPreferences("UserSession");
        final int currentCoins = prefs.getInteger("total_coins", 0);
        // Cek apakah user SUDAH punya AK47?
        boolean alreadyHasAK = prefs.getBoolean("has_ak", false);

        // 1. JUDUL
        Label shopTitle = new Label("BLACK MARKET", Main.skin, "title");
        shopTitle.setColor(Color.GOLD);
        shopTitle.setFontScale(1.2f);

        // 2. INFO WALLET
        final Label walletLabel = new Label("CURRENT WALLET: " + currentCoins + " COINS", Main.skin, "subtitle");
        walletLabel.setColor(Color.GREEN);
        walletLabel.setFontScale(0.9f);

        shopTable.add(shopTitle).colspan(2).center().padBottom(5).row();
        shopTable.add(walletLabel).colspan(2).left().padLeft(100).padBottom(40).width(400).row();

        // 3. ITEM: AK-47
        final Label akLabel = new Label("AK-47 RIFLE (High Damage)", Main.skin, "subtitle");
        final TextButton btnBuyAK = new TextButton("500 COINS", Main.skin, "btn-main");

        // --- LOGIKA UI SAAT DIBUKA ---
        // Kalau dari awal udah punya, langsung matikan tombolnya
        if (alreadyHasAK) {
            akLabel.setText("AK-47 (ALREADY OWNED)");
            akLabel.setColor(Color.GRAY);
            btnBuyAK.setText("OWNED");
            btnBuyAK.setDisabled(true);
            btnBuyAK.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled); // GABISA DIKLIK
        }

        shopTable.add(akLabel).left().padLeft(100).padBottom(20).width(500);
        shopTable.add(btnBuyAK).width(200).height(50).padRight(100).padBottom(20).row();

        // 4. TOMBOL BACK
        TextButton btnClose = new TextButton("EXIT MARKET", Main.skin, "btn-main");
        shopTable.add(btnClose).colspan(2).center().padTop(50).width(300).height(60);

        // --- LOGIKA TOMBOL BELI (YANG DIPERBAIKI) ---
        btnBuyAK.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                final com.badlogic.gdx.Preferences p = Gdx.app.getPreferences("UserSession");

                // Cek lokal dulu
                if (p.getBoolean("has_ak", false)) return;

                int coins = p.getInteger("total_coins", 0);
                if(coins < 500) {
                    akLabel.setText("NOT ENOUGH COINS!");
                    akLabel.setColor(Color.RED);
                    akLabel.addAction(Actions.sequence(Actions.delay(1.5f), Actions.run(() -> {
                        akLabel.setText("AK-47 RIFLE (High Damage)");
                        akLabel.setColor(Color.WHITE);
                    })));
                    return;
                }

                // KIRIM REQUEST KE SERVER
                String username = p.getString("current_user", "");
                // Kirim JSON: username dan hasAk=true (sebagai tanda mau beli AK)
                String jsonContent = "{\"username\":\"" + username + "\", \"hasAk\":true}";

                com.badlogic.gdx.net.HttpRequestBuilder requestBuilder = new com.badlogic.gdx.net.HttpRequestBuilder();
                com.badlogic.gdx.Net.HttpRequest httpRequest = requestBuilder.newRequest()
                    .method(com.badlogic.gdx.Net.HttpMethods.POST)
                    .url("http://localhost:8081/auth/buyItem")
                    .header("Content-Type", "application/json")
                    .content(jsonContent)
                    .build();

                // Matikan tombol biar gak double click pas loading
                btnBuyAK.setDisabled(true);
                akLabel.setText("PURCHASING...");

                Gdx.net.sendHttpRequest(httpRequest, new com.badlogic.gdx.Net.HttpResponseListener() {
                    @Override
                    public void handleHttpResponse(com.badlogic.gdx.Net.HttpResponse httpResponse) {
                        final int status = httpResponse.getStatus().getStatusCode();
                        final String result = httpResponse.getResultAsString();

                        Gdx.app.postRunnable(() -> {
                            if (status == 200) {
                                // SUKSES BELI DI SERVER
                                // Update Lokal
                                int sisa = coins - 500;
                                p.putInteger("total_coins", sisa);
                                p.putBoolean("has_ak", true);
                                p.flush();

                                // Update UI
                                walletLabel.setText("CURRENT WALLET: " + sisa + " COINS");
                                akLabel.setText("AK-47 (PURCHASED!)");
                                akLabel.setColor(Color.GREEN);
                                btnBuyAK.setText("OWNED");
                                btnBuyAK.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);

                                // Refresh UI Utama
//                                setupUI();
                            } else {
                                // GAGAL (Misal uang di server ternyata beda/kurang)
                                akLabel.setText("TRANSACTION FAILED!");
                                akLabel.setColor(Color.RED);
                                btnBuyAK.setDisabled(false); // Nyalain lagi
                            }
                        });
                    }

                    @Override
                    public void failed(Throwable t) {
                        Gdx.app.postRunnable(() -> {
                            akLabel.setText("SERVER ERROR!");
                            akLabel.setColor(Color.RED);
                            btnBuyAK.setDisabled(false);
                        });
                    }

                    @Override
                    public void cancelled() {}
                });
            }
        });

        btnClose.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                shopTable.remove(); // 1. Tutup/Hapus tabel Shop
                stage.clear(); // Hapus semua tombol menu utama yang lama (yang angkanya salah)
                setupUI();     // Gambar ulang menu utama (ini akan membaca Preferences terbaru)
            }
        });

        stage.addActor(shopTable);
    }

    private void setupBackgroundWorld() {
        cam = new PerspectiveCamera(67f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.near = 1f;
        cam.far = 1000f;

        env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.6f, 1f));
        env.add(new DirectionalLight().set(0.9f, 0.9f, 0.8f, -0.2f, -1f, -0.3f));
        env.set(new ColorAttribute(ColorAttribute.Fog, SKY_COLOR.r, SKY_COLOR.g, SKY_COLOR.b, 1f));

        modelBatch = new ModelBatch();
        renderContext = new RenderContext(new DefaultTextureBinder(DefaultTextureBinder.LRU, 1));

        perlin = new PerlinNoise();
        perlin.amplitude = 100f;
        perlin.frequencyX = 0.05f;
        perlin.frequencyZ = 0.05f;
        perlin.offsetX = MathUtils.random(0f, 5000f);
        perlin.offsetZ = MathUtils.random(0f, 5000f);

        treeModel = game.assets.get("models/pohon.g3dj", Model.class);

        for(Material mat : treeModel.materials){
            mat.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA),
                FloatAttribute.createAlphaTest(0.25f),
                IntAttribute.createCullFace(GL20.GL_NONE));
        }

        terrain = new Terrain(env, perlin, 200, 200, 500f, 500f);
        terrain.generateTrees(treeModel, treeInstances, 800);
    }

    private TextureRegionDrawable createColorDrawable(Color color) {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(color);
        pm.fill();
        Texture tex = new Texture(pm);
        pm.dispose();
        return new TextureRegionDrawable(new TextureRegion(tex));
    }

    @Override
    public void render(float delta) {
        camTimer += delta * 0.04f;

        float radius = 200f;
        float camX = MathUtils.sin(camTimer) * radius;
        float camZ = MathUtils.cos(camTimer) * radius;
        float camY = terrain.getHeight(camX, camZ) + 60f;

        cam.position.set(camX, camY, camZ);
        cam.lookAt(0, -10f, 0);
        cam.update();

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(SKY_COLOR.r, SKY_COLOR.g, SKY_COLOR.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);

        renderContext.begin();
        terrain.render(cam, renderContext);
        renderContext.end();

        modelBatch.begin(cam);
        for(ModelInstance tree : treeInstances) {
            modelBatch.render(tree, env);
        }
        modelBatch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        cam.viewportWidth = width;
        cam.viewportHeight = height;
        cam.update();
    }

    @Override
    public void dispose() {
        stage.dispose();
        if(modelBatch != null) modelBatch.dispose();
        if(terrain != null) terrain.dispose();
    }

    @Override
    public void show() {
        // setiap kali balik ke menu, kita bikin ulang UInya biar datanya fresh
        stage.clear(); // bersihin stage lama
        setupUI(); // gambar ulang pake data terbaru dari Preferences
        Gdx.input.setInputProcessor(stage);
    }

    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
}
