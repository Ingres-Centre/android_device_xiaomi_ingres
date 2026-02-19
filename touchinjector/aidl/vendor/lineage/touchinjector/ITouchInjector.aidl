package vendor.lineage.touchinjector;

import vendor.lineage.touchinjector.TouchPosition;
import vendor.lineage.touchinjector.ITouchInjectorClient;

@VintfStability
interface ITouchInjector {
    void registerClient(in ITouchInjectorClient client);
    void unregisterClient(in ITouchInjectorClient client);

    void beginTouch(in ITouchInjectorClient client, in int slotId, in TouchPosition position);
    void endTouch(in ITouchInjectorClient client, in int slotId);
}
