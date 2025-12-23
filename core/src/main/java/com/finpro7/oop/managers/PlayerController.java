package com.finpro7.oop.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.finpro7.oop.entities.PlayerStats;
import com.finpro7.oop.logics.WaveManager;
import com.finpro7.oop.world.Terrain;

public class PlayerController {

    private PerspectiveCamera cam;
    private PlayerStats playerStats;

    // variabel buat ngatur arah pandang mouse
    private float yawDeg, pitchDeg;
    private float mouseSens = 0.14f;

    // variabel buat kecepatan gerak
    private float moveSpeed = 10f;
    private float sprintMul = 2.0f;
    private float eyeHeight = 2.0f;
    private float margin = 1.5f;

    // urusan fisika lompat dan jatuh
    private float verticalVelocity = 0f;
    private float gravity = 30f;
    private float jumpForce = 15f;
    private boolean isGrounded = false;

    // vektor bantuan biar gak bikin objek baru terus
    private final Vector3 tempPos = new Vector3();

    // status lari dan stamina
    public boolean isSprinting = false;
    private boolean staminaLocked = false;
    private float staminaRegenDelay = 3f;
    private float staminaRegenTimer = 0f;

    // konfigurasi pengurangan dan pengisian stamina
    private float staminaDrainSprint = 8f;   // pas lari shift
    private float staminaRegenWalk = 5f;      // pas jalan biasa
    private float staminaRegenIdle = 10f;     // pas diem

    // cooldown buat notifikasi barrier
    private float barrierWarningCooldown = 0f;
    // interface sederhana buat callback warning ke hud
    private WarningListener warningListener;

    public interface WarningListener {
        void onBarrierHit();
    }

    public PlayerController(PerspectiveCamera cam, PlayerStats playerStats) {
        this.cam = cam;
        this.playerStats = playerStats;
    }

    // ini method utama yang dipanggil terus menerus dari gamescreen
    public void update(float delta, Terrain terrain, Array<ModelInstance> treeInstances, WaveManager waveManager, boolean isPaused) {
        // kalo lagi pause, jangan gerak apa apa
        if (isPaused) return;

        // update cooldown warning barrier
        if (barrierWarningCooldown > 0) barrierWarningCooldown -= delta;

        // jalanin fungsi fungsi utamanya
        updateMouseLook();
        updateMovement(delta, terrain, treeInstances, waveManager);
        clampAndStickToTerrain(delta, terrain);
    }

    // fungsi buat nengok kanan kiri atas bawah pake mouse
    private void updateMouseLook() {
        // pastiin kursornya ke lock biar gak lari keluar layar
        if (!Gdx.input.isCursorCatched()) return;

        int dx = Gdx.input.getDeltaX();
        int dy = Gdx.input.getDeltaY();

        yawDeg -= dx * mouseSens;
        pitchDeg -= dy * mouseSens;

        // batasin nengok atas bawah biar leher gak patah
        pitchDeg = MathUtils.clamp(pitchDeg, -89f, 89f);

        float yawRad = yawDeg * MathUtils.degreesToRadians;
        float pitchRad = pitchDeg * MathUtils.degreesToRadians;

        // rumus matematika buat arah kamera
        cam.direction.set(
            MathUtils.sin(yawRad) * MathUtils.cos(pitchRad),
            MathUtils.sin(pitchRad),
            MathUtils.cos(yawRad) * MathUtils.cos(pitchRad)
        ).nor();
    }

    // fungsi paling ribet, ngurusin jalan, lari, stamina, dan tabrakan
    private void updateMovement(float delta, Terrain terrain, Array<ModelInstance> treeInstances, WaveManager waveManager) {
        // cek tombol apa aja yang dipencet
        boolean isMoving =
            Gdx.input.isKeyPressed(Input.Keys.W) ||
                Gdx.input.isKeyPressed(Input.Keys.A) ||
                Gdx.input.isKeyPressed(Input.Keys.S) ||
                Gdx.input.isKeyPressed(Input.Keys.D);

        // update status pemain dulu
        playerStats.update(delta, isMoving);

        boolean shiftPressed = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT);

        // logika stamina pas lari
        if (isMoving && shiftPressed && playerStats.stamina > 0) {
            // lagi sprint nih
            isSprinting = true;
            playerStats.stamina -= staminaDrainSprint * delta;
            staminaRegenTimer = 0f;

            // kalo stamina abis, kunci dulu sampe regen
            if (playerStats.stamina <= 0) {
                playerStats.stamina = 0;
                staminaLocked = true;
            }

        } else {
            isSprinting = false;

            if (!shiftPressed) {
                if (isMoving) {
                    // jalan santai nambah dikit staminanya
                    if (!staminaLocked) {
                        playerStats.stamina += staminaRegenWalk * delta;
                    }
                } else {
                    // kalo diem nambah deres
                    if (staminaLocked) {
                        staminaRegenTimer += delta;
                        if (staminaRegenTimer >= staminaRegenDelay) {
                            staminaLocked = false;
                        }
                    } else {
                        playerStats.stamina += staminaRegenIdle * delta;
                    }
                }
            }
        }

        // jaga jaga biar stamina gak minus atau kelebihan
        playerStats.stamina = MathUtils.clamp(playerStats.stamina, 0f, playerStats.maxStamina);

        // nentuin kecepatan gerak
        float speed = isSprinting ? moveSpeed * sprintMul : moveSpeed;

