package com.finpro7.oop;

import com.badlogic.gdx.Screen;
import com.finpro7.oop.managers.LoginBackground;
import com.finpro7.oop.managers.LoginHUD;

public class LoginScreen implements Screen {

    private final Main game;

    // ini dua asisten kita buat ngurus tampilan
    private LoginBackground loginBackground;
    private LoginHUD loginHUD;

    public LoginScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        // siapin background 3d nya
        loginBackground = new LoginBackground(game);

        // siapin form login dan tombol tombolnya
        loginHUD = new LoginHUD(game);
    }

    @Override
    public void render(float delta) {
        // gambar background duluan biar ada di belakang
        loginBackground.render(delta);

        // terus tumpuk sama ui login di depannya
        loginHUD.render(delta);
    }

    @Override
    public void resize(int width, int height) {
        loginBackground.resize(width, height);
        loginHUD.resize(width, height);
    }

    @Override
    public void dispose() {
        if (loginBackground != null) loginBackground.dispose();
        if (loginHUD != null) loginHUD.dispose();
    }

    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { dispose(); }
}
