package com.gabri.potioneditor.client.screen;

import com.gabri.babel.core.client.ui.BabelButton;
import com.gabri.babel.core.client.ui.BabelColumn;
import com.gabri.babel.core.client.ui.BabelLabel;
import com.gabri.babel.core.client.ui.BabelPanel;
import com.gabri.babel.core.client.ui.BabelRow;
import com.gabri.babel.core.client.ui.BabelScreen;
import com.gabri.babel.core.client.ui.BabelScrollPane;
import com.gabri.babel.core.client.ui.BabelTextField;
import com.gabri.babel.core.client.ui.BabelTemplates;
import com.gabri.potioneditor.client.widget.PotionListRowWidget;
import com.gabri.potioneditor.potion.PotionCatalog;
import com.gabri.potioneditor.potion.PotionCatalog.PotionFamily;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;

import java.util.ArrayList;
import java.util.List;

public class PotionCatalogScreen extends BabelScreen {
    private final List<PotionFamily> allFamilies = new ArrayList<>(PotionCatalog.families());
    private final List<PotionFamily> filteredFamilies = new ArrayList<>();
    private BabelScrollPane listPane;
    private BabelTextField searchField;
    private PotionFamily selectedFamily;

    public PotionCatalogScreen() {
        super(Component.literal("Potion Catalog"));
        filteredFamilies.addAll(allFamilies);
        if (!filteredFamilies.isEmpty()) {
            selectedFamily = filteredFamilies.get(0);
        }
    }

    @Override
    protected BabelPanel buildRoot(int width, int height) {
        BabelPanel root = BabelTemplates.screenShell();

        BabelLabel title = new BabelLabel(Component.literal("Potion Catalog"));
        title.style().textColor(0xFFFFFFFF);

        searchField = new BabelTextField("", value -> {
            filter(value);
            rebuildList();
        }).placeholder(Component.literal("Search potions")).maxLength(64).fillWidth();

        BabelButton refreshButton = new BabelButton(Component.literal("Refresh"), () -> {
            filter(searchField.value());
            rebuildList();
        });

        BabelRow toolbar = BabelTemplates.toolbar(title, searchField, refreshButton);

        listPane = new BabelScrollPane();
        listPane.fillWidth().fillHeight();
        listPane.showScrollbar(true);

        BabelPanel content = new BabelPanel().fillWidth().fillHeight().gap(8);
        content.add(listPane);

        root.add(toolbar, content);
        rebuildList();
        return root;
    }

    private void filter(String query) {
        filteredFamilies.clear();
        String normalized = query == null ? "" : query.trim().toLowerCase();
        for (PotionFamily family : allFamilies) {
            if (normalized.isEmpty() || PotionCatalog.prettyName(family.familyId()).toLowerCase().contains(normalized)) {
                filteredFamilies.add(family);
            }
        }
        if (selectedFamily != null && !filteredFamilies.contains(selectedFamily) && !filteredFamilies.isEmpty()) {
            selectedFamily = filteredFamilies.get(0);
        }
    }

    private void rebuildList() {
        if (listPane == null) {
            return;
        }

        listPane.clear();
        for (PotionFamily family : filteredFamilies) {
            Potion potion = null;
            if (!family.variants().isEmpty()) {
                potion = net.minecraft.core.registries.BuiltInRegistries.POTION.get(family.variants().get(0));
            }
            ItemStack icon = potion == null ? ItemStack.EMPTY : PotionUtils.setPotion(new ItemStack(net.minecraft.world.item.Items.POTION), potion);
            PotionFamily current = family;
            PotionListRowWidget row = new PotionListRowWidget(
                    Component.literal(PotionCatalog.prettyName(family.familyId())),
                    Component.literal(family.variants().size() + " variants"),
                    icon,
                    () -> selectedFamily = current,
                    () -> Minecraft.getInstance().setScreen(new PotionEditorScreen(current.familyId()))
            ).selected(current.equals(selectedFamily)).fillWidth();
            listPane.add(row);
        }
    }
}

