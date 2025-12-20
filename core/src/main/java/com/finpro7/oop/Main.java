package com.finpro7.oop;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.*;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.*;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.finpro7.oop.world.Terrain;

public class Main extends ApplicationAdapter {

    private PerspectiveCamera cam; // ini ceritanya mata pemain
    private Environment env; // buat ngatur cahaya matahari/ambient
    private RenderContext renderContext;
    // buat world gamenya
    private PerlinNoise perlin;
    private Terrain terrain;
    private Model treeModel;
    private AssetManager assets;
    private Array<ModelInstance> treeInstances = new Array<>();
    private ModelBatch modelBatch; // batch buat render objek 3D kyak pohon, musuh, dll

    private float yawDeg; // buat kamera nengok kanan kiri
    private float pitchDeg; // nengok atas bawah
    private float mouseSens = 0.14f; // buat sensitivitas mousenyaa
    private float moveSpeed = 10f; // buat kecepatan jalan santainya
    private float sprintMul = 2.0f; // buat pengali jalan cepernya kalo pencet shift
    private float eyeHeight = 2.0f; // tinggi badan player biar pandangannya gak nyusruk tanah
    private float margin = 1.5f; // batas aman biar ga jatoh ke ujung stiap sisi map void
    // bagian lompat player
    private float verticalVelocity = 0f; // kecepatan vertikal naik/turun
    private float gravity = 30f; // ini kekuatan tarikan world, makin gede makin cepet jatuh
    private float jumpForce = 15f; // kekuatan dorongan kaki pass loncat, makin tinggi lomcatnya makin tinggi
    private boolean isGrounded = false; // status lagi napak tanah atau lagi loncat
    private int skipMouseFrames = 3; // buat skip 3 frame awal, nyegah snap
    // bagian gerakin kabut
    private Model fogModel;
    private Array<ModelInstance> fogInstances = new Array<>();
    private float fogSpeed = 2.0f; // kecepatan gerak kabutnya
    private int fogCount = 200; // jumlah gumpalan kabut, makin banyak makin tebel tapi makin berat

