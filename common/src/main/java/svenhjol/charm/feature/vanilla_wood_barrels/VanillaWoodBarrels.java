package svenhjol.charm.feature.vanilla_wood_barrels;

import svenhjol.charm.Charm;
import svenhjol.charm.feature.variant_barrels.VariantBarrels;
import svenhjol.charm_core.annotation.Feature;
import svenhjol.charm_core.base.CharmFeature;
import svenhjol.charm_core.enums.VanillaWood;

@Feature(mod = Charm.MOD_ID, description = "Barrels in all vanilla wood types.")
public class VanillaWoodBarrels extends CharmFeature {
    @Override
    public void register() {
        for (var material : VanillaWood.getTypes()) {
            VariantBarrels.registerBarrel(material);
        }
    }
}
