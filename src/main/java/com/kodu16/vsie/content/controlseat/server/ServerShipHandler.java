package com.kodu16.vsie.content.controlseat.server;


import com.kodu16.vsie.content.controlseat.block.ControlSeatBlockEntity;
import com.kodu16.vsie.content.controlseat.entity.ControlSeatMountEntity;
import com.kodu16.vsie.content.controlseat.functions.ScanNearByShips;
import com.kodu16.vsie.content.warpprojectile.WarpProjecTileEntity;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.foundation.Vec;
import com.kodu16.vsie.network.controlseat.S2C.ControlSeatInputS2CPacket;
import com.kodu16.vsie.network.controlseat.S2C.ControlSeatS2CPacket;
import com.kodu16.vsie.network.controlseat.S2C.ControlSeatStatusS2CPacket;
import com.kodu16.vsie.network.controlseat.S2C.NearbyShipsS2CPacket;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3dc;
import com.kodu16.vsie.registries.vsieEntities;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import com.kodu16.vsie.registries.ModNetworking;

import org.slf4j.Logger;

import javax.annotation.Nullable;


public class ServerShipHandler {
    private static final double FLIGHT_ASSIST_LINEAR_RESPONSE = 0.60D;
    private static final double FLIGHT_ASSIST_ANGULAR_RESPONSE = 0.45D;
    private static final double ANTI_GRAVITY_VERTICAL_RESPONSE = 0.80D;
    private static final double FLIGHT_ASSIST_LINEAR_THRUST_FRACTION = 0.25D;
    private static final double FLIGHT_ASSIST_ANGULAR_THRUST_FRACTION = 0.18D;
    private static final double ANTI_GRAVITY_DAMPING_THRUST_FRACTION = 0.25D;
    private static final double CONTROL_FORCE_SCALE = 0.25D;
    private static final double CONTROL_TORQUE_SCALE = 0.12D;
    private static final double TRANSLATION_THROTTLE_EQUIVALENT = 0.10D;
    private static final double LINEAR_REFERENCE_SPEED = 10.0D;
    private static final double ANGULAR_REFERENCE_SPEED = 1.5D;
    private static final double ANGULAR_ASSIST_REST_SPEED = 0.02D;
    private static final double MASS_PROPERTY_RESPONSE = 2.0D;
    private static final double MIN_VALID_MASS = 1.0D;
    private static final double MIN_VALID_INERTIA = 1.0D;
    private static final double CONTROL_INPUT_RESPONSE = 14.0D;
    private static final double THROTTLE_INPUT_RESPONSE = 8.0D;
    private static final double SABLE_GRAVITY_IMPULSE_SCALE = 1.0D / 2.1D;
    private static final double AXIS_EPSILON = 1.0E-8D;
    private static final double WARP_ALIGNMENT_THRESHOLD_DEGREES = 1.0D;
    private static final double WARP_ALIGNMENT_TORQUE_SCALE = 2.0D;
    private static final double WARP_PROJECTILE_DISTANCE_SCALE = 1.5D;
    private static final int WARP_TELEPORT_EXTRA_DELAY_TICKS = 100;
    private ControlSeatServerData data;
    public static final Logger LOGGER = LogUtils.getLogger();

    public ServerShipHandler(ControlSeatServerData data){
        this.data = data;
    }

    public void resetControlInput() {
        smoothedControlTorque.set(0.0D, 0.0D, 0.0D);
        smoothedTranslationInput.set(0.0D, 0.0D, 0.0D);
        smoothedThrottle = 0.0D;
        data.setFinaltorque(new Vector3d());
        data.setFinalforce(new Vector3d());
    }

