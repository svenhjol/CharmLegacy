package svenhjol.charm.feature.no_spyglass_scope;

import svenhjol.charm.CharmClient;
import svenhjol.charmony.annotation.ClientFeature;
import svenhjol.charmony.base.CharmFeature;

@ClientFeature(
    mod = CharmClient.MOD_ID,
    description = "Removes the dark outer overlay when zooming in with the spyglass, giving you the full view."
)
public class NoSpyglassScope extends CharmFeature {
    public static boolean shouldRemoveHud() {
        return CharmClient.instance().loader().isEnabled(NoSpyglassScope.class);
    }
}
