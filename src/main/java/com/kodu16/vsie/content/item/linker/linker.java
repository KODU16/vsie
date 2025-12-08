package com.kodu16.vsie.content.item.linker;

import com.kodu16.vsie.content.controlseat.AbstractControlSeatBlockEntity;
import com.kodu16.vsie.content.thruster.AbstractThrusterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class linker extends Item {

    // 存储 A -> {B点: true}
    //private static final Map<BlockPos, Map<BlockPos, Boolean>> controllerMap = new HashMap<>();

    public linker(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.PASS;

        ServerPlayer player = (ServerPlayer) context.getPlayer();
        InteractionHand hand = context.getHand();
        BlockPos clickedPos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        CompoundTag nbt = stack.getOrCreateTag();

        // 如果没控制椅，绑定控制椅
        if (!nbt.contains("ControlSeatPos")) {
            int[] pos = {clickedPos.getX(), clickedPos.getY(), clickedPos.getZ()};
            BlockEntity blockEntity = level.getBlockEntity(convertToBlockPos(new Vector3d(clickedPos.getX(), clickedPos.getY(), clickedPos.getZ())));
            if (blockEntity instanceof AbstractControlSeatBlockEntity controlseat) {
                nbt.putIntArray("ControlSeatPos", pos);
                player.displayClientMessage(Component.literal("§a绑定器已绑定至控制椅: " + clickedPos), true);
                //controllerMap.putIfAbsent(clickedPos, new HashMap<>());
                return InteractionResult.CONSUME;
            }
            else {
                player.displayClientMessage(Component.literal("绑定器未绑定控制椅"), true);
                return InteractionResult.CONSUME;
            }
        }

        // 如果已经绑定控制椅：绑定外设
        int[] controllerArray = nbt.getIntArray("ControlSeatPos");
        BlockPos controllerPos = new BlockPos(controllerArray[0], controllerArray[1], controllerArray[2]);
        BlockEntity blockEntityA = level.getBlockEntity(convertToBlockPos(new Vector3d(clickedPos.getX(), clickedPos.getY(), clickedPos.getZ())));
        if (blockEntityA instanceof AbstractControlSeatBlockEntity controlseat) {
            //controllerMap.computeIfAbsent(controllerPos, k -> new HashMap<>());
            //controllerMap.get(controllerPos).put(clickedPos, true);
            Vec3 pos = new Vec3(clickedPos.getX(), clickedPos.getY(), clickedPos.getZ());
            BlockEntity blockEntityB = level.getBlockEntity(convertToBlockPos(new Vector3d(clickedPos.getX(), clickedPos.getY(), clickedPos.getZ())));
            if (blockEntityB instanceof AbstractThrusterBlockEntity thruster) {
                controlseat.addLinkedPeripheral(pos, 0);
            }
            player.displayClientMessage(Component.literal("§b已将控制椅: " + controllerPos + " 与外设: " + clickedPos + " 绑定"), true);
            return InteractionResult.CONSUME;
        }
        else {
            player.displayClientMessage(Component.literal("在绑定的坐标找不到控制椅"), true);
            nbt.remove("ControlSeatPos");
            return InteractionResult.CONSUME;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (stack.hasTag() && stack.getTag().contains("controllerPos")) {
            int[] arr = stack.getTag().getIntArray("controllerPos");
            BlockPos pos = new BlockPos(arr[0], arr[1], arr[2]);
            tooltip.add(Component.literal("§e控制椅位置: " + pos));
        } else {
            tooltip.add(Component.literal("§7右键控制椅绑定控制椅，再右键外设绑定任意外设\n外设必须被绑定才可被控制椅操纵\n在跨越维度或重新进入游戏时，绑定关系不会失效\n一个外设仅能被一个控制椅绑定，后续绑定不会生效\n控制椅发现外设被移除时，将自动与外设位置解除绑定"));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }

    public static BlockPos convertToBlockPos(Vector3dc vector) {
        // 获取 Vector3dc 的坐标
        int x = (int) Math.floor(vector.x());
        int y = (int) Math.floor(vector.y());
        int z = (int) Math.floor(vector.z());

        // 创建并返回 BlockPos 对象
        //我讨厌vector3dc
        return new BlockPos(x, y, z);
    }
}
