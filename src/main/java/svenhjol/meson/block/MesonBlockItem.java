package svenhjol.meson.block;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;

public class MesonBlockItem extends BlockItem {
    private int burnTime;

    public MesonBlockItem(Block blockIn, Properties builder) {
        super(blockIn, builder);
    }

    public void setBurnTime(int burnTime) {
        this.burnTime = burnTime;
    }

    @Override
    public int getBurnTime(ItemStack itemStack) {
        return this.burnTime;
    }
}
