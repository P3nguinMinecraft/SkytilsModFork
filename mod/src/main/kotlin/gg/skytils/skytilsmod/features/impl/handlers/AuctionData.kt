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
package gg.skytils.skytilsmod.features.impl.handlers

import gg.essential.universal.UChat
import gg.skytils.event.EventSubscriber
import gg.skytils.hypixel.types.skyblock.Pet
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.client
import gg.skytils.skytilsmod.Skytils.json
import gg.skytils.skytilsmod.core.Config
import gg.skytils.skytilsmod.utils.ItemUtil
import gg.skytils.skytilsmod.utils.toStringIfTrue
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonPrimitive
import net.minecraft.item.ItemStack
import kotlin.concurrent.fixedRateTimer
import kotlin.jvm.optionals.getOrDefault
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.jvm.javaField

object AuctionData : EventSubscriber {

    private val dataURL
        get() = "https://${Skytils.domain}/api/auctions/lowestbins"
    val lowestBINs = HashMap<String, Double>()

    override fun setup() {}

    fun getIdentifier(item: ItemStack?): String? {
        val extraAttr = ItemUtil.getExtraAttributes(item) ?: return null
        var id = ItemUtil.getSkyBlockItemID(extraAttr) ?: return null
        when (id) {
            "PET" -> if (extraAttr.getString("petInfo").getOrDefault("").startsWith("{")) {
                val petInfo = json.decodeFromString<Pet>(extraAttr.getString("petInfo").getOrDefault("{\"type\":\"null\",\"tier\":\"null\"}"))
                id = "PET-${petInfo.type}-${petInfo.tier}"
            }

            "ATTRIBUTE_SHARD" -> if (extraAttr.contains("attributes")) {
                val attributes = extraAttr.getCompound("attributes").getOrNull() ?: return null
                val attribute = attributes.keys.firstOrNull()
                if (attribute != null) {
                    id = "ATTRIBUTE_SHARD-${attribute.uppercase()}-${attributes.getInt(attribute)}"
                }
            }

            "ENCHANTED_BOOK" -> if (extraAttr.contains("enchantments")) {
                val enchants = extraAttr.getCompound("enchantments").getOrNull() ?: return null
                val enchant = enchants.keys.firstOrNull()
                if (enchant != null) {
                    id = "ENCHANTED_BOOK-${enchant.uppercase()}-${enchants.getInt(enchant)}"
                }
            }

            "POTION" -> if (extraAttr.contains("potion") && extraAttr.contains("potion_level")) {
                id = "POTION-${
                    extraAttr.getString("potion").getOrDefault("")
                        .uppercase()
                }-${extraAttr.getInt("potion_level")}${"-ENHANCED".toStringIfTrue(extraAttr.contains("enhanced"))}${
                    "-EXTENDED".toStringIfTrue(
                        extraAttr.contains(
                            "extended"
                        )
                    )
                }${"-SPLASH".toStringIfTrue(extraAttr.contains("splash"))}"
            }

            "RUNE", "UNIQUE_RUNE" -> if (extraAttr.contains("runes")) {
                val runes = extraAttr.getCompound("runes").getOrNull() ?: return null
                val rune = runes.keys.firstOrNull()
                if (rune != null) {
                    id = "RUNE-${rune.uppercase()}-${runes.getInt(rune)}"
                }
            }
        }
        return id
    }

    init {
        Skytils.config.registerListener(Config::fetchLowestBINPrices.javaField!!) { value: Boolean ->
            if (!value) lowestBINs.clear()
        }
        fixedRateTimer(name = "Skytils-FetchAuctionData", period = 60 * 1000L) {
            if (Skytils.config.fetchLowestBINPrices) {
                Skytils.IO.launch {
                    client.get(dataURL).let {
                        if (it.headers["cf-cache-status"] == "STALE") {
                            UChat.chat("${Skytils.failPrefix} Uh oh! Auction data is stale, prices may be inaccurate.")
                        }
                        it.body<JsonObject>().forEach { itemId, price ->
                            lowestBINs[itemId] = price.jsonPrimitive.double
                        }
                    }
                }
            }
        }
    }

}