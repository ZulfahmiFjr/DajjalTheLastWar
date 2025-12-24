package com.finpro7.oop.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.finpro7.oop.GameScreen;
import com.finpro7.oop.LoginScreen;
import com.finpro7.oop.Main;
import com.finpro7.oop.MenuScreen;
import com.finpro7.oop.entities.PlayerStats;

public class GameHUD {

    // ini panggung utamanya buat naro semua ui
    public Stage stage;
    private final Main game;

    // alat buat gambar bentuk kotak bar darah sama gambar 2d
    private ShapeRenderer shapeRenderer;
    private SpriteBatch uiBatch;

    // kumpulan label buat nampilin teks di layar
    private Label stageLabel;
    private Label enemyCountLabel;
    private Label ammoLabel;
    private Label coinLabel;
    private Label notificationLabel;

    // label buat info waktu dan lokasi pas pause
    private Label timeLabel;
    private Label coordLabel;

    // tabel tabel buat ngelompokin ui biar rapi
    private Table hudTable;
    private Table bossUiTable;
    private Table gameOverTable;
    private Table victoryTable;
    private Table pauseContainer;

    // gambar overlay hitam transparan pas pause
    private Image overlay;

    // bar darahnya si boss dajjal
    private ProgressBar bossHealthBar;

    // aset tekstur buat crosshair dan hit marker
    private Texture crosshairTex;
    private Texture hitMarkerTex;
    private TextureRegion hitWrapper;

    // variabel buat ngatur efek hit marker
    private Array<FloatingHit> activeHitEffects = new Array<>();
    private Vector3 tmpScreenPos = new Vector3();

    // buat callback dari gamescreen
    private TextButton btnResume;

    // var buat layar merah pas kena damage
    private Image damageOverlay;

    public GameHUD(Main game) {
        this.game = game;

        // inisialisasi stage dan alat gambar
        stage = new Stage(new ScreenViewport());
        shapeRenderer = new ShapeRenderer();
        uiBatch = new SpriteBatch();

        // muat gambar crosshair dari aset
        crosshairTex = ResourceManager.getInstance().assets.get("textures/crosshair.png", Texture.class);
        hitMarkerTex = crosshairTex; // pake gambar yg sama aja biar hemat
        hitWrapper = new TextureRegion(hitMarkerTex);

        // jalanin semua setup ui nya
        setupHUD();
        setupPauseInterface();
        setupGameOverUI();
        setupVictoryUI();
    }

    // ini method buat nyiapin hud utama kayak ammo, koin, dll
    private void setupHUD() {
        hudTable = new Table();
        hudTable.top().left();
        hudTable.setFillParent(true);
        hudTable.pad(20);

        // bikin label labelnya pake skin bawaan
        stageLabel = new Label("STAGE 1", Main.skin, "title");
        stageLabel.setFontScale(1.4f);
        stageLabel.setColor(Color.GOLD);

        enemyCountLabel = new Label("ENEMIES: 0", Main.skin, "subtitle");
        enemyCountLabel.setColor(Color.WHITE);

        ammoLabel = new Label("AMMO: -- / --", Main.skin, "subtitle");
        ammoLabel.setColor(Color.LIGHT_GRAY);

        coinLabel = new Label("COINS: 0", Main.skin, "subtitle");
        coinLabel.setColor(Color.GOLD);

        // susun labelnya ke dalem tabel
        hudTable.add(stageLabel).left().row();
        hudTable.add(enemyCountLabel).left().padTop(5).row();
        hudTable.add(ammoLabel).left().padTop(5).row();
        hudTable.add(coinLabel).left().padTop(5);

        // masukin tabel hud ke stage
        stage.addActor(hudTable);

        // bikin tekstur merah transparan manual pake pixmap
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1f, 0f, 0f, 0.3f); // warna merah
        pixmap.fill();
        Texture redTex = new Texture(pixmap);
        pixmap.dispose(); // buang pixmapnya biar hemat memori
        // inisialisasi imagenya
        damageOverlay = new Image(new TextureRegionDrawable(new TextureRegion(redTex)));
        damageOverlay.setFillParent(true); // menuhin satu layar
        damageOverlay.setVisible(false); // sembunyiin dulu
        damageOverlay.setTouchable(Touchable.disabled); // biar gak ngehalangin klik mouse pas nembak
        // tambah ke stage
        stage.addActor(damageOverlay);

        // siapin label notifikasi gede di tengah layar
        notificationLabel = new Label("", Main.skin, "title");
        notificationLabel.setFontScale(1.5f);
        notificationLabel.setColor(Color.RED);
        notificationLabel.setAlignment(Align.center);
        notificationLabel.setPosition(0, Gdx.graphics.getHeight() / 2f + 50f);
        notificationLabel.setWidth(Gdx.graphics.getWidth());
        notificationLabel.getColor().a = 0f; // set transparan dulu
        stage.addActor(notificationLabel);

