package com.finpro7.oop.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.finpro7.oop.PerlinNoise;

public class Terrain implements Disposable {

    public Mesh mesh;
    public Renderable renderable;
    public DefaultShader shader;
    private PerlinNoise perlin;

    private float mountainRadius; // radius gunungnyaa, lebar gunungnya makan tempat di map
    private float mountainPower = 2.4f; // keruncingannya, makin besar makin runcing
//    private float bumpsStrength = 0.35f; // buat ngatur seberaapa keras permukaan gunungnya
    private float hillHeightEdge = 5.0f;  // bukit kecil di pinggir
    private float hillHeightPeak = 10.0f; // tinggi tambahan pas makin deket ke puncak

    // buat bagian jalan spiralnya
    private boolean roadEnabled = true; // buat ada jalan apa gak di gunungnya, kalo off gunung gaada jalan spiralnya
    private float roadWidth = 12f; // meter skala world
    private float roadShoulder = 10f; // area transisi ke tanah
    private float roadCut = 5f; // pahatan jalannya biar keliatan bedanya dengan gunung aslinya
    private int roadTurns = 3; // banyaknya putaran jalan dari bawah ke atas
    private float roadStartRadius; // radius awal di deket dekett kaki gunung
    private float roadEndRadius; // radius akhir di deket deket puncak gunung
    private float roadPitch; // tingkat kemiringan/tanjakan jalan
    private float roadBumpsMul = 0.2f; // seberapa halus jalan dibanding terrain kalo 0.0 = super mulus, 1.0 = jadi gelombang
    //    private float roadFadeTheta = 1.5f; // buat bikin jalan nyatu pelan pelan di ujung sama gunungnya
    private final Vector3 tmpRoad = new Vector3(); // variabel pembantu buat itung itungan biar hemat memori

    // buat bagian puncak
    private float peakFlatRadius = 45.0f; // luas arena boss, makin gede makin luas di puncak gunungnya
    private float peakBlendRange = 30.0f; // buat transisi biar lereng gunung miring sama puncak datar jadi mulus gak patah

    // buat mesh sama datanya
    public final int gridX, gridZ; // resolusi terrain, makin gede makin detail tapi makin berat
    public final float width, depth; // ukuran asli map di dunia game dalam meter
    final float uvScale = 0.1f; // skala texture rumputnya
    private static final int STRIDE = 8; // jarak data antar vertex kyak posisi + normal + UV
    private float[] vertices; // buat nyimpen semua titik tanah.
    private short[] indices; // buat nyimpen urutan sambungan titik tanah yg segitiga
    private Texture texture; // gambar rumputnyaa
    private static final Vector3 tmpN = new Vector3(); // buat ngitung arah normal buat pencahayaan
    // buat nyimpen data mesh
    private int vertsX, vertsZ;
    private float halfW, halfD;
    private float cellX, cellZ;

    public Terrain(Environment env, PerlinNoise perlin, int gridX, int gridZ, float width, float depth) {
        this.gridX = gridX;
        this.gridZ = gridZ;
        this.width = width;
        this.depth = depth;
        this.perlin = perlin;
        this.mountainRadius = Math.min(width, depth) * 0.7f;
        float halfMapSize = Math.min(width, depth) * 0.5f;
        float safetyMargin = 1.0f;
        this.roadStartRadius = halfMapSize - safetyMargin - roadWidth;
        this.roadEndRadius   = this.peakFlatRadius + 2f;
        this.roadPitch       = (roadStartRadius - roadEndRadius) / (roadTurns * MathUtils.PI2);
        ShaderProgram.pedantic = false;
        buildMesh(perlin);
        texture = new Texture(Gdx.files.internal("textures/grass.png"));
        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        Material material = new Material(TextureAttribute.createDiffuse(texture));
        renderable = new Renderable();
        renderable.environment = env;
        renderable.worldTransform.idt();
        renderable.meshPart.set("terrain", this.mesh, 0, this.indices.length, GL20.GL_TRIANGLES);
        renderable.material = material;
        shader = new DefaultShader(renderable);
        shader.init();
    }

