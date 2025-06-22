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

package gg.skytils.skytilsmod.mixins.transformers.renderer;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gg.skytils.skytilsmod.mixins.hooks.renderer.RenderItemHookKt;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.model.BasicItemModel;
import net.minecraft.client.render.item.model.SpecialItemModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = { BasicItemModel.class, SpecialItemModel.class })
public abstract class MixinItemModel {
    @WrapOperation(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/ItemRenderState$LayerRenderState;setGlint(Lnet/minecraft/client/render/item/ItemRenderState$Glint;)V"))
    private void onUpdateGlint(ItemRenderState.LayerRenderState instance, ItemRenderState.Glint glint, Operation<Void> original) {
        if (RenderItemHookKt.getSkipGlint()) {
            original.call(instance, ItemRenderState.Glint.NONE);
            return;
        }

        original.call(instance, glint);
    }
}