    @Override
    public void create() {
        // setup kamera perspektif biar kyak mata manusia ada jauh dekatnya
        cam = new PerspectiveCamera(67f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.near = 0.1f; // jarak pandang terdekat
        cam.far = 200f; // jarak pandang terjauh, dipendekin jadi 100 buat efek kabut
        // buat setup pencahayaan biar gak gelap gulita
        env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.6f, 1f)); // cahaya dasar
        env.add(new DirectionalLight().set(1f, 1f, 1f, -0.6f, -1f, -0.3f)); // matahari
        env.set(new ColorAttribute(ColorAttribute.Fog, 0.08f, 0.1f, 0.14f, 1f)); // tambahin atribut kabut ke env
        renderContext = new RenderContext(new DefaultTextureBinder(DefaultTextureBinder.LRU, 1));
        modelBatch = new ModelBatch(); // inisialisasi ModelBatch buat model
        // buat setup generator perlin noisenya biar gunungnya random tiap kali playy
        perlin = new PerlinNoise();
//        perlin.terrainHeight = 6f;
        perlin.amplitude = 80f; // tinggi maksimal gunung
        perlin.frequencyX = 0.08f;
        perlin.frequencyZ = 0.08f;
        perlin.offsetX = MathUtils.random(0f, 999f); // geser seed random
        perlin.offsetZ = MathUtils.random(0f, 999f);
        assets = new AssetManager(); // setup asset manager, buat model model 3d
//        assets.setLoader(Model.class, ".g3db", new G3dModelLoader(new UBJsonReader()));
        assets.load("models/pohon.g3dj", Model.class); // load file model pohon dari folder assets/models/
        assets.load("textures/batang_pohon.png", Texture.class);
        assets.load("textures/daun_pohon.png", Texture.class);
        assets.finishLoading(); // ngeloading dulu biar simpel codinganny
        // ambil model buat pohon, dllnya
        treeModel = assets.get("models/pohon.g3dj", Model.class);
        ModelBuilder modelBuilder = new ModelBuilder();
        // bikin tekstur kabut procedural 128x128 biar lumayan tajem
        Texture kabutTex = createProceduralFogTexture(128);
        Material fogMat = new Material(
            TextureAttribute.createDiffuse(kabutTex),
            // atur blending biar transparan kyak asep beneran
            new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA),
            IntAttribute.createCullFace(GL20.GL_NONE)
        );
        long attr = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates;
        modelBuilder.begin();
        MeshPartBuilder meshBuilder = modelBuilder.part("fog_cluster", GL20.GL_TRIANGLES, attr, fogMat);
        // loop buat numpuk beberapa plane biar jadi gumpalan yg bervolume
        int planesCount = 1;
        float baseSize = 10f; // ukuran dasar per lembarnya
        for(int i = 0; i < planesCount; i++){
            // cari sumbu putar acak
            Vector3 axis = new Vector3(MathUtils.random(-1f, 1f), MathUtils.random(-1f, 1f), MathUtils.random(-1f, 1f)).nor();
            float angle = MathUtils.random(0f, 360f); // sudut putar random
            // geser dikit posisinya biar ga numpuk pas di tengah
            Vector3 offset = new Vector3(MathUtils.random(-1.5f, 1.5f), MathUtils.random(-1.5f, 1.5f), MathUtils.random(-1.5f, 1.5f));
            // tentuin 4 titik sudut plane secara manual, anggep awalnya madep depan
            Vector3 p1 = new Vector3(-baseSize, -baseSize, 0); // kiri bawah
            Vector3 p2 = new Vector3( baseSize, -baseSize, 0); // kanan bawah
            Vector3 p3 = new Vector3( baseSize,  baseSize, 0); // kanan atas
            Vector3 p4 = new Vector3(-baseSize,  baseSize, 0); // kiri atas
            Vector3 normal = new Vector3(0, 0, 1); // arah normal
            // terapin rotasi sama offset ke titik titik tadi
            p1.rotate(axis, angle).add(offset);
            p2.rotate(axis, angle).add(offset);
            p3.rotate(axis, angle).add(offset);
            p4.rotate(axis, angle).add(offset);
            normal.rotate(axis, angle);
            // masukin data titiknya ke mesh builder
            meshBuilder.rect(
                p1.x, p1.y, p1.z,
                p2.x, p2.y, p2.z,
                p3.x, p3.y, p3.z,
                p4.x, p4.y, p4.z,
                normal.x, normal.y, normal.z
            );
        }
        fogModel = modelBuilder.end();
        // bikin terrainnya grid 254x254, ukuran worldnya 320x320 meter
        terrain = new Terrain(env, perlin, 254, 254, 320f, 320f);
        // sebar objek kabutnya ke seluruh map
        for(int i = 0; i < fogCount; i++){
            ModelInstance fog = new ModelInstance(fogModel);
            // random posisi X sama Z nyaa
            float x = MathUtils.random(-160f, 160f);
            float z = MathUtils.random(-160f, 160f);
            float yT = terrain.getHeight(x, z); // ambil tinggi tanah di koordinat itu biar kabutnya napak
            float y = MathUtils.random(yT, yT + 5f); // posisi y kabut random minimal sesuai tinggi tanah
            fog.transform.setToTranslation(x, y + MathUtils.random(1f, 5f), z); // set posisi awalnya, y ditambah dikit biar ngambang
            fog.transform.rotate(Vector3.Y, MathUtils.random(0f, 360f)); // rotasi acak biar ga keliatan seragam madepnya
            float randomScale = MathUtils.random(1.5f, 5.0f); // random ukurannya biar variatif, ada yg gede ada yg kecil
            fog.transform.scale(randomScale, randomScale * 0.6f, randomScale); // skala Y dipenyetin dikit biar kyak lapisan tipis
            fogInstances.add(fog);
        }
        // biar daunnya transparan & keliatan dari dua sisi
        for(Material mat : treeModel.materials){
            // cek material daun index 1
            if(treeModel.materials.indexOf(mat, true) == 1){
                mat.set(
                    new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA),
                    FloatAttribute.createAlphaTest(0.25f),
                    IntAttribute.createCullFace(GL20.GL_NONE)
                );
            }
        }
