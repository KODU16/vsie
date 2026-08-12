package com.kodu16.vsie.content.custom_turret.export;

import com.kodu16.vsie.utility.ItemStackNbt;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

/** Stores the two export corners on the tool so copying or saving the stack keeps the selection. */
public record RegionExportSelection(ResourceLocation dimension, BlockPos first, BlockPos second) {
    public static final String DIMENSION_TAG = "RegionExportDimension";
    public static final String FIRST_TAG = "RegionExportFirst";
    public static final String SECOND_TAG = "RegionExportSecond";
    public static final String SUBLEVEL_TAG = "RegionExportSubLevel";

    public BlockPos min() {
        return new BlockPos(
                Math.min(first.getX(), second.getX()),
                Math.min(first.getY(), second.getY()),
                Math.min(first.getZ(), second.getZ())
        );
    }

    public BlockPos max() {
        return new BlockPos(
                Math.max(first.getX(), second.getX()),
                Math.max(first.getY(), second.getY()),
                Math.max(first.getZ(), second.getZ())
        );
    }

    public long volume() {
        return RegionObjExportContract.inclusiveVolume(
                first.getX(), first.getY(), first.getZ(), second.getX(), second.getY(), second.getZ());
    }

    public boolean isIn(Level level) {
        return level.dimension().location().equals(dimension);
    }

    public static Optional<ResourceLocation> dimension(ItemStack stack) {
        CompoundTag tag = ItemStackNbt.get(stack);
        if (tag == null || !tag.contains(DIMENSION_TAG)) {
            return Optional.empty();
        }
        return ResourceLocation.read(tag.getString(DIMENSION_TAG)).result();
    }

    public static Optional<BlockPos> first(ItemStack stack) {
        CompoundTag tag = ItemStackNbt.get(stack);
        return tag != null && tag.contains(FIRST_TAG) ? Optional.of(BlockPos.of(tag.getLong(FIRST_TAG))) : Optional.empty();
    }

    public static Optional<UUID> subLevelId(ItemStack stack) {
        CompoundTag tag = ItemStackNbt.get(stack);
        return tag != null && tag.hasUUID(SUBLEVEL_TAG) ? Optional.of(tag.getUUID(SUBLEVEL_TAG)) : Optional.empty();
    }

    public static boolean isSameSubLevel(ItemStack stack, Level level, BlockPos pos) {
        SubLevel subLevel = Sable.HELPER.getContaining(level, pos);
        Optional<UUID> saved = subLevelId(stack);
        return saved.isPresent()
                ? subLevel != null && saved.get().equals(subLevel.getUniqueId())
                : subLevel == null;
    }

    public static Optional<RegionExportSelection> complete(ItemStack stack) {
        CompoundTag tag = ItemStackNbt.get(stack);
        if (tag == null || !tag.contains(DIMENSION_TAG) || !tag.contains(FIRST_TAG) || !tag.contains(SECOND_TAG)) {
            return Optional.empty();
        }
        return ResourceLocation.read(tag.getString(DIMENSION_TAG)).result()
                .map(dimension -> new RegionExportSelection(
                        dimension,
                        BlockPos.of(tag.getLong(FIRST_TAG)),
                        BlockPos.of(tag.getLong(SECOND_TAG))
                ));
    }

    public static void setFirst(ItemStack stack, Level level, BlockPos pos) {
        CompoundTag tag = ItemStackNbt.getOrCreate(stack);
        tag.putString(DIMENSION_TAG, level.dimension().location().toString());
        tag.putLong(FIRST_TAG, pos.asLong());
        tag.remove(SECOND_TAG);
        SubLevel subLevel = Sable.HELPER.getContaining(level, pos);
        if (subLevel == null) {
            tag.remove(SUBLEVEL_TAG);
        } else {
            tag.putUUID(SUBLEVEL_TAG, subLevel.getUniqueId());
        }
        ItemStackNbt.set(stack, tag);
    }

    public static void setSecond(ItemStack stack, BlockPos pos) {
        CompoundTag tag = ItemStackNbt.getOrCreate(stack);
        tag.putLong(SECOND_TAG, pos.asLong());
        ItemStackNbt.set(stack, tag);
    }

    public static void clear(ItemStack stack) {
        CompoundTag tag = ItemStackNbt.get(stack);
        if (tag == null) {
            return;
        }
        tag.remove(DIMENSION_TAG);
        tag.remove(FIRST_TAG);
        tag.remove(SECOND_TAG);
        tag.remove(SUBLEVEL_TAG);
        ItemStackNbt.set(stack, tag);
    }
}
