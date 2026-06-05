package com.kodu16.vsie.content.weapon.redstone_relay;

import com.kodu16.vsie.content.cooldown.FireCooldown;
import com.kodu16.vsie.content.weapon.AbstractWeaponBlock;
import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RedstoneRelayBlockEntity extends AbstractWeaponBlockEntity {
    private static final String CUSTOM_DISPLAY_NAME_TAG = "CustomHudDisplayName";
    private static final String DEFAULT_SHORT_NAME = "RLY";
    private static final int MAX_SHORT_NAME_LENGTH = 12;

    private String customHudDisplayName = "";

    public RedstoneRelayBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        syncRedstoneOutputState();
    }

    @Override
    public float getmaxrange() {
        // Function: the relay has no projectile or raycast behaviour, so its working range is irrelevant.
        return 0;
    }

    @Override
    public int getcooldown() {
        return 0;
    }

    @Override
    public boolean isEnergyWeapon() {
        return true;
    }

    @Override
    public Item getAmmoItem() {
        return null;
    }

    @Override
    public FireCooldown getFireCooldown() {
        // Function: holding fire should keep the redstone output asserted every tick without heat buildup.
        return FireCooldown.cool1(0);
    }

    @Override
    public boolean supportsBlockDestructionToggle() {
        return false;
    }

    @Override
    public int getCooldownHudValue() {
        return 0;
    }

    @Override
    public int getCooldownHudMax() {
        return 0;
    }

    @Override
    public void fire() {
        // Function: the actual redstone output is synchronized from isfiring in tick() so all six faces stay live while held.
    }

    @Override
    public Component getDisplayName() {
        return Component.literal(getResolvedHudDisplayName());
    }

    @Override
    public String getweapontype() {
        return "redstone_relay";
    }

    public String getCustomHudDisplayName() {
        return customHudDisplayName;
    }

    public String getResolvedHudDisplayName() {
        return customHudDisplayName.isBlank() ? DEFAULT_SHORT_NAME : customHudDisplayName;
    }

    public void setCustomHudDisplayName(String displayName) {
        String sanitized = sanitizeDisplayName(displayName);
        if (sanitized.equals(this.customHudDisplayName)) {
            return;
        }
        this.customHudDisplayName = sanitized;
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            markUpdated();
        }
    }

    private String sanitizeDisplayName(String displayName) {
        if (displayName == null) {
            return "";
        }
        String trimmed = displayName.trim();
        if (trimmed.length() > MAX_SHORT_NAME_LENGTH) {
            trimmed = trimmed.substring(0, MAX_SHORT_NAME_LENGTH);
        }
        return trimmed;
    }

    private void syncRedstoneOutputState() {
        Level level = this.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = this.getBlockState();
        int targetPower = getData().isfiring ? 15 : 0;
        if (state.getValue(AbstractWeaponBlock.POWER) == targetPower) {
            return;
        }

        BlockState updatedState = state.setValue(AbstractWeaponBlock.POWER, targetPower);
        // Function: only flip the blockstate on output transitions so redstone neighbours update without per-tick spam.
        level.setBlock(this.worldPosition, updatedState, Block.UPDATE_CLIENTS);
        level.updateNeighborsAt(this.worldPosition, updatedState.getBlock());
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
            level.updateNeighborsAt(this.worldPosition.relative(direction), updatedState.getBlock());
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putString(CUSTOM_DISPLAY_NAME_TAG, this.customHudDisplayName);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains(CUSTOM_DISPLAY_NAME_TAG)) {
            this.customHudDisplayName = sanitizeDisplayName(tag.getString(CUSTOM_DISPLAY_NAME_TAG));
        }
    }
}
