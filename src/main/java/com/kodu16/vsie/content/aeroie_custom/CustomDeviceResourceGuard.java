package com.kodu16.vsie.content.aeroie_custom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** Validates external files referenced by saved or placed custom device definitions. */
public final class CustomDeviceResourceGuard {
    private static final double WARNING_RADIUS_SQR = 64.0D * 64.0D;

    private CustomDeviceResourceGuard() {
    }

    public static Optional<String> firstMissingObj(CustomDeviceDefinition definition) {
        for (CustomDeviceDefinition.Bone bone : definition.bones) {
            if (bone.model == null || bone.model.isBlank()) {
                continue;
            }
            try {
                Path path = CustomDeviceStorage.resolveResource(definition.deviceType, bone.model);
                if (!Files.isRegularFile(path)) {
                    return Optional.of(bone.model);
                }
            } catch (IllegalArgumentException exception) {
                return Optional.of(bone.model);
            }
        }
        return Optional.empty();
    }

    public static boolean removePlacedDeviceIfMissingObj(Level level, BlockPos pos, CustomDeviceDefinition definition) {
        if (level == null || level.isClientSide) {
            return false;
        }
        Optional<String> missing = firstMissingObj(definition);
        if (missing.isEmpty()) {
            return false;
        }
        if (level instanceof ServerLevel serverLevel) {
            Component message = Component.translatable("message.vsie.custom_device.missing_obj_removed",
                    definition.name, missing.get(), pos.toShortString()).withStyle(ChatFormatting.RED);
            for (ServerPlayer player : serverLevel.players()) {
                if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= WARNING_RADIUS_SQR) {
                    player.sendSystemMessage(message);
                }
            }
        }
        level.removeBlock(pos, false);
        return true;
    }

    public static int refreshLoadedDevices(ServerLevel level, BlockPos center, int radius) {
        int removed = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int minY = Math.max(level.getMinBuildHeight(), center.getY() - radius);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, center.getY() + radius);
        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                for (int y = minY; y <= maxY; y++) {
                    cursor.set(x, y, z);
                    if (!level.hasChunkAt(cursor)) {
                        continue;
                    }
                    BlockEntity blockEntity = level.getBlockEntity(cursor);
                    if (blockEntity instanceof CustomDeviceBlockEntity customDevice
                            && removePlacedDeviceIfMissingObj(level, cursor.immutable(), customDevice.getDefinition())) {
                        removed++;
                    }
                }
            }
        }
        return removed;
    }
}
