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

package gg.skytils.skytilsmod.mixins.transformers.cloud;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import org.incendo.cloud.minecraft.modded.internal.ModdedExceptionHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ModdedExceptionHandler.class)
public interface MixinModdedExceptionHandler {
    @WrapOperation(method = "lambda$decorateHoverStacktrace$9", at = @At(value = "NEW", target = "(Lnet/minecraft/text/HoverEvent$Action;Ljava/lang/Object;)Lnet/minecraft/text/HoverEvent;"))
    private static HoverEvent skytils$createHoverEvent(HoverEvent.Action action, Object text, Operation<HoverEvent> original) {
        return new HoverEvent.ShowText((Text) text);
    }

    @WrapOperation(method = "lambda$decorateHoverStacktrace$9", at = @At(value = "NEW", target = "(Lnet/minecraft/text/ClickEvent$Action;Ljava/lang/String;)Lnet/minecraft/text/ClickEvent;"))
    private static ClickEvent skytils$createClickEvent(ClickEvent.Action action, String string, Operation<ClickEvent> original) {
        return new ClickEvent.CopyToClipboard(string);
    }
}
