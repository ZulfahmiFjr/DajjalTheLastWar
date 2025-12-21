package com.finpro7.oop;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class Main extends Game {

    // Skin kita buat static agar bisa diakses langsung dari class lain (MenuScreen/GameScreen)
    // tanpa perlu passing parameter yang berlebihan.
    public static Skin skin;

    // Palet warna tema game
    private final Color COLOR_GOLD = new Color(1f, 0.84f, 0.0f, 1f);
    private final Color COLOR_BLOOD = new Color(0.9f, 0.1f, 0.1f, 1f);

    @Override
    public void create() {
        // Generate style UI secara prosedural sebelum masuk ke game
        createStyle();

        // Langsung transisi ke MenuScreen sebagai tampilan awal
        this.setScreen(new MenuScreen(this));
    }

    @Override
    public void render() {
        // Penting: super.render() akan memanggil method render() milik screen yang sedang aktif.
        // Jika dihapus, screen tidak akan menggambar apa-apa.
        super.render();
    }

    @Override
    public void dispose() {
        // Bersihkan resource skin global saat aplikasi ditutup total
        if (skin != null) skin.dispose();
    }

    // Membangun Skin UI secara programatis (tanpa file .json eksternal)
    // Ini mencakup Font, Texture dasar, dan Style untuk Widget (Label, Button, Window)
    private void createStyle() {
        skin = new Skin();

        // Setup BitmapFont
        // Kita scale up font default agar tajam dan terbaca jelas
        BitmapFont fontTitle = new BitmapFont();
        fontTitle.getData().setScale(5.0f);
        skin.add("font-title", fontTitle);
        skin.add("default-font", fontTitle); // Mapping default diperlukan oleh beberapa widget dialog

        BitmapFont fontButton = new BitmapFont();
        fontButton.getData().setScale(1.5f);
        skin.add("font-button", fontButton);

        BitmapFont fontSmall = new BitmapFont();
        fontSmall.getData().setScale(1.0f);
        skin.add("font-small", fontSmall);

        // Membuat tekstur putih 1x1 pixel secara runtime
        // Teknik ini efisien untuk membuat warna solid tanpa perlu memuat file gambar
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fill();
        Texture whiteTex = new Texture(p);
        skin.add("white", whiteTex);
        p.dispose(); // Pixmap sudah tidak dibutuhkan setelah jadi Texture

        // Membuat Drawable dasar dan variasi overlay gelap
        TextureRegionDrawable whiteDrawable = new TextureRegionDrawable(new TextureRegion(whiteTex));
        Drawable dimDrawable = whiteDrawable.tint(new Color(0f, 0f, 0f, 0.85f));
        skin.add("dim-overlay", dimDrawable);

        // Definisi Style Label
        Label.LabelStyle titleStyle = new Label.LabelStyle(fontTitle, COLOR_GOLD);
        skin.add("title", titleStyle);

        Label.LabelStyle shadowStyle = new Label.LabelStyle(fontTitle, Color.BLACK);
        skin.add("shadow", shadowStyle);

        Label.LabelStyle subStyle = new Label.LabelStyle(fontButton, COLOR_BLOOD);
        skin.add("subtitle", subStyle);

        Label.LabelStyle textStyle = new Label.LabelStyle(fontSmall, Color.GRAY);
        skin.add("text", textStyle);

        // Style default fallback
        skin.add("default", new Label.LabelStyle(fontButton, Color.WHITE));

        // Definisi Style TextButton
        // Kita gunakan tinting pada drawable putih untuk efek hover dan click
        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.font = fontButton;
        btnStyle.up = whiteDrawable.tint(new Color(0.1f, 0.1f, 0.1f, 0.6f));   // State normal
        btnStyle.over = whiteDrawable.tint(new Color(0.5f, 0f, 0f, 0.8f));      // State hover (mouse di atas)
        btnStyle.down = whiteDrawable.tint(new Color(0.8f, 0.7f, 0f, 1f));      // State klik
        btnStyle.fontColor = Color.WHITE;
        btnStyle.overFontColor = COLOR_GOLD;
        skin.add("btn-main", btnStyle);
        skin.add("default", btnStyle);

        // Definisi Style Window (untuk Dialog Pause)
        Window.WindowStyle windowStyle = new Window.WindowStyle();
        windowStyle.background = dimDrawable;
        windowStyle.titleFont = fontTitle;
        windowStyle.titleFontColor = COLOR_GOLD;
        skin.add("default", windowStyle);
    }
}
