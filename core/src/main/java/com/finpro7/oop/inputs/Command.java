package com.finpro7.oop.inputs;

import com.finpro7.oop.managers.PlayerController;

public interface Command {
    // method utama yang bakal dipanggil pas tombol dipencet
    void execute(PlayerController player);
}
