package com.gabri.potioneditor.client;

import com.gabri.potioneditor.network.PotionEditorNetwork;
import com.gabri.potioneditor.client.screen.PotionCatalogScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class PotionEditorClientHooks {
    private static boolean helloSent;

    private PotionEditorClientHooks() {
    }

    public static void openCatalogScreen() {
        Minecraft.getInstance().setScreen(new PotionCatalogScreen());
    }

    public static void registerClientEvents() {
        MinecraftForge.EVENT_BUS.register(PotionEditorClientHooks.class);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        boolean connected = minecraft.player != null && minecraft.getConnection() != null;
        if (!connected) {
            helloSent = false;
            return;
        }

        if (!helloSent) {
            PotionEditorNetwork.sendClientHello();
            helloSent = true;
        }
    }
}