    private long lastSendMs = 0;
    private long lastSendStatusMs = 0;
    private long lastSendInputMs = 0;
    private long lastScanShipsMs = 0;
    int lastSentEncode = 0;
    int current=0;
    private volatile Vec3 worldXDirection;
    private volatile Vec3 worldYDirection;
    private volatile Vec3 worldZDirection;
    private final Vector3d smoothedControlTorque = new Vector3d();
    // Function: smooth unlocked WASD translation so force does not step sharply when keys change.
    private final Vector3d smoothedTranslationInput = new Vector3d();
    private double smoothedThrottle = 0.0D;
    private double smoothedMass = Double.NaN;
    private double smoothedAverageInertia = Double.NaN;
    //鏉╂獏yd瀵板牆褰查懗钘夋皑閺勵垱顒村ú璁崇瑝閸欐垵瀵橀惃鍕斧閸?
    public void getandsendshipdata(ServerSubLevel subLevel,BlockPos pos) {
        if (data.getDirectionForward() == null || data.getDirectionUp() == null || data.getDirectionRight() == null) {
            return;
        }

        Vec3 ForwardDirection =  subLevel.logicalPose().transformNormal(Vec3.atLowerCornerOf(data.getDirectionForward()));
        Vec3 UpDirection =  subLevel.logicalPose().transformNormal(Vec3.atLowerCornerOf(data.getDirectionUp()));
        Vec3 RightDirection =  subLevel.logicalPose().transformNormal(Vec3.atLowerCornerOf(data.getDirectionRight()));
        Level level = data.level;
        long now = System.currentTimeMillis();
        if (now - lastScanShipsMs > 500) {
            lastScanShipsMs = now;
            // Function: automatic heavy turrets still need fresh enemy ship targets when no player is seated.
            refreshNearbyShips(pos, level);
            if (data.getPlayer() != null) {
                ModNetworking.sendToPlayer(new NearbyShipsS2CPacket(data.shipsData), (ServerPlayer) data.getPlayer());
            }
        }
        if (data.getPlayer() != null) {
            if (now - lastSendMs > 50) {//韫囶偄瀵?
                lastSendMs = now;
                ControlSeatS2CPacket packet = new ControlSeatS2CPacket(
                        pos,
                        Vec.toVector3d(ForwardDirection),
                        Vec.toVector3d(UpDirection),
                        data.enemy,
                        data.ally,
                        data.lockedenemyslug,
                        data.getThrottle(),
                        data.isviewlocked
                );
                ModNetworking.sendToPlayer(packet, (ServerPlayer) data.getPlayer());
            }

            if(now - lastSendStatusMs > 250) {//閻樿埖鈧礁瀵橀敍鍫熷弮閸栧嵀ut閿?
                lastSendStatusMs = now;
                ControlSeatStatusS2CPacket packetstatus = new ControlSeatStatusS2CPacket(pos,
                        data.avalibleenergy,data.totalenergystorage,
                        data.avaliblefuel,data.totalfuelstorage,
                        data.isshieldon, (int) data.avalibleshield, (int) data.totalshield,
                        data.isflightassiston, data.isantigravityon,
                        data.isWarpPreparing, data.hasPendingWarpTeleport, data.warpTargetName,
                        data.activeWeaponHudInfos);
                //LogUtils.getLogger().warn("shieldtotal:"+data.totalshield+"avalible:"+data.avalibleshield);
                ModNetworking.sendToPlayer(packetstatus, (ServerPlayer) data.getPlayer());
            }

            if(now - lastSendInputMs > 250) {//閹稿鏁崠鍜冪礄閹便垹瀵榦ut閿?
                // 閸旂喕鍏橀敍姘卞缁斿绶崗銉ュ瘶閸欐垿鈧浇濡ù浣规闂傝揪绱濋柆鍨帳娑撳海濮搁幀浣稿瘶閸忚京鏁ょ拋鈩冩閸ｃ劌顕遍懛纾嬬翻閸忋儱瀵橀弶鈥叉濮樻瓕绻欐稉宥嗗灇缁斿鈧?
                lastSendInputMs = now;
                ControlSeatInputS2CPacket packet = new ControlSeatInputS2CPacket(pos, data.channelencode);
                ModNetworking.sendToPlayer(packet, (ServerPlayer) data.getPlayer());
            }
        }
    }

    private void refreshNearbyShips(BlockPos pos, Level level) {
        if (level == null) {
            data.shipsData.clear();
            data.enemyshipsData.clear();
            data.lockedenemyslug = "";
            data.lockedEnemySubLevel = null;
            data.lockedenemyindex = 0;
            return;
        }

        // 鍔熻兘锛氭壂鎻忓悓缁村害 Sable sublevel锛屾寜 IFF 鍚嶇О瑙勫垯鐢熸垚 HUD 鏍囪鏁版嵁鍜岄噸鍨嬬偖濉旀晫鑸扮洰鏍囧垪琛ㄣ€?
        data.shipsData = ScanNearByShips.withEnemyTargetIndexes(
                ScanNearByShips.scanships(null, pos, level),
                data.enemy,
                data.ally
        );
        data.enemyshipsData = ScanNearByShips.scanenemyships(null, pos, level, data.enemy, data.ally);
        if (data.enemyshipsData.isEmpty()) {
            data.lockedenemyindex = 0;
            data.lockedenemyslug = "";
            data.lockedEnemySubLevel = null;
            return;
        }

        data.lockedenemyindex = Math.floorMod(data.lockedenemyindex, data.enemyshipsData.size());
        data.lockedenemyslug = ScanNearByShips.lockedEnemySlug(data.shipsData, data.lockedenemyindex);
        data.lockedEnemySubLevel = ScanNearByShips.scanEnemySubLevelByIndex(
                null,
                pos,
                level,
                data.enemy,
                data.ally,
                data.lockedenemyindex
        );
    }

