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
import gg.skytils.skytilsmod.utils.stripControlCodes
import net.minecraft.inventory.ContainerChest
import net.minecraft.item.ItemStack
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraft.network.play.server.S30PacketWindowItems
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.random.Random

object StartsWithSequenceSolver {

    @JvmField
    val shouldClick = hashSetOf<Int>()
    private var sequenceNeeded: String? = null
    private val titlePattern = Regex("^What starts with: ['\"](.+)['\"]\\?$")
    private var windowId: Int? = null

    @SubscribeEvent
    fun onCloseWindow(event: GuiContainerEvent.CloseWindowEvent) {
        shouldClick.clear()
        sequenceNeeded = null
        windowId = null
    }

    @SubscribeEvent
    fun onPacket(event: MainReceivePacketEvent<*, *>) {
        if (event.packet is S2DPacketOpenWindow) {
            val chestName = event.packet.windowTitle.unformattedText
            if (chestName.startsWith("What starts with:")) {
                windowId = event.packet.windowId

                val sequence = titlePattern.find(chestName)?.groupValues?.get(1) ?: return
                if (sequence != sequenceNeeded) {
                    sequenceNeeded = sequence
                    shouldClick.clear()
                }
            } else {
                shouldClick.clear()
                sequenceNeeded = null
                windowId = null
            }
        }

        if (!Skytils.config.startsWithSequenceTerminalSolver || !TerminalFeatures.isInPhase3()) return

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
            } else if (item.displayName.stripControlCodes().startsWith(sequenceNeeded!!)) {
                shouldClick.add(slot)
            }
        }
    }

    @SubscribeEvent
    fun onDrawSlot(event: GuiContainerEvent.DrawSlotEvent.Pre) {
        if (!Utils.inDungeons || !Skytils.config.startsWithSequenceTerminalSolver) return
        if (event.container is ContainerChest && event.chestName.startsWith("What starts with:")) {
            val slot = event.slot
            if (shouldClick.size > 0 && !shouldClick.contains(slot.slotNumber) && slot.inventory !== mc.thePlayer.inventory) {
                event.isCanceled = true
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!Utils.inDungeons || !Skytils.config.startsWithSequenceTerminalSolver || !Skytils.config.blockIncorrectTerminalClicks) return
        if (event.container is ContainerChest && event.chestName.startsWith("What starts with:")) {
            if (shouldClick.isNotEmpty() && !shouldClick.contains(event.slotId)) event.isCanceled = true
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onTooltip(event: ItemTooltipEvent) {
        if (event.toolTip == null || !Utils.inDungeons || !Skytils.config.startsWithSequenceTerminalSolver) return
        val container = mc.thePlayer?.openContainer
        if (container is ContainerChest) {
            val chestName = container.lowerChestInventory.displayName.unformattedText
            if (chestName.startsWith("What starts with:")) {
                event.toolTip.clear()
            }
        }
    }
}