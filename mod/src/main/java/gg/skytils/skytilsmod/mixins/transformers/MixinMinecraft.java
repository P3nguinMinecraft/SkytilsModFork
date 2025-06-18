/*
 * Skytils - Hypixel Skyblock Quality of Life Mod
 * Copyright (C) 2020-2023 Skytils
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package gg.skytils.skytilsmod.mixins.transformers;

import com.google.common.util.concurrent.ListenableFuture;
import gg.skytils.skytilsmod.Skytils;
import gg.skytils.skytilsmod.utils.ItemUtil;
import gg.skytils.skytilsmod.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;
import java.util.concurrent.Executor;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraft
        //#if MC==10809
        //$$ implements Executor
        //#endif
{
    @Shadow
    public ClientPlayerEntity player;

    //#if MC==10809
    //$$ @Shadow
    //$$ public abstract ListenableFuture<Object> submit(Runnable runnableToSchedule);
    //#endif

    @Inject(method = "doAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;swingHand(Lnet/minecraft/util/Hand;)V", shift = At.Shift.AFTER))
    private void clickMouse(CallbackInfoReturnable<Boolean> cir) {
        if (!Utils.INSTANCE.getInSkyblock()) return;

        ItemStack item = this.player.getMainHandStack();
        if (item != null) {
            NbtCompound extraAttr = ItemUtil.getExtraAttributes(item);
            String itemId = ItemUtil.getSkyBlockItemID(extraAttr);

            if (Objects.equals(itemId, "BLOCK_ZAPPER")) {
                Skytils.sendMessageQueue.add("/undozap");
            }
        }
    }

    //#if MC==10809
    //$$ @Override
    //$$ public void execute(@NotNull Runnable command) {
    //$$    this.submit(command);
    //$$ }
    //#endif
}