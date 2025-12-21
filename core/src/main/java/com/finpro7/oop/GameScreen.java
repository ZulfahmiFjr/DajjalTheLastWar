package com.finpro7.oop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.*;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.*;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
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

public class GameScreen implements Screen {

    final Main game;
    private Stage stage;

    // Sistem 3D
    private PerspectiveCamera cam;
    private Environment env;
    private RenderContext renderContext;
    private ModelBatch modelBatch;

    // Asset dan World
    private AssetManager assets;
    private Terrain terrain;
    private PerlinNoise perlin;
    private Model treeModel;
    private Array<ModelInstance> treeInstances = new Array<>();

    // Kontrol Pemain
    private float yawDeg, pitchDeg;
    private float mouseSens = 0.14f;
    private float moveSpeed = 10f;
    private float sprintMul = 2.0f;
    private float eyeHeight = 2.0f;
    private float margin = 1.5f;

    // Fisika
    private float verticalVelocity = 0f;
    private float gravity = 30f;
    private float jumpForce = 15f;
    private boolean isGrounded = false;
    private int skipMouseFrames = 3;

    // Sistem Kabut
    private Model fogModel;
    private Array<ModelInstance> fogInstances = new Array<>();
    private float fogSpeed = 2.0f;
    private int fogCount = 200;

    // Sistem UI & State
    private boolean isPaused = false;
    private Table pauseContainer;
    private Image overlay;

    // Variabel Data UI (Dummy Stats)
    private float missionTimer = 0f;
    private Label coordLabel;
    private Label timeLabel;

    public GameScreen(final Main game) {
        this.game = game;
        stage = new Stage(new ScreenViewport());

        setup3DWorld();
        setupPauseInterface();

        setPaused(false);
    }

    private void setup3DWorld() {
        cam = new PerspectiveCamera(67f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.near = 0.1f;
        cam.far = 200f;

        env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.6f, 1f));
        env.add(new DirectionalLight().set(1f, 1f, 1f, -0.6f, -1f, -0.3f));
        env.set(new ColorAttribute(ColorAttribute.Fog, 0.08f, 0.1f, 0.14f, 1f));

        renderContext = new RenderContext(new DefaultTextureBinder(DefaultTextureBinder.LRU, 1));
        modelBatch = new ModelBatch();

        perlin = new PerlinNoise();
        perlin.amplitude = 80f;
        perlin.frequencyX = 0.08f;
        perlin.frequencyZ = 0.08f;
        perlin.offsetX = MathUtils.random(0f, 999f);
        perlin.offsetZ = MathUtils.random(0f, 999f);

        assets = new AssetManager();
        assets.load("models/pohon.g3dj", Model.class);
        assets.load("textures/batang_pohon.png", Texture.class);
        assets.load("textures/daun_pohon.png", Texture.class);
        assets.finishLoading();

        treeModel = assets.get("models/pohon.g3dj", Model.class);
        createFogSystem();

        terrain = new Terrain(env, perlin, 254, 254, 320f, 320f);