    public void applyForceAndTorque(ServerSubLevel subLevel, BlockPos pos, double timeStep) {
        // 閸旂喕鍏橀敍姘槨 tick 閸忓牊顥呴弻銉︽Ц閸氾箑鍩屾禍鍡楁鏉╃喕绌潻浣叫曢崣鎴炴闂傝揪绱濈涵顔荤箽瀵€涚秼鐎靛灝鎳＄紒鎾存将閸氬氦鍏橀懛顏勫З閹笛嗩攽 teleportship閵?
        processPendingWarpTeleport(subLevel);
        boolean hasControlAxes = data.getDirectionForward() != null && data.getDirectionUp() != null && data.getDirectionRight() != null;

        Player player = data.getPlayer();
        boolean controlling = true;
        // 1. 閻溾晛顔嶆稉铏光敄閹存牕鍑＄紒蹇旑劥娴滃棴绱濋惄瀛樺复閸熴儵鍏樻稉宥呭叡
        if (player == null || !player.isAlive() || player.isRemoved()) {
            data.reset();
            resetControlInput();
            controlling = false;
        }
        // 2. 閻溾晛顔嶈ぐ鎾冲娑旀ê娼楅惃鍕杽娴ｆ挷璐熺粚鐚寸礉閹存牞鈧懍绗夐弰?VS2 閻ㄥ嫯鍩為幐鍌濇祰鐎圭偘缍?
        Entity vehicle = null;
        if (player != null) {
            vehicle = player.getVehicle();
        }
        if (!(vehicle instanceof ControlSeatMountEntity)) {
            data.reset();
            resetControlInput();
            controlling = false;
        }
        if (controlling && !hasControlAxes) {
            resetControlInput();
            return;
        }

        MassData massData = subLevel.getMassTracker();
        if (massData == null || massData.isInvalid()) {
            resetControlInput();
            return;
        }
        double rawMass = massData.getMass();
        Matrix3dc momentOfInertia = massData.getInertiaTensor();
        double rawAverageInertia = averageInertia(momentOfInertia);
        if (!isUsableMassProperties(rawMass, momentOfInertia, rawAverageInertia)) {
            resetControlInput();
            return;
        }
        updateSmoothedMassProperties(rawMass, rawAverageInertia, timeStep);
        double mass = conservativeMass(rawMass);
        double averageInertia = conservativeAverageInertia(rawAverageInertia);

        RigidBodyHandle handle = RigidBodyHandle.of(subLevel);
        if (handle == null || !handle.isValid()) {
            resetControlInput();
            return;
        }
        Vector3d omega = handle.getAngularVelocity(new Vector3d());
        Vector3d velocity = handle.getLinearVelocity(new Vector3d());
        if (!isFiniteVector(omega) || !isFiniteVector(velocity)) {
            resetControlInput();
            return;
        }

        double totalThrust = Math.max(0.0D, data.thruster_strength);
        double linearDampingAlpha = authorityDampingAlpha(
                totalThrust * FLIGHT_ASSIST_LINEAR_THRUST_FRACTION,
                mass,
                velocity.length(),
                LINEAR_REFERENCE_SPEED,
                FLIGHT_ASSIST_LINEAR_RESPONSE,
                timeStep
        );
        Vector3d invforce = velocity.negate(new Vector3d()).mul(mass * linearDampingAlpha);

        double angularTorqueAuthority = angularTorqueAuthority(totalThrust, mass, averageInertia);
        double angularDampingAlpha = authorityDampingAlpha(
                angularTorqueAuthority * FLIGHT_ASSIST_ANGULAR_THRUST_FRACTION,
                averageInertia,
                omega.length(),
                ANGULAR_REFERENCE_SPEED,
                FLIGHT_ASSIST_ANGULAR_RESPONSE,
                timeStep
        );
        Vector3d invtorque = calculateFlightAssistTorque(
                subLevel,
                momentOfInertia,
                omega,
                angularDampingAlpha,
                rawAverageInertia,
                averageInertia
        );

        Vector3d finaltorque = new Vector3d(0,0,0);
        Vector3d finalforce  = new Vector3d(0,0,0);

        if (data.isflightassiston) {
            finaltorque.add(invtorque);
            finalforce.add(invforce);
        }
        if (data.isantigravityon) {
            Vector3d gravity = DimensionPhysicsData.getGravity(
                    subLevel.getLevel(),
                    subLevel.logicalPose().position(),
                    new Vector3d()
            );
            double gravityLength = gravity.length();
            if (gravityLength > 1.0E-6D) {
                Vector3d gravityDirection = gravity.normalize(new Vector3d());
                // Function: match Sable's ServerSubLevel gravity integration scale so anti-gravity does not overcompensate.
                finalforce.fma(-mass * timeStep * SABLE_GRAVITY_IMPULSE_SCALE, gravity);

                double verticalVelocity = velocity.dot(gravityDirection);
                double verticalDampingAlpha = authorityDampingAlpha(
                        totalThrust * ANTI_GRAVITY_DAMPING_THRUST_FRACTION,
                        mass,
                        Math.abs(verticalVelocity),
                        LINEAR_REFERENCE_SPEED,
                        ANTI_GRAVITY_VERTICAL_RESPONSE,
                        timeStep
                );
                finalforce.fma(-mass * verticalVelocity * verticalDampingAlpha, gravityDirection);
            }
        }

        double torqueAlpha = smoothingAlpha(CONTROL_INPUT_RESPONSE, timeStep);
        double throttleAlpha = smoothingAlpha(THROTTLE_INPUT_RESPONSE, timeStep);

        if(controlling) {
            boolean warpRotationLocked = data.isWarpPreparing || data.hasPendingWarpTeleport;
            // Function: while warp is active, mouse torque must not rotate the ship; preparation uses auto-alignment only.
            Vec3 torque = warpRotationLocked ? Vec3.ZERO : data.getTorque();
            if (!updateWorldControlAxes(subLevel)) {
                resetControlInput();
                return;
            }

            Vec3 steeringTorque = data.isWarpPreparing ? calculateWarpPreparationTorque(subLevel,pos) : torque;
            Vec3 translationInput = data.getForce();
            if (data.hasPendingWarpTeleport && !data.isWarpPreparing) {
                smoothedControlTorque.set(0.0D, 0.0D, 0.0D);
            } else {
                smoothVector(smoothedControlTorque, steeringTorque.x, steeringTorque.y, steeringTorque.z, torqueAlpha);
            }
            smoothVector(smoothedTranslationInput, translationInput.x, translationInput.y, translationInput.z, torqueAlpha);
            smoothedThrottle += ((data.getThrottle() / 100.0D) - smoothedThrottle) * throttleAlpha;
            double torquescale = angularTorqueAuthority * CONTROL_TORQUE_SCALE * timeStep;
            Vector3d controltorque = new Vector3d(smoothedControlTorque).mul(torquescale);

            if (data.isWarpPreparing) {
                // 閸旂喕鍏橀敍姘閺冿箒鍤滈崝銊ヮ嚠閸戝棜鎻崚浼存閸婄》绱濈粩瀣祮閸︺劏鍩炴担鎾茬秴缂冾喚鏁撻幋?warp projectile閿涘苯鑻熼柅鈧崙鍝勫櫙婢跺洨濮搁幀渚€妲诲銏ゅ櫢婢跺秶鏁撻幋鎰┾偓?
                LogUtils.getLogger().warn("preparing warp...");
                tryLaunchWarpProjectile(subLevel);
            }

            Vec3 Invarianttorque = calculateWorldTorque(controltorque, worldXDirection, worldYDirection, worldZDirection);
            // Function: positive throttle must push along the seat's forward axis.
            double forcescale = -smoothedThrottle * data.thruster_strength * CONTROL_FORCE_SCALE * timeStep;
            Vec3 Invariantforce = new Vec3(worldXDirection.x * forcescale, worldXDirection.y * forcescale, worldXDirection.z * forcescale);
            double translationForceScale = data.thruster_strength * CONTROL_FORCE_SCALE * TRANSLATION_THROTTLE_EQUIVALENT * timeStep;
            Vec3 translationForce = calculateWorldTorque(new Vector3d(smoothedTranslationInput).mul(translationForceScale), worldXDirection, worldYDirection, worldZDirection);

            // 鐠侊紕鐣婚崣宥呮倻闂冭鍑归崝娑氱叐閿涘奔绗岀憴鎺椻偓鐔峰閹存劖鐦笟?
            if (Double.isNaN(torque.x()) || Double.isNaN(torque.y()) || Double.isNaN(torque.z())
                    || Double.isNaN(translationInput.x()) || Double.isNaN(translationInput.y()) || Double.isNaN(translationInput.z())) {
                return;
            }
            finaltorque.add(Vec.toVector3d(Invarianttorque));
            finalforce.add(Vec.toVector3d(Invariantforce));
            finalforce.add(Vec.toVector3d(translationForce));
            //LogUtils.getLogger().warn("finaltorque:"+finaltorque+"inverttorque:"+invtorque+"origin:"+Invarianttorque);
        } else {
            resetControlInput();
        }
        data.setFinaltorque(finaltorque);
        data.setFinalforce(finalforce);
        //閸掓媽绻栭幍宥囩暬閺傝棄濮為惇鐔割劀閻ㄥ嫬濮?
        ServerShipUtils.applyWorldForceAndTorqueAtCenterOfMass(subLevel,finalforce,finaltorque);
    }