    public void getRoadStartPos(Vector3 out){
        // kalo theta 0 itu brarti ujung jalan, pake rumus polar ke cartesian: x = r * cos(theta), z = r * sin(theta)
        // jadi karna theta = 0, maka cos(0)=1, sin(0)=0
        float startX = roadStartRadius;
        float startZ = 0f;
        // buat tau tingginya di titik itu berapa
        float startY = heightAt(startX, startZ);
        out.set(startX, startY, startZ);
    }

    private float ridged01(float n01){
        return 1f - Math.abs(2f * n01 - 1f); // 0..1
    }

    private float mountain01(float x, float z){
        float r = (float)Math.sqrt(x * x + z * z);
        // biar rata setinggi puncak kalo r kurang dari radius arena
        float effectiveR = Math.max(0f, r - peakFlatRadius);
        float effectiveRadius = mountainRadius - peakFlatRadius;
        float t = 1f - (effectiveR / effectiveRadius);
        t = MathUtils.clamp(t, 0f, 1f);
        return (float)Math.pow(t, mountainPower);
    }

    private float heightRaw(float x, float z, float bumpsMul){
        float r = (float)Math.sqrt(x * x + z * z);
        float base01 = mountain01(x, z);
        float baseH  = base01 * perlin.amplitude;
        float n = 0.60f * ridged01(perlin.getHeight(x, z)) + 0.30f * ridged01(perlin.getHeight(x * 2f, z * 2f)) + 0.10f * ridged01(perlin.getHeight(x * 4f, z * 4f));
        float hillH = MathUtils.lerp(hillHeightEdge, hillHeightPeak, base01);
        // kaloo di dalam peakFlatRadius, mask = 0 jadi ga ada noise, kalo diluar pelan pelan jadi 1 jadi ada noise
        float mask = (r - peakFlatRadius) / peakBlendRange;
        mask = MathUtils.clamp(mask, 0f, 1f);
        mask = smoothstep01(mask); // biar transisinya mulus banget
        return baseH + (n * hillH * bumpsMul * mask);
    }

    private static float smoothstep01(float t){
        t = MathUtils.clamp(t, 0f, 1f);
//        return t * t * (3f - 2f * t);
        return t * t * t * (t * (t * 6f - 15f) + 10f); // biar lebih halus jalan sama tanahnya
    }

    // pake newton 3 iter buat cari theta terdekat di spiral
    private float closestThetaOnSpiral(float x, float z, float theta0){
        float theta = theta0;
        float maxTheta = roadTurns * MathUtils.PI2;
        for(int i = 0; i < 3; i++){
            theta = MathUtils.clamp(theta, 0f, maxTheta);
            float r = roadStartRadius - roadPitch * theta;
            if(r < roadEndRadius || r > roadStartRadius) break;
            float c = MathUtils.cos(theta);
            float s = MathUtils.sin(theta);
            float sx = r * c;
            float sz = r * s;
            float dx = sx - x;
            float dz = sz - z;
            // S' turunan 1 yaitu kecepatan buat kecepetan perubahannya kalo theta digeser sedikit
            float dx1 = -roadPitch * c - r * s;
            float dz1 = -roadPitch * s + r * c;
            // S'' turunan 2 yaituu percepatan buat kelengkungan spiral
            float dx2 =  2f * roadPitch * s - r * c;
            float dz2 = -2f * roadPitch * c - r * s;
            float f1 = 2f * (dx * dx1 + dz * dz1);
            float f2 = 2f * ((dx1 * dx1 + dz1 * dz1) + (dx * dx2 + dz * dz2));
            if(Math.abs(f2) < 1e-6f) break;
            float step = f1 / f2;
            step = MathUtils.clamp(step, -0.6f, 0.6f); // biar ga lompat jauh
            theta -= step;
        }
        return MathUtils.clamp(theta, 0f, roadTurns * MathUtils.PI2);
    }

