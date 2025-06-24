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
package gg.skytils.skytilsmod.commands.impl

import gg.essential.universal.UChat
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.Skytils.successPrefix
import gg.skytils.skytilsmod.core.PersistentSave
import gg.skytils.skytilsmod.features.impl.protectitems.strategy.impl.FavoriteStrategy
import gg.skytils.skytilsmod.gui.features.ProtectItemGui
import gg.skytils.skytilsmod.utils.Utils
import gg.skytils.skytilsmod.utils.formattedText
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Commands
import org.incendo.cloud.annotations.Flag

@Commands
object ProtectItemCommand {
    @Command("protectitem clearall")
    fun clearAll() {
        FavoriteStrategy.clearFavorites()
        PersistentSave.markDirty<FavoriteStrategy.FavoriteStrategySave>()
        UChat.chat("$successPrefix §aCleared all your protected items!")
    }

    @Command("protectitem gui")
    fun openGui() {
        if (!Utils.inSkyblock) throw IllegalArgumentException("You must be in Skyblock to use this command!")
        Skytils.displayScreen = ProtectItemGui()
    }

    @Command("protectitem")
    fun toggleFavorite(
        @Flag("itemId", description = "Use the item ID instead of the UUID")
        useItemId: Boolean = false
    ) {
        if (!Utils.inSkyblock) throw IllegalArgumentException("You must be in Skyblock to use this command!")
        val item = mc.player?.mainHandStack
            ?: throw IllegalArgumentException("You must hold an item to use this command")
        when (FavoriteStrategy.toggleItem(item)) {
            FavoriteStrategy.ToggleItemResult.SUCCESS_ADDED -> UChat.chat("$successPrefix §aI will now protect your ${item.name.formattedText}§a!")
            FavoriteStrategy.ToggleItemResult.SUCCESS_REMOVED -> UChat.chat("$successPrefix §cI will no longer protect your ${item.name.formattedText}§c!")
            FavoriteStrategy.ToggleItemResult.FAILED_NO_UUID -> throw IllegalArgumentException("Unable to get Item UUID: ${item.name.formattedText}")
            FavoriteStrategy.ToggleItemResult.FAILED_NO_ITEM_ID -> throw IllegalArgumentException("This item doesn't have a Skyblock ID.")
            FavoriteStrategy.ToggleItemResult.FAILED_NO_EXT_ATTRB -> throw IllegalArgumentException("This isn't a Skyblock Item? Where'd you get it from cheater...")
        }
    }
}