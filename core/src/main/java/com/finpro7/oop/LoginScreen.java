package com.finpro7.oop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class LoginScreen implements Screen {

    private final Main game;
    private Stage stage;
    private TextField usernameField;
    private TextField passwordField;
    private Label statusLabel;

    // Arahkan ke Backend Spring Boot kamu (Localhost port 8080)
    private final String BASE_URL = "http://localhost:8080/auth";

    public LoginScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage); // Aktifkan input mouse & keyboard

        Table table = new Table();
        table.setFillParent(true);
        // table.setDebug(true); // Uncomment ini kalau mau lihat garis bantu layout

        // --- COMPONENTS ---
        Label titleLabel = new Label("DAJJAL SURVIVOR", Main.skin, "title");
        Label subLabel = new Label("LOGIN SYSTEM", Main.skin, "subtitle");

        // Input Username
        usernameField = new TextField("", Main.skin);
        usernameField.setMessageText("Username"); // Placeholder text transparan
        usernameField.setAlignment(Align.center);

        // Input Password
        passwordField = new TextField("", Main.skin);
        passwordField.setMessageText("Password");
        passwordField.setPasswordMode(true); // Ubah jadi bintang-bintang (*)
        passwordField.setPasswordCharacter('*');
        passwordField.setAlignment(Align.center);

        // Tombol
        TextButton btnLogin = new TextButton("LOGIN", Main.skin);
        TextButton btnRegister = new TextButton("SIGN UP", Main.skin);
        TextButton btnExit = new TextButton("EXIT", Main.skin);

        // Label Status (Buat nampilin error/loading)
        statusLabel = new Label("Connect to database...", Main.skin, "text");
        statusLabel.setAlignment(Align.center);

        // --- LAYOUT TABLE ---
        table.add(titleLabel).padBottom(10).row();
        table.add(subLabel).padBottom(50).row();

        table.add(usernameField).width(400).height(50).padBottom(15).row();
        table.add(passwordField).width(400).height(50).padBottom(30).row();

        table.add(btnLogin).width(200).height(60).padBottom(10).row();
        table.add(btnRegister).width(200).height(60).padBottom(10).row();
        table.add(btnExit).width(200).height(60).padBottom(20).row();

        table.add(statusLabel).width(600).row();

        stage.addActor(table);

        // --- LOGIC TOMBOL ---
        btnLogin.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                sendAuthRequest("/login");
            }
        });

        btnRegister.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
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

    // Fungsi utama buat nembak API ke Backend
    private void sendAuthRequest(final String endpoint) {
        String user = usernameField.getText();
        String pass = passwordField.getText();

        // Validasi input kosong
        if (user.trim().isEmpty() || pass.trim().isEmpty()) {
            statusLabel.setText("Isi Username dan Password dulu!");
            statusLabel.setColor(Color.RED);
            return;
        }

        statusLabel.setText("Connecting...");
        statusLabel.setColor(Color.YELLOW);

        // Bikin JSON string manual (biar gak ribet tambah library JSON di client)
        String jsonContent = "{\"username\":\"" + user + "\", \"password\":\"" + pass + "\"}";

        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest httpRequest = requestBuilder.newRequest()
            .method(Net.HttpMethods.POST)
            .url(BASE_URL + endpoint)
            .header("Content-Type", "application/json")
            .content(jsonContent)
            .build();

        // Kirim Request
        Gdx.net.sendHttpRequest(httpRequest, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                final int statusCode = httpResponse.getStatus().getStatusCode();
                final String result = httpResponse.getResultAsString();

                // Balik ke Main Thread untuk update UI (WAJIB!)
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        if (statusCode == 200) {
                            if (endpoint.equals("/login")) {
                                // SUKSES LOGIN -> Masuk ke Menu Game
                                // Pastikan kamu punya class MenuScreen ya!
                                game.setScreen(new MenuScreen(game));
                            } else {
                                // SUKSES REGISTER
                                statusLabel.setText("Register Berhasil! Silakan Login.");
                                statusLabel.setColor(Color.GREEN);
                            }
                        } else {
                            // GAGAL (Password salah / User not found)
                            // Tampilkan pesan error dari backend
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
                        statusLabel.setText("Gagal Konek ke Server! Pastikan Backend Jalan.");
                        statusLabel.setColor(Color.RED);
                    }
                });
            }

            @Override
            public void cancelled() { }
        });
    }

    @Override
    public void render(float delta) {
        // Background Gelap
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
    }

    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { dispose(); }
}