    private boolean closestRoadPointXZ(float x, float z, Vector3 out){
        float half = roadWidth * 0.5f;
        float maxD = half + roadShoulder + 2f; // marginnya
        float r = (float)Math.sqrt(x * x + z * z);
        if(r < roadEndRadius - maxD || r > roadStartRadius + maxD) return false;
        float baseTheta = MathUtils.atan2(z, x);
        if(baseTheta < 0f) baseTheta += MathUtils.PI2; // 0..2pi
        // estimasi index turnnya
        float nFloat = roadTurns * (roadStartRadius - r) / (roadStartRadius - roadEndRadius) - (baseTheta / MathUtils.PI2);
        int nEst = MathUtils.clamp(MathUtils.round(nFloat), 0, roadTurns - 1);
        float bestDist2 = Float.POSITIVE_INFINITY;
        float bestTheta = 0f;
        float bestX = 0f, bestZ = 0f;
        // cek 3 kandidat turn biar stabil
        for(int dn = -1; dn <= 1; dn++){
            int n = nEst + dn;
            if(n < 0 || n >= roadTurns) continue;
            float theta0 = baseTheta + MathUtils.PI2 * n;
            // quick reject kasar
            float rg = roadStartRadius - roadPitch * theta0;
            float gx = rg * MathUtils.cos(theta0);
            float gz = rg * MathUtils.sin(theta0);
            float gdx = x - gx, gdz = z - gz;
            float gdist2 = gdx*gdx + gdz*gdz;
            if(gdist2 > (maxD * maxD) * 9f) continue; // jauh bangett, jadi ga usah pake newton
            float theta = closestThetaOnSpiral(x, z, theta0);
            float rr = roadStartRadius - roadPitch * theta;
            if(rr < roadEndRadius || rr > roadStartRadius) continue;
            float cx = rr * MathUtils.cos(theta);
            float cz = rr * MathUtils.sin(theta);
            float dx = x - cx;
            float dz = z - cz;
            float dist2 = dx*dx + dz*dz;
            if(dist2 < bestDist2){
                bestDist2 = dist2;
                bestTheta = theta;
                bestX = cx;
                bestZ = cz;
            }
        }
        if(bestDist2 == Float.POSITIVE_INFINITY) return false;
        if(bestDist2 > maxD * maxD) return false;
        out.set(bestX, bestTheta, bestZ);
        return true;
    }

    private float heightAt(float x, float z){
        float raw = heightRaw(x, z, 1f);
        if(!roadEnabled) return raw;
        if(!closestRoadPointXZ(x, z, tmpRoad)) return raw;
        float cx = tmpRoad.x;
//        float theta = tmpRoad.y;
        float cz = tmpRoad.z;
        float dx = x - cx;
        float dz = z - cz;
        float d  = (float)Math.sqrt(dx*dx + dz*dz);
        float half = roadWidth * 0.5f;
        float m;
        if(d <= half){
            m = 1f; // inti jalan bener bener rata
        }else if(d >= half + roadShoulder){
            m = 0f;
        }else{
            float t = (d - half) / roadShoulder; // 0..1
            m = 1f - smoothstep01(t);
        }
        //  biar ujung jalan fade in dan fadeout lebih kerasa, tapi nanti player bisa naik ke tingkat 2 jadi dikomen dulu
//        float maxTheta = roadTurns * MathUtils.PI2;
//        float fadeRange = (roadFadeTheta > 0.1f) ? roadFadeTheta : 5.0f;
//        // Hitung progres di awal dan akhir jalan
//        float startProgress = MathUtils.clamp(theta / fadeRange, 0f, 1f);
//        float endProgress = MathUtils.clamp((maxTheta - theta) / fadeRange, 0f, 1f);
//        float cutMultiplier = smoothstep01(startProgress * endProgress); // gabungin
//        float currentCut = roadCut * cutMultiplier;
//        float roadH = heightRaw(cx, cz, roadBumpsMul) - currentCut;
        // tinggi jalan ambil dari centerline biar ga miring ke samping
        float roadH = heightRaw(cx, cz, roadBumpsMul) - roadCut;
        return MathUtils.lerp(raw, roadH, m);
    }

