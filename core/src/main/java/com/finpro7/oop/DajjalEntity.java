package com.finpro7.oop;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController.AnimationDesc;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController.AnimationListener;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.finpro7.oop.world.Terrain;

// ini class khusus buat ngurusin si dajjal biar main gak kotor
public class DajjalEntity {

    public ModelInstance badanDajjal; // ini badan 3d nyaa,
    public AnimationController pengaturAnimasi;// buat ngatur gerak geriknya, ganti dari animationController

    // status mati idupnya
    private boolean isKoit = false; // ganti dead
    private float nyawa = 100f; // ganti health

    // vektor sementara buat itung itungan posisi biar hemat memori kyak yg diminta
    private final Vector3 posisiSaatIni = new Vector3();
    private final Vector3 posisiTarget = new Vector3();

    // ini otak buatan buat si dajjal (state machinenya)
    private TingkahLaku tingkahLakuSaatIni;

    // list nama animasinya biar gampang ganti ganti kalo salah
    private final String ANIM_JALAN = "Armature|running"; //walk"; // anim jalan
    private final String ANIM_PUKUL = "Armature|attack"; // hit"; // anim nyerangnya
    private final String ANIM_MATI = "Armature|dive"; // anim pas mati/diving
    private final String ANIM_MUNCUL = "Armature|emerge"; // anim pas nyihir

    public DajjalEntity(Model model, float x, float y, float z){
        // pasang model 3d nya
        badanDajjal = new ModelInstance(model);
        badanDajjal.transform.setToTranslation(x, y, z);
        // pasang controllernya
        pengaturAnimasi = new AnimationController(badanDajjal);
        // set awal kcepatan lari 10.0, jarak pukul 5.0 biar rada jauhan dikit
        gantiTingkahLaku(new ModeMemburu(10.0f, 5.0f));
    }

    // buat update logika tiap frame dipanggil di render main
    public void update(float delta, Vector3 targetPlayer, Terrain terrain){
        pengaturAnimasi.update(delta); // update frame animasinya dlu
        if (isKoit) return; // kalo mati ga usah mikir
        posisiTarget.set(targetPlayer); // update posisi target alias player
        if(tingkahLakuSaatIni != null) tingkahLakuSaatIni.jalankanLogika(delta, terrain); // jalanin isi otaknya
        // tempelin kaki ke tanah biar ga melayang
        badanDajjal.transform.getTranslation(posisiSaatIni);
        float tinggiTanah = terrain.getHeight(posisiSaatIni.x, posisiSaatIni.z);
        posisiSaatIni.y = tinggiTanah;
        badanDajjal.transform.setTranslation(posisiSaatIni); // update posisi asli modelnya
    }

    // buat ganti isi otaknya atau statenya
    public void gantiTingkahLaku(TingkahLaku perilakuBaru){
        tingkahLakuSaatIni = perilakuBaru;
        tingkahLakuSaatIni.mulai(); // buat inisialisasi perilaku baru
    }

    // core dasar otaknya
    abstract class TingkahLaku{
        public abstract void mulai(); // method pas pertama kali behavior ini aktif
        public abstract void jalankanLogika(float delta, Terrain terrain); // method yg jalan terus terusan
    }

    // ini logika kejar trus pukul
    class ModeMemburu extends TingkahLaku{

        float lajuLari;
        float jarakSerang;
        boolean lagiNyerang = false; // penanda lagi mukul apa ngga
        float timerSerangan = 0f;
        float batasGerak = 2.0f; // buat ngatur durasi gerak maju pas nonjok
        float sudutKunci = 0f; // variabel buat nyimpen arah bidikan pas mulai nonjok

        public ModeMemburu(float laju, float jarak){
            this.lajuLari = laju;
            this.jarakSerang = jarak;
        }

        @Override
        public void mulai(){
            // pas mulai langsung gass animasi jalan looping
            pengaturAnimasi.setAnimation(ANIM_JALAN, -1, 2.0f, null);
            lagiNyerang = false;
        }

        @Override
        public void jalankanLogika(float delta, Terrain terrain){
            badanDajjal.transform.getTranslation(posisiSaatIni);
            float dx = posisiTarget.x - posisiSaatIni.x;
            float dz = posisiTarget.z - posisiSaatIni.z;
            float jarakKePlayer = (float)Math.sqrt(dx*dx + dz*dz);
            if(lagiNyerang) timerSerangan += delta;
            float sudutYaw = 0f;
            // logika muter badan
            if(!lagiNyerang){
                sudutYaw = MathUtils.atan2(dx, dz) * MathUtils.radiansToDegrees; // kalo jalan biasa itung langsung ke arah player
            }else sudutYaw = sudutKunci; // kalo lagi nyerang gak ambil dari badan, tapi dari sudutKunci biar lurus terus pas mukul
            // bagian geraknya
            if(lagiNyerang){
                float forwardX = MathUtils.sinDeg(sudutYaw);
                float forwardZ = MathUtils.cosDeg(sudutYaw);
                if(timerSerangan < batasGerak){
                    float lajuNyerang = 5.0f;
                    posisiSaatIni.x += forwardX * lajuNyerang * delta;
                    posisiSaatIni.z += forwardZ * lajuNyerang * delta;
                }
            }else{
                // jalan biasa
                float dirX = 0;
                float dirZ = 0;
                if(jarakKePlayer > 0){
                    dirX = dx / jarakKePlayer;
                    dirZ = dz / jarakKePlayer;
                }
                if(jarakKePlayer > jarakSerang){
                    posisiSaatIni.x += dirX * lajuLari * delta;
                    posisiSaatIni.z += dirZ * lajuLari * delta;
                    if(pengaturAnimasi.current == null || !pengaturAnimasi.current.animation.id.equals(ANIM_JALAN)){
                        pengaturAnimasi.animate(ANIM_JALAN, -1, 2.0f, null, 0.4f);
                    }
                }else lakukanSerangan();
            }
            badanDajjal.transform.setToRotation(Vector3.Y, sudutYaw);
            badanDajjal.transform.setTranslation(posisiSaatIni);
            badanDajjal.transform.scale(0.05f, 0.05f, 0.05f);
        }

        private void lakukanSerangan(){
            lagiNyerang = true;
            timerSerangan = 0f;
            // biar auto aim, pas detik ini mau mukul, itung dlu sudut ke player skarang, trus simpen di sudutKunci, jadi mau badannya miring tetep maksa nengok player dlu baru nonjok
            float dx = posisiTarget.x - posisiSaatIni.x;
            float dz = posisiTarget.z - posisiSaatIni.z;
            sudutKunci = MathUtils.atan2(dx, dz) * MathUtils.radiansToDegrees;
            pengaturAnimasi.animate(ANIM_PUKUL, 1, 1.5f, new AnimationListener() {
                @Override
                public void onEnd(AnimationDesc animation){
                    lagiNyerang = false;
                    pengaturAnimasi.animate(ANIM_JALAN, -1, 1f, null, 0.2f);
                }
                @Override
                public void onLoop(AnimationDesc animation){}
            }, 0.2f);
        }
    }
}
