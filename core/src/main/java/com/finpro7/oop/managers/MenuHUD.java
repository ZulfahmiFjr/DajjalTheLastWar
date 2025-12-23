package com.finpro7.oop.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.finpro7.oop.GameScreen;
import com.finpro7.oop.LoginScreen;
import com.finpro7.oop.Main;

public class MenuHUD {

    private final Main game;
    private Stage stage;

    public MenuHUD(Main game) {
        this.game = game;
        stage = new Stage(new ScreenViewport());

        // panggil setup ui pas pertama dibuat
        setupUI();
    }

    public void show() {
        // setiap kali menu tampil, bersihin ui lama dan bikin baru biar datanya update
        stage.clear();
        setupUI();
        Gdx.input.setInputProcessor(stage);
    }

    private void setupUI() {
        Table rootTable = new Table();
        rootTable.setFillParent(true);

        com.badlogic.gdx.Preferences prefs = Gdx.app.getPreferences("UserSession");
        String username = prefs.getString("current_user", "UNKNOWN");
        int totalCoins = prefs.getInteger("total_coins", 0);

        TextureRegionDrawable panelDrawable = createColorDrawable(new Color(0f, 0f, 0f, 0.75f));
        TextureRegionDrawable lineDrawable = createColorDrawable(new Color(1f, 1f, 1f, 0.5f));

        // tabel buat nampilin judul game
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

        // tabel info player
        Table playerInfoTable = new Table();
        playerInfoTable.setBackground(panelDrawable);
        playerInfoTable.pad(20);

        Label nameLabel = new Label("USERNAME: " + username.toUpperCase(), Main.skin, "subtitle");
        nameLabel.setFontScale(1.2f);
        nameLabel.setColor(Color.WHITE);

        Label coinsLabel = new Label("WALLET: " + totalCoins + " COINS", Main.skin, "subtitle");
        coinsLabel.setFontScale(1.2f);
        coinsLabel.setColor(Color.GOLD);

        playerInfoTable.add(nameLabel).left().row();
        playerInfoTable.add(coinsLabel).left().padTop(8);

        // tabel menu utama
        Table menuTable = new Table();
        menuTable.setBackground(panelDrawable);
        menuTable.pad(40);

        TextButton btnStart = new TextButton("DEPLOY MISSION", Main.skin, "btn-main");
        TextButton btnShop = new TextButton("BLACK MARKET", Main.skin, "btn-main");
        TextButton btnLogout = new TextButton("LOGOUT", Main.skin, "btn-main");
        TextButton btnExit = new TextButton("EXIT GAME", Main.skin, "btn-main");

        menuTable.add(btnStart).width(300).height(60).padBottom(10).row();
        menuTable.add(btnShop).width(300).height(60).padBottom(10).row();
        menuTable.add(btnLogout).width(300).height(60).padBottom(10).row();
        menuTable.add(btnExit).width(300).height(60).row();

        // tabel footer
        Table footerTable = new Table();
        footerTable.setBackground(panelDrawable);
        footerTable.pad(10);
        Label verLabel = new Label("VERSION: 1.2.0 STABLE", Main.skin, "text");
        Label copyLabel = new Label(" | (C) 2025 KELOMPOK 7 OOP", Main.skin, "text");
        verLabel.setColor(Color.GRAY);
        footerTable.add(verLabel);
        footerTable.add(copyLabel);

        // susun layout
        rootTable.add(titleTable).top().left().pad(50).row();
        rootTable.add(playerInfoTable).left().padLeft(50).padTop(-20).row();
        rootTable.add(footerTable).expand().bottom().left().pad(20);
        rootTable.add(menuTable).bottom().right().pad(50);

        stage.addActor(rootTable);

        // listener tombol
        btnStart.addListener(new ClickListener(){
            @Override public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new GameScreen(game));
            }
        });
        btnShop.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                openShopUI();
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
        shopTable.setBackground(createColorDrawable(new Color(0, 0, 0, 0.95f)));

        com.badlogic.gdx.Preferences prefs = Gdx.app.getPreferences("UserSession");
        final int currentCoins = prefs.getInteger("total_coins", 0);

        // header toko
        Label shopTitle = new Label("BLACK MARKET - UPGRADES", Main.skin, "title");
        shopTitle.setColor(Color.GOLD);
        shopTitle.setFontScale(1.2f);

        final Label walletLabel = new Label("WALLET: " + currentCoins + " COINS", Main.skin, "subtitle");
        walletLabel.setColor(Color.GREEN);
        walletLabel.setFontScale(0.9f);

        shopTable.add(shopTitle).colspan(3).center().padBottom(10).row();
        shopTable.add(walletLabel).colspan(3).center().padBottom(30).row();

        // bagian varian pistol
        shopTable.add(new Label("--- SIDEARMS (PISTOLS) ---", Main.skin, "subtitle")).colspan(3).center().row();

        int activeId = prefs.getInteger("equipped_pistol_id", 0);

        // regular pistol yang selalu ada
        addShopItem(shopTable, "REGULAR PISTOL", "Standard Issue", 0, "regular", walletLabel, activeId == 0);
        shopTable.row().padTop(10);

        // varian zippy
        addShopItem(shopTable, "ZIPPY PISTOL", "High Fire Rate", 500, "zippy", walletLabel, activeId == 1);
        shopTable.row().padTop(10);

        // varian chunky
        addShopItem(shopTable, "CHUNKY PISTOL", "High Damage", 600, "chunky", walletLabel, activeId == 2);
        shopTable.row().padTop(10);

        // varian long barrel
        addShopItem(shopTable, "LONG BARREL", "High Accuracy", 700, "long", walletLabel, activeId == 3);
        shopTable.row().padTop(10);

        // varian scoped
        addShopItem(shopTable, "SCOPED PISTOL", "Zoom Capability", 800, "scoped", walletLabel, activeId == 4);
        shopTable.row().padTop(30);

        // senjata spesial
        shopTable.add(new Label("--- SPECIAL WEAPON ---", Main.skin, "subtitle")).colspan(3).center().row();

        // ak-47
        addShopItem(shopTable, "AK-47 RIFLE", "Assault Rifle", 1000, "has_ak", walletLabel, false);
        // tombol buat keluar
        TextButton btnClose = new TextButton("EXIT MARKET", Main.skin, "btn-main");
        shopTable.row();
        shopTable.add(btnClose).colspan(3).center().padTop(50).width(300).height(60);

        btnClose.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                shopTable.remove();
                stage.clear();
                setupUI();
            }
        });

        stage.addActor(shopTable);
    }

    // method buat nambahin item jualan tanpa ribet
    private void addShopItem(Table table, String itemName, String desc, int price, final String itemKey, final Label walletLabel, boolean isEquipped) {
        final com.badlogic.gdx.Preferences prefs = Gdx.app.getPreferences("UserSession");

        // logika cek kepemilikan
        boolean isOwned = false;

        if (itemKey.equals("regular")) {
            isOwned = true; // regular selalu punya
        } else if (itemKey.equals("has_ak")) {
            isOwned = prefs.getBoolean("has_ak", false);
        } else {
            // buat pistol varian zippy, chunky dll cek preference
            isOwned = prefs.getBoolean("own_" + itemKey, false);
        }

        Label nameLbl = new Label(itemName, Main.skin, "subtitle");
        Label descLbl = new Label(desc, Main.skin, "text");
        descLbl.setColor(Color.GRAY);

        final TextButton btnBuy = new TextButton(price + " COINS", Main.skin, "btn-main");

        // logika tampilan tombol
        if (isEquipped) {
            btnBuy.setText("EQUIPPED");
            btnBuy.setDisabled(true);
            btnBuy.setColor(Color.GREEN);
        } else if (isOwned) {
            // kalau udah punya ak, cuma status owned doang
            if (itemKey.equals("has_ak")) {
                btnBuy.setText("OWNED");
                btnBuy.setDisabled(true);
                btnBuy.setColor(Color.YELLOW);
            } else {
                // kalau pistol varian udah punya tapi belum dipake jadi tombol equip
                btnBuy.setText("EQUIP");
            }
        }

        Table itemInfo = new Table();
        itemInfo.add(nameLbl).left().row();
        itemInfo.add(descLbl).left();

        table.add(itemInfo).left().width(400).padLeft(50);
        table.add(btnBuy).width(150).height(50).padRight(50);

        // logika klik tombol
        btnBuy.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (btnBuy.isDisabled() || btnBuy.getText().toString().equals("OWNED") || btnBuy.getText().toString().equals("EQUIPPED")) {
                    return; // langsung stop, jangan lanjut ke bawah
                }
                // kalo tombolnya equip gratis cuma ganti senjata
                if (btnBuy.getText().toString().equals("EQUIP")) {
                    String requestKey = "equip_" + itemKey; // kirim "equip_zippy"
                    sendRequest(requestKey, 0, walletLabel, btnBuy, itemKey, prefs, true);
                    return;
                }

                // kalo tombolnya beli bayar pake koin
                int coins = prefs.getInteger("total_coins", 0);
                if (coins < price) {
                    btnBuy.setText("NO MONEY!");
                    btnBuy.addAction(Actions.sequence(Actions.delay(1f), Actions.run(() -> btnBuy.setText(price + " COINS"))));
                    return;
                }

                // tentuin request key buat beli
                String requestKey = "";
                if (itemKey.equals("has_ak")) requestKey = "buy_ak";
                else requestKey = "buy_" + itemKey;

                sendRequest(requestKey, price, walletLabel, btnBuy, itemKey, prefs, false);
            }
        });
    }

    // helper buat kirim request ke server biar method addShopItem gak kepanjangan
    private void sendRequest(String requestKey, int price, Label walletLabel, TextButton btnBuy, String itemKey, com.badlogic.gdx.Preferences prefs, boolean isEquipRequest) {
        btnBuy.setDisabled(true);
        btnBuy.setText("...");

        String username = prefs.getString("current_user", "");
        String jsonContent = "{\"username\":\"" + username + "\", \"itemBought\":\"" + requestKey + "\"}";

        com.badlogic.gdx.net.HttpRequestBuilder requestBuilder = new com.badlogic.gdx.net.HttpRequestBuilder();
        com.badlogic.gdx.Net.HttpRequest httpRequest = requestBuilder.newRequest()
            .method(com.badlogic.gdx.Net.HttpMethods.POST)
            .url("http://localhost:8081/auth/buyItem")
            .header("Content-Type", "application/json")
            .content(jsonContent)
            .build();

        Gdx.net.sendHttpRequest(httpRequest, new com.badlogic.gdx.Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(com.badlogic.gdx.Net.HttpResponse httpResponse) {
                final int status = httpResponse.getStatus().getStatusCode();

                Gdx.app.postRunnable(() -> {
                    if (status == 200) {
                        // update lokal
                        if (!isEquipRequest) {
                            int currentCoins = prefs.getInteger("total_coins", 0);
                            int sisa = currentCoins - price;
                            prefs.putInteger("total_coins", sisa);
                            walletLabel.setText("WALLET: " + sisa + " COINS");

                            // simpan status kepemilikan
                            if (itemKey.equals("has_ak")) prefs.putBoolean("has_ak", true);
                            else prefs.putBoolean("own_" + itemKey, true);
                        }

                        // update id equip lokal
                        if (isEquipRequest || (!itemKey.equals("has_ak"))) {
                            // kalo sukses beli pistol otomatis update equip id di lokal juga
                            updateLocalEquipId(itemKey, prefs);
                        }

                        prefs.flush();

                        // refresh toko biar tombol update
                        if (btnBuy.getParent() != null && btnBuy.getParent().getParent() != null) {
                            btnBuy.getParent().getParent().remove();
                        }
                        openShopUI();

                    } else {
                        btnBuy.setText("ERROR");
                        btnBuy.setDisabled(false);
                    }
                });
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.postRunnable(() -> {
                    btnBuy.setText("FAIL");
                    btnBuy.setDisabled(false);
                });
            }

            @Override
            public void cancelled() { }
        });
    }

    private void updateLocalEquipId(String key, com.badlogic.gdx.Preferences prefs) {
        if (key.equals("regular")) prefs.putInteger("equipped_pistol_id", 0);
        else if (key.equals("zippy")) prefs.putInteger("equipped_pistol_id", 1);
        else if (key.equals("chunky")) prefs.putInteger("equipped_pistol_id", 2);
        else if (key.equals("long")) prefs.putInteger("equipped_pistol_id", 3);
        else if (key.equals("scoped")) prefs.putInteger("equipped_pistol_id", 4);
    }

    private TextureRegionDrawable createColorDrawable(Color color) {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(color);
        pm.fill();
        Texture tex = new Texture(pm);
        pm.dispose();
        return new TextureRegionDrawable(new TextureRegion(tex));
    }

    public void render(float delta) {
        stage.act(delta);
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public void dispose() {
        stage.dispose();
    }
}