    // 閸旂喕鍏橀敍姝竌rp 閸戝棗顦悩鑸碘偓浣风瑓閺嶈宓侀幒褍鍩楀鍛閸氭垳绗岄惄顔界垼閺傜懓鎮滈惃鍕仚鐟欐帞鏁撻幋鎰殰閸斻劌顕崙鍡樺閻晪绱辩紒鎾寸亯鐞氼偊妾洪崚璺烘躬閹靛濮╂Η鐘崇垼閹貉冨煑閻ㄥ嫭娓舵径褑绶崗銉ㄥ瘱閸ユ潙鍞撮妴?
    private static double smoothingAlpha(double response, double timeStep) {
        return Mth.clamp(1.0D - Math.exp(-response * timeStep), 0.0D, 1.0D);
    }

    private static double authorityDampingAlpha(double authority, double inertia, double speed, double referenceSpeed, double response, double timeStep) {
        if (authority <= AXIS_EPSILON || inertia <= AXIS_EPSILON || timeStep <= 0.0D) {
            return 0.0D;
        }

        double effectiveSpeed = Math.sqrt(speed * speed + referenceSpeed * referenceSpeed);
        double dampingRate = (authority / inertia) * response / effectiveSpeed;
        return smoothingAlpha(dampingRate, timeStep);
    }

