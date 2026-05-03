package com.kodu16.vsie.content.screen;

import com.kodu16.vsie.content.screen.server.ServerInfoGetter;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.constant.dataticket.SerializableDataTicket;

import java.util.UUID;

public abstract class AbstractScreenBlockEntity extends SmartBlockEntity implements GeoBlockEntity {
    private ItemStack renderStack = ItemStack.EMPTY;
    private String renderText = "Hello";
    private HolderLookup.Provider nbtRegistries;
    public int displaytype = 0;//0:闆疯揪 1:鏈嶅姟鍣ㄤ俊鎭?
    public static SerializableDataTicket<Integer> SCREEN_SPIN_X;
    public static SerializableDataTicket<Integer> SCREEN_SPIN_Y;
    public static SerializableDataTicket<Integer> SCREEN_OFFSET_X;
    public static SerializableDataTicket<Integer> SCREEN_OFFSET_Y;
    public static SerializableDataTicket<Integer> SCREEN_OFFSET_Z;

    public int spinx;
    public int spiny;
    public int offsetx;
    public int offsety;
    public int offsetz;

    public float clientJVMpercentage = 0;
    public float serverJVMpercentage = 0;
    public int tps = 0;
    public int phystps = 0;

    // 鍔熻兘锛氫负 serverinfo 淇濆瓨鏈€杩?20 娆￠噰鏍疯褰曪紙姣忛」閮藉瓨褰掍竴鍖栨瘮渚嬶紝渚夸簬瀹㈡埛绔粺涓€缁樺浘锛夈€?
    private static final int SERVERINFO_HISTORY_LIMIT = 20;
    // 鍔熻兘锛氭帶鍒堕噰鏍烽鐜囷紝姣?100 tick 璁板綍涓€娆°€?
    private static final int SERVERINFO_SAMPLE_INTERVAL = 100;
    // 鍔熻兘锛氳褰曞凡瀛樺偍鐨勬牱鏈暟閲忥紙鏈€澶?20锛屼粎瀹㈡埛绔湰鍦扮淮鎶わ級銆?
    private int serverInfoHistorySize = 0;
    // 鍔熻兘锛氭湇鍔＄閲囨牱璁℃暟鍣紙鐢ㄤ簬 100 tick 瑙﹀彂涓€娆♀€滄渶鏂版牱鏈€濆悓姝ワ級銆?
    private int serverInfoSampleTickCounter = 0;
    // 鍔熻兘锛氭湇鍔＄鏈€鏂版牱鏈簭鍙凤紱姣忎骇鐢熶竴涓柊鏍锋湰灏?+1锛屽苟閫氳繃 NBT 鍚屾鍒板鎴风銆?
    private int serverInfoSampleSequence = 0;
    // 鍔熻兘锛氬鎴风宸叉秷璐圭殑鏈€鏂版牱鏈簭鍙凤紱鐢ㄤ簬纭繚鈥滄瘡娆″悓姝ュ彧鍏ラ槦涓€娆″巻鍙测€濄€?
    private int clientConsumedSampleSequence = -1;
    // 鍔熻兘锛歍PS 鍘嗗彶锛堝綊涓€鍖栧埌 0~1锛屾渶澶у€兼寜 20 璁＄畻锛夈€?
    private final float[] tpsHistory = new float[SERVERINFO_HISTORY_LIMIT];
    // 鍔熻兘锛歅hysTPS 鍘嗗彶锛堝綊涓€鍖栧埌 0~1锛屾渶澶у€兼寜 60 璁＄畻锛夈€?
    private final float[] physTpsHistory = new float[SERVERINFO_HISTORY_LIMIT];
    // 鍔熻兘锛氭湇鍔″櫒鍐呭瓨鍗犵敤鐜囧巻鍙诧紙0~1锛夈€?
    private final float[] serverMemoryHistory = new float[SERVERINFO_HISTORY_LIMIT];
    // 鍔熻兘锛氬鎴风鍐呭瓨鍗犵敤鐜囧巻鍙诧紙0~1锛夈€?
    private final float[] clientMemoryHistory = new float[SERVERINFO_HISTORY_LIMIT];

