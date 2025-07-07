/*
 * Skytils - Hypixel Skyblock Quality of Life Mod
 * Copyright (C) 2020-2025 Skytils
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

package gg.skytils.skytilsmod.mixins.transformers.renderer.sodium;

import gg.skytils.skytilsmod.mixins.hooks.renderer.BlockRendererDispatcherHookKt;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.world.LevelSlice")
public class MixinLevelSlice {
    @Inject(method = "getBlockState(III)Lnet/minecraft/block/BlockState;", at = @At("RETURN"), cancellable = true)
    private void modifyBlockState(int blockX, int blockY, int blockZ, CallbackInfoReturnable<BlockState> cir){
        BlockState state = BlockRendererDispatcherHookKt.modifyBlockState(blockX, blockY, blockZ);
        if (!cir.getReturnValue().equals(state)){
         cir.setReturnValue(state);
        }
    }
}
