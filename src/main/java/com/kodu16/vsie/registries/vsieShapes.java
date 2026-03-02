package com.kodu16.vsie.registries;

import net.createmod.catnip.math.VoxelShaper;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;

import net.minecraft.core.Direction.Axis;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.BiFunction;

import static net.minecraft.core.Direction.NORTH;

public class vsieShapes {
    public static final VoxelShaper
            //是Z朝上而不是mc传统的Y朝上
    BASIC_THRUSTER = ShapeBuilder.shape()
    .add(Block.box(0, 0, 0, 16, 16, 18))
    .forDirectional(),

    MEDIUM_THRUSTER = ShapeBuilder.shape()
    .add(Block.box(-16, -16, -16, 32, 32, 32))
    .forDirectional(),

    BASIC_SCREEN = ShapeBuilder.shape()
            .add(Block.box(0, 0, 0, 16, 1, 16))
            .forDirectional(),

    WING = getWingShape(4),
            WING_8 = getWingShape(8),
            WING_12 = getWingShape(12);

    public static VoxelShaper getWingShape(int width) {
        if (width % 2 != 0) throw new IllegalArgumentException("exception in wing shape!");
        int halfWidth = width / 2;
        return ShapeBuilder.shape()
                .add(Block.box(0, 8 - halfWidth, 0, 16, 8 + halfWidth, 16))
                .forDirectional(Direction.UP);
    }

    public static class ShapeBuilder {
        private VoxelShape shape;

        public static ShapeBuilder shapeBuilder(VoxelShape shape) {
            return new ShapeBuilder(shape);
        }

        public static ShapeBuilder shape() {
            return new ShapeBuilder(Shapes.empty());
        }

        private static VoxelShape cuboid(double x1, double y1, double z1, double x2, double y2, double z2) {
            return Block.box(x1, y1, z1, x2, y2, z2);
        }

        public ShapeBuilder(VoxelShape shape) {
            this.shape = shape;
        }

        public ShapeBuilder add(VoxelShape shape) {
            this.shape = Shapes.or(this.shape, shape);
            return this;
        }

        public ShapeBuilder add(double x1, double y1, double z1, double x2, double y2, double z2) {
            return add(cuboid(x1, y1, z1, x2, y2, z2));
        }

        public ShapeBuilder erase(double x1, double y1, double z1, double x2, double y2, double z2) {
            this.shape = Shapes.join(shape, cuboid(x1, y1, z1, x2, y2, z2), BooleanOp.ONLY_FIRST);
            return this;
        }

        public VoxelShape build() {
            return shape;
        }

        public VoxelShaper build(BiFunction<VoxelShape, Direction, VoxelShaper> factory, Direction direction) {
            return factory.apply(shape, direction);
        }

        public VoxelShaper build(BiFunction<VoxelShape, Axis, VoxelShaper> factory, Axis axis) {
            return factory.apply(shape, axis);
        }

        public VoxelShaper forDirectional(Direction direction) {
            return build(VoxelShaper::forDirectional, direction);
        }

        public VoxelShaper forAxis() {
            return build(VoxelShaper::forAxis, Axis.Y);
        }

        public VoxelShaper forHorizontalAxis() {
            return build(VoxelShaper::forHorizontalAxis, Axis.Z);
        }

        public VoxelShaper forHorizontal(Direction direction) {
            return build(VoxelShaper::forHorizontal, direction);
        }

        public VoxelShaper forDirectional() {
            return forDirectional(NORTH);
        }
    }
}