    // 鍔熻兘锛氶浄杈惧睆骞曠粦瀹氱殑鎺у埗妞呯帺瀹?UUID锛岀敤浜庡鎴风鍙嶆煡瀵瑰簲鐜╁鐨?ClientData銆?
    private UUID radarPlayerUuid;
    // 鍔熻兘锛氱紦瀛樻帶鍒舵涓栫晫鍧愭爣锛屼緵瀹㈡埛绔皢鍛ㄥ洿鑸瑰彧鎶曞奖鍒板睆骞曢浄杈句笂銆?
    private Vector3d radarControlSeatWorldPos = new Vector3d();


    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public AbstractScreenBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    public ItemStack getRenderStack() { return renderStack; }
    public String getRenderText() { return renderText; }

    public abstract String getDisplaytype();

    public void setscreendisplaytype(int type){
        this.displaytype = type;
    }

    // 鍔熻兘锛氳鍙栧綋鍓嶉浄杈剧粦瀹氱帺瀹?UUID銆?
    public UUID getRadarPlayerUuid() {
        return radarPlayerUuid;
    }

    // 鍔熻兘锛氬啓鍏ュ綋鍓嶉浄杈剧粦瀹氱帺瀹?UUID锛屽苟瑙﹀彂鏂瑰潡瀹炰綋鍚屾銆?
    public void setRadarPlayerUuid(UUID radarPlayerUuid) {
        this.radarPlayerUuid = radarPlayerUuid;
        setChanged();
    }

    // 鍔熻兘锛氭鏌ラ浄杈炬槸鍚﹀凡缁忕粦瀹氱帺瀹躲€?
    public boolean hasRadarPlayer() {
        return radarPlayerUuid != null;
    }

    // 鍔熻兘锛氳鍙栨帶鍒舵鐨勪笘鐣屽潗鏍囩紦瀛樸€?
    public Vector3d getRadarControlSeatWorldPos() {
        return new Vector3d(radarControlSeatWorldPos);
    }

    // 鍔熻兘锛氭洿鏂版帶鍒舵涓栫晫鍧愭爣缂撳瓨锛屽苟瑙﹀彂鏂瑰潡瀹炰綋鍚屾銆?
    public void setRadarControlSeatWorldPos(Vector3d worldPos) {
        this.radarControlSeatWorldPos = new Vector3d(worldPos);
        setChanged();
    }

    @Override
    public void tick() {
        super.tick();
        if(this.displaytype == 1) {
            if(this.level.isClientSide()) {
                long[] JVMc = ServerInfoGetter.getJVM();
                this.clientJVMpercentage = (float) JVMc[0] /JVMc[1];

                // 鍔熻兘锛氬鎴风妫€娴嬪埌鏈嶅姟绔€滄柊鏍锋湰搴忓彿鈥濆悗锛屽皢鏈€鏂版牱鏈帹鍏ユ湰鍦?20 鏉″巻鍙插苟鍒犻櫎鏈€鏃ф暟鎹€?
                if (this.clientConsumedSampleSequence != this.serverInfoSampleSequence) {
                    this.clientConsumedSampleSequence = this.serverInfoSampleSequence;
                    pushServerInfoHistory(
                            clamp01((float) this.tps / 20f),
                            clamp01((float) this.phystps / 60f),
                            clamp01(this.serverJVMpercentage),
                            clamp01(this.clientJVMpercentage)
                    );
                }
            } else {
                long[] JVMs = ServerInfoGetter.getJVM();
                this.serverJVMpercentage = (float) JVMs[0] /JVMs[1];

                this.phystps = ServerInfoGetter.getServerPhysTPS(this.level);

                this.tps = (int) ServerInfoGetter.getServerTPS(this.level);

                // 鍔熻兘锛氭湇鍔＄姣?100 tick 浠呭悓姝モ€滀竴涓渶鏂版牱鏈€濓紝閬垮厤姣忔閮戒紶鏁存鍘嗗彶 NBT銆?
                this.serverInfoSampleTickCounter++;
                if (this.serverInfoSampleTickCounter >= SERVERINFO_SAMPLE_INTERVAL) {
                    this.serverInfoSampleTickCounter = 0;
                    this.serverInfoSampleSequence++;
                    setChanged();
                }
            }
        }
    }

