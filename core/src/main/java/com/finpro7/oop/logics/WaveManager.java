package com.finpro7.oop.logics;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.finpro7.oop.entities.BaseEnemy;
import com.finpro7.oop.entities.EnemyFactory;
import com.finpro7.oop.world.Terrain;

public class WaveManager {

    private StageConfigs.BaseStage currentStage;
    private Vector3 spawnPos = new Vector3(); // variabel temp buat nyimpen posisi sementara biar hemat memori

    private int currentStageIndex = 0; // levelnya dari 1 sampe 6
    private int enemiesSpawnedInStage = 0; // udah berapa biji musuh yang nongol di stage ini
    private int enemiesKilledInStage = 0; // yang udah modar berapa
    private float timer = 0f;

    // penanda buat ngasih tau ui kalo stagenya barusan ganti
    public boolean justChangedStage = false;

    private float totalPlayerAngle = 0f; // akumulasi sudut total (bisa lebih dari 360 derajat)
    private float lastRawAngle = 0f; // sudut frame sebelumnya (-pi sampe pi)
    private boolean isFirstUpdate = true;
    private float maxTotalAngle; // total sudut jalan muter sampe puncak
    private float anglePerStage; // jatah sudut putaran per stage

    // ini dia variabel baru buat hud biar bisa nampilin totalnya
    private int totalEnemiesForCurrentStage = 0;

    // buat debug dimatiin dulu biar main jujur dari awal
    private boolean debugModeStage6 = false;

    public WaveManager() {
        currentStage = null; // awalnya belum masuk stage mana mana masih di basecamp bawah
    }

    public void initLevelData(Terrain terrain){
        // ngambil total putaran jalan dari terrain langsung
        this.maxTotalAngle = terrain.getMaxRoadAngle();
        this.anglePerStage = maxTotalAngle / 6.0f;
    }

    public void update(float delta, Vector3 playerPos, Terrain terrain, EnemyFactory factory, Array<BaseEnemy> activeEnemies) {
        // update sudut pemain pake sistem atan2
        updatePlayerAngle(playerPos.x, playerPos.z);

        // itung kita lagi di stage berapa berdasarkan progres nanjak
        int calculatedStage = MathUtils.ceil(((totalPlayerAngle / maxTotalAngle) * 6) + 0.1f);
        if(calculatedStage < 1) calculatedStage = 1;
        if(calculatedStage > 7) calculatedStage = 7;

        // kalo player nyolong start ke stage berikutnya padahal musuh belum abis, kita paksa tahan di stage lama (barrier logic)
        if(currentStageIndex > 0 && calculatedStage > currentStageIndex && !isStageCleared()) calculatedStage = currentStageIndex;

        // kalo deteksi stage beneran ganti (dan valid)
        if(calculatedStage > currentStageIndex){
            currentStageIndex = calculatedStage;
            enemiesSpawnedInStage = 0; // reset itungan spawn
            enemiesKilledInStage = 0; // reset kill counter juga biar mulai dari nol di stage baru
            timer = 0f; // reset timer
            justChangedStage = true; // kasih sinyal ke ui!

            // switch class strategi musuh sesuai stage, ini polymorphism
            switch(currentStageIndex){
                case 1: currentStage = new StageConfigs.StageOne(); break;
                case 2: currentStage = new StageConfigs.StageTwo(); break;
                case 3: currentStage = new StageConfigs.StageThree(); break;
                case 4: currentStage = new StageConfigs.StageFour(); break;
                case 5: currentStage = new StageConfigs.StageFive(); break;
                case 6: currentStage = new StageConfigs.StageSix(); break;
                case 7:
                    currentStage = null; // stop spawn kroco, ini panggungnya dajjal!
                    break;
            }
            // biar gamescreen bisa ambil angkanya buat hud
            if(currentStage != null){
                totalEnemiesForCurrentStage = currentStage.getTotalEnemies();
            }
        }

        // logika spawn musuhnya simpel: selama data stage ada dan kuota musuh belum abis, kita munculin terus
        if(currentStage != null && enemiesSpawnedInStage < currentStage.getTotalEnemies()){
            timer += delta;
            // cek interval waktu spawn, ngambil dari config class stage masing masing
            if(timer >= currentStage.getSpawnInterval()){
                // coba cari posisi spawn valid, spawn di depan player jarak 5-10 meter
                if(terrain.getSpawnPointAhead(playerPos, 5f, 10f, spawnPos)){
                    // spawn musuh pake factory
                    BaseEnemy enemy = factory.spawnEnemy(spawnPos.x, spawnPos.z, terrain, currentStage);
                    activeEnemies.add(enemy);
                    enemiesSpawnedInStage++; // tambahin counter spawn
                    timer = 0f; // reset timer
                }else timer = currentStage.getSpawnInterval() - 0.5f; // kalo gagal dapet posisi misal beda lantai, coba lagi frame depan tapi dicepetin
            }
        }
    }

    private void updatePlayerAngle(float x, float z){
        // itung sudut murni geometris (-pi sampe pi)
        float currentRawAngle = MathUtils.atan2(z, x);

        // inisialisasi awal pas game baru mulai
        if(isFirstUpdate){
            lastRawAngle = currentRawAngle;
            totalPlayerAngle = 0f; // start normal dari sudut nol (bawah)
            isFirstUpdate = false;
            return;
        }

        // itung selisih putaran dari frame kemaren
        float delta = currentRawAngle - lastRawAngle;
        // logic buat handle perbatasan 180 derajat (pi) biar gak bug muter balik
        if(delta < -MathUtils.PI){
            delta += MathUtils.PI2; // nambah 360 derajat
        }else if(delta > MathUtils.PI) delta -= MathUtils.PI2; // kurang 360 derajat
        // update total
        totalPlayerAngle += delta;
        // karena posisi start di terrain mungkin bukan di sudut 0 pas, kita clamp biar gak negatif di awal
        if(totalPlayerAngle < 0) totalPlayerAngle = 0;
        lastRawAngle = currentRawAngle;
    }

    // barrier atau tembok pake itungan sudut murni
    public float getAngleBarrier(){
        if(isStageCleared()) return 99999f; // kalo clear, barrier ilang (angka gede banget)
        float barrierAngle = currentStageIndex * anglePerStage;
        // toleransi dikit (buffer) sekitar 10 derajat (0.17 rad) biar player gak kejedot pas baru nyentuh garis
        return barrierAngle + 0.17f;
    }

    // getter buat gamescreen ngecek posisi sudut player
    public float getPlayerCurrentAngle(){
        return totalPlayerAngle;
    }

    // panggil method ini pas ada musuh yg koit
    public void reportEnemyDeath(){
        if(currentStage != null) {
            enemiesKilledInStage++;
        }
    }

    public int getCurrentStageNum(){
        return currentStageIndex;
    }

    public int getRemainingEnemies(){
        if (currentStage == null) return 0;
        // total kuota dikurang yang udah mati
        int left = currentStage.getTotalEnemies() - enemiesKilledInStage;
        return Math.max(0, left); // biar gak minus angkanya jaga jaga
    }

    // getter baru buat ui di gamescreen
    public int getTotalEnemiesInStage(){
        return totalEnemiesForCurrentStage;
    }

    // cek apakah stage ini udah bersih
    public boolean isStageCleared(){
        if(currentStage == null) return true; // kalo belum mulai, anggep clear
        return getRemainingEnemies() <= 0; // stage dianggap clear kalo sisa musuh 0 atau kurang
    }
}
