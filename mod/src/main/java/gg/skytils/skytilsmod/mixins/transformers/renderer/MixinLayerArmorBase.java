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

package gg.skytils.skytilsmod.mixins.transformers.renderer;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import gg.skytils.skytilsmod.Skytils;
import gg.skytils.skytilsmod.core.Config;
import gg.skytils.skytilsmod.mixins.extensions.ExtensionEntityRenderState;
import gg.skytils.skytilsmod.mixins.hooks.renderer.LayerArmorBaseHookKt;
import gg.skytils.skytilsmod.utils.Utils;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModel;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ArmorFeatureRenderer.class)
public abstract class MixinLayerArmorBase<T extends EntityModel> {

    @Unique
    private boolean modifiedAlpha = false;

    @WrapMethod(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V")
    private void onRenderAllArmor(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, BipedEntityRenderState bipedEntityRenderState, float f, float g, Operation<Void> original) {
        if (Config.INSTANCE.getTransparentArmorLayer() != 0 || !Utils.INSTANCE.getInSkyblock() || ((ExtensionEntityRenderState) bipedEntityRenderState).getSkytilsEntity() != Skytils.getMc().player) {
            original.call(matrixStack, vertexConsumerProvider, i, bipedEntityRenderState, f, g);
        }
    }
/*
    @Inject(method = "renderArmor", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ArmorItem;method_0_8149(Lnet/minecraft/item/ItemStack;)I"))
    private void setAlpha(LivingEntity entitylivingbaseIn, float p_177182_2_, float p_177182_3_, float partialTicks, float p_177182_5_, float p_177182_6_, float p_177182_7_, float scale, int armorSlot, CallbackInfo ci) {
        if (Utils.INSTANCE.getInSkyblock() && entitylivingbaseIn == Skytils.getMc().player) {
            modifiedAlpha = true;
            this.alpha = Config.INSTANCE.getTransparentArmorLayer();
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(770, 771, 1, 0);
        }
    }

    @Dynamic
    @Inject(method = "renderArmor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;setAngles(Lnet/minecraft/entity/Entity;FFFFFF)V", shift = At.Shift.AFTER, ordinal = 1))
    private void resetAlpha(LivingEntity entitylivingbaseIn, float p_177182_2_, float p_177182_3_, float partialTicks, float p_177182_5_, float p_177182_6_, float p_177182_7_, float scale, int armorSlot, CallbackInfo ci) {
        if (modifiedAlpha) {
            this.alpha = 1f;
            modifiedAlpha = false;
        }
    }

    @Inject(method = "renderArmor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/feature/ArmorFeatureRenderer;method_4171(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/model/EntityModel;FFFFFFF)V"), cancellable = true)
    private void replaceArmorGlint(LivingEntity entitylivingbaseIn, float p_177182_2_, float p_177182_3_, float partialTicks, float p_177182_5_, float p_177182_6_, float p_177182_7_, float scale, int armorSlot, CallbackInfo ci) {
        LayerArmorBaseHookKt.replaceArmorGlint(this, this.field_4828, entitylivingbaseIn, p_177182_2_, p_177182_3_, partialTicks, p_177182_5_, p_177182_6_, p_177182_7_, scale, armorSlot, ci);
    }*/
}
