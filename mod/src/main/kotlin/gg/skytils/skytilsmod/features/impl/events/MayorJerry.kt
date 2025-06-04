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
package gg.skytils.skytilsmod.features.impl.events

import gg.essential.elementa.layoutdsl.LayoutScope
import gg.essential.elementa.layoutdsl.Modifier
import gg.essential.elementa.layoutdsl.color
import gg.essential.elementa.layoutdsl.row
import gg.essential.elementa.state.v2.State
import gg.essential.elementa.state.v2.combinators.and
import gg.essential.elementa.state.v2.mutableStateOf
import gg.essential.elementa.state.v2.stateUsingSystemTime
import gg.essential.universal.UChat
import gg.skytils.event.EventPriority
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.play.ChatMessageReceivedEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.core.GuiManager.createTitle
import gg.skytils.skytilsmod.core.structure.v2.HudElement
import gg.skytils.skytilsmod.features.impl.handlers.MayorInfo
import gg.skytils.skytilsmod.features.impl.trackers.impl.MayorJerryTracker
import gg.skytils.skytilsmod.gui.layout.text
import gg.skytils.skytilsmod.gui.profile.components.ItemComponent
import gg.skytils.skytilsmod.utils.NumberUtil
import gg.skytils.skytilsmod.utils.SBInfo
import gg.skytils.skytilsmod.utils.Utils
import gg.skytils.skytilsmod.utils.formattedText
import gg.skytils.skytilsmod.utils.stripControlCodes
import net.minecraft.item.Items
import net.minecraft.item.ItemStack
import java.awt.Color
import java.time.Instant
import java.time.temporal.ChronoUnit

object MayorJerry : EventSubscriber {

    private val jerryType = Regex("(\\w+)(?=\\s+Jerry)")
    val lastJerryState = mutableStateOf(Instant.MAX)

    init {
        Skytils.guiManager.registerElement(JerryPerkHud())
        Skytils.guiManager.registerElement(HiddenJerryTimerHud())
    }

    override fun setup() {
        register(::onChat, EventPriority.Highest)
    }

    fun onChat(event: ChatMessageReceivedEvent) {
        if (!Utils.inSkyblock) return
        if (!Skytils.config.hiddenJerryTimer && !Skytils.config.hiddenJerryAlert && !Skytils.config.trackHiddenJerry) return
        val unformatted = event.message.string.stripControlCodes()
        val formatted = event.message.formattedText
        if (formatted.startsWith("§b ☺ §e") && unformatted.contains("Jerry") && !unformatted.contains(
                "Jerry Box"
            )
        ) {
            val match = jerryType.find(formatted)
            if (match != null) {
                val lastJerry = lastJerryState.getUntracked()
                val now = Instant.now()
                if (Skytils.config.hiddenJerryTimer && lastJerry.isBefore(now)) UChat.chat(
                    "§bIt has been ${
                        NumberUtil.nf.format(
                            lastJerry.until(now, ChronoUnit.SECONDS)
                        )
                    } seconds since the last Jerry."
                )
                lastJerryState.set { now }
                val color = match.groups[1]!!.value
                MayorJerryTracker.onJerry("§$color Jerry")
                if (Skytils.config.hiddenJerryAlert) {
                    createTitle("§" + color.uppercase() + " JERRY!", 60)
                }
            }
        }
    }

    class JerryPerkHud : HudElement("Mayor Jerry Perk Display", 10f, 10f) {
        val diff = stateUsingSystemTime { time -> time.until(MayorInfo.newJerryPerksState()) }
        override fun LayoutScope.render() {
            if_(SBInfo.skyblockState and { MayorInfo.currentMayorState() == "Jerry" }) {
                if_(stateUsingSystemTime{ time -> MayorInfo.jerryMayorState() == null || diff().getValue().isPositive }) {
                    text("Visit Jerry!", Modifier.color(Color.RED))
                } `else`  {
                    val timer = State {
                        "${MayorInfo.jerryMayor!!.name}: ${
                            diff().getValue().run { 
                                "${toHoursPart()}h${toMinutesPart()}m"
                            }
                        }"
                    }
                    text(timer, Modifier.color(Color.ORANGE))
                }
            }
        }

        override fun LayoutScope.demoRender() {
            text("Paul (0:30)", Modifier.color(Color.ORANGE))
        }

    }

    class HiddenJerryTimerHud : HudElement("Mayor Jerry Timer", 10f, 20f) {
        private val villagerEgg = ItemStack(Items.VILLAGER_SPAWN_EGG)
        override fun LayoutScope.render() {
            val elapsed = stateUsingSystemTime { time -> time.until(lastJerryState()) }
            if_(SBInfo.skyblockState and { elapsed().isNegative }) {
                row {
                    ItemComponent(villagerEgg)()
                    text({
                        val since = elapsed().getValue()
                        val minutes = since.toMinutesPart()
                        "${if (minutes >= 6) "§a" else ""}${minutes}:${
                            "%02d".format(
                                since.toSecondsPart()
                            )
                        }"
                    }, Modifier.color(Color.ORANGE))

                }
            }
        }

        override fun LayoutScope.demoRender() {
            row {
                ItemComponent(villagerEgg)()
                text("0:30", Modifier.color(Color.ORANGE))
            }
        }

    }

}