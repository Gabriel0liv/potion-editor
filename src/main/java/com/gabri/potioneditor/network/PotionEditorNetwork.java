package com.gabri.potioneditor.network;

import com.gabri.potioneditor.PotionEditor;
import com.gabri.potioneditor.potion.PotionEditorClientCache;
import com.gabri.potioneditor.potion.PotionEditorRuntime;
import com.gabri.potioneditor.potion.PotionEditorState;
import com.gabri.potioneditor.potion.PotionVariantData;
import com.gabri.potioneditor.potion.PotionVariantKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Map;
import java.util.function.Supplier;

public final class PotionEditorNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(PotionEditor.id("main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    private static int nextId = 0;

    private PotionEditorNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(OpenCatalogPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenCatalogPacket::encode)
                .decoder(OpenCatalogPacket::decode)
                .consumerMainThread(OpenCatalogPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncOverridesPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncOverridesPacket::encode)
                .decoder(SyncOverridesPacket::decode)
                .consumerMainThread(SyncOverridesPacket::handle)
                .add();

        CHANNEL.messageBuilder(ApplyOverridePacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ApplyOverridePacket::encode)
                .decoder(ApplyOverridePacket::decode)
                .consumerMainThread(ApplyOverridePacket::handle)
                .add();
    }

    public static void syncToAll(PotionEditorState state) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), new SyncOverridesPacket(state.overridesCopy()));
    }

    public static void syncToPlayer(ServerPlayer player, PotionEditorState state) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncOverridesPacket(state.overridesCopy()));
    }

    public static void openCatalog(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenCatalogPacket());
    }

    public static void sendApply(PotionVariantKey key, PotionVariantData data, boolean clear) {
        CHANNEL.sendToServer(new ApplyOverridePacket(key, data, clear));
    }

    private record OpenCatalogPacket() {
        private static void encode(OpenCatalogPacket packet, FriendlyByteBuf buffer) {
        }

        private static OpenCatalogPacket decode(FriendlyByteBuf buffer) {
            return new OpenCatalogPacket();
        }

        private static void handle(OpenCatalogPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                if (FMLEnvironment.dist == Dist.CLIENT) {
                    com.gabri.potioneditor.client.PotionEditorClientHooks.openCatalogScreen();
                }
            });
            context.setPacketHandled(true);
        }
    }

    private record SyncOverridesPacket(Map<String, PotionVariantData> overrides) {
        private static void encode(SyncOverridesPacket packet, FriendlyByteBuf buffer) {
            CompoundTag root = new CompoundTag();
            CompoundTag overridesTag = new CompoundTag();
            for (Map.Entry<String, PotionVariantData> entry : packet.overrides.entrySet()) {
                overridesTag.put(entry.getKey(), entry.getValue().save());
            }
            root.put("Overrides", overridesTag);
            buffer.writeNbt(root);
        }

        private static SyncOverridesPacket decode(FriendlyByteBuf buffer) {
            CompoundTag root = buffer.readNbt();
            Map<String, PotionVariantData> overrides = new java.util.HashMap<>();
            if (root != null && root.contains("Overrides")) {
                CompoundTag overridesTag = root.getCompound("Overrides");
                for (String key : overridesTag.getAllKeys()) {
                    overrides.put(key, PotionVariantData.load(overridesTag.getCompound(key)));
                }
            }
            return new SyncOverridesPacket(overrides);
        }

        private static void handle(SyncOverridesPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                PotionEditorClientCache.replaceAll(packet.overrides);
            });
            context.setPacketHandled(true);
        }
    }

    private record ApplyOverridePacket(PotionVariantKey key, PotionVariantData data, boolean clear) {
        private static void encode(ApplyOverridePacket packet, FriendlyByteBuf buffer) {
            buffer.writeNbt(packet.key.save());
            buffer.writeBoolean(packet.clear);
            buffer.writeNbt(packet.data == null ? new CompoundTag() : packet.data.save());
        }

        private static ApplyOverridePacket decode(FriendlyByteBuf buffer) {
            CompoundTag keyTag = buffer.readNbt();
            PotionVariantKey key = PotionVariantKey.load(keyTag == null ? new CompoundTag() : keyTag);
            boolean clear = buffer.readBoolean();
            CompoundTag dataTag = buffer.readNbt();
            PotionVariantData data = dataTag == null ? new PotionVariantData() : PotionVariantData.load(dataTag);
            return new ApplyOverridePacket(key, data, clear);
        }

        private static void handle(ApplyOverridePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            ServerPlayer player = context.getSender();
            context.enqueueWork(() -> {
                if (player == null || !player.hasPermissions(2)) {
                    return;
                }

                PotionEditorState state = PotionEditorState.get(player.server);
                if (packet.clear) {
                    state.clearOverride(packet.key);
                } else {
                    state.setOverride(packet.key, packet.data);
                }
                state.setDirty();
                PotionEditorNetwork.syncToAll(state);
                player.server.overworld().getDataStorage().save();
            });
            context.setPacketHandled(true);
        }
    }
}