        // itung arah depan samping
        Vector3 forward = new Vector3(cam.direction.x, 0f, cam.direction.z).nor();
        Vector3 right = new Vector3(forward).crs(Vector3.Y).nor();
        Vector3 move = new Vector3();

        if (Gdx.input.isKeyPressed(Input.Keys.W)) move.add(forward);
        if (Gdx.input.isKeyPressed(Input.Keys.S)) move.sub(forward);
        if (Gdx.input.isKeyPressed(Input.Keys.D)) move.add(right);
        if (Gdx.input.isKeyPressed(Input.Keys.A)) move.sub(right);

        // kalo ada input gerak, kita proses
        if (move.len2() > 0) {
            move.nor();

            // kita pecah langkahnya jadi kecil kecil biar deteksi tabrakan lebih akurat
            int substeps = 4;
            float subDelta = delta / substeps;

            for (int i = 0; i < substeps; i++) {
                float stepX = move.x * speed * subDelta;
                float stepZ = move.z * speed * subDelta;
                float nextX = cam.position.x + stepX;
                float nextZ = cam.position.z + stepZ;

                // logika barrier biar gak bisa nerobos stage yang belum kebuka
                if (!waveManager.isStageCleared()) {
                    float baseAngle = waveManager.getPlayerCurrentAngle();
                    float currentRaw = MathUtils.atan2(cam.position.z, cam.position.x);
                    float nextRaw = MathUtils.atan2(nextZ, nextX);

                    float deltaAngle = nextRaw - currentRaw;
                    if (deltaAngle < -MathUtils.PI) deltaAngle += MathUtils.PI2;
                    else if (deltaAngle > MathUtils.PI) deltaAngle -= MathUtils.PI2;

                    float predictedTotalAngle = baseAngle + deltaAngle;
                    float barrier = waveManager.getAngleBarrier();

                    // cek nabrak barrier gak
                    if (predictedTotalAngle > barrier && deltaAngle > 0.0001f) {
                        // trigger notifikasi kalo cooldown udah aman
                        if (barrierWarningCooldown <= 0 && warningListener != null) {
                            warningListener.onBarrierHit();
                            barrierWarningCooldown = 1.5f;
                        }
                        break; // stop gerak
                    }
                }

                // logika tabrakan sama tanah miring dan pohon
                float probeFar = 0.6f;
                float r = (float) Math.sqrt(cam.position.x * cam.position.x + cam.position.z * cam.position.z);

                // kalo deket puncak (80 meter), toleransi miringnya lebih ketat
                float slopeLimit = (r < 80f) ? 1.4f : 0.6f;

                float currentY = terrain.getHeight(cam.position.x, cam.position.z);

                // cek sumbu x
                boolean safeX = true;
                float dirX = Math.signum(move.x);
                float probeX_Far = cam.position.x + dirX * probeFar;

                if (terrain.getHeight(probeX_Far, cam.position.z) - currentY > slopeLimit) safeX = false;
                if (cekNabrakPohon(cam.position.x + stepX, cam.position.z, treeInstances)) safeX = false;

                if (safeX) cam.position.x += stepX;

                // cek sumbu z
                currentY = terrain.getHeight(cam.position.x, cam.position.z);
                boolean safeZ = true;
                float dirZ = Math.signum(move.z);
                float probeZ_Far = cam.position.z + dirZ * probeFar;

                if (terrain.getHeight(cam.position.x, probeZ_Far) - currentY > slopeLimit) safeZ = false;
                if (cekNabrakPohon(cam.position.x, cam.position.z + stepZ, treeInstances)) safeZ = false;

                if (safeZ) cam.position.z += stepZ;
            }
        }

        // logika lompat
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
            && isGrounded
            && playerStats.stamina > 0f) {

            verticalVelocity = jumpForce;
            isGrounded = false;

            playerStats.stamina -= 10f;
            if (playerStats.stamina < 0) playerStats.stamina = 0;
        }
    }

    // ini buat nempelin kaki ke tanah dan ngurusin gravitasi
    private void clampAndStickToTerrain(float delta, Terrain terrain) {
        // pastiin gak keluar map
        cam.position.x = terrain.clampX(cam.position.x, margin);
        cam.position.z = terrain.clampZ(cam.position.z, margin);

        // tarik ke bawah biar gak melayang terus
        verticalVelocity -= gravity * delta;
        cam.position.y += verticalVelocity * delta;

        float groundHeight = terrain.getHeight(cam.position.x, cam.position.z);
        float minHeight = groundHeight + eyeHeight;

        // kalo tembus tanah, balikin ke atas
        if (cam.position.y < minHeight) {
            cam.position.y = minHeight;
            verticalVelocity = 0f;
            isGrounded = true;
        } else isGrounded = false;
    }

    // cek dulu nih nabrak pohon apa enggak
    private boolean cekNabrakPohon(float x, float z, Array<ModelInstance> treeInstances) {
        float radiusPlayer = 0.5f;
        float radiusPohon = 0.8f;
        float jarakMinimal = radiusPlayer + radiusPohon;
        float jarakMinimalKuadrat = jarakMinimal * jarakMinimal;

        for (ModelInstance tree : treeInstances) {
            tree.transform.getTranslation(tempPos);
            float dx = x - tempPos.x;
            float dz = z - tempPos.z;
            if (dx * dx + dz * dz < jarakMinimalKuadrat) return true;
        }
        return false;
    }

    // setter buat listener warning
    public void setWarningListener(WarningListener listener) {
        this.warningListener = listener;
    }
}
