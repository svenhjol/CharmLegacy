package svenhjol.charm.feature.coral_squids;

import com.google.common.collect.Maps;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.BaseCoralPlantTypeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import svenhjol.charm.Charm;
import svenhjol.charmony.helper.ItemNbtHelper;

import javax.annotation.Nullable;
import java.util.Map;

@SuppressWarnings("unused")
public class CoralSquidEntity extends WaterAnimal {
    public static final String CORAL_SQUID_TYPE_TAG = "CoralSquidType";
    public static final String CORAL_SQUID_FROM_BUCKET_TAG = "FromBucket";
    private static final EntityDataAccessor<Boolean> FROM_BUCKET = SynchedEntityData.defineId(CoralSquidEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> CORAL_SQUID_TYPE = SynchedEntityData.defineId(CoralSquidEntity.class, EntityDataSerializers.INT);;
    public static final Map<Integer, ResourceLocation> TEXTURES = Util.make(Maps.newHashMap(), map -> {
        map.put(0, new ResourceLocation(Charm.MOD_ID, "textures/entity/coral_squid/tube.png"));
        map.put(1, new ResourceLocation(Charm.MOD_ID, "textures/entity/coral_squid/brain.png"));
        map.put(2, new ResourceLocation(Charm.MOD_ID, "textures/entity/coral_squid/bubble.png"));
        map.put(3, new ResourceLocation(Charm.MOD_ID, "textures/entity/coral_squid/fire.png"));
        map.put(4, new ResourceLocation(Charm.MOD_ID, "textures/entity/coral_squid/horn.png"));
    });
    public static final Map<Integer, Item> DROPS = Util.make(Maps.newHashMap(), map -> {
        map.put(0, Items.TUBE_CORAL);
        map.put(1, Items.BRAIN_CORAL);
        map.put(2, Items.BUBBLE_CORAL);
        map.put(3, Items.FIRE_CORAL);
        map.put(4, Items.HORN_CORAL);
    });

    public float xBodyRot;
    public float xBodyRot0;
    public float zBodyRot;
    public float zBodyRot0;
    public float tentacleMovement;
    public float oldTentacleMovement;
    public float tentacleAngle;
    public float oldTentacleAndle;
    private float speed;
    private float tentacleSpeed;
    private float rotateSpeed;
    private float swimX;
    private float swimY;
    private float swimZ;

    public CoralSquidEntity(EntityType<? extends CoralSquidEntity> entityType, Level level) {
        super(entityType, level);
        this.random.setSeed(this.getId());
        this.tentacleSpeed = 1.0F / (this.random.nextFloat() + 1.0F) * 0.2F;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnReason, @Nullable SpawnGroupData entityData, @Nullable CompoundTag entityTag) {
        entityData = super.finalizeSpawn(level, difficulty, spawnReason, entityData, entityTag);
        setCoralSquidType(random.nextInt(5));
        return entityData;
    }

    @SuppressWarnings("deprecation")
    public static boolean canSpawn(EntityType<CoralSquidEntity> type, LevelAccessor level, MobSpawnType spawnReason, BlockPos pos, RandomSource random) {
        var coralBelow = false;

        for (int y = 0; y > -16; y--) {
            BlockPos downPos = pos.offset(0, y, 0);
            BlockState downState = level.getBlockState(downPos);
            coralBelow = downState.getBlock() instanceof BaseCoralPlantTypeBlock;

            if (coralBelow) break;
        }

        return pos.getY() > 20
            && pos.getY() < level.getSeaLevel()
            && coralBelow;
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 4; // might be important for performance
    }

    public ResourceLocation getTexture() {
        return TEXTURES.getOrDefault(getCoralSquidType(), TEXTURES.get(0));
    }

    public int getCoralSquidType() {
        return this.entityData.get(CORAL_SQUID_TYPE);
    }

    public void setCoralSquidType(int type) {
        if (type < 0 || type > 4) {
            type = this.random.nextInt(5);
        }

        this.entityData.set(CORAL_SQUID_TYPE, type);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CORAL_SQUID_TYPE, 1);
        this.entityData.define(FROM_BUCKET, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt(CORAL_SQUID_TYPE_TAG, this.getCoralSquidType());
        nbt.putBoolean(CORAL_SQUID_FROM_BUCKET_TAG, this.isFromBucket());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setCoralSquidType(nbt.getInt(CORAL_SQUID_TYPE_TAG));
        this.setFromBucket(nbt.getBoolean(CORAL_SQUID_FROM_BUCKET_TAG));
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(0, new SquidRandomMovementGoal(this));
        this.goalSelector.addGoal(1, new SquidFleeGoal());
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int lootingMultiplier, boolean allowDrops) {
        super.dropCustomDeathLoot(source, lootingMultiplier, allowDrops);
        if (random.nextFloat() < CoralSquids.dropChance) {
            this.spawnAtLocation(DROPS.get(getCoralSquidType()));
        }
    }

    public static AttributeSupplier.Builder createSquidAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 5.0D);
    }

    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height * 0.8F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SQUID_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SQUID_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SQUID_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    @Override
    public float getVoicePitch() {
        return 1.25F;
    }

    /**
     * Copypasta from {@link Squid#aiStep()}
     */
    public void aiStep() {
        super.aiStep();
        this.xBodyRot0 = this.xBodyRot;
        this.zBodyRot0 = this.zBodyRot;
        this.oldTentacleMovement = this.tentacleMovement;
        this.oldTentacleAndle = this.tentacleAngle;
        this.tentacleMovement += this.tentacleSpeed;

        if ((double)this.tentacleMovement > 6.283185307179586D) {
            if (this.level().isClientSide) {
                this.tentacleMovement = 6.2831855F;
            } else {
                this.tentacleMovement = (float)((double)this.tentacleMovement - 6.283185307179586D);
                if (this.random.nextInt(10) == 0) {
                    this.tentacleSpeed = 1.0F / (this.random.nextFloat() + 1.0F) * 0.2F;
                }

                this.level().broadcastEntityEvent(this, (byte)19);
            }
        }

        if (this.isInWaterOrBubble()) {
            if (this.tentacleMovement < 3.1415927F) {
                float f = this.tentacleMovement / 3.1415927F;
                this.tentacleAngle = Mth.sin(f * f * 3.1415927F) * 3.1415927F * 0.25F;
                if ((double)f > 0.75D) {
                    this.speed = 1.0F;
                    this.rotateSpeed = 1.0F;
                } else {
                    this.rotateSpeed *= 0.8F;
                }
            } else {
                this.tentacleAngle = 0.0F;
                this.speed *= 0.9F;
                this.rotateSpeed *= 0.99F;
            }

            if (!this.level().isClientSide) {
                this.setDeltaMovement((this.swimX * this.speed), (this.swimY * this.speed), (this.swimZ * this.speed));
            }

            Vec3 vec3d = this.getDeltaMovement();
            double d = vec3d.horizontalDistance();
            this.yBodyRot += (-((float)Mth.atan2(vec3d.x, vec3d.z)) * 57.295776F - this.yBodyRot) * 0.1F;
            this.setYRot(this.yBodyRot);
            this.zBodyRot = (float)((double)this.zBodyRot + 3.141592653589793D * (double)this.rotateSpeed * 1.5D);
            this.xBodyRot += (-((float)Mth.atan2(d, vec3d.y)) * 57.295776F - this.xBodyRot) * 0.1F;
        } else {
            this.tentacleAngle = Mth.abs(Mth.sin(this.tentacleMovement)) * 3.1415927F * 0.25F;
            if (!this.level().isClientSide) {
                double d = this.getDeltaMovement().y;
                if (this.hasEffect(MobEffects.LEVITATION)) {
                    var effect = this.getEffect(MobEffects.LEVITATION);
                    if (effect != null) {
                        d = 0.05D * (double) (effect.getAmplifier() + 1);
                    }
                } else if (!this.isNoGravity()) {
                    d -= 0.08D;
                }

                this.setDeltaMovement(0.0D, d * 0.981D, 0.0D);
            }

            this.xBodyRot = (float)((double)this.xBodyRot + (double)(-90.0F - this.xBodyRot) * 0.02D);
        }
    }

    public void travel(Vec3 movementInput) {
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    public void handleEntityEvent(byte status) {
        if (status == 19) {
            this.tentacleMovement = 0.0F;
        } else {
            super.handleEntityEvent(status);
        }
    }

    public void setMovementVector(float x, float y, float z) {
        this.swimX = x;
        this.swimY = y;
        this.swimZ = z;
    }

    public boolean hasMovementVector() {
        return this.swimX != 0.0F || this.swimY != 0.0F || this.swimZ != 0.0F;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        var held = player.getItemInHand(hand);
        if (held.getItem() == Items.WATER_BUCKET && this.isAlive()) {
            this.playSound(SoundEvents.BUCKET_FILL_FISH, 1.0F, 1.0F);
            held.shrink(1);

            var bucket = new ItemStack(CoralSquids.bucketItem.get());
            var nbt = new CompoundTag();
            ItemNbtHelper.setCompound(bucket, CoralSquidBucketItem.STORED_CORAL_SQUID, this.saveWithoutId(nbt));

            if (hasCustomName()) {
                bucket.setHoverName(this.getCustomName());
            }

            if (!level().isClientSide) {
                CriteriaTriggers.FILLED_BUCKET.trigger((ServerPlayer) player, bucket);
            }

            player.getInventory().placeItemBackInInventory(bucket);
            this.discard();

            return InteractionResult.sidedSuccess(level().isClientSide);
        } else {
            return super.mobInteract(player, hand);
        }
    }

    private boolean isFromBucket() {
        return this.entityData.get(FROM_BUCKET);
    }

    public void setFromBucket(boolean fromBucket) {
        this.entityData.set(FROM_BUCKET, fromBucket);
    }

    @Override
    public boolean requiresCustomPersistence() {
        return super.requiresCustomPersistence() || this.isFromBucket();
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return !this.isFromBucket() && !this.hasCustomName();
    }

    class SquidFleeGoal extends Goal {
        private int timer;

        private SquidFleeGoal() {
        }

        public boolean canUse() {
            var livingEntity = CoralSquidEntity.this.getLastHurtByMob();
            if (CoralSquidEntity.this.isInWater() && livingEntity != null) {
                return CoralSquidEntity.this.distanceToSqr(livingEntity) < 100.0D;
            } else {
                return false;
            }
        }

        public void start() {
            this.timer = 0;
        }

        public void tick() {
            ++this.timer;
            var livingEntity = CoralSquidEntity.this.getLastHurtByMob();
            if (livingEntity != null) {
                var vec3d = new Vec3(CoralSquidEntity.this.getX() - livingEntity.getX(), CoralSquidEntity.this.getY() - livingEntity.getY(), CoralSquidEntity.this.getZ() - livingEntity.getZ());
                var blockState = CoralSquidEntity.this.level().getBlockState(new BlockPos((int) (CoralSquidEntity.this.getX() + vec3d.x), (int) (CoralSquidEntity.this.getY() + vec3d.y), (int) (CoralSquidEntity.this.getZ() + vec3d.z)));
                var fluidState = CoralSquidEntity.this.level().getFluidState(new BlockPos((int) (CoralSquidEntity.this.getX() + vec3d.x), (int) (CoralSquidEntity.this.getY() + vec3d.y), (int) (CoralSquidEntity.this.getZ() + vec3d.z)));
                if (fluidState.is(FluidTags.WATER) || blockState.isAir()) {
                    double d = vec3d.length();
                    if (d > 0.0D) {
                        vec3d.normalize();
                        float f = 3.0F;
                        if (d > 5.0D) {
                            f = (float)((double)f - (d - 5.0D) / 5.0D);
                        }

                        if (f > 0.0F) {
                            vec3d = vec3d.scale(f);
                        }
                    }

                    if (blockState.isAir()) {
                        vec3d = vec3d.subtract(0.0D, vec3d.y, 0.0D);
                    }

                    CoralSquidEntity.this.setMovementVector((float)vec3d.x / 20.0F, (float)vec3d.y / 20.0F, (float)vec3d.z / 20.0F);
                }

                if (this.timer % 10 == 5) {
                    CoralSquidEntity.this.level().addParticle(ParticleTypes.BUBBLE, CoralSquidEntity.this.getX(), CoralSquidEntity.this.getY(), CoralSquidEntity.this.getZ(), 0.0D, 0.0D, 0.0D);
                }

            }
        }
    }

    private static class SquidRandomMovementGoal extends Goal {
        private final CoralSquidEntity squid;

        public SquidRandomMovementGoal(CoralSquidEntity squid) {
            this.squid = squid;
        }

        public boolean canUse() {
            return true;
        }

        public void tick() {
            var i = this.squid.getNoActionTime();
            if (i > 100) {
                this.squid.setMovementVector(0.0F, 0.0F, 0.0F);
            } else if (this.squid.getRandom().nextInt(30) == 0 || !this.squid.wasTouchingWater || !this.squid.hasMovementVector()) {
                var f = this.squid.getRandom().nextFloat() * 6.2831855F;
                var g = Mth.cos(f) * 0.2F;
                var h = -0.1F + this.squid.getRandom().nextFloat() * 0.05F;
                var j = Mth.sin(f) * 0.2F;
                this.squid.setMovementVector(g, h, j);
            }
        }
    }
}
