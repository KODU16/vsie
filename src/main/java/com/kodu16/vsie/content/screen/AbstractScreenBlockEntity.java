package com.kodu16.vsie.content.screen;

import com.kodu16.vsie.content.screen.server.ServerInfoGetter;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.network.SerializableDataTicket;

import java.util.UUID;

public abstract class AbstractScreenBlockEntity extends SmartBlockEntity implements GeoBlockEntity {
    private ItemStack renderStack = ItemStack.EMPTY;
    private String renderText = "Hello";
    public int displaytype = 0;//0:雷达 1:服务器信息
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

    public float clientJVMpercentage = 0;
    public float serverJVMpercentage = 0;
    public int tps = 0;
    public int phystps = 0;

    // 功能：为 serverinfo 保存最近 20 次采样记录（每项都存归一化比例，便于客户端统一绘图）。
    private static final int SERVERINFO_HISTORY_LIMIT = 20;
    // 功能：控制采样频率，每 100 tick 记录一次。
    private static final int SERVERINFO_SAMPLE_INTERVAL = 100;
    // 功能：记录已存储的样本数量（最大 20，仅客户端本地维护）。
    private int serverInfoHistorySize = 0;
    // 功能：服务端采样计数器（用于 100 tick 触发一次“最新样本”同步）。
    private int serverInfoSampleTickCounter = 0;
    // 功能：服务端最新样本序号；每产生一个新样本就 +1，并通过 NBT 同步到客户端。
    private int serverInfoSampleSequence = 0;
    // 功能：客户端已消费的最新样本序号；用于确保“每次同步只入队一次历史”。
    private int clientConsumedSampleSequence = -1;
    // 功能：TPS 历史（归一化到 0~1，最大值按 20 计算）。
    private final float[] tpsHistory = new float[SERVERINFO_HISTORY_LIMIT];
    // 功能：PhysTPS 历史（归一化到 0~1，最大值按 60 计算）。
    private final float[] physTpsHistory = new float[SERVERINFO_HISTORY_LIMIT];
    // 功能：服务器内存占用率历史（0~1）。
    private final float[] serverMemoryHistory = new float[SERVERINFO_HISTORY_LIMIT];
    // 功能：客户端内存占用率历史（0~1）。
    private final float[] clientMemoryHistory = new float[SERVERINFO_HISTORY_LIMIT];

    // 功能：雷达屏幕绑定的控制椅玩家 UUID，用于客户端反查对应玩家的 ClientData。
    private UUID radarPlayerUuid;
    // 功能：缓存控制椅世界坐标，供客户端将周围船只投影到屏幕雷达上。
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

    // 功能：读取当前雷达绑定玩家 UUID。
    public UUID getRadarPlayerUuid() {
        return radarPlayerUuid;
    }

    // 功能：写入当前雷达绑定玩家 UUID，并触发方块实体同步。
    public void setRadarPlayerUuid(UUID radarPlayerUuid) {
        this.radarPlayerUuid = radarPlayerUuid;
        setChanged();
    }

    // 功能：检查雷达是否已经绑定玩家。
    public boolean hasRadarPlayer() {
        return radarPlayerUuid != null;
    }

    // 功能：读取控制椅的世界坐标缓存。
    public Vector3d getRadarControlSeatWorldPos() {
        return new Vector3d(radarControlSeatWorldPos);
    }

    // 功能：更新控制椅世界坐标缓存，并触发方块实体同步。
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

                // 功能：客户端检测到服务端“新样本序号”后，将最新样本推入本地 20 条历史并删除最旧数据。
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

                // 功能：服务端每 100 tick 仅同步“一个最新样本”，避免每次都传整段历史 NBT。
                this.serverInfoSampleTickCounter++;
                if (this.serverInfoSampleTickCounter >= SERVERINFO_SAMPLE_INTERVAL) {
                    this.serverInfoSampleTickCounter = 0;
                    this.serverInfoSampleSequence++;
                    setChanged();
                }
            }
        }
    }

    // 功能：向历史数组追加一条记录；满 20 条后左移一格，始终保留最新 20 条。
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

    // 功能：对外提供历史数据长度，供渲染层按有效样本数量绘制。
    public int getServerInfoHistorySize() {
        return serverInfoHistorySize;
    }

    // 功能：对外提供 TPS 历史比例数组。
    public float[] getTpsHistory() {
        return tpsHistory;
    }

    // 功能：对外提供 PhysTPS 历史比例数组。
    public float[] getPhysTpsHistory() {
        return physTpsHistory;
    }

    // 功能：对外提供服务器内存历史比例数组。
    public float[] getServerMemoryHistory() {
        return serverMemoryHistory;
    }

    // 功能：对外提供客户端内存历史比例数组。
    public float[] getClientMemoryHistory() {
        return clientMemoryHistory;
    }

    // 功能：将浮点值限制在 0~1，避免采样异常导致绘制越界。
    private static float clamp01(float value) {
        if (value < 0f) return 0f;
        return Math.min(value, 1f);
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
        // 功能：仅同步最新样本序号，让客户端基于该序号在本地维护 20 条历史滑窗。
        tag.putInt("serverInfoSampleSequence", serverInfoSampleSequence);
        // 功能：持久化雷达绑定玩家信息。
        if (radarPlayerUuid != null) {
            tag.putUUID("RadarPlayerUuid", radarPlayerUuid);
        }
        // 功能：持久化雷达用的控制椅世界坐标。
        tag.putDouble("RadarSeatWorldX", radarControlSeatWorldPos.x);
        tag.putDouble("RadarSeatWorldY", radarControlSeatWorldPos.y);
        tag.putDouble("RadarSeatWorldZ", radarControlSeatWorldPos.z);
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
        if(tag.contains("type")) {
            displaytype = tag.getInt("type");
        }
        if(tag.contains("spinx") && tag.contains("spiny") && tag.contains("offsetx") && tag.contains("offfsety") && tag.contains("offsetz")) {
            this.spinx = tag.getInt("spinx");
            this.spiny = tag.getInt("spiny");
            this.offsetx = tag.getInt("offsetx");
            this.offsety = tag.getInt("offsety");
            this.offsetz = tag.getInt("offsetz");
            this.setdata(this.spinx,this.spiny,this.offsetx,this.offsety,this.offsetz);
        }

        if(tag.contains("serverjvm")) {this.serverJVMpercentage = tag.getFloat("serverjvm");}
        // 功能：从同步数据恢复 TPS，供 screentype=1 的文字层显示。
        if(tag.contains("tps")) {this.tps = tag.getInt("tps");}
        // 功能：从同步数据恢复 PhysTPS，供 screentype=1 的文字层显示。
        if(tag.contains("phystps")) {this.phystps = tag.getInt("phystps");}
        // 功能：接收服务端最新样本序号；客户端会在 tick 中将其转成本地历史点。
        if (tag.contains("serverInfoSampleSequence")) {
            this.serverInfoSampleSequence = tag.getInt("serverInfoSampleSequence");
        }

        // 功能：读取雷达绑定玩家 UUID。
        radarPlayerUuid = tag.hasUUID("RadarPlayerUuid") ? tag.getUUID("RadarPlayerUuid") : null;
        // 功能：读取雷达用控制椅世界坐标。
        radarControlSeatWorldPos = new Vector3d(
                tag.getDouble("RadarSeatWorldX"),
                tag.getDouble("RadarSeatWorldY"),
                tag.getDouble("RadarSeatWorldZ")
        );
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
