/*
 * Skytils - Hypixel Skyblock Quality of Life Mod
 * Copyright (C) 2020-2024 Skytils
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

package gg.skytils.event.mixins.entity;

import gg.skytils.event.EventsKt;
import gg.skytils.event.impl.entity.BossBarSetEvent;
import net.minecraft.class_0_1003;
import net.minecraft.entity.boss.BossEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_0_1003.class)
public class MixinBossStatus {
    @Inject(method = "method_0_3293", at = @At("HEAD"), cancellable = true)
    private static void onSetBossStatus(BossEntity displayData, boolean hasColorModifierIn, CallbackInfo ci) {
        BossBarSetEvent event = new BossBarSetEvent(displayData, hasColorModifierIn);
        if (EventsKt.postCancellableSync(event)) {
            ci.cancel();
        }
    }
}
