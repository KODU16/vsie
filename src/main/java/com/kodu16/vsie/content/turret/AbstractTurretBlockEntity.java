package com.kodu16.vsie.content.turret;

import com.kodu16.vsie.content.cooldown.FireCooldown;
import com.kodu16.vsie.foundation.LoadedChunkRaycast;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.foundation.Vec;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.constant.dataticket.SerializableDataTicket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.util.RenderUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;

import org.slf4j.Logger;

public abstract class AbstractTurretBlockEntity extends SmartBlockEntity implements GeoBlockEntity, MenuProvider, IItemHandlerModifiable {
    Logger LOGGER = LogUtils.getLogger();
    private static final String AMMO_INVENTORY_TAG = "AmmoInventory";
    protected static final int DEFAULT_CONTROL_SEAT_ENERGY_COST_PER_TICK = 50;

    public static SerializableDataTicket<Boolean> TURRET_HAS_TARGET;

    public boolean hasInitialized = false;//闁稿﹦鍘х欢杈╂偖椤愩垹鏅搁柛蹇ｆ珣bstract缂侇偅妲掗～锕傚箥閳ь剟寮垫径澶嬬溄閻庢冻缂氱弧鍕晬?
    public Level level = null;
    public BlockPos pos = this.getBlockPos();
    public BlockState state = this.getBlockState();
    public boolean onShip = false;

    public Vec3 targetPos = new Vec3(0,0,0); //閺夆晜鐟﹀Σ鍝ユ偖椤愶腹鍋撴径瀣仴闁汇劌瀚伴崑鍛▔椤忓棙绐楅柡宥呮川濞堟垶鎷呭鍥╂瀭
    public double targetDistance;

    public double getTargetDistance() {
        return targetDistance;
    }

    public @Nullable LivingEntity targetentity;
    private @Nullable SubLevel selectedtargetShip;
    public List<Vector3d> targetPreVelocity = new ArrayList<Vector3d>();

    public int aimtype = 0; //0闁挎稒姘ㄩ埞?1闁挎稒鑹鹃悿鍕媴?2闁挎稒淇洪崺鐐哄矗?