        setupBossHealthBar();
    }

    // buat nampilin efek layar merah pas kena hit
    public void showDamageFlash() {
        if (damageOverlay == null) return;

        // reset animasinya dulu
        damageOverlay.clearActions();
        damageOverlay.setVisible(true);
        damageOverlay.setColor(1f, 0f, 0f, 0.75f); // merah 1/3 transparan

        // efek ilang pelan pelan (fade out)
        damageOverlay.addAction(Actions.sequence(
            Actions.delay(0.2f), // tahan dulu
            Actions.fadeOut(1.5f), // durasi 1.5 detik fadeoutnyaa
            Actions.visible(false)
        ));
    }

    // misahin setup bar darah boss biar gak kepanjangan
    private void setupBossHealthBar() {
        // bikin gaya bar nya
        ProgressBar.ProgressBarStyle barStyle = new ProgressBar.ProgressBarStyle();

        // bikin background barnya agak gelap transparan
        com.badlogic.gdx.scenes.scene2d.utils.Drawable background = Main.skin.newDrawable("white", new Color(0.1f, 0.1f, 0.1f, 0.6f));
        com.badlogic.gdx.scenes.scene2d.utils.Drawable knob = Main.skin.newDrawable("white", new Color(0.8f, 0f, 0f, 1f));

        // paksa tingginya biar keliatan jelas
        background.setMinHeight(20f);
        knob.setMinHeight(20f);

        barStyle.background = background;
        barStyle.knobBefore = knob;

        // inisialisasi progress barnya
        bossHealthBar = new ProgressBar(0f, 1f, 0.01f, false, barStyle);
        bossHealthBar.setValue(1f);
        bossHealthBar.setAnimateDuration(0.25f);

        // masukin ke tabel khusus boss
        bossUiTable = new Table();
        bossUiTable.top();
        bossUiTable.setFillParent(true);

        Label bossName = new Label("DAJJAL - THE LAST WAR", Main.skin, "title");
        bossName.setColor(Color.RED);
        bossName.setFontScale(1.8f);

        bossUiTable.add(bossName).padTop(15).row();
        bossUiTable.add(bossHealthBar).width(600f).padTop(10);

        // sembunyiin dulu di awal
        bossUiTable.setVisible(false);
        stage.addActor(bossUiTable);
    }

    // siapin tampilan pas lagi pause
    private void setupPauseInterface() {
        // bikin layar item transparan buat overlay
        overlay = new Image(Main.skin.newDrawable("white", new Color(0.05f, 0.05f, 0.08f, 0.85f)));
        overlay.setFillParent(true);
        overlay.setVisible(false);
        stage.addActor(overlay);

        pauseContainer = new Table();
        pauseContainer.setFillParent(true);
        pauseContainer.setVisible(false);

        // tabel info misi di kiri
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

        // tabel tombol tombol di kanan
        Table buttonTable = new Table();

        // tombol resume
        btnResume = createStyledButton("RESUME MISSION", new Runnable() {
            @Override
            public void run() {}
        });
        // kita perlu cara buat ngehubungin tombol ini ke gamescreen
        // tapi biar simpel, kita set visible nya aja disini, logic nya di gamescreen

        TextButton btnRestart = createStyledButton("RESTART SECTOR", new Runnable() {
            @Override
            public void run() {
                game.setScreen(new GameScreen(game));
            }
        });

        TextButton btnExit = createStyledButton("ABORT OPERATION", new Runnable() {
            @Override
            public void run() {
                game.setScreen(new MenuScreen(game));
            }
        });

        buttonTable.add(btnResume).width(300).height(60).padBottom(15).right().row();
        buttonTable.add(btnRestart).width(300).height(60).padBottom(15).right().row();
        buttonTable.add(btnExit).width(300).height(60).right().row();

        pauseContainer.add(infoTable).width(400).expandY().fillY().left();
        pauseContainer.add(buttonTable).expand().bottom().right().pad(50);

        stage.addActor(pauseContainer);

        // simpen referensi button biar bisa diakses listeners nya dari luar kalo perlu
        // atau kita akali logic nya nanti
    }

    public TextButton getResumeButton() {
        return btnResume;
    }

    // method bantu buat bikin tombol keren yang bisa gerak pas di hover
    public TextButton createStyledButton(String text, final Runnable action) {
        final TextButton btn = new TextButton(text, Main.skin, "btn-main");
        btn.setTransform(true);
        btn.setOrigin(Align.right);
        btn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (pointer == -1)
                    btn.addAction(Actions.parallel(Actions.scaleTo(1.05f, 1.05f, 0.1f), Actions.moveBy(-10f, 0f, 0.1f)));
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (pointer == -1)
                    btn.addAction(Actions.parallel(Actions.scaleTo(1f, 1f, 0.1f), Actions.moveBy(10f, 0f, 0.1f)));
            }
        });
        return btn;
    }

    // siapin ui game over
    private void setupGameOverUI() {
        gameOverTable = new Table();
        gameOverTable.setFillParent(true);
        gameOverTable.setVisible(false);

        // pake drawable jgn textureregiondrawable biar gak error
        com.badlogic.gdx.scenes.scene2d.utils.Drawable bg = Main.skin.newDrawable("white", new Color(0.2f, 0f, 0f, 0.85f));
        gameOverTable.setBackground(bg);

        Label failLabel = new Label("MISSION FAILED", Main.skin, "title");
        failLabel.setColor(Color.RED);
        failLabel.setFontScale(2.0f);

        Label diedLabel = new Label("YOU DIED", Main.skin, "subtitle");

        TextButton btnMenu = new TextButton("RETURN TO BASE", Main.skin, "btn-main");
        btnMenu.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MenuScreen(game));
            }
        });

        TextButton btnQuit = new TextButton("QUIT GAME", Main.skin, "btn-main");
        btnQuit.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        gameOverTable.add(failLabel).padBottom(10).row();
        gameOverTable.add(diedLabel).padBottom(50).row();
        gameOverTable.add(btnMenu).width(300).height(60).padBottom(20).row();
        gameOverTable.add(btnQuit).width(300).height(60).row();

        stage.addActor(gameOverTable);
    }

    // siapin ui kemenangan
    private void setupVictoryUI() {
        victoryTable = new Table();
        victoryTable.setFillParent(true);
        victoryTable.setVisible(false);

        // background emas gelap
        com.badlogic.gdx.scenes.scene2d.utils.Drawable bg = Main.skin.newDrawable("white", new Color(0.1f, 0.1f, 0f, 0.85f));
        victoryTable.setBackground(bg);

        Label winLabel = new Label("MISSION ACCOMPLISHED", Main.skin, "title");
        winLabel.setColor(Color.GOLD);
        winLabel.setFontScale(1.5f);

        Label subLabel = new Label("DAJJAL HAS BEEN DEFEATED", Main.skin, "subtitle");
        subLabel.setColor(Color.WHITE);

        Label rewardLabel = new Label("REWARD: +100 COINS", Main.skin, "subtitle");
        rewardLabel.setColor(Color.GREEN);
        rewardLabel.setFontScale(1.2f);

        TextButton btnMenu = new TextButton("RETURN TO BASE", Main.skin, "btn-main");
        btnMenu.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MenuScreen(game));
            }
        });

        TextButton btnQuit = new TextButton("QUIT GAME", Main.skin, "btn-main");
        btnQuit.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        victoryTable.add(winLabel).padBottom(10).row();
        victoryTable.add(subLabel).padBottom(20).row();
        victoryTable.add(rewardLabel).padBottom(50).row();
        victoryTable.add(btnMenu).width(300).height(60).padBottom(20).row();
        victoryTable.add(btnQuit).width(300).height(60).row();

        stage.addActor(victoryTable);
    }

    // update data ui setiap frame
    public void update(float delta, int stageNum, int enemiesLeft, int totalEnemies, int score, String ammoText) {
        stage.act(delta);

        stageLabel.setText("STAGE " + stageNum);
        enemyCountLabel.setText("ENEMIES: " + enemiesLeft + " / " + totalEnemies);

        // ubah warna text musuh kalo tinggal dikit
        if (enemiesLeft <= 3) enemyCountLabel.setColor(Color.RED);
        else enemyCountLabel.setColor(Color.WHITE);

        coinLabel.setText("COINS: " + score);
        ammoLabel.setText(ammoText);
    }

    // khusus buat update label ammo warnanya
    public void setAmmoColor(Color color) {
        ammoLabel.setColor(color);
    }

    // update label info pas pause
    public void updatePauseInfo(float missionTimer, Vector3 camPos) {
        int minutes = (int) missionTimer / 60;
        int seconds = (int) missionTimer % 60;
        String minStr = (minutes < 10 ? "0" : "") + minutes;
        String secStr = (seconds < 10 ? "0" : "") + seconds;
        timeLabel.setText("T+ " + minStr + ":" + secStr);
        coordLabel.setText("LOC: " + (int) camPos.x + ", " + (int) camPos.y + ", " + (int) camPos.z);
    }

    // ini buat nampilin notifikasi stage
    public void showStageNotification(int stageNum) {
        notificationLabel.setText("ENTERING STAGE " + stageNum);
        notificationLabel.getColor().a = 0f;
        notificationLabel.clearActions();
        notificationLabel.addAction(Actions.sequence(
            Actions.fadeIn(0.5f),
            Actions.delay(2.0f),
            Actions.fadeOut(1.0f)
        ));
    }

    // notifikasi peringatan barrier
    public void showBarrierWarning(int enemiesLeft) {
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

    // tampilkan menu pause atau sembunyikan
    public void setPauseVisible(boolean visible) {
        if (hudTable != null) hudTable.setVisible(!visible);

        // paksa ngumpet boss bar pas pause
        if (visible && bossUiTable != null) {
            bossUiTable.setVisible(false);
        }

        if (visible) {
            overlay.setVisible(true);
            overlay.getColor().a = 0f;
            overlay.addAction(Actions.fadeIn(0.2f));
            pauseContainer.setVisible(true);
            pauseContainer.setPosition(0, -50);
            pauseContainer.getColor().a = 0f;
            pauseContainer.addAction(Actions.parallel(Actions.fadeIn(0.25f, Interpolation.fade), Actions.moveTo(0, 0, 0.4f, Interpolation.circleOut)));
        } else {
            overlay.setVisible(false);
            pauseContainer.setVisible(false);
        }
    }

    // munculin ui game over
    public void showGameOver() {
        if (gameOverTable != null) {
            gameOverTable.setVisible(true);
            gameOverTable.getColor().a = 0f;
            gameOverTable.addAction(Actions.fadeIn(1.0f));
        }
    }

    // munculin ui menang
    public void showVictory() {
        if (victoryTable != null) {
            victoryTable.setVisible(true);
            victoryTable.getColor().a = 0f;
            victoryTable.addAction(Actions.sequence(
                Actions.delay(1.0f),
                Actions.fadeIn(2.0f)
            ));
        }
    }

    // tambah efek hit marker
    public void addHitMarker(Vector3 pos) {
        activeHitEffects.add(new FloatingHit(pos));
    }

    // method utama buat gambar semua ui
    public void render(PlayerStats playerStats, PerspectiveCamera cam, float delta) {
        // gambar crosshair
        uiBatch.begin();
        uiBatch.setColor(Color.WHITE);
        uiBatch.draw(crosshairTex, Gdx.graphics.getWidth() / 2f - 16, Gdx.graphics.getHeight() / 2f - 16, 32, 32);

        // gambar hit marker 3d to 2d
        for (int i = activeHitEffects.size - 1; i >= 0; i--) {
            FloatingHit hit = activeHitEffects.get(i);

            hit.lifeTime -= delta;
            hit.position.y += 1.5f * delta;

            if (hit.lifeTime <= 0) {
                activeHitEffects.removeIndex(i);
                continue;
            }

            tmpScreenPos.set(hit.position);
            cam.project(tmpScreenPos);

            if (tmpScreenPos.z < 1) {
                float alpha = hit.lifeTime / hit.maxLife;
                uiBatch.setColor(1f, 1f, 0f, alpha);
                uiBatch.draw(hitWrapper,
                    tmpScreenPos.x - 8, tmpScreenPos.y - 8,
                    8, 8,
                    16, 16,
                    1f, 1f,
                    45f);
            }
        }
        uiBatch.setColor(Color.WHITE);
        uiBatch.end();

        // gambar bar darah dan stamina pake shaperenderer
        shapeRenderer.setProjectionMatrix(stage.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // background bar
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(20, 40, 220, 18);
        shapeRenderer.rect(20, 15, 220, 12);

        // health bar merah
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.rect(
            20,
            40,
            220 * (playerStats.health / playerStats.maxHealth),
            18
        );

        // stamina bar oren
        shapeRenderer.setColor(Color.ORANGE);
        shapeRenderer.rect(
            20,
            15,
            220 * (playerStats.stamina / playerStats.maxStamina),
            12
        );

        shapeRenderer.end();

        // gambar stage dan label label lain
        stage.draw();
    }

    // akses label notifikasi buat diganti dari luar
    public Label getNotificationLabel() {
        return notificationLabel;
    }

    // akses tabel boss buat diatur visible nya
    public Table getBossUiTable() {
        return bossUiTable;
    }

    // akses progress bar boss buat diupdate nilainya
    public ProgressBar getBossHealthBar() {
        return bossHealthBar;
    }

    // jangan lupa dispose resource
    public void dispose() {
        stage.dispose();
        shapeRenderer.dispose();
        uiBatch.dispose();
    }

    // sesuaikan viewport kalo layar berubah ukuran
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    // class kecil buat efek hit marker
    public class FloatingHit {
        public Vector3 position;
        public float lifeTime;
        public float maxLife;

        public FloatingHit(Vector3 pos) {
            this.position = new Vector3(pos);
            this.maxLife = 0.5f;
            this.lifeTime = this.maxLife;
        }
    }
}