    private static double averageInertia(Matrix3dc inertia) {
        if (inertia == null) {
            return AXIS_EPSILON;
        }

        return Math.max((Math.abs(inertia.m00()) + Math.abs(inertia.m11()) + Math.abs(inertia.m22())) / 3.0D, AXIS_EPSILON);
    }

    private static boolean isUsableMassProperties(double mass, Matrix3dc inertia, double averageInertia) {
        return Double.isFinite(mass)
                && mass >= MIN_VALID_MASS
                && inertia != null
                && isFiniteMatrix(inertia)
                && inertia.m00() > AXIS_EPSILON
                && inertia.m11() > AXIS_EPSILON
                && inertia.m22() > AXIS_EPSILON
                && Double.isFinite(averageInertia)
                && averageInertia >= MIN_VALID_INERTIA;
    }

    private static boolean isFiniteMatrix(Matrix3dc matrix) {
        return Double.isFinite(matrix.m00()) && Double.isFinite(matrix.m01()) && Double.isFinite(matrix.m02())
                && Double.isFinite(matrix.m10()) && Double.isFinite(matrix.m11()) && Double.isFinite(matrix.m12())
                && Double.isFinite(matrix.m20()) && Double.isFinite(matrix.m21()) && Double.isFinite(matrix.m22());
    }

    private static boolean isFiniteVector(Vector3d vector) {
        return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }

    private void updateSmoothedMassProperties(double mass, double averageInertia, double timeStep) {
        double alpha = smoothingAlpha(MASS_PROPERTY_RESPONSE, timeStep);
        smoothedMass = smoothPositiveMetric(smoothedMass, mass, alpha);
        smoothedAverageInertia = smoothPositiveMetric(smoothedAverageInertia, averageInertia, alpha);
    }

    private static double smoothPositiveMetric(double current, double target, double alpha) {
        if (!Double.isFinite(current) || current <= 0.0D) {
            return target;
        }

        return current + (target - current) * alpha;
    }

    private double conservativeMass(double rawMass) {
        if (!Double.isFinite(smoothedMass) || smoothedMass <= 0.0D) {
            return rawMass;
        }

        return Math.max(Math.min(rawMass, smoothedMass), MIN_VALID_MASS);
    }

    private double conservativeAverageInertia(double rawAverageInertia) {
        if (!Double.isFinite(smoothedAverageInertia) || smoothedAverageInertia <= 0.0D) {
            return rawAverageInertia;
        }

        return Math.max(Math.min(rawAverageInertia, smoothedAverageInertia), MIN_VALID_INERTIA);
    }

    private static Vector3d transformWithConservativeInertia(Matrix3dc inertia, Vector3d localDeltaOmega, double rawAverageInertia, double effectiveAverageInertia) {
        Vector3d angularImpulse = new Vector3d(localDeltaOmega);
        inertia.transform(angularImpulse);
        if (!isFiniteVector(angularImpulse) || !Double.isFinite(rawAverageInertia) || rawAverageInertia <= AXIS_EPSILON) {
            return new Vector3d();
        }

        return angularImpulse.mul(effectiveAverageInertia / rawAverageInertia);
    }

