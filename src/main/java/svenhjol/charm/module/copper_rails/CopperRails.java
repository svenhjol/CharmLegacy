package svenhjol.charm.module.copper_rails;

import svenhjol.charm.Charm;
import svenhjol.charm.annotation.Module;
import svenhjol.charm.loader.CommonModule;

@Module(mod = Charm.MOD_ID, description = "Copper ingots can be used to craft rails.")
public class CopperRails extends CommonModule {
    public static svenhjol.charm.module.copper_rails.CopperRailBlock COPPER_RAIL;

    @Override
    public void register() {
        COPPER_RAIL = new CopperRailBlock(this);
    }
}