        for(Material mat : treeModel.materials){
            if(treeModel.materials.indexOf(mat, true) == 1){
                mat.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA),
                    FloatAttribute.createAlphaTest(0.25f),
                    IntAttribute.createCullFace(GL20.GL_NONE));
            }
        }
        terrain.generateTrees(treeModel, treeInstances, 600);

        Vector3 startPos = new Vector3();
        terrain.getRoadStartPos(startPos);
        cam.position.set(startPos.x + 5.0f, startPos.y + eyeHeight, startPos.z + 5.0f);

        Vector3 lookTarget = new Vector3();
        terrain.getRoadLookAtPos(lookTarget);
        cam.direction.set(lookTarget.sub(cam.position)).nor();
        cam.up.set(Vector3.Y);
        cam.update();
    }

    private void setupPauseInterface() {
        // 1. Overlay Fullscreen Hitam Transparan
        overlay = new Image(Main.skin.newDrawable("white", new Color(0.05f, 0.05f, 0.08f, 0.85f)));
        overlay.setFillParent(true);
        overlay.setVisible(false);
        stage.addActor(overlay);

        // 2. Container Utama (Menggunakan Table)
        pauseContainer = new Table();
        pauseContainer.setFillParent(true);
        pauseContainer.setVisible(false);

        // -- Bagian Kiri: Info Panel (Stats) --
        Table infoTable = new Table();
        infoTable.setBackground(Main.skin.newDrawable("white", new Color(0f, 0f, 0f, 0.5f)));
        infoTable.pad(30);

        Label sysTitle = new Label("SYSTEM PAUSED", Main.skin, "title");
        sysTitle.setFontScale(0.8f);
        sysTitle.setColor(Color.ORANGE);

        Label missionText = new Label("CURRENT OP: RECON", Main.skin, "text");

        // Label dinamis untuk data
        timeLabel = new Label("T+ 00:00:00", Main.skin, "subtitle");
        coordLabel = new Label("LOC: 000, 000", Main.skin, "text");
        coordLabel.setColor(Color.LIGHT_GRAY);

        infoTable.add(sysTitle).left().padBottom(10).row();
        infoTable.add(missionText).left().padBottom(30).row();
        infoTable.add(timeLabel).left().padBottom(5).row();
        infoTable.add(coordLabel).left().expandY().top();

        // -- Bagian Kanan: Menu Buttons --
        Table buttonTable = new Table();

        TextButton btnResume = createStyledButton("RESUME MISSION", () -> togglePause());
        TextButton btnRestart = createStyledButton("RESTART SECTOR", () -> game.setScreen(new GameScreen(game)));
        TextButton btnExit = createStyledButton("ABORT OPERATION", () -> {
            game.setScreen(new MenuScreen(game));
            dispose();
        });

        buttonTable.add(btnResume).width(300).height(60).padBottom(15).right().row();
        buttonTable.add(btnRestart).width(300).height(60).padBottom(15).right().row();
        buttonTable.add(btnExit).width(300).height(60).right().row();

        // Menyusun layout kiri dan kanan
        // Expand X agar mereka terpisah ke ujung layar
        pauseContainer.add(infoTable).width(400).expandY().fillY().left();
        pauseContainer.add(buttonTable).expand().bottom().right().pad(50);

        stage.addActor(pauseContainer);
    }

    private TextButton createStyledButton(String text, Runnable action) {
        TextButton btn = new TextButton(text, Main.skin, "btn-main");
        btn.setTransform(true);
        btn.setOrigin(Align.right); // Pivot di kanan untuk efek scaling

        btn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                // Efek hover: geser sedikit ke kiri dan membesar
                if(pointer == -1) {
                    btn.addAction(Actions.parallel(
                        Actions.scaleTo(1.05f, 1.05f, 0.1f),
                        Actions.moveBy(-10f, 0f, 0.1f)
                    ));
                }
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if(pointer == -1) {
                    btn.addAction(Actions.parallel(
                        Actions.scaleTo(1f, 1f, 0.1f),
                        Actions.moveBy(10f, 0f, 0.1f)
                    ));
                }
            }
        });
        return btn;
    }

    public void togglePause() {
        isPaused = !isPaused;

        if (isPaused) {
            Gdx.input.setCursorCatched(false);
            Gdx.input.setInputProcessor(stage);

            // Tampilkan Overlay dengan Fade In
            overlay.setVisible(true);
            overlay.getColor().a = 0f;
            overlay.addAction(Actions.fadeIn(0.2f));

            // Animasi Menu Slide In dari bawah
            pauseContainer.setVisible(true);
            pauseContainer.setPosition(0, -50); // Mulai sedikit di bawah
            pauseContainer.getColor().a = 0f;

            pauseContainer.addAction(Actions.parallel(
                Actions.fadeIn(0.25f, Interpolation.fade),
                Actions.moveTo(0, 0, 0.4f, Interpolation.circleOut)
            ));
        } else {
            Gdx.input.setCursorCatched(true);
            Gdx.input.setInputProcessor(null);
            skipMouseFrames = 3;

            overlay.setVisible(false);
            pauseContainer.setVisible(false);
        }
    }

    public void setPaused(boolean v) {
        if (isPaused != v) togglePause();
    }

    private void updateUI() {
        if (!isPaused) return;

        // Update timer format MM:SS
        int minutes = (int)missionTimer / 60;
        int seconds = (int)missionTimer % 60;
        timeLabel.setText(String.format("T+ %02d:%02d", minutes, seconds));

        // Update koordinat player
        coordLabel.setText(String.format("LOC: %d, %d, %d",
            (int)cam.position.x, (int)cam.position.y, (int)cam.position.z));
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            togglePause();
        }

        if (!isPaused) {
            updateMouseLook();
            updateMovement(delta);
            clampAndStickToTerrain(delta);
            updateFog(delta);
            missionTimer += delta;
        } else {
            // Update data UI saat pause aktif
            updateUI();
        }

        cam.update();

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.08f, 0.1f, 0.14f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);

        renderContext.begin();
        terrain.render(cam, renderContext);
        renderContext.end();

        modelBatch.begin(cam);
        for(ModelInstance tree : treeInstances) modelBatch.render(tree, env);

        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.gl.glDepthMask(false);
        for(ModelInstance fog : fogInstances) modelBatch.render(fog);
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);
        modelBatch.end();

        stage.act(delta);
        stage.draw();
    }

    private void updateMouseLook(){
        if(isPaused || !Gdx.input.isCursorCatched()) return;

        int dx = Gdx.input.getDeltaX();
        int dy = Gdx.input.getDeltaY();

        if(skipMouseFrames > 0){ skipMouseFrames--; return; }

        yawDeg -= dx * mouseSens;
        pitchDeg -= dy * mouseSens;
        pitchDeg = MathUtils.clamp(pitchDeg, -89f, 89f);

        float yawRad = yawDeg * MathUtils.degreesToRadians;
        float pitchRad = pitchDeg * MathUtils.degreesToRadians;
        cam.direction.set(MathUtils.sin(yawRad) * MathUtils.cos(pitchRad), MathUtils.sin(pitchRad), MathUtils.cos(yawRad) * MathUtils.cos(pitchRad)).nor();
    }

    private void updateMovement(float delta){
        float speed = moveSpeed;
        if(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) speed *= sprintMul;

        Vector3 forward = new Vector3(cam.direction.x, 0f, cam.direction.z).nor();
        Vector3 right = new Vector3(forward).crs(Vector3.Y).nor();
        Vector3 move = new Vector3();

        if(Gdx.input.isKeyPressed(Input.Keys.W)) move.add(forward);
        if(Gdx.input.isKeyPressed(Input.Keys.S)) move.sub(forward);
        if(Gdx.input.isKeyPressed(Input.Keys.D)) move.add(right);
        if(Gdx.input.isKeyPressed(Input.Keys.A)) move.sub(right);

        if(move.len2() > 0){
            move.nor();
            int substeps = 4;
            float subDelta = delta / substeps;
            for(int i = 0; i < substeps; i++){
                float stepX = move.x * speed * subDelta;
                float stepZ = move.z * speed * subDelta;

                float probeFar = 0.6f;
                float r = (float)Math.sqrt(cam.position.x * cam.position.x + cam.position.z * cam.position.z);
                float slopeLimit = (r < 80f) ? 1.4f : 0.6f;

                float currentY = terrain.getHeight(cam.position.x, cam.position.z);

                boolean safeX = true;
                float dirX = Math.signum(move.x);
                float probeX_Far = cam.position.x + dirX * probeFar;
                if(terrain.getHeight(probeX_Far, cam.position.z) - currentY > slopeLimit) safeX = false;

                if(safeX) cam.position.x += stepX;
                currentY = terrain.getHeight(cam.position.x, cam.position.z);

                boolean safeZ = true;
                float dirZ = Math.signum(move.z);
                float probeZ_Far = cam.position.z + dirZ * probeFar;
                if(terrain.getHeight(cam.position.x, probeZ_Far) - currentY > slopeLimit) safeZ = false;

                if(safeZ) cam.position.z += stepZ;
            }
        }

        if(Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && isGrounded){
            verticalVelocity = jumpForce;
            isGrounded = false;
        }
    }

    private void clampAndStickToTerrain(float delta){
        cam.position.x = terrain.clampX(cam.position.x, margin);
        cam.position.z = terrain.clampZ(cam.position.z, margin);

        verticalVelocity -= gravity * delta;
        cam.position.y += verticalVelocity * delta;

        float groundHeight = terrain.getHeight(cam.position.x, cam.position.z);
        float minHeight = groundHeight + eyeHeight;

        if(cam.position.y < minHeight){
            cam.position.y = minHeight;
            verticalVelocity = 0f;
            isGrounded = true;
        }else isGrounded = false;
    }

    private void createFogSystem() {
        ModelBuilder modelBuilder = new ModelBuilder();
        Texture kabutTex = createProceduralFogTexture(128);
        Material fogMat = new Material(TextureAttribute.createDiffuse(kabutTex),
            new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA),
            IntAttribute.createCullFace(GL20.GL_NONE));

        long attr = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates;
        modelBuilder.begin();
        MeshPartBuilder meshBuilder = modelBuilder.part("fog_cluster", GL20.GL_TRIANGLES, attr, fogMat);

        Vector3 p1 = new Vector3(-10, -10, 0);
        Vector3 p2 = new Vector3( 10, -10, 0);
        Vector3 p3 = new Vector3( 10,  10, 0);
        Vector3 p4 = new Vector3(-10,  10, 0);
        Vector3 normal = new Vector3(0,0,1);
        meshBuilder.rect(p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, p3.x, p3.y, p3.z, p4.x, p4.y, p4.z, normal.x, normal.y, normal.z);
        fogModel = modelBuilder.end();

        for(int i = 0; i < fogCount; i++){
            ModelInstance fog = new ModelInstance(fogModel);
            float x = MathUtils.random(-160f, 160f);
            float z = MathUtils.random(-160f, 160f);
            float y = MathUtils.random(0, 10);
            fog.transform.setToTranslation(x, y, z);
            fogInstances.add(fog);
        }
    }

    private void updateFog(float delta){
        for(ModelInstance fog : fogInstances){
            Vector3 pos = fog.transform.getTranslation(new Vector3());
            pos.x += fogSpeed * delta;
            if(pos.x > 160f){
                pos.x = -160f;
                pos.z = MathUtils.random(-160f, 160f);
                pos.y = terrain.getHeight(pos.x, pos.z) + MathUtils.random(1f, 5f);
                fog.transform.idt().setToTranslation(pos).rotate(Vector3.Y, MathUtils.random(0f, 360f));
                float s = MathUtils.random(1.5f, 5.0f);
                fog.transform.scale(s, s * 0.6f, s);
            }else fog.transform.setTranslation(pos);
        }
    }

    private Texture createProceduralFogTexture(int size){
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(size, size, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        PerlinNoise texNoise = new PerlinNoise();
        texNoise.frequencyX = 0.07f; texNoise.frequencyZ = 0.07f;
        texNoise.offsetX = MathUtils.random(0, 1000f); texNoise.offsetZ = MathUtils.random(0, 1000f);

        for(int x = 0; x < size; x++){
            for(int y = 0; y < size; y++){
                if(x == 0 || x == size - 1 || y == 0 || y == size - 1){
                    pixmap.setColor(0f, 0f, 0f, 0f); pixmap.drawPixel(x, y); continue;
                }
                float noiseVal = texNoise.getHeight(x, y);
                float dx = x - size/2f; float dy = y - size/2f;
                float dist = (float)Math.sqrt(dx*dx + dy*dy);
                float sphereMask = Math.max(0, 1.0f - (dist / (size/2f)));
                sphereMask = (float)Math.pow(sphereMask, 3.5f);
                float alpha = Math.min(noiseVal * sphereMask * 1.3f, 1.0f);
                pixmap.setColor(0.92f, 0.96f, 1f, alpha);
                pixmap.drawPixel(x, y);
            }
        }
        Texture t = new Texture(pixmap);
        pixmap.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    @Override
    public void resize(int width, int height) {
        cam.viewportWidth = width;
        cam.viewportHeight = height;
        cam.update();
        stage.getViewport().update(width, height, true);
    }

    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void dispose() {
        if(terrain != null) terrain.dispose();
        if(modelBatch != null) modelBatch.dispose();
        if(assets != null) assets.dispose();
        if(fogModel != null) fogModel.dispose();
        if(stage != null) stage.dispose();
    }
}
