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

package gg.skytils.skytilsmod.mixins.transformers.gui;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import gg.essential.universal.UScreen;
import gg.skytils.skytilsmod.gui.OptionsGui;
import gg.skytils.skytilsmod.mixins.hooks.gui.GuiIngameHudHookKt;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.item.ItemStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(InGameHud.class)
public abstract class MixinGuiIngameHud {

    @Shadow private ItemStack currentStack;

    @ModifyExpressionValue(method = "renderHeldItemTooltip", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/hud/InGameHud;heldItemTooltipFade:I", opcode = Opcodes.GETFIELD))
    private int alwaysShowItemHighlight(int original) {
        return GuiIngameHudHookKt.alwaysShowItemHighlight(original);
    }

    @ModifyArgs(method = "renderHeldItemTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithBackground(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIII)I"))
    private void modifyItemHighlightPosition(Args args) {
        GuiIngameHudHookKt.modifyItemHighlightPosition(args, this.currentStack);
    }

    @ModifyArgs(method = "renderOverlayMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V"))
    private void modifyActionBarPosition(Args args) {
        GuiIngameHudHookKt.modifyActionBarPosition(args);
    }

    @ModifyVariable(method = "renderHealthBar", at = @At(value = "HEAD"), ordinal = 6, remap = false, argsOnly = true)
    private int removeAbsorption(int value) {
        return GuiIngameHudHookKt.setAbsorptionAmount(value);
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void renderOverlay(CallbackInfo ci) {
        if (UScreen.getCurrentScreen() instanceof OptionsGui) {
            ci.cancel();
        }
    }
}
