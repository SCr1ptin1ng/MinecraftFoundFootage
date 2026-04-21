package com.sp.item.custom;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class SpyderDebugItem extends Item {
    private static final String MODE_KEY = "SpyderMode";
    private static final String TARGET_X_KEY = "SpyderTargetX";
    private static final String TARGET_Y_KEY = "SpyderTargetY";
    private static final String TARGET_Z_KEY = "SpyderTargetZ";
    private static final String NORMAL_X_KEY = "SpyderNormalX";
    private static final String NORMAL_Y_KEY = "SpyderNormalY";
    private static final String NORMAL_Z_KEY = "SpyderNormalZ";
    private static final String HAS_TARGET_KEY = "SpyderHasTarget";

    public enum Mode {
        FOLLOW_CURSOR,
        WALK_TO_POINT
    }

    public SpyderDebugItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient() && user instanceof ServerPlayerEntity serverPlayer) {
            Mode next = getMode(stack) == Mode.FOLLOW_CURSOR ? Mode.WALK_TO_POINT : Mode.FOLLOW_CURSOR;
            setMode(stack, next);
            serverPlayer.sendMessage(Text.literal(next == Mode.FOLLOW_CURSOR
                    ? "Spyder debug: following cursor"
                    : "Spyder debug: point-walk mode"), true);
        }
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        ItemStack stack = context.getStack();
        if (getMode(stack) != Mode.WALK_TO_POINT) {
            return ActionResult.PASS;
        }

        World world = context.getWorld();
        Direction side = context.getSide();
        Vec3d hitPos = context.getHitPos().add(side.getOffsetX() * 0.28, side.getOffsetY() * 0.28, side.getOffsetZ() * 0.28);
        Vec3d normal = new Vec3d(side.getOffsetX(), side.getOffsetY(), side.getOffsetZ()).normalize();

        if (!world.isClient() && context.getPlayer() instanceof ServerPlayerEntity serverPlayer) {
            setTarget(stack, hitPos, normal);
            serverPlayer.sendMessage(Text.literal(String.format("Spyder target set: %.2f %.2f %.2f", hitPos.x, hitPos.y, hitPos.z)), true);
        }

        return ActionResult.success(world.isClient());
    }

    public static Mode getMode(ItemStack stack) {
        String raw = stack.getOrCreateNbt().getString(MODE_KEY);
        if (raw == null || raw.isEmpty()) {
            return Mode.FOLLOW_CURSOR;
        }
        try {
            return Mode.valueOf(raw);
        } catch (IllegalArgumentException ignored) {
            return Mode.FOLLOW_CURSOR;
        }
    }

    public static void setMode(ItemStack stack, Mode mode) {
        stack.getOrCreateNbt().putString(MODE_KEY, mode.name());
    }

    public static boolean hasTarget(ItemStack stack) {
        return stack.hasNbt() && stack.getOrCreateNbt().getBoolean(HAS_TARGET_KEY);
    }

    public static Vec3d getTarget(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        return new Vec3d(nbt.getDouble(TARGET_X_KEY), nbt.getDouble(TARGET_Y_KEY), nbt.getDouble(TARGET_Z_KEY));
    }

    public static Vec3d getTargetNormal(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        Vec3d normal = new Vec3d(nbt.getDouble(NORMAL_X_KEY), nbt.getDouble(NORMAL_Y_KEY), nbt.getDouble(NORMAL_Z_KEY));
        return normal.lengthSquared() < 0.0001 ? new Vec3d(0.0, 1.0, 0.0) : normal.normalize();
    }

    public static void clearTarget(ItemStack stack) {
        stack.getOrCreateNbt().putBoolean(HAS_TARGET_KEY, false);
    }

    private static void setTarget(ItemStack stack, Vec3d target, Vec3d normal) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putDouble(TARGET_X_KEY, target.x);
        nbt.putDouble(TARGET_Y_KEY, target.y);
        nbt.putDouble(TARGET_Z_KEY, target.z);
        nbt.putDouble(NORMAL_X_KEY, normal.x);
        nbt.putDouble(NORMAL_Y_KEY, normal.y);
        nbt.putDouble(NORMAL_Z_KEY, normal.z);
        nbt.putBoolean(HAS_TARGET_KEY, true);
    }
}
