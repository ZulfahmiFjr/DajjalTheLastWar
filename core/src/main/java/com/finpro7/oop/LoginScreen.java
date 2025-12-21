package com.finpro7.oop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.finpro7.oop.world.Terrain;

public class LoginScreen implements Screen {

    private final Main game;
    private Stage stage;
    private TextField usernameField;
    private TextField passwordField;
    private Label statusLabel;

    private final String BASE_URL = "http://localhost:8081/auth";

    private PerspectiveCamera cam;
    private Environment env;
    private ModelBatch modelBatch;
    private RenderContext renderContext;
    private Terrain terrain;
    private PerlinNoise perlin;
    private Model treeModel;
    private Array<ModelInstance> treeInstances = new Array<>();
    private float camTimer = 0f;
    private final Color SKY_COLOR = new Color(0.2f, 0.25f, 0.3f, 1f);

    public LoginScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        setup3DBackground();
        setupUI();
    }

    private void setup3DBackground() {
        cam = new PerspectiveCamera(67f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.near = 1f;
        cam.far = 1000f;

        env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.3f, 0.3f, 0.4f, 1f));
        env.add(new DirectionalLight().set(0.6f, 0.6f, 0.6f, -0.2f, -1f, -0.3f));
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
        terrain.generateTrees(treeModel, treeInstances, 500);
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

        TextButton btnLogin = new TextButton("LOGIN", Main.skin);
        TextButton btnRegister = new TextButton("SIGN UP", Main.skin);
        TextButton btnExit = new TextButton("EXIT", Main.skin);

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

    private void sendAuthRequest(final String endpoint) {
        String user = usernameField.getText();
        String pass = passwordField.getText();

        if (user.trim().isEmpty() || pass.trim().isEmpty()) {
            statusLabel.setText("Isi Username dan Password dulu!");
            statusLabel.setColor(Color.RED);
            return;
        }

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

        Gdx.net.sendHttpRequest(httpRequest, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                final int statusCode = httpResponse.getStatus().getStatusCode();
                final String result = httpResponse.getResultAsString();

                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        if (statusCode == 200) {
                            if (endpoint.equals("/login")) {
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
        camTimer += delta * 0.03f;

        float radius = 250f;
        float camX = MathUtils.sin(camTimer) * radius;
        float camZ = MathUtils.cos(camTimer) * radius;
        float camY = terrain.getHeight(camX, camZ) + 80f;

        cam.position.set(camX, camY, camZ);
        cam.lookAt(0, -20f, 0);
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
        if (stage != null) stage.dispose();
        if(modelBatch != null) modelBatch.dispose();
        if(terrain != null) terrain.dispose();
    }

    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { dispose(); }
}
