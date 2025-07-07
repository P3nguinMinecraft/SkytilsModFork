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
package gg.skytils.skytilsmod.mixins.hooks.entity

import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.features.impl.dungeons.WitherKingDragons
import gg.skytils.skytilsmod.utils.SuperSecretSettings
import gg.skytils.skytilsmod.utils.Utils
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.particle.ParticleType
import net.minecraft.potion.Potions
import net.minecraft.world.World
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.awt.Color
import kotlin.random.Random

//#if MC>12000
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.particle.ParticleTypes

//#endif

class EntityLivingBaseHook(val entity: LivingEntity) {

    var colorMultiplier: Color? = null
    var masterDragonType: WitherKingDragons? = null

    //#if MC==10809
    //$$  val isBreefing by lazy {
    //$$      entity.name.string == "Breefing" && (SuperSecretSettings.breefingDog || Random.nextInt(
    //$$          100
    //$$      ) < 3)
    //$$  }
    //$$
    //$$  val isSmol by lazy {
    //$$      Utils.inSkyblock && entity is PlayerEntity && (SuperSecretSettings.smolPeople || isBreefing)
    //$$  }
    //#endif

    //#if MC>12000
    fun modifyPotionActive(statusEffect: StatusEffect, cir: CallbackInfoReturnable<Boolean>) {
        if (!Utils.inSkyblock) return
        if (Skytils.config.disableNightVision && statusEffect == StatusEffects.NIGHT_VISION && entity is ClientPlayerEntity) {
            cir.returnValue = false
        }
    }
    //#else
    //$$ fun modifyPotionActive(potionId: Int, cir: CallbackInfoReturnable<Boolean>) {
    //$$     if (!Utils.inSkyblock) return
    //$$     if (Skytils.config.disableNightVision && potionId == Potions.NIGHT_VISION && entity is ClientPlayerEntity) {
    //$$         cir.returnValue = false
    //$$     }
    //$$ }
    //#endif

    //#if MC>12000
    fun shouldRemove() = !Skytils.config.hideDeathParticles || !Utils.inSkyblock
    //#else
    //$$ fun removeDeathParticle(
    //$$     particleType: ParticleType<*>,
    //$$ ): Boolean {
    //$$     return !Skytils.config.hideDeathParticles || !Utils.inSkyblock || particleType != ParticleTypes.EXPLOSION
    //$$ }
    //#endif

    //#if MC==10809
    //$$fun isChild(cir: CallbackInfoReturnable<Boolean>) {
    //$$    cir.returnValue = isSmol
    //$$}
    //#endif
}