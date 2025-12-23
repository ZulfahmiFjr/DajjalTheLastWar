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
import com.finpro7.oop.inputs.InputHandler; // import yang baru dibuat

public class PlayerController {

    private PerspectiveCamera cam;
    private PlayerStats playerStats;

    // tambahin input handler di sini
    private InputHandler inputHandler;

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

    // ini variabel penampung input gerakan dari command
    private final Vector3 inputDirection = new Vector3();

    // status lari dan stamina
    public boolean isSprinting = false;
    private boolean staminaLocked = false;
    private float staminaRegenDelay = 3f;
    private float staminaRegenTimer = 0f;

    // konfigurasi pengurangan dan pengisian stamina
    private float staminaDrainSprint = 8f;
    private float staminaRegenWalk = 5f;
    private float staminaRegenIdle = 10f;

    // cooldown buat notifikasi barrier
    private float barrierWarningCooldown = 0f;
    private WarningListener warningListener;

    public interface WarningListener {
        void onBarrierHit();
    }

    public PlayerController(PerspectiveCamera cam, PlayerStats playerStats) {
        this.cam = cam;
        this.playerStats = playerStats;
        // inisialisasi input handler pas controller dibuat
        this.inputHandler = new InputHandler();
    }

    // ini method utama yang dipanggil terus menerus dari gamescreen
    public void update(float delta, Terrain terrain, Array<ModelInstance> treeInstances, WaveManager waveManager, boolean isPaused) {
        if (isPaused) return;

        if (barrierWarningCooldown > 0) barrierWarningCooldown -= delta;

        updateMouseLook();
        updateMovement(delta, terrain, treeInstances, waveManager);
        clampAndStickToTerrain(delta, terrain);
    }

    private void updateMouseLook() {
        if (!Gdx.input.isCursorCatched()) return;

        int dx = Gdx.input.getDeltaX();
        int dy = Gdx.input.getDeltaY();

        yawDeg -= dx * mouseSens;
        pitchDeg -= dy * mouseSens;

        pitchDeg = MathUtils.clamp(pitchDeg, -89f, 89f);

        float yawRad = yawDeg * MathUtils.degreesToRadians;
        float pitchRad = pitchDeg * MathUtils.degreesToRadians;

        cam.direction.set(
            MathUtils.sin(yawRad) * MathUtils.cos(pitchRad),
            MathUtils.sin(pitchRad),
            MathUtils.cos(yawRad) * MathUtils.cos(pitchRad)
        ).nor();
    }

