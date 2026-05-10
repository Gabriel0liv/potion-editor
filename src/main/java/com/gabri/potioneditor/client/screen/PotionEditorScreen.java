package com.gabri.potioneditor.client.screen;

import com.gabri.babel.core.client.ui.BabelButton;
import com.gabri.babel.core.client.ui.BabelAlign;
import com.gabri.babel.core.client.ui.BabelColumn;
import com.gabri.babel.core.client.ui.BabelLabel;
import com.gabri.babel.core.client.ui.BabelPanel;
import com.gabri.babel.core.client.ui.BabelRow;
import com.gabri.babel.core.client.ui.BabelScreen;
import com.gabri.babel.core.client.ui.BabelScrollPane;
import com.gabri.babel.core.client.ui.BabelTextField;
import com.gabri.babel.core.client.ui.BabelTemplates;
import com.gabri.babel.core.client.ui.BabelSlider;
import com.gabri.babel.core.client.ui.BabelTheme;
import com.gabri.potioneditor.client.widget.PotionColorPreviewWidget;
import com.gabri.potioneditor.client.widget.PotionEffectRowWidget;
import com.gabri.potioneditor.client.widget.MobEffectChoiceWidget;
import com.gabri.potioneditor.client.widget.PotionListRowWidget;
import com.gabri.potioneditor.network.PotionEditorNetwork;
import com.gabri.potioneditor.potion.PotionCatalog;
import com.gabri.potioneditor.potion.PotionEditorRuntime;
import com.gabri.potioneditor.potion.PotionEditorClientCache;
import com.gabri.potioneditor.potion.PotionForm;
import com.gabri.potioneditor.potion.PotionEffectSpec;
import com.gabri.potioneditor.potion.PotionVariantData;
import com.gabri.potioneditor.potion.PotionVariantKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.List;

public class PotionEditorScreen extends BabelScreen {
    private final ResourceLocation familyId;
    private PotionCatalog.PotionFamily family;
    private ResourceLocation selectedPotionId;
    private PotionForm selectedForm = PotionForm.DRINK;
    private PotionVariantData workingData = new PotionVariantData();
    private boolean dirty;

    private BabelScrollPane variantListPane;
    private BabelScrollPane effectListPane;
    private BabelButton addEffectButton;
    private BabelButton colorButton;
    private BabelLabel titleLabel;
    private BabelLabel statusLabel;
    private PotionColorPreviewWidget colorPreviewWidget;
    private BabelLabel colorStateLabel;
    private BabelTextField colorHexField;
    private BabelSlider colorRedSlider;
    private BabelSlider colorGreenSlider;
    private BabelSlider colorBlueSlider;
    private BabelLabel colorRedValue;
    private BabelLabel colorGreenValue;
    private BabelLabel colorBlueValue;
    private BabelTextField effectFilterField;
    private BabelScrollPane effectPickerListPane;
    private final List<PotionListRowWidget> variantRows = new ArrayList<>();
    private final List<BabelButton> formButtons = new ArrayList<>();
    private long lastCacheVersion = -1L;

    public PotionEditorScreen(ResourceLocation familyId) {
        super(Component.literal("Potion Editor"));
        this.familyId = familyId;
    }

    @Override
    protected BabelPanel buildRoot(int width, int height) {
        family = PotionCatalog.familyById(familyId);
        if (family == null) {
            family = new PotionCatalog.PotionFamily(familyId, List.of(familyId));
        }
        if (selectedPotionId == null) {
            selectedPotionId = family.variants().isEmpty() ? family.familyId() : family.variants().get(0);
        }

        BabelPanel root = BabelTemplates.screenShell();

        titleLabel = new BabelLabel(Component.literal("Potion Editor: " + PotionCatalog.prettyName(family.familyId())));
        titleLabel.style().textColor(0xFFFFFFFF);

        BabelButton backButton = new BabelButton(Component.literal("Back"), () -> Minecraft.getInstance().setScreen(new PotionCatalogScreen()));
        BabelButton saveButton = new BabelButton(Component.literal("Save"), this::saveCurrentVariant);
        BabelButton resetButton = new BabelButton(Component.literal("Reset"), this::resetCurrentVariant);
        statusLabel = new BabelLabel(Component.literal("Ready"));
        statusLabel.style().textColor(0xFF9BC2FF);

        BabelRow toolbar = BabelTemplates.toolbar(backButton, titleLabel, statusLabel, saveButton, resetButton);

        variantListPane = new BabelScrollPane();
        variantListPane.fillWidth().fillHeight();
        variantListPane.showScrollbar(true);
        BabelPanel variantPanel = new BabelPanel().fillHeight().width(260).gap(8);
        variantPanel.add(new BabelLabel(Component.literal("Variants")), variantListPane);

        effectListPane = new BabelScrollPane();
        effectListPane.fillWidth().fillHeight();
        effectListPane.showScrollbar(true);
        BabelPanel editorPanel = new BabelPanel().fillWidth().fillHeight().gap(8);
        editorPanel.add(buildFormBar(), buildEffectHeaderRow(), effectListPane);

        BabelRow body = BabelTemplates.splitPane(variantPanel, editorPanel, 260);
        body.fillHeight();
        root.add(toolbar, body);

        rebuildVariantRows();
        loadWorkingData();
        rebuildEffectRows();
        return root;
    }

