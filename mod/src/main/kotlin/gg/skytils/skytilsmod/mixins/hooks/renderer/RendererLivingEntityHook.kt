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
package gg.skytils.skytilsmod.mixins.hooks.renderer

import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.features.impl.dungeons.LividFinder
import gg.skytils.skytilsmod.features.impl.slayer.SlayerFeatures.slayer
import gg.skytils.skytilsmod.features.impl.slayer.impl.DemonlordSlayer
import gg.skytils.skytilsmod.features.impl.slayer.impl.SeraphSlayer
import gg.skytils.skytilsmod.mixins.extensions.ExtensionEntityLivingBase
import gg.skytils.skytilsmod.mixins.extensions.ExtensionEntityRenderState
import gg.skytils.skytilsmod.utils.Utils
import gg.skytils.skytilsmod.utils.graphics.colors.ColorFactory
import gg.skytils.skytilsmod.utils.withAlpha
import net.minecraft.client.render.entity.state.LivingEntityRenderState
import net.minecraft.entity.Entity
import net.minecraft.entity.mob.EndermanEntity
import net.minecraft.entity.mob.MobEntity
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

fun setColorMultiplier(
    entity: Entity,
    lightBrightness: Float,
    partialTickTime: Float,
    cir: CallbackInfoReturnable<Int>
) {
    if (entity is ExtensionEntityLivingBase && entity.skytilsHook.colorMultiplier != null) cir.returnValue =
        entity.skytilsHook.colorMultiplier?.rgb
    if (Skytils.config.recolorSeraphBoss && Utils.inSkyblock && entity is EndermanEntity) {
        if (slayer?.entity != entity) return
        entity.hurtTime = 0
        (slayer as? SeraphSlayer)?.run {
            if (thrownEntity != null || thrownLocation != null) {
                cir.returnValue = Skytils.config.seraphBeaconPhaseColor.withAlpha(169)
            } else if (hitPhase) {
                cir.returnValue = Skytils.config.seraphHitsPhaseColor.withAlpha(169)
            } else {
                cir.returnValue = Skytils.config.seraphNormalPhaseColor.withAlpha(169)
            }
        }
    } else if (Skytils.config.attunementDisplay && Utils.inSkyblock && entity is MobEntity) {
        (slayer as? DemonlordSlayer)?.let {
            if (entity == it.relevantEntity) {
                entity.hurtTime = 0
                it.relevantColor?.let {
                    // Colors might be too hard to see because of the entities textures and colors,
                    // as opposed to the enderman's almost fully black texture
                    cir.returnValue = it.withAlpha(169)
                }
            }
        }
    } else if (LividFinder.livid == entity) {
        cir.returnValue = ColorFactory.AZURE.withAlpha(169)
    }
}

fun replaceHurtState(
    state: LivingEntityRenderState,
    original: Operation<Boolean>
): Boolean {
    state as ExtensionEntityRenderState
    val entity = state.skytilsEntity
    entity as ExtensionEntityLivingBase
    return if (Skytils.config.changeHurtColorOnWitherKingsDragons && entity.skytilsHook.masterDragonType != null) false else original.call(state)
}
