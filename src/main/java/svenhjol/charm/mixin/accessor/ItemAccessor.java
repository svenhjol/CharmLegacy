package svenhjol.charm.mixin.accessor;

import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import svenhjol.charm.annotation.CharmMixin;

@Mixin(Item.class)
@CharmMixin(required = true)
public interface ItemAccessor {
    @Accessor
    int getMaxCount();

    @Accessor
    void setMaxCount(int maxCount);

    @Accessor()
    void setTranslationKey(String key);
}
