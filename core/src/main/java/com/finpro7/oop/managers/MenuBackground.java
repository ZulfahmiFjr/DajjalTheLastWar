package com.finpro7.oop.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.finpro7.oop.Main;
import com.finpro7.oop.world.PerlinNoise;
import com.finpro7.oop.world.Terrain;

public class MenuBackground {

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

    public MenuBackground(Main game) {
        setupBackgroundWorld(game);
    }

    private void setupBackgroundWorld(Main game) {
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

        for(com.badlogic.gdx.graphics.g3d.Material mat : treeModel.materials){
            mat.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA),
                FloatAttribute.createAlphaTest(0.25f),
                IntAttribute.createCullFace(GL20.GL_NONE));
        }

        terrain = new Terrain(env, perlin, 200, 200, 500f, 500f);
        terrain.generateTrees(treeModel, treeInstances, 800);
    }

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
    }

    public void resize(int width, int height) {
        cam.viewportWidth = width;
        cam.viewportHeight = height;
        cam.update();
    }

    public void dispose() {
        if(modelBatch != null) modelBatch.dispose();
        if(terrain != null) terrain.dispose();
    }
}
