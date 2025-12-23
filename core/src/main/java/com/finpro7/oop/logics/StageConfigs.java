package com.finpro7.oop.logics;

// ini cetakan dasar buat semua stage yg ada, total ada 6
public class StageConfigs {

    // abstract class biar semua stage punya struktur data yang sama
    public static abstract class BaseStage {
        public abstract int getTotalEnemies(); // kuota zombie di stage ini
        public abstract float getHpMultiplier(); // pengali darah
        public abstract float getSpeedMultiplier(); // pengali lari
        public abstract float getDamageMultiplier(); // pengali sakit pukulan
        public abstract float getSpawnInterval(); // jeda spawn antar zombie di stage ini
    }

    // STAGE 1: Kaki Gunung (Pemanasan)
    public static class StageOne extends BaseStage {
        @Override public int getTotalEnemies() { return 1; } // cuma 5 curut
        @Override public float getHpMultiplier() { return 1.0f; } // normal
        @Override public float getSpeedMultiplier() { return 1.0f; } // pelan
        @Override public float getDamageMultiplier() { return 1.0f; }
        @Override public float getSpawnInterval() { return 4.0f; } // munculnya santai
    }

    // STAGE 2: Mulai Nanjak
    public static class StageTwo extends BaseStage {
        @Override public int getTotalEnemies() { return 1; }
        @Override public float getHpMultiplier() { return 1.2f; }
        @Override public float getSpeedMultiplier() { return 1.1f; }
        @Override public float getDamageMultiplier() { return 1.2f; }
        @Override public float getSpawnInterval() { return 3.5f; }
    }

    // STAGE 3: Pertengahan
    public static class StageThree extends BaseStage {
        @Override public int getTotalEnemies() { return 1; }
        @Override public float getHpMultiplier() { return 1.5f; }
        @Override public float getSpeedMultiplier() { return 1.25f; }
        @Override public float getDamageMultiplier() { return 1.4f; }
        @Override public float getSpawnInterval() { return 3.0f; }
    }

    // STAGE 4: Agak Tinggi
    public static class StageFour extends BaseStage {
        @Override public int getTotalEnemies() { return 1; }
        @Override public float getHpMultiplier() { return 1.8f; }
        @Override public float getSpeedMultiplier() { return 1.4f; }
        @Override public float getDamageMultiplier() { return 1.6f; }
        @Override public float getSpawnInterval() { return 2.5f; }
    }

    // STAGE 5: Deket Puncak
    public static class StageFive extends BaseStage {
        @Override public int getTotalEnemies() { return 1; }
        @Override public float getHpMultiplier() { return 2.2f; }
        @Override public float getSpeedMultiplier() { return 1.6f; }
        @Override public float getDamageMultiplier() { return 1.8f; }
        @Override public float getSpawnInterval() { return 1.5f; } // makin cepet munculnya
    }

    // STAGE 6: THE PEAK (Neraka)
    public static class StageSix extends BaseStage {
        @Override public int getTotalEnemies() { return 1; } // rame bangett
        @Override public float getHpMultiplier() { return 3.0f; } // tebel bat
        @Override public float getSpeedMultiplier() { return 2.0f; } // ngebut
        @Override public float getDamageMultiplier() { return 2.5f; } // sekali pukul sakit
        @Override public float getSpawnInterval() { return 0.8f; } // spam musuh
    }
}
