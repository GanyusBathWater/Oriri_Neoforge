package net.ganyusbathwater.oririmod.effect.vestiges;

public interface VestigeEffect {

    /** Wird jedes Tick aufgerufen, solange ausgerüstet */
    void tick(VestigeContext ctx);

    /** Wird beim Ausrüsten aufgerufen */
    default void onEquip(VestigeContext ctx) {}

    /** Wird beim Ablegen aufgerufen */
    default void onUnequip(VestigeContext ctx) {}
}