    // 鍔熻兘锛氬悜鍘嗗彶鏁扮粍杩藉姞涓€鏉¤褰曪紱婊?20 鏉″悗宸︾Щ涓€鏍硷紝濮嬬粓淇濈暀鏈€鏂?20 鏉°€?
    private void pushServerInfoHistory(float tpsRatio, float physTpsRatio, float serverMemoryRatio, float clientMemoryRatio) {
        if (serverInfoHistorySize < SERVERINFO_HISTORY_LIMIT) {
            int idx = serverInfoHistorySize++;
            tpsHistory[idx] = tpsRatio;
            physTpsHistory[idx] = physTpsRatio;
            serverMemoryHistory[idx] = serverMemoryRatio;
            clientMemoryHistory[idx] = clientMemoryRatio;
            return;
        }
        for (int i = 1; i < SERVERINFO_HISTORY_LIMIT; i++) {
            tpsHistory[i - 1] = tpsHistory[i];
            physTpsHistory[i - 1] = physTpsHistory[i];
            serverMemoryHistory[i - 1] = serverMemoryHistory[i];
            clientMemoryHistory[i - 1] = clientMemoryHistory[i];
        }
        int last = SERVERINFO_HISTORY_LIMIT - 1;
        tpsHistory[last] = tpsRatio;
        physTpsHistory[last] = physTpsRatio;
        serverMemoryHistory[last] = serverMemoryRatio;
        clientMemoryHistory[last] = clientMemoryRatio;
    }

    // 鍔熻兘锛氬澶栨彁渚涘巻鍙叉暟鎹暱搴︼紝渚涙覆鏌撳眰鎸夋湁鏁堟牱鏈暟閲忕粯鍒躲€?
    public int getServerInfoHistorySize() {
        return serverInfoHistorySize;
    }

    // 鍔熻兘锛氬澶栨彁渚?TPS 鍘嗗彶姣斾緥鏁扮粍銆?
    public float[] getTpsHistory() {
        return tpsHistory;
    }

    // 鍔熻兘锛氬澶栨彁渚?PhysTPS 鍘嗗彶姣斾緥鏁扮粍銆?
    public float[] getPhysTpsHistory() {
        return physTpsHistory;
    }

    // 鍔熻兘锛氬澶栨彁渚涙湇鍔″櫒鍐呭瓨鍘嗗彶姣斾緥鏁扮粍銆?
    public float[] getServerMemoryHistory() {
        return serverMemoryHistory;
    }

    // 鍔熻兘锛氬澶栨彁渚涘鎴风鍐呭瓨鍘嗗彶姣斾緥鏁扮粍銆?
    public float[] getClientMemoryHistory() {
        return clientMemoryHistory;
    }

    // 鍔熻兘锛氬皢娴偣鍊奸檺鍒跺湪 0~1锛岄伩鍏嶉噰鏍峰紓甯稿鑷寸粯鍒惰秺鐣屻€?
    private static float clamp01(float value) {
        if (value < 0f) return 0f;
        return Math.min(value, 1f);
    }

    // 鏇存柊鏁版嵁鏃跺悓姝ュ埌瀹㈡埛绔?
    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public void setdata(int spinx, int spiny, int offsetx, int offsety, int offsetz) {
        this.setAnimData(SCREEN_SPIN_X,spinx);
        this.spinx = spinx;
        this.setAnimData(SCREEN_SPIN_Y,spiny);
        this.spiny = spiny;
        this.setAnimData(SCREEN_OFFSET_X,offsetx);
        this.offsetx = offsetx;
        this.setAnimData(SCREEN_OFFSET_Y,offsety);
        this.offsety = offsety;
        this.setAnimData(SCREEN_OFFSET_Z,offsetz);
        this.offsetz = offsetz;
    }

    private HolderLookup.Provider currentNbtRegistries() {
        return nbtRegistries != null ? nbtRegistries : this.level.registryAccess();
    }

