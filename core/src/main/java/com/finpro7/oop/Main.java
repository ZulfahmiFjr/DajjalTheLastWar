package com.finpro7.oop;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

import com.finpro7.oop.world.Terrain;

public class Main extends ApplicationAdapter {

    private PerspectiveCamera cam; // ini ceritanya mata pemain
    private Environment env; // buat ngatur cahaya matahari/ambient
    private RenderContext renderContext;
    // buat world gamenya
    private PerlinNoise perlin;
    private Terrain terrain;

    private float yawDeg; // buat kamera nengok kanan kiri
    private float pitchDeg; // nengok atas bawah
    private float mouseSens = 0.14f; // buat sensitivitas mousenyaa
    private float moveSpeed = 10f; // buat kecepatan jalan santainya
    private float sprintMul = 2.0f; // buat pengali jalan cepernya kalo pencet shift
    private float eyeHeight = 2.0f; // tinggi badan player biar pandangannya gak nyusruk tanah
    private float margin = 1.5f; // batas aman biar ga jatoh ke ujung stiap sisi map void
    // bagian lompat player
    private float verticalVelocity = 0f; // kecepatan vertikal naik/turun
    private float gravity = 25f; // ini kekuatan tarikan world, makin gede makin cepet jatuh
    private float jumpForce = 12f; // kekuatan dorongan kaki pass loncat, makin tinggi lomcatnya makin tinggi
    private boolean isGrounded = false; // status lagi napak tanah atau lagi loncat
    private int skipMouseFrames = 3; // buat skip 3 frame awal, nyegah snap

