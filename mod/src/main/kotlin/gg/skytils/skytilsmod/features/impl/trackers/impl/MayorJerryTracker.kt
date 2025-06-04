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

package gg.skytils.skytilsmod.features.impl.trackers.impl

import gg.essential.elementa.layoutdsl.LayoutScope
import gg.essential.elementa.layoutdsl.Modifier
import gg.essential.elementa.layoutdsl.color
import gg.essential.elementa.state.v2.MutableState
import gg.essential.elementa.state.v2.mutableStateOf
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.play.ChatMessageReceivedEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.core.structure.v2.HudElement
import gg.skytils.skytilsmod.features.impl.trackers.Tracker
import gg.skytils.skytilsmod.gui.layout.text
import gg.skytils.skytilsmod.utils.SBInfo
import gg.skytils.skytilsmod.utils.formattedText
import gg.skytils.skytilsmod.utils.stripControlCodes
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.awt.Color
import java.io.Reader
import java.io.Writer

object MayorJerryTracker : EventSubscriber, Tracker("mayorjerry") {

    @Suppress("UNUSED")
    enum class HiddenJerry(val type: String, val colorCode: String, val discoveredTimes: MutableState<Int> = mutableStateOf(0)) {
        GREEN("Green Jerry", "a"),
        BLUE("Blue Jerry", "9"),
        PURPLE("Purple Jerry", "5"),
        GOLDEN("Golden Jerry", "6");

        companion object {
            fun getFromString(str: String): HiddenJerry? {
                return entries.find { str == "§${it.colorCode}${it.type}" }
            }

            fun getFromType(str: String): HiddenJerry? {
                return entries.find { str == it.type }
            }
        }
    }

    @Suppress("UNUSED")
    enum class JerryBoxDrops(val dropName: String, val colorCode: String, val droppedAmount: MutableState<Int> = mutableStateOf(0)) {
        COINS("Coins", "6"),
        FARMINGXP("Farming XP", "b"),
        FORAGINGXP("Foraging XP", "b"),
        MININGXP("Mining XP", "b"),
        JERRYCANDY("Jerry Candy", "a"),
        JERRYRUNE("Jerry Rune", "f"),
        JERRYTALI("Green Jerry Talisman", "a"),
        JERRYSTONE("Jerry Stone", "9"),
        JERRYCHINE("Jerry-chine Gun", "5"),
        JERRYGLASSES("Jerry 3D Glasses", "6");

        companion object {
            fun getFromName(str: String): JerryBoxDrops? {
                return entries.find { it.dropName == str }
            }
        }
    }

    fun onJerry(type: String) {
        if (!Skytils.config.trackHiddenJerry) return
        HiddenJerry.getFromString(type)!!.discoveredTimes.set { it + 1 }
        markDirty<MayorJerryTracker>()
    }

    override fun setup() {
        register(::onChat, gg.skytils.event.EventPriority.Highest)
    }

    fun onChat(event: ChatMessageReceivedEvent) {
        if (!Skytils.config.trackHiddenJerry) return
        val formatted = event.message.formattedText
        val unformatted = event.message.string.stripControlCodes()
        if (!formatted.startsWith("§r§b ☺ ")) return
        if (formatted.startsWith("§r§b ☺ §r§eYou claimed ") && formatted.endsWith("§efrom the Jerry Box!§r")) {
            if (formatted.contains("coins")) {
                JerryBoxDrops.COINS.droppedAmount.set { it + unformatted.replace(Regex("[^0-9]"), "").toInt() }
                markDirty<MayorJerryTracker>()
            } else if (formatted.contains("XP")) {
                val xpType = with(formatted) {
                    when {
                        contains("Farming XP") -> JerryBoxDrops.FARMINGXP
                        contains("Foraging XP") -> JerryBoxDrops.FORAGINGXP
                        contains("Mining XP") -> JerryBoxDrops.MININGXP
                        else -> null
                    }
                }
                if (xpType != null) {
                    xpType.droppedAmount.set { it + unformatted.replace(Regex("[^0-9]"), "").toInt() }
                    markDirty<MayorJerryTracker>()
                }
            } else {
                (JerryBoxDrops.entries.find {
                    formatted.contains(it.dropName)
                } ?: return).droppedAmount.set { it + 1 }
                markDirty<MayorJerryTracker>()
            }
            return
        }
        if (formatted.endsWith("§r§ein a Jerry Box!§r") && formatted.contains(mc.player!!.name.string)) {
            (JerryBoxDrops.entries.find {
                formatted.contains(it.dropName)
            } ?: return).droppedAmount.set { it + 1 }
            markDirty<MayorJerryTracker>()
        }
    }

    override fun resetLoot() {
        HiddenJerry.entries.onEach { it.discoveredTimes.set { 0 } }
        JerryBoxDrops.entries.onEach { it.droppedAmount.set { 0 } }
    }

    // TODO: 5/3/2022  Redo this entire thing
    @Serializable
    private data class TrackerSave(
        val jerry: Map<HiddenJerry, Int>,
        val drops: Map<JerryBoxDrops, Int>
    )

    override fun read(reader: Reader) {
        val save = json.decodeFromString<TrackerSave>(reader.readText())
        HiddenJerry.entries.forEach {
            it.discoveredTimes.set { _ -> save.jerry[it] ?: 0 }
        }
        JerryBoxDrops.entries.forEach {
            it.droppedAmount.set { _ -> save.drops[it] ?: 0 }
        }
    }

    override fun write(writer: Writer) {
        writer.write(
            json.encodeToString(
                TrackerSave(
                    HiddenJerry.entries.associateWith { jerry -> jerry.discoveredTimes.getUntracked() },
                    JerryBoxDrops.entries.associateWith { drop -> drop.droppedAmount.getUntracked() }
                )
            )
        )
    }

    override fun setDefault(writer: Writer) {
        write(writer)
    }

    init {
        Skytils.guiManager.registerElement(JerryTrackerHud())
    }

    class JerryTrackerHud : HudElement("Mayor Jerry Tracker", 150f, 120f) {
        override fun LayoutScope.render() {
            if_(SBInfo.skyblockState) {
                HiddenJerry.entries.forEach { jerry ->
                    if_({ jerry.discoveredTimes() != 0 }) {
                        text({ "§${jerry.colorCode}${jerry.type}§f: ${jerry.discoveredTimes()}" })
                    }
                }
                JerryBoxDrops.entries.forEach { drop ->
                    if_({ drop.droppedAmount() != 0 }) {
                        text({ "§${drop.colorCode}${drop.dropName}§f: ${drop.droppedAmount()}" })
                    }
                }
            }
        }

        override fun LayoutScope.demoRender() {
            text("Jerry Tracker", Modifier.color(Color.YELLOW))
        }

    }
}