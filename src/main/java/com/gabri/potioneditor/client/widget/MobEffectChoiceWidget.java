package com.gabri.potioneditor.client.widget;

import com.gabri.babel.core.client.ui.BabelTheme;
import com.gabri.babel.core.client.ui.BabelWidget;
import com.gabri.babel.core.client.ui.BabelSize;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

public class MobEffectChoiceWidget extends BabelWidget {
    private final MobEffect effect;
    private final Runnable onSelect;
    private final Component label;

    public MobEffectChoiceWidget(MobEffect effect, Runnable onSelect) {
        this.effect = effect;
        this.onSelect = onSelect == null ? () -> {} : onSelect;
        ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect);
        this.label = Component.literal(id == null ? "unknown" : id.toString());
        style().fillWidth();
        style().height(24);
        style().padding(4, 6);
        style().background(0x661F1F1F);
        style().border(BabelTheme.BORDER, 1);
    }

    @Override
    public BabelSize measure(Font font, int availableWidth, int availableHeight) {
        return resolveOuterSize(0, 24, availableWidth, availableHeight);
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        style().background(hovered() ? 0x882A2A2A : 0x661F1F1F);
        drawBackground(graphics);

        int iconX = bounds().x() + 5;
        int iconY = bounds().y() + 4;
        TextureAtlasSprite sprite = Minecraft.getInstance().getMobEffectTextures().get(effect);
        if (sprite != null) {
            graphics.blit(iconX, iconY, 0, 16, 16, sprite);
        }

        int textX = iconX + 22;
        int textY = bounds().y() + (bounds().height() - font.lineHeight) / 2;
        graphics.drawString(font, label, textX, textY, BabelTheme.TEXT, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isInside(mouseX, mouseY)) {
            setActive(true);
            onSelect.run();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && active()) {
            setActive(false);
            return true;
        }
        return false;
    }
}

