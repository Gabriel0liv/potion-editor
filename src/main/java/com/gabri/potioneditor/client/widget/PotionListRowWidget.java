package com.gabri.potioneditor.client.widget;

import com.gabri.babel.core.client.ui.BabelTheme;
import com.gabri.babel.core.client.ui.BabelWidget;
import com.gabri.babel.core.client.ui.BabelSize;
import com.gabri.potioneditor.potion.PotionCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.Util;
import net.minecraft.world.item.ItemStack;

public class PotionListRowWidget extends BabelWidget {
    private final Component title;
    private final Component subtitle;
    private final ItemStack icon;
    private final Runnable onSelect;
    private final Runnable onOpen;
    private boolean selected;
    private long lastClick;

    public PotionListRowWidget(Component title, Component subtitle, ItemStack icon, Runnable onSelect, Runnable onOpen) {
        this.title = title == null ? Component.empty() : title;
        this.subtitle = subtitle == null ? Component.empty() : subtitle;
        this.icon = icon == null ? ItemStack.EMPTY : icon;
        this.onSelect = onSelect == null ? () -> {} : onSelect;
        this.onOpen = onOpen == null ? () -> {} : onOpen;
        style().padding(5, 6);
        style().background(0x661F1F1F);
        style().border(BabelTheme.BORDER, 1);
        style().fillWidth();
        style().height(34);
    }

    public PotionListRowWidget selected(boolean selected) {
        this.selected = selected;
        return this;
    }

    @Override
    public BabelSize measure(Font font, int availableWidth, int availableHeight) {
        return resolveOuterSize(availableWidth, 34, availableWidth, availableHeight);
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        int background = selected ? 0x883B6CF6 : (hovered() ? 0x882A2A2A : 0x661F1F1F);
        graphics.fill(bounds().x(), bounds().y(), bounds().right(), bounds().bottom(), background);
        if (style().borderVisible()) {
            int color = style().borderColor();
            int bw = style().borderWidth();
            graphics.fill(bounds().x(), bounds().y(), bounds().right(), bounds().y() + bw, color);
            graphics.fill(bounds().x(), bounds().bottom() - bw, bounds().right(), bounds().bottom(), color);
            graphics.fill(bounds().x(), bounds().y(), bounds().x() + bw, bounds().bottom(), color);
            graphics.fill(bounds().right() - bw, bounds().y(), bounds().right(), bounds().bottom(), color);
        }

        int iconX = bounds().x() + 6;
        int iconY = bounds().y() + 9;
        if (!icon.isEmpty()) {
            graphics.renderItem(icon, iconX, iconY);
        }

        int textX = iconX + 20;
        int titleY = bounds().y() + 5;
        graphics.drawString(font, title, textX, titleY, BabelTheme.TEXT, false);
        if (!subtitle.getString().isEmpty()) {
            graphics.drawString(font, subtitle, textX, titleY + font.lineHeight + 1, BabelTheme.TEXT_MUTED, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !isInside(mouseX, mouseY)) {
            return false;
        }

        long now = Util.getMillis();
        if (now - lastClick < 260L) {
            onOpen.run();
        } else {
            onSelect.run();
        }
        lastClick = now;
        return true;
    }
}

