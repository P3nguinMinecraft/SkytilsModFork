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

import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.features.impl.handlers.GlintCustomizer
import gg.skytils.skytilsmod.utils.ItemUtil.getExtraAttributes
import gg.skytils.skytilsmod.utils.ItemUtil.getSkyBlockItemID
import gg.skytils.skytilsmod.utils.Utils
import gg.skytils.skytilsmod.utils.countMatches
import gg.skytils.skytilsmod.utils.formattedText
import gg.skytils.skytilsmod.utils.ifNull
import net.minecraft.component.DataComponentTypes
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

const val star = "✪"
val masterStars = ('➊'..'➎').map { it.toString() }
val starRegex = Regex("§6${star}+")
val masterStarRegex = Regex("§c(?<tier>[${masterStars.joinToString("")}])")

fun showEnchantmentGlint(stack: Any, cir: CallbackInfoReturnable<Boolean>) {
    (stack as ItemStack).apply {
        if (!Utils.inSkyblock) return
        val extraAttr = getExtraAttributes(this)
        if (extraAttr != null) {
            val itemId = getSkyBlockItemID(extraAttr)
            GlintCustomizer.glintItems[itemId]?.override?.let {
                cir.returnValue = it
                return
            }
            if (Skytils.config.enchantGlintFix) {
                if (extraAttr.contains("enchantments") && extraAttr.getCompoundOrEmpty("enchantments").keys.isNotEmpty()) {
                    cir.returnValue = true
                    return
                }
            }
        }
        val customData = get(DataComponentTypes.CUSTOM_DATA)
        if (customData?.contains("SkytilsForceGlint") == true) {
            cir.returnValue = customData.nbt.getBoolean("SkytilsForceGlint").get()
        }
    }
}

fun modifyDisplayName(displayName: Text): Text {
    if (!Utils.inSkyblock || Skytils.config.starDisplayType == 0 || !displayName.string.contains(star)) return displayName

    try {
        val formattedText = displayName.formattedText
        when (Skytils.config.starDisplayType) {
            1 -> {
                masterStarRegex.find(formattedText)?.destructured?.let { (tier) ->
                    val count = masterStars.indexOf(tier) + 1

                    return Text.literal(formattedText.replace(masterStarRegex, "")
                        .replaceFirst("§6" + star.repeat(count), "§c" + star.repeat(count) + "§6"))
                }
            }

            2 -> {
                masterStarRegex.find(formattedText)?.destructured?.let { (tier) ->
                    val count = masterStars.indexOf(tier) + 1 + 5
                    return Text.literal(formattedText.replace(starRegex, "").replace(masterStarRegex, "") + "§c$count$star")
                } ?: return Text.literal(formattedText.replace(starRegex, "") + "§6${formattedText.countMatches(star)}$star")
            }
        }
    } catch (ignored: Exception) { }

    return displayName
}