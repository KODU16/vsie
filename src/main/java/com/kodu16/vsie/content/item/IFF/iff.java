package com.kodu16.vsie.content.item.IFF;

import com.kodu16.vsie.content.controlseat.AbstractControlSeatBlockEntity;
import com.kodu16.vsie.utility.ItemStackNbt;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public class iff extends Item {

    private static final String KEY_ENEMY = "enemy";
    private static final String KEY_ALLY   = "ally";

    public iff(Properties pProperties) {
        super(pProperties);
    }

    // ───────────────────────────────────────────────
    //           常用读取方法（推荐都加上）
    // ───────────────────────────────────────────────

    /** 获取敌方队伍名，没设置返回空字符串（永不返回 null） */
    public static String getEnemy(ItemStack stack) {
        CompoundTag tag = ItemStackNbt.get(stack);
        return tag == null ? "" : tag.getString(KEY_ENEMY);
    }

    /** 获取友方队伍名，没设置返回空字符串（永不返回 null） */
    public static String getAlly(ItemStack stack) {
        CompoundTag tag = ItemStackNbt.get(stack);
        return tag == null ? "" : tag.getString(KEY_ALLY);
    }

    /** 判断是否有设置过敌方队伍（非空字符串） */
    public static boolean hasEnemy(ItemStack stack) {
        return !getEnemy(stack).isEmpty();
    }

    /** 判断是否有设置过友方队伍（非空字符串） */
    public static boolean hasAlly(ItemStack stack) {
        return !getAlly(stack).isEmpty();
    }

    /** 可选：给物品栏 tooltip 显示用 */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        String enemy = getEnemy(stack);
        String ally   = getAlly(stack);

        if (!enemy.isEmpty()) {
            tooltip.add(Component.literal("§c敌方: " + enemy));
        }
        if (!ally.isEmpty()) {
            tooltip.add(Component.literal("§a友方: " + ally));
        }
        if (enemy.isEmpty() && ally.isEmpty()) {
            tooltip.add(Component.literal("§7未设置 IFF"));
        }
    }

    // 右键空气 / 右键非方块 / 没点中任何东西 → 打开界面
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }

        ItemStack stack = player.getItemInHand(hand);

        // 服务端才处理菜单
        if (!level.isClientSide) {
            // 这里不再无条件打开，而是可以加更多条件（目前无条件打开）
            player.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("container.vsie.iff");
                }

                @Override
                public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player) {
                    return new IFFContainerMenu(windowId, inv, player.getMainHandItem());
                }
            });
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    // 右键方块时的行为
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.PASS;
        }

        ServerPlayer player = (ServerPlayer) context.getPlayer();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        BlockEntity be = level.getBlockEntity(pos);

        if (be instanceof AbstractControlSeatBlockEntity controlSeat) {
            CompoundTag tag = ItemStackNbt.get(stack);

            boolean hasChange = false;

            if (tag != null && tag.contains(KEY_ENEMY)) {
                String enemy = tag.getString(KEY_ENEMY);
                controlSeat.setEnemy(enemy);
                hasChange = true;
            }
            if (tag != null && tag.contains(KEY_ALLY)) {
                String ally = tag.getString(KEY_ALLY);
                controlSeat.setAlly(ally);
                hasChange = true;
            }

            if (hasChange) {
                player.displayClientMessage(
                        Component.literal("已设置 IFF → 敌方: " + getEnemy(stack) + "  友方: " + getAlly(stack)),
                        true
                );
                return InteractionResult.CONSUME;   // 消耗动作（不继续执行 use）
            } else {
                player.displayClientMessage(
                        Component.literal("物品上没有设置任何 IFF 信息"),
                        true
                );
                return InteractionResult.CONSUME;
            }
        }

        // 不是控制椅
        player.displayClientMessage(
                Component.literal("目标方块不是可设置 IFF 的控制席位"),
                true
        );

        // 重要：这里返回 CONSUME 或 SUCCESS，让 use() 不被触发
        return InteractionResult.CONSUME;
    }


}
