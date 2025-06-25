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

import com.mojang.blaze3d.opengl.GlStateManager
import gg.skytils.event.EventPriority
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.item.ItemTooltipEvent
import gg.skytils.event.impl.screen.GuiContainerBackgroundDrawnEvent
import gg.skytils.event.impl.screen.GuiContainerPreDrawSlotEvent
import gg.skytils.event.impl.screen.ScreenOpenEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.utils.RenderUtil.highlight
import com.mojang.blaze3d.systems.RenderSystem
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UMinecraft
import gg.skytils.skytilsmod.utils.rendering.DrawHelper
import net.minecraft.block.StainedGlassPaneBlock
import net.minecraft.client.font.TextRenderer
import net.minecraft.item.BlockItem
import net.minecraft.screen.GenericContainerScreenHandler
import org.lwjgl.opengl.GL11
import java.awt.Color

object ClickInOrderSolver : EventSubscriber {

    private val slotOrder = HashMap<Int, Int>()
    private var neededClick = 0
    private val menuSlots = (10..16) + (19..25)

    override fun setup() {
        register(::onGuiOpen)
        register(::onBackgroundDrawn)
        register(::onDrawSlotLow, EventPriority.Low)
        register(::onTooltip, EventPriority.Lowest)
    }

    fun onGuiOpen(event: ScreenOpenEvent) {
        neededClick = 0
        slotOrder.clear()
    }

    fun onBackgroundDrawn(event: GuiContainerBackgroundDrawnEvent) {
        if (!TerminalFeatures.isInPhase3() || !Skytils.config.clickInOrderTerminalSolver || event.container !is GenericContainerScreenHandler) return
        val invSlots = event.container.slots
        if (event.chestName == "Click in order!") {
            for (i in menuSlots) {
                val itemStack = invSlots[i].stack ?: continue
                if ((itemStack.item as? BlockItem)?.block is StainedGlassPaneBlock || (itemStack.damage != 14 && itemStack.damage != 5)) continue
                if (itemStack.damage == 5 && itemStack.count > neededClick) {
                    neededClick = itemStack.count
                }
                slotOrder[itemStack.count - 1] = i
            }
        }
        if (slotOrder.isEmpty()) return
        val firstSlot = slotOrder[neededClick]
        val secondSlot = slotOrder[neededClick + 1]
        val thirdSlot = slotOrder[neededClick + 2]
        if (firstSlot != null) {
            val slot = invSlots[firstSlot]
            if (slot != null) slot highlight Skytils.config.clickInOrderFirst
        }
        if (secondSlot != null) {
            val slot = invSlots[secondSlot]
            if (slot != null) slot highlight Skytils.config.clickInOrderSecond
        }
        if (thirdSlot != null) {
            val slot = invSlots[thirdSlot]
            if (slot != null) slot highlight Skytils.config.clickInOrderThird
        }
    }

    fun onDrawSlotLow(event: GuiContainerPreDrawSlotEvent) {
        if (!TerminalFeatures.isInPhase3()) return
        if (!Skytils.config.clickInOrderTerminalSolver) return
        if (event.container is GenericContainerScreenHandler) {
            val fr = mc.textRenderer
            val slot = event.slot
            if (event.chestName == "Click in order!") {
                if (slot.hasStack() && slot.inventory !== mc.player?.inventory) {
                    val item = slot.stack
                    if ((item.item as? BlockItem)?.block is StainedGlassPaneBlock && item.damage == 14) {
                        val matrixStack = UMatrixStack()
                        DrawHelper.setupContainerScreenTransformations(matrixStack, aboveItems = true)
                        UGraphics.drawString(matrixStack, item.count.toString(), slot.x + 9 - fr.getWidth(item.count.toString()) / 2f, slot.y + 4f, Color.WHITE.rgb, false)
                        event.cancelled = true
                    }
                }
            }
        }
    }

    fun onTooltip(event: ItemTooltipEvent) {
        if (!TerminalFeatures.isInPhase3() || !Skytils.config.clickInOrderTerminalSolver) return
        val chestName = mc.currentScreen?.title ?: return
        if (chestName.string == "Click in order!") {
            event.tooltip.clear()
        }
    }
}