    @Override
    public void tick() {
        if (!dirty && PotionEditorClientCache.version() != lastCacheVersion) {
            loadWorkingData();
            syncSelectedVariantRows();
            syncFormButtons();
            rebuildEffectRows();
        }
    }

    private BabelRow buildFormBar() {
        BabelRow row = new BabelRow().fillWidth().gap(6);
        formButtons.clear();
        for (PotionForm form : PotionForm.values()) {
            BabelButton button = new BabelButton(Component.literal(form.id()), () -> selectForm(form));
            formButtons.add(button);
            row.add(button);
        }
        colorButton = new BabelButton(Component.literal("Color"), this::openColorModal)
                .colors(0xFF3C6DF0, 0xFF5E89F5, 0xFFFFFFFF);
        row.add(new BabelLabel(Component.empty()).fillWidth(), colorButton);
        syncFormButtons();
        syncColorButton();
        return row;
    }

    private BabelRow buildEffectHeaderRow() {
        addEffectButton = new BabelButton(Component.literal("Add effect"), this::openEffectPickerModal)
                .colors(0xFF2F3642, 0xFF3A4253, BabelTheme.TEXT);
        BabelLabel hint = new BabelLabel(Component.literal("Search and add registered effects"));
        hint.style().textColor(BabelTheme.TEXT_MUTED);
        hint.wrap(true);
        BabelRow row = new BabelRow().fillWidth().gap(8).align(com.gabri.babel.core.client.ui.BabelAlign.CENTER);
        row.add(addEffectButton, hint.fillWidth());
        return row;
    }

    private void rebuildVariantRows() {
        if (variantListPane == null) {
            return;
        }

        variantListPane.clear();
        variantRows.clear();
        for (ResourceLocation potionId : family.variants()) {
            Potion potion = BuiltInRegistries.POTION.get(potionId);
            ItemStack icon = potion == null ? ItemStack.EMPTY : PotionUtils.setPotion(new ItemStack(Items.POTION), potion);
            ResourceLocation current = potionId;
            PotionListRowWidget row = new PotionListRowWidget(
                    Component.literal(PotionCatalog.prettyName(potionId)),
                    Component.literal(potionId.toString()),
                    icon,
                    () -> selectPotion(current),
                    () -> selectPotion(current)
            ).selected(current.equals(selectedPotionId)).fillWidth();
            variantRows.add(row);
            variantListPane.add(row);
        }
        syncSelectedVariantRows();
    }

    private void rebuildEffectRows() {
        if (effectListPane == null) {
            return;
        }

        effectListPane.clear();
        for (int i = 0; i < workingData.effects().size(); i++) {
            int index = i;
            PotionEffectSpec spec = workingData.effects().get(i);
            PotionEffectRowWidget row = new PotionEffectRowWidget(spec, () -> {
                setDirty();
                syncColorSection();
            }, () -> {
                workingData.effects().remove(index);
                setDirty();
                rebuildEffectRows();
            });
            effectListPane.add(row);
        }
        syncColorSection();
    }

    private void selectPotion(ResourceLocation potionId) {
        if (potionId == null) {
            return;
        }
        if (dirty) {
            saveCurrentVariant();
        }
        selectedPotionId = potionId;
        loadWorkingData();
        syncSelectedVariantRows();
        rebuildEffectRows();
        syncColorButton();
        syncColorSection();
    }

    private void selectForm(PotionForm form) {
        if (form == null) {
            return;
        }
        if (dirty) {
            saveCurrentVariant();
        }
        selectedForm = form;
        loadWorkingData();
        syncFormButtons();
        rebuildEffectRows();
        syncColorButton();
        syncColorSection();
    }

