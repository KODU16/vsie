package com.kodu16.vsie.content.controlseat.client;

import com.kodu16.vsie.registries.vsieKeyMappings;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.client.KeyMapping;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.valkyrienskies.mod.common.entity.ShipMountingEntity;

public class ClientInputHandler {

    // 在这干活不必考虑你做的是哪个player，你做的就是entity告诉你的客户端的player，这整个程序是跑在客户端的
    //handle负责挨个检测一遍，然后给服务端发包
    public static final Logger LOGGER = LogUtils.getLogger();
    public static void handle(LocalPlayer player, BlockPos pos) {
        //最好统一使用minecraft实例和客户端数据，虽然我估计底下的搞到的都是同一个
        ControlSeatClientData data = ClientDataManager.getClientData(player);
        if (player.getUUID() == data.getUserUUID() && data.getUserUUID()!=null) {
            Minecraft minecraft = Minecraft.getInstance();
            handleMouseLock(player, data, minecraft);
            double dx = data.getAccumulatedMousex();
            double dy = data.getAccumulatedMousey();
            //LOGGER.warn(String.valueOf(Component.literal("mouseDX:"+dx+"mouseDY:"+dy)));
            //dxdy都是（-1,1）
            if (data.isViewLocked()) {
                ClientSeatInputSender.tickSend(pos, data.getUserUUID(), dx, dy, 0);
            }
            else {
                ClientSeatInputSender.tickSend(pos, data.getUserUUID(), 0, 0, 0);
                data.reset();
            }
        }
    }

    public static void handleMouseLock(LocalPlayer player, ControlSeatClientData data, Minecraft minecraft) {
        KeyMapping jumpKey = vsieKeyMappings.KEY_TOGGLE_LOCK; // alt键绑定为默认切换视角锁
        // 如果玩家不存在或没有控制座椅，则把视角锁关掉，UUID清掉并且跳过
        // 这个必须得看的，客户端玩家可能下船，下船下成残疾人或者下一个人上来UUID没更新你就有的乐了
        if (player == null || !(player.getVehicle() instanceof ShipMountingEntity)) {
            data.disableViewLock();
            data.clearUserUUID();
            //data.reset();
            return;
        }
        // 捕捉空格键按下，加上延迟按键，省的按一下切三下视角给玩家搞不会
        if (jumpKey.isDown() && System.currentTimeMillis()- data.getLastKeyPressTime()>800) {
            //我哪知道行不行，我猜行
            //好吧的确行，我爱deepseek，GPT5和mixin
            //下次搞完mixin记得重构，打包项目不算重构
            //按下空格键时锁定视角
            data.toggleViewLock();
            if (data.isViewLocked()) {
                player.displayClientMessage(Component.literal("locking view to direction"), true);
                setPlayerViewDirection(player,data.getShipFacing());
            } else {
                player.displayClientMessage(Component.literal("unlocking view"), true);
            }
            data.updatelastKeyPressTime();
        }
    }

    public static void setPlayerViewDirection(LocalPlayer player, Vector3d targetDirection) {
        // 创建一个向量来表示玩家的前方
        Vector3f targetDir = new Vector3f((float) targetDirection.x, (float) targetDirection.y, (float) targetDirection.z);
        targetDir.normalize();
        // 设置玩家的朝向旋转（这里使用的是玩家的 yaw 和 pitch）
        float yaw = (float) Math.toDegrees(Math.atan2(targetDirection.x, targetDirection.z));
        float pitch = (float) Math.toDegrees(Math.asin(targetDirection.y));

        // 设置玩家的旋转 (yaw 作为水平旋转，pitch 作为垂直旋转)
        player.setYRot(180-yaw);
        player.setXRot(pitch);

        // 同步头部和身体的旋转
        player.setYHeadRot(180-yaw);
        player.setYBodyRot(180-yaw);
    }
}
