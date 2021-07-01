package svenhjol.charm.module.variant_ladders;

import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;
import svenhjol.charm.loader.ClientModule;

@svenhjol.charm.annotation.ClientModule(module = VariantLadders.class)
public class VariantLaddersClient extends ClientModule {
    @Override
    public void register() {
        VariantLadders.LADDER_BLOCKS.values().forEach(ladder -> BlockRenderLayerMap.INSTANCE.putBlock(ladder, RenderType.cutout()));
    }
}
