package com.kodu16.vsie.content.weapon.missile_launcher;

import com.kodu16.vsie.content.missile.entity.BasicMissileEntity;
import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.content.weapon.WeaponData;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.registries.vsieEntities;
import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractMissileLauncherBlockEntity extends AbstractWeaponBlockEntity {
    private boolean pendingLaunch = false;
    private int pendingLaunchChannel = 0;

    public AbstractMissileLauncherBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        this.weaponData = new WeaponData();
        this.hasInitialized = true;
    }

    @Override
    public abstract float getmaxrange();

    @Override
    public abstract int getcooldown();

    @Override
    public abstract String getweapontype();

    @Override
    public void receivechannel(int encode) {
        int previous = getData().receivingchannel;
        super.receivechannel(encode);
        if (previous == 0 && encode != 0) {
            // Function: a left-click press queues exactly one missile launch instead of continuous fire.
            this.pendingLaunch = true;
            this.pendingLaunchChannel = encode;
        }
        if (encode == 0) {
            getData().isfiring = false;
        }
    }

    @Override
    public void tick() {
        // Function: missile launchers use click-triggered fire while still respecting cooldown.
        tickFireCooldown(canFireForChannel(getData().receivingchannel));
        if (!pendingLaunch) {
            getData().isfiring = false;
            return;
        }
        if (!canFireForChannel(pendingLaunchChannel)) {
            pendingLaunch = false;
            getData().isfiring = false;
            return;
        }
        if (!isFireCooldownReady()) {
            return;
        }

        consumeFireCooldown();
        pendingLaunch = false;
        Level level = this.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        if (hasInitialized) {
            getData().isfiring = true;
            fire();
        }
    }

    @Override
    public void fire() {
        Level level = this.getLevel();
        if (getData().targetship == null || level == null || level.isClientSide()) {
            LogUtils.getLogger().warn("target is empty");
            return;
        }
        if (!hasMissileAmmo()) {
            return;
        }

        SubLevel target = getData().targetship;
        Vec3 targetPoint = ServerShipUtils.getStructureCenterWorld(target);
        if (targetPoint == null) {
            LogUtils.getLogger().warn("missile target has no valid structure center");
            return;
        }

        Vec3 spawnpos = getMissileSpawnPosition(level);
        BasicMissileEntity missile = new BasicMissileEntity(vsieEntities.BASIC_MISSILE.get(), level);
        missile.setTarget(target);
        missile.setPos(spawnpos);
        missile.setInitialDirection(getMissileLaunchDirection(level));

        if (level.addFreshEntity(missile) && consumeMissileAmmo()) {
            LogUtils.getLogger().warn("firing missile at:" + getData().targetship + " from:" + spawnpos);
        } else {
            missile.discard();
        }
    }

    private Vec3 getMissileSpawnPosition(Level level) {
        Vec3 spawnpos = getMissileCenterWorld(level);
        Vec3 worldForward = getMissileLaunchDirection(level);
        if (worldForward.lengthSqr() > 1.0E-6D) {
            spawnpos = spawnpos.add(worldForward.normalize().scale(1.25D));
        }
        return spawnpos;
    }

    private Vec3 getMissileCenterWorld(Level level) {
        Vec3 localCenter = Vec3.atCenterOf(this.getBlockPos());
        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, this.getBlockPos());
        if (subLevel != null) {
            // Function: convert the launcher block center from sublevel coordinates to world coordinates.
            return subLevel.logicalPose().transformPosition(localCenter);
        }
        return localCenter;
    }

    private Vec3 getMissileLaunchDirection(Level level) {
        BlockState state = this.getBlockState();
        Direction facing = state.hasProperty(BlockStateProperties.FACING)
                ? state.getValue(BlockStateProperties.FACING)
                : Direction.NORTH;
        Vec3 localForward = Vec3.atLowerCornerOf(facing.getNormal());
        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, this.getBlockPos());
        Vec3 worldForward = localForward;
        if (subLevel != null) {
            // Function: convert the launcher facing direction from sublevel coordinates to world coordinates.
            worldForward = subLevel.logicalPose().transformNormal(localForward);
        }
        return worldForward.lengthSqr() > 1.0E-6D ? worldForward.normalize() : Vec3.ZERO;
    }

    private boolean canFireForChannel(int encode) {
        boolean channel1 = (encode & 1) != 0 && getData().channel1;
        boolean channel2 = (encode & 2) != 0 && getData().channel2;
        boolean channel3 = (encode & 4) != 0 && getData().channel3;
        boolean channel4 = (encode & 8) != 0 && getData().channel4;
        return channel1 || channel2 || channel3 || channel4;
    }

    @Override
    public Component getDisplayName() {
        return null;
    }

    protected boolean consumeMissileAmmo() {
        // Function: launchers without an inventory keep the base missile spawning behavior.
        return true;
    }

    protected boolean hasMissileAmmo() {
        // Function: subclasses with internal ammo storage can veto spawning before an entity is created.
        return true;
    }

    public abstract String getmissilelaunchertype();
}
