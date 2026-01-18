package com.kodu16.vsie.content.weapon;

import mekanism.common.upgrade.IUpgradeData;

public class WeaponData implements IUpgradeData {
    public volatile boolean channel1 = false;//四个频道（可以同时处在多个）
    public volatile boolean channel2 = false;
    public volatile boolean channel3 = false;
    public volatile boolean channel4 = false;

    public boolean isChannel1(){return channel1;}
    public boolean isChannel2(){return channel2;}
    public boolean isChannel3(){return channel3;}
    public boolean isChannel4(){return channel4;}

    public void setChannel1(boolean channel){this.channel1 = channel;}
    public void setChannel2(boolean channel){this.channel2 = channel;}
    public void setChannel3(boolean channel){this.channel3 = channel;}
    public void setChannel4(boolean channel){this.channel4 = channel;}
}