    private void withNbtRegistries(HolderLookup.Provider registries, Runnable action) {
        HolderLookup.Provider previous = this.nbtRegistries;
        this.nbtRegistries = registries;
        try {
            action.run();
        } finally {
            this.nbtRegistries = previous;
        }
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientpacket) {
        super.write(tag, registries, clientpacket);
        // 淇濆瓨鏁版嵁鍒?NBT
        tag.put("RenderStack", renderStack.saveOptional(registries));
        tag.putString("RenderText", renderText);
        tag.putInt("type", displaytype);
        tag.putInt("spinx",spinx);
        tag.putInt("spiny",spiny);
        tag.putInt("offsetx",offsetx);
        tag.putInt("offsety",offsety);
        tag.putInt("offsetz",offsetz);
        //tag.putFloat("clientjvm",clientJVMpercentage);
        tag.putFloat("serverjvm",serverJVMpercentage);
        tag.putInt("tps",tps);
        tag.putInt("phystps",phystps);
        // 鍔熻兘锛氫粎鍚屾鏈€鏂版牱鏈簭鍙凤紝璁╁鎴风鍩轰簬璇ュ簭鍙峰湪鏈湴缁存姢 20 鏉″巻鍙叉粦绐椼€?
        tag.putInt("serverInfoSampleSequence", serverInfoSampleSequence);
        // 鍔熻兘锛氭寔涔呭寲闆疯揪缁戝畾鐜╁淇℃伅銆?
        if (radarPlayerUuid != null) {
            tag.putUUID("RadarPlayerUuid", radarPlayerUuid);
        }
        // 鍔熻兘锛氭寔涔呭寲闆疯揪鐢ㄧ殑鎺у埗妞呬笘鐣屽潗鏍囥€?
        tag.putDouble("RadarSeatWorldX", radarControlSeatWorldPos.x);
        tag.putDouble("RadarSeatWorldY", radarControlSeatWorldPos.y);
        tag.putDouble("RadarSeatWorldZ", radarControlSeatWorldPos.z);
    }

    @Override
    public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientpacket) {
        super.read(tag, registries, clientpacket);
        if(tag.contains("RenderStack")) {
            renderStack = ItemStack.parseOptional(registries, tag.getCompound("RenderStack"));
        }
        if(tag.contains("RenderText")) {
            renderText = tag.getString("RenderText");
        }
        if(tag.contains("type")) {
            displaytype = tag.getInt("type");
        }
        if(tag.contains("spinx") && tag.contains("spiny") && tag.contains("offsetx") && tag.contains("offsety") && tag.contains("offsetz")) {
            this.spinx = tag.getInt("spinx");
            this.spiny = tag.getInt("spiny");
            this.offsetx = tag.getInt("offsetx");
            this.offsety = tag.getInt("offsety");
            this.offsetz = tag.getInt("offsetz");
            this.setdata(this.spinx,this.spiny,this.offsetx,this.offsety,this.offsetz);
        }

        if(tag.contains("serverjvm")) {this.serverJVMpercentage = tag.getFloat("serverjvm");}
        // 鍔熻兘锛氫粠鍚屾鏁版嵁鎭㈠ TPS锛屼緵 screentype=1 鐨勬枃瀛楀眰鏄剧ず銆?
        if(tag.contains("tps")) {this.tps = tag.getInt("tps");}
        // 鍔熻兘锛氫粠鍚屾鏁版嵁鎭㈠ PhysTPS锛屼緵 screentype=1 鐨勬枃瀛楀眰鏄剧ず銆?
        if(tag.contains("phystps")) {this.phystps = tag.getInt("phystps");}
        // 鍔熻兘锛氭帴鏀舵湇鍔＄鏈€鏂版牱鏈簭鍙凤紱瀹㈡埛绔細鍦?tick 涓皢鍏惰浆鎴愭湰鍦板巻鍙茬偣銆?
        if (tag.contains("serverInfoSampleSequence")) {
            this.serverInfoSampleSequence = tag.getInt("serverInfoSampleSequence");
        }

        // 鍔熻兘锛氳鍙栭浄杈剧粦瀹氱帺瀹?UUID銆?
        radarPlayerUuid = tag.hasUUID("RadarPlayerUuid") ? tag.getUUID("RadarPlayerUuid") : null;
        // 鍔熻兘锛氳鍙栭浄杈剧敤鎺у埗妞呬笘鐣屽潗鏍囥€?
        radarControlSeatWorldPos = new Vector3d(
                tag.getDouble("RadarSeatWorldX"),
                tag.getDouble("RadarSeatWorldY"),
                tag.getDouble("RadarSeatWorldZ")
        );
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return super.getUpdateTag(registries);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag, registries);
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        withNbtRegistries(registries, () -> read(tag, registries, true));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
