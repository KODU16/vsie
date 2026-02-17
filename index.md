# VSCore API 文档 | VSCore API Document
# 适用于 VSCore API 1.1.0 | Works in VSCore API 1.1.0
# 目录是自动生成的，有点小问题，某些目录可能没法跳转，你可以尝试点击它周围的条目来导航到它附近
---

# 目录 | Contents
- [attachment | `org.valkyrienskies.core.api.attachment`](#attachment--orgvalkyrienskiescoreapiattachment)
    - [`interface AttachmentHolder`](#interface-attachmentholder)
    - [`interface AttachmentRegistration<T>`](#interface-attachmentregistrationt)
    - [`interface AttachmentSerializer`](#interface-attachmentserializer)
    - [`object JacksonAttachmentSerializer` : `AttachmentSerializer`](#object-jacksonattachmentserializer--attachmentserializer)
    - [`object LegacyAttachmentSerializer` : `AttachmentSerializer`](#object-legacyattachmentserializer--attachmentserializer)
    - [`object TransientAttachmentSerializer` : `AttachmentSerializer`](#object-transientattachmentserializer--attachmentserializer)
    - [`interface BinaryAttachmentSerializer<T>` : `AttachmentSerializer`](#interface-binaryattachmentserializert--attachmentserializer)
- [bodies | `org.valkyrienskies.core.api.bodies`](#bodies--orgvalkyrienskiescoreapibodies)
    - [`interface BaseVsBody` : `Identified`](#interface-basevsbody--identified)
    - [`interface PhysicsVsBody` : `ServerBaseVsBody`](#interface-physicsvsbody--serverbasevsbody)
    - [`interface ServerBaseVsBody` : `BaseVsBody`](#interface-serverbasevsbody--basevsbody)
    - [`interface ServerVsBody` : `ServerBaseVsBody`](#interface-servervsbody--serverbasevsbody)
    - [`interface VsBody` : `BaseVsBody`](#interface-vsbody--basevsbody)
  - [shape | `org.valkyrienskies.core.api.bodies.shape`](#shape--orgvalkyrienskiescoreapibodiesshape)
    - [`interface BodyShape`](#interface-bodyshape)
    - [`interface Box` : `BodyShape`](#interface-box--bodyshape)
    - [`interface Capsule` : `BodyShape`](#interface-capsule--bodyshape)
    - [`interface Sphere` : `BodyShape`](#interface-sphere--bodyshape)
    - [`interface Voxel` : `BodyShape`](#interface-voxel--bodyshape)
    - [`interface VoxelType`](#interface-voxeltype)
    - [`interface VoxelUpdate `](#interface-voxelupdate-)
    - [`interface Wheel` : `BodyShape`](#interface-wheel--bodyshape)
  - [properties | `org.valkyrienskies.core.api.bodies.properties`](#properties--orgvalkyrienskiescoreapibodiesproperties)
    - [`interface BodyInertia`](#interface-bodyinertia)
    - [`interface BodyKinematics`](#interface-bodykinematics)
    - [`interface BodyPose`](#interface-bodypose)
    - [`interface BodyTransform`](#interface-bodytransform)
      - [`fun withVelocity()` : `BodyKinematics`](#fun-withvelocity--bodykinematics)
      - [`fun toBuilder(): Builder`](#fun-tobuilder-builder)
      - [`fun writeTransform()`](#fun-writetransform)
    - [`BodyIdKt`](#bodyidkt)
- [event `org.valkyrienskies.core.api.event`](#event-orgvalkyrienskiescoreapievent)
    - [`interface EmittableEvent<in T> `](#interface-emittableeventin-t-)
    - [`fun interface EventConsumer<in T>`](#fun-interface-eventconsumerin-t)
    - [`interface ListenableEvent<out T>`](#interface-listenableeventout-t)
    - [`fun interface RegisteredListener`](#fun-interface-registeredlistener)
    - [`interface SingleEvent<T>` : `ListenableEvent<T>`, `EmittableEvent<T>`](#interface-singleeventt--listenableeventt-emittableeventt)
- [events `org.valkyrienskies.core.api.events`](#events-orgvalkyrienskiescoreapievents)
    - [`interface CollisionEvent`](#interface-collisionevent)
    - [`interface MergeEvent`](#interface-mergeevent)
    - [`interface PhysTickEvent`](#interface-phystickevent)
    - [`interface ShipLoadEvent`](#interface-shiploadevent)
    - [`interface ShipLoadEventClient`](#interface-shiploadeventclient)
    - [`interface ShipUnloadEventClient`](#interface-shipunloadeventclient)
    - [`interface SplitEvent`](#interface-splitevent)
    - [`interface StartUpdateRenderTransformsEvent`](#interface-startupdaterendertransformsevent)
    - [`interface TickEndEvent`](#interface-tickendevent)
- [physics  `org.valkyrienskies.core.api.physics`](#physics--orgvalkyrienskiescoreapiphysics)
    - [`data class RayCastResult`](#data-class-raycastresult)
    - [`interface ContactPoint`](#interface-contactpoint)
  - [blockstates `org.valkyrienskies.core.api.physics.blockstates`](#blockstates-orgvalkyrienskiescoreapiphysicsblockstates)
    - [`interface BlockShape`](#interface-blockshape)
    - [`interface BoxBlockShape` : `LiquidBlockShape`](#interface-boxblockshape--liquidblockshape)
    - [`interface BoxesBlockShape` : `SolidBlockShape`](#interface-boxesblockshape--solidblockshape)
    - [`data class CollisionPoint`](#data-class-collisionpoint)
    - [`interface LiquidBlockShape` :  `BlockShape`](#interface-liquidblockshape---blockshape)
    - [`interface LiquidState`](#interface-liquidstate)
    - [`interface SolidBlockShape` : `BlockShape`](#interface-solidblockshape--blockshape)
    - [`interface SolidState`](#interface-solidstate)
- [ships | `org.valkyrienskies.core.api.ships`](#ships--orgvalkyrienskiescoreapiships)
    - [`interface ClientShip` :   `LoadedShip`](#interface-clientship----loadedship)
    - [`interface ClientShipTransformProvider`](#interface-clientshiptransformprovider)
    - [`interface ContraptionWingProvider`](#interface-contraptionwingprovider)
    - [`interface DragController`](#interface-dragcontroller)
    - [`interface LoadedServerShip` : `AttachmentHolder`, `LoadedShip`, `ServerShip`](#interface-loadedservership--attachmentholder-loadedship-servership)
    - [`interface LoadedShip` : `Ship`](#interface-loadedship--ship)
    - [`interface PhysShip` : `Ship`](#interface-physship--ship)
    - [`interface QueryableShipData<out ShipType : Ship>` : `Collection<ShipType> `](#interface-queryableshipdataout-shiptype--ship--collectionshiptype-)
    - [`interface ServerShip` : `Ship`](#interface-servership--ship)
    - [`interface ServerShipTransformProvider`](#interface-servershiptransformprovider)
    - [`interface ServerTickListener`](#interface-serverticklistener)
    - [`interface Ship` : `Identified`](#interface-ship--identified)
    - [`interface ShipPhysicsListener`](#interface-shipphysicslistener)
    - [`data class Wing`](#data-class-wing)
    - [`data class PositionedWing`](#data-class-positionedwing)
    - [`interface WingManager`](#interface-wingmanager)
    - [`data class WingGroupChanges`](#data-class-winggroupchanges)
    - [`data class WingManagerChanges`](#data-class-wingmanagerchanges)
  - [properties | `org.valkyrienskies.core.api.ships.properties`](#properties--orgvalkyrienskiescoreapishipsproperties)
    - [`interface ChunkClaim`](#interface-chunkclaim)
    - [`interface IShipActiveChunksSet`](#interface-ishipactivechunksset)
    - [`interface ShipTransform` : `BodyTransform`](#interface-shiptransform--bodytransform)
    - [`ShipIdKt`](#shipidkt)
- [util `org.valkyrienskies.core.api.util`](#util-orgvalkyrienskiescoreapiutil)
    - [`interface Identified`](#interface-identified)
    - [`data class DoublePair`](#data-class-doublepair)
    - [`interface AerodynamicUtils`](#interface-aerodynamicutils)
    - [VS作者说不应该碰的东西](#vs作者说不应该碰的东西)
  - [functions | `org.valkyrienskies.core.api.util.functions`](#functions--orgvalkyrienskiescoreapiutilfunctions)
    - [`fun interface IntTernaryConsumer`](#fun-interface-intternaryconsumer)
    - [`fun interface DoubleTernaryConsumer`](#fun-interface-doubleternaryconsumer)
    - [`fun interface IntBinaryConsumer`](#fun-interface-intbinaryconsumer)
- [world | `org.valkyrienskies.core.api.world`](#world--orgvalkyrienskiescoreapiworld)
    - [`interface ClientShipWorld` : `ShipWorld`](#interface-clientshipworld--shipworld)
    - [`data class LevelYRange`](#data-class-levelyrange)
    - [`interface PhysLevel`](#interface-physlevel)
      - [`fun getShipById() : PhysShip?`](#fun-getshipbyid--physship)
      - [`fun physTick()`](#fun-phystick)
      - [`fun rayCast() : RayCastResult?`](#fun-raycast--raycastresult)
      - [`fun enableCollisionBetween() : Boolean`](#fun-enablecollisionbetween--boolean)
      - [`fun disableCollisionBetween() : Boolean`](#fun-disablecollisionbetween--boolean)
    - [`interface ServerShipWorld` : `ShipWorld`](#interface-servershipworld--shipworld)
    - [`interface ShipWorld`](#interface-shipworld)
  - [properties | `org.valkyrienskies.core.api.world.properties`](#properties--orgvalkyrienskiescoreapiworldproperties)
    - [`DimensionIdKt`](#dimensionidkt)
  - [connectivity | `org.valkyrienskies.core.api.world.connectivity`](#connectivity--orgvalkyrienskiescoreapiworldconnectivity)
    - [`interface Component`](#interface-component)
    - [`enum class ConnectionStatus`](#enum-class-connectionstatus)
    - [`interface DoubleAugmentation`](#interface-doubleaugmentation)
    - [`interface DoubleComponentAugmentation` : `DoubleAugmentation`](#interface-doublecomponentaugmentation--doubleaugmentation)

---

## attachment | `org.valkyrienskies.core.api.attachment`

#### `interface AttachmentHolder`

<br>

#### `interface AttachmentRegistration<T>`

<br>

#### `interface AttachmentSerializer`

<br>

#### `object JacksonAttachmentSerializer` : [`AttachmentSerializer`](#interface-attachmentserializer)

<br>

#### `object LegacyAttachmentSerializer` : [`AttachmentSerializer`](#interface-attachmentserializer)

<br>

#### `object TransientAttachmentSerializer` : [`AttachmentSerializer`](#interface-attachmentserializer)

<br>

#### `interface BinaryAttachmentSerializer<T>` : [`AttachmentSerializer`](#interface-attachmentserializer)


---

## bodies | `org.valkyrienskies.core.api.bodies`

#### `interface BaseVsBody` : [`Identified`](#interface-identified)

<br>

#### `interface PhysicsVsBody` : [`ServerBaseVsBody`](#interface-serverbasevsbody--basevsbody)

<br>

#### `interface ServerBaseVsBody` : [`BaseVsBody`](#interface-basevsbody--identified)

<br>

#### `interface ServerVsBody` : [`ServerBaseVsBody`](#interface-serverbasevsbody--basevsbody)

<br>

#### `interface VsBody` : [`BaseVsBody`](#interface-basevsbody--identified)

<br>

### shape | `org.valkyrienskies.core.api.bodies.shape`

#### `interface BodyShape`

<br>

#### `interface Box` : [`BodyShape`](#interface-bodyshape)

<br>

#### `interface Capsule` : [`BodyShape`](#interface-bodyshape)

<br>

#### `interface Sphere` : [`BodyShape`](#interface-bodyshape)

<br>

#### `interface Voxel` : [`BodyShape`](#interface-bodyshape)

<br>

#### `interface VoxelType`

<br>

#### `interface VoxelUpdate `

<br>

#### `interface Wheel` : [`BodyShape`](#interface-bodyshape)

<br>

### properties | `org.valkyrienskies.core.api.bodies.properties`

#### `interface BodyInertia`
属性：
- `inertiaTensor: Matrix3dc`    物理体的惯性张量
- `centerOfMass: Vector3dc` 物理体在**物理体坐标系**中的质心位置
- `mass: Double`    物理体的质量，单位为kg
>作者贴了一些开盖即食的代码，免去了学习普通物理学的痛苦
>
>计算角动量：
>`val angularMomentum: Vector3d = inertiaTensor. transform(Vector3d(omega))`
>
>计算绕某轴转动的转动惯量：
>1. 规范化成单位向量
>`val unitAxis: Vector3d = Vector3d(axis).normalize()`
>2. 使用惯性张量进行基变换
>`val transformedAxis: Vector3d = inertiaTensor. transform(Vector3d(unitAxis))`
>3. 使用基变换之后的转动轴计算转动惯量
>`val momentOfInertia: Double = unitAxis. dot(transformedAxis)`
>
>如果你想知道这是为什么，就去学物理吧，API作者在这里贴了《费曼物理学讲义》的链接


<br>

#### `interface BodyKinematics`

<br>

#### `interface BodyPose`

<br>

#### `interface BodyTransform`
表示各种东西的各种变换，船只或物体的，坐标，旋转，缩放，或者其他的什么

属性：
- `position: Vector3dc` 物理体在**世界坐标系**中的位置
- `rotation: Quaterniondc`  物理体在**世界坐标系**下的旋转

物理体自身具有旋转，所以物理体坐标系和世界坐标系下的方向是**不同**的，这时，
使用
`val worldDirection = rotation.transform(modelDirection)`
可以将**物理体坐标系中的方向**换算成**世界坐标系中的方向**

使用
`val modelDirection = rotation.transformInverse(worldDirection)`
可以将**世界坐标系中的方向**换算成**物理体坐标系中的方向**

- `scaling: Vector3dc`

就缩放，一般正常比例的船只的缩放值是`(1, 1, 1)`
如果你改变了这个缩放的值，物理体本身就会根据这个值变大变小，这时，
使用
`val worldScaledDirection = Vector3d(modelDirection).mul(scaling)`
可以进行“乘以缩放倍数”操作，这会将**世界中的大小**映射为**物理体中的大小**

使用
`val modelScaledDirection = Vector3d(worldDirection).div(scaling)`
可以进行“除以缩放倍数”操作，这会将**物理体中的大小**映射为**世界中的大小**

>Note: The physics currently don't support non-uniform scaling, but this may change in the future.
>
>作者说：物理操作现在不支持不均匀的（即各个方向缩放倍数不同的）缩放，以后的版本可能会支持

- `positionInModel: Vector3dc`

物理体相对于**物理体坐标系**所在的位置，一般等于物理体的质心位置
>The position of the body in model space. On a ship, this is the position of the ship in the shipyard.
>This is typically equivalent to the body's center of mass.
>
>在模型空间中物理体的位置。在船上，这是船只在船坞里的位置
>这一般等于物理体的质心位置
>
>推测：VS的船只实际上是由物理体+碰撞箱构成的，物理体负责物理运算，碰撞箱负责碰撞相关的计算
>这个位置代表的是物理体的中心，为了正常计算，物理体的中心就应该在船只的质心处，所以才有这个属性
>某些附属中，某些奇异的设备或功能可能就是依靠修改这个实现的


- `toWorld: Matrix4dc`

一个仿射变换矩阵，它能把物理体中的位置或者方向进行合适的变换，映射到世界坐标系中
使用
`val worldPosition = toWorld.transformPosition(Vector3d(modelPosition))`
可以将**物理体坐标系**中的位置变换成**世界坐标系**中的位置

`val worldDirection = toWorld.transformDirection(Vector3d(modelDirection))`
可以将**物理体坐标系**中的方向变换成**世界坐标系**中的方向，同时会根据scaling进行伸缩
如果不需要伸缩，你应该使用rotation中的方法

- `toModel: Matrix4dc`

其实就是上面的东西反过来
一个仿射变换矩阵，它能把世界坐标系中的位置或者方向进行合适的变换，映射到物理体中
使用
`val modelPosition = toModel.transformPosition(Vector3d(worldPosition))`
可以将**世界坐标系**中的位置变换成**物理体坐标系**中的位置

`val modelDirection = toModel.transformDirection(Vector3d(worldDirection))`
可以将**世界坐标系**中的方向变换成**物理体坐标系**中的方向，同时会根据scaling进行伸缩
如果不需要伸缩，你应该使用rotation中的方法

<br>

##### `fun withVelocity()` : [`BodyKinematics`](#interface-bodykinematics)
形参列表：
- `velocity: Vector3dc`
- `angularVelocity: Vector3dc`

##### `fun toBuilder(): Builder`

##### `fun writeTransform()`
形参列表：
- `output`: `DataOutput`

<br>

#### `BodyIdKt`


---

## event `org.valkyrienskies.core.api.event`

#### `interface EmittableEvent<in T> `

<br>

#### `fun interface EventConsumer<in T>`

<br>

#### `interface ListenableEvent<out T>`

<br>

#### `fun interface RegisteredListener`

<br>

#### `interface SingleEvent<T>` : [`ListenableEvent<T>`](#interface-listenableeventout-t), [`EmittableEvent<T>`](#interface-emittableeventin-t)

---

## events `org.valkyrienskies.core.api.events`

#### `interface CollisionEvent`

<br>

#### `interface MergeEvent`

<br>

#### `interface PhysTickEvent`

<br>

#### `interface ShipLoadEvent`

<br>

#### `interface ShipLoadEventClient`

<br>

#### `interface ShipUnloadEventClient`

<br>

#### `interface SplitEvent`

<br>

#### `interface StartUpdateRenderTransformsEvent`

<br>

#### `interface TickEndEvent`

---

## physics  `org.valkyrienskies.core.api.physics`

#### `data class RayCastResult`
射线投射结果

属性：
- `hitBody`: [`PhysShip`](#interface-physship--ship)  被击中的船只
- `distance: Double`   被击中船只的距离
- `hitNormal: Vector3dc`   被击中平面的法向量
- `velocity: Vector3dc`    被击中平面的线速度
- `angularVelocity: Vector3dc` 被击中平面的角速度

<br>

#### `interface ContactPoint`
船只之间的接触点

属性：
- `position: Vector3dc`    接触点的位置（坐标系不明，推测应为世界坐标系）
- `normal: Vector3dc`  接触点的法向量（？？？）
- `separation: Float`  接触点的分离（距离？），正值为相互分离，负值对应穿模
- `velocity: Vector3dc`    接触点处的相对速度

<br>

### blockstates `org.valkyrienskies.core.api.physics.blockstates`

#### `interface BlockShape`

<br>

#### `interface BoxBlockShape` : [`LiquidBlockShape`](#interface-liquidblockshape------blockshape)

<br>

#### `interface BoxesBlockShape` : [`SolidBlockShape`](#interface-solidblockshape-------blockshape)

<br>

#### `data class CollisionPoint`
表示碰撞体中的碰撞点

属性：
- `x: Double`
- `y: Double`
- `z: Double`
- `radius: Double`

<br>

#### `interface LiquidBlockShape` : [ `BlockShape`](#interface-blockshape)

<br>

#### `interface LiquidState`

<br>

#### `interface SolidBlockShape` : [`BlockShape`](#interface-blockshape)

<br>

#### `interface SolidState`


---

## ships | `org.valkyrienskies.core.api.ships`

#### `interface ClientShip` :   `LoadedShip`

<br>

#### `interface ClientShipTransformProvider`

<br>

#### `interface ContraptionWingProvider`

<br>

#### `interface DragController`

<br>

#### `interface LoadedServerShip` : [`AttachmentHolder`](#interface-attachmentholder), [`LoadedShip`](#interface-loadedship--ship), [`ServerShip`](#interface-servership--ship)

<br>

#### `interface LoadedShip` : [`Ship`](#interface-ship--identified)

<br>

#### `interface PhysShip` : [`Ship`](#interface-ship--identified)

<br>

#### `interface QueryableShipData<out ShipType : Ship>` : `Collection<ShipType> `

<br>

#### `interface ServerShip` : [`Ship`](#interface-ship--identified)

<br>

#### `interface ServerShipTransformProvider`

<br>

#### `interface ServerTickListener`

<br>

#### `interface Ship` : [`Identified`](#interface-identified)
船只的抽象接口，几乎是API里最重要的东西，这是所有种类Ship的基本接口
以下的`shipTransform`可以直接参考[`BodyTransform`](#interface-bodytransform)，两者几乎只有名字上的区别

属性：
- `id`: [`ShipId`](#shipidkt)    船只的id，不能为-1
- `slug: String?`   船只的字符串形式的id，用于ui操作，例如`/vs`指令
- `kinematics`: [`BodyKinematics`](#interface-bodykinematics)
- `transform`: [`ShipTransform`](#interface-shiptransform--bodytransform)
- `prevTickTransform`: [`ShipTransform`](#interface-shiptransform--bodytransform)
- `chunkClaim`: [`ChunkClaim`](#interface-chunkclaim)
- `chunkClaimDimension`: [`DimensionId`](#dimensionidkt)
- `worldAABB`: `AABBdc`
- `shipAABB`: `AABBic?`
- `velocity: Vector3dc` 速度矢量
- `angularVelocity: Vector3dc`  角速度矢量
- `activeChunksSet`: [`IShipActiveChunksSet`](#interface-ishipactivechunksset)
- `shipToWorld: Matrix4dc`
- `worldToShip: Matrix4dc`
- `prevTickShipTransform`: [`ShipTransform`](#interface-shiptransform--bodytransform)
- `omega: Vector3dc`
- `shipTransform`: [`ShipTransform`](#interface-shiptransform--bodytransform)
- `shipVoxelAABB`: `AABBic?`
- `shipActiveChunksSet`: [`IShipActiveChunksSet`](#interface-ishipactivechunksset)


#### `interface ShipPhysicsListener`

<br>

#### `data class Wing`

<br>

#### `data class PositionedWing`

<br>

#### `interface WingManager`

<br>

#### `data class WingGroupChanges`

<br>

#### `data class WingManagerChanges`

<br>

### properties | `org.valkyrienskies.core.api.ships.properties`

#### `interface ChunkClaim`

<br>

#### `interface IShipActiveChunksSet`

<br>

#### `interface ShipTransform` : [`BodyTransform`](#interface-bodytransform)

<br>

#### `ShipIdKt`
这里定义了ShipId
`typealias ShipId = Long`


---

## util `org.valkyrienskies.core.api.util`

#### `interface Identified`
- `Long id` 一个id，不能为-1，除此之外你可以选择你喜欢的任何正负值
>The base class for all Valkyrien Skies things that have an ID.
IDs are unique across dimensions. However, there may be multiple objects with the same ID for thread-safety purposes. For example, there is a server, physics, and client copy of each ship with the same ID.
No object should ever have an ID value of -1, it is reserved as a sentinel value. All other values (including negative ones) are unreserved and can be valid IDs.

>在VS中，所有具有ID的对象的基类
ID在所有不同维度中都是唯一的。然而出于线程安全的考虑，可能会存在多个ID相同的对象。例如，每艘飞船都有服务器、物理和客户端的副本，这三个副本具有相同的ID
任何对象的ID值都不应该是-1，因为这是保留的哨位值。所有其他值（包括负值）都是未保留的，可以是有效的 ID

<br>

#### `data class DoublePair`
一个装有双精度浮点的数对，它的作用是防止使用`Pair<Double, Double>`的时候触发自动装箱（autoboxing）
- `Double first`
- `Double second`
>Represents a pair of two doubles.
The only reason this exists is in order to prevent the autoboxing that occurs when you use Pair<Double, Double>.

<br>

#### `interface AerodynamicUtils`

#### VS作者说不应该碰的东西
对于`annotation class GameTickOnly`以及`annotation class PhysTickOnly`，作者写了个这
>This annotation defines a class, method, or property that is managed by the Physics Thread, and should **NOT** be accessed from the Game Thread directly.

>这个注解（Annotation）定义了一个类/方法/或者属性，这个属性是由物理线程管理的，并且**不应该**被游戏线程直接访问

<br>

### functions | `org.valkyrienskies.core.api.util.functions`

#### `fun interface IntTernaryConsumer`

<br>

#### `fun interface DoubleTernaryConsumer`

<br>

#### `fun interface IntBinaryConsumer`


---

## world | `org.valkyrienskies.core.api.world`

#### `interface ClientShipWorld` : [`ShipWorld`](#interface-shipworld)

<br>

#### `data class LevelYRange`

<br>

#### `interface PhysLevel`
属性：
- `dimension` : [`DimensionId`](#dimensionidkt)   这个PhysLevel对应的游戏维度id
- `aerodynamicUtils` : `AerodynamicUtils`   进行空气动力学计算的工具类

##### `fun getShipById() : PhysShip?`
根据船只的id返回一个`PhysShip`对象，如果找不到对象就返回一个`null`

形参列表：
- `shipId`: [`ShipId`](#shipidkt)  你要找的船只的id

返回值：
详见[`PhysShip`](#interface-physship--ship)

##### `fun physTick()`
物理线程的tick函数，每个tick会触发一次

形参列表：
- `delta: Double`   用途不明

>Tick function, called by the physics thread.
>Do not access variables from the Game Thread in this function, as it will cause `ConcurrentModificationExceptions`. This is because the objects on the Game Thread are not thread-safe, and by accessing them here, you could potentially modify them on both threads at the same time.
>Instead, use thread-safe collections or other thread-safe mechanisms to communicate between the Game Thread and the Physics Thread, such as a `ConcurrentLinkedQueue`.

>**不要**在此函数中访问游戏线程中的变量，否则可能引起`ConcurrentModificationException`异常

##### `fun rayCast() : RayCastResult?`
在物理线程上进行一次射线检测

形参列表：
- `start: Vector3dc`    射线投射的起点（世界坐标系）
- `direction: Vector3dc`    射线投射的方向向量，（x, y, z）格式的**单位向量**
- `length: Double`  射线的最大长度
- `vararg ignoredShips`: [`ShipId`](#shipidkt) 忽略的船只

返回值：
详见[`RayCastResult`](#data-class-raycastresult)

##### `fun enableCollisionBetween() : Boolean`
形参列表：
- `shipId0`: [`ShipId`](#shipidkt)
- `shipId1`: [`ShipId`](#shipidkt)

返回值：
`Boolean`，作用未知

##### `fun disableCollisionBetween() : Boolean`
形参列表：
- `shipId0`: [`ShipId`](#shipidkt)
- `shipId1`: [`ShipId`](#shipidkt)

返回值：
`Boolean`，作用未知
    
<br>

#### `interface ServerShipWorld` : [`ShipWorld`](#interface-shipworld)

<br>

#### `interface ShipWorld`

<br>

### properties | `org.valkyrienskies.core.api.world.properties`

#### `DimensionIdKt`

<br>

### connectivity | `org.valkyrienskies.core.api.world.connectivity`

#### `interface Component`

<br>

#### `enum class ConnectionStatus`

<br>

#### `interface DoubleAugmentation`

<br>

#### `interface DoubleComponentAugmentation` : [`DoubleAugmentation`](#interface-doubleaugmentation)






