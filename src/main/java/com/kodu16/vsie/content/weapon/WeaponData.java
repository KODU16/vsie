package com.kodu16.vsie.content.weapon;

import org.valkyrienskies.core.api.ships.Ship;

// 功能：武器状态数据容器，已与 Mekanism 升级接口解耦。
public class WeaponData {
    public volatile boolean channel1 = false;//四个频道（可以同时处在多个）
    public volatile boolean channel2 = false;
    public volatile boolean channel3 = false;
    public volatile boolean channel4 = false;
    public volatile int receivingchannel = 0000;
    public volatile boolean isfiring = false;
    public Ship targetship = null;

    public boolean getChannel1(){return channel1;}
    public boolean getChannel2(){return channel2;}
    public boolean getChannel3(){return channel3;}
    public boolean getChannel4(){return channel4;}

    public void setChannel1(boolean channel){this.channel1 = channel;}
    public void setChannel2(boolean channel){this.channel2 = channel;}
    public void setChannel3(boolean channel){this.channel3 = channel;}
    public void setChannel4(boolean channel){this.channel4 = channel;}
}