    public float clampX(float x, float margin){
        return MathUtils.clamp(x, -halfW + margin, halfW - margin);
    }

    public float clampZ(float z, float margin){
        return MathUtils.clamp(z, -halfD + margin, halfD - margin);
    }

    private void buildMesh(PerlinNoise perlin){
        this.halfW = width * 0.5f;
        this.halfD = depth * 0.5f;
        this.vertsX = gridX + 1;
        this.vertsZ = gridZ + 1;
        this.cellX = width / gridX;
        this.cellZ = depth / gridZ;
        int vertexCount = this.vertsX * this.vertsZ;
        this.vertices = new float[vertexCount * STRIDE];
        int vi = 0;
        for(int z = 0; z < this.vertsZ; z++){
            float pz = z * this.cellZ - this.halfD;
            for(int x = 0; x < this.vertsX; x++){
                float px = x * this.cellX - this.halfW;
                float eps = Math.min(this.cellX, this.cellZ);
                float h  = heightAt(px, pz);
                float hl = heightAt(px - eps, pz);
                float hr = heightAt(px + eps, pz);
                float hd = heightAt(px, pz - eps);
                float hu = heightAt(px, pz + eps);
                tmpN.set(hl - hr, 2f * eps, hd - hu).nor();
                this.vertices[vi++] = px;
                this.vertices[vi++] = h;
                this.vertices[vi++] = pz;
                this.vertices[vi++] = tmpN.x;
                this.vertices[vi++] = tmpN.y;
                this.vertices[vi++] = tmpN.z;
                this.vertices[vi++] = (px + this.halfW) * uvScale;
                this.vertices[vi++] = (pz + this.halfD) * uvScale;
            }
        }
        this.indices = new short[gridX * gridZ * 6];
        int ii = 0;
        short row = (short) this.vertsX;
        for(short z = 0; z < gridZ; z++){
            for(short x = 0; x < gridX; x++){
                short topLeft = (short) (x + z * row);
                short bottomLeft = (short) (topLeft + row);
                this.indices[ii++] = topLeft;
                this.indices[ii++] = bottomLeft;
                this.indices[ii++] = (short) (topLeft + 1);
                this.indices[ii++] = (short) (topLeft + 1);
                this.indices[ii++] = bottomLeft;
                this.indices[ii++] = (short) (bottomLeft + 1);
            }
        }
        this.mesh = new Mesh(true, vertexCount, this.indices.length,
            new VertexAttribute(VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
            new VertexAttribute(VertexAttributes.Usage.Normal, 3, ShaderProgram.NORMAL_ATTRIBUTE),
            new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0")
        );
        this.mesh.setVertices(this.vertices);
        this.mesh.setIndices(this.indices);
    }


    private float vertexHeight(int x, int z){
        if(vertices == null || vertsX <= 0 || vertsZ <= 0) return 0f;
        x = MathUtils.clamp(x, 0, vertsX - 1);
        z = MathUtils.clamp(z, 0, vertsZ - 1);
        int index = (x + z * vertsX) * STRIDE;
        return vertices[index + 1];
    }

    public float getHeight(float worldX, float worldZ){
        float gx = (worldX + halfW) / cellX;
        float gz = (worldZ + halfD) / cellZ;
        int x0 = (int) Math.floor(gx);
        int z0 = (int) Math.floor(gz);
        float tx = gx - x0;
        float tz = gz - z0;
        float h00 = vertexHeight(x0, z0);
        float h10 = vertexHeight(x0 + 1, z0);
        float h01 = vertexHeight(x0, z0 + 1);
        float h11 = vertexHeight(x0 + 1, z0 + 1);
        float hx0 = MathUtils.lerp(h00, h10, tx);
        float hx1 = MathUtils.lerp(h01, h11, tx);
        return MathUtils.lerp(hx0, hx1, tz);
    }

    public void render(Camera cam, RenderContext context){
        shader.begin(cam, context);
        shader.render(renderable);
        shader.end();
    }

    @Override
    public void dispose(){
        if(mesh != null) mesh.dispose();
        if(shader != null) shader.dispose();
        texture.dispose();
    }
}
