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

        TextureRegionDrawable panelDrawable = createColorDrawable(new Color(0f, 0f, 0f, 0.75f));
        TextureRegionDrawable lineDrawable = createColorDrawable(new Color(1f, 1f, 1f, 0.5f));

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
        deptLabel.setFontScale(0.9f);

        titleTable.add(title).left().row();
        titleTable.add(subtitle).left().padBottom(10).row();
        titleTable.add(separator).growX().height(3).padBottom(10).row();
        titleTable.add(deptLabel).left();

        Table menuTable = new Table();
        menuTable.setBackground(panelDrawable);
        menuTable.pad(40);

        Label menuLabel = new Label("MISSION CONTROL", Main.skin, "subtitle");
        menuLabel.setAlignment(Align.center);
        menuLabel.setColor(Color.GOLD);

        TextButton btnStart = new TextButton("DEPLOY MISSION", Main.skin, "btn-main");
        TextButton btnSettings = new TextButton("SETTINGS", Main.skin, "btn-main");
        TextButton btnLogout = new TextButton("LOGOUT", Main.skin, "btn-main");
        TextButton btnExit = new TextButton("EXIT GAME", Main.skin, "btn-main");

        menuTable.add(menuLabel).padBottom(20).row();
        menuTable.add(btnStart).width(300).height(60).padBottom(10).row();
        menuTable.add(btnSettings).width(300).height(60).padBottom(10).row();
        menuTable.add(btnLogout).width(300).height(60).padBottom(10).row();
        menuTable.add(btnExit).width(300).height(60).row();

        Table footerTable = new Table();
        footerTable.setBackground(panelDrawable);
        footerTable.pad(10);
        Label verLabel = new Label("VERSION: 1.2.0 STABLE", Main.skin, "text");
        Label copyLabel = new Label(" | (C) 2025 KELOMPOK 7 OOP", Main.skin, "text");
        verLabel.setColor(Color.GRAY);
        footerTable.add(verLabel);
        footerTable.add(copyLabel);

        rootTable.add(titleTable).expand().top().left().pad(50);
        rootTable.row();
        rootTable.add(footerTable).expandX().bottom().left().pad(20);
        rootTable.add(menuTable).bottom().right().pad(50);

        stage.addActor(rootTable);

        btnStart.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (game.assets.update()) {
                    game.setScreen(new GameScreen(game));
                }
            }
        });

        btnSettings.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                System.out.println("Settings clicked");
            }
        });

        btnLogout.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new LoginScreen(game));
            }
        });

        btnExit.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });
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

    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
}