    private static Vector3d calculateFlightAssistTorque(
            ServerSubLevel subLevel,
            Matrix3dc inertia,
            Vector3d worldOmega,
            double angularDampingAlpha,
            double rawAverageInertia,
            double effectiveAverageInertia
    ) {
        if (angularDampingAlpha <= 0.0D || worldOmega.length() < ANGULAR_ASSIST_REST_SPEED) {
            return new Vector3d();
        }

        Vector3d localOmega = new Vector3d(worldOmega);
        subLevel.logicalPose().orientation().transformInverse(localOmega);
        if (!isFiniteVector(localOmega)) {
            return new Vector3d();
        }

        Vector3d localDeltaOmega = localOmega.negate(new Vector3d()).mul(angularDampingAlpha);
        Vector3d localAngularImpulse = transformWithConservativeInertia(inertia, localDeltaOmega, rawAverageInertia, effectiveAverageInertia);
        Vector3d worldAngularImpulse = new Vector3d(localAngularImpulse);
        subLevel.logicalPose().orientation().transform(worldAngularImpulse);

        return isFiniteVector(worldAngularImpulse) ? worldAngularImpulse : new Vector3d();
    }

    private static double angularTorqueAuthority(double totalThrust, double mass, double averageInertia) {
        if (totalThrust <= AXIS_EPSILON) {
            return 0.0D;
        }

        double radiusOfGyration = Math.sqrt(Math.max(averageInertia, AXIS_EPSILON) / Math.max(mass, AXIS_EPSILON));
        return totalThrust * Math.max(radiusOfGyration, 0.5D);
    }

    private static void smoothVector(Vector3d current, double targetX, double targetY, double targetZ, double alpha) {
        current.x += (targetX - current.x) * alpha;
        current.y += (targetY - current.y) * alpha;
        current.z += (targetZ - current.z) * alpha;
    }

    private boolean updateWorldControlAxes(ServerSubLevel subLevel) {
        Vec3 rawForward = transformSeatAxis(subLevel, data.getDirectionForward());
        Vec3 rawUp = transformSeatAxis(subLevel, data.getDirectionUp());
        Vec3 rawRight = transformSeatAxis(subLevel, data.getDirectionRight());
        if (!isUsableAxis(rawForward) || !isUsableAxis(rawUp) || !isUsableAxis(rawRight)) {
            return false;
        }

        // Function: yaw/pitch must follow the control seat's own transformed axes, not the ship body's canonical axes.
        Vec3 up = rawUp.normalize();
        Vec3 right = rawRight.subtract(up.scale(rawRight.dot(up)));
        if (right.lengthSqr() < AXIS_EPSILON) {
            right = rawForward.cross(up);
        }
        if (right.lengthSqr() < AXIS_EPSILON) {
            return false;
        }
        right = right.normalize();

        Vec3 forward = up.cross(right);
        if (forward.lengthSqr() < AXIS_EPSILON) {
            return false;
        }
        forward = forward.normalize();
        if (forward.dot(rawForward) < 0.0D) {
            right = right.scale(-1.0D);
            forward = up.cross(right).normalize();
        }

        worldXDirection = forward;
        worldYDirection = up;
        worldZDirection = right;
        return true;
    }

    private static Vec3 transformSeatAxis(ServerSubLevel subLevel, Vec3i localAxis) {
        return subLevel.logicalPose().transformNormal(Vec3.atLowerCornerOf(localAxis));
    }

    private static boolean isUsableAxis(Vec3 axis) {
        return axis != null
                && Double.isFinite(axis.x)
                && Double.isFinite(axis.y)
                && Double.isFinite(axis.z)
                && axis.lengthSqr() >= AXIS_EPSILON;
    }

