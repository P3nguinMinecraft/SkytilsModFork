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

package gg.skytils.skytilsmod.features.impl.protectitems.strategy.impl

import gg.essential.elementa.unstable.state.v2.*
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.core.PersistentSave
import gg.skytils.skytilsmod.features.impl.dungeons.DungeonFeatures
import gg.skytils.skytilsmod.features.impl.protectitems.strategy.ItemProtectStrategy
import gg.skytils.skytilsmod.utils.ItemUtil
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import java.io.File
import java.io.Reader
import java.io.Writer
import kotlin.jvm.optionals.getOrNull

object FavoriteStrategy : ItemProtectStrategy() {
    private val favoriteUUIDState = mutableSetState<String>()
    private val favoriteItemIdState = mutableSetState<String>()
    val save = FavoriteStrategySave

    override fun worthProtecting(item: ItemStack, extraAttr: NbtCompound?, type: ProtectType): Boolean {
        if (type == ProtectType.HOTBARDROPKEY && DungeonFeatures.hasClearedText) return false
        return favoriteUUIDState.getUntracked().contains(extraAttr?.getString("uuid")?.getOrNull()) ||
                favoriteItemIdState.getUntracked().contains(ItemUtil.getSkyBlockItemID(extraAttr))
    }

    fun clearFavorites() {
        favoriteUUIDState.clear()
        favoriteItemIdState.clear()
    }

    /**
     * Toggles the favorite status of an item.
     * @param item - Item to toggle
     * @param useItemId - Overrides the default behavior of preferring UUID
     * @return The new toggle status of the item (true if present, false if not, null if some other error occurred)
     */
    fun toggleItem(item: ItemStack, useItemId: Boolean = false): ToggleItemResult {
        fun <T> MutableSetState<T>.toggle(element: T): ToggleItemResult = run {
            var status = ToggleItemResult.SUCCESS_REMOVED
            set {
                if (element in it) {
                    it.remove(element)
                } else {
                    status = ToggleItemResult.SUCCESS_ADDED
                    it.add(element)
                }
            }
            return status
        }

        val extraAttributes = ItemUtil.getExtraAttributes(item) ?: return ToggleItemResult.FAILED_NO_EXT_ATTRB
        val status = if (!useItemId && extraAttributes.contains("uuid")) {
            val uuid = extraAttributes.getString("uuid").getOrNull() ?: return ToggleItemResult.FAILED_NO_UUID
            favoriteUUIDState.toggle(uuid)
        } else {
            val itemId = ItemUtil.getSkyBlockItemID(item) ?: return ToggleItemResult.FAILED_NO_ITEM_ID
            favoriteItemIdState.toggle(itemId)
        }
        PersistentSave.markDirty<FavoriteStrategySave>()
        return status
    }

    enum class ToggleItemResult {
        SUCCESS_ADDED,
        SUCCESS_REMOVED,
        FAILED_NO_EXT_ATTRB,
        FAILED_NO_UUID,
        FAILED_NO_ITEM_ID;
    }

    override val isToggled: Boolean = true

    object FavoriteStrategySave : PersistentSave(File(Skytils.modDir, "favoriteitems.json")) {
        override fun read(reader: Reader) {
            val data = json.decodeFromString<JsonElement>(reader.readText())
            if (data is JsonObject) {
                json.decodeFromJsonElement<Schema>(data).also {
                    favoriteUUIDState.addAll(it.favoriteUUIDs)
                    favoriteItemIdState.addAll(it.favoriteItemIds)
                }
            } else if (data is JsonArray) {
                favoriteUUIDState.addAll(json.decodeFromJsonElement<Set<String>>(data))
            }
        }

        override fun write(writer: Writer) {
            writer.write(json.encodeToString(Schema(favoriteUUIDState.getUntracked(), favoriteItemIdState.getUntracked())))
        }

        override fun setDefault(writer: Writer) {
            writer.write(json.encodeToString(Schema()))
        }

        @Serializable
        data class Schema(val favoriteUUIDs: Set<String> = emptySet(), val favoriteItemIds: Set<String> = emptySet())
    }
}