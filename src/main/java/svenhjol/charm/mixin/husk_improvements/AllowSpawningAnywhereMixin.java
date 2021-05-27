package svenhjol.charm.mixin.husk_improvements;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.HuskEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ServerWorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import svenhjol.charm.module.husk_improvements.HuskImprovements;

import java.util.Random;

@Mixin(HuskEntity.class)
public abstract class AllowSpawningAnywhereMixin {

    /**
     * Defer to canSpawn. If the check is false, use the default husk spawn logic.
     */
    @Inject(
        method = "canSpawn",
        at = @At("RETURN"),
        cancellable = true
    )
    private static void hookCanSpawn(EntityType<HuskEntity> entity, ServerWorldAccess world, SpawnReason reason, BlockPos pos, Random rand, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && HuskImprovements.canSpawn())
            cir.setReturnValue(HuskEntity.canSpawnInDark(entity, world, reason, pos, rand));
    }
}