    private Vec3 calculateWarpPreparationTorque(ServerSubLevel subLevel,BlockPos pos) {
        if (data.warpTargetName == null || data.warpTargetName.isEmpty() || data.warpTargetPos == null || data.warpTargetPos.equals(BlockPos.ZERO)) {
            return new Vec3(0, 0, 0);
        }

        Vec3 targetDirection = getNormalizedWarpTargetDirection(pos);
        if (targetDirection == null) {
            return new Vec3(0, 0, 0);
        }

        Vec3 currentForward = worldXDirection.normalize();
        // 閸旂喕鍏橀敍姘冲殰閸斻劌顕崙鍡涙付鐟曚胶鏁撻幋鎰ㄢ偓婊€绮犺ぐ鎾冲閺堟繂鎮滄潪顒€鍩岄惄顔界垼閺堟繂鎮滈垾婵堟畱閸欒櫕澧滈弮瀣祮鏉炶揪绱辨担璺ㄦ暏 target x current 娴兼碍濡搁幍顓犵叐閺傜懓鎮滈崣宥堢箖閺夈儻绱濈€佃壈鍤ч幒褍鍩楀鍛纯缂佹洜娲伴弽鍥у冀閺傜懓鎮滈幗鍡楀З閵?
        Vec3 rotationAxisWorld = targetDirection.cross(currentForward);
        if (rotationAxisWorld.lengthSqr() < 1.0E-6) {
            return new Vec3(0, 0, 0);
        }

        double alignment = Mth.clamp(currentForward.dot(targetDirection), -1.0D, 1.0D);
        double angleStrength = Mth.clamp((1.0D - alignment) * 2.0D, 0.0D, 1.0D);
        rotationAxisWorld.normalize();
        rotationAxisWorld.scale(angleStrength);
        double factor = subLevel.getMassTracker().getMass();
        // 閸旂喕鍏橀敍姘涧娴ｈ法鏁?yaw/pitch 娑撱倓閲滄潪纾嬬箻鐞涘矁鍤滈崝銊ヮ嚠閸戝棴绱濋柆鍨帳 warp 閸戝棗顦梼鑸殿唽缂佹瑦甯堕崚鑸殿槳瀵洖鍙嗘０婵嗩樆濠婃俺娴嗛妴?
        double localYawTorque = Mth.clamp(rotationAxisWorld.dot(worldYDirection) * WARP_ALIGNMENT_TORQUE_SCALE, -factor, factor);
        double localPitchTorque = Mth.clamp(rotationAxisWorld.dot(worldZDirection) * WARP_ALIGNMENT_TORQUE_SCALE, -factor, factor);
        return new Vec3(0, localYawTorque, localPitchTorque);
    }

    // 閸旂喕鍏橀敍姘梾閺屻儱缍嬮崜宥堝煘妫ｆ牗妲搁崥锕€鍑＄€电懓鍣?warp 閻╊喗鐖ｉ敍娑滃婢剁顫楃亸蹇庣艾 1 鎼达讣绱濋崚娆愬瘻閼搁€涚秼閺堚偓婢堆冨瘶閸ュ娲呯亸鍝勵嚟閻㈢喐鍨?warp projectile閵?
    private void tryLaunchWarpProjectile(ServerSubLevel subLevel) {
        if (data.hasPendingWarpTeleport) {
            return;
        }
        if (data.warpTargetPos == null || data.warpTargetPos.equals(BlockPos.ZERO)) {
            return;
        }

        Vec3 launchDirection = getNormalizedWarpTargetDirection(data.controlSeatPos);
        if (launchDirection == null) {
            return;
        }

        double alignment = Mth.clamp(worldXDirection.dot(launchDirection), -1.0D, 1.0D);
        double angleDegrees = Math.toDegrees(Math.acos(alignment));
        if (Math.abs(angleDegrees-180) >= WARP_ALIGNMENT_THRESHOLD_DEGREES) {
            return;
        }
        Level level = data.level;
        Vec3 shipPos = ServerShipUtils.getStructureCenterWorld(subLevel);
        if (level == null || shipPos == null) {
            return;
        }

        double structureMaxDimension = ServerShipUtils.getStructureMaxDimension(subLevel);
        if (structureMaxDimension <= 0.0D) {
            return;
        }
        double projectileTravelDistance = structureMaxDimension * WARP_PROJECTILE_DISTANCE_SCALE;

        WarpProjecTileEntity warpProjectile = new WarpProjecTileEntity(vsieEntities.WARP_PROJECTILE.get(), level);
        // Function: launch from the sublevel center, fly past the hull, and keep FX scale based on the source bounds.
        warpProjectile.setPos(shipPos.x(), shipPos.y(), shipPos.z());
        warpProjectile.configureLaunch(
                new net.minecraft.world.phys.Vec3(launchDirection.x, launchDirection.y, launchDirection.z),
                projectileTravelDistance,
                structureMaxDimension
        );
        level.addFreshEntity(warpProjectile);

        // Function: teleport only after the projectile has flown the full bounds-derived distance.
        long executeGameTime = level.getGameTime()
                + WarpProjecTileEntity.lifeTicksForDistance(projectileTravelDistance)
                + WARP_TELEPORT_EXTRA_DELAY_TICKS;
        data.schedulePendingWarpTeleport(new Vector3d(
                data.warpTargetPos.getX() + 0.5D,
                data.warpTargetPos.getY() + 0.5D,
                data.warpTargetPos.getZ() + 0.5D
        ), executeGameTime);
        data.clearWarpPreparation();
        syncWarpPreparationState();
    }

