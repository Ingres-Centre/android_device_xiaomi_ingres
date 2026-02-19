package vendor.lineage.gamekeys;

import vendor.lineage.gamekeys.GameKeyEvent;

@VintfStability
oneway interface IGameKeysEventListener {
    void onGameKeyEvent(in GameKeyEvent event);
}
