package com.finpro7.oop.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.finpro7.oop.world.Terrain;

public abstract class BaseEnemy {

    public ModelInstance modelInstance;
    public AnimationController animController;

    public final Vector3 position = new Vector3();
    public final Vector3 targetPos = new Vector3();
    private final Vector3 tmpTreePos = new Vector3();
    private final Vector3 separationForce = new Vector3();
    private final Vector3 moveDirection = new Vector3();
    // var bantu buat collision
    private final Vector3 collisionNormal = new Vector3();
    private final Vector3 slideDirection = new Vector3();
    public boolean isReadyToRemove = false; // Bendera buat ngasih tau GameScreen "Hapus gue dong"
    private float deathTimer = 0f; // Timer buat animasi jatuh

    // stats dasar musuhnya
    public float health;
    public float maxHealth;

    // urusan kecepatan
    public float walkSpeed; // pas lagi jalan santai
    public float runSpeed; // pas lagi lari ngebut

    // jarak pukul yang bisa berubah ubah
    public float attackRange;
    public float damage;

    public boolean isDead = false;
    public boolean isRising = false;
    public boolean countedAsDead = false; // penanda biar kill count gak keitung dobel

    // visual hit
    private float hitFlashTimer = 0f; //timer buat efek kedip
    private final Color originalColor = new Color(1,1,1,1); // warna asli
    private final Color hitColor = new Color(1, 0, 0, 1);   // warna merah

    private final float BODY_SCALE = 0.022f;
    protected State currentState;
    protected float currentYaw = 0f;

    // nama animasi bawaan dari blender
    protected String ANIM_IDLE = "Armature|idle";
    protected String ANIM_WALK = "Armature|walking";
    protected String ANIM_RUN = "Armature|running";
    protected String ANIM_ATTACK = "Armature|attack";

    // batesan jarak buat nentuin kapan dia harus lari
    private final float RUN_DISTANCE_THRESHOLD = 5.0f;

    public BaseEnemy() { }

    public void update(float delta, Vector3 playerPos, Terrain terrain, Array<ModelInstance> trees, Array<BaseEnemy> allEnemies, PlayerStats playerStats) {
        if(isReadyToRemove) return;
        targetPos.set(playerPos);
        if(animController != null) animController.update(delta);
        // Update State
        if(currentState != null) currentState.update(delta, playerPos, terrain, trees, allEnemies, playerStats);
        // kalo mati kita serahin urusan posisi/rotasi ke DeathState sepenuhnya.
        // BaseEnemy cuma ngurus transform kalo dia masih HIDUP.
        if (!isDead) {
            if(!isRising) position.y = terrain.getHeight(position.x, position.z);
            modelInstance.transform.setToTranslation(position);
            rotateTowardsPlayer();
            modelInstance.transform.scale(BODY_SCALE, BODY_SCALE, BODY_SCALE);
        }
        // Logika kedip merah
        if (hitFlashTimer > 0) {
            hitFlashTimer -= delta;
            if (hitFlashTimer <= 0) setColor(Color.WHITE);
        }
    }

    // kena damage
    public void takeDamage(float amount, Terrain terrain){
        if (isDead || isRising) return;
        health -= amount;
        // set warna jadi merah
        setColor(Color.RED);
        hitFlashTimer = 0.1f; // kedip merah selama 0.1 detik
        // Cek Mati
        if(health <= 0){
            health = 0;
            isDead = true;
            setColor(Color.WHITE); // Reset warna merah
            switchState(new DeathState(), terrain); // Masuk animasi mati
        }
    }

    // method helper buat ganti warna material model
    private void setColor(Color color){
        // ubah warna Diffuse (warna dasar) semua material di model ini
        for(Material mat : modelInstance.materials){
            ColorAttribute attrib = (ColorAttribute) mat.get(ColorAttribute.Diffuse);
            if(attrib != null) attrib.color.set(color);
        }
    }

    private void die(){
        isDead = true;
        setColor(Color.WHITE);
        // animController.animate("death", 1, 1f, null, 0.2f);
        // kalo gak ada animasi mati lempar dia ke bawah tanah atau ilangin
        position.y -= 1000f; // cara kasar ngilangin mayat :v
    }

    private void rotateTowardsPlayer(){
        float dx = targetPos.x - position.x;
        float dz = targetPos.z - position.z;
        // pake atan2 biar dapet sudut yang bener 360 derajat
        float angleYaw = MathUtils.atan2(dx, dz) * MathUtils.radiansToDegrees;
        // tambahin interpolasi dikit biar muternya alus gak patah patah banget opsional, tapi langsung set juga biar responsif
        this.currentYaw = angleYaw;
        modelInstance.transform.rotate(Vector3.Y, angleYaw);
    }

    public void switchState(State newState, Terrain terrain){
        this.currentState = newState;
        newState.enter(terrain);
    }

