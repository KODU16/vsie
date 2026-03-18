package com.kodu16.vsie.content.controlseat.server;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;
import net.minecraft.world.entity.player.Player;
import org.valkyrienskies.core.api.ships.Ship;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.kodu16.vsie.content.controlseat.ActiveWeaponHudInfo;


//请勿在客户端使用，或加入任何仅限客户端的值
public class ControlSeatServerData {
    public volatile List<BlockPos> thrusterpositionslist = new ArrayList<>(); // 用来存储推进器的位置
    @Getter
    public volatile Vector3d force =  new Vector3d(0,0,0);
    @Getter
    @Setter
    public volatile Vector3d torque = new Vector3d(0,0,0);
    @Getter
    @Setter
    public volatile int throttle = 0;
    //似乎自带UUID
    @Getter
    @Setter
    public volatile Player player = null;
    //Direction in ship space. Expected to be normalized
    @Setter
    @Getter
    private volatile Vector3d directionForward;
    @Setter
    @Getter
    private volatile Vector3d directionUp;
    @Setter
    @Getter
    private volatile Vector3d directionRight;

    public volatile boolean channel1 = true;
    public volatile boolean channel2 = true;
    public volatile boolean channel3 = true;
    public volatile boolean channel4 = true;
    public volatile int channelencode = 0;
    public volatile boolean isfiring = false;

    public volatile String enemy = "";
    public volatile String ally = "";
    public volatile int lockedenemyindex = 0;
    public volatile Map<String, Object> shipsData = new HashMap<>();
    public volatile ArrayList<Ship> enemyshipsData = new ArrayList<>();

    public volatile float thruster_strength = 0;

    public volatile int totalenergystorage = 100;//最大可储存的电量
    public volatile int avalibleenergy = 0;//当前可用的电量

    public volatile int totalfuelstorage = 100;//最大可储存的电量
    public volatile int avaliblefuel = 0;//当前可用的电量

    public volatile double totalshield = 1;//最大护盾
    public volatile double avalibleshield = 0;//当前剩余护盾
    public volatile double shieldradius = 0;//护盾的范围
    public volatile double shieldcostperprojectile = 0;//拦截一个弹射物消耗的护盾能量
    public volatile double shieldregeneratepertick = 0;//每秒回复量
    public volatile double shieldmaxcooldowntime = 0;//护盾过载后需要多长时间才能回充
    public volatile double shieldcooldowntime = 0;//护盾过载后准备重新开始回充的时间
    public volatile boolean isshieldon = false;//是否开启护盾
    public volatile boolean isflightassiston = true;//是否开启飞行辅助
    public volatile boolean isantigravityon = true;//是否开启反重力
    public volatile double shieldmin = 0;
    public volatile double shieldmax = 0;

    // 功能：缓存“当前控制椅激活频道下可响应武器”的 HUD 数据（名称+冷却进度），用于 HUD 展示。
    public volatile List<ActiveWeaponHudInfo> activeWeaponHudInfos = new ArrayList<>();

    // 功能：记录控制椅下一次跃迁所使用的目标坐标、维度与芯片名称，供后续跃迁逻辑读取。
    public volatile BlockPos warpTargetPos = BlockPos.ZERO;
    public volatile String warpTargetDimension = "minecraft:overworld";
    public volatile String warpTargetName = "";

    public volatile boolean isviewlocked = false;
    public volatile int playerrotx = 0;
    public volatile int playerroty = 0;

    public Level level;


    @Getter
    @Setter
    public volatile Vector3d finaltorque = new Vector3d(0,0,0);
    @Getter
    @Setter
    public volatile Vector3d finalforce = new Vector3d(0,0,0);

    public boolean getChannel1() {return channel1;}
    public boolean getChannel2() {return channel2;}
    public boolean getChannel3() {return channel3;}
    public boolean getChannel4() {return channel4;}


    public void reset() {
        this.torque = new Vector3d(0,0,0);
        this.force = new Vector3d(0,0,0);
        this.throttle = 0;
        this.player = null;
        this.isfiring = false;
    }
}
