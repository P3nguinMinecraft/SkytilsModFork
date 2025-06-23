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

package gg.skytils.skytilsmod.features.impl.misc

import gg.essential.elementa.layoutdsl.LayoutScope
import gg.essential.elementa.layoutdsl.Modifier
import gg.essential.elementa.layoutdsl.color
import gg.essential.elementa.layoutdsl.fillHeight
import gg.essential.elementa.layoutdsl.height
import gg.essential.elementa.layoutdsl.row
import gg.essential.elementa.layoutdsl.widthAspect
import gg.essential.elementa.state.v2.State
import gg.essential.elementa.state.v2.mutableStateOf
import gg.skytils.event.EventSubscriber
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod._event.MainThreadPacketReceiveEvent
import gg.skytils.skytilsmod.core.GuiManager
import gg.skytils.skytilsmod.core.structure.v2.HudElement
import gg.skytils.skytilsmod.gui.layout.text
import gg.skytils.skytilsmod.gui.components.ItemComponent
import gg.skytils.skytilsmod.utils.ItemUtil
import gg.skytils.skytilsmod.utils.SBInfo
import gg.skytils.skytilsmod.utils.Utils
import net.minecraft.item.Items
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket
import java.awt.Color

object QuiverStuff : EventSubscriber {
    private val activeArrowRegex = Regex("§7Active Arrow: (?<type>§.[\\w -]+) §7\\(§e(?<amount>\\d+)§7\\)")

    private val selectedTypeState = mutableStateOf("")
    private val arrowCountState = mutableStateOf(-1)
    private var sentWarning = false

    init {
        Skytils.guiManager.registerElement(QuiverDisplay)
        Skytils.guiManager.registerElement(SelectedArrowDisplay)
    }

    override fun setup() {
        register(::onReceivePacket)
    }

    fun onReceivePacket(event: MainThreadPacketReceiveEvent<*>) {
        if (!Utils.inSkyblock || event.packet !is ScreenHandlerSlotUpdateS2CPacket || event.packet.slot != 44) return
        val stack = event.packet.stack ?: return
        if (!Utils.equalsOneOf(stack.item, Items.ARROW, Items.FEATHER)) return
        val line = ItemUtil.getItemLore(stack).getOrNull(4) ?: return
        val match = activeArrowRegex.matchEntire(line) ?: return
        selectedTypeState.set { match.groups["type"]?.value ?: "" }
        arrowCountState.set { match.groups["amount"]?.value?.toIntOrNull() ?: -1 }

        if (sentWarning && Skytils.config.restockArrowsWarning != 0 && arrowCountState.getUntracked() >= Skytils.config.restockArrowsWarning) {
            sentWarning = false
        } else if (
            !sentWarning && arrowCountState.getUntracked() != -1 &&
            Skytils.config.restockArrowsWarning != 0 && arrowCountState.getUntracked() < Skytils.config.restockArrowsWarning
        ) {
            GuiManager.createTitle("§c§lRESTOCK §r${selectedTypeState.getUntracked().ifBlank { "§cUnknown" }}", 60)
            sentWarning = true
        }
    }

    object QuiverDisplay : HudElement("Quiver Display", 0.05, 0.4) {
        override val toggleState: State<Boolean>
            get() = Skytils.config.quiverDisplay
        private val ARROW = ItemStack(Items.ARROW)
        private val color = State {
            val count = arrowCountState()
            when {
                count < 400 -> Color.RED
                count < 1200 -> Color.YELLOW
                else -> Color.GREEN
            }
        }
        private val text = State {
            val count = arrowCountState()
            if (count == -1) "???" else count.toString()
        }
        override fun LayoutScope.render() {
            if_(SBInfo.skyblockState) {
                row(Modifier.height(16f)) {
                    ItemComponent(ARROW)(Modifier.fillHeight().widthAspect(1f))
                    text(text, Modifier.color(color))
                }
            }
        }

        override fun LayoutScope.demoRender() {
            row(Modifier.height(16f)) {
                ItemComponent(ARROW)(Modifier.fillHeight().widthAspect(1f))
                text("2000", Modifier.color(Color.GREEN))
            }
        }

    }

    object SelectedArrowDisplay : HudElement("SelectedArrowDisplay", 0.65, 0.85) {
        override val toggleState: State<Boolean>
            get() = Skytils.config.showSelectedArrowDisplay
        val text = State { "Selected: §r${selectedTypeState().ifBlank { "§cUnknown" }}" }
        override fun LayoutScope.render() {
            if_(SBInfo.skyblockState) {
                text(text, Modifier.color(Color.GREEN))
            }
        }

        override fun LayoutScope.demoRender() {
            text("Selected: Redstone-tipped Arrow")
        }
    }
}