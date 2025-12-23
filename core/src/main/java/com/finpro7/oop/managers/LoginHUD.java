package com.finpro7.oop.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.finpro7.oop.Main;
import com.finpro7.oop.MenuScreen;

public class LoginHUD {

    private final Main game;
    private Stage stage;

    // komponen ui dijadiin variabel global biar bisa diakses dari method lain
    private TextField usernameField;
    private TextField passwordField;
    private Label statusLabel;
    private TextButton btnLogin;
    private TextButton btnRegister;
    private TextButton btnExit;

    private final String BASE_URL = "http://localhost:8081/auth";

    public LoginHUD(Main game) {
        this.game = game;
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        setupUI();
    }

    private void setupUI() {
        Table table = new Table();
        table.setFillParent(true);

        Label titleLabel = new Label("DAJJAL - THE LAST WAR", Main.skin, "title");
        Label subLabel = new Label("LOGIN SYSTEM", Main.skin, "subtitle");

        usernameField = new TextField("", Main.skin);
        usernameField.setMessageText("Username");
        usernameField.setAlignment(Align.center);

        passwordField = new TextField("", Main.skin);
        passwordField.setMessageText("Password");
        passwordField.setPasswordMode(true);
        passwordField.setPasswordCharacter('*');
        passwordField.setAlignment(Align.center);

        // inisialisasi tombol
        btnLogin = new TextButton("LOGIN", Main.skin);
        btnRegister = new TextButton("SIGN UP", Main.skin);
        btnExit = new TextButton("EXIT", Main.skin);

        statusLabel = new Label("Connect to database...", Main.skin, "text");
        statusLabel.setAlignment(Align.center);

        table.add(titleLabel).padBottom(10).row();
        table.add(subLabel).padBottom(50).row();

        table.add(usernameField).width(400).height(50).padBottom(15).row();
        table.add(passwordField).width(400).height(50).padBottom(30).row();

        table.add(btnLogin).width(200).height(60).padBottom(10).row();
        table.add(btnRegister).width(200).height(60).padBottom(10).row();
        table.add(btnExit).width(200).height(60).padBottom(20).row();

        table.add(statusLabel).width(600).row();

        stage.addActor(table);

        btnLogin.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // lepas fokus dari textfield biar kliknya langsung keproses
                stage.unfocusAll();
                sendAuthRequest("/login");
            }
        });

        btnRegister.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                stage.unfocusAll();
                sendAuthRequest("/register");
            }
        });

        btnExit.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });
    }

    private void sendAuthRequest(final String endpoint) {
        String user = usernameField.getText();
        String pass = passwordField.getText();

        if (user.trim().isEmpty() || pass.trim().isEmpty()) {
            statusLabel.setText("Isi Username dan Password dulu!");
            statusLabel.setColor(Color.RED);
            return;
        }

        // matikan tombol biar user gak spam klik
        setButtonsEnabled(false);

        statusLabel.setText("Connecting...");
        statusLabel.setColor(Color.YELLOW);

        String jsonContent = "{\"username\":\"" + user + "\", \"password\":\"" + pass + "\"}";

        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest httpRequest = requestBuilder.newRequest()
            .method(Net.HttpMethods.POST)
            .url(BASE_URL + endpoint)
            .header("Content-Type", "application/json")
            .content(jsonContent)
            .build();
        httpRequest.setTimeOut(5000); // kasih timeout biar gk bengong

        Gdx.net.sendHttpRequest(httpRequest, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                final int statusCode = httpResponse.getStatus().getStatusCode();
                final String result = httpResponse.getResultAsString();

                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        // nyalain lagi tombolnya karena request udah kelar
                        setButtonsEnabled(true);

                        if (statusCode == 200) {
                            if (endpoint.equals("/login")) {
                                System.out.println("RESPON DARI SERVER: " + result);

                                // proteksi biar gak crash kalo datanya aneh
                                int totalKoinDariDb = 0;
                                boolean hasAk = false;
                                boolean ownZippy = false;
                                boolean ownChunky = false;
                                boolean ownLong = false;
                                boolean ownScoped = false;
                                int equippedId = 0;

                                if (result != null && !result.trim().isEmpty() && result.startsWith("{")) {
                                    try {
                                        com.badlogic.gdx.utils.JsonReader json = new com.badlogic.gdx.utils.JsonReader();
                                        com.badlogic.gdx.utils.JsonValue base = json.parse(result);
                                        // pake default 0 kalo datanya gak ketemu
                                        totalKoinDariDb = base.getInt("coins", 0);
                                        hasAk = base.getBoolean("hasAk", false);
                                        // ambil data kepemilikan pistol
                                        ownZippy = base.getBoolean("ownZippy", false);
                                        ownChunky = base.getBoolean("ownChunky", false);
                                        ownLong = base.getBoolean("ownLong", false);
                                        ownScoped = base.getBoolean("ownScoped", false);
                                        // ambil ID senjata yang lagi dipake
                                        equippedId = base.getInt("equippedPistolId", 0);
                                    } catch (Exception e) {
                                        System.out.println("Gagal baca JSON: " + e.getMessage());
                                    }
                                } else {
                                    System.out.println("SERVER CUMA NGIRIM TEKS BIASA, BUKAN OBJEK USER!");
                                }

                                // simpen data ke memori lokal
                                com.badlogic.gdx.Preferences prefs = Gdx.app.getPreferences("UserSession");
                                prefs.putString("current_user", user);
                                prefs.putInteger("total_coins", totalKoinDariDb);
                                prefs.putBoolean("has_ak", hasAk);
                                // simpen data pistol
                                prefs.putBoolean("own_zippy", ownZippy);
                                prefs.putBoolean("own_chunky", ownChunky);
                                prefs.putBoolean("own_long", ownLong);
                                prefs.putBoolean("own_scoped", ownScoped);
                                prefs.putInteger("equipped_pistol_id", equippedId);
                                prefs.flush();

                                game.setScreen(new MenuScreen(game));
                            } else {
                                statusLabel.setText("Register Berhasil! Silakan Login.");
                                statusLabel.setColor(Color.GREEN);
                            }
                        } else {
                            statusLabel.setText(result);
                            statusLabel.setColor(Color.RED);
                        }
                    }
                });
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        // nyalain lagi tombolnya biar bisa coba lagi
                        setButtonsEnabled(true);

                        statusLabel.setText("Gagal Konek ke Server! Pastikan Backend Jalan.");
                        statusLabel.setColor(Color.RED);
                    }
                });
            }

            @Override
            public void cancelled() {
                // kalo dicancel manual, jangan lupa nyalain lagi tombolnya
                Gdx.app.postRunnable(() -> setButtonsEnabled(true));
            }
        });
    }

    // method bantuan buat matiin atau nyalain tombol
    private void setButtonsEnabled(boolean enabled) {
        Touchable touchable = enabled ? Touchable.enabled : Touchable.disabled;
        btnLogin.setTouchable(touchable);
        btnRegister.setTouchable(touchable);

        // opsional, ubah warna dikit biar keliatan lagi loading
        if (!enabled) {
            btnLogin.setColor(Color.GRAY);
            btnRegister.setColor(Color.GRAY);
        } else {
            btnLogin.setColor(Color.WHITE);
            btnRegister.setColor(Color.WHITE);
        }
    }

    public void render(float delta) {
        stage.act(delta);
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public void dispose() {
        if (stage != null) stage.dispose();
    }
}
