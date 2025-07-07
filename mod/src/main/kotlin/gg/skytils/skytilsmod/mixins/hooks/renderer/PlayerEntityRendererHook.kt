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

package gg.skytils.skytilsmod.mixins.hooks.renderer

import gg.skytils.skytilsmod.utils.SuperSecretSettings
import gg.skytils.skytilsmod.utils.Utils
import net.minecraft.client.render.entity.state.PlayerEntityRenderState
import net.minecraft.client.util.math.MatrixStack
import kotlin.random.Random

class PlayerEntityRendererHook {
    fun isBreefing(state: PlayerEntityRenderState): Boolean
        = state.name == "Breefing" && (SuperSecretSettings.breefingDog || Random.nextInt(
            100
        ) < 3)

    fun smol(state: PlayerEntityRenderState, ms: MatrixStack) {
        if (Utils.inSkyblock && (SuperSecretSettings.smolPeople || isBreefing(state))){
            ms.scale(0.5f, 0.5f, 0.5f)
        }
    }
}