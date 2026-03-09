package com.kodu16.vsie.content.screen;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.engine_room.flywheel.backend.gl.array.VertexAttribute;
import mekanism.common.registries.MekanismItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.network.SerializableDataTicket;

import java.util.UUID;

public abstract class AbstractScreenBlockEntity extends SmartBlockEntity implements GeoBlockEntity {
    private ItemStack renderStack = ItemStack.EMPTY;
    private String renderText = "Hello";
    public static SerializableDataTicket<Integer> SPINX;
    public static SerializableDataTicket<Integer> SPINY;
    public static SerializableDataTicket<Integer> OFFSETX;
    public static SerializableDataTicket<Integer> OFFSETY;
    public static SerializableDataTicket<Integer> OFFSETZ;

    public int spinx;
    public int spiny;
    public int offsetx;
    public int offsety;
    public int offsetz;

    // 功能：雷达屏幕绑定的控制椅玩家 UUID，用于客户端反查对应玩家的 ClientData。
    private UUID radarPlayerUuid;
    // 功能：缓存控制椅世界坐标，供客户端将周围船只投影到屏幕雷达上。
    private Vector3d radarControlSeatWorldPos = new Vector3d();


    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public AbstractScreenBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    public ItemStack getRenderStack() { return renderStack; }
    public String getRenderText() { return renderText; }

    public abstract String getScreentype();

    // 功能：读取当前雷达绑定玩家 UUID。
    public UUID getRadarPlayerUuid() {
        return radarPlayerUuid;
    }

    // 功能：写入当前雷达绑定玩家 UUID，并触发方块实体同步。
    public void setRadarPlayerUuid(UUID radarPlayerUuid) {
        this.radarPlayerUuid = radarPlayerUuid;
        setChanged();
    }

    // 功能：检查雷达是否已经绑定玩家。
    public boolean hasRadarPlayer() {
        return radarPlayerUuid != null;
    }

    // 功能：读取控制椅的世界坐标缓存。
    public Vector3d getRadarControlSeatWorldPos() {
        return new Vector3d(radarControlSeatWorldPos);
    }

    // 功能：更新控制椅世界坐标缓存，并触发方块实体同步。
    public void setRadarControlSeatWorldPos(Vector3d worldPos) {
        this.radarControlSeatWorldPos = new Vector3d(worldPos);
        setChanged();
    }

    @Override
    public void tick() {
        super.tick();
        this.renderStack = new ItemStack(MekanismItems.ATOMIC_ALLOY,32);
        this.renderText = "hello";
    }

    // 更新数据时同步到客户端
    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public void setdata(int spinx, int spiny, int offsetx, int offsety, int offsetz) {
        this.setAnimData(SPINX,spinx);
        this.spinx = spinx;
        this.setAnimData(SPINY,spiny);
        this.spiny = spiny;
        this.setAnimData(OFFSETX,offsetx);
        this.offsetx = offsetx;
        this.setAnimData(OFFSETY,offsety);
        this.offsety = offsety;
        this.setAnimData(OFFSETZ,offsetz);
        this.offsetz = offsetz;
    }

    @Override
    public void write(CompoundTag tag, boolean clientpacket) {
        super.write(tag,clientpacket);
        // 保存数据到 NBT
        tag.put("RenderStack", renderStack.save(new CompoundTag()));
        tag.putString("RenderText", renderText);
        tag.putInt("spinx",spinx);
        tag.putInt("spiny",spiny);
        tag.putInt("offsetx",offsetx);
        tag.putInt("offsety",offsety);
        tag.putInt("offsetz",offsetz);
        // 功能：持久化雷达绑定玩家信息。
        if (radarPlayerUuid != null) {
            tag.putUUID("RadarPlayerUuid", radarPlayerUuid);
        }
        // 功能：持久化雷达用的控制椅世界坐标。
        tag.putDouble("RadarSeatWorldX", radarControlSeatWorldPos.x);
        tag.putDouble("RadarSeatWorldY", radarControlSeatWorldPos.y);
        tag.putDouble("RadarSeatWorldZ", radarControlSeatWorldPos.z);
    }

    @Override
    public void read(CompoundTag tag, boolean clientpacket) {
        super.read(tag,clientpacket);
        if(tag.contains("RenderStack")) {
            renderStack = ItemStack.of(tag.getCompound("RenderStack"));
        }
        if(tag.contains("RenderText")) {
            renderText = tag.getString("RenderText");
        }
        if(tag.contains("spinx") && tag.contains("spiny") && tag.contains("offsetx") && tag.contains("offfsety") && tag.contains("offsetz")) {
            this.spinx = tag.getInt("spinx");
            this.spiny = tag.getInt("spiny");
            this.offsetx = tag.getInt("offsetx");
            this.offsety = tag.getInt("offsety");
            this.offsetz = tag.getInt("offsetz");
            this.setdata(this.spinx,this.spiny,this.offsetx,this.offsety,this.offsetz);
        }
        // 功能：读取雷达绑定玩家 UUID。
        radarPlayerUuid = tag.hasUUID("RadarPlayerUuid") ? tag.getUUID("RadarPlayerUuid") : null;
        // 功能：读取雷达用控制椅世界坐标。
        radarControlSeatWorldPos = new Vector3d(
                tag.getDouble("RadarSeatWorldX"),
                tag.getDouble("RadarSeatWorldY"),
                tag.getDouble("RadarSeatWorldZ")
        );
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        write(tag, true);
        return tag;
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag);
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        read(tag, true);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
