package vendor.lineage.gamekeys;

import vendor.lineage.gamekeys.IGameKeysEventListener;

@VintfStability
interface IGameKeys {
    void registerEventListener(in IGameKeysEventListener listener);
    void unregisterEventListener(in IGameKeysEventListener listener);
}