    // bagain state machinenyaa
    public abstract class State {
        public abstract void enter(Terrain terrain);
        public abstract void update(float delta, Vector3 playerPos, Terrain terrain, Array<ModelInstance> trees, Array<BaseEnemy> activeEnemies, PlayerStats playerStats);
    }

    // state emerge pas muncul dari dalem tanah
    public class EmergeState extends State {
        float finalY;
        float riseSpeed = 1.5f;

        @Override
        public void enter(Terrain terrain){
            isRising = true;
            // animasi diem dulu pas lagi naik
            animController.setAnimation(ANIM_IDLE, -1, 1f, null);
            finalY = terrain.getHeight(position.x, position.z);
            position.y = finalY - 2.5f; // mulai dari bawah tanah sedalam 2.5 meter
        }

        @Override
        public void update(float delta, Vector3 playerPos, Terrain terrain, Array<ModelInstance> trees, Array<BaseEnemy> activeEnemies, PlayerStats playerStats) {
            position.y += riseSpeed * delta;
            if(position.y >= finalY){
                position.y = finalY;
                isRising = false;
                switchState(new ChaseState(), terrain);
            }
        }
    }

    // state chase pas lagi ngejar jalan vs lari
    public class ChaseState extends State {

        // variabel buat track animasi yg lagi aktif biar gak set animasi terus terusan
        private String currentAnim = "";

        @Override
        public void enter(Terrain terrain){
            // awal masuk state paksa update logic di frame pertama
            currentAnim = "";
        }

        @Override
        public void update(float delta, Vector3 playerPos, Terrain terrain, Array<ModelInstance> trees, Array<BaseEnemy> activeEnemies, PlayerStats playerStats) {
            float dist = position.dst(playerPos);
            // cek jarak buat nyerang pake variabel attackRange dinamis
            if(dist < attackRange){
                switchState(new AttackState(), terrain);
                return;
            }
            // logic buat nentuin jalan atau lari
            float currentSpeed;
            String targetAnim;
            if(dist > RUN_DISTANCE_THRESHOLD){
                // kalo jauh lari
                currentSpeed = runSpeed;
                targetAnim = ANIM_RUN;
            }else{
                // kalo deket jalan santai aja
                currentSpeed = walkSpeed;
                targetAnim = ANIM_WALK;
            }
            // cuma ganti animasi kalo beda sama yg sekarang biar hemat performa
            if(!currentAnim.equals(targetAnim)){
                animController.animate(targetAnim, -1, 1f, null, 0.2f); // 0.2f blending biar transisi alus
                currentAnim = targetAnim;
            }
            // hitung vektor gerak pake steering behavior, vektor nuju ke arah player
            moveDirection.set(playerPos).sub(position).nor();
            // vektor separation buat ngindar dari temennya
            calculateSeparation(activeEnemies);
            // gabungin arah player tambah dorongan jauh dari temen kali bobot, bobot 1.5f itu dorongan ngejauh lebih prioritas dikit biar gak nempel banget
            moveDirection.add(separationForce.scl(1.5f));
            // normalisasi lagi biar kecepatannya konsisten gak jadi super cepet
            moveDirection.nor();
            // terapkan gerakannya
            float moveX = moveDirection.x * currentSpeed * delta;
            float moveZ = moveDirection.z * currentSpeed * delta;
            // logika tabrak pohon tetep sama persis kyak sebelumnya
            float nextX = position.x + moveX;
            float nextZ = position.z + moveZ;
            boolean nabrak = false;
            float radiusEnemy = 0.5f;
            float radiusPohon = 0.8f;
            float jarakAmanKuadrat = (radiusEnemy + radiusPohon) * (radiusEnemy + radiusPohon);
            for(ModelInstance tree : trees){
                tmpTreePos.set(0,0,0); // reset dulu biar aman
                tree.transform.getTranslation(tmpTreePos);
                float dx = nextX - tmpTreePos.x;
                float dz = nextZ - tmpTreePos.z;
                if(dx*dx + dz*dz < jarakAmanKuadrat){
                    nabrak = true;
                    // ambil garis normal, arah tegak lurus dari pohon ke musuh
                    collisionNormal.set(position).sub(tmpTreePos);
                    collisionNormal.y = 0; // kita main di XZ plane aja
                    collisionNormal.nor(); // normalisasi biar panjangnya 1
                    // ini bikin vektornya jadi sejajar sama permukaan pohon
                    slideDirection.set(collisionNormal).rotateRad(Vector3.Y, MathUtils.HALF_PI);
                    if(slideDirection.dot(moveDirection) < 0) slideDirection.scl(-1); // kalo berlawanan kita balik arah slidenya biar tetep maju mendekati target
                    break;
                }
            }
            if(!nabrak){
                position.x += moveX;
                position.z += moveZ;
            }else{
                // kalo nabrak pake vektor slide
                position.x += slideDirection.x * currentSpeed * delta;
                position.z += slideDirection.z * currentSpeed * delta;
            }
        }

