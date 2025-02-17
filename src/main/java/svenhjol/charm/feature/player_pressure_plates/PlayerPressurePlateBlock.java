package svenhjol.charm.feature.player_pressure_plates;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;
import svenhjol.charmony.base.CharmBlockItem;
import svenhjol.charmony.base.CharmFeature;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Supplier;

public class PlayerPressurePlateBlock extends PressurePlateBlock {
    public PlayerPressurePlateBlock(CharmFeature feature) {
        super(Sensitivity.MOBS, Properties.of()
            .requiresCorrectToolForDrops()
            .noCollission()
            .mapColor(MapColor.COLOR_BLACK)
            .strength(2F, 1200F),
            BlockSetType.STONE);
    }

    @Override
    protected int getSignalStrength(@Nonnull Level level, @Nonnull BlockPos pos) {
        net.minecraft.world.phys.AABB bounds = TOUCH_AABB.move(pos);
        List<? extends Entity> entities = level.getEntitiesOfClass(Player.class, bounds);
        return !entities.isEmpty() ? 15 : 0;
    }

    public static class BlockItem extends CharmBlockItem {
        public <T extends Block> BlockItem(CharmFeature feature, Supplier<T> block) {
            super(feature, block, new Properties());
        }
    }
}
