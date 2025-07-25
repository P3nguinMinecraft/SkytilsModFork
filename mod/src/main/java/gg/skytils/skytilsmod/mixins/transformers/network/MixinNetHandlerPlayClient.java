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

package gg.skytils.skytilsmod.mixins.transformers.network;

import com.llamalad7.mixinextras.sugar.Local;
import gg.skytils.skytilsmod.features.impl.dungeons.MasterMode7Features;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.listener.ClientPlayPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC==10809
//$$ import net.minecraft.network.packet.s2c.play.MobSpawnS2CPacket;
//#else
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
//#endif

@Mixin(value = ClientPlayNetworkHandler.class, priority = 1001)
public abstract class MixinNetHandlerPlayClient implements ClientPlayPacketListener {
    //#if MC==10809
    //$$ @Inject(method = "onMobSpawn", at = @At("TAIL"))
    //#else
    @Inject(method = "onEntitySpawn", at = @At("TAIL"))
    //#endif
    private void onHandleSpawnMobTail(
            //#if MC==10809
            //$$ MobSpawnS2CPacket packet,
            //#else
            EntitySpawnS2CPacket packet,
            //#endif
            CallbackInfo ci, @Local Entity entity) {
        if (entity != null) {
            MasterMode7Features.INSTANCE.onMobSpawned(entity);
        }
    }
}
