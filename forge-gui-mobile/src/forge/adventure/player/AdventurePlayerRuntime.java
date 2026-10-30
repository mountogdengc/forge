package forge.adventure.player;

/** Runtime listeners belong to a UI session and must never survive a campaign switch. */
public final class AdventurePlayerRuntime {
    private AdventurePlayerRuntime() { }

    public static void clearUiListeners(AdventurePlayer player) {
        player.onLifeTotalChangeList.clear();
        player.onShardsChangeList.clear();
        player.onGoldChangeList.clear();
        player.onPlayerChangeList.clear();
        player.onEquipmentChange.clear();
        player.onBlessing.clear();
    }
}
