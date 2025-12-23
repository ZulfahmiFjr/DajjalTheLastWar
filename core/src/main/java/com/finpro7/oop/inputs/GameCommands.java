package com.finpro7.oop.inputs;

import com.badlogic.gdx.math.Vector3;
import com.finpro7.oop.managers.PlayerController;

public class GameCommands {

    // perintah buat maju
    public static class MoveForward implements Command {
        @Override
        public void execute(PlayerController player) {
            // kita ambil arah depan dari kamera terus kasih tau controller buat gerak ke sana
            Vector3 forward = new Vector3(player.getCamDirection().x, 0f, player.getCamDirection().z).nor();
            player.addMovementInput(forward);
        }
    }

    // perintah buat mundur
    public static class MoveBackward implements Command {
        @Override
        public void execute(PlayerController player) {
            // kebalikan dari forward
            Vector3 backward = new Vector3(player.getCamDirection().x, 0f, player.getCamDirection().z).nor().scl(-1);
            player.addMovementInput(backward);
        }
    }

    // perintah buat geser kanan (strafe)
    public static class StrafeRight implements Command {
        @Override
        public void execute(PlayerController player) {
            Vector3 forward = new Vector3(player.getCamDirection().x, 0f, player.getCamDirection().z).nor();
            Vector3 right = new Vector3(forward).crs(Vector3.Y).nor();
            player.addMovementInput(right);
        }
    }

    // perintah buat geser kiri
    public static class StrafeLeft implements Command {
        @Override
        public void execute(PlayerController player) {
            Vector3 forward = new Vector3(player.getCamDirection().x, 0f, player.getCamDirection().z).nor();
            Vector3 left = new Vector3(forward).crs(Vector3.Y).nor().scl(-1);
            player.addMovementInput(left);
        }
    }

    // perintah buat lompat
    public static class Jump implements Command {
        @Override
        public void execute(PlayerController player) {
            player.performJump();
        }
    }

    // perintah buat lari
    public static class Sprint implements Command {
        @Override
        public void execute(PlayerController player) {
            player.setSprinting(true);
        }
    }
}
