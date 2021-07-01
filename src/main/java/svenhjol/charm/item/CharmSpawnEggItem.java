package svenhjol.charm.item;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import svenhjol.charm.loader.CommonModule;

public class CharmSpawnEggItem extends SpawnEggItem implements ICharmItem {
    private final CommonModule module;

    public CharmSpawnEggItem(CommonModule module, String name, EntityType<? extends Mob> type, int primaryColor, int secondaryColor, Properties settings) {
        super(type, primaryColor, secondaryColor, settings);
        this.module = module;
        this.register(module, name);
    }

    public CharmSpawnEggItem(CommonModule module, String name, EntityType<? extends Mob> type, int primaryColor, int secondaryColor) {
        this(module, name, type, primaryColor, secondaryColor, (new Item.Properties()).tab(CreativeModeTab.TAB_MISC));
    }

    @Override
    public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> stacks) {
        if (enabled())
            super.fillItemCategory(group, stacks);
    }

    @Override
    public boolean enabled() {
        return module.isEnabled();
    }
}
