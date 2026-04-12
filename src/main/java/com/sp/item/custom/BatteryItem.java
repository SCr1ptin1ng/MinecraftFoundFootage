package com.sp.item.custom;

import com.sp.cca_stuff.InitializeComponents;
import com.sp.cca_stuff.PlayerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class BatteryItem extends Item {
    public BatteryItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient() && user instanceof ServerPlayerEntity serverPlayer) {
            PlayerComponent component = InitializeComponents.PLAYER.get(serverPlayer);
            if (!component.isFlashlightFixed()) {
                component.setFlashlightFixed(true);
                component.sync();
                user.sendMessage(Text.literal("You replace the flashlight battery."), true);

                if (!user.isCreative()) {
                    stack.decrement(1);
                }

                return TypedActionResult.success(stack, world.isClient());
            }
        }

        return TypedActionResult.pass(stack);
    }
}