    public static SerializableDataTicket<Float> XROT; //閺夆晜鐟﹀Σ鎼佸礉閵娧勬毎閻犱緤绱曢悾濠氭偨閵娧勭暠
    public static SerializableDataTicket<Float> YROT;
    public final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this); // 闁告梻鍠曢崗姗€鏁嶅顐ょ闁伙絾鐟㈤埀顒佺矋濡叉悂宕ラ敂鑺ョ畳闁烩晩鍠楅悥锝夊灳濠靛牊鐣遍柛鏂诲妿閺侀箖宕ョ仦缁㈠妱闁哄秴娲╅鍥晬鐏炶棄绠甸梻鍕╁€曢?Mekanism 闁活澀绲婚崜鎶芥⒖閸℃ê鐏囨繛澶堝姀琚欓柣銊ュ缁堕鎸ч弽銉㈠亾?


    public static Vector3d pivotPoint = new Vector3d(); // 婵☆垪鈧磭鈧攱绋夐鐘崇暠闁哄鍨奸柊閬嶆倷?婵縿鍊曢幃妤佸濮橆厼绁︽慨婵勫€涢崵婊堝礉閵婎煈鍚€缂佺姵顨嗛悘鎴炴姜鐎电浠柣銊ュ娴滃摜绮?

    public int idleTicks = 0;
    protected double fireCooldownValue = -1.0D;
    // Function: every ordinary turret gets a shared 9-slot ammo buffer; energy turrets reject all inserts.
    private final ItemStackHandler ammoInventory = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return acceptsAmmoStack(stack);
        }
    };
    // 闁告梻鍠曢崗姗€鏁嶅鎰佸敹鐟滅増娲滈崑鏍矗閿濆洣绱戦柣鎺撴緲婢ф寧鎷呭▎鎰枖缂佲偓閻戞ɑ顦ч梻鍌濇彧缁辨瑩宕￠弴姘辩Т闁挎稒顒甶ck闁挎稑顧€缁辨繈鎮介妸銈囪壘閻庡湱鍋熼獮鍥灳濠婂啰纾婚柣蹇ｅ亜閹顕欓幆鎵闁绘柨瀚导鍐灳濠靛洦娅忛柡瀣殠閳?
    public int muzzleFlashTicks = 0;

    // 闁告梻鍠曢崗姗€鏁嶅鎰佸殺闁告瑦鐗滈惌鎴犫偓娑欏姉閸?firepoint 闁秆勫姈閻栵綁鏁嶅畝鍐闁搞儳鍋涙竟鍥嫉椤掑嫪缂夐柛蹇撶Т椤﹀鏌堥妸锕€澹堝鑸电墧閹便劑寮ㄩ悷鏉挎暥闂侇喓鍔庢慨鎼佸箑娴ｇ鍋?
    // 闁告梻鍠曢崗姗€鏁嶅顐ょ閻庢稒锚椤撳綊骞嬫搴紓濞戞挸锕ｇ槐鍫曟儍?firepoint 闁秆勫姈閻栵綁鏁嶅畝鈧惌鎴犫偓娑欏姉閸嬫牕顕ｉ埀顒勬倶椤愶絾顦ч柣鈺佺摠鐢瓨鎷呭鈧拹鐔衡偓娑欏姇閼村﹪鎮介悢绋跨亣闁绘劙鈧稑鈻忛柣顫妸閳?
    private Vector3d FirePoint = null;
    // 闁告梻鍠曢崗姗€鏁嶅鎰佸敹鐟滅増娲嶉埀顒佺矊椤曨噣鎳滈幏灞界厴鐎殿喒鍋撻柣蹇ｅ亐閳ь剚绻冨鍌氣柦閸喚绉奸柛鎾崇У濠€鐐哄触閹存繄娈哥紒鐐椤ュ懎霉鐎ｎ亜鐓傞柣銊ュ閺岀喖宕稿Δ鈧妤呭冀閸ラ绀夊ù鐘叉噸缁绘岸鎮惧▎鎰粯閺夆晜鍨崇粩鏉戔枎閿涘嫮娉㈤柡瀣玻缁辨瑩寮悩缁樹粯 NBT 闁告艾鏈鐐烘晬婢跺牃鍋?
    private BlockPos lastShipShotHitBlockPos = BlockPos.ZERO;
    // 闁告梻鍠曢崗姗€鏁嶅顓犲灱閻犱焦澹嬮埀顒佺矋濠€鏉库枎閳ユ剚鍤犻柤鍛婂閸╃偟浜搁崟顓炴疇闁哄嫷鍨伴幆渚€宕楅崼婵囧殥濞戞搩鍘虹花锟犳嚊椤忓洭鐓╅柟纰樺亾闁革负鍔忛崺鐐存媴閹绢垪鍋撳┑鎾剁闁活潿鍔嬬花顒勬⒓缂佹﹩鍓鹃悹鍥跺灟濠碘偓闁煎浜ｉ棅鈺呮嚋妫颁胶绉奸柣銊ュ缁辨垿鎮橀鐘亾?
    private boolean shipShotBlockedBySelfShip = false;

    public Vector3d getFirePoint() {
        return FirePoint;
    }

    public BlockPos getLastShipShotHitBlockPos() {
        return lastShipShotHitBlockPos;
    }

    private static final double SEARCH_RADIUS = 128.0;

    public Vec3 currentworldpos = new Vec3(this.getBlockPos().getX(), this.getBlockPos().getY(), this.getBlockPos().getZ());
    protected TurretData turretData;

    public Vector3d worldXDirection = new Vector3d(0,0,0);
    public Vector3d worldYDirection = new Vector3d(0,0,0);
    public Vector3d worldZDirection = new Vector3d(0,0,0);

    protected AbstractTurretBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        // 闁告帗绻傞～鎰板礌?turretData
        this.turretData = new TurretData();
    }

    public TurretData getData() {
        if (turretData == null) { turretData = new TurretData(); }
        return turretData;
    }

    public void modifyTargetType(int type) {
        // 闁告梻鍠曢崗姗€鏁嶅顓犳Ж婵炲棌鈧弶鍙忛柡鈧憴鍕槯闂侇喛妫勯悿鍕籍閹偊鍤㈤柛娆愮墬閺岀喖宕稿Δ鈧悿鍕媴閹惧磭绉奸柛?level闁挎稑鐭傛导鈺呭礂瀹ュ嫬鈻忛柣顫妽閻庮垶鏌呴悩铏焸缂傚倹鎸搁悺銊╂儍閸曨厸鏁?level 閻庝絻澹堥崵褔骞愭径鎰唉闁哄啰濮甸弲銉╁Υ?
        this.level = this.getLevel();
        if (level == null || level.isClientSide) { return; }

        TurretData data = getData();

        if(type==4){
            this.aimtype = 2;
            data.flip(data.TARGET_SHIP);
            if ( data.isTargetsShip() ) { data.reset(( data.TARGET_HOSTILE | data.TARGET_PASSIVE | data.TARGET_PLAYER )); }
        }
        else{
            this.aimtype = 1;
            if(type==1){ data.flip(data.TARGET_HOSTILE); }
            if(type==2){ data.flip(data.TARGET_PASSIVE); }
            if(type==3){ data.flip(data.TARGET_PLAYER); }
        }
        if (data.getTargetStatus()==data.TARGET_MANUAL) { this.aimtype = 0; }

        else if ((data.getTargetStatus()&(~data.TARGET_SHIP))!=0) { data.reset(data.TARGET_SHIP); }
    }

    public void modifydefaultspin(int spinx, int spiny) {
        this.defaultspinx = spinx;
        this.defaultspiny = spiny;
    }

    public void setAimLimits(int minX, int maxX, int minY, int maxY) {
        getData().setAimLimits(minX, maxX, minY, maxY);
    }

    public boolean supportsBlockDestructionToggle() {
        return true;
    }

    public boolean breaksBlocksEnabled() {
        return getData().isBreaksBlocks();
    }

    public void setBreaksBlocksEnabled(boolean enabled) {
        getData().setBreaksBlocks(enabled);
    }

    public void toggleBreaksBlocksEnabled() {
        setBreaksBlocksEnabled(!breaksBlocksEnabled());
    }

    protected boolean breakTurretTargetBlockAsMined(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        BlockState state = serverLevel.getBlockState(pos);
        if (!serverLevel.isLoaded(pos) || state.isAir() || state.getDestroySpeed(serverLevel, pos) < 0.0F) {
            return false;
        }
        // Function: turret terrain damage should look like a mined block break and never spawn item drops.
        serverLevel.levelEvent(2001, pos, Block.getId(state));
        boolean destroyed = serverLevel.destroyBlock(pos, false);
        if (destroyed) {
            maybeTriggerTurretBlockBreakTnt(serverLevel, pos);
        }
        return destroyed;
    }

    protected float getBlockBreakTntChance() {
        return 0.0F;
    }

    protected float getBlockBreakTntPower() {
        return 4.0F;
    }

    protected void maybeTriggerTurretBlockBreakTnt(ServerLevel level, BlockPos pos) {
        float chance = Mth.clamp(getBlockBreakTntChance(), 0.0F, 1.0F);
        if (chance <= 0.0F || level.random.nextFloat() >= chance) {
            return;
        }
        // Function: turret classes can opt into a TNT-like follow-up blast after a successful block break.
        level.explode(
                null,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                getBlockBreakTntPower(),
                false,
                Level.ExplosionInteraction.TNT
        );
    }

    public void tick() {
        Level level = this.getLevel();
        if (level == null || level.isClientSide()) { return; }
        this.level = level;

        if (!hasInitialized){
            BlockPos pos = this.getBlockPos();
            BlockState state = this.getBlockState();
            Initialize.initialize(level,pos,state,pivotPoint);

            hasInitialized = true;
            return;
        }

        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level,pos);

        if (subLevel != null) {
            Vector3d pivotoffsetworld = subLevel.logicalPose().transformNormal(this.turretData.getBasePivotOffset().normalize().mul(this.turretData.getBasePivotOffset().length())
            );
            this.turretData.setWorldPivotOffset(pivotoffsetworld);

            // 闁告梻鍠曢崗姗€鏁嶅顒€鏋庨柛妤€鐡ㄥ鍌炴⒒缂堢姷鐭岄柣顫妺缁剛绮嬫担璇″壘鐎殿喒鍋撻柣蹇ｅ亾缁辨繃绋夊鍛櫃闂傚啰绮弻鍥╂閵忊剝娅曞☉鎾虫唉濞村棝宕ラ幋锔瑰亾閺勫繒甯嗛柕鍡曢潬
            // 闁告梻鍠曢崗姗€鏁嶅顓犳Ж tick 閻炴稒婢橀崳娲倷椤旂厧缍撻柣蹇ｅ亞閸旀瑩寮伴崜褋浠涢悹浣插墲濡炲倿鏁嶅畝鍐惧悁闁哄啫澧庣划銊╁级閻旈攱鍊甸柤濂変簻婵晠姊鹃幇顖涱棏闁诲浚鍋嗛崝娆戜沪閸屾ǚ鍋?
            if (muzzleFlashTicks > 0) {
                muzzleFlashTicks = muzzleFlashTicks - 1;
            }
            // 闁告梻鍠曢崗姗€鏁嶅杈╁煚濞戞挴鍋撻柛鎺楁敱閺屽﹪鎮欓纰辨晩濞戞挻鐗滈弲顐﹀锤閹邦厾鍨奸柨娑樿嫰閸ｈ櫣浜?tick 濞戞挾绮粊锔剧矙鐎ｎ亜鐎婚柡鈧姘兼Щ闁哄鍊哥€规娊濡?
            // Function: ordinary turret aiming and shot traces use the raised pivot/muzzle origin, not the block body center.
            currentworldpos = getTurretAimOriginWorld();
            // 闁告梻鍠曢崗姗€鏁嶅杈╁煚濞戞挴鍋撳璺哄閹﹪鎯勯鐣屽灱闁瑰吋绮庨崒銊╂晬瀹€鍐仧闁哄啰濮靛﹢渚€寮崼銏＄獥闁哄秴娲ら崹顖滄媼閳哄啫浠忓┑澶嬫煥濞叉牞銇愰幒妤冨笡閻犱降鍊涢～妤佹償閿旇　鍋?
            acquireTargetByAimType();
            tryInvalidateTarget();
            tickFireCooldown(hasValidTarget());

            if (hasValidTarget()) {
                // 闁告梻鍠曢崗姗€鏁嶅杈ㄦ▕闁硅翰鍊濋埀顒傚枎鐎规娊鏌岄崶銊у缂佹劖顨呰ぐ娑㈡晬鐏炶壈绀嬬€殿喖缍婃禍鐐紣閸曨剛銈撮柟缁樺姃缁剁敻寮甸埀顒佹交閹寸姌鈺呭礉閵娿劎歇闁告棁锟ラ埀?
                appendTargetVelocitySample();
                // 闁告梻鍠曢崗姗€鏁嶅杈╁煚濞戞挴鍋撻柡鍥х摠閺屽﹨銇愰幘鍐差枀闁烩晩鍠楅悥锝夋倷閻у摜绀夐梺顒€鐏濋崢銈団偓鍦仒缂?闁煎憡濯介崺鐐烘煂瀹ュ拋妲婚柛鎺戞閺侇喗绂掗敐鍥╁灣闁?
                updateCurrentTargetPos();

                targetPos = getShootLocation(targetPos, targetPreVelocity, level, currentworldpos);
                updateTargetRot();
                this.xRot0 = closestReachableX(xRot0, getMaxSpinSpeed(), targetxrot);
                this.yRot0 = closestReachableY(yRot0, getMaxSpinSpeed(), targetyrot);
                if (xOK && yOK) {
                    fireWhenLocked();
                }
            } else {
                // 闁告梻鍠曢崗姗€鏁嶅顒傜Ъ闁告稏鍔屽ú鍨柦閳╁啯绠掗柡鍫濐槹閺呫儵寮仦鍏肩溄闁哄啳顔愮槐婵嬬嵁閾忣偆鎷ㄩ柛銉у仜閸╁矂鎮介妸锕€鐓曢悹浣稿⒔閻ゅ棝鎯冮崟顖滃笡閻犱降鍊撻崚濠冪?闁稿绻楅崺鍛喆閹哄鍋?
                returnToDefaultRotation();
            }
            //LogUtils.getLogger().warn("targetx:"+targetxrot+"y:"+targetyrot+"currentx:"+xRot0+"y:"+yRot0+"OK?"+xOK+yOK);
            this.setAnimData(XROT, xRot0);
            this.setAnimData(YROT, yRot0);
            this.markUpdated();
        }
    }

    // 闁告梻鍠曢崗姗€鏁嶅顓炵樆鐟滅増鎸告晶鐘垫閵忊剝娅曟俊顖椻偓宕囩閻忓繑绻嗛惁顖炴嚔瀹勬澘绲块柣鈺婂枟閻栵綁鏁嶅畝鍕級闁稿繐绉村﹢?tick 濞戞挸顭烽埀顒佹缁额偅绋夐鐔告疇闁解偓閽樺妯嬮悘?if闁?
    private void acquireTargetByAimType() {
        if (aimtype == 1) {
            tryFindTargetEntity();
        } else if (aimtype == 2) {
            // 闁告梻鍠曢崗姗€鏁嶅鍐茬厒闁肩婀遍崒銊╁极鐏炲墽妲?tick 闂侇喛濮ゆ晶鐣屾偘鐏炶偐顏辨繛鍡忔缁辨繃绂掗妷銈団敀闁革负鍔嶉弲顐︽嚋閺夊灝鐏欓悶娑栧妽缁旇崵绮氶悜妯活槯缂佹柨顑呭畵鍡涙焻閳ь剟宕欓搹鐟板亶闁轰礁鑻懟鐔煎炊閻愯尙绉哄娑欘焾椤撶粯鎱ㄩ幐搴樺亾娴ｇ鍋?
            tryFindtargetShip();
        }
    }

    // 闁告梻鍠曢崗姗€鏁嶅杈╁煚濞戞挴鍋撻柛鎺嬪€栭弻鍥灳濠婂啰绉奸柛鎾崇У濡叉悂宕ラ敂钘夌槷闁哄牆顦板﹢渚€寮崼銏＄獥闁哄秴娲犻埀顒佺缚閳?
    private boolean hasValidTarget() {
        return (aimtype == 1 && isValidTargetEntity(targetentity))
                || (aimtype == 2 && isValidTargetShip(selectedtargetShip));
    }

    // 闁告梻鍠曢崗姗€鏁嶅杈ㄦ▕闁硅翰鍊栧〒鑸靛緞?5 闁哄绱曞ú浼村冀閸ヮ兘鍋撻悢宄邦唺闁告ê妫楄ぐ鍫曟晬鐏炶偐杩斿Λ鏉垮缁佹潙顕ｈぐ鎺嶅闁哄啯婀规繛鍥偨閵婏絺鍋?
    private void appendTargetVelocitySample() {
        if (targetPreVelocity.size() >= 5) {
            targetPreVelocity.remove(0);
        }
        if (aimtype == 1 && isValidTargetEntity(targetentity)) {
            targetPreVelocity.add(new Vector3d(targetentity.getDeltaMovement().x, targetentity.getDeltaMovement().y, targetentity.getDeltaMovement().z));
        } else if (aimtype == 2 && isValidTargetShip(selectedtargetShip)) {
            RigidBodyHandle rigidBodyHandle = RigidBodyHandle.of((ServerSubLevel) selectedtargetShip);
            targetPreVelocity.add(rigidBodyHandle.getLinearVelocity(new Vector3d()));
        }
    }

    // 闁告梻鍠曢崗姗€鏁嶅顓犲闁硅鍠氬ú浼村冀閸モ晞顫﹂柛銊ヮ儐濞插潡寮弶璺ㄧЪ闁告挸绉堕悗顖炲礄閸℃瑥浠柨娑樺缁剁敻宕ユ惔锝囨暰鐎殿喖缍婃禍鐐紣閸曨剛銈村☉鎾冲濡棙娼鍐惧悁缂佺姵銇炴繛鍥偨閵婏絺鍋?
    private void updateCurrentTargetPos() {
        if (aimtype == 1 && isValidTargetEntity(targetentity)) {
            targetPos = new Vec3(
                    targetentity.getX(),
                    targetentity.getY(),
                    targetentity.getZ()
            );
            return;
        }
        if (aimtype == 2 && isValidTargetShip(selectedtargetShip)) {
            // 闁告梻鍠曢崗姗€鏁嶅顒夊殸闁煎憡濯介崺鐐烘儎椤旂晫鍨兼慨?tick 闂侇喕绮欓崳鎼佸棘娴肩补鍋撴径瀣仴闁炽儲绮岃ぐ鑼喆娴ｅ壊妯嗛悶娑栧姂濞间即鎮欓崗澶嗗亾濠垫挾绀夐梺顒€鐏濋崢銈嗘叏鐎ｎ剛鐭掗柛銉у仜閸熸挻绋夋ウ鍨厴闁告瑯浜欓懙鎴ｇ疀閸愵亜浠柕?
            targetPos = getShipAimPoint(selectedtargetShip);
        }
    }

    // 闁告梻鍠曢崗姗€鏁嶅顒佽含闁绘劦鍠栬ぐ娑氣偓鐟版湰閸ㄦ氨鈧數鎳撻崳顖炲籍閹澏鏇㈠矗閹存繄纾婚柣蹇ｅ亾缁辨繈鐛幆閭﹀晭缂傚喚鍠氱划鐑樼▔閳ь剟宕樺畡鏉跨ケ闁?
    private void fireWhenLocked() {
        //LogUtils.getLogger().warn("shooting");
        // 闁告梻鍠曢崗姗€鏁嶅顒€鏋庨柛妤€鐡ㄥ﹢锟犳⒒閺夋垵甯掗悹浣告憸閹撮绱掗鐘插亶闁轰礁濂旂粭宀勫籍鐎ｎ厽绁柨娑樺缁插墽绮嬫担璇″壘闂佹彃绉撮ˇ鎻掝嚕閳ь剟鎮橀鐘亾?
        if (!isFireCooldownReady()) {
            return;
        }
        // 闁告梻鍠曢崗姗€鏁嶅顒€甯掗悹浣侯焾閻℃瑧鐚剧拠璇х矗闁哄嫬绨堕埀顒佺矊缂嶅宕滃鍡樞﹂柛姘鹃檮瀵呮惥閸愯尙纾婚柣蹇ｅ亯缁侇偄鈹冮幇顓熻拫濞寸姴鐏堥埀顒佺箰缁辨瑦淇婇崒姘冲墾闁肩瓔鍨划銊╂晬婢舵稓绀夊☉鎾崇У瀵呮惥閾忣偅顦уù鐘叉噽閻庮垶宕欓崱鏇犵憹閻忓繐瀚崵顕€濡?
        if (!canShootCurrentTarget()) {
            return;
        }
        if (!consumeAmmoForShot()) {
            return;
        }
        if (aimtype == 1) {
            targetDistance = Vec.Distance(currentworldpos, targetPos);
            shootentity();
            consumeFireCooldown();
            // 闁告梻鍠曢崗姗€鏁嶅顒傛澖濞达絾鎸惧ú浼村冀閸パ呯；闁诲浚鍋勯幃妤佺┍濠靛洤鐦?0.5 缂佸甯為崑鏍矗閿濆洣绱戦柣鎺斿濡绮堥悮瀵哥20tick/s * 0.5s = 10tick闁挎稑顦埀?
            muzzleFlashTicks = 10;
        } else if (aimtype == 2) {
            targetDistance = Vec.Distance(currentworldpos, targetPos);
            // 闁告梻鍠曢崗姗€鏁嶅顐ょ煂闁革负鍔岄顕€鎳滈幏灞界厴閻忓繐瀚崵顕€寮懜闈涒挃閻炴稑濂旂粩鏉戔枎?clip 婵☆偀鍋撴繛鏉戭儜缁辨繈鐛幆閭﹀敹鐟滅増娲栫紞瀣礈瀹ュ棙绡傞柛姘灥閹斥剝绋夐鐘崇暠闁哄倻鎳撳锟犲锤閹邦厾鍨奸柕?
            recordShipShotHitBlockPos();
            // 闁告梻鍠曢崗姗€鏁嶅顒夋搐闁哄绮岄惃鐘电棯閸喖甯ラ柛娑欏灊閼垫垿鎳涢鍥叐闁圭鍋撻柛锔哄姀閸╃偞鎷呴幙鍕闁告帗鐟ラ崹鐣屸偓瑙勬皑濞蹭即寮介崶锔剧憹闁告瑯鍨甸～鍡涚嵁閸撲焦绾柟鎭掑劚瑜板洤鈽夐崼鐔告嫳婵炲棌鈧磭纾婚柣蹇ｅ亖閳?
            if (shipShotBlockedBySelfShip) {
                return;
            }
            shootship();
            consumeFireCooldown();
            // 闁告梻鍠曢崗姗€鏁嶅鍐茬厒闁肩婀卞ú浼村冀閸パ呯；闁诲浚鍋勯幃妤呭触鐏炲墽澹夊ǎ鍥ㄧ箖鐎?0.5 缂佸甯為崑鏍矗閿濆洣绱戦柣鎺斿濡绮堥幁鎺嗗亾?
            muzzleFlashTicks = 10;
        }
    }



    // Function: use recent target velocity samples to lead moving targets.
    public abstract Vec3 getShootLocation(Vec3 vec, List<Vector3d> preV, Level lv, Vec3 pos);

    public abstract String getturrettype();

    public boolean isEnergyTurret() {
        return true;
    }

    public @Nullable Item getAmmoItem() {
        return null;
    }

    public abstract double getYAxisOffset();

    public abstract double getcannonlength();

    protected Vec3 getTurretLocalUpDirection() {
        Direction facing = this.getBlockState().getValue(AbstractTurretBlock.FACING);
        // Function: Yoffset is authored in turret-local space, so map it onto the mounted block's local up axis first.
        return Vec3.atLowerCornerOf(facing.getOpposite().getNormal());
    }

    protected Vec3 getTurretAimOriginWorld() {
        Vec3 localOrigin = Vec3.atCenterOf(this.getBlockPos()).add(getTurretLocalUpDirection().scale(getYAxisOffset()));
        Level level = this.getLevel();
        if (level == null) {
            return localOrigin;
        }

        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, this.getBlockPos());
        // Function: compute the server-side aim origin in turret-local coordinates before any sublevel rotation is applied.
        return subLevel == null ? localOrigin : subLevel.logicalPose().transformPosition(localOrigin);
    }

    public Vec3 getHudAimOriginWorld() {
        // Function: HUD marker projection uses the same raised aim origin as real turret firing.
        return getTurretAimOriginWorld();
    }

    protected @Nullable Vec3 getCannonMuzzleWorld(Vec3 target) {
        Vec3 origin = getTurretAimOriginWorld();
        Vec3 direction = getCurrentBarrelDirectionWorld();
        if (direction == null) {
            direction = target.subtract(origin);
        }
        if (direction.lengthSqr() < 1.0E-6D) {
            return null;
        }

        // Function: project cannon length from the raised origin along the current barrel axis.
        return origin.add(direction.normalize().scale(getcannonlength()));
    }

    protected @Nullable Vec3 getCurrentBarrelDirectionWorld() {
        if (this.getLevel() == null) {
            return null;
        }
        return getBarrelDirectionWorldForAngles(xRot0, yRot0);
    }

    public @Nullable Vec3 getRenderedBarrelDirectionWorld() {
        if (this.getLevel() == null) {
            return null;
        }
        // Function: HUD markers follow the same smoothed client-side angles the player is actually seeing.
        return getBarrelDirectionWorldForAngles(prevxrot, prevyrot);
    }

    protected @Nullable Vec3 getBarrelDirectionWorldForAngles(float pitch, float yaw) {
        updateWorldControlAxes();

        double worldYaw = -yaw;
        double worldPitch = pitch;
        double horizontal = Math.cos(worldPitch);
        double localX = Math.sin(worldYaw) * horizontal;
        double localY = Math.sin(worldPitch);
        double localZ = Math.cos(worldYaw) * horizontal;

        // Function: rebuild the muzzle axis from the same basis used by server-side aiming.
        Vec3 direction = new Vec3(worldZDirection.x, worldZDirection.y, worldZDirection.z).scale(localX)
                .add(new Vec3(worldYDirection.x, worldYDirection.y, worldYDirection.z).scale(localY))
                .add(new Vec3(worldXDirection.x, worldXDirection.y, worldXDirection.z).scale(localZ));
        return direction.lengthSqr() < 1.0E-6D ? null : direction.normalize();
    }

    public abstract float getMaxSpinSpeed();

    public abstract int getCoolDown();

    public FireCooldown getFireCooldown() {
        return FireCooldown.cool1(getCoolDown());
    }

    public int getenergypertick() {
        // Function: linked turrets consume a baseline control-seat FE upkeep even before subclasses tune it.
        return DEFAULT_CONTROL_SEAT_ENERGY_COST_PER_TICK;
    }

    public int getControlSeatEnergyCostPerTick() {
        return getenergypertick();
    }

    public abstract void shootentity();

    public abstract void shootship();

    // 闁告梻鍠曢崗姗€鏁嶅顒傛憤缂侇偉顕цぐ鑼啺閸℃鏅搁悹鍥ュ劜閺岀喎鈻旈弴鐔蜂粯闁告帒鐏堥埀顒佺矋濡叉悂宕ラ敃鈧崢鎴犳媼閸涘﹥鎷?tick 鐎殿喒鍋撻柣蹇ｅ亐閳ь剚绻愮槐婵囶渶濡鍚囧┑顔碱儑缁捇宕楁担绛嬪晠闁?
    protected boolean canShootCurrentTarget() {
        return hasAmmoReady();
    }

    public boolean hasAmmoInventorySlots() {
        return !isEnergyTurret() && getAmmoItem() != null;
    }

    protected boolean hasAmmoReady() {
        if (!hasAmmoInventorySlots()) {
            return true;
        }
        for (int slot = 0; slot < ammoInventory.getSlots(); slot++) {
            if (!ammoInventory.extractItem(slot, 1, true).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    protected boolean consumeAmmoForShot() {
        if (!hasAmmoInventorySlots()) {
            return true;
        }
        for (int slot = 0; slot < ammoInventory.getSlots(); slot++) {
            ItemStack extracted = ammoInventory.extractItem(slot, 1, false);
            if (!extracted.isEmpty()) {
                setChanged();
                return true;
            }
        }
        return false;
    }

    protected boolean acceptsAmmoStack(ItemStack stack) {
        Item ammoItem = getAmmoItem();
        return ammoItem != null && hasAmmoInventorySlots() && stack.is(ammoItem);
    }

    public void dropStoredAmmo(Level level, BlockPos pos) {
        if (!hasAmmoInventorySlots()) {
            return;
        }
        // Function: preserve buffered ammo when a non-energy turret is broken.
        for (int slot = 0; slot < ammoInventory.getSlots(); slot++) {
            ItemStack stack = ammoInventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                net.minecraft.world.level.block.Block.popResource(level, pos, stack.copy());
                ammoInventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    public IItemHandlerModifiable getItemHandler() {
        return this;
    }

    // 闁告梻鍠曢崗姗€鏁嶅顒€甯掗悹浣侯焾閻℃瑧鐚剧拠璇х矗闁?Geo 婵☆垪鈧磭鈧攱绋?turret 濡ょ姰鍔戦娑㈡儍閸曨剛浜奸弶鐐差嚟閸嬶綁鏁嶉崼婵嗙濞达絽绋勭槐鏉课熼垾宕団偓鐑藉磽韫囨洜顦遍柨娑樿嫰鐢偊鎮欓柅娑滅闁哄倻鎳撳锟犲嫉椤掆偓濠€鎾储閻斿搫浠柨娑橆槶閳?
    protected void tickFireCooldown(boolean fireRequested) {
        FireCooldown cooldown = getFireCooldown();
        if (idleTicks > 0) {
            idleTicks--;
        }
        ensureFireCooldownValue(cooldown);
        if (cooldown.usesValue() && !fireRequested) {
            fireCooldownValue = Math.min(cooldown.maxValue(), fireCooldownValue + cooldown.recoveryPerTick());
        }
    }

    protected boolean isFireCooldownReady() {
        FireCooldown cooldown = getFireCooldown();
        ensureFireCooldownValue(cooldown);
        return idleTicks <= 0 && (!cooldown.usesValue() || fireCooldownValue > 0);
    }

    protected void consumeFireCooldown() {
        FireCooldown cooldown = getFireCooldown();
        ensureFireCooldownValue(cooldown);
        idleTicks = cooldown.intervalTicks();
        if (cooldown.usesValue()) {
            fireCooldownValue = Math.max(0, fireCooldownValue - 1);
        }
    }

    public int getCooldownHudValue() {
        FireCooldown cooldown = getFireCooldown();
        ensureFireCooldownValue(cooldown);
        return cooldown.usesValue() ? Mth.floor(fireCooldownValue) : Math.max(0, idleTicks);
    }

    public int getCooldownHudMax() {
        FireCooldown cooldown = getFireCooldown();
        return cooldown.usesValue() ? cooldown.maxValue() : cooldown.intervalTicks();
    }

    public boolean isCooldownHudRemaining() {
        return !getFireCooldown().usesValue();
    }

    private void ensureFireCooldownValue(FireCooldown cooldown) {
        if (!cooldown.usesValue()) {
            return;
        }
        if (fireCooldownValue < 0) {
            fireCooldownValue = cooldown.maxValue();
        } else if (fireCooldownValue > cooldown.maxValue()) {
            fireCooldownValue = cooldown.maxValue();
        }
    }

    protected Vector3d getTurretPivotInGeoPixels() {
        return new Vector3d(0.0, 0.0, 0.0);
    }

    // 闁告梻鍠曢崗姗€鏁嶅顒€甯掗悹浣侯焾閻℃瑧鐚剧拠璇х矗闁?Geo 婵☆垪鈧磭鈧攱绋?cannon 濡ょ姰鍔戦娑㈡儍閸曨剛浜奸弶鐐差嚟閸嬶綁鏁嶉崼婵嗙濞达絽绋勭槐鏉课熼垾宕団偓鐑藉磽韫囨洜顦遍柨娑樿嫰鐢偊鎮欓柅娑滅闁哄倻鎳撳锟犲嫉椤掆偓濠€鎾储閻斿搫浠柨娑橆槶閳?
    protected Vector3d getCannonPivotInGeoPixels() {
        return new Vector3d(0.0, 0.0, 0.0);
    }


    public void updateenemy(ArrayList<SubLevel> enemyshipsData) {
        this.getData().enemyShipsData = enemyshipsData;
    }

    public void clearControlSeatTargeting() {
        // Function: power loss must immediately clear stale targets so autonomous turrets cannot keep firing on old data.
        this.getData().enemyShipsData = new ArrayList<>();
        this.targetentity = null;
        this.selectedtargetShip = null;
        this.targetPreVelocity.clear();
        this.targetPos = Vec3.ZERO;
        this.targetDistance = 0.0D;
    }

    protected boolean shouldApplyAutoAimLimits() {
        // Function: base turrets always gate automatic target acquisition through the configured aim windows.
        return true;
    }

    protected void updateWorldControlAxes() {
        Direction facing = this.getBlockState().getValue(AbstractTurretBlock.FACING);
        Vec3 localUp = Vec3.atLowerCornerOf(facing.getOpposite().getNormal());
        Vec3 localForward = switch (facing) {
            case NORTH -> new Vec3(0, 1, 0);
            case SOUTH -> new Vec3(0, -1, 0);
            case WEST, EAST, UP, DOWN -> new Vec3(0, 0, -1);
        };
        Vec3 localRight = switch (facing) {
            case NORTH, DOWN, SOUTH -> new Vec3(1, 0, 0);
            case WEST -> new Vec3(0, -1, 0);
            case EAST -> new Vec3(0, 1, 0);
            case UP -> new Vec3(-1, 0, 0);
        };

        SubLevel ship = ServerShipUtils.getSubLevelAtBlockPos(this.getLevel(), this.getBlockPos());
        if (ship != null) {
            worldXDirection = ship.logicalPose().transformNormal(new Vector3d(localForward.x, localForward.y, localForward.z)).normalize();
            worldYDirection = ship.logicalPose().transformNormal(new Vector3d(localUp.x, localUp.y, localUp.z)).normalize();
            worldZDirection = ship.logicalPose().transformNormal(new Vector3d(localRight.x, localRight.y, localRight.z)).normalize();
            return;
        }

        worldXDirection = new Vector3d(localForward.x, localForward.y, localForward.z).normalize();
        worldYDirection = new Vector3d(localUp.x, localUp.y, localUp.z).normalize();
        worldZDirection = new Vector3d(localRight.x, localRight.y, localRight.z).normalize();
    }

    protected double[] computeTargetAimAngles(Vec3 targetWorldPos) {
        updateWorldControlAxes();

        Vec3 toTargetWorld = new Vec3(
                targetWorldPos.x - currentworldpos.x,
                targetWorldPos.y - currentworldpos.y,
                targetWorldPos.z - currentworldpos.z
        );
        if (toTargetWorld.lengthSqr() < 1.0E-6D) {
            return null;
        }
        toTargetWorld = toTargetWorld.normalize();

        double localZ = toTargetWorld.dot(new Vec3(worldXDirection.x, worldXDirection.y, worldXDirection.z));
        double localY = toTargetWorld.dot(new Vec3(worldYDirection.x, worldYDirection.y, worldYDirection.z));
        double localX = toTargetWorld.dot(new Vec3(worldZDirection.x, worldZDirection.y, worldZDirection.z));

        double yaw = Math.atan2(localX, localZ);
        double pitch = Math.atan2(localY, Math.sqrt(localX * localX + localZ * localZ));
        return new double[]{pitch, -yaw};
    }

    protected boolean isTargetWithinAimLimits(Vec3 targetWorldPos) {
        if (!shouldApplyAutoAimLimits()) {
            return true;
        }

        double[] aimAngles = computeTargetAimAngles(targetWorldPos);
        if (aimAngles == null) {
            return false;
        }

        TurretData data = getData();
        double xDegrees = Math.toDegrees(aimAngles[0]);
        double yDegrees = Math.toDegrees(aimAngles[1]);
        return isAngleInsideWindow(xDegrees, data.aimLimitMinX, data.aimLimitMaxX)
                && isAngleInsideWindow(yDegrees, data.aimLimitMinY, data.aimLimitMaxY);
    }

    private boolean isAngleInsideWindow(double angleDegrees, int minDegrees, int maxDegrees) {
        // Function: the default [-180, 180] span means unrestricted rotation on that axis.
        if (minDegrees <= TurretData.DEFAULT_AIM_LIMIT_MIN && maxDegrees >= TurretData.DEFAULT_AIM_LIMIT_MAX) {
            return true;
        }

        double normalizedAngle = normalizeDegrees(angleDegrees);
        double normalizedMin = normalizeDegrees(minDegrees);
        double normalizedMax = normalizeDegrees(maxDegrees);
        if (normalizedMin <= normalizedMax) {
            return normalizedAngle >= normalizedMin && normalizedAngle <= normalizedMax;
        }
        // Function: allow wrap-around windows such as [150, -150] for a rear-only firing arc.
        return normalizedAngle >= normalizedMin || normalizedAngle <= normalizedMax;
    }

    private double normalizeDegrees(double degrees) {
        double normalized = degrees % 360.0D;
        if (normalized <= -180.0D) {
            normalized += 360.0D;
        } else if (normalized > 180.0D) {
            normalized -= 360.0D;
        }
        return normalized;
    }

    private void tryInvalidateTarget() {
        if(aimtype==1) {
            if(!isValidTargetEntity(targetentity)) {
                setAnimData(TURRET_HAS_TARGET, false);
                targetentity = null;
                targetDistance = 0;
                targetPreVelocity.clear();
            }
        }
        else if(aimtype==2) {
            if(!isValidTargetShip(selectedtargetShip)) {
                setAnimData(TURRET_HAS_TARGET, false);
                selectedtargetShip = null;
                targetDistance = 0;
                targetPreVelocity.clear();
            }
        }
    }

    public void tryFindTargetEntity() {
        // 闁告梻鍠曢崗姗€鏁嶅鍗炲亶闁轰礁鐭傚Ο浣糕枔閸忓摜鐟濋柛鎰У閺佸ジ宕濋妸銉х；闁诲浚鍋勯崰搴ㄥ础鏉堝墽绀夐梺顒€鐏濋崢銈夊礃瀹勬澘绁卞☉鎾虫捣閸屻劑寮仦钘夊綑闁活潿鍔忛鎼佸极閺夋寧鐝ら悗浣冨閸ぱ囧箮閺嵮冃楅柕?
        if (targetentity != null && targetentity.isAlive()) return; // 闁哄牆顦板鍧楁儎椤旂晫鍨奸悘蹇撳綁缁楀鏌屽鍜佹Щ闁?

        if ((this.getLevel().getGameTime() + this.hashCode()) % 5 != 0) return;

        AABB searchBox = new AABB(
                currentworldpos.x - SEARCH_RADIUS,
                currentworldpos.y - SEARCH_RADIUS,
                currentworldpos.z - SEARCH_RADIUS,
                currentworldpos.x + SEARCH_RADIUS,
                currentworldpos.y + SEARCH_RADIUS,
                currentworldpos.z + SEARCH_RADIUS
        );

        List<LivingEntity> candidates = this.getLevel().getEntitiesOfClass(LivingEntity.class, searchBox, this::isValidTargetEntity);

        if (candidates.isEmpty()) {
            return;
        }

        targetentity = candidates.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(currentworldpos.x, currentworldpos.y, currentworldpos.z)))
                .orElse(null);
        // 闁稿繑濞婇弫顓㈡晬濮樺磭绠归梺鎻掑缁斿鈧淇洪々锕傚触鐏炵虎鍔勯柡鍥х摠閺?targetPos闁挎稐绶ょ槐?
        this.targetPos = new Vec3(
                targetentity.getX(),
                targetentity.getY(),
                targetentity.getZ()
        );
        setChanged();
    }

    // 闁告梻鍠曢崗姗€鏁嶅鍗炲亶闁轰礁鐭傚Ο浣糕枔閸忓摜鐟濋柛鎰У閺佸ジ宕濋妸銉х；闁诲浚鍋勯崰搴ㄥ础鏉堝墽绀夐梺顒€鐏濋崢銈夊礃瀹勬澘绁卞☉鎾虫捣閸屻劑寮仦钘夊綑闁活潿鍔忛鎼佸极閺夋寧鐝ら悗浣冨閸ぱ囧箮閺嵮冃楅柕?
    public void tryFindtargetShip() {
        ArrayList<SubLevel> enemylist = getData().enemyShipsData;
        //LogUtils.getLogger().warn("enemy list size:"+getData().enemyShipsData.size());
        if (enemylist.isEmpty()) {
            selectedtargetShip = null;
            targetPos = Vec3.ZERO;
            targetDistance = 0;
            targetPreVelocity.clear();
            setAnimData(TURRET_HAS_TARGET, false);
            return;
        }
        // 闁告梻鍠曢崗姗€鏁嶅顒傜Ъ鐟滅増鎸告晶鐘绘儎椤旂晫鍨奸柤鍛婃緲閸戔剝绋夊鍛含闁哄牃鍋撻柡鍌滃閺咁偊鎳滈弶鍨仚閻炴稏鍔嬮懙鎴﹀籍鐠佸湱绀夐悷娆忔鐠愮喐寰勬潏銊︽珡闁烩晩鍠楅悥锝夋晬瀹€鍐冩洟宕ｉ幋锕€娅㈤梺顐㈩槶閳?
        if (selectedtargetShip != null && !enemylist.contains(selectedtargetShip)) {
            selectedtargetShip = null;
        }
        // 闁告梻鍠曢崗姗€鏁嶅顒傜Ъ闁告挸绉跺ú浼村冀閸ワ妇鐭濋柛娆樺灥椤棙绋夐弮鈧﹢渚€寮崼鐔割槯濞ｅ洦绻冪€垫棃鏌ㄦ担鍝ユ毎闁挎稑鐭傛导鈺呭礂瀹ュ棙锟ラ柟鏉跨箣缁犵喖骞庨弽褍袟闁告帒娲﹀畷鏌ユ儎椤旂晫鍨奸柕?
        if (isValidTargetShip(selectedtargetShip)) return;

        this.selectedtargetShip = findNextVisibleTargetShip(enemylist);

        if (this.selectedtargetShip != null) {
            // 闁告梻鍠曢崗姗€鏁嶅鍐茬厒闁肩婀卞ú浼村冀閸ャ劍鏆☉鎾广€€閳ь剚绮岃ぐ鑼喆娴ｈ櫣鈧垶宕欓崱娆忎化闁炽儲绻愮槐娆愬濡搫甯ラ柛娆樺灥椤棙寰勯弽顑锯偓鍐椤喚绀嗛柨娑樼焸娴尖晠宕楀鍥ㄧ獥闁哄秴娲ㄩ崑锝夋媰閽樺韬柤鎼佲偓娑氱Ъ闁告劕鎳橀崕瀵糕偓浣冨閸ぱ冾潩濮濆瞼绠奸柡鍐У绾爼鏌ㄦ担鍝ユ毎闁?
            this.targetPos = getShipAimPoint(this.selectedtargetShip);
            setChanged();
        } else {
            targetPos = Vec3.ZERO;
            // 闁告梻鍠曢崗姗€鏁嶅顒傜Ъ闁圭鍋撻柡鍫濐槹閺咁偊鎳滄导鏉戝幋濞戞挸绉磋ぐ鑼喆?濞戞挸绉磋ぐ鏌ユ偨閵婏附顦ч柨娑樻湰缁旇崵绮氬ú顏呮暁閻庤鐔槐婵囩閵堝洦鏆犲☉鎾剁帛缁侊妇绮欑€ｎ亝绀€闁告帊鍗崇划顖滄媼閵堝牜娼￠幖杈捐礋閳?
            targetDistance = 0;
            targetPreVelocity.clear();
            setAnimData(TURRET_HAS_TARGET, false);
        }
    }

    // 闁告瑯浜ｇ粈瀣嫻閿濆懐鏉藉ù锝嗘尭閸ㄤ粙寮銊х閺夊牊鎸搁崣鍡涙儍閸曨偄娑ч柡鍫濐槸閻ゅ嫭鎷?
    private @Nullable SubLevel findNextVisibleTargetShip(List<SubLevel> enemylist) {
        int currentIndex = selectedtargetShip == null ? -1 : enemylist.indexOf(selectedtargetShip);
        int startIndex = currentIndex >= 0 ? currentIndex + 1 : 0;
        for (int offset = 0; offset < enemylist.size(); offset++) {
            SubLevel candidate = enemylist.get(Math.floorMod(startIndex + offset, enemylist.size()));
            // Function: scan enemy sublevels in order and lock only the first one with a visible aim point.
            if (isValidTargetShip(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    public boolean isValidTargetEntity(@Nullable LivingEntity e) {

        if (e == null) {
            return false;
        }
        if (!e.isAlive()) {
            return false;
        }
        MobCategory category = e.getType().getCategory();
        if (getData().isTargetsHostile() && category.isFriendly()
                || getData().isTargetsPassive() && !category.isFriendly()
                || getData().isTargetsPlayers() && e instanceof Player player && player.isCreative()) {
            return false;
        }

        double distSq = e.distanceToSqr(currentworldpos.x, currentworldpos.y, currentworldpos.z);
        if (distSq > SEARCH_RADIUS * SEARCH_RADIUS) {
            return false;
        }

        Vec3 candidatePos = new Vec3(e.getX(), e.getY(), e.getZ());
        if (!isTargetWithinAimLimits(candidatePos)) {
            return false;
        }
        return canSeeTarget(candidatePos);
    }

    protected boolean isValidTargetShip(SubLevel ship) {
        if (ship == null) {
            return false;
        }
        Vec3 shippos = ServerShipUtils.getStructureCenterWorld(ship);
        Vec3 pos = new Vec3(currentworldpos.x, currentworldpos.y, currentworldpos.z);
        double distance = Vec.Distance(pos, shippos);
        if (distance > 1280) {
            return false;
        }
        if (!canAimAtShip(ship)) {
            return false;
        }
        return canSeeShipTarget(ship);
    }

    // 闁告梻鍠曢崗姗€鏁嶅顒夊殸闁煎憡濯介崺婵BB闁汇劌瀚ˇ鎸庣▔椤忓嫷妯嗛悶娑栧姂濞间即鎮欓悷棰佺驳閻熸瑥妫涢崵搴∥涢埀顒€霉鐎ｅ墎绀夐柛娆樹海椤╋箓寮垫径澶岊伇濞戞搩浜滆ぐ鑼喆娴ｅ搫浠柛妤€鍟块崹鐣屸偓瑙勮壘瑜拌尙鎲存担纰樺亾?
    private boolean canAimAtShip(SubLevel ship) {
        for (Vec3 samplePoint : getShipAimCandidates(ship)) {
            if (isTargetWithinAimLimits(samplePoint)) {
                return true;
            }
        }
        return false;
    }

    private boolean canSeeShipTarget(SubLevel ship) {
        // 闁告梻鍠曢崗姗€鏁嶅顒夋Щ闁活潿鍔庣划鐑樼▔閳ь剟鏌岄崶銊у闁绘劕缍婂▔锕傚触閸剛绀勫鑸电墳閵嗗啴妫?濞戞搩鍘肩缓楣冩晬婢跺﹣绮甸柛娆樺灥椤棝骞€瑜嶉崹鐣屸偓瑙勭啲缁辨繃绌卞┑濠勬缂佷究鍨洪弲顐ｇ▔鎼达絿鈧垶宕欓崱姘兼綈闁告帗鐟ょ粩鎾嚊濞ｎ兘鍋?
        for (Vec3 samplePoint : getShipAimCandidates(ship)) {
            if (isTargetWithinAimLimits(samplePoint) && traceTargetShipBlock(ship, samplePoint) != null) {
                return true;
            }
        }
        return false;
    }

    // 闁告梻鍠曢崗姗€鏁嶅宕囩闁搞儳鍋樼粩瀛樼▔椤忓啰鍠橀柛蹇撶墕瑜拌尙鎲存担鐑樼暠闁煎憡濯介崺鐐烘儔閸曨偄娅欓柣鎰缁辨繈宕欒箛鎾舵瘜闁绘劦鍠栭、娆撳捶閵娾懇鍋撳鈧粭澶愬矗椤栨繍娼岄悹鎰╁妼缁洪箖鍨惧┑鍛憪闁告瑥绉撮ˇ鏌ユ煂瀹ュ鍋撴径灞剧獥闁哄秴娲ㄥ▓鎴﹀箮閼恒儲鍊烽柕?
    protected Vec3 getShipAimPoint(SubLevel ship) {
        // 闁告梻鍠曢崗姗€鏁嶅顓炵樆闁炽儲绮岄ˇ鑽ゆ偘閵娾晜妗ㄩ柣鎰扳偓娑氬枠闁稿繐鐗勯埀顑挎閼垫垼绠涢崘顏勪化闁稿繑绮岀花鎶藉灳濠靛牊鐣卞銈呮惈缁厽娼婚弬鎸庣闁活剙瀚崳顖炴倷閻у摜绀夊ù鍏济崢娑㈠礄缂佹ê鈪甸柤宕囨櫕濠€鍛喆娴ｇ儤鐣卞ù锝呯Ф閻ゅ棝濡?
        for (Vec3 samplePoint : getShipAimCandidates(ship)) {
            if (isTargetWithinAimLimits(samplePoint) && traceTargetShipBlock(ship, samplePoint) != null) {
                return samplePoint;
            }
        }
        // 闁告梻鍠曢崗姗€鏁嶅顒傜Ъ闁稿繈鍔戦崕鎾倷瑜版帒鍘村☉鎾崇Т瑜拌尙鎲存担瑙勵槯闁挎稑鑻ú鏍焻閳ь剟宕氶幏灞界厒闁兼悂鈧稖鍘煫鍥у枦缁辨繈鏌嗛崹顔煎赋閺夆晜鏌ㄥú鏍矚閸濆嫧鍋撻悡搴殼闁奸攱娼欓幃妤冪磼椤撶喐顥嬮弶鐑嗗墮缁辨挾鏁幖鐐╁亾?
        return ServerShipUtils.getStructureCenterWorld(ship);
    }

    // 闁告梻鍠曢崗姗€鏁嶅杈ㄦ櫢闁瑰瓨鍔橀崺宀勬嚋缁楃檰BB闁汇劌瀚埀顒佺懇閳ь剙顦遍悗顖炲礄閸℃瑥浠柨娑樼墕椤︽寧寰勯弽顑锯偓鍐閵忋垹浠?濞戞搩鍘肩缓楣冨礂濠婂啰淇洪柨娑橆檧缁辨繈鎮介妸銈囪壘闁告瑯鍨甸～鍡涘箑瑜庨ˉ鍛圭€ｎ亝瀚查悘蹇撳閸ゎ喚浜稿┑濠勬Ц闁?
    protected @Nullable SubLevel getSelectedTargetShip() {
        // Function: subclasses need the locked ship to convert visual hit positions back to sublevel body positions.
        return selectedtargetShip;
    }

    private List<Vec3> getShipAimCandidates(SubLevel ship) {
        BoundingBox3dc worldAabb = ship.boundingBox();
        double minX = worldAabb.minX();
        double minY = worldAabb.minY();
        double minZ = worldAabb.minZ();
        double maxX = worldAabb.maxX();
        double maxY = worldAabb.maxY();
        double maxZ = worldAabb.maxZ();
        double centerX = (minX + maxX) * 0.5;
        double centerY = (minY + maxY) * 0.5;
        double centerZ = (minZ + maxZ) * 0.5;

        List<Vec3> samplePoints = new ArrayList<>();
        // Function: center-first probing avoids locking onto exposed bounding-box air.
        samplePoints.add(new Vec3(centerX, centerY, centerZ));
        // 闁告梻鍠曢崗姗€鏁嶅顒€甯ラ悘蹇旂箚閻︻垶宕楅婵嬪殝闂傚牜婢€閼垫垼绠涢崘顏勪化闁挎稑鐭侀鍝ョ不濡や礁鐏囬柡鍫厸缂嶅棙绋夐弮鍥у幋閻熸洖妫涘ú濠冨緞瑜嶉ˇ鍧楀极閺夊灝璁查悷娆庣劍閸庡繘宕橀悙鍏夊亾?
        samplePoints.add(new Vec3(minX, centerY, centerZ));
        samplePoints.add(new Vec3(maxX, centerY, centerZ));
        samplePoints.add(new Vec3(centerX, minY, centerZ));
        samplePoints.add(new Vec3(centerX, maxY, centerZ));
        samplePoints.add(new Vec3(centerX, centerY, minZ));
        samplePoints.add(new Vec3(centerX, centerY, maxZ));
        // 闁告梻鍠曢崗姗€鏁嶅顒€鏅欓悘蹇旂箚閻︻垶宕跺☉妤呭殝濞戞挸锕ㄩ妴鍐閵忥綆娼￠柣鎰缁辨繈骞撻幇顒€纾抽梺顒夊枟鐏忓懘宕烽悜妯荤彲濞戞挸顑嗘竟姗€宕氶弶鍨閻熸瑤鑳堕崑锝夋儍閸曨剦娲ら柣婊冩储閳?
        samplePoints.add(new Vec3(minX, maxY, minZ));
        samplePoints.add(new Vec3(minX, maxY, maxZ));
        samplePoints.add(new Vec3(maxX, maxY, minZ));
        samplePoints.add(new Vec3(maxX, maxY, maxZ));
        // 闁告梻鍠曢崗姗€鏁嶅顓熶粯闁告艾楠告慨鐐哄礂閵夈倛鍘煫鍥у暟閸嬶絾鎷呭鈧拹鐔煎礂濠婂啰淇洪柣鈺婂枟閻栵綁鏁嶅畝鍕級闁稿繐绉撮悾顒勫礂閵娿儯浜奸柛妯垮吹濞蹭即寮介崶銉㈠亾?
        samplePoints.add(new Vec3(centerX, centerY, centerZ));
        return samplePoints;
    }

    private @Nullable BlockHitResult traceTargetShipBlock(SubLevel ship, Vec3 aimPoint) {
        Level level = this.getLevel();
        if (level == null || ship == null) {
            return null;
        }
        Vec3 from = new Vec3(currentworldpos.x, currentworldpos.y, currentworldpos.z);
        BlockHitResult hitResult = LoadedChunkRaycast.clipIgnoringUnloadedChunks(
                level,
                from,
                aimPoint,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                CollisionContext.empty(),
                this::configureShipTargetClipContext
        );
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockPos hitPos = hitResult.getBlockPos();
        // Function: sublevel targets must resolve to an actual block on the selected enemy ship, not AABB air.
        if (isBlockOnSameShipAsTurret(hitPos) || !isBlockOnShip(ship, hitPos)) {
            return null;
        }
        return hitResult;
    }

    private void configureShipTargetClipContext(ClipContext context) {
        Level level = this.getLevel();
        if (level == null) {
            return;
        }
        SubLevel ownShip = ServerShipUtils.getSubLevelAtBlockPos(level, this.getBlockPos());
        if (ownShip != null && context instanceof ClipContextExtension extension) {
            // Function: ship target rays must pass through the turret's own sublevel before testing enemies.
            extension.sable$setIgnoredSubLevel(ownShip);
        }
    }

    private boolean canSeeTarget(Vec3 pos) {
        Vec3 turretpos = new Vec3(currentworldpos.x, currentworldpos.y, currentworldpos.z);
        // 闁告梻鍠曢崗姗€鏁嶅姣欌晠姊介妶鍛稄闁哄秴娲ゅú鎾绘嚋瀹ュ嫮瀹夐柛蹇嬪劵缁辨繈鏌嗛崹顔煎赋閻熸瑥妫涢崵搴ㄥ礆閵堝棙鐒介柛锔哄姀缁旂喖鎮剧仦绛嬫П闁硅埖鐗曟慨鈺冣偓浣冨閸ぱ囨倷椤旂⒈鏁婇柟鎯板Г閹瑩濡?
        Vec3 targetPos = new Vec3(pos.x(), pos.y(), pos.z());
        Vec3 lookVec = turretpos.vectorTo(targetPos).normalize().scale(0.75F);
        return LoadedChunkRaycast.clipIgnoringUnloadedChunks(
                this.getLevel(),
                turretpos.add(lookVec),
                targetPos,
                ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE,
                CollisionContext.empty()
        ).getType().equals(HitResult.Type.MISS);
    }

    // 闁告梻鍠曢崗姗€鏁嶅顒€鐏查柡鍌ゅ幗鐎垫氨鈧纰嶉弻鐔煎锤濡や焦笑闁告熬绠戦惈妯荤鎼达絽浠忓┑澶嬫閸ゆ粓鐓锝咁暡闁革负鍔忛崺鐐存媴閹垮嫮绀夐柣顫妺缁剟骞忛敂钘夌劵闁炽儲绮忛銈囦焊閸℃艾娈伴棅顒夊亯閸╃偞鎷呴幘宕囩Ъ濞达絾绮岃ぐ鏌ュ绩鐠囨彃姣婇柣鈺婂枟閻栵綁鍨惧┑鍫熺暠闁诡垰鎳庨崰宀勫Υ?
    private boolean isBlockOnSameShipAsTurret(BlockPos blockPos) {
        Level level = this.getLevel();
        if (level == null) {
            return false;
        }
        SubLevel turretShip = ServerShipUtils.getSubLevelAtBlockPos(level,pos);
        if (turretShip == null) {
            return false;
        }
        SubLevel hitShip = ServerShipUtils.getSubLevelAtBlockPos(level,blockPos);
        return hitShip != null && hitShip.hashCode() == turretShip.hashCode();
    }

    private boolean isBlockOnShip(SubLevel ship, BlockPos blockPos) {
        Level level = this.getLevel();
        if (level == null || ship == null) {
            return false;
        }
        SubLevel hitShip = ServerShipUtils.getSubLevelAtBlockPos(level, blockPos);
        return hitShip != null && hitShip.hashCode() == ship.hashCode();
    }

    private void recordShipShotHitBlockPos() {
        Level level = this.getLevel();
        this.shipShotBlockedBySelfShip = false;
        if (level == null) {
            this.lastShipShotHitBlockPos = BlockPos.ZERO;
            return;
        }
        Vec3 from = new Vec3(currentworldpos.x, currentworldpos.y, currentworldpos.z);
        List<Vec3> shotCandidates = new ArrayList<>();
        // 闁告梻鍠曢崗姗€鏁嶅顐ゅ枠闁稿繐鐗嗛惃鍓ф嫚閺囩偟绉奸柛鎾崇Т閸戯繝鏌ㄦ担鍝ユ毎闁烩晩鍠楅悥锝夋倷閻у摜绀夊ǎ鍥ㄧ箚閻﹀鎮欓鐓庣稉閻熸瑥妫滈～搴㈢▔鎼达絾鍩傞悗鍦仜閻ㄧ姷鐥径鍝ヮ伇闁奸攱鐣埀?
        shotCandidates.add(targetPos);
        if (isValidTargetShip(selectedtargetShip)) {
            // 闁告梻鍠曢崗姗€鏁嶅鍐差仧濡絾鐗旈柌婊堟儎椤旂晫鍨奸柣鎰嚀閻ㄧ姷鐥懗顖涘劙缂佸瞼灏ㄧ槐婵嬪礆濞嗘垶鍩涚紓渚囧幖閻ㄥ墽鎷犻弴鈥崇厒闁煎摜鎳撻崣鐐媴濞嗗浚妯嗛悶娑栧姂濞间即鏌岄崶銊у闁绘劕绠嶉埀?
            for (Vec3 candidate : getShipAimCandidates(selectedtargetShip)) {
                if (Vec.Distance(candidate,targetPos) > 1.0e-6) {
                    shotCandidates.add(candidate);
                }
            }
        }

        for (Vec3 shotPoint : shotCandidates) {
            Vec3 to = new Vec3(shotPoint.x, shotPoint.y, shotPoint.z);
            BlockHitResult hitResult = LoadedChunkRaycast.clipIgnoringUnloadedChunks(
                    level,
                    from,
                    to,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    CollisionContext.empty(),
                    this::configureShipTargetClipContext
            );
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos hitPos = hitResult.getBlockPos();
                if (isBlockOnSameShipAsTurret(hitPos)) {
                    this.shipShotBlockedBySelfShip = true;
                    this.lastShipShotHitBlockPos = BlockPos.ZERO;
                    return;
                }
                // Function: ship shots only accept body-space hits on the currently selected target.
                if (selectedtargetShip != null && !isBlockOnShip(selectedtargetShip, hitPos)) {
                    continue;
                }
                this.targetPos = shotPoint;
                this.lastShipShotHitBlockPos = hitPos;
                return;
            }
        }
        // 闁告梻鍠曢崗姗€鏁嶅顓烆暡闁哄牆顦埀顒佺懇閳ь剙顦遍崑锝夋焾閼恒儲寮撻柛娑欏灊閼垫垿寮悷鐗堝仴闁哄啫鐖奸崳鍝ョ磾椤旀槒绀?ZERO闁挎稑鐭傛导鈺呭礂瀹ュ嫮绠介柣锝嗙懄濡偊寮悧鍫濈ウ閻犲浂鍨伴崹浠嬪Υ?
        this.lastShipShotHitBlockPos = BlockPos.ZERO;
    }

    @Override
    public double getTick(Object BlockEntity) {
        return RenderUtil.getCurrentTick();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // 缁绢収鍠曠换?turretData 閻炴凹鍋呴婊呮兜椤旂厧鐏ュ┑顔碱儏鐎?
        if (this.turretData == null) {
            this.turretData = new TurretData();
        }
        markUpdated();
    }

    public void markUpdated() {
        this.setChanged();
        this.getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        //if(!this.level.isClientSide()) sendUpdatePacket();
    }


    @Override
    public Component getDisplayName() {
        return Component.literal("Turret Screen");
    }

    @Override
    public @NotNull AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new TurretContainerMenu(containerId, inv, this);
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

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put(AMMO_INVENTORY_TAG, ammoInventory.serializeNBT(registries));
        tag.putInt("aimtype", aimtype);
        tag.putInt("configregister", turretData.configRegister);
        tag.putDouble("distance", this.getTargetDistance());
        tag.putFloat("xrot", this.targetxrot);
        tag.putFloat("yrot", this.targetyrot);
        tag.putInt("defaultxrot", this.defaultspinx);
        tag.putInt("defaultyrot", this.defaultspiny);
        tag.putInt("aimLimitMinX", getData().aimLimitMinX);
        tag.putInt("aimLimitMaxX", getData().aimLimitMaxX);
        tag.putInt("aimLimitMinY", getData().aimLimitMinY);
        tag.putInt("aimLimitMaxY", getData().aimLimitMaxY);
        tag.putBoolean("breaksBlocks", getData().isBreaksBlocks());
        tag.putInt("muzzleFlashTicks", this.muzzleFlashTicks);
        tag.putDouble("fireCooldownValue", this.fireCooldownValue);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains(AMMO_INVENTORY_TAG)) {
            ammoInventory.deserializeNBT(registries, tag.getCompound(AMMO_INVENTORY_TAG));
        }
        if (this.turretData == null) {
            this.turretData = new TurretData();
        }
        if (tag.contains("aimtype")) {this.aimtype = tag.getInt("aimtype");}
        if (tag.contains("configregister")) {turretData.configRegister = tag.getInt("configregister");}
        if (tag.contains("distance")) {this.targetDistance = tag.getDouble("distance");}
        if (tag.contains("xrot")) {this.targetxrot = tag.getFloat("xrot");}
        if (tag.contains("yrot")) {this.targetyrot = tag.getFloat("yrot");}
        if (tag.contains("defaultyrot")) {this.defaultspiny = tag.getInt("defaultyrot");}
        if (tag.contains("defaultxrot")) {this.defaultspinx = tag.getInt("defaultxrot");}
        if (tag.contains("aimLimitMinX")) {getData().aimLimitMinX = tag.getInt("aimLimitMinX");}
        if (tag.contains("aimLimitMaxX")) {getData().aimLimitMaxX = tag.getInt("aimLimitMaxX");}
        if (tag.contains("aimLimitMinY")) {getData().aimLimitMinY = tag.getInt("aimLimitMinY");}
        if (tag.contains("aimLimitMaxY")) {getData().aimLimitMaxY = tag.getInt("aimLimitMaxY");}
        if (tag.contains("breaksBlocks")) {getData().setBreaksBlocks(tag.getBoolean("breaksBlocks"));}
        if (tag.contains("muzzleFlashTicks")) {this.muzzleFlashTicks = tag.getInt("muzzleFlashTicks");}
        if (tag.contains("fireCooldownValue")) {this.fireCooldownValue = tag.getDouble("fireCooldownValue");}
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public float closestReachableX(float current, float maxChange, float target) {
        // 闁稿繐鐗婃俊?target 闁瑰嘲顦崺?current 閸?80閹?闁肩厧鍟ú鍧楀礃?
        float delta = target - current;
        delta = (delta + Mth.PI) % (Mth.TWO_PI) - Mth.PI;  // -閿?~ +閿?

        float minAllowed = -maxChange;
        float maxAllowed = maxChange;

        float move;
        if (delta < minAllowed) {
            move = minAllowed;
            this.xOK = false;
        } else if (delta > maxAllowed) {
            move = maxAllowed;
            this.xOK = false;
        } else {
            move = delta;
            this.xOK = true;
        }

        return current + move;
    }

    public float closestReachableY(float current, float maxChange, float target) {
        // 闁稿繐鐗婃俊?target 闁瑰嘲顦崺?current 閸?80閹?闁肩厧鍟ú鍧楀礃?
        float delta = target - current;
        delta = (delta + Mth.PI) % (Mth.TWO_PI) - Mth.PI;  // -閿?~ +閿?

        float minAllowed = -maxChange;
        float maxAllowed = maxChange;

        float move;
        if (delta < minAllowed) {
            move = minAllowed;
            this.yOK = false;
        } else if (delta > maxAllowed) {
            move = maxAllowed;
            this.yOK = false;
        } else {
            move = delta;
            this.yOK = true;
        }

        return current + move;
    }

    private void updateTargetRot() {
        double[] aimAngles = computeTargetAimAngles(targetPos);
        if (aimAngles == null) {
            return;
        }

        this.targetxrot = (float) aimAngles[0];
        this.targetyrot = (float) aimAngles[1];
    }

    // 闁告梻鍠曢崗姗€鏁嶅顓燂骏闁哄牆顦伴弲銉╂儎椤旂晫鍨奸柡鍐硾閻ㄣ垽鎮欓纰辨晩闁哄牊绻傞幃婊堢嵁閾忣偆鎷ㄩ柛銉у仜缂嶅﹪宕氭导瀵稿笡閻犱降鍊涢～妤佹償閿旇偐绀刣efaultxrot/defaultyrot闁挎稑顦埀?
    public void returnToDefaultRotation() {
        this.targetxrot = this.defaultspinx;
        this.targetyrot = this.defaultspiny;
        this.xRot0 = closestReachableX(xRot0, getMaxSpinSpeed(), targetxrot*Mth.PI/180);
        this.yRot0 = closestReachableY(yRot0, getMaxSpinSpeed(), targetyrot*Mth.PI/180);
    }

    // 闁告梻鍠曢崗姗€鏁嶅杈ㄦ殸 C2S 闁轰胶澧楀畵渚€宕犻崨顓炴櫢闁稿繈鍎抽惌鎴犫偓娑欏姉閸?firepoint 闁秆勫姈閻栵綁鏁嶅畝鍕級闁稿繐绉靛﹢鍥礉閿涘嫷浼傞柛鎰Х椤撳摜绮?pivot 濞戞挻鐗滈弲顐﹀锤閹邦厾鍨奸柕?
    public void setFirePoint(Vector3d postofire) {
        if (postofire == null) {
            this.FirePoint = null;
            return;
        }
        // Store an absolute sublevel-space muzzle point captured from the Geckolib firepoint bone.
        this.FirePoint = new Vector3d(postofire);
    }

    public float xRot0 = 0;
    public float yRot0 = 0;
    public float prevxrot = 0;
    public float prevyrot = 0;
    public boolean xOK = false;
    public boolean yOK = false;
    public float targetxrot = 0;
    public float targetyrot = 0;
    public int defaultspinx = 0;
    public int defaultspiny = 0;


    // 濞寸姰鍎扮粭鍛▔閻戞ɑ鐓€闂侇喓鍔岄崹?

    // 濞戞挶鍊撻柌婊堝棘閻熺増鍊婚柣銊ュ濞撹埖寰勮椤鏌呴悢宄邦唺 rad/s
    protected final float MAX_OMEGA_YAW = 1;
    protected final float MAX_OMEGA_PITCH = 1;

    protected float defaultYaw = 0;
    protected float defaultPitch = 0;

    protected float currentYaw = 0;
    protected float currentPitch = 0;


    public class servo{
        // d^2/dt^2 angle = Kp * (target - angle) - Kd * d/dt angle
        // Phi = Kp/(s^2 + Kd*s + Kp)
        // Omega_N = sqrt(Kp)
        // Epsilon = Kd / ( 2*sqrt(Kp) )

        public float angle = 0; // rad
        public float omega = 0;
        public float beta  = 0;
        private float Kp;
        private float Kd;
        private final float dt = 1f / 20;
        private static final float PI = (float) Math.PI;

        public boolean isStable = false;

        public void servoInitial(float Kp, float Kd){ // 闁告帗绻傞～鎰板礌閺嶇數绀夐柛蹇氭硾閻ゅ嫬顕欐ウ娆惧敶闁活潿鍔嬬粭鍛存閵忋倕浜濆☉?
            this.Kp = Kp;
            this.Kd = Kd;
        }

        public void servoAutoInitial(int stableTick){ // 閺夊牊鎸搁崣鍡欑矙閸愯尙鏆伴柡鍐ㄧ埣濡潡鏁嶉崸鐪巆ks闁挎稑顦銊ф偘瀹€瀣婵炲鍔嶉崜鐗堢▔瀹ュ牜娲ｉ弶鈺佹矗缂嶅棝鏁嶆担鍝ョ处閻犱緡鍠涢崵锔句焊閹存粏绀?ticks
            // 濠碘€冲€归悘澶嬫媴閻樿櫣缈婚柛?tick 闁告瑯鍨甸崗妯诲濮樿鲸绠欓悷娆庣閵囧洦顦版惔銈嗙盃
            // 濠㈠爢鍥舵閺夌儑璁ｇ槐鎺楁晬娓氬﹦纾?
            float second = stableTick * dt;
            this.Kp = 32f / (second * second);
            this.Kd = 8f / second;
        }

        private static float angleNormalize(float angle) {
            angle %= 2 * PI;
            if (angle > PI) angle -= 2 * PI;
            else if (angle < PI) angle += 2 * PI;
            return angle;
        }

        public boolean updateServo(float target){ // 閺夆晜鏌ㄥú?闁哄嫷鍨伴幆浣烘崉閻斿吋顓圭紒瀣暱閻?
            // 闁硅矇鍐ㄧ厬缂侇垵宕电划?
            float error = angleNormalize( target - this.angle );
            this.beta = Kp * error - Kd* this.omega;

            // 缂侇垵宕电划娲礉閵娿儱顫旈悗娑冲婵悂骞€?
            this.omega += this.beta * dt;
            this.angle += this.omega * dt;

            this.angle = angleNormalize(this.angle);
            this.isStable = (error <=0.034);; // 2閹?

            return this.isStable;
        }
    }
    
    // 闁烩晩鍠楅悥锝団偓瑙勭煯缂嶅懘寮憴鍕€婇柛婊冭嫰閻ｇ娀鎯冮崟顏嗙憦濞戞搩浜滈惃婵堟啑?
    // 濞达絿濮寸花鑼嫚閵夈倕鈻忛柣顫妼閻ㄦ繄鎲?
    private double[] doSightTransform(
            Vector3d dirInWorld,
            SubLevel subLevel
    ){
        Vector3d dirInShip=subLevel.logicalPose().transformNormalInverse(dirInWorld);
        Vector3d dirInModel=this.turretData.getCoordAxis().transform(dirInShip);

        // 閻犲洠鈧磭纾介柣銊ュ濞兼寮介崶褍缍侀柟?闁哄秷顫夊畵浣肝熼垾宕団偓鐑藉级閵壯勭暠
        double yaw = Math.atan2(
                dirInModel.x,
                dirInModel.z
        );
        double pitch=Math.atan2(
                Math.sqrt(dirInModel.x * dirInModel.x + dirInModel.z * dirInModel.z),
                dirInModel.y
        );

        return new double[]{yaw,pitch};
    }

    public double[] sightTransformByDir(
            Vector3d dirInWorld,
            SubLevel subLevel
    ){
        return doSightTransform(dirInWorld, subLevel);
    }

    public double[] sightTransformByVec3Pos(
            Vector3d TargetPosInWorld,
            SubLevel subLevel
    ){
        Vector3d TurretPos = new Vector3d(this.getBlockPos().getX(),this.getBlockPos().getY(),this.getBlockPos().getZ());
        TurretPos.add(this.getData().basePivotOffset);
        Vector3d dirInWorld = TargetPosInWorld.sub(TurretPos);

        return doSightTransform(dirInWorld, subLevel);
    }
    public double[] sightTransformByBlockPos(
            BlockPos TargetBlockPosInWorld,
            SubLevel subLevel
    ){
        Vector3d TurretPos = new Vector3d(this.getBlockPos().getX(),this.getBlockPos().getY(),this.getBlockPos().getZ());
        TurretPos.add(this.getData().basePivotOffset);
        Vector3d TargetPosInWorld = new Vector3d(TargetBlockPosInWorld.getX(),TargetBlockPosInWorld.getY(),TargetBlockPosInWorld.getZ());
        Vector3d dirInWorld = TargetPosInWorld.sub(TurretPos);

        return doSightTransform(dirInWorld, subLevel);
    }
    @Override
    public int getSlots() {
        return ammoInventory.getSlots();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return ammoInventory.getStackInSlot(slot);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return ammoInventory.insertItem(slot, stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ammoInventory.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return ammoInventory.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return ammoInventory.isItemValid(slot, stack);
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        ammoInventory.setStackInSlot(slot, stack);
        setChanged();
    }
}

