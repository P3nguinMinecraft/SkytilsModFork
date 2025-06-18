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

import gg.skytils.skytilsmod.mixins.hooks.gui.GuiMainMenuHookKt;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class MixinGuiMainMenu extends Screen {
    protected MixinGuiMainMenu(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screen/TitleScreen;splashText:Lnet/minecraft/client/gui/screen/SplashTextRenderer;", shift = At.Shift.AFTER, opcode = Opcodes.PUTFIELD))
    private void setSplashText(CallbackInfo ci) {
        GuiMainMenuHookKt.setSplashText((TitleScreen) (Object) this);
    }
}