        // method buat ngitung gaya tolak menolak
        private void calculateSeparation(Array<BaseEnemy> neighbors){
            separationForce.set(0, 0, 0); // reset
            int count = 0;
            float separationRadius = 1.2f; // jarak personal space mereka jangan terlalu gede
            for(BaseEnemy other : neighbors){
                // jangan cek diri sendiri
                if(other == BaseEnemy.this) continue;
                // cek jarak ke musuh lain
                float d = position.dst(other.position);
                // kalau terlalu deket kurang dari radius personal
                if(d < separationRadius && d > 0){
                    // hitung vektor menjauh posisi kita dikurang posisi dia
                    Vector3 push = new Vector3(position).sub(other.position);
                    push.nor(); // normalisasi biar jadi arah doang
                    push.scl(1.0f / d); // semakin deket dorongannya semakin kuat inverse distance
                    separationForce.add(push);
                    count++;
                }
            }
            // rata rata gaya dorongnya
            if(count > 0) separationForce.scl(1.0f / count);
        }
    }

    // state attack pas lagi nyerang
    public class AttackState extends State {
        boolean hasDealtDamage = false;
        float timer = 0f;

        @Override
        public void enter(Terrain terrain){
            hasDealtDamage = false;
            timer = 0f;
            animController.animate(ANIM_ATTACK, 1, 1.2f, null, 0.1f);
        }

        @Override
        public void update(float delta, Vector3 playerPos, Terrain terrain, Array<ModelInstance> trees, Array<BaseEnemy> activeEnemies, PlayerStats playerStats) {
            timer += delta;
            // logika damage sederhana
            if(timer > 0.4f && !hasDealtDamage){
                float dist = position.dst(playerPos);
                // cek lagi jaraknya pas pukul biar kalo player kabur gak kena
                if(dist <= attackRange + 0.5f){
                    // kurangi darah player disini
                    if(playerStats != null) {
                        playerStats.takeDamage(damage);
                        System.out.println("Player kena pukul! Damage: " + damage);
                    }
                    hasDealtDamage = true;
                }
            }
            if(timer > 1.2f) switchState(new ChaseState(), terrain); // durasi animasi kelar
        }
    }

    // Import Quaternion jangan lupa (biasanya udah auto import, kalo belum tambahin ini di atas: import com.badlogic.gdx.math.Quaternion;)
    public class DeathState extends State {
        // Kita pake Quaternion biar nyimpen rotasinya AKURAT 100%
        private final com.badlogic.gdx.math.Quaternion savedRotation = new com.badlogic.gdx.math.Quaternion();
        private float savedYaw = 0f;
        private float currentPitch = 0f; // Sudut rebahan

        @Override
        public void enter(Terrain terrain){
            // 1. Matikan animasi jalan/lari, ganti idle atau pose mati
            if(animController != null) animController.setAnimation(ANIM_IDLE, -1, 1f, null);

            this.savedYaw = BaseEnemy.this.currentYaw;
            // Kita ambil rotasi persis saat dia mati, simpen di savedRotation
//            modelInstance.transform.getRotation(savedRotation);
        }

        @Override
        public void update(float delta, Vector3 playerPos, Terrain terrain, Array<ModelInstance> trees, Array<BaseEnemy> activeEnemies, PlayerStats playerStats) {
            deathTimer += delta;

            // FASE 1: REBAHAN (0 detik - 1 detik)
            if (deathTimer < 1.0f) {
                // Nambah sudut rebahan pelan-pelan sampe -90 derajat
                // Interpolasi biar jatuhnya halus (makin lama makin cepet dikit)
                currentPitch = MathUtils.lerp(currentPitch, -90f, delta * 5f);
            }
            // FASE 2: TENGGELAM (Setelah 1 detik)
            else {
                currentPitch = -90f; // Pastikan udah rebahan total
                position.y -= 1.5f * delta; // Turun ke bawah tanah
            }

            // --- PENERAPAN TRANSFORMASI MANUAL ---
            // Karena di update() utama kita skip transform pas mati, kita harus susun manual di sini.
            // Urutannya PENTING: Posisi -> Rotasi Asli -> Rotasi Rebahan -> Scale
            modelInstance.transform.idt();
            // 2. Pindahin ke lokasi mayat (X, Y yg tenggelam, Z)
            modelInstance.transform.translate(position);
            // 3. Putar Y (Hadap ke arah player terakhir) - INI PENTING DULUAN
            modelInstance.transform.rotate(Vector3.Y, savedYaw);
            // 4. Putar X (Rebahan ke belakang)
            // Karena Y udah diputar, X sekarang jadi "X Lokal" (samping kanan kiri musuh)
            // Jadi dia bakal rebahan ke belakang punggungnya sendiri
            modelInstance.transform.rotate(Vector3.X, currentPitch);
            // 5. Kembalikan ukuran
            modelInstance.transform.scale(BODY_SCALE, BODY_SCALE, BODY_SCALE); // 4. Balikin ukurannya

            // FASE 3: HAPUS (Setelah 3 detik)
            if (deathTimer > 3.0f) {
                isReadyToRemove = true;
            }
        }
    }
}