    // 閸旂喕鍏橀敍姘躬閺堝秴濮熼崳?tick 閸掓媽鎻０鍕暰閺冨爼妫块弮鎯扮殶閻?teleportship閿涘本濡搁懜鐟板涧娴肩娀鈧礁鍩屾稊瀣闁夸礁鐣鹃惃鍕┈鏉╀胶娲伴弽鍥モ偓?
    private void processPendingWarpTeleport(ServerSubLevel subLevel) {
        Level level = data.level;
        if (level == null || level.isClientSide() || !data.hasPendingWarpTeleport) {
            return;
        }
        if (level.getGameTime() < data.pendingWarpTeleportGameTime) {
            return;
        }
        // Function: copy the pending target before clearing state, otherwise clearPendingWarpTeleport resets it to zero.
        Vector3d pendingTeleportPos = new Vector3d(data.pendingWarpTeleportPos);
        if (ServerShipUtils.teleportKeepOrientation(subLevel, pendingTeleportPos)) {
            data.clearPendingWarpTeleport();
        }
    }

    // 閸旂喕鍏橀敍姘槻閻劍甯堕崚鑸殿槳閸掓壆娲伴弽鍥╁仯閻ㄥ嫬缍婃稉鈧崠鏍ㄦ煙閸氭垼顓哥粻妤嬬礉娓氭稖鍤滈崝銊ヮ嚠閸戝棔绗?warp projectile 閸欐垵鐨犻崗杈╂暏閸氬奔绔撮弬鐟版倻閸╁搫鍣妴?
    private Vec3 getNormalizedWarpTargetDirection(BlockPos pos) {
        SubLevel sublevel = ServerShipUtils.getSubLevelAtBlockPos(data.level,pos);
        Vec3 seatWorldPos = ServerShipUtils.getBlockCenterWorld(sublevel, pos);
        Vec3 targetDirection = new Vec3(
                data.warpTargetPos.getX() + 0.5 - seatWorldPos.x,
                data.warpTargetPos.getY() + 0.5 - seatWorldPos.y,
                data.warpTargetPos.getZ() + 0.5 - seatWorldPos.z
        );
        if (targetDirection.lengthSqr() < 1.0E-6D) {
            return null;
        }
        return targetDirection.normalize();
    }

    // 閸旂喕鍏橀敍姝竌rp 閸戝棗顦悩鑸碘偓浣虹波閺夌喎鎮楃粩瀣煝閹跺﹥甯堕崚鑸殿槳閺傜懓娼＄€圭偘缍嬮崥灞绢劄缂佹瑥顓归幋椋庮伂閿涘矂浼╅崗宥咁吂閹撮顏禒宥嗘▔缁€鐑樻＋閻ㄥ嫬鍣径鍥╁Ц閹降鈧?
    private void syncWarpPreparationState() {
        if (data.level == null || data.controlSeatPos == null) {
            return;
        }
        if (!(data.level.getBlockEntity(data.controlSeatPos) instanceof ControlSeatBlockEntity controlSeat)) {
            return;
        }
        controlSeat.setChanged();
    }

    public static Vec3 calculateWorldTorque(Vector3d localTorque, Vec3 worldDirectionX, Vec3 worldDirectionY, Vec3 worldDirectionZ) {
        // 閺冨娴嗛惌鈺呮█閺勵垳鏁遍幒褍鍩楀鍖? Y, Z鏉炴潙婀稉鏍櫕閸ф劖鐖ｇ化璁崇瑓閻ㄥ嫬宕熸担宥呮倻闁插繑鐎幋鎰畱
        // 閺嬪嫬缂撻弮瀣祮閻晠妯€
        double[][] rotationMatrix = new double[3][3];
        rotationMatrix[0][0] = worldDirectionX.x;
        rotationMatrix[0][1] = worldDirectionY.x;
        rotationMatrix[0][2] = worldDirectionZ.x;

        rotationMatrix[1][0] = worldDirectionX.y;
        rotationMatrix[1][1] = worldDirectionY.y;
        rotationMatrix[1][2] = worldDirectionZ.y;

        rotationMatrix[2][0] = worldDirectionX.z;
        rotationMatrix[2][1] = worldDirectionY.z;
        rotationMatrix[2][2] = worldDirectionZ.z;

        // 閺嶈宓侀弮瀣祮閻晠妯€閸滃苯鐪柈銊ユ綏閺嶅洨閮撮惃鍕閻晜娼电拋锛勭暬娑撴牜鏅崸鎰垼缁绗呴惃鍕閻?
        double a = rotationMatrix[0][0] * localTorque.x + rotationMatrix[0][1] * localTorque.y + rotationMatrix[0][2] * localTorque.z;
        double b = rotationMatrix[1][0] * localTorque.x + rotationMatrix[1][1] * localTorque.y + rotationMatrix[1][2] * localTorque.z;
        double c = rotationMatrix[2][0] * localTorque.x + rotationMatrix[2][1] * localTorque.y + rotationMatrix[2][2] * localTorque.z;
        return new Vec3(a,b,c);
        // 鏉╂柨娲栨稉鏍櫕閸ф劖鐖ｇ化璁崇瑓d(a, b, c);
    }

}
