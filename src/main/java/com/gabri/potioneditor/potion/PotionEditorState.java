package com.gabri.potioneditor.potion;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.server.MinecraftServer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class PotionEditorState extends SavedData {
    private static final String DATA_NAME = "potioneditor_potion_overrides";
    private static final Function<CompoundTag, PotionEditorState> LOAD = PotionEditorState::load;
    private static final Supplier<PotionEditorState> NEW = PotionEditorState::new;

    private final Map<String, PotionVariantData> overrides = new HashMap<>();

    public static PotionEditorState get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(LOAD, NEW, DATA_NAME);
    }

    public static PotionEditorState load(CompoundTag tag) {
        PotionEditorState state = new PotionEditorState();
        ListTag list = tag.getList("Overrides", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            state.overrides.put(entry.getString("Key"), PotionVariantData.load(entry.getCompound("Data")));
        }
        return state;
    }

    public Map<String, PotionVariantData> overridesCopy() {
        Map<String, PotionVariantData> copy = new HashMap<>();
        for (Map.Entry<String, PotionVariantData> entry : overrides.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().copy());
        }
        return copy;
    }

    public PotionVariantData getOverride(PotionVariantKey key) {
        PotionVariantData data = overrides.get(key.asStringKey());
        return data == null ? null : data.copy();
    }

    public void setOverride(PotionVariantKey key, PotionVariantData data) {
        if (key == null) {
            return;
        }

        if (data == null || data.isEmpty()) {
            overrides.remove(key.asStringKey());
        } else {
            overrides.put(key.asStringKey(), data.copy());
        }
        setDirty();
    }

    public void clearOverride(PotionVariantKey key) {
        if (key != null && overrides.remove(key.asStringKey()) != null) {
            setDirty();
        }
    }

    public boolean hasOverride(PotionVariantKey key) {
        return key != null && overrides.containsKey(key.asStringKey());
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<String, PotionVariantData> entry : overrides.entrySet()) {
            CompoundTag e = new CompoundTag();
            e.putString("Key", entry.getKey());
            e.put("Data", entry.getValue().save());
            list.add(e);
        }
        tag.put("Overrides", list);
        return tag;
    }
}

