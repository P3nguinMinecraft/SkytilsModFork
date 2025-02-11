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
package gg.skytils.skytilsmod.features.impl.dungeons.solvers.terminals

import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.Companion.mc
import gg.skytils.skytilsmod.events.impl.GuiContainerEvent
import gg.skytils.skytilsmod.events.impl.MainReceivePacketEvent
import gg.skytils.skytilsmod.utils.SuperSecretSettings
import gg.skytils.skytilsmod.utils.Utils
import net.minecraft.inventory.ContainerChest
import net.minecraft.item.EnumDyeColor
import net.minecraft.item.ItemStack
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraft.network.play.server.S30PacketWindowItems
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.random.Random

object SelectAllColorSolver {

    @JvmField
    val shouldClick = hashSetOf<Int>()
    private var colorNeeded: String? = null
    private val colors by lazy {
        EnumDyeColor.entries.associateWith { it.name.replace("_", " ").uppercase() }
    }
    private var windowId: Int? = null

    @SubscribeEvent
    fun onCloseWindow(event: GuiContainerEvent.CloseWindowEvent) {
        shouldClick.clear()
        colorNeeded = null
        windowId = null
    }

    @SubscribeEvent
    fun onPacket(event: MainReceivePacketEvent<*, *>) {
        if (event.packet is S2DPacketOpenWindow) {
            val chestName = event.packet.windowTitle.unformattedText
            if (chestName.startsWith("Select all the")) {
                windowId = event.packet.windowId

                val promptColor = colors.entries.find { (_, name) ->
                    chestName.contains(name)
                }?.key?.unlocalizedName
                if (promptColor != colorNeeded) {
                    colorNeeded = promptColor
                    shouldClick.clear()
                }
            } else {
                shouldClick.clear()
                colorNeeded = null
                windowId = null
            }
        }

        if (!Skytils.config.selectAllColorTerminalSolver || !TerminalFeatures.isInPhase3()) return

        when (event.packet) {
            is S2FPacketSetSlot -> {
                if (event.packet.func_149175_c() != windowId) return
                handleItemStack(event.packet.func_149173_d(), event.packet.func_149174_e())
            }
            is S30PacketWindowItems -> {
                if (event.packet.func_148911_c() != windowId) return
                event.packet.itemStacks.forEachIndexed(::handleItemStack)
            }
        }
    }

    private fun handleItemStack(slot: Int, item: ItemStack) {
        val column = slot % 9
        if (slot in 9..44 && column in 1..7) {
            if (item.isItemEnchanted) {
                shouldClick.remove(slot)
            } else if (SuperSecretSettings.bennettArthur) {
                if (Random.nextInt(3) == 0) shouldClick.add(slot)
            } else if (item.unlocalizedName.contains(colorNeeded!!)) {
                shouldClick.add(slot)
            }
        }
    }

    @SubscribeEvent
    fun onDrawSlot(event: GuiContainerEvent.DrawSlotEvent.Pre) {
        if (!Utils.inDungeons) return
        if (!Skytils.config.selectAllColorTerminalSolver) return
        if (event.container is ContainerChest) {
            if (event.chestName.startsWith("Select all the")) {
                val slot = event.slot
                if (shouldClick.isNotEmpty() && slot.slotNumber !in shouldClick && slot.inventory !== mc.thePlayer.inventory) {
                    event.isCanceled = true
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!Utils.inDungeons || !Skytils.config.selectAllColorTerminalSolver || !Skytils.config.blockIncorrectTerminalClicks) return
        if (event.container is ContainerChest && event.chestName.startsWith("Select all the")) {
            if (shouldClick.isNotEmpty() && !shouldClick.contains(event.slotId)) event.isCanceled = true
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onTooltip(event: ItemTooltipEvent) {
        if (event.toolTip == null || !Utils.inDungeons || !Skytils.config.selectAllColorTerminalSolver) return
        val chest = mc.thePlayer.openContainer
        if (chest is ContainerChest) {
            val chestName = chest.lowerChestInventory.displayName.unformattedText
            if (chestName.startsWith("Select all the")) {
                event.toolTip.clear()
            }
        }
    }
}