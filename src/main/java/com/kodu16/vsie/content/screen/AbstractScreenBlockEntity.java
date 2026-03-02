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
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.network.SerializableDataTicket;

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


    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public AbstractScreenBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    public ItemStack getRenderStack() { return renderStack; }
    public String getRenderText() { return renderText; }

    public abstract String getScreentype();

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
