package com.finpro7.oop.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.utils.Array;

import com.finpro7.oop.entities.ItemPickup;
import com.finpro7.oop.entities.PlayerStats;
import com.finpro7.oop.world.weapon.Firearm;

public class ItemManager {

    // list buat nampung semua item yg lagi ada di map
    private Array<ItemPickup> activeItems = new Array<>();

    // model 3d buat setiap jenis item
    private Model coinModel;
    private Model healthModel;
    private Model ammoModel;

    // referensi ke statistik player buat nambah darah/ammo
    private PlayerStats playerStats;

    // referensi buat update ui duit
    private ItemListener listener;

    // interface biar gamescreen tau kalo ada item keambil
    public interface ItemListener {
        void onCoinCollected(int amount);
        void onHealthCollected(int amount);
        void onAmmoCollected(int amount);
    }

    public ItemManager(Model coinModel, Model healthModel, Model ammoModel, PlayerStats playerStats) {
        this.coinModel = coinModel;
        this.healthModel = healthModel;
        this.ammoModel = ammoModel;
        this.playerStats = playerStats;
    }

    public void setListener(ItemListener listener) {
        this.listener = listener;
    }

    // method buat spawn item pas musuh mati
    public void spawnItem(float x, float y, float z) {
        // pastiin spawn koin (100% dapet)
        activeItems.add(new ItemPickup(coinModel, x, y, z, ItemPickup.Type.COIN, 10));

        // peluang dapet darah 30%
        // geser dikit posisinya biar gak numpuk sama koin
        if (MathUtils.randomBoolean(0.3f)) {
            activeItems.add(new ItemPickup(healthModel, x + 0.5f, y, z + 0.5f, ItemPickup.Type.HEALTH, 20));
        }

        // peluang dapet peluru 40%
        // geser ke arah lain
        if (MathUtils.randomBoolean(0.4f)) {
            activeItems.add(new ItemPickup(ammoModel, x - 0.5f, y, z - 0.5f, ItemPickup.Type.AMMO, 30));
        }
    }

    // method update yg dipanggil tiap frame
    public void update(float delta, Vector3 playerPos, Firearm currentWeapon) {
        // loop terbalik biar aman pas ngehapus item
        for (int i = activeItems.size - 1; i >= 0; i--) {
            ItemPickup item = activeItems.get(i);

            // update animasi muter muter item
            item.update(delta);

            // cek jarak player ke item (1.5 meter buat ngambil)
            if (playerPos.dst(item.position) < 1.5f) {

                // logika efek pas diambil
                switch (item.type) {
                    case COIN:
                        playerStats.addCoins(item.value); // update ke playerstats biar langsung save lokal, solusi biar kalo internet putus koin tetep nambah
                        if (listener != null) listener.onCoinCollected(item.value);
                        saveCoinToServer(item.value); // kirim ke server buat backup doang
                        System.out.println("Dapet Duit: " + item.value);
                        break;

                    case HEALTH:
                        // nambah darah player tapi gak boleh lebih dari max
                        playerStats.health += item.value;
                        if(playerStats.health > playerStats.maxHealth) {
                            playerStats.health = playerStats.maxHealth;
                        }
                        if (listener != null) listener.onHealthCollected(item.value);
                        System.out.println("Dapet Darah: " + item.value);
                        break;

                    case AMMO:
                        // nambah peluru ke senjata yg lagi dipegang
                        if (currentWeapon != null) {
                            currentWeapon.totalAmmo += item.value;
                            if (listener != null) listener.onAmmoCollected(item.value);
                            System.out.println("Dapet Peluru: " + item.value);
                        }
                        break;
                }

                // hapus item dari dunia karena udah diambil
                activeItems.removeIndex(i);
            }
        }
    }

    // method buat render semua item
    public void render(ModelBatch batch, Environment env) {
        for (ItemPickup item : activeItems) {
            batch.render(item.instance, env);
        }
    }

    // logika simpen koin ke server
    private void saveCoinToServer(int amount) {
        com.badlogic.gdx.Preferences prefs = Gdx.app.getPreferences("UserSession");
        String username = prefs.getString("current_user", "Guest");

        // jangan save kalo user cuma tamu
        if (username.equals("Guest")) return;

        // update data lokal dulu biar cepet
        int currentTotal = prefs.getInteger("total_coins", 0);
        prefs.putInteger("total_coins", currentTotal + amount);
        prefs.flush();

        // kirim data ke server lewat http request
        String jsonContent = "{\"username\":\"" + username + "\", \"coins\":" + amount + "}";
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest httpRequest = requestBuilder.newRequest()
            .method(Net.HttpMethods.POST)
            .url("http://localhost:8081/auth/saveCoins")
            .header("Content-Type", "application/json")
            .content(jsonContent)
            .build();

        Gdx.net.sendHttpRequest(httpRequest, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                // log kalo sukses
                 Gdx.app.log("SERVER", "Coin saved: " + httpResponse.getResultAsString());
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.error("SERVER", "Gagal save koin: " + t.getMessage());
            }

            @Override
            public void cancelled() { }
        });
    }

    // bersih bersih memori
    public void dispose() {
        // model biasanya di dispose di asset manager, jadi disini cuma clear list aja
        activeItems.clear();
    }
}
