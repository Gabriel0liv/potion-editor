package com.gabri.potioneditor.potion;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.alchemy.Potion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PotionCatalog {
    private PotionCatalog() {
    }

    public static List<PotionFamily> families() {
        Map<ResourceLocation, PotionFamilyBuilder> builders = new HashMap<>();

        for (ResourceLocation potionId : BuiltInRegistries.POTION.keySet()) {
            Potion potion = BuiltInRegistries.POTION.get(potionId);
            if (potion == null) {
                continue;
            }

            ResourceLocation familyId = familyId(potionId);
            builders.computeIfAbsent(familyId, PotionFamilyBuilder::new).add(potionId);
        }

        List<PotionFamily> families = new ArrayList<>();
        for (PotionFamilyBuilder builder : builders.values()) {
            families.add(builder.build());
        }
        families.sort(Comparator.comparing(f -> f.familyId().toString()));
        return families;
    }

    public static PotionFamily familyById(ResourceLocation familyId) {
        for (PotionFamily family : families()) {
            if (family.familyId().equals(familyId)) {
                return family;
            }
        }
        return null;
    }

    public static ResourceLocation familyId(ResourceLocation potionId) {
        String path = potionId.getPath();
        boolean changed;
        do {
            changed = false;
            if (path.startsWith("long_")) {
                path = path.substring("long_".length());
                changed = true;
            }
            if (path.startsWith("strong_")) {
                path = path.substring("strong_".length());
                changed = true;
            }
        } while (changed);

        return new ResourceLocation(potionId.getNamespace(), path);
    }

    public static String prettyName(ResourceLocation id) {
        String path = id.getPath().replace('_', ' ').trim();
        if (path.isEmpty()) {
            return id.toString();
        }

        StringBuilder builder = new StringBuilder();
        boolean capitalize = true;
        for (char ch : path.toCharArray()) {
            if (capitalize && Character.isLetter(ch)) {
                builder.append(Character.toUpperCase(ch));
                capitalize = false;
            } else {
                builder.append(ch);
            }
            if (ch == ' ') {
                capitalize = true;
            }
        }
        return builder.toString();
    }

    public record PotionFamily(ResourceLocation familyId, List<ResourceLocation> variants) {
    }

    private static final class PotionFamilyBuilder {
        private final ResourceLocation familyId;
        private final List<ResourceLocation> variants = new ArrayList<>();

        private PotionFamilyBuilder(ResourceLocation familyId) {
            this.familyId = familyId;
        }

        private void add(ResourceLocation potionId) {
            variants.add(potionId);
        }

        private PotionFamily build() {
            variants.sort(Comparator.comparing(ResourceLocation::toString));
            return new PotionFamily(familyId, List.copyOf(variants));
        }
    }
}

