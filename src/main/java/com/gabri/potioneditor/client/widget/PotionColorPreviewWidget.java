package com.gabri.potioneditor.client.widget;

import com.gabri.babel.core.client.ui.BabelSize;
import com.gabri.babel.core.client.ui.BabelTheme;
import com.gabri.babel.core.client.ui.BabelWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class PotionColorPreviewWidget extends BabelWidget {
    private ItemStack stack = ItemStack.EMPTY;
    private int color = 0x3F76E4;
    private Component caption = Component.translatable("potioneditor.preview");

    public PotionColorPreviewWidget setPreview(ItemStack stack, int color, Component caption) {
        this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
        this.color = color & 0xFFFFFF;
        this.caption = caption == null ? Component.translatable("potioneditor.preview") : caption;
        return this;
    }

    @Override
    public BabelSize measure(Font font, int availableWidth, int availableHeight) {
        int size = Math.max(132, Math.min(availableWidth, availableHeight));
        size = Math.min(size, 176);
        return resolveOuterSize(size, size, availableWidth, availableHeight);
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        drawBackground(graphics);

        int borderColor = hovered() ? BabelTheme.ACCENT_HOVER : BabelTheme.BORDER;
        graphics.fill(bounds().x(), bounds().y(), bounds().right(), bounds().y() + 1, borderColor);
        graphics.fill(bounds().x(), bounds().bottom() - 1, bounds().right(), bounds().bottom(), borderColor);
        graphics.fill(bounds().x(), bounds().y(), bounds().x() + 1, bounds().bottom(), borderColor);
        graphics.fill(bounds().right() - 1, bounds().y(), bounds().right(), bounds().bottom(), borderColor);

        int colorFill = 0xFF000000 | (color & 0xFFFFFF);
        graphics.fill(bounds().x() + 2, bounds().y() + 2, bounds().right() - 2, bounds().bottom() - 18, colorFill);

        if (!stack.isEmpty()) {
            float scale = Math.min(
                    (bounds().width() - 16) / 16.0f,
                    (bounds().height() - 40) / 16.0f
            );
            scale = Math.max(2.5f, Math.min(scale, 5.5f));
            int scaledSize = Math.round(16 * scale);
            int itemX = bounds().x() + (bounds().width() - scaledSize) / 2;
            int itemY = bounds().y() + (bounds().height() - 40 - scaledSize) / 2 + 2;
            graphics.pose().pushPose();
            graphics.pose().translate(itemX, itemY, 0.0F);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.renderItem(stack, 0, 0);
            graphics.pose().popPose();
        }

        int captionColor = contrastTextColor(color);
        graphics.drawCenteredString(
                font,
                caption,
                bounds().x() + bounds().width() / 2,
                bounds().bottom() - font.lineHeight - 4,
                captionColor
        );
    }

    private static int contrastTextColor(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int luminance = (r * 299 + g * 587 + b * 114) / 1000;
        return luminance > 150 ? 0xFF111111 : 0xFFFFFFFF;
    }
}