    private void loadWorkingData() {
        PotionVariantKey key = currentKey();
        workingData = PotionEditorRuntime.currentVariantData(key).copy();
        dirty = false;
        lastCacheVersion = PotionEditorClientCache.version();
        syncColorButton();
        syncColorSection();
        statusLabel.text(Component.literal(key.asStringKey()));
    }

    private void saveCurrentVariant() {
        PotionVariantKey key = currentKey();
        PotionEditorClientCache.put(key, workingData);
        PotionEditorNetwork.sendApply(key, workingData.copy(), false);
        lastCacheVersion = PotionEditorClientCache.version();
        dirty = false;
        statusLabel.text(Component.literal("Saved " + key.asStringKey()));
    }

    private void resetCurrentVariant() {
        PotionVariantKey key = currentKey();
        PotionEditorClientCache.remove(key);
        PotionEditorNetwork.sendApply(key, new PotionVariantData(), true);
        loadWorkingData();
        rebuildEffectRows();
        lastCacheVersion = PotionEditorClientCache.version();
        dirty = false;
        statusLabel.text(Component.literal("Reset " + key.asStringKey()));
    }

    private void syncSelectedVariantRows() {
        for (PotionListRowWidget row : variantRows) {
            row.selected(false);
        }
        for (int i = 0; i < family.variants().size(); i++) {
            if (family.variants().get(i).equals(selectedPotionId) && i < variantRows.size()) {
                variantRows.get(i).selected(true);
            }
        }
    }

    private void syncFormButtons() {
        for (int i = 0; i < formButtons.size(); i++) {
            PotionForm form = PotionForm.values()[i];
            BabelButton button = formButtons.get(i);
            boolean selected = form == selectedForm;
            button.text(Component.literal(form.id() + (selected ? " *" : "")));
            button.colors(selected ? 0xFF3B6CF6 : 0xFF4A4A4A, selected ? 0xFF5691FF : 0xFF666666, 0xFFFFFFFF);
        }
    }

    private void syncColorButton() {
        if (colorButton == null || workingData == null) {
            return;
        }

        int color = resolveCurrentColor();
        String label = workingData.customColor() == null ? "Auto" : "#" + formatHex(color);
        colorButton.text(Component.literal("Color · " + label));
        colorButton.colors(color, lightenColor(color), 0xFFFFFFFF);
    }

    private void openColorModal() {
        if (workingData == null) {
            return;
        }

        int initialColor = resolveCurrentColor();
        BabelPanel modal = new BabelPanel().width(520).gap(8).padding(10).background(0xFF1A1F28);
        BabelRow titleRow = new BabelRow().fillWidth().gap(8).align(BabelAlign.CENTER);
        BabelLabel title = new BabelLabel(Component.literal("Pick color"));
        title.style().textColor(BabelTheme.ACCENT);
        BabelButton closeButton = new BabelButton(Component.literal("Close"), this::closeModal)
                .colors(0xFF4A4A4A, 0xFF666666, 0xFFFFFFFF);
        titleRow.add(title, new BabelLabel(Component.empty()).fillWidth(), closeButton);

        colorPreviewWidget = new PotionColorPreviewWidget().width(94).height(94);
        colorPreviewWidget.setPreview(createPreviewStack(initialColor), initialColor, Component.literal(selectedForm.id()));

        colorHexField = new BabelTextField(formatHex(initialColor), value -> {
            Integer parsed = parseColor(value);
            if (parsed != null) {
                applyColor(parsed, true);
            }
        })
                .placeholder(Component.literal("hex RRGGBB"))
                .maxLength(8)
                .fillWidth();

        colorRedValue = new BabelLabel(Component.literal(String.valueOf((initialColor >> 16) & 0xFF)));
        colorGreenValue = new BabelLabel(Component.literal(String.valueOf((initialColor >> 8) & 0xFF)));
        colorBlueValue = new BabelLabel(Component.literal(String.valueOf(initialColor & 0xFF)));
        colorRedValue.style().width(28).textColor(BabelTheme.TEXT_MUTED);
        colorGreenValue.style().width(28).textColor(BabelTheme.TEXT_MUTED);
        colorBlueValue.style().width(28).textColor(BabelTheme.TEXT_MUTED);

        colorRedSlider = new BabelSlider((initialColor >> 16) & 0xFF, 0, 255, value -> applyColor(fromRgb(value, colorGreenSlider.value(), colorBlueSlider.value()), true));
        colorGreenSlider = new BabelSlider((initialColor >> 8) & 0xFF, 0, 255, value -> applyColor(fromRgb(colorRedSlider.value(), value, colorBlueSlider.value()), true));
        colorBlueSlider = new BabelSlider(initialColor & 0xFF, 0, 255, value -> applyColor(fromRgb(colorRedSlider.value(), colorGreenSlider.value(), value), true));

        BabelRow redRow = buildColorSliderRow("R", colorRedSlider, colorRedValue);
        BabelRow greenRow = buildColorSliderRow("G", colorGreenSlider, colorGreenValue);
        BabelRow blueRow = buildColorSliderRow("B", colorBlueSlider, colorBlueValue);

        BabelColumn controls = new BabelColumn().fillWidth().gap(6);
        BabelRow headRow = new BabelRow().fillWidth().gap(8).align(BabelAlign.CENTER);
        BabelLabel hexLabel = new BabelLabel(Component.literal("Hex"));
        hexLabel.style().textColor(BabelTheme.TEXT_MUTED);
        colorStateLabel = new BabelLabel(Component.literal(workingData.customColor() == null ? "Auto" : "#" + formatHex(initialColor)));
        colorStateLabel.style().textColor(BabelTheme.TEXT_MUTED);
        headRow.add(hexLabel, colorHexField, colorStateLabel);
        controls.add(headRow, redRow, greenRow, blueRow);

        BabelRow bodyRow = new BabelRow().fillWidth().gap(12).align(BabelAlign.CENTER);
        bodyRow.add(colorPreviewWidget, controls);
        modal.add(titleRow, bodyRow);

        openModal(modal);
        syncColorSection();
    }

