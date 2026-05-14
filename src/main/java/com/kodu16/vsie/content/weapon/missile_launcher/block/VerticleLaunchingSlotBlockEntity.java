package com.kodu16.vsie.content.weapon.missile_launcher.block;

import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class VerticleLaunchingSlotBlockEntity extends AbstractWeaponBlockEntity {
    private static final String COOLDOWN_TAG = "CooldownTicks";
    private static final String CORE_ACTIVE_TAG = "LinkedCoreActive";
    private static final String CAP_OPEN_TAG = "CapOpen";
    private static final int SLOT_COOLDOWN_TICKS = 100;
    private static final int PRE_OPEN_COOLDOWN_WINDOW = 20;
    private static final int CLIENT_ANIMATION_RETRY_TICKS = 8;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation OPEN_CAP_ANIMATION = RawAnimation.begin().then("opencap", Animation.LoopType.HOLD_ON_LAST_FRAME);
    private static final RawAnimation CLOSE_CAP_ANIMATION = RawAnimation.begin().then("closecap", Animation.LoopType.HOLD_ON_LAST_FRAME);

    private int cooldownTicks = 0;
    private boolean linkedCoreActive = false;
    private boolean capOpen = false;
    private Boolean pendingClientCapAnimation = null;
    private int pendingClientCapAnimationTicks = 0;

    public VerticleLaunchingSlotBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        this.currentTick = SLOT_COOLDOWN_TICKS;
    }

    public void tick() {
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            playPendingClientCapAnimation();
            return;
        }
        if (cooldownTicks > 0) {
            cooldownTicks--;
            currentTick = SLOT_COOLDOWN_TICKS - cooldownTicks;
        } else {
            currentTick = SLOT_COOLDOWN_TICKS;
        }
        updateCapForCurrentState();
    }

    public boolean canLaunch() {
        return cooldownTicks <= 0;
    }

    public void markLaunched() {
        // Function: each slot owns its cooldown so the core can skip only busy cells.
        cooldownTicks = SLOT_COOLDOWN_TICKS;
        currentTick = 0;
        updateCapForCurrentState();
        setChanged();
        sendData();
    }

    public void setLinkedCoreActive(boolean active) {
        if (this.linkedCoreActive == active) {
            updateCapForCurrentState();
            return;
        }
        // Function: the core broadcasts channel-active state; slot animation is decided locally from that plus cooldown.
        this.linkedCoreActive = active;
        updateCapForCurrentState();
        setChanged();
        sendData();
    }

    private void updateCapForCurrentState() {
        boolean shouldOpen = linkedCoreActive && cooldownTicks < PRE_OPEN_COOLDOWN_WINDOW;
        //LogUtils.getLogger().warn("shouldopen:"+shouldOpen+"capopen:"+capOpen);
        if (shouldOpen && !capOpen) {
            LogUtils.getLogger().warn("opening cap");
            capOpen = true;
            if(!level.isClientSide){
                triggerCapAnimation(true);
            }

            setChanged();
            sendData();
        } else if (!shouldOpen && capOpen) {
            LogUtils.getLogger().warn("closing cap");
            capOpen = false;
            if(!level.isClientSide){
                triggerCapAnimation(false);
            }

            setChanged();
            sendData();
        }
    }

    @Override
    public float getmaxrange() {
        return 0;
    }

    @Override
    public int getcooldown() {
        return SLOT_COOLDOWN_TICKS;
    }

    @Override
    public String getweapontype() {
        return "verticle_launching_slot";
    }

    @Override
    public void fire() {
        // Function: launch slots are passive linked endpoints; missile spawning belongs to the core.
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Vertical Launching Slot");
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt(COOLDOWN_TAG, cooldownTicks);
        tag.putBoolean(CORE_ACTIVE_TAG, linkedCoreActive);
        tag.putBoolean(CAP_OPEN_TAG, capOpen);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        boolean previousCapOpen = capOpen;
        if (tag.contains(COOLDOWN_TAG, Tag.TAG_INT)) {
            cooldownTicks = Math.max(0, tag.getInt(COOLDOWN_TAG));
        }
        if (tag.contains(CORE_ACTIVE_TAG)) {
            linkedCoreActive = tag.getBoolean(CORE_ACTIVE_TAG);
        }
        if (tag.contains(CAP_OPEN_TAG)) {
            capOpen = tag.getBoolean(CAP_OPEN_TAG);
        }
        currentTick = SLOT_COOLDOWN_TICKS - cooldownTicks;
        if (clientPacket && level != null && level.isClientSide && previousCapOpen != capOpen) {
            // Function: client render instances may miss server-side triggerAnim packets, so replay from synced cap state.
            queueClientCapAnimation(capOpen);
        }
    }

    private void playPendingClientCapAnimation() {
        if (pendingClientCapAnimation == null) {
            return;
        }
        triggerCapAnimation(pendingClientCapAnimation);
        pendingClientCapAnimationTicks--;
        if (pendingClientCapAnimationTicks <= 0) {
            pendingClientCapAnimation = null;
        }
    }

    private void queueClientCapAnimation(boolean open) {
        // Function: retry briefly because GeckoLib drops triggers fired before the first model render.
        pendingClientCapAnimation = open;
        pendingClientCapAnimationTicks = CLIENT_ANIMATION_RETRY_TICKS;
    }

    private void triggerCapAnimation(boolean open) {
        triggerAnim("controller", open ? "opencap" : "closecap");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> PlayState.CONTINUE)
                .triggerableAnim("opencap", OPEN_CAP_ANIMATION)
                .triggerableAnim("closecap", CLOSE_CAP_ANIMATION));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
