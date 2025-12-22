package com.finpro7.oop.entities;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController.AnimationDesc;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController.AnimationListener;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.finpro7.oop.world.Terrain;

public class DajjalEntity extends BaseEnemy {

    // variabel khusus buat logika serangan ala dajjal (sesuai request)
    private float sudutKunci = 0f; // buat nyimpen arah muka pas lagi nonjok
    private boolean lagiNyerang = false; // flag biar tau lagi mode gebuk apa lari

    public DajjalEntity(Model model, float x, float y, float z) {
        super();
        this.manualTransform = true;
        this.modelInstance = new ModelInstance(model);
        this.modelInstance.transform.setToTranslation(x, y, z);
        this.animController = new AnimationController(this.modelInstance);
        this.position.set(x, y, z);

        // --- STATS SESUAI BASEENEMY TAPI DIOPREK DIKIT ---
        this.maxHealth = 2500f;
        this.health = this.maxHealth;

        // ini speed lari ngejar player (lajuLari di kode lama)
        this.runSpeed = 10.0f;

        // ini jarak mulai mukul (jarakSerang di kode lama)
        this.attackRange = 5.0f;
        this.damage = 40f;

        // nama animasi (sesuai request terakhir)
        this.ANIM_IDLE = "Armature|idle";
        this.ANIM_WALK = "Armature|walk";
        this.ANIM_RUN = "Armature|walk"; // lari pake animasi walk aja
        this.ANIM_ATTACK = "Armature|hit";

        // langsung masuk mode ngejar pas lahir
        // pake state khusus dajjal, bukan chase state biasa punya base enemy
        switchState(new DajjalChaseState(), null);
    }

    @Override
    public void update(float delta, Vector3 playerPos, Terrain terrain, Array<ModelInstance> trees, Array<BaseEnemy> allEnemies, PlayerStats playerStats) {
        // jalanin update bapaknya dulu biar animasi & darah keurus
        super.update(delta, playerPos, terrain, trees, allEnemies, playerStats);

        // --- OVERRIDE/TIMPA LOGIKA POSISI ---
        if (!isDead) {
            // Karena BaseEnemy otomatis muter badan ke player tiap frame,
            // Kita harus koreksi kalo Dajjal lagi nyerang (dia harus ngunci arah/sudutKunci)

            float yawYangDipake = currentYaw; // defaultnya ngikutin logic base enemy

            if (lagiNyerang) {
                // Kalo lagi nyerang, paksa madep ke sudut kunci, jangan tolah toleh ke player
                yawYangDipake = sudutKunci;
            }else {
                // Kalo gak nyerang, kita itung rotasi ke player manual di sini
                // (Persis kyak rotateTowardsPlayer tapi versi Dajjal)
                float dx = playerPos.x - position.x;
                float dz = playerPos.z - position.z;
                yawYangDipake = MathUtils.atan2(dx, dz) * MathUtils.radiansToDegrees;
                // Update currentYaw biar sinkron kalo perlu
                this.currentYaw = yawYangDipake;
            }

            modelInstance.transform.idt(); // reset
            modelInstance.transform.translate(position); // taroh di posisi update terbaru
            modelInstance.transform.rotate(Vector3.Y, yawYangDipake); // puter badan
            modelInstance.transform.scale(0.05f, 0.05f, 0.05f); // pastiin tetep raksasa
        }
    }

    @Override
    public void takeDamage(float amount, Terrain terrain) {
        // PENTING: Panggil super biar logic 'isDead', kurangin darah, sama 'setColor(Color.RED)' jalan!
        super.takeDamage(amount, terrain);

        // Logic tambahan khusus Dajjal kalo ditembak
        if(!isDead && health > 0){
            // Misal mau bikin dia langsung nengok ke player pas ditembak?
            // Bisa atur logic di sini, contoh:
            // lagiNyerang = false; // Batalin serangan kalo kesakitan? (Opsional)
        }
    }

