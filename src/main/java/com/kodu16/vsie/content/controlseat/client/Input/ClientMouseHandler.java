package com.kodu16.vsie.content.controlseat.client.Input;

import com.kodu16.vsie.content.controlseat.client.ControlSeatClientData;
import com.kodu16.vsie.content.controlseat.client.ControlSeatWarpSelectionScreen;
import com.kodu16.vsie.registries.vsieKeyMappings;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.slf4j.Logger;
import org.valkyrienskies.mod.common.entity.ShipMountingEntity;

public class ClientMouseHandler {

    // 在这干活不必考虑你做的是哪个player，你做的就是entity告诉你的客户端的player，这整个程序是跑在客户端的
    //handle负责挨个检测一遍，然后给服务端发包
    public static final Logger LOGGER = LogUtils.getLogger();
    public boolean viewlock = false;
    public static void handle(LocalPlayer player, BlockPos pos) {
        //最好统一使用minecraft实例和客户端数据，虽然我估计底下的搞到的都是同一个
        if(player!=null) {
            ControlSeatClientData data = ClientDataManager.getClientData(player);
            if (player.getUUID() == data.getUserUUID() && data.getUserUUID()!=null) {
                Minecraft minecraft = Minecraft.getInstance();
                handleMouseLock(player, data, minecraft, pos);
                handleWarpSelection(data, minecraft, pos);
                double dx = data.getAccumulatedMousex();
                double dy = data.getAccumulatedMousey();
                //LOGGER.warn(String.valueOf(Component.literal("mouseDX:"+dx+"mouseDY:"+dy)));
                //dxdy都是（-1,1）
                if (data.isViewLocked()) {
                    ClientSeatInputSender.tickSend(pos, data.getUserUUID(), dx, dy, 0, data.mouseLpress, data.viewLock, (int) player.getXRot(), (int) player.getYHeadRot());
                    //LOGGER.warn(String.valueOf(Component.literal("sending mousepress:"+data.mouseLpress)));
                }
                else {
                    ClientSeatInputSender.tickSend(pos, data.getUserUUID(), 0, 0, 0, false, data.viewLock, (int) player.getXRot(), (int) player.getYHeadRot());
                    data.reset();
                }
            }
        }
    }


    // 功能：视角锁定时按下 warp 键会打开/关闭一个可滚轮滑动的跃迁目标选单。
    private static void handleWarpSelection(ControlSeatClientData data, Minecraft minecraft, BlockPos pos) {
        if (!vsieKeyMappings.KEY_START_WARP.consumeClick()) {
            return;
        }
        if (!data.isViewLocked()) {
            if (minecraft.screen instanceof ControlSeatWarpSelectionScreen) {
                minecraft.setScreen(null);
            }
            return;
        }
        if (minecraft.screen instanceof ControlSeatWarpSelectionScreen) {
            minecraft.setScreen(null);
            return;
        }
        minecraft.setScreen(new ControlSeatWarpSelectionScreen(pos));
    }

    public static void handleMouseLock(LocalPlayer player, ControlSeatClientData data, Minecraft minecraft, BlockPos pos) {
        KeyMapping jumpKey = vsieKeyMappings.KEY_TOGGLE_LOCK; // alt键绑定为默认切换视角锁
        // 如果玩家不存在或没有控制座椅，则把视角锁关掉，UUID清掉并且跳过
        // 这个必须得看的，客户端玩家可能下船，下船下成残疾人或者下一个人上来UUID没更新你就有的乐了
        if (player == null || !(player.getVehicle() instanceof ShipMountingEntity)) {
            data.disableViewLock();
            data.clearUserUUID();
            if (minecraft.screen instanceof ControlSeatWarpSelectionScreen) {
                // 功能：玩家离开控制椅时自动关闭跃迁目标选单，避免残留 UI 挡住游戏视图。
                minecraft.setScreen(null);
            }
            //data.reset();
            return;
        }
        // 捕捉空格键按下，加上延迟按键，省的按一下切三下视角给玩家搞不会
        if (jumpKey.isDown() && System.currentTimeMillis()- data.getLastKeyPressTime()>800) {
            //我哪知道行不行，我猜行
            //按下左alt时锁定视角
            //很麻烦，视角的锁定角度必须根据控制椅的方块朝向规定四种情况，所以必须回返一个控制椅朝向
            data.toggleViewLock();
            if (data.isViewLocked()) {
                player.displayClientMessage(Component.literal("locking view to direction"), true);
                Level level = minecraft.level;
                BlockState state = level.getBlockState(pos);
                Direction facing = state.getValue(BlockStateProperties.FACING);
                int Yrot;
                if (facing == Direction.NORTH) {
                    Yrot=0;
                } else if (facing == Direction.SOUTH) {
                    Yrot=180;
                } else if (facing == Direction.EAST) {
                    Yrot=90;
                } else {
                    Yrot=270;
                }
                player.setYRot(Yrot);
                player.setXRot(0);
                // 同步头部和身体的旋转
                player.setYHeadRot(Yrot);
                player.setYBodyRot(0);
            } else {
                player.displayClientMessage(Component.literal("unlocking view"), true);
            }
            data.updatelastKeyPressTime();
        }
    }

}
