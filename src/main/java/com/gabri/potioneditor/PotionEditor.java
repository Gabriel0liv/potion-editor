package com.gabri.potioneditor;

import com.gabri.potioneditor.network.PotionEditorNetwork;
import com.gabri.potioneditor.potion.PotionEditorState;
import com.gabri.babel.core.api.BabelItems;
import com.gabri.babel.core.item.BabelItemStackResolver;
import com.gabri.babel.core.item.BabelItemStackView;
import com.gabri.potioneditor.potion.PotionEditorRuntime;
import com.gabri.potioneditor.potion.PotionVariantData;
import com.gabri.potioneditor.potion.PotionVariantKey;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod(PotionEditor.MODID)
public class PotionEditor {
    public static final String MODID = "potioneditor";
    private static final String COLOR_RESOLVER_ID = "potioneditor:variant_color";

    public PotionEditor() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, PotionEditorConfig.SPEC);
        registerBabelResolvers();
        PotionEditorNetwork.register();
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static net.minecraft.resources.ResourceLocation id(String path) {
        return new net.minecraft.resources.ResourceLocation(MODID, path);
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            PotionEditorNetwork.syncToPlayer(player, PotionEditorState.get(player.server));
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        PotionEditorCommands.register(event.getDispatcher());
    }

    private static void registerBabelResolvers() {
        BabelItems.colorResolvers().register(new BabelItemStackResolver<>() {
            @Override
            public String id() {
                return COLOR_RESOLVER_ID;
            }

            @Override
            public int priority() {
                return 100;
            }

            @Override
            public boolean applies(BabelItemStackView view) {
                if (view == null || view.isEmpty()) {
                    return false;
                }

                return switch (view.itemId().toString()) {
                    case "minecraft:potion", "minecraft:splash_potion", "minecraft:lingering_potion" -> true;
                    default -> false;
                };
            }

            @Override
            public Integer resolve(BabelItemStackView view, Integer currentValue) {
                PotionVariantKey key = PotionVariantKey.from(view.stack());
                PotionVariantData data = PotionEditorRuntime.currentVariantData(key);
                if (data != null && data.customColor() != null) {
                    return data.customColor() & 0xFFFFFF;
                }

                return currentValue;
            }
        });
    }
}