//        terrain = new Terrain(env, perlin, 160, 160, 80f, 80f);
        terrain.generateTrees(treeModel, treeInstances, 600); // generate pohon pohonnya di terrain sebanyak 600 secara random
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

    // buat gerakin kabutnya biar kayak kebawa angin
    private void updateFog(float delta){
        for(ModelInstance fog : fogInstances){
            Vector3 pos = fog.transform.getTranslation(new Vector3()); // ambil posisi kabut sekarang
            pos.x += fogSpeed * delta; // geser posisinya ke arah X positif
            // cek kalo kabutnya udah kejauhan lewat batas map kanan
            if(pos.x > 160f){
                pos.x = -160f; // lempar balik ke ujung kiri
                pos.z = MathUtils.random(-160f, 160f); // acak posisi Z baru
                pos.y = terrain.getHeight(pos.x, pos.z) + MathUtils.random(1f, 5f); // sesuain tingginya sama tanah plus random dikit
                // pas respawn acak ulang rotasi & ukurannya biar ga monoton bentuknya
                fog.transform.idt();
                fog.transform.setToTranslation(pos);
                fog.transform.rotate(Vector3.Y, MathUtils.random(0f, 360f));
                float s = MathUtils.random(1.5f, 5.0f);
                fog.transform.scale(s, s * 0.6f, s);
            }else fog.transform.setTranslation(pos); // pake setTranslation biar cuma geser posisi doang tanpa ngerusak rotasi atau ukuran yg udah kita set sebelumnya
        }
    }

    // buat bikin tekstur kabut manual tanpa butuh file gambar\
    private Texture createProceduralFogTexture(int size){
        // bikin pixmap baru buat nampung pixelnya
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(size, size, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        PerlinNoise texNoise = new PerlinNoise();
        // frekuensinya dikecilin biar gumpalan kabutnya lebih gede dan alus gak pecah pecahh
        texNoise.frequencyX = 0.07f;
        texNoise.frequencyZ = 0.07f;
        texNoise.offsetX = MathUtils.random(0, 1000f);
        texNoise.offsetZ = MathUtils.random(0, 1000f);
        for(int x = 0; x < size; x++){
            for(int y = 0; y < size; y++){
                // biar gk ada garis kotak samar aneh di pinggirannyaa
                if(x == 0 || x == size - 1 || y == 0 || y == size - 1){
                    pixmap.setColor(0f, 0f, 0f, 0f);
                    pixmap.drawPixel(x, y);
                    continue;
                }
                float noiseVal = texNoise.getHeight(x, y);
                // itung jarak dari titik tengah buat bikin masking bunder
                float centerX = size / 2f;
                float centerY = size / 2f;
                float dx = x - centerX;
                float dy = y - centerY;
                float dist = (float)Math.sqrt(dx*dx + dy*dy);
                float radius = size / 2f;
                float ratio = dist / radius;
                // rumus masking bunder pake pangkat biar gradasinya lembut ke pinggir
                float sphereMask = 1.0f - ratio;
                if(sphereMask < 0) sphereMask = 0;
                sphereMask = (float)Math.pow(sphereMask, 3.5f);
                float alpha = noiseVal * sphereMask;
                // tebelin dikit bagian tengahnya biar lebih nampak
                alpha = Math.min(alpha * 1.3f, 1.0f);
                // kasih warna putih kebiruan dikit
                pixmap.setColor(0.92f, 0.96f, 1f, alpha);
                pixmap.drawPixel(x, y);
            }
        }
        Texture texture = new Texture(pixmap);
        pixmap.dispose(); // buang pixmapnya kalo udh jadi tekstur
        // set filter linear biar pixelnya ngeblur halus jadi gak keliatan kotak kotak pixelnya
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return texture;
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
            move.nor();
            // mecah gerakan satu frame jadi 4 step, biar ga ada celah buat nembus tembok spiral pas lagi ngebut/loncat
            int substeps = 4;
            float subDelta = delta / substeps;
            for(int i = 0; i < substeps; i++){
                float stepX = move.x * speed * subDelta;
                float stepZ = move.z * speed * subDelta;
                float probeFar = 0.6f;
                float probeNear = 0.2f;
                float r = (float)Math.sqrt(cam.position.x * cam.position.x + cam.position.z * cam.position.z);
                float slopeLimit = 0.6f; // keketatan biar ga bisa panjat tebing
                // pas dekat puncak radius < 80 kita toleransi lebih tinggi biar bisa lewat gundukan transisinya
                if(r < 80f)  slopeLimit = 1.4f;
                // update posisi tanah tempat kita berdiri setiap step kecil
                float currentY = terrain.getHeight(cam.position.x, cam.position.z);
                boolean safeX = true;
                // sensor jauh x
                float dirX = Math.signum(move.x);
                float dirZ = Math.signum(move.z);
                float probeX_Far = cam.position.x + dirX * probeFar;
                float diffX_Far = terrain.getHeight(probeX_Far, cam.position.z) - currentY;
                if(diffX_Far > slopeLimit) safeX = false;
                // sensor deket x buat gundukan lancip
                if(safeX){
                    float probeX_Near = cam.position.x + dirX * probeNear;
                    float diffX_Near = terrain.getHeight(probeX_Near, cam.position.z) - currentY;
                    if(diffX_Near > slopeLimit) safeX = false;
                }
                // bisa gerak arah x kalo aman
                if(safeX)cam.position.x += stepX;
                // refresh currentY lagi karna x mungkin udah geser
                currentY = terrain.getHeight(cam.position.x, cam.position.z);
                float probeDiagX = cam.position.x + move.x * probeFar;
                float probeDiagZ = cam.position.z + move.z * probeFar;
                float diffDiag = terrain.getHeight(probeDiagX, probeDiagZ) - currentY;
                // kalo di depan arah gerak ada tanjakan tebing, stop total
                if(diffDiag > slopeLimit) continue; // skip substep ini, gaa maju samsek
                float radius = 0.4f; // kira kira lebar badan player
                float sideX = -move.z * radius;
                float sideZ =  move.x * radius;
                float hL = terrain.getHeight(probeDiagX + sideX, probeDiagZ + sideZ) - currentY;
                float hR = terrain.getHeight(probeDiagX - sideX, probeDiagZ - sideZ) - currentY;
                if(Math.max(hL, hR) > slopeLimit) continue;
                boolean safeZ = true;
                // sensor jauh z
                float probeZ_Far  = cam.position.z + dirZ * probeFar;
                float diffZ_Far = terrain.getHeight(cam.position.x, probeZ_Far) - currentY;
                if(diffZ_Far > slopeLimit) safeZ = false;
                // sensor deketnya Z
                if(safeZ){
                    float probeZ_Near = cam.position.z + dirZ * probeNear;
                    float diffZ_Near = terrain.getHeight(cam.position.x, probeZ_Near) - currentY;
                    if(diffZ_Near > slopeLimit) safeZ = false;
                }
                // bisa gerak sumbu z kalo aman
                if(safeZ) cam.position.z += stepZ;
            }
        }
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
        Gdx.gl.glClearColor(0.08f, 0.1f, 0.14f, 1f); // set warna background jadi gelap kebiruan kyak suasana malem
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT); // bersihin buffer warna sama depth biar bersih buat frame baru
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST); // idupin depth test biar objek yg harusnya di belakang gak nongol di depan
        Gdx.gl.glEnable(GL20.GL_CULL_FACE); // optimasi cull face biar sisi belakang model yg gak keliatan gak usah digambar
        renderContext.begin();
        terrain.render(cam, renderContext); // render tanahnya duluan
        renderContext.end();
        updateFog(delta); // update posisi kabut sebelum dirender
        modelBatch.begin(cam);
        for(ModelInstance tree : treeInstances){
            modelBatch.render(tree, env); // masukin ke env biar pohonnya kena cahaya matahari dan keliatan
        }
        Gdx.gl.glDisable(GL20.GL_CULL_FACE); // matiin culling dulu sebelum render kabut biar teksturnya keliatan dari depan belakang, jadi ga ilang pas kita liat dari sisi sebaliknya
        Gdx.gl.glDepthMask(false); // matiin depth mask biar transparansinya alus, jadi ga numpuk aneh sama objek di belakangnya
        for(ModelInstance fog : fogInstances){
            modelBatch.render(fog); // render kabutnya ga pake lighting biar dia tetep keliatan putih samar kyak asep walau worldnya lagi gelap
        }
        // nyalain lagi depth mask sama cullingnya buat performa objek lain
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);
        modelBatch.end();
    }

    @Override
    public void dispose() {
        if(terrain != null) terrain.dispose();
        if(modelBatch != null) modelBatch.dispose(); // bersihin modelbatch
        if(assets != null) assets.dispose(); // bersihin assets model dan texturenya yg udh diload
        if(fogModel != null) fogModel.dispose(); // bersihin model kabutnya
    }
}
