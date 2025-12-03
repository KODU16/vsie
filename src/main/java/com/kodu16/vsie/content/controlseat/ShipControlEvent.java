package com.kodu16.vsie.content.controlseat;

import com.kodu16.vsie.content.controlseat.server.ControlSeatServerData;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Ship;

// 新建一个事件类
public class ShipControlEvent extends Event {
    private final Ship ship;                    // 当前被控制的船
    private final Player controller;            // 坐在控制椅上的玩家（可能为空）
    private final ControlSeatServerData input;  // 你现有的输入数据

    // 力、力矩、油门等你已经算好的最终值
    private final Vector3d force;    // 世界坐标系总推力（N）
    private final Vector3d torque;   // 世界坐标系总力矩（N·m）
    private final float throttle;    // 0~1 油门

    public ShipControlEvent(Ship ship, Player controller,
                            ControlSeatServerData input,
                            Vector3d force, Vector3d torque, float throttle) {
        this.ship = ship;
        this.controller = controller;
        this.input = input;
        this.force = force;
        this.torque = torque;
        this.throttle = throttle;
    }

    // getters...
    public Ship getShip() { return ship; }
    public Player getController() { return controller; }
    public ControlSeatServerData getRawInput() { return input; }
    public Vector3d getForce() { return force; }
    public Vector3d getTorque() { return torque; }
    public float getThrottle() { return throttle; }
}

