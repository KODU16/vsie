package com.kodu16.vsie.content.controlseat.server;

import net.minecraft.core.BlockPos;
import org.joml.Vector3d;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class ControlSeatServerData {
    //忍痛大换血换成了一个更清晰的结构，现在这里都是server的，和方块本身绑定的，player也是serverplayer
    //对于一个控制椅，最大的问题就是它朝向不定，对于船只坐标系，有+X,-X,+Z,-Z四种可能，不过基于控制椅的放置相对方向，可以转换一下控制矢量输入的方向
    //我到现在总算明白direction是怎么用的了
    public volatile List<BlockPos> thrusterpositionslist = new ArrayList<>(); // 用来存储推进器的位置
    public volatile Vector3d force =  new Vector3d(0,0,0);
    public volatile Vector3d torque = new Vector3d(0,0,0);
    public volatile int throttle = 0;
    public volatile Player player = null;
    private volatile Vector3d directionForward;
    private volatile Vector3d directionUp;
    private volatile Vector3d directionRight;

    public volatile Vector3d finaltorque = new Vector3d(0,0,0);
    public volatile Vector3d finalforce = new Vector3d(0,0,0);

    public Player getPlayer() { return player; } //似乎自带UUID
    public Vector3d getForce() { return force; }
    public Vector3d getTorque() { return torque; }
    public int getThrottle() {return throttle;}
    public Vector3d getFinaltorque() { return finaltorque; }
    public Vector3d getFinalforce() { return finalforce; }

    public void setPlayer(Player player) { this.player = player; }
    public void setTorque(Vector3d torque) { this.torque = torque; }
    public void setThrottle(int throttle) { this.throttle = throttle; }
    public void setFinaltorque(Vector3d finaltorque) { this.finaltorque = finaltorque; }
    public void setFinalforce(Vector3d finalforce) { this.finalforce = finalforce; }

    //Direction in ship space. Expected to be normalized
    public Vector3d getDirectionForward() { return directionForward; }
    public Vector3d getDirectionUp() { return directionUp; }
    public Vector3d getDirectionRight() { return directionRight; }
    public void setDirectionForward(Vector3d direction) { this.directionForward = direction; }
    public void setDirectionUp(Vector3d direction) { this.directionUp = direction; }
    public void setDirectionRight(Vector3d direction) { this.directionRight = direction; }
    public void reset() {
        this.torque = new Vector3d(0,0,0);
        this.force = new Vector3d(0,0,0);
        this.throttle = 0;
        this.player = null;
    }
}
