package com.kodu16.vsie.foundation;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/** Stores block links as owner-relative offsets so Sable plot relocation cannot invalidate them. */
public final class RelativeBlockPosNbt {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String LINK_TRACE_PREFIX = "[VSIE-LINK-TRACE]";
    private static final String RELATIVE_SUFFIX = "Relative";

    private RelativeBlockPosNbt() {
    }

    public static void write(CompoundTag tag, String key, BlockPos ownerPos, @Nullable BlockPos targetPos) {
        if (targetPos == null || targetPos.equals(BlockPos.ZERO)) {
            tag.putLong(key, BlockPos.ZERO.asLong());
            tag.putBoolean(key + RELATIVE_SUFFIX, false);
            return;
        }

        BlockPos offset = targetPos.subtract(ownerPos);
        tag.putIntArray(key, new int[]{offset.getX(), offset.getY(), offset.getZ()});
        tag.putBoolean(key + RELATIVE_SUFFIX, true);
    }

    public static @Nullable BlockPos read(CompoundTag tag, String key, BlockPos ownerPos, boolean nullable) {
        if (tag.getBoolean(key + RELATIVE_SUFFIX) && tag.contains(key, Tag.TAG_INT_ARRAY)) {
            int[] offset = tag.getIntArray(key);
            if (offset.length == 3) {
                return ownerPos.offset(offset[0], offset[1], offset[2]);
            }
            LOGGER.warn("{} phase=REVERSE_READ_INVALID_RELATIVE key={} owner={} length={}",
                    LINK_TRACE_PREFIX, key, ownerPos, offset.length);
        }
        if (tag.contains(key, Tag.TAG_LONG)) {
            BlockPos absolute = BlockPos.of(tag.getLong(key));
            traceLegacyAbsolute(key, ownerPos, absolute, "long");
            return nullable && absolute.equals(BlockPos.ZERO) ? null : absolute;
        }
        if (tag.contains(key, Tag.TAG_INT_ARRAY)) {
            int[] absolute = tag.getIntArray(key);
            if (absolute.length == 3) {
                BlockPos position = new BlockPos(absolute[0], absolute[1], absolute[2]);
                traceLegacyAbsolute(key, ownerPos, position, "int_array");
                return nullable && position.equals(BlockPos.ZERO) ? null : position;
            }
        }
        if (tag.contains(key, Tag.TAG_COMPOUND)) {
            CompoundTag absolute = tag.getCompound(key);
            BlockPos position = new BlockPos(absolute.getInt("x"), absolute.getInt("y"), absolute.getInt("z"));
            traceLegacyAbsolute(key, ownerPos, position, "compound");
            return nullable && position.equals(BlockPos.ZERO) ? null : position;
        }
        return nullable ? null : BlockPos.ZERO;
    }

    private static void traceLegacyAbsolute(String key, BlockPos ownerPos, BlockPos targetPos, String format) {
        if (!targetPos.equals(BlockPos.ZERO)) {
            LOGGER.info("{} phase=REVERSE_READ_LEGACY key={} owner={} target={} format={}",
                    LINK_TRACE_PREFIX, key, ownerPos, targetPos, format);
        }
    }
}
