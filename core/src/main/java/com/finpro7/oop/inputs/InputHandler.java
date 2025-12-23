package com.finpro7.oop.inputs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.finpro7.oop.managers.PlayerController;

public class InputHandler {

    // kita simpen commandnya di variabel biar reusable
    private Command moveForward;
    private Command moveBackward;
    private Command strafeLeft;
    private Command strafeRight;
    private Command jump;
    private Command sprint;

    public InputHandler() {
        // pasang command ke masing masing slot
        moveForward = new GameCommands.MoveForward();
        moveBackward = new GameCommands.MoveBackward();
        strafeLeft = new GameCommands.StrafeLeft();
        strafeRight = new GameCommands.StrafeRight();
        jump = new GameCommands.Jump();
        sprint = new GameCommands.Sprint();
    }

    // method ini dipanggil tiap frame dari player controller
    public void handleInput(PlayerController player) {
        // reset status lari dulu tiap frame
        player.setSprinting(false);

        // cek tombol wasd
        if (Gdx.input.isKeyPressed(Input.Keys.W)) moveForward.execute(player);
        if (Gdx.input.isKeyPressed(Input.Keys.S)) moveBackward.execute(player);
        if (Gdx.input.isKeyPressed(Input.Keys.A)) strafeLeft.execute(player);
        if (Gdx.input.isKeyPressed(Input.Keys.D)) strafeRight.execute(player);

        // cek tombol spasi buat lompat
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) jump.execute(player);

        // cek tombol shift buat lari
        if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) sprint.execute(player);
    }
}