    private void openEffectPickerModal() {
        BabelPanel modal = new BabelPanel().width(420).height(360).gap(8).padding(10).background(0xFF1A1F28);
        BabelRow titleRow = new BabelRow().fillWidth().gap(8).align(com.gabri.babel.core.client.ui.BabelAlign.CENTER);
        BabelLabel title = new BabelLabel(Component.literal("Add effect"));
        title.style().textColor(BabelTheme.ACCENT);
        BabelButton closeButton = new BabelButton(Component.literal("Close"), this::closeModal)
                .colors(0xFF4A4A4A, 0xFF666666, 0xFFFFFFFF);
        titleRow.add(title, new BabelLabel(Component.empty()).fillWidth(), closeButton);

        effectFilterField = new BabelTextField("", value -> rebuildEffectPickerList(value))
                .placeholder(Component.literal("Search by id or namespace"))
                .maxLength(64)
                .fillWidth();

        effectPickerListPane = new BabelScrollPane();
        effectPickerListPane.fillWidth().fillHeight();
        effectPickerListPane.showScrollbar(true);
        effectPickerListPane.background(BabelTheme.PANEL_ALT);
        effectPickerListPane.border(BabelTheme.BORDER, 1);
        effectPickerListPane.padding(4);

        BabelPanel content = new BabelPanel().fillWidth().fillHeight().gap(8).padding(0);
        content.background(0x00000000);
        content.border(0x00000000, 0);
        content.add(effectFilterField, effectPickerListPane);
        modal.add(titleRow, content);

        openModal(modal);
        rebuildEffectPickerList("");
        effectFilterField.focus();
    }

    private void rebuildEffectPickerList(String query) {
        if (effectPickerListPane == null) {
            return;
        }

        effectPickerListPane.clear();
        String normalized = normalizeQuery(query);
        List<MobEffect> effects = BuiltInRegistries.MOB_EFFECT.stream()
                .sorted(Comparator.comparing(effect -> {
                    ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect);
                    return id == null ? "" : id.toString();
                }))
                .filter(effect -> matchesEffectQuery(effect, normalized))
                .toList();

        if (effects.isEmpty()) {
            BabelLabel empty = new BabelLabel(Component.literal("No effects match this query."));
            empty.style().textColor(BabelTheme.TEXT_MUTED);
            effectPickerListPane.add(empty);
            return;
        }

