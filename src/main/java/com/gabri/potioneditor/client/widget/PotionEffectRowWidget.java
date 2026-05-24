package com.gabri.potioneditor.client.widget;

import com.gabri.babel.core.client.ui.BabelAlign;
import com.gabri.babel.core.client.ui.BabelColumn;
import com.gabri.babel.core.client.ui.BabelButton;
import com.gabri.babel.core.client.ui.BabelLabel;
import com.gabri.babel.core.client.ui.BabelPanel;
import com.gabri.babel.core.client.ui.BabelRow;
import com.gabri.babel.core.client.ui.BabelSize;
import com.gabri.babel.core.client.ui.BabelWidget;
import com.gabri.babel.core.client.ui.BabelTextField;
import com.gabri.babel.core.client.ui.BabelTheme;
import com.gabri.potioneditor.potion.PotionEffectSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

public class PotionEffectRowWidget extends BabelPanel {
    private final PotionEffectSpec spec;
    private final Runnable onDirty;
    private final Runnable onRemove;
    private final EffectIconWidget effectIconWidget;
    private final BabelTextField effectField;
    private final BabelTextField durationField;
    private final BabelTextField amplifierField;
    private final BabelButton ambientButton;
    private final BabelButton visibleButton;
    private final BabelButton iconButton;

    public PotionEffectRowWidget(PotionEffectSpec spec, Runnable onDirty, Runnable onRemove) {
        this.spec = spec;
        this.onDirty = onDirty == null ? () -> {} : onDirty;
        this.onRemove = onRemove == null ? () -> {} : onRemove;

        style().fillWidth();
        style().gap(8);
        style().padding(8);
        style().background(0xFF1B2029);
        style().border(BabelTheme.BORDER, 1);

        effectIconWidget = new EffectIconWidget();
        effectField = new BabelTextField(spec.effectId(), value -> {
            spec.effectId(value.trim());
            this.onDirty.run();
        })
                .placeholder(Component.translatable("potioneditor.widget.effect_id_placeholder"))
                .maxLength(64)
                .fillWidth();

        BabelButton removeButton = new BabelButton(Component.literal("X"), () -> this.onRemove.run())
                .colors(0xFF8B3A3A, 0xFFB24A4A, 0xFFFFFFFF);
        removeButton.padding(0);
        removeButton.width(20);
        removeButton.height(18);

        BabelRow headerRow = new BabelRow().fillWidth().gap(8).align(BabelAlign.CENTER);
        headerRow.add(effectIconWidget, effectField, removeButton);

        BabelLabel durationLabel = new BabelLabel(Component.translatable("potioneditor.widget.duration"));
        durationLabel.style().textColor(BabelTheme.TEXT_MUTED);
        durationField = createNumberField(() -> spec.durationSeconds(), value -> {
            spec.durationSeconds(value);
            this.onDirty.run();
        }, "potioneditor.widget.seconds");

        BabelLabel amplifierLabel = new BabelLabel(Component.translatable("potioneditor.widget.amplifier"));
        amplifierLabel.style().textColor(BabelTheme.TEXT_MUTED);
        amplifierField = createNumberField(() -> spec.amplifier(), value -> {
            spec.amplifier(value);
            this.onDirty.run();
        }, "potioneditor.widget.level");

        BabelColumn durationColumn = new BabelColumn().fillWidth().gap(4);
        durationColumn.add(durationLabel, durationField);

        BabelColumn amplifierColumn = new BabelColumn().fillWidth().gap(4);
        amplifierColumn.add(amplifierLabel, amplifierField);

        BabelRow statsRow = new BabelRow().fillWidth().gap(8);
        statsRow.add(durationColumn, amplifierColumn);

        ambientButton = new BabelButton(Component.translatable("potioneditor.effect.ambient"), () -> {
            spec.ambient(!spec.ambient());
            this.onDirty.run();
        });
        visibleButton = new BabelButton(Component.translatable("potioneditor.effect.visible"), () -> {
            spec.visible(!spec.visible());
            this.onDirty.run();
        });
        iconButton = new BabelButton(Component.translatable("potioneditor.effect.icon"), () -> {
            spec.showIcon(!spec.showIcon());
            this.onDirty.run();
        });

        BabelRow togglesRow = new BabelRow().fillWidth().gap(6);
        togglesRow.add(ambientButton, visibleButton, iconButton);

        add(headerRow, statsRow, togglesRow);
        syncFields();
        syncLabels();
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics graphics, net.minecraft.client.gui.Font font, int mouseX, int mouseY, float partialTick) {
        syncFields();
        syncLabels();
        super.render(graphics, font, mouseX, mouseY, partialTick);
    }

    private BabelTextField createNumberField(java.util.function.IntSupplier getter, java.util.function.IntConsumer setter, String placeholderKey) {
        return new BabelTextField(String.valueOf(getter.getAsInt()), value -> {
            if (value == null || value.isBlank()) {
                setter.accept(0);
                return;
            }
            try {
                setter.accept(Math.max(0, Integer.parseInt(value)));
            } catch (NumberFormatException ignored) {
            }
        })
                .inputFilter(text -> text == null || text.isBlank() || text.chars().allMatch(Character::isDigit))
                .placeholder(Component.translatable(placeholderKey))
                .maxLength(8)
                .width(120);
    }

    private void syncFields() {
        syncTextField(effectField, spec.effectId());
        syncTextField(durationField, String.valueOf(spec.durationSeconds()));
        syncTextField(amplifierField, String.valueOf(spec.amplifier()));
    }

    private void syncLabels() {
        effectIconWidget.effectId(spec.effectId());
        syncToggle(ambientButton, "potioneditor.effect.ambient", spec.ambient());
        syncToggle(visibleButton, "potioneditor.effect.visible", spec.visible());
        syncToggle(iconButton, "potioneditor.effect.icon", spec.showIcon());
    }

    private static void syncTextField(BabelTextField field, String value) {
        if (field == null || field.focused()) {
            return;
        }
        String next = value == null ? "" : value;
        if (!next.equals(field.value())) {
            field.setValueSilently(next);
        }
    }

    private static void syncToggle(BabelButton button, String translationKey, boolean state) {
        button.text(Component.translatable(translationKey).append(": ").append(Component.translatable(state ? "potioneditor.toggle.on" : "potioneditor.toggle.off")));
        button.colors(state ? 0xFF256B3D : 0xFF4A4A4A, state ? 0xFF318A50 : 0xFF666666, 0xFFFFFFFF);
    }

    private static ResourceLocation parseEffectId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return id.contains(":") ? ResourceLocation.tryParse(id) : ResourceLocation.withDefaultNamespace(id);
    }

    private static final class EffectIconWidget extends BabelWidget {
        private ResourceLocation effectId;

        @Override
        public BabelSize measure(Font font, int availableWidth, int availableHeight) {
            return resolveOuterSize(18, 18, availableWidth, availableHeight);
        }

        public void effectId(String effectId) {
            this.effectId = parseEffectId(effectId);
        }

        @Override
        public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
            drawBackground(graphics);
            MobEffect effect = effectId == null ? null : BuiltInRegistries.MOB_EFFECT.get(effectId);
            if (effect == null) {
                return;
            }

            TextureAtlasSprite sprite = Minecraft.getInstance().getMobEffectTextures().get(effect);
            if (sprite != null) {
                graphics.blit(bounds().x() + 1, bounds().y() + 1, 0, 16, 16, sprite);
            }
        }
    }
}

