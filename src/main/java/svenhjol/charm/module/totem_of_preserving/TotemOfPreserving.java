package svenhjol.charm.module.totem_of_preserving;

import com.google.common.collect.ImmutableList;
import net.fabricmc.fabric.api.loot.v1.FabricLootPoolBuilder;
import net.fabricmc.fabric.api.loot.v1.FabricLootSupplierBuilder;
import net.fabricmc.fabric.api.loot.v1.event.LootTableLoadingCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import svenhjol.charm.Charm;
import svenhjol.charm.annotation.CommonModule;
import svenhjol.charm.annotation.Config;
import svenhjol.charm.api.event.TotemOfPreservingEvents;
import svenhjol.charm.event.EntityDropXpCallback;
import svenhjol.charm.event.PlayerDropInventoryCallback;
import svenhjol.charm.helper.ItemHelper;
import svenhjol.charm.helper.LogHelper;
import svenhjol.charm.init.CharmAdvancements;
import svenhjol.charm.loader.CharmModule;
import svenhjol.charm.module.totem_works_from_inventory.TotemWorksFromInventory;
import svenhjol.charm.registry.CommonRegistry;

import java.util.*;

@CommonModule(mod = Charm.MOD_ID, description = "The player's inventory items will be held in the Totem of Preserving upon death.\n" +
    "By default, a new totem will always be spawned to hold items upon dying in Easy or Peaceful mode ('Grave mode').\n" +
    "In Normal and Hard mode, the player must be holding an empty Totem of Preserving in order for it to hold items upon death.")
public class TotemOfPreserving extends CharmModule {
    public static TotemOfPreservingItem TOTEM_OF_PRESERVING;
    public static LootItemFunctionType LOOT_FUNCTION;

    public static final ResourceLocation LOOT_ID = new ResourceLocation(Charm.MOD_ID, "totem_of_preserving_loot");
    public static final ResourceLocation TRIGGER_USED_TOTEM_OF_PRESERVING = new ResourceLocation(Charm.MOD_ID, "used_totem_of_preserving");

    public static List<Difficulty> GRAVE_MODE_DIFFICULTIES = new ArrayList<>();
    public static List<ResourceLocation> VALID_LOOT_TABLES = new ArrayList<>();

    @Config(name = "Grave mode game difficulties", description = "A list of game difficulties in which totems will behave in 'Grave mode'.\n" +
        "In Grave mode, a totem will be dropped on death even if the player doesn't have an empty totem in their inventory.")
    public static List<String> configGraveModeDifficulties = Arrays.asList(
        "peaceful", "easy"
    );

    @Config(name = "Add totems to loot", description = "Loot tables that totems of preserving will be added to.\n" +
        "This does not apply if Grave mode is active for the current game difficulty. See 'Grave mode game difficulties' to configure this.")
    public static List<String> configLootTables = Arrays.asList(
        "entities/witch", "chests/pillager_outpost", "chests/woodland_mansion"
    );

    @Config(name = "Preserve XP", description = "If true, the totem will preserve the player's experience and restore when broken.")
    public static boolean preserveXp = false;

    @Config(name = "Show death position", description = "If true, the coordinates where you died will be added to the player's chat screen.")
    public static boolean showDeathPosition = false;

    @Override
    public void register() {
        TOTEM_OF_PRESERVING = new TotemOfPreservingItem(this);

        configGraveModeDifficulties.stream().map(String::toLowerCase).map(Difficulty::byName).filter(Objects::nonNull).forEach(d -> GRAVE_MODE_DIFFICULTIES.add(d));
        configLootTables.stream().map(ResourceLocation::new).forEach(l -> VALID_LOOT_TABLES.add(l));
    }

    @Override
    public void runWhenEnabled() {
        ItemHelper.ITEM_LIFETIME.put(TOTEM_OF_PRESERVING, Integer.MAX_VALUE); // probably stupid

        // register loot function
        LOOT_FUNCTION = CommonRegistry.lootFunctionType(LOOT_ID, new LootItemFunctionType(new TotemOfPreservingLootFunction.Serializer()));

        PlayerDropInventoryCallback.EVENT.register(this::handleDropInventory);
        EntityDropXpCallback.BEFORE.register(this::handleDropXp);
        LootTableLoadingCallback.EVENT.register(this::handleLootTables);
    }

    private void handleLootTables(ResourceManager manager, LootTables lootTables, ResourceLocation id, FabricLootSupplierBuilder supplier, LootTableLoadingCallback.LootTableSetter setter) {
        if (VALID_LOOT_TABLES.contains(id)) {
            FabricLootPoolBuilder builder = FabricLootPoolBuilder.builder()
                .rolls(ConstantValue.exactly(1))
                .with(LootItem.lootTableItem(Items.AIR)
                    .setWeight(1)
                    .apply(() -> new TotemOfPreservingLootFunction(new LootItemCondition[0])));

            supplier.withPool(builder);
        }
    }

    public InteractionResult handleDropXp(LivingEntity entity) {
        if (!preserveXp || !(entity instanceof Player) || entity.level.isClientSide) {
            return InteractionResult.PASS;
        }

        return InteractionResult.SUCCESS;
    }

