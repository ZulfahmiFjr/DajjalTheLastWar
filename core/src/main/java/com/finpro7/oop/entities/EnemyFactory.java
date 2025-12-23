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

    // method ini butuh data stage buat ngatur kekuatan musuh
    public BaseEnemy spawnEnemy(float x, float z, Terrain terrain, StageConfigs.BaseStage stageData){
        // acak mau spawn yajuj atau majuj
        boolean isYajuj = MathUtils.randomBoolean();
        BaseEnemy enemy;
        if(isYajuj){
            enemy = new Yajuj(yajujModel);
        }else enemy = new Majuj(majujModel);

        // stats dasar musuh sebelum dikali level
        float baseHp = 40f;
        float baseWalk = 2.5f;
        float baseRun  = 5.0f;
        float baseRange = 3.5f;
        float baseDmg = 10f;

        // kaliin stats dasar sama multiplier dari stage
        enemy.maxHealth = baseHp * stageData.getHpMultiplier();
        enemy.health = enemy.maxHealth;
        enemy.walkSpeed = baseWalk * stageData.getSpeedMultiplier();
        enemy.runSpeed = baseRun * stageData.getSpeedMultiplier();
        enemy.damage = baseDmg * stageData.getDamageMultiplier();
        // jarak serang makin jauh dikit kalo musuh makin cepet
        enemy.attackRange = baseRange * ((stageData.getSpeedMultiplier() + 1f) / 2f);

        // atur posisi y sesuai tinggi tanah
        float y = terrain.getHeight(x, z);
        enemy.position.set(x, y, z);

        // mulai dengan animasi keluar dari tanah
        enemy.switchState(enemy.new EmergeState(), terrain);
        return enemy;
    }

    // method khusus buat spawn boss terakhir
    public BaseEnemy spawnDajjal(float x, float z, Terrain terrain){
        float y = terrain.getHeight(x, z);
        DajjalEntity boss = new DajjalEntity(dajjalModel, x, y, z);

        // dajjal langsung ngejar gak pake animasi keluar tanah
        boss.switchState(boss.new DajjalChaseState(), terrain);
        return boss;
    }
}
