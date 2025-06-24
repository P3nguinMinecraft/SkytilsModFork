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

package gg.skytils.skytilsmod.gui.features

import gg.essential.api.EssentialAPI
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.dsl.basicColorConstraint
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.events.UIClickEvent
import gg.essential.elementa.unstable.layoutdsl.*
import gg.essential.elementa.unstable.state.v2.State
import gg.essential.elementa.unstable.state.v2.mapList
import gg.essential.elementa.unstable.state.v2.toListState
import gg.essential.universal.UChat
import gg.essential.vigilance.utils.onLeftClick
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.features.impl.protectitems.strategy.ItemProtectStrategy
import gg.skytils.skytilsmod.features.impl.protectitems.strategy.impl.FavoriteStrategy
import gg.skytils.skytilsmod.gui.components.SlotComponent
import gg.skytils.skytilsmod.utils.ItemUtil
import gg.skytils.skytilsmod.utils.formattedText
import gg.skytils.skytilsmod.utils.multiplatform.armorItems
import java.awt.Color

class ProtectItemGui : WindowScreen(ElementaVersion.V2, newGuiScale = EssentialAPI.Companion.getGuiUtil().getGuiScale()) {

    val protectedColor = Color(75, 227, 62, 190)
    val unprotectedColor = Color(255, 0, 0, 190)

    val inventoryState = State {
        client?.player?.armorItems ?: emptyList()
    }.toListState()

    val armorState = State {
        client?.player?.armorItems?.reversed() ?: emptyList()
    }.toListState()

    init {
        window.layout {
            row {
                val armorComponent = column {
                    forEach(armorState) { item ->
                        SlotComponent(item)().apply(::setup)
                    }
                }
                column {
                    forEach(inventoryState.mapList { it.chunked(9) }) { row ->
                        row {
                            row.forEach { item ->
                                SlotComponent(item)().apply(::setup)
                            }
                        }
                    }
                }
                spacer(armorComponent, armorComponent)
            }
        }
    }

    private fun setup(slot: SlotComponent) {
        if (slot.item != null) {
            slot.constrain {
                color = basicColorConstraint {
                    if (FavoriteStrategy.worthProtecting(
                            slot.item,
                            ItemUtil.getExtraAttributes(slot.item),
                            ItemProtectStrategy.ProtectType.DROPKEYININVENTORY
                        )
                    )
                        protectedColor
                    else
                        unprotectedColor
                }
            }
        }
        slot.onLeftClick { e ->
            onLeftClickSlot(e, slot)
        }
    }

    fun onLeftClickSlot(event: UIClickEvent, slot: SlotComponent): Boolean {
        val item = slot.item ?: return false
        val extraAttributes = ItemUtil.getExtraAttributes(item) ?: return false
        val isUUID = extraAttributes.contains("uuid")
        when (FavoriteStrategy.toggleItem(item)) {
            FavoriteStrategy.ToggleItemResult.SUCCESS_ADDED -> {
                if (isUUID) {
                    UChat.chat("${Skytils.successPrefix} §aI will now protect your ${item.name.formattedText}§a!")
                } else {
                    val itemId = ItemUtil.getSkyBlockItemID(item)!!
                    UChat.chat("${Skytils.successPrefix} §aI will now protect all of your ${itemId}§as!")
                }
                return true
            }
            FavoriteStrategy.ToggleItemResult.SUCCESS_REMOVED -> {
                if (isUUID) {
                    UChat.chat("${Skytils.successPrefix} §cI will no longer protect your ${item.name.formattedText}§c!")
                } else {
                    val itemId = ItemUtil.getSkyBlockItemID(item)!!
                    UChat.chat("${Skytils.successPrefix} §cI will no longer protect all of your ${itemId}§cs!")
                }
                return true
            }
            else -> return false
        }
    }
}