    public InteractionResult handleDropInventory(Player player, Inventory inventory) {
        if (player.level.isClientSide) {
            return InteractionResult.PASS;
        }

        ServerLevel serverLevel = (ServerLevel)player.level;
        ServerPlayer serverPlayer = (ServerPlayer)player;
        Random random = serverLevel.getRandom();
        ItemStack totem = new ItemStack(TOTEM_OF_PRESERVING);
        CompoundTag serialized = new CompoundTag();
        List<ItemStack> holdable = new ArrayList<>();
        List<ItemStack> totemsToSpawn = new ArrayList<>();

        List<NonNullList<ItemStack>> combinedInventory = ImmutableList.of(inventory.items, inventory.armor, inventory.offhand);
        combinedInventory.forEach(list -> list.stream().filter(Objects::nonNull).filter(stack -> !stack.isEmpty()).forEach(holdable::add));

        boolean totemWorksFromInventory = Charm.LOADER.isEnabled(TotemWorksFromInventory.class);
        boolean graveMode = isGraveMode(player.level.getDifficulty());
        boolean foundEmptyTotem = false;

        if (!graveMode && !totemWorksFromInventory) {
            // check if player is holding an empty totem - if not, exit early
            boolean holding = false;
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack held = player.getItemInHand(hand);
                if (held.getItem() == TOTEM_OF_PRESERVING && !TotemOfPreservingItem.hasItems(totem)) {
                    holding = true;
                    break;
                }
            }

            if (!holding) {
                LogHelper.debug(getClass(), "No empty totem in hands and (graveMode = false && totemWorksFromInventory = false), skipping");
                return InteractionResult.PASS;
            }
        }

        // get all inventories and store them in the totem
        for (int i = 0; i < holdable.size(); i++) {
            ItemStack stack = holdable.get(i);

            if (stack.getItem() == TOTEM_OF_PRESERVING) {
                if (!TotemOfPreservingItem.getItems(stack).isEmpty()) {
                    // if there's already a filled totem in the inventory, spawn this separately
                    LogHelper.debug(getClass(), "A filled totem was found, spawning this separately");
                    totemsToSpawn.add(stack);
                    continue;
                } else if (!graveMode && !foundEmptyTotem) {
                    // if not in grave mode and an empty totem is found, use this to add items to
                    LogHelper.debug(getClass(), "An empty totem was found and (graveMode = false), going to try and add items to this totem");
                    foundEmptyTotem = true;
                    continue;
                }
            }

            InteractionResult result = TotemOfPreservingEvents.BEFORE_ADD_STACK.invoker().invoke(serverPlayer, stack);
            if (result == InteractionResult.FAIL) continue;

            serialized.put(Integer.toString(i), holdable.get(i).save(new CompoundTag()));
        }

        if (graveMode || foundEmptyTotem) {
            if (preserveXp) {
                int xp = serverPlayer.totalExperience;
                LogHelper.debug(getClass(), "Preserving player XP in totem: " + xp);
                TotemOfPreservingItem.setXp(totem, xp);
            }
            TotemOfPreservingItem.setItems(totem, serialized);
            TotemOfPreservingItem.setMessage(totem, serverPlayer.getScoreboardName());

            if (!TotemOfPreservingItem.getItems(totem).isEmpty()) {
                LogHelper.debug(getClass(), "Inventory has items so they will be saved in the totem");
                totemsToSpawn.add(totem);
            }
        }

        if (totemsToSpawn.isEmpty()) {
            LogHelper.debug(getClass(), "No totems to spawn, skipping");
            return InteractionResult.PASS;
        }

        BlockPos playerPos = serverPlayer.blockPosition();
        Entity vehicle = serverPlayer.getVehicle();
        double x, y, z;

        if (vehicle != null) {
            x = vehicle.getX() + 0.25D;
            y = vehicle.getY() + 0.75D;
            z = vehicle.getZ() + 0.25D;
        } else {
            x = playerPos.getX() + 0.25D;
            y = playerPos.getY() + 0.75D;
            z = playerPos.getZ() + 0.25D;
        }

        if (y < serverLevel.getMinBuildHeight()) {
            y = serverLevel.getSeaLevel(); // fetching your totem from the void is sad
        }

        // spawn totems
        for (ItemStack stack : totemsToSpawn) {
            double tx = x + random.nextFloat() * 0.25D;
            double ty = y + 0.25D;
            double tz = z + random.nextFloat() * 0.25D;
            InteractionResult result = TotemOfPreservingEvents.BEFORE_CREATE.invoker().invoke(serverPlayer, new BlockPos(tx, ty, tz), stack);
            if (result == InteractionResult.FAIL) continue;

            ItemEntity totemEntity = new ItemEntity(serverLevel, x, y, z, stack);
            totemEntity.setNoGravity(true);
            totemEntity.setDeltaMovement(0, 0, 0);
            totemEntity.setPosRaw(tx, ty, tz);
            totemEntity.setExtendedLifetime();
            totemEntity.setGlowingTag(true);
            totemEntity.setInvulnerable(true);

            serverLevel.addFreshEntity(totemEntity);
            TotemOfPreservingEvents.AFTER_CREATE.invoker().invoke(serverPlayer, totemEntity.blockPosition(), stack);
        }

        BlockPos deathPos = new BlockPos(x, y, z);
        triggerUsedTotemOfPreserving((ServerPlayer) player);
        LogHelper.info(getClass(), "Spawned a totem at pos: " + deathPos);

        // clear player's inventory
        for (NonNullList<ItemStack> inv : combinedInventory) {
            for (int i = 0; i < inv.size(); i++) {
                inv.set(i, ItemStack.EMPTY);
            }
        }

        if (showDeathPosition) {
            player.displayClientMessage(new TranslatableComponent("gui.charm.totem_of_preserving.deathpos", x, y, z), false);
        }

        return InteractionResult.SUCCESS;
    }

    public static boolean isGraveMode(Difficulty difficulty) {
        return GRAVE_MODE_DIFFICULTIES.contains(difficulty);
    }

    public static void triggerUsedTotemOfPreserving(ServerPlayer player) {
        CharmAdvancements.ACTION_PERFORMED.trigger(player, TRIGGER_USED_TOTEM_OF_PRESERVING);
    }
}