    private void updateMovement(float delta, Terrain terrain, Array<ModelInstance> treeInstances, WaveManager waveManager) {
        // reset input direction setiap frame sebelum diproses sama input handler
        inputDirection.set(0, 0, 0);

        // panggil input handler buat ngisi inputDirection dan cek tombol lainnya
        // ini gantiin if else keyboard yang tadinya numpuk di sini
        inputHandler.handleInput(this);

        // cek apakah ada input gerak
        boolean isMoving = inputDirection.len2() > 0;

        // update status pemain dulu
        playerStats.update(delta, isMoving);

        // logika stamina pas lari
        if (isMoving && isSprinting && playerStats.stamina > 0) {
            playerStats.stamina -= staminaDrainSprint * delta;
            staminaRegenTimer = 0f;

            if (playerStats.stamina <= 0) {
                playerStats.stamina = 0;
                staminaLocked = true;
            }

        } else {
            // kalo gak lari set false
            if (staminaLocked || playerStats.stamina <= 0) isSprinting = false;

            if (!isSprinting) {
                if (isMoving) {
                    if (!staminaLocked) {
                        playerStats.stamina += staminaRegenWalk * delta;
                    }
                } else {
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

        playerStats.stamina = MathUtils.clamp(playerStats.stamina, 0f, playerStats.maxStamina);

        float speed = isSprinting ? moveSpeed * sprintMul : moveSpeed;

        // pake inputDirection yang udah diisi sama command
        if (isMoving) {
            inputDirection.nor(); // normalisasi biar jalannya gak ngebut pas miring

            int substeps = 4;
            float subDelta = delta / substeps;

            for (int i = 0; i < substeps; i++) {
                float stepX = inputDirection.x * speed * subDelta;
                float stepZ = inputDirection.z * speed * subDelta;
                float nextX = cam.position.x + stepX;
                float nextZ = cam.position.z + stepZ;

                if (!waveManager.isStageCleared()) {
                    float baseAngle = waveManager.getPlayerCurrentAngle();
                    float currentRaw = MathUtils.atan2(cam.position.z, cam.position.x);
                    float nextRaw = MathUtils.atan2(nextZ, nextX);

                    float deltaAngle = nextRaw - currentRaw;
                    if (deltaAngle < -MathUtils.PI) deltaAngle += MathUtils.PI2;
                    else if (deltaAngle > MathUtils.PI) deltaAngle -= MathUtils.PI2;

                    float predictedTotalAngle = baseAngle + deltaAngle;
                    float barrier = waveManager.getAngleBarrier();

                    if (predictedTotalAngle > barrier && deltaAngle > 0.0001f) {
                        if (barrierWarningCooldown <= 0 && warningListener != null) {
                            warningListener.onBarrierHit();
                            barrierWarningCooldown = 1.5f;
                        }
                        break;
                    }
                }

                float probeFar = 0.6f;
                float r = (float) Math.sqrt(cam.position.x * cam.position.x + cam.position.z * cam.position.z);
                float slopeLimit = (r < 80f) ? 1.4f : 0.6f;

                float currentY = terrain.getHeight(cam.position.x, cam.position.z);

                boolean safeX = true;
                float dirX = Math.signum(inputDirection.x);
                float probeX_Far = cam.position.x + dirX * probeFar;

                if (terrain.getHeight(probeX_Far, cam.position.z) - currentY > slopeLimit) safeX = false;
                if (cekNabrakPohon(cam.position.x + stepX, cam.position.z, treeInstances)) safeX = false;

                if (safeX) cam.position.x += stepX;

                currentY = terrain.getHeight(cam.position.x, cam.position.z);
                boolean safeZ = true;
                float dirZ = Math.signum(inputDirection.z);
                float probeZ_Far = cam.position.z + dirZ * probeFar;

                if (terrain.getHeight(cam.position.x, probeZ_Far) - currentY > slopeLimit) safeZ = false;
                if (cekNabrakPohon(cam.position.x, cam.position.z + stepZ, treeInstances)) safeZ = false;

                if (safeZ) cam.position.z += stepZ;
            }
        }
        // logic lompat dipindah ke method performJump yang dipanggil command
    }

    // ini method method bantuan buat command biar bisa akses internal controller

    // buat nambah vektor gerakan dari command
    public void addMovementInput(Vector3 dir) {
        inputDirection.add(dir);
    }

    // buat ambil arah kamera
    public Vector3 getCamDirection() {
        return cam.direction;
    }

    // buat set status lari
    public void setSprinting(boolean sprinting) {
        this.isSprinting = sprinting;
    }

    // logic lompat yang dipanggil command jump
    public void performJump() {
        if (isGrounded && playerStats.stamina > 0f) {
            verticalVelocity = jumpForce;
            isGrounded = false;

            playerStats.stamina -= 10f;
            if (playerStats.stamina < 0) playerStats.stamina = 0;
        }
    }

    private void clampAndStickToTerrain(float delta, Terrain terrain) {
        cam.position.x = terrain.clampX(cam.position.x, margin);
        cam.position.z = terrain.clampZ(cam.position.z, margin);

        verticalVelocity -= gravity * delta;
        cam.position.y += verticalVelocity * delta;

        float groundHeight = terrain.getHeight(cam.position.x, cam.position.z);
        float minHeight = groundHeight + eyeHeight;

        if (cam.position.y < minHeight) {
            cam.position.y = minHeight;
            verticalVelocity = 0f;
            isGrounded = true;
        } else isGrounded = false;
    }

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

    public void setWarningListener(WarningListener listener) {
        this.warningListener = listener;
    }
}
