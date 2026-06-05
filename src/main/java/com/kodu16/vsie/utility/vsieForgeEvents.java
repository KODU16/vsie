package com.kodu16.vsie.utility;

import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.network.fuel.SyncThrusterFuelsPacket;
import com.kodu16.vsie.registries.ModNetworking;
import com.kodu16.vsie.registries.fuel.ThrusterFuelManager;
import com.kodu16.vsie.registries.vsieItems;
import com.kodu16.vsie.vsie;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = vsie.ID, bus = EventBusSubscriber.Bus.GAME)
public class vsieForgeEvents {
    private static final double GROUND_PROBE_DEPTH = 0.0625D;
    private static final double GROUND_PROBE_EPSILON = 1.0E-4D;

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ThrusterFuelManager());
    }

    // Function: sync thruster fuel data for client HUDs and particle rendering when a player logs in.
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModNetworking.sendToPlayer(SyncThrusterFuelsPacket.create(ThrusterFuelManager.getFuelPropertiesMap()), player);
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!entity.getType().is(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS)) {
            return;
        }
        if (!isInsideAnySubLevelBounds(entity, serverLevel)) {
            return;
        }
        if (isTouchingWorldGround(entity, serverLevel)) {
            return;
        }

        int count = 1 + getLootingBonus(event.getSource());
        ItemStack drop = new ItemStack(vsieItems.SOLID_E710.get(), count);
        AABB box = entity.getBoundingBox();
        event.getDrops().add(new ItemEntity(serverLevel, entity.getX(), box.minY + 0.1D, entity.getZ(), drop));
    }

    // Function: require the dying mob to occupy the active world-space bounds of at least one sublevel.
    private static boolean isInsideAnySubLevelBounds(LivingEntity entity, ServerLevel level) {
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return false;
        }

        AABB entityBox = entity.getBoundingBox();
        double centerX = entityBox.getCenter().x;
        double centerY = entityBox.getCenter().y;
        double centerZ = entityBox.getCenter().z;

        for (ServerSubLevel subLevel : container.getAllSubLevels()) {
            if (subLevel == null || subLevel.isRemoved()) {
                continue;
            }
            var bounds = subLevel.boundingBox();
            if (!entityBox.intersects(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ())) {
                continue;
            }
            if (centerX >= bounds.minX() && centerX <= bounds.maxX()
                    && centerY >= bounds.minY() && centerY <= bounds.maxY()
                    && centerZ >= bounds.minZ() && centerZ <= bounds.maxZ()) {
                return true;
            }
        }
        return false;
    }

    // Function: world blocks count as ground, but sublevel decks still qualify as airborne for this drop rule.
    private static boolean isTouchingWorldGround(LivingEntity entity, ServerLevel level) {
        AABB entityBox = entity.getBoundingBox();
        AABB probeBox = new AABB(
                entityBox.minX + GROUND_PROBE_EPSILON,
                entityBox.minY - GROUND_PROBE_DEPTH,
                entityBox.minZ + GROUND_PROBE_EPSILON,
                entityBox.maxX - GROUND_PROBE_EPSILON,
                entityBox.minY + GROUND_PROBE_EPSILON,
                entityBox.maxZ - GROUND_PROBE_EPSILON
        );
        CollisionContext collisionContext = CollisionContext.of(entity);

        int minX = (int) Math.floor(probeBox.minX);
        int minY = (int) Math.floor(probeBox.minY);
        int minZ = (int) Math.floor(probeBox.minZ);
        int maxX = (int) Math.floor(probeBox.maxX);
        int maxY = (int) Math.floor(probeBox.maxY);
        int maxZ = (int) Math.floor(probeBox.maxZ);

        for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            boolean intersectsProbe = state.getCollisionShape(level, pos, collisionContext).toAabbs().stream()
                    .map(box -> box.move(pos))
                    .anyMatch(box -> box.intersects(probeBox));
            if (!intersectsProbe) {
                continue;
            }
            if (ServerShipUtils.getSubLevelAtBlockPos(level, pos) == null) {
                return true;
            }
        }
        return false;
    }

    private static int getLootingBonus(DamageSource source) {
        if (source.getEntity() instanceof LivingEntity attacker && attacker.level() instanceof ServerLevel serverLevel) {
            var enchantments = serverLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            return Math.max(0, EnchantmentHelper.getEnchantmentLevel(enchantments.getOrThrow(Enchantments.LOOTING), attacker));
        }
        return 0;
    }
}
