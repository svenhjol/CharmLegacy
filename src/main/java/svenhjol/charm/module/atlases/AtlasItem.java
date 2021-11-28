package svenhjol.charm.module.atlases;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import svenhjol.charm.item.CharmItem;
import svenhjol.charm.loader.CharmModule;

public class AtlasItem extends CharmItem {

    public AtlasItem(CharmModule module) {
        super(module, "atlas", new Properties()
            .tab(CreativeModeTab.TAB_MISC)
            .stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.consume(itemStack);
        }
        if (hand == InteractionHand.OFF_HAND && !Atlases.offHandOpen) {
            return InteractionResultHolder.pass(itemStack);
        }
        AtlasInventory inventory = Atlases.getInventory(level, itemStack);
        inventory.getCurrentDimensionMapInfos(level).values().forEach(it -> Atlases.sendMapToClient((ServerPlayer) player, it.map, true));
        player.openMenu(inventory);
        return InteractionResultHolder.consume(itemStack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockState blockstate = level.getBlockState(context.getClickedPos());
        if (blockstate.is(BlockTags.BANNERS)) {
            if (!level.isClientSide) {
                Player player = context.getPlayer();
                if (player instanceof ServerPlayer) {
                    AtlasInventory inventory = Atlases.getInventory(level, context.getItemInHand());
                    MapItemSavedData mapdata = inventory.getActiveMap(level);
                    if (mapdata != null) {
                        mapdata.toggleBanner(level, context.getClickedPos());
                    }
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        } else {
            return super.useOn(context);
        }
    }
}
