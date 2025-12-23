package com.finpro7.oop.entities;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;

public class Majuj extends BaseEnemy {

    public Majuj(Model model) {
        this.modelInstance = new ModelInstance(model);
        this.animController = new AnimationController(modelInstance);
        // bisa ditambahin logic khusus majuj kalo ada waktuu
    }
}