        for (MobEffect effect : effects) {
            ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect);
            if (id == null) {
                continue;
            }
            effectPickerListPane.add(new MobEffectChoiceWidget(effect, () -> {
                addEffect(id);
                closeModal();
            }));
        }
    }

    private void addEffect(ResourceLocation id) {
        if (workingData == null || id == null) {
            return;
        }
        workingData.effects().add(new PotionEffectSpec(id.toString(), 20 * 10, 0, false, true, true));
        setDirty();
        rebuildEffectRows();
        syncColorButton();
        syncColorSection();
    }

    private void applyColorInput(String value) {
        Integer parsed = parseColor(value);
        if (parsed != null) {
            applyColor(parsed, true);
        }
    }

    private void applyColor(int color, boolean commit) {
        if (workingData == null) {
            return;
        }

        int cleaned = color & 0xFFFFFF;
        Integer previous = workingData.customColor();
        if (commit) {
            workingData.customColor(cleaned);
            if (previous == null || previous.intValue() != cleaned) {
                setDirty();
            }
        }
        syncColorSection();
    }

    private void syncColorSection() {
        if (workingData == null) {
            return;
        }

        int resolved = resolveCurrentColor();
        boolean custom = workingData.customColor() != null;
        if (colorStateLabel != null) {
            colorStateLabel.text(Component.literal(custom ? "#" + formatHex(resolved) : "Auto"));
        }

        if (colorPreviewWidget != null) {
            colorPreviewWidget.setPreview(createPreviewStack(resolved), resolved, Component.literal(selectedForm.id()));
        }
        if (colorHexField != null && !colorHexField.focused()) {
            colorHexField.setValueSilently(formatHex(resolved));
        }
        if (colorRedSlider != null) {
            colorRedSlider.setValueSilently((resolved >> 16) & 0xFF);
        }
        if (colorGreenSlider != null) {
            colorGreenSlider.setValueSilently((resolved >> 8) & 0xFF);
        }
        if (colorBlueSlider != null) {
            colorBlueSlider.setValueSilently(resolved & 0xFF);
        }
        if (colorRedValue != null) {
            colorRedValue.text(Component.literal(String.valueOf((resolved >> 16) & 0xFF)));
        }
        if (colorGreenValue != null) {
            colorGreenValue.text(Component.literal(String.valueOf((resolved >> 8) & 0xFF)));
        }
        if (colorBlueValue != null) {
            colorBlueValue.text(Component.literal(String.valueOf(resolved & 0xFF)));
        }
    }

    private BabelRow buildColorSliderRow(String labelText, BabelSlider slider, BabelLabel valueLabel) {
        BabelRow row = new BabelRow().fillWidth().gap(8).align(BabelAlign.CENTER);
        BabelLabel label = new BabelLabel(Component.literal(labelText));
        label.style().width(14).textColor(BabelTheme.TEXT_MUTED);
        row.add(label, slider, valueLabel);
        return row;
    }

    private int resolveCurrentColor() {
        Integer custom = workingData == null ? null : workingData.customColor();
        if (custom != null) {
            return custom & 0xFFFFFF;
        }

        List<MobEffectInstance> effects = workingData == null ? List.of() : workingData.effects().stream()
                .map(PotionEffectSpec::toInstance)
                .filter(instance -> instance != null)
                .toList();
        if (effects.isEmpty()) {
            return 0x3F76E4;
        }
        return PotionUtils.getColor(effects);
    }

    private static String formatHex(int color) {
        return String.format("%06X", color & 0xFFFFFF);
    }

    private static int lightenColor(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        r = Math.min(255, r + 24);
        g = Math.min(255, g + 24);
        b = Math.min(255, b + 24);
        return (r << 16) | (g << 8) | b;
    }

    private static String normalizeQuery(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean matchesEffectQuery(MobEffect effect, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }

        ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect);
        if (id == null) {
            return false;
        }

        String name = PotionCatalog.prettyName(id).toLowerCase(Locale.ROOT);
        String full = id.toString().toLowerCase(Locale.ROOT);
        return full.contains(query) || name.contains(query);
    }

    private void setDirty() {
        dirty = true;
        statusLabel.text(Component.literal("Unsaved changes"));
    }

    private PotionVariantKey currentKey() {
        return new PotionVariantKey(selectedPotionId, selectedForm);
    }

    private static Integer parseColor(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.trim();
        if (cleaned.startsWith("#")) {
            cleaned = cleaned.substring(1);
        }

        if (cleaned.isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(cleaned, 16) & 0xFFFFFF;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private ItemStack createPreviewStack(int resolvedColor) {
        Potion potion = BuiltInRegistries.POTION.get(selectedPotionId);
        ItemStack stack = new ItemStack(selectedForm.item());
        if (potion != null) {
            PotionUtils.setPotion(stack, potion);
        }
        stack.getOrCreateTag().putInt("CustomPotionColor", resolvedColor & 0xFFFFFF);
        return stack;
    }

    private static int fromRgb(int red, int green, int blue) {
        return ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
    }
}

