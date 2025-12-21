package com.finpro7.oop;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.finpro7.oop.world.weapon.Firearm;
import com.finpro7.oop.world.weapon.WeaponMod;

public class Main extends Game {

    public static Skin skin;
    public AssetManager assets;

    public static com.finpro7.oop.world.weapon.AkRifle.Template autoRifleTemplate;
    public static com.badlogic.gdx.graphics.g3d.Model weaponsModel;

    // Palet warna tema game
    private final Color COLOR_GOLD = new Color(1f, 0.84f, 0.0f, 1f);
    private final Color COLOR_BLOOD = new Color(0.9f, 0.1f, 0.1f, 1f);

    @Override
    public void create() {
        assets = new AssetManager();
        createStyle();
        loadAssets();
        // Ambil modelnya dulu dari assets
        weaponsModel = assets.get("models/weapons.g3db", Model.class);

        autoRifleTemplate = new com.finpro7.oop.world.weapon.AkRifle.Template(
            new com.badlogic.gdx.graphics.g3d.ModelInstance(weaponsModel),
            new com.badlogic.gdx.math.Vector3(0, 0, 0),
            null,
            null
        );
        // Langsung masuk ke LoginScreen saat aplikasi dibuka
        this.setScreen(new LoginScreen(this));
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
        if (skin != null) skin.dispose();
        if (assets != null) assets.dispose();
    }

    private void createStyle() {
        skin = new Skin();

        // --- FONT SETUP ---
        BitmapFont fontTitle = new BitmapFont();
        fontTitle.getData().setScale(4.0f);
        skin.add("font-title", fontTitle);
        skin.add("default-font", fontTitle);

        BitmapFont fontButton = new BitmapFont();
        fontButton.getData().setScale(1.5f);
        skin.add("font-button", fontButton);

        BitmapFont fontSmall = new BitmapFont();
        fontSmall.getData().setScale(1.2f);
        skin.add("font-small", fontSmall);

        // --- TEXTURE SETUP ---
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fill();
        Texture whiteTex = new Texture(p);
        skin.add("white", whiteTex);
        p.dispose();

        // Drawable
        TextureRegionDrawable whiteDrawable = new TextureRegionDrawable(new TextureRegion(whiteTex));
        Drawable dimDrawable = whiteDrawable.tint(new Color(0f, 0f, 0f, 0.85f));
        skin.add("dim-overlay", dimDrawable);

        // --- LABEL STYLES ---
        Label.LabelStyle titleStyle = new Label.LabelStyle(fontTitle, COLOR_GOLD);
        skin.add("title", titleStyle);

        Label.LabelStyle shadowStyle = new Label.LabelStyle(fontTitle, Color.BLACK);
        skin.add("shadow", shadowStyle);

        Label.LabelStyle subStyle = new Label.LabelStyle(fontButton, COLOR_BLOOD);
        skin.add("subtitle", subStyle);

        Label.LabelStyle textStyle = new Label.LabelStyle(fontSmall, Color.WHITE);
        skin.add("text", textStyle);

        Label.LabelStyle errorStyle = new Label.LabelStyle(fontSmall, Color.RED);
        skin.add("error", errorStyle);

        skin.add("default", new Label.LabelStyle(fontButton, Color.WHITE));

        // --- BUTTON STYLES ---
        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.font = fontButton;
        btnStyle.up = whiteDrawable.tint(new Color(0.1f, 0.1f, 0.1f, 0.6f));
        btnStyle.over = whiteDrawable.tint(new Color(0.5f, 0f, 0f, 0.8f));
        btnStyle.down = whiteDrawable.tint(new Color(0.8f, 0.7f, 0f, 1f));
        btnStyle.fontColor = Color.WHITE;
        btnStyle.overFontColor = COLOR_GOLD;
        skin.add("btn-main", btnStyle);
        skin.add("default", btnStyle);

        // --- WINDOW STYLE ---
        Window.WindowStyle windowStyle = new Window.WindowStyle();
        windowStyle.background = dimDrawable;
        windowStyle.titleFont = fontTitle;
        windowStyle.titleFontColor = COLOR_GOLD;
        skin.add("default", windowStyle);

        // --- TEXTFIELD STYLE (Wajib buat Login) ---
        TextField.TextFieldStyle tfStyle = new TextField.TextFieldStyle();
        tfStyle.font = fontSmall;
        tfStyle.fontColor = Color.WHITE;
        tfStyle.background = whiteDrawable.tint(new Color(0.2f, 0.2f, 0.2f, 0.8f)); // Background abu gelap
        tfStyle.cursor = whiteDrawable.tint(COLOR_GOLD);
        tfStyle.selection = whiteDrawable.tint(Color.BLUE);
        skin.add("default", tfStyle);
    }

    private void loadAssets(){
        assets = new AssetManager();

        // --- LOAD MODELS ---
        assets.load("models/pohon.g3dj", Model.class);
        assets.load("models/dajjal.g3db", Model.class);
        assets.load("models/majuj/majuj.g3db", Model.class);
        assets.load("models/yajuj/yajuj.g3db", Model.class);

        // --- LOAD TEXTURES ---
        assets.load("textures/batang_pohon.png", Texture.class);
        assets.load("textures/daun_pohon.png", Texture.class);

        assets.load("models/dajjal_diffuse.png", Texture.class);
        assets.load("models/dajjal_glow.png", Texture.class);

        assets.load("models/majuj/majuj1.png", Texture.class);
        assets.load("models/majuj/majuj2.png", Texture.class);

        assets.load("models/yajuj/yajuj1.png", Texture.class);
        assets.load("models/yajuj/yajuj2.png", Texture.class);
        assets.load("models/yajuj/yajuj3.png", Texture.class);
        assets.load("models/yajuj/yajuj4.png", Texture.class);

        assets.load("models/weapons.g3db", Model.class);
        assets.load("textures/crosshair.png", Texture.class);

        // Finish Loading (Blocking sampai semua ke-load)
        assets.finishLoading();
    }
}
