package com.gabri.potioneditor.client;

import com.gabri.potioneditor.client.screen.PotionCatalogScreen;
import net.minecraft.client.Minecraft;

public final class PotionEditorClientHooks {
    private PotionEditorClientHooks() {
    }

    public static void openCatalogScreen() {
        Minecraft.getInstance().setScreen(new PotionCatalogScreen());
    }
}
