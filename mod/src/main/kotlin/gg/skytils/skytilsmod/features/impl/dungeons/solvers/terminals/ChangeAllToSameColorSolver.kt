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
import gg.skytils.event.impl.screen.GuiContainerForegroundDrawnEvent
import gg.skytils.event.impl.screen.GuiContainerSlotClickEvent
import gg.skytils.event.impl.screen.ScreenOpenEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.utils.Utils
import net.minecraft.client.gui.screen.ingame.HandledScreen
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.skytils.skytilsmod.utils.formattedText
import net.minecraft.block.StainedGlassPaneBlock
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack
import net.minecraft.screen.GenericContainerScreenHandler
import net.minecraft.util.DyeColor
import java.awt.Color

object ChangeAllToSameColorSolver : EventSubscriber {
    private val ordering =
        setOf(
            DyeColor.RED,
            DyeColor.ORANGE,
            DyeColor.YELLOW,
            DyeColor.GREEN,
            DyeColor.BLUE
        ).withIndex().associate { (i, c) ->
            c to i
        }
    private var mostCommon = DyeColor.RED
    private var isLocked = false

    override fun setup() {
        register(::onForegroundEvent)
        register(::onSlotClick, EventPriority.High)
        register(::onTooltip, EventPriority.Lowest)
        register(::onWindowClose)
    }

    fun onWindowClose(event: ScreenOpenEvent) {
        if (event.screen as? HandledScreen<*> == null) isLocked = false
    }


    fun onForegroundEvent(event: GuiContainerForegroundDrawnEvent) {
        if (!Utils.inDungeons || !Skytils.config.changeAllSameColorTerminalSolver || event.container !is GenericContainerScreenHandler || event.chestName != "Change all to same color!") return
        val container = event.container as? GenericContainerScreenHandler ?: return
        val grid = container.slots.filter {
            it.inventory == container.inventory && it.stack?.name?.formattedText?.startsWith("§a") == true
        }

        if (!Skytils.config.changeToSameColorLock || !isLocked) {
            val counts = ordering.keys.associateWith { c -> grid.count { getColorFromItem(it.stack) == c } }
            val currentPath = counts[mostCommon]!!
            val (candidate, maxCount) = counts.maxBy { it.value }

            if (maxCount > currentPath) {
                mostCommon = candidate
            }
            isLocked = true
        }

        val targetIndex = ordering[mostCommon]!!
        val mapping = grid.filter { getColorFromItem(it.stack) != mostCommon }.associateWith { slot ->
            val stack = slot.stack
            val myIndex = ordering[getColorFromItem(stack)]!!
            val normalCycle = ((targetIndex - myIndex) % ordering.size + ordering.size) % ordering.size
            val otherCycle = -((myIndex - targetIndex) % ordering.size + ordering.size) % ordering.size
            normalCycle to otherCycle
        }
        val matrixStack = UMatrixStack.Compat.get()
        matrixStack.push()
        matrixStack.translate(0f, 0f, 299f)
        for ((slot, clicks) in mapping) {
            var betterOpt = if (clicks.first > -clicks.second) clicks.second else clicks.first
            var color = Color.WHITE
            if (Skytils.config.changeToSameColorMode == 1) {
                betterOpt = clicks.first
                when (betterOpt) {
                    1 -> color = Color.GREEN
                    2, 3 -> color = Color.YELLOW
                    4 -> color = Color.RED
                }
            }

            matrixStack.runWithGlobalState {
                // disable lighting
                GlStateManager._disableDepthTest()
                GlStateManager._disableBlend()
                UGraphics.drawString(
                    matrixStack,
                    "$betterOpt",
                    slot.x + 9f,
                    slot.y + 4f,
                    color.rgb,
                    false
                )
                // enable lighting
                GlStateManager._enableDepthTest()
            }
        }
        matrixStack.pop()
    }

    fun onSlotClick(event: GuiContainerSlotClickEvent) {
        if (!Utils.inDungeons || !Skytils.config.changeAllSameColorTerminalSolver || !Skytils.config.blockIncorrectTerminalClicks) return
        if (event.container is GenericContainerScreenHandler && event.chestName == "Change all to same color!") {
            if (event.slot?.stack?.let(::getColorFromItem) == mostCommon) event.cancelled = true
        }
    }

    fun onTooltip(event: ItemTooltipEvent) {
        if (!Utils.inDungeons || !Skytils.config.changeAllSameColorTerminalSolver) return
        val chestName = mc.currentScreen?.takeIf { it is GenericContainerScreen }?.title?.string
        if (chestName == "Change all to same color!") {
            event.tooltip.clear()
        }
    }

    private fun getColorFromItem(itemstack: ItemStack): DyeColor =
        ((itemstack.item as BlockItem).block as StainedGlassPaneBlock).color
}