    // --- STATE KHUSUS DAJJAL (ADAPTASI DARI KODE LAMA KAMU) ---

    // 1. Logic Ngejar (Simpel, lurus doang gak pake hindar pohon biar ganas)
    public class DajjalChaseState extends State {
        @Override
        public void enter(Terrain terrain) {
            lagiNyerang = false;
            // set animasi lari (walk)
            animController.setAnimation(ANIM_RUN, -1, 2.0f, null);
        }

        @Override
        public void update(float delta, Vector3 playerPos, Terrain terrain, Array<ModelInstance> trees, Array<BaseEnemy> activeEnemies, PlayerStats playerStats) {
            float dx = playerPos.x - position.x;
            float dz = playerPos.z - position.z;
            float jarakKePlayer = (float)Math.sqrt(dx*dx + dz*dz);

            // kalo udah deket banget, sikat!
            if (jarakKePlayer <= attackRange) {
                switchState(new DajjalAttackState(), terrain);
                return;
            }

            // Logic gerak lurus kyak kode lama (ModeMemburu bagian else)
            // Gak pake steering behavior ribet, pokoknya tabrak lurus
            float dirX = 0;
            float dirZ = 0;
            if (jarakKePlayer > 0) {
                dirX = dx / jarakKePlayer;
                dirZ = dz / jarakKePlayer;
            }

            // Maju jalan!
            // kita pake runSpeed (10.0f) sesuai request
            position.x += dirX * runSpeed * delta;
            position.z += dirZ * runSpeed * delta;
        }
    }

    // 2. Logic Nyerang (Ada maju dikit + Kunci Rotasi)
    public class DajjalAttackState extends State {
        float timerSerangan = 0f;
        float batasGerak = 2.0f; // durasi maju pas nonjok

        @Override
        public void enter(Terrain terrain) {
            lagiNyerang = true;
            timerSerangan = 0f;

            // itung sudut ke player SAAT INI, trus kunci.
            float dx = targetPos.x - position.x;
            float dz = targetPos.z - position.z;
            sudutKunci = MathUtils.atan2(dx, dz) * MathUtils.radiansToDegrees;

            // mainin animasi pukul sekali, kalo kelar balik ngejar
            animController.animate(ANIM_ATTACK, 1, 1.5f, new AnimationListener() {
                @Override
                public void onEnd(AnimationDesc animation) {
                    // pas animasi kelar, balik jadi anak baik (ngejar lagi)
                    switchState(new DajjalChaseState(), terrain);
                }
                @Override
                public void onLoop(AnimationDesc animation) {}
            }, 0.2f);
        }

        @Override
        public void update(float delta, Vector3 playerPos, Terrain terrain, Array<ModelInstance> trees, Array<BaseEnemy> activeEnemies, PlayerStats playerStats) {
            timerSerangan += delta;

            // Logika maju dikit pas mukul (biar kerasa impactnya)
            // sesuai request: lajuNyerang = 5.0f
            if (timerSerangan < batasGerak) {
                float lajuNyerang = 5.0f;
                // maju sesuai arah sudutKunci
                float forwardX = MathUtils.sinDeg(sudutKunci);
                float forwardZ = MathUtils.cosDeg(sudutKunci);

                position.x += forwardX * lajuNyerang * delta;
                position.z += forwardZ * lajuNyerang * delta;
            }

            // Logic deal damage sederhana
            // Kalo animasi udah jalan setengah (misal detik ke 0.5), cek kena gak
            if(timerSerangan > 0.5f && timerSerangan < 0.6f){
                float dist = position.dst(playerPos);
                if(dist <= attackRange + 1.0f){ // tambah dikit range toleransi
                    // Disini nanti logic kurangin darah player kalo ada
                    // System.out.println("DAJJAL HIT PLAYER!");
                    if(playerStats != null) {
                        // Dajjal damage sakit bos!
                        playerStats.takeDamage(damage);
                        System.out.println("DAJJAL GEBUK PLAYER!");
                    }
                }
            }
        }
    }
}
