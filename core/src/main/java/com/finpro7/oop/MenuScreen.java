package com.finpro7.oop;

import com.badlogic.gdx.Screen;
import com.finpro7.oop.managers.MenuBackground;
import com.finpro7.oop.managers.MenuHUD;

public class MenuScreen implements Screen {

    final Main game;

    // ini dua class pecahan
    private MenuBackground menuBackground;
    private MenuHUD menuHUD;

    public MenuScreen(final Main game) {
        this.game = game;

        // inisialisasi background 3d
        menuBackground = new MenuBackground(game);

        // inisialisasi tampilan ui
        menuHUD = new MenuHUD(game);
    }

    @Override
    public void show() {
        // setiap kali tampil, refresh ui biar duitnya update
        menuHUD.show();
    }

    @Override
    public void render(float delta) {
        // render background dulu (paling belakang)
        menuBackground.render(delta);

        // baru render ui di atasnya (overlay)
        menuHUD.render(delta);
    }

    @Override
    public void resize(int width, int height) {
        menuBackground.resize(width, height);
        menuHUD.resize(width, height);
    }

    @Override
    public void dispose() {
        menuBackground.dispose();
        menuHUD.dispose();
    }

    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
}
