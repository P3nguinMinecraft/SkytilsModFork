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
package gg.skytils.skytilsmod.mixins.hooks.item

import gg.skytils.skytilsmod.features.impl.handlers.ArmorColor
import gg.skytils.skytilsmod.utils.ItemUtil.getExtraAttributes
import gg.skytils.skytilsmod.utils.Utils
import net.minecraft.item.ItemStack
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

fun getArmorColorOverride(stack: ItemStack): Int? {
    if (!Utils.inSkyblock) return null
    val extraAttributes = getExtraAttributes(stack)
    if (extraAttributes != null && extraAttributes.contains("uuid")) {
        val uuid = extraAttributes.getString("uuid", null)
        if (ArmorColor.armorColors.containsKey(uuid)) {
            return ArmorColor.armorColors[uuid]!!.toInt()
        }
    }
    return null
}

fun replaceArmorColor(stack: ItemStack, cir: CallbackInfoReturnable<Int>) {
    val color = getArmorColorOverride(stack)
    if (color != null) {
        cir.returnValue = color
    }
}

fun hasCustomArmorColor(stack: ItemStack, cir: CallbackInfoReturnable<Boolean>) {
    val color = getArmorColorOverride(stack)
    if (color != null) {
        cir.returnValue = true
    }
}