    @Override
    public void create() {
        // setup kamera perspektif biar kyak mata manusia ada jauh dekatnya
        cam = new PerspectiveCamera(67f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.near = 0.1f; // jarak pandang terdekat
        cam.far = 800f; // jarak pandang terdekat
        // buat setup pencahayaan biar gak gelap gulita
        env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.6f, 1f)); // cahaya dasar
        env.add(new DirectionalLight().set(1f, 1f, 1f, -0.6f, -1f, -0.3f)); // matahari
        renderContext = new RenderContext(new DefaultTextureBinder(DefaultTextureBinder.LRU, 1));
        // buat setup generator perlin noisenya biar gunungnya random tiap kali playy
        perlin = new PerlinNoise();
//        perlin.terrainHeight = 6f;
        perlin.amplitude = 80f; // tinggi maksimal gunung
        perlin.frequencyX = 0.08f;
        perlin.frequencyZ = 0.08f;
        perlin.offsetX = MathUtils.random(0f, 999f); // geser seed random
        perlin.offsetZ = MathUtils.random(0f, 999f);
//        terrain = new Terrain(env, perlin, 160, 160, 80f, 80f);
        // bikin terrainnya grid 254x254, ukuran worldnya 320x320 meter
        terrain = new Terrain(env, perlin, 254, 254, 320f, 320f);
        // buat ngatur spawn playernya
        Vector3 startPos = new Vector3();
        terrain.getRoadStartPos(startPos); // minta koordinat start, biar di awal jalan spiral startnya
        cam.position.set(startPos.x + 5.0f, startPos.y + eyeHeight, startPos.z + 5.0f); // buat set posisi kamera
        Vector3 lookTarget = new Vector3();
        // Panggil methodnya aja, jangan itung manual di sini!
        terrain.getRoadLookAtPos(lookTarget);
        // ngitung sudut dari vektor posisi
        float dx = lookTarget.x - cam. position.x;
        float dy = lookTarget.y - cam. position.y;
        float dz = lookTarget.z - cam.position.z;
        // ngitung yaw dan pitch dari vektor offset
        yawDeg = MathUtils. atan2(dx, dz)*MathUtils.radiansToDegrees;
        float horizontalDist = (float)Math.sqrt(dx * dx + dz * dz);
        pitchDeg = MathUtils. atan2(dy, horizontalDist) * MathUtils.radiansToDegrees;
        // set direction dari sudut yg baru dihitung biar konsisten
        float yawRad = yawDeg * MathUtils.degreesToRadians;
        float pitchRad = pitchDeg * MathUtils.degreesToRadians;
        cam.direction.set(MathUtils. sin(yawRad) * MathUtils.cos(pitchRad), MathUtils.sin(pitchRad), MathUtils.cos(yawRad) * MathUtils.cos(pitchRad)).nor();
        cam.up.set(Vector3.Y);
        cam.update();
        skipMouseFrames = 3;
        Gdx.input.setCursorCatched(true); // buat ngunci kursor mouse biar ga lari lari keluar jendela game
//        Gdx.input.getDeltaX();
//        Gdx.input.getDeltaY();
    }

    @Override
    public void resize(int width, int height) {
        // update viewport kalo jendela game diresize
        cam.viewportWidth = width;
        cam.viewportHeight = height;
        cam.update();
    }

    // buat nengok nengok pake mouse
    private void updateMouseLook(){
        // biar bisa unlock mouse kalo neken ESC
        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)){
            boolean now = !Gdx.input.isCursorCatched();
            Gdx.input.setCursorCatched(now);
            if(now) skipMouseFrames = 3;
        }
        // kalo mouse lagi ga dikunci ga usah update kamera
        if(!Gdx.input.isCursorCatched()) return;
        int dx = Gdx. input.getDeltaX();
        int dy = Gdx. input.getDeltaY();
        if(skipMouseFrames > 0){
            skipMouseFrames--;
            return;
        }
        if(Math.abs(dx) > 100 || Math.abs(dy) > 100) return; // buang delta yg gak wajar, biar gk snap saat awal
        // ambil pergerakan mousenya yaitu deltanya
        yawDeg -= dx * mouseSens;
        pitchDeg -= dy * mouseSens;
        pitchDeg = MathUtils.clamp(pitchDeg, -89f, 89f); // batesin nengok atas bawah/clamp biar leher player gaa patah, max 89 derajat
        // buat ngonversi sudut yaw/pitch ke vektor arah jadi arah X, Y, Z
        float yawRad = yawDeg * MathUtils.degreesToRadians;
        float pitchRad = pitchDeg * MathUtils.degreesToRadians;
        cam.direction.set(MathUtils.sin(yawRad) * MathUtils.cos(pitchRad), MathUtils.sin(pitchRad), MathUtils.cos(yawRad) * MathUtils.cos(pitchRad)).nor();
    }

    // ini logika jalan WASDnya
    private void updateMovement(float delta){
        float speed = moveSpeed;
        if(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) speed *= sprintMul; // biar bisa lari
        // ambil arah depan kamera tapi Ynya dinolin, biar kalo kita nunduk jalannya tetep maju kedepan, bukan masuk tanah
        Vector3 forward = new Vector3(cam.direction.x, 0f, cam.direction.z);
        if(forward.len2() < 1e-6f) forward.set(0, 0, 1);
        forward.nor();
        // itung arah kanan pake cross product depan x atas
        Vector3 right = new Vector3(forward).crs(Vector3.Y).nor();
        Vector3 move = new Vector3();
        // input keyboardnya
        if(Gdx.input.isKeyPressed(Input.Keys.W)) move.add(forward);
        if(Gdx.input.isKeyPressed(Input.Keys.S)) move.sub(forward);
        if(Gdx.input.isKeyPressed(Input.Keys.D)) move.add(right);
        if(Gdx.input.isKeyPressed(Input.Keys.A)) move.sub(right);
        // buat eksekusi gerak
        if(move.len2() > 0){
            move.nor().scl(speed * delta);
            cam.position.add(move);
        }
        // buat tes doang, kalo space/ctrl bisa terbang
//        if(Gdx.input.isKeyPressed(Input.Keys.SPACE)) cam.position.y += speed * 0.6f * delta;
//        if(Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) cam.position.y -= speed * 0.6f * delta;
        // kalo pencet spasi dan lagi napak tanah biar gaa double jump di udara
        if(Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && isGrounded){
            verticalVelocity = jumpForce; // dorong ke atas alias loncat
            isGrounded = false; // set jadi lagi melayang sekarang
        }
    }

    // buat logika napak tanahnyaa
    private void clampAndStickToTerrain(float delta){
        // batesin gerak biar ga keluar map atau biar gk clamp
        cam.position.x = terrain.clampX(cam.position.x, margin);
        cam.position.z = terrain.clampZ(cam.position.z, margin);
//        cam.position.y = terrain.getHeight(cam.position.x, cam.position.z) + eyeHeight; // buat cari tinggi tanah x z di terrain
        verticalVelocity -= gravity * delta; // ngurangin kecepatan vertikal pake gravitasi tiap frame
        cam.position.y += verticalVelocity * delta; // nerapin kecepatan juga ke posisi kamera
        // ngecek nyentuh tanah apa engga
        float groundHeight = terrain.getHeight(cam.position.x, cam.position.z);
        float minHeight = groundHeight + eyeHeight;
        // kalo posisi kita nembus ke bawah tanah
        if(cam.position.y < minHeight){
            cam.position.y = minHeight; // balikin ke atas tanah
            verticalVelocity = 0f; // reset kecepatan jatuhnya
            isGrounded = true; // set kaki napak tanah
        }else isGrounded = false; // kaloo ga napak tanah berarti lagi loncat jadi set ke false;
//        cam.update();
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        // update logika game
        updateMouseLook();
        updateMovement(delta);
        clampAndStickToTerrain(delta);
        cam.update(); // update kamera cuman di sini, buat nyegah snap
        // bersihin layar
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.08f, 0.1f, 0.14f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE); // optimasi biar ga render sisi belakang
        renderContext.begin();
        terrain.render(cam, renderContext);
        renderContext.end();
    }

    @Override
    public void dispose() {
        if(terrain != null) terrain.dispose();
    }
}
