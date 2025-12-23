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

    // variabel bantu buat ngitung tabrakan
    private final Vector3 collisionNormal = new Vector3();
    private final Vector3 slideDirection = new Vector3();

    // penanda buat ngasih tau game screen kalo musuh ini udah boleh dihapus
    public boolean isReadyToRemove = false;

    // timer buat nunggu animasi mati selesai sebelum dihapus
    private float deathTimer = 0f;

    // status dasar musuh kayak darah
    public float health;
    public float maxHealth;

    // urusan kecepatan gerak
    public float walkSpeed; // kecepatan jalan santai
    public float runSpeed; // kecepatan lari ngebut
    protected boolean manualTransform = false;

    // jarak pukul dan karusakannya
    public float attackRange;
    public float damage;

    public boolean isDead = false;
    public boolean isRising = false;
    public boolean countedAsDead = false; // penanda biar skor gak keitung dua kali

    // efek visual pas kena pukul
    private float hitFlashTimer = 0f; // timer buat efek kedip merah
    private final Color originalColor = new Color(1,1,1,1); // warna normal
    private final Color hitColor = new Color(1, 0, 0, 1);   // warna merah pas sakit

    private final float BODY_SCALE = 0.022f;
    protected State currentState;
    protected float currentYaw = 0f;

    // variabel sementara buat itung itungan vektor biar hemat memori
    private final Vector3 tmpSep = new Vector3();

    // nama nama animasi bawaan dari model blender
    protected String ANIM_IDLE = "Armature|idle";
    protected String ANIM_WALK = "Armature|walking";
    protected String ANIM_RUN = "Armature|running";
    protected String ANIM_ATTACK = "Armature|attack";

    // batesan jarak buat nentuin kapan dia mulai lari
    private final float RUN_DISTANCE_THRESHOLD = 5.0f;

    public BaseEnemy() { }

    public void update(float delta, Vector3 playerPos, Terrain terrain, Array<ModelInstance> trees, Array<BaseEnemy> allEnemies, PlayerStats playerStats) {
        if(isReadyToRemove) return;
        targetPos.set(playerPos);
        if(animController != null) animController.update(delta);

        // update logic state musuh sekarang lagi ngapain
        if(currentState != null) currentState.update(delta, playerPos, terrain, trees, allEnemies, playerStats);

        // kalo musuh masih hidup dan gak diatur manual posisinya sama dajjal
        // base enemy yang ngurus posisi dan rotasinya
        if (!isDead && !manualTransform) {
            if(!isRising) position.y = terrain.getHeight(position.x, position.z);
            modelInstance.transform.setToTranslation(position);
            rotateTowardsPlayer();
            modelInstance.transform.scale(BODY_SCALE, BODY_SCALE, BODY_SCALE);
        }

        // logika buat balikin warna jadi normal abis kedip merah
        if (hitFlashTimer > 0) {
            hitFlashTimer -= delta;
            if (hitFlashTimer <= 0) setColor(Color.WHITE);
        }
    }

    // method pas musuh kena damage
    public void takeDamage(float amount, Terrain terrain){
        if (isDead || isRising) return;
        health -= amount;

        // ubah warna jadi merah sebentar
        setColor(Color.RED);
        hitFlashTimer = 0.1f; // durasi kedip merah 0.1 detik

        // cek kalo darah abis berarti mati
        if(health <= 0){
            health = 0;
            isDead = true;
            setColor(Color.WHITE); // balikin warna normal
            switchState(new DeathState(), terrain); // masuk ke animasi mati
        }
    }

    // method bantu buat ganti warna model musuh
    private void setColor(Color color){
        // loop semua material di model terus ganti warna dasarnya
        for(Material mat : modelInstance.materials){
            ColorAttribute attrib = (ColorAttribute) mat.get(ColorAttribute.Diffuse);
            if(attrib != null) attrib.color.set(color);
        }
    }

    private void die(){
        isDead = true;
        setColor(Color.WHITE);
        // cara kasar ngilangin mayat dilempar ke bawah tanah
        position.y -= 1000f;
    }

    private void rotateTowardsPlayer(){
        float dx = targetPos.x - position.x;
        float dz = targetPos.z - position.z;

        // pake atan2 biar dapet sudut putar yang akurat 360 derajat
        float angleYaw = MathUtils.atan2(dx, dz) * MathUtils.radiansToDegrees;

        // simpen sudutnya dan langsung puter modelnya
        this.currentYaw = angleYaw;
        modelInstance.transform.rotate(Vector3.Y, angleYaw);
    }

    public void switchState(State newState, Terrain terrain){
        this.currentState = newState;
        newState.enter(terrain);
    }

    // struktur dasar state machine buat ngatur perilaku musuh
    public abstract class State {
        public abstract void enter(Terrain terrain);
        public abstract void update(float delta, Vector3 playerPos, Terrain terrain, Array<ModelInstance> trees, Array<BaseEnemy> activeEnemies, PlayerStats playerStats);
    }

    // state pas musuh muncul pelan pelan dari dalem tanah
    public class EmergeState extends State {
        float finalY;
        float riseSpeed = 1.5f;

        @Override
        public void enter(Terrain terrain){
            isRising = true;
            // pas lagi naik animasinya diem dulu
            animController.setAnimation(ANIM_IDLE, -1, 1f, null);
            finalY = terrain.getHeight(position.x, position.z);
            position.y = finalY - 2.5f; // mulai dari kedalaman 2.5 meter
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

    // state pas musuh ngejar player entah jalan atau lari
    public class ChaseState extends State {

        // variabel buat nyatet animasi apa yang lagi jalan biar gak diset ulang terus
        private String currentAnim = "";

        @Override
        public void enter(Terrain terrain){
            // reset pencatat animasi pas baru masuk state ini
            currentAnim = "";
        }

        @Override
        public void update(float delta, Vector3 playerPos, Terrain terrain, Array<ModelInstance> trees, Array<BaseEnemy> activeEnemies, PlayerStats playerStats) {
            float dist = position.dst(playerPos);

            // cek kalo jarak udah cukup deket langsung serang
            if(dist < attackRange){
                switchState(new AttackState(), terrain);
                return;
            }

            // logika milih mau jalan santai atau lari ngebut
            float currentSpeed;
            String targetAnim;
            if(dist > RUN_DISTANCE_THRESHOLD){
                // kalo jauh kita lari
                currentSpeed = runSpeed;
                targetAnim = ANIM_RUN;
            }else{
                // kalo deket jalan biasa aja
                currentSpeed = walkSpeed;
                targetAnim = ANIM_WALK;
            }

            // ganti animasi cuma kalau beda sama yang sekarang biar hemat performa
            if(!currentAnim.equals(targetAnim)){
                animController.animate(targetAnim, -1, 1f, null, 0.2f); // pake blending dikit biar alus
                currentAnim = targetAnim;
            }

            // hitung arah gerak menuju player
            moveDirection.set(playerPos).sub(position).nor();

            // hitung gaya tolak menolak biar gak numpuk sama temen
            calculateSeparation(activeEnemies);

            // gabungin arah ke player dan arah ngejauh dari temen
            moveDirection.add(separationForce.scl(1.5f));

            // normalisasi lagi biar kecepatannya stabil
            moveDirection.nor();

            // terapin pergerakannya
            float moveX = moveDirection.x * currentSpeed * delta;
            float moveZ = moveDirection.z * currentSpeed * delta;

            // logika deteksi tabrakan pohon
            float nextX = position.x + moveX;
            float nextZ = position.z + moveZ;
            boolean nabrak = false;
            float radiusEnemy = 0.5f;
            float radiusPohon = 0.8f;
            float jarakAmanKuadrat = (radiusEnemy + radiusPohon) * (radiusEnemy + radiusPohon);

            for(ModelInstance tree : trees){
                tmpTreePos.set(0,0,0); // reset vektor bantuan
                tree.transform.getTranslation(tmpTreePos);
                float dx = nextX - tmpTreePos.x;
                float dz = nextZ - tmpTreePos.z;

                if(dx*dx + dz*dz < jarakAmanKuadrat){
                    nabrak = true;
                    // hitung arah pantulan biar musuh geser ke samping pohon
                    collisionNormal.set(position).sub(tmpTreePos);
                    collisionNormal.y = 0; // abaikan tinggi
                    collisionNormal.nor();

                    // bikin vektor geser sejajar permukaan pohon
                    slideDirection.set(collisionNormal).rotateRad(Vector3.Y, MathUtils.HALF_PI);

                    // kalo arah gesernya malah ngejauh dari tujuan kita balik arahnya
                    if(slideDirection.dot(moveDirection) < 0) slideDirection.scl(-1);
                    break;
                }
            }

            if(!nabrak){
                position.x += moveX;
                position.z += moveZ;
            }else{
                // kalo nabrak pohon kita geser nyamping
                position.x += slideDirection.x * currentSpeed * delta;
                position.z += slideDirection.z * currentSpeed * delta;
            }
        }

        // method buat ngitung biar musuh gak dempet dempetan
        private void calculateSeparation(Array<BaseEnemy> neighbors){
            separationForce.set(0, 0, 0); // reset gaya
            int count = 0;
            float separationRadius = 1.2f; // jarak aman antar musuh

            for(BaseEnemy other : neighbors){
                // jangan ngitung jarak ke diri sendiri
                if(other == BaseEnemy.this) continue;

                // hitung jarak ke musuh lain
                float d = position.dst(other.position);

                // kalo terlalu deket kita bikin gaya tolak
                if(d < separationRadius && d > 0){
                    // pake vektor bantuan biar hemat memori
                    tmpSep.set(position);
                    tmpSep.sub(other.position);
                    tmpSep.nor(); // jadiin arah doang
                    tmpSep.scl(1.0f / d); // makin deket tolakannya makin kuat

                    // kumpulin semua gaya tolaknya
                    separationForce.add(tmpSep);
                    count++;
                }
            }
            // ambil rata rata gaya tolaknya
            if(count > 0) separationForce.scl(1.0f / count);
        }
    }

    // state pas musuh lagi nyerang player
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

            // cek damage di tengah tengah animasi serangan
            if(timer > 0.4f && !hasDealtDamage){
                float dist = position.dst(playerPos);

                // pastiin player masih dalam jangkauan
                if(dist <= attackRange + 0.5f){
                    // kurangi darah player
                    if(playerStats != null) {
                        playerStats.takeDamage(damage);
                        System.out.println("Player kena pukul! Damage: " + damage);
                    }
                    hasDealtDamage = true;
                }
            }
            // kalo animasi kelar balik ngejar lagi
            if(timer > 1.2f) switchState(new ChaseState(), terrain);
        }
    }

    // state pas musuh mati
    public class DeathState extends State {
        // variabel buat nyimpen rotasi biar mayatnya gak muter aneh
        private final com.badlogic.gdx.math.Quaternion savedRotation = new com.badlogic.gdx.math.Quaternion();
        private float savedYaw = 0f;
        private float currentPitch = 0f; // sudut kemiringan rebahan

        @Override
        public void enter(Terrain terrain){
            // ganti animasi jadi diem aja pas mati
            if(animController != null) animController.setAnimation(ANIM_IDLE, -1, 1f, null);

            this.savedYaw = BaseEnemy.this.currentYaw;
        }

        @Override
        public void update(float delta, Vector3 playerPos, Terrain terrain, Array<ModelInstance> trees, Array<BaseEnemy> activeEnemies, PlayerStats playerStats) {
            deathTimer += delta;

            // fase satu rebahan ke belakang selama satu detik
            if (deathTimer < 1.0f) {
                // nambah sudut rebahan pelan pelan pake interpolasi
                currentPitch = MathUtils.lerp(currentPitch, -90f, delta * 5f);
            }
            // fase dua mayat tenggelam ke tanah
            else {
                currentPitch = -90f; // kunci posisi rebahan
                position.y -= 1.5f * delta; // pelan pelan turun
            }

            // atur posisi dan rotasi mayat secara manual biar pas
            modelInstance.transform.idt();

            // pindahin ke lokasi terakhir
            modelInstance.transform.translate(position);

            // hadapkan ke arah terakhir dia liat
            modelInstance.transform.rotate(Vector3.Y, savedYaw);

            // baringkan badan ke belakang
            modelInstance.transform.rotate(Vector3.X, currentPitch);

            // balikin ukurannya biar gak berubah
            modelInstance.transform.scale(BODY_SCALE, BODY_SCALE, BODY_SCALE);

            // fase tiga hapus dari game setelah tiga detik
            if (deathTimer > 3.0f) {
                isReadyToRemove = true;
            }
        }
    }
}
