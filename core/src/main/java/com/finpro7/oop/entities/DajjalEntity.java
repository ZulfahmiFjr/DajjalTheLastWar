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

    // variabel buat nyimpen arah muka pas lagi mukul biar gak muter muter
    private float sudutKunci = 0f;
    private boolean lagiNyerang = false; // penanda lagi mode serangan atau lari

    public DajjalEntity(Model model, float x, float y, float z) {
        super();
        this.manualTransform = true; // kita atur posisi dajjal manual di sini
        this.modelInstance = new ModelInstance(model);
        this.modelInstance.transform.setToTranslation(x, y, z);
        this.animController = new AnimationController(this.modelInstance);
        this.position.set(x, y, z);

        // set darahnya tebel banget karena ini boss
        this.maxHealth = 2500f;
        this.health = this.maxHealth;

        // kecepatan lari ngejar player
        this.runSpeed = 10.0f;

        // jarak mulai mukul dan damagenya
        this.attackRange = 5.0f;
        this.damage = 40f;

        // atur nama animasi sesuai model
        this.ANIM_IDLE = "Armature|idle";
        this.ANIM_WALK = "Armature|walk";
        this.ANIM_RUN = "Armature|walk"; // lari pake animasi jalan aja
        this.ANIM_ATTACK = "Armature|hit";

        // pas lahir langsung masuk mode ngejar pake state khusus dajjal
        switchState(new DajjalChaseState(), null);
    }

    @Override
    public void update(float delta, Vector3 playerPos, Terrain terrain, Array<ModelInstance> trees, Array<BaseEnemy> allEnemies, PlayerStats playerStats) {
        // panggil update induknya buat ngurus animasi dan darah
        super.update(delta, playerPos, terrain, trees, allEnemies, playerStats);

        // logika posisi khusus dajjal
        if (!isDead) {
            // update ketinggian biar napak tanah
            position.y = terrain.getHeight(position.x, position.z);
            float yawYangDipake = currentYaw;

            if (lagiNyerang) {
                // kalo lagi nyerang arah mukanya dikunci jangan nengok nengok
                yawYangDipake = sudutKunci;
            }else {
                // kalo lagi ngejar itung rotasi ke player manual
                float dx = playerPos.x - position.x;
                float dz = playerPos.z - position.z;
                yawYangDipake = MathUtils.atan2(dx, dz) * MathUtils.radiansToDegrees;
                // update variabel yaw biar sinkron
                this.currentYaw = yawYangDipake;
            }

            modelInstance.transform.idt(); // reset transformasi
            modelInstance.transform.translate(position); // pindahin ke posisi baru
            modelInstance.transform.rotate(Vector3.Y, yawYangDipake); // puter badan
            modelInstance.transform.scale(0.05f, 0.05f, 0.05f); // pastiin ukurannya tetep raksasa
        }
    }

    @Override
    public void takeDamage(float amount, Terrain terrain) {
        // panggil super biar logika mati dan kedip merah tetep jalan
        super.takeDamage(amount, terrain);

        // bisa tambah logika khusus dajjal kalo kena damage di sini
        if(!isDead && health > 0){
            // contoh: batalin serangan kalo sakit banget
        }
    }

    // state khusus buat dajjal ngejar player
    public class DajjalChaseState extends State {
        @Override
        public void enter(Terrain terrain) {
            lagiNyerang = false;
            // set animasi lari
            animController.setAnimation(ANIM_RUN, -1, 2.0f, null);
        }

        @Override
        public void update(float delta, Vector3 playerPos, Terrain terrain, Array<ModelInstance> trees, Array<BaseEnemy> activeEnemies, PlayerStats playerStats) {
            float dx = playerPos.x - position.x;
            float dz = playerPos.z - position.z;
            float jarakKePlayer = (float)Math.sqrt(dx*dx + dz*dz);

            // kalo udah deket langsung hajar
            if (jarakKePlayer <= attackRange) {
                switchState(new DajjalAttackState(), terrain);
                return;
            }

            // dajjal geraknya lurus aja gak usah hindar pohon biar ganas
            float dirX = 0;
            float dirZ = 0;
            if (jarakKePlayer > 0) {
                dirX = dx / jarakKePlayer;
                dirZ = dz / jarakKePlayer;
            }

            // maju jalan ngejar player
            position.x += dirX * runSpeed * delta;
            position.z += dirZ * runSpeed * delta;
        }
    }

    // state khusus buat dajjal nyerang
    public class DajjalAttackState extends State {
        float timerSerangan = 0f;
        float batasGerak = 2.0f; // durasi maju dikit pas nonjok

        @Override
        public void enter(Terrain terrain) {
            lagiNyerang = true;
            timerSerangan = 0f;

            // itung arah ke player saat ini terus kunci
            float dx = targetPos.x - position.x;
            float dz = targetPos.z - position.z;
            sudutKunci = MathUtils.atan2(dx, dz) * MathUtils.radiansToDegrees;

            // mainin animasi pukul sekali kalo kelar balik ngejar
            animController.animate(ANIM_ATTACK, 1, 1.5f, new AnimationListener() {
                @Override
                public void onEnd(AnimationDesc animation) {
                    // animasi kelar balik jadi ngejar lagi
                    switchState(new DajjalChaseState(), terrain);
                }
                @Override
                public void onLoop(AnimationDesc animation) {}
            }, 0.2f);
        }

        @Override
        public void update(float delta, Vector3 playerPos, Terrain terrain, Array<ModelInstance> trees, Array<BaseEnemy> activeEnemies, PlayerStats playerStats) {
            timerSerangan += delta;

            // dajjal maju dikit pas mukul biar kerasa impactnya
            if (timerSerangan < batasGerak) {
                float lajuNyerang = 5.0f;
                // maju sesuai arah yang udah dikunci
                float forwardX = MathUtils.sinDeg(sudutKunci);
                float forwardZ = MathUtils.cosDeg(sudutKunci);

                position.x += forwardX * lajuNyerang * delta;
                position.z += forwardZ * lajuNyerang * delta;
            }

            // logika kena damage pas animasi pukulan lagi jalan
            if(timerSerangan > 0.5f && timerSerangan < 0.6f){
                float dist = position.dst(playerPos);
                // kasih toleransi jarak dikit
                if(dist <= attackRange + 1.0f){
                    if(playerStats != null) {
                        // kurangi darah player sakit banget nih
                        playerStats.takeDamage(damage);
                        System.out.println("DAJJAL GEBUK PLAYER!");
                    }
                }
            }
        }
    }
}
