package com.kodu16.vsie.content.shield;

import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.joml.Vector3f;

import java.util.List;

public class ShieldGeneratorBlockEntity extends SmartBlockEntity {
    public SmartFluidTankBehaviour tank;
    public ShieldGeneratorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }
    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tank = SmartFluidTankBehaviour.single(this, 200);
        behaviours.add(tank);
    }

    public BlockPos linkedcontrolseatpos = new BlockPos(0,0,0);
    double RADIUS = 3;
    public int maxreceiverate = 100;
    public EnergyStorage energyStorage = new EnergyStorage(
            100000,    // 鏈€澶у閲?(capacity)
            maxreceiverate,      // 鏈€澶ф帴鏀堕€熺巼 (max receive)   鍙互璁?Integer.MAX_VALUE 濡傛灉鎯虫棤闄愬埗
            Integer.MAX_VALUE,      // 鏈€澶ц緭鍑洪€熺巼 (max extract)
            0         // 鍒濆鑳介噺
    );

    public void tick(Level level, BlockPos pos, BlockState state, ShieldGeneratorBlockEntity be) {
        if (level.isClientSide || level.getGameTime() % 2 != 0) return;

        Vec3 center = Vec3.atCenterOf(pos);
        AABB searchBox = new AABB(this.getBlockPos()).inflate(RADIUS + 6.0); // 澶氭悳涓€鐐癸紝闃叉楂橀€熷疄浣撲竴甯х┛杩囧幓

        // 鏍稿績锛氬彧绛涢€夆€滄病鏈夌敓鍛藉€?+ 閫熷害澶熷揩 + 涓嶆槸鐜╁涔熶笉鏄洈鐢叉灦鈥濅箣绫荤殑瀹炰綋
        level.getEntitiesOfClass(Entity.class, searchBox, entity -> {
            if (entity.isRemoved() || entity instanceof LivingEntity)
                return false;

            // 閫熷害闃堝€硷紝鍙皟锛堝崟浣嶏細鏂瑰潡/鍒伙級
            double speed = entity.getDeltaMovement().length();
            if (speed < 0.25) return false; // 澶參鐨勭洿鎺ュ拷鐣ワ紙姣斿婕傛诞鐨勭墿鍝侊級

            // 璁＄畻鏄惁鏈濇姢鐩鹃鏉?
            Vec3 toEntity = entity.position().subtract(center);
            double dot = entity.getDeltaMovement().normalize().dot(toEntity.normalize());
            return dot < -0.3; // 瓒婅礋璇存槑瓒婃瀵规姢鐩鹃鏉ワ紙-0.3~0.6 涔嬮棿璋冭妭鎵嬫劅锛?
        }).forEach(entity -> {

            Vec3 toEntity = entity.position().subtract(center);
            double distSq = toEntity.lengthSqr();

            if (distSq > RADIUS * RADIUS || distSq < 0.25) return;

            if(getEnergy().getEnergyStored()>20000)
            {
                // 鎷︽埅锛?
                entity.discard(); // 鐩存帴鍒犻櫎锛屽吋瀹?99% 鐨勬ā缁勫疄浣?
                // 绮掑瓙浜ょ偣
                Vec3 hitDir = toEntity.normalize();
                Vec3 hitPoint = center.add(hitDir.scale(RADIUS));
                spawnRippleParticles((ServerLevel) level, hitPoint, hitDir);

                // 鍙€夛細鎾斁闊虫晥
                level.playSound(null, hitPoint.x, hitPoint.y, hitPoint.z,
                        SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS,
                        1.0f, 1.2f + level.random.nextFloat() * 0.4f);
                getEnergyStorage().extractEnergy(20,false);
            }
        });
    }

    private static void spawnRippleParticles(ServerLevel level, Vec3 hitPoint, Vec3 hitDir) {
        int numRings = 3;
        int particlesPerRing = 16;
        double speed = 0.02;

        // 鏋勯€犱竴涓笌 hitDir 鍨傜洿鐨勫眬閮ㄥ潗鏍囩郴 (u, v)
        Vec3 w = hitDir.normalize();
        Vec3 arbitrary = Math.abs(w.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 u = w.cross(arbitrary).normalize();   // 绗竴涓浜ゅ悜閲?
        Vec3 v = w.cross(u).normalize();           // 绗簩涓浜ゅ悜閲忥紙涔熷瀭鐩翠簬 w锛?

        for (int ring = 0; ring < numRings; ring++) {
            double ringRadius = 0.2 + ring * 0.4;
            for (int i = 0; i < particlesPerRing; i++) {
                double angle = (i * 2 * Math.PI) / particlesPerRing;
                Vec3 localOffset = u.scale(Math.cos(angle) * ringRadius)
                        .add(v.scale(Math.sin(angle) * ringRadius));

                // 璁＄畻绮掑瓙浣嶇疆
                Vec3 pos = hitPoint.add(localOffset);

                // 璁＄畻婕傛诞閫熷害
                Vec3 outwardMovement = hitDir.scale(0.05); // 鍚戝婕傛诞鐨勯€熷害

                // 浣跨敤 DustParticleOptions锛堟祬钃濊壊锛?
                Vector3f dustColor = new Vector3f(1.0f, 0.8f, 1.0f);  // 娴呰摑鑹?RGB
                DustParticleOptions dustParticle = new DustParticleOptions(dustColor, 1.0f); // 娴呰摑鑹?
                level.sendParticles(dustParticle,
                        pos.x, pos.y, pos.z,
                        1, outwardMovement.x, outwardMovement.y, outwardMovement.z, speed);

                // 浣跨敤 Glow 鍙戝厜绮掑瓙
                level.sendParticles(ParticleTypes.GLOW,
                        pos.x, pos.y, pos.z,
                        1, outwardMovement.x, outwardMovement.y, outwardMovement.z, speed);
            }
        }
    }

    // 鍔熻兘锛氭彁渚涚粰 NeoForge 1.21.1 capability 娉ㄥ唽鍣ㄧ殑 FE 鍌ㄨ兘鎺ュ彛瀹炰緥銆?
    public IEnergyStorage getEnergyCapability() {
        return energyStorage;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientpacket) {
        super.write(tag, registries, clientpacket);
        tag.putInt("Energy", getEnergy().getEnergyStored());
        writeVec3(tag, "controlpos", linkedcontrolseatpos);
    }

    @Override
    public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientpacket) {
        super.read(tag, registries, clientpacket);
        if (tag.contains("Energy")) {
            energyStorage.receiveEnergy(tag.getInt("Energy"), false);
        }
        readVec3(tag, "controlpos");
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
        read(tag, registries, true);
    }

    // 鏂逛究澶栭儴鐩存帴璋冪敤锛堜緥濡?tick銆丟UI銆乄aila 绛夛級
    public EnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    // 鎴栫洿鎺ヨ繑鍥?IEnergyStorage 鎺ュ彛
    public IEnergyStorage getEnergy() {
        return energyStorage;
    }

    private void writeVec3(CompoundTag nbt, String key, BlockPos position) {
        CompoundTag vecTag = new CompoundTag();
        vecTag.putInt("x", position.getX());
        vecTag.putInt("y", position.getY());
        vecTag.putInt("z", position.getZ());
        nbt.put(key, vecTag);
    }

    private void readVec3(CompoundTag nbt, String key) {
        // 鍔熻兘锛氭寜 CompoundTag 缁撴瀯璇诲彇鎺у埗搴ф爣锛岄伩鍏嶅洜绫诲瀷涓嶅尮閰嶅鑷磋仈鍔ㄤ綅缃涪澶便€?
        if (!nbt.contains(key, Tag.TAG_COMPOUND)) return;
        CompoundTag vecTag = nbt.getCompound(key);
        int x = vecTag.getInt("x");
        int y = vecTag.getInt("y");
        int z = vecTag.getInt("z");
        this.linkedcontrolseatpos = new BlockPos(x, y, z);
        LogUtils.getLogger().warn("shield linked controlseat pos:"+this.linkedcontrolseatpos);
    }

}
