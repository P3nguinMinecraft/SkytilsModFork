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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gg.skytils.event.EventsKt;
import gg.skytils.event.impl.entity.BossBarSetEvent;
import net.minecraft.entity.boss.BossBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(targets = "net.minecraft.client.gui.hud.BossBarHud$1")
public class MixinBossStatus {
    @WrapOperation(method = "add(Ljava/util/UUID;Lnet/minecraft/text/Text;FLnet/minecraft/entity/boss/BossBar$Color;Lnet/minecraft/entity/boss/BossBar$Style;ZZZ)V", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    public <K, V> V onSetBossStatus(Map<K, V> instance, K k, V v, Operation<V> original) {
        if (!EventsKt.postCancellableSync(new BossBarSetEvent((BossBar) v, false))) {
            return original.call(instance, k, v);
        }
        return null;
    }
}
