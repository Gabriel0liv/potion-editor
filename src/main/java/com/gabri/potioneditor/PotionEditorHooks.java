package com.gabri.potioneditor;

public final class PotionEditorHooks {
    public static Runnable openCatalogScreen = () -> {};

    private PotionEditorHooks() {
    }

    public static void openCatalog() {
        openCatalogScreen.run();
    }
}
