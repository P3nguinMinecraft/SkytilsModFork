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

package gg.skytils.skytilsmod.mixins.transformers.crash;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import gg.skytils.skytilsmod.mixins.hooks.crash.CrashReportHook;
import net.minecraft.util.SystemDetails;
import net.minecraft.util.crash.CrashReport;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CrashReport.class, priority = 988)
public abstract class MixinCrashReport {

    @Shadow
    @Final
    private SystemDetails systemDetailsSection;

    @Unique
    private final CrashReportHook hook = new CrashReportHook((CrashReport) (Object) this);

    @Definition(id = "stringBuilder", local = @Local(ordinal = 0), type = StringBuilder.class)
    @Definition(id = "append", method = "Ljava/lang/StringBuilder;append(Ljava/lang/String;)Ljava/lang/StringBuilder;")
    @Expression("stringBuilder.append('Time: ')")
    @Inject(method = "asString(Lnet/minecraft/util/crash/ReportType;Ljava/util/List;)Ljava/lang/String;", at = @At("MIXINEXTRAS:EXPRESSION"))
    private void injectInfoIntoReport(CallbackInfoReturnable<String> cir, @Local(ordinal = 0) StringBuilder stringbuilder) {
        hook.checkSkytilsCrash(cir, stringbuilder);
    }

    @ModifyExpressionValue(method = "asString(Lnet/minecraft/util/crash/ReportType;Ljava/util/List;)Ljava/lang/String;", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/crash/CrashReport;getCauseAsString()Ljava/lang/String;"))
    private String otherReplaceCauseForLauncher(String theCauseStackTraceOrString) {
        return hook.generateCauseForLauncher(theCauseStackTraceOrString);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void addDataToCrashReport(CallbackInfo ci) {
        hook.addDataToCrashReport(this.systemDetailsSection);
    }
}
