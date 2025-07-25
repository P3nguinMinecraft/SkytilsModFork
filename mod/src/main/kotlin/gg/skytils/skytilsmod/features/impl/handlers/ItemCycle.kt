/*
 * Skytils - Hypixel Skyblock Quality of Life Mod
 * Copyright (C) 2020-2024 Skytils
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

import gg.skytils.event.EventPriority
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.TickEvent
import gg.skytils.event.impl.screen.GuiContainerSlotClickEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.core.PersistentSave
import gg.skytils.skytilsmod.utils.ItemUtil
import gg.skytils.skytilsmod.utils.SBInfo
import gg.skytils.skytilsmod.utils.SkyblockIsland
import gg.skytils.skytilsmod.utils.Utils
import gg.skytils.skytilsmod.utils.multiplatform.SlotActionType
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import net.minecraft.item.ItemStack
import java.io.File
import java.io.Reader
import java.io.Writer
import java.util.*
import kotlin.jvm.optionals.getOrNull

object ItemCycle : PersistentSave(File(Skytils.modDir, "itemcycle.json")), EventSubscriber {

    val cycles = hashMapOf<UUID, Cycle>()
    private val itemLocations = hashMapOf<Cycle.ItemIdentifier, Int>()

    override fun read(reader: Reader) {
        cycles.clear()
        cycles.putAll(json.decodeFromString<Map<@Contextual UUID, Cycle>>(reader.readText()))
    }

    override fun write(writer: Writer) {
        writer.write(json.encodeToString<Map<@Contextual UUID, Cycle>>(cycles))
    }

    override fun setDefault(writer: Writer) {
        writer.write(json.encodeToString(emptyMap<@Contextual UUID, Cycle>()))
    }

    override fun setup() {
        register(::onTick)
        register(::onSlotClick, EventPriority.Lowest)
    }

    fun onTick(event: TickEvent) {
        if (cycles.isEmpty() || !Utils.inSkyblock || mc.player == null) return

        itemLocations.clear()
        val player = mc.player ?: return
        for (slot in player.playerScreenHandler.slots) {
            val item = slot.stack?.getIdentifier() ?: continue

            itemLocations[item] = slot.id
        }
    }

    fun onSlotClick(event: GuiContainerSlotClickEvent) {
        if (!Utils.inSkyblock || cycles.isEmpty() || event.clickType == 2 || event.container != mc.player?.playerScreenHandler) return

        if (event.slotId !in 36..44) return

        val clickedItem = event.slot?.stack?.getIdentifier()

        val cycle = cycles.values.find { cycle ->
            cycle.conditions.all { cond -> cond.check(event, clickedItem) }
        } ?: return

        val swapTo = itemLocations[cycle.swapTo] ?: return

        mc.interactionManager?.clickSlot(event.container.syncId, swapTo, event.slotId - 36, SlotActionType.SWAP, mc.player)

        event.cancelled = true
    }

    fun ItemStack?.getIdentifier() = ItemUtil.getExtraAttributes(this)?.let { extraAttributes ->
        if (extraAttributes.contains("uuid")) {
            extraAttributes.getString("uuid").getOrNull()?.let { uuid -> Cycle.ItemIdentifier(uuid, Cycle.ItemIdentifier.Type.SKYBLOCK_UUID) }
        } else {
            val sbId = ItemUtil.getSkyBlockItemID(extraAttributes)
            when {
                sbId != null -> {
                    Cycle.ItemIdentifier(sbId, Cycle.ItemIdentifier.Type.SKYBLOCK_ID)
                }

                this != null -> {
                    Cycle.ItemIdentifier(this.item.translationKey, Cycle.ItemIdentifier.Type.VANILLA_ID)
                }

                else -> null
            }
        }
    }

    @Serializable
    data class Cycle(
        val uuid: @Contextual UUID, var name: String, val conditions: MutableSet<Condition>, var swapTo: ItemIdentifier
    ) {
        @Serializable
        data class ItemIdentifier(
            val id: String, val type: Type
        ) {
            enum class Type {
                SKYBLOCK_ID, SKYBLOCK_UUID, VANILLA_ID
            }
        }

        @Serializable
        sealed class Condition(val uuid: @Contextual UUID = UUID.randomUUID()) {
            abstract fun check(event: GuiContainerSlotClickEvent, clickedItem: ItemIdentifier?): Boolean

            abstract fun displayText(): String

            @Serializable
            @SerialName("IslandCondition")
            class IslandCondition(var islands: Set<@Serializable(with = SkyblockIsland.ModeSerializer::class) SkyblockIsland>, var negated: Boolean = false) : Condition() {
                override fun check(event: GuiContainerSlotClickEvent, clickedItem: ItemIdentifier?): Boolean =
                    islands.any { SBInfo.mode == it.mode } == !negated

                override fun displayText(): String = "${if (negated) "Not " else ""}${islands.joinToString(", ")}"
            }

            @Serializable
            @SerialName("ClickCondition")
            class ClickCondition(var clickedButton: Int, var clickType: Int, var negated: Boolean = false) :
                Condition() {
                override fun check(event: GuiContainerSlotClickEvent, clickedItem: ItemIdentifier?): Boolean =
                    ((clickedButton == -1000 || event.clickedButton == clickedButton) && (clickType == -1000 || event.clickType == clickType)) == !negated

                override fun displayText(): String =
                    "${if (negated) "Not " else ""} button $clickedButton, type $clickType"
            }

            @Serializable
            @SerialName("ItemCondition")
            class ItemCondition(var item: ItemIdentifier, var negated: Boolean = false) : Condition() {
                override fun check(event: GuiContainerSlotClickEvent, clickedItem: ItemIdentifier?): Boolean {
                    return (clickedItem == item) == !negated
                }

                override fun displayText(): String = "${if (negated) "Not " else ""}${item.type}: ${item.id}"
            }

            @Serializable
            @SerialName("SlotCondition")
            class SlotCondition(var slotId: Int, var negated: Boolean = false) :
                Condition() {
                override fun check(event: GuiContainerSlotClickEvent, clickedItem: ItemIdentifier?): Boolean =
                    event.slotId == slotId == !negated

                override fun displayText(): String =
                    "${if (negated) "Not " else ""} slot $slotId"
            }
        }
    }
}