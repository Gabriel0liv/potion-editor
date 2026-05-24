package com.gabri.potioneditor.client;

import com.gabri.potioneditor.PotionEditor;
import com.gabri.potioneditor.PotionEditorHooks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod.EventBusSubscriber(modid = PotionEditor.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class PotionEditorClient {
    private PotionEditorClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            PotionEditorHooks.openCatalogScreen = PotionEditorClientHooks::openCatalogScreen;
            PotionEditorClientHooks.registerClientEvents();
        });
    }
}
