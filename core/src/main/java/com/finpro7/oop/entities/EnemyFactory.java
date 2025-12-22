package com.finpro7.oop.entities;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.MathUtils;
import com.finpro7.oop.logics.StageConfigs;
import com.finpro7.oop.world.Terrain;

public class EnemyFactory {

    private Model yajujModel;
    private Model majujModel;
    private Model dajjalModel;

    public EnemyFactory(Model yajujModel, Model majujModel, Model dajjalModel) {
        this.yajujModel = yajujModel;
        this.majujModel = majujModel;
        this.dajjalModel = dajjalModel;
    }

    // method ini butuh objek basestage buat ngintip statsnya
    public BaseEnemy spawnEnemy(float x, float z, Terrain terrain, StageConfigs.BaseStage stageData){
        // ngocok dadu dulu buat nentuin spawn yajuj atau majuj
        boolean isYajuj = MathUtils.randomBoolean();
        BaseEnemy enemy;
        if(isYajuj){
            enemy = new Yajuj(yajujModel);
        }else enemy = new Majuj(majujModel);
        // ini angka dasar atau base statsnya sebelum diapa apain
        float baseHp = 40f;
        float baseWalk = 2.5f;
        float baseRun  = 5.0f;
        float baseRange = 3.5f;
        float baseDmg = 10f;
        // nahh di sini baru dikaliin sama multiplier dari stage data
        enemy.maxHealth = baseHp * stageData.getHpMultiplier();
        enemy.health = enemy.maxHealth;
        enemy.walkSpeed = baseWalk * stageData.getSpeedMultiplier();
        enemy.runSpeed = baseRun * stageData.getSpeedMultiplier();
        enemy.damage = baseDmg * stageData.getDamageMultiplier();
        enemy.attackRange = baseRange * ((stageData.getSpeedMultiplier() + 1f) / 2f); // jangkauan serangannya dibikin makin jauh dikit kalo stagenya makin tinggi
        // nentuin posisi y ambil dari tinggi terrain biar napak
        float y = terrain.getHeight(x, z);
        enemy.position.set(x, y, z);
        // set animasi awalnya pas muncul kyak keluar dari tanah gitu
        enemy.switchState(enemy.new EmergeState(), terrain);
        return enemy;
    }

    // method khusus buat spawn boss dajjal
    public BaseEnemy spawnDajjal(float x, float z, Terrain terrain){
        float y = terrain.getHeight(x, z);
        DajjalEntity boss = new DajjalEntity(dajjalModel, x, y, z);
        // dajjal langsung mode ngejar aja, gak usah emerge biar langsung gelut
//        boss.switchState(boss.new ChaseState(), terrain);
        boss.switchState(boss.new DajjalChaseState(), terrain);
        return boss;
    }
}
