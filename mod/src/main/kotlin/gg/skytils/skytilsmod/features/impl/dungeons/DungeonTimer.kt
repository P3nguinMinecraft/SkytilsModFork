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
package gg.skytils.skytilsmod.features.impl.dungeons

import gg.essential.elementa.unstable.layoutdsl.LayoutScope
import gg.essential.elementa.unstable.layoutdsl.column
import gg.essential.elementa.unstable.state.v2.MutableState
import gg.essential.elementa.unstable.state.v2.State
import gg.essential.elementa.unstable.state.v2.combinators.and
import gg.essential.elementa.unstable.state.v2.mutableStateOf
import gg.essential.elementa.unstable.state.v2.stateOf
import gg.essential.elementa.unstable.state.v2.withSystemTime
import gg.essential.universal.UChat
import gg.skytils.event.EventPriority
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.play.ChatMessageReceivedEvent
import gg.skytils.event.impl.play.WorldUnloadEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.core.structure.v2.HudElement
import gg.skytils.skytilsmod.core.tickTimer
import gg.skytils.skytilsmod.features.impl.dungeons.DungeonFeatures.dungeonFloorNumber
import gg.skytils.skytilsmod.features.impl.dungeons.DungeonFeatures.dungeonFloorNumberState
import gg.skytils.skytilsmod.features.impl.dungeons.catlas.core.map.RoomState
import gg.skytils.skytilsmod.features.impl.dungeons.catlas.core.map.RoomType
import gg.skytils.skytilsmod.features.impl.dungeons.catlas.handlers.DungeonInfo
import gg.skytils.skytilsmod.gui.layout.text
import gg.skytils.skytilsmod.listeners.DungeonListener
import gg.skytils.skytilsmod.utils.NumberUtil
import gg.skytils.skytilsmod.utils.NumberUtil.roundToPrecision
import gg.skytils.skytilsmod.utils.SBInfo
import gg.skytils.skytilsmod.utils.Utils
import gg.skytils.skytilsmod.utils.formattedText
import gg.skytils.skytilsmod.utils.stripControlCodes
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

object DungeonTimer : EventSubscriber {
    val dungeonStartTime
        get() = dungeonStartTimeState.getUntracked()
    val dungeonStartTimeState: MutableState<Instant?> = mutableStateOf(null)
    val bloodOpenTime
        get() = bloodOpenTimeState.getUntracked()
    val bloodOpenTimeState: MutableState<Instant?> = mutableStateOf(null)
    val bloodClearTime
        get() = bloodClearTimeState.getUntracked()
    val bloodClearTimeState: MutableState<Instant?> = mutableStateOf(null)
    val bossEntryTime
        get() = bossEntryTimeState.getUntracked()
    val bossEntryTimeState: MutableState<Instant?> = mutableStateOf(null)
    val bossClearTime
        get() = bossClearTimeState.getUntracked()
    val bossClearTimeState: MutableState<Instant?> = mutableStateOf(null)
    val phase1ClearTime
        get() = phase1ClearTimeState.getUntracked()
    val phase1ClearTimeState: MutableState<Instant?> = mutableStateOf(null)
    val phase2ClearTime
        get() = phase2ClearTimeState.getUntracked()
    val phase2ClearTimeState: MutableState<Instant?> = mutableStateOf(null)
    val phase3ClearTime
        get() = phase3ClearTimeState.getUntracked()
    val phase3ClearTimeState: MutableState<Instant?> = mutableStateOf(null)
    val terminalClearTime
        get() = terminalClearTimeState.getUntracked()
    val terminalClearTimeState: MutableState<Instant?> = mutableStateOf(null)
    val phase4ClearTime
        get() = phase4ClearTimeState.getUntracked()
    val phase4ClearTimeState: MutableState<Instant?> = mutableStateOf(null)
    val terraClearTime
        get() = terraClearTimeState.getUntracked()
    val terraClearTimeState: MutableState<Instant?> = mutableStateOf(null)
    val giantsClearTime
        get() = giantsClearTimeState.getUntracked()
    val giantsClearTimeState: MutableState<Instant?> = mutableStateOf(null)
    val witherDoors
        get() = witherDoorsState.getUntracked()
    val witherDoorsState = mutableStateOf(0)
    val scoreShownAt
        get() = scoreShownAtState.getUntracked()
    val scoreShownAtState: MutableState<Instant?> = mutableStateOf(null)

    init {
        Skytils.guiManager.registerElement(DungeonTimerHud)
        Skytils.guiManager.registerElement(NecronPhaseTimerHud)
        Skytils.guiManager.registerElement(SadanPhaseTimer)
    }

    override fun setup() {
        register(::onChat, EventPriority.Highest)
        register(::onWorldChange)
    }

    fun onChat(event: ChatMessageReceivedEvent) {
        if (!Utils.inDungeons) return
        val message = event.message.formattedText
        val unformatted = event.message.string.stripControlCodes()
        when {
            scoreShownAt == null && message.contains("§r§fTeam Score: §r") -> {
                scoreShownAtState.set(Instant.now())
            }

/*            (message == "§r§aStarting in 1 second.§r") && dungeonStartTime == -1L -> {
                dungeonStartTime = System.currentTimeMillis() + 1000
            }*/

            message.endsWith(" §r§ehas obtained §r§a§r§6§r§8Wither Key§r§e!§r") || unformatted == "A Wither Key was picked up!" || message.endsWith(
                "§r§ehas obtained §r§8Wither Key§r§e!§r"
            ) -> {
                witherDoorsState.set { it + 1 }
            }

            bloodOpenTime == null && (unformatted == "The BLOOD DOOR has been opened!" || message.startsWith(
                "§r§c[BOSS] The Watcher§r§f"
            )) -> {
                bloodOpenTimeState.set { Instant.now() }
                if (Skytils.config.dungeonTimer) UChat.chat(
                    "§4Blood §btook ${diff(bloodOpenTime!!, dungeonStartTime!!)} seconds to open."
                )
            }

            bloodOpenTime != null && bloodClearTime == null && message == "§r§c[BOSS] The Watcher§r§f: That will be enough for now.§r" -> {
                DungeonInfo.uniqueRooms["Blood"]?.let {
                    assert(it.mainRoom.data.type == RoomType.BLOOD)
                    if (it.mainRoom.state > RoomState.CLEARED) {
                        it.mainRoom.state = RoomState.CLEARED
                    }
                }
            }

            message == "§r§c[BOSS] The Watcher§r§f: You have proven yourself. You may pass.§r" -> {
                bloodClearTimeState.set { Instant.now() }
                if (Skytils.config.dungeonTimer) UChat.chat(
                    "§cWatcher §btook ${diff(bloodClearTime!!, bloodOpenTime!!)} seconds to clear."
                )
            }

            bossEntryTime == null && unformatted.startsWith("[BOSS] ") && unformatted.contains(":") -> {
                val bossName = unformatted.substringAfter("[BOSS] ").substringBefore(":").trim()
                if (bossName != "The Watcher" && DungeonFeatures.dungeonFloor != null && Utils.checkBossName(
                        DungeonFeatures.dungeonFloor!!,
                        bossName
                    )
                ) {
                    bossEntryTimeState.set { Instant.now() }
                    DungeonListener.markAllRevived()
                    if (Skytils.config.dungeonTimer && bloodClearTime != null) UChat.chat(
                        "§dPortal §btook ${diff(bossEntryTime!!, bloodClearTime!!)} seconds to enter."
                    )
                }
            }

            bossEntryTime != null && bossClearTime == null && message.contains("§r§c☠ §r§eDefeated §r") -> {
                bossClearTimeState.set { Instant.now() }
                tickTimer(5) {
                    arrayListOf<String>().apply {
                        if (Skytils.config.dungeonTimer) {
                            add("§7Wither Doors: $witherDoors")
                            add("§4Blood §btook ${diff(bloodOpenTime!!, dungeonStartTime!!)} seconds to open.")
                            if (bloodClearTime == null) {
                                add("§c§lGG! §cWatcher §bWAS SKIPPED!")
                                add("§d§lGG! §dPortal §bWAS SKIPPED!")
                            } else {
                                add("§cWatcher §btook ${diff(bloodClearTime!!, bloodOpenTime!!)} seconds to clear.")
                                add("§dPortal §btook ${diff(bossEntryTime!!, bloodClearTime!!)} seconds to enter.")
                            }// (bossEntryTime - dungeonStartTime!!.toEpochMilli()) / 1000.0
                            add("§9Boss entry §bwas ${dungeonTimeFormat(Duration.between(dungeonStartTime, bossEntryTime))}.")
                        }
                        if (Skytils.config.sadanPhaseTimer && dungeonFloorNumber == 6) {
                            add("§dTerracotta §btook ${diff(terraClearTime!!, bossEntryTime!!)} seconds.")
                            add("§aGiants §btook ${diff(giantsClearTime!!, terraClearTime!!)} seconds.")
                            add("§cSadan §btook ${diff(bossClearTime!!, giantsClearTime!!)} seconds.")
                        } else if (Skytils.config.necronPhaseTimer && dungeonFloorNumber == 7) {
                            add("§bMaxor took ${diff(phase1ClearTime!!, bossEntryTime!!)} seconds.")
                            add("§cStorm §btook ${diff(phase2ClearTime!!, phase1ClearTime!!)} seconds.")
                            add("§eTerminals §btook ${diff(terminalClearTime!!, phase2ClearTime!!)} seconds.")
                            add("§6Goldor §btook ${diff(phase3ClearTime!!, terminalClearTime!!)} seconds.")
                            add("§4Necron §btook ${diff(phase4ClearTime!!, phase3ClearTime!!)} seconds.")
                            if (DungeonFeatures.dungeonFloor == "M7") {
                                add("§7Wither King §btook ${diff(bossClearTime!!, phase4ClearTime!!)} seconds.")
                            }
                        }
                        if (Skytils.config.dungeonTimer) {
                            add("§bDungeon finished in ${diff(bossClearTime!!, dungeonStartTime!!)} seconds.")
                        }
                        if (isNotEmpty()) UChat.chat(joinToString("\n"))
                    }
                }
            }

            dungeonFloorNumber == 7 && (message.startsWith("§r§4[BOSS] ") || message.startsWith("§r§aThe Core entrance ")) -> {
                when {
                    message.endsWith("§r§cPathetic Maxor, just like expected.§r") && phase1ClearTime == null -> {
                        phase1ClearTimeState.set { Instant.now() }
                        if (Skytils.config.necronPhaseTimer) UChat.chat(
                            "§bMaxor took ${diff(phase1ClearTime!!, bossEntryTime!!)} seconds."
                        )
                    }

                    message.endsWith("§r§cWho dares trespass into my domain?§r") && phase2ClearTime == null -> {
                        phase2ClearTimeState.set { Instant.now() }
                        if (Skytils.config.necronPhaseTimer) UChat.chat(
                            "§cStorm §btook ${diff(phase2ClearTime!!, phase1ClearTime!!)} seconds."
                        )
                    }

                    message.endsWith(" is opening!§r") && terminalClearTime == null -> {
                        terminalClearTimeState.set { Instant.now() }
                        if (Skytils.config.necronPhaseTimer) UChat.chat(
                            "§eTerminals §btook ${diff(terminalClearTime!!, phase2ClearTime!!)} seconds."
                        )
                    }

                    message.endsWith("§r§cYou went further than any human before, congratulations.§r") && phase3ClearTime == null -> {
                        phase3ClearTimeState.set { Instant.now() }
                        if (Skytils.config.necronPhaseTimer) UChat.chat(
                            "§6Goldor §btook ${diff(phase3ClearTime!!, terminalClearTime!!)} seconds."
                        )
                    }

                    message.endsWith("§r§cAll this, for nothing...§r") -> {
                        phase4ClearTimeState.set { Instant.now() }
                        if (Skytils.config.necronPhaseTimer) UChat.chat(
                            "§4Necron §btook ${diff(phase4ClearTime!!, phase3ClearTime!!)} seconds."
                        )
                    }
                }
            }

            dungeonFloorNumber == 6 && message.startsWith("§r§c[BOSS] Sadan") -> {
                when {
                    (message.endsWith("§r§f: ENOUGH!§r") && terraClearTime == null) -> {
                        terraClearTimeState.set { Instant.now() }
                        if (Skytils.config.sadanPhaseTimer) UChat.chat(
                            "§dTerracotta §btook ${diff(terraClearTime!!, bossEntryTime!!)} seconds."
                        )
                    }

                    (message.endsWith("§r§f: You did it. I understand now, you have earned my respect.§r") && giantsClearTime == null) -> {
                        giantsClearTimeState.set { Instant.now() }
                        if (Skytils.config.sadanPhaseTimer) UChat.chat(
                            "§aGiants §btook ${diff(giantsClearTime!!, terraClearTime!!)} seconds."
                        )
                    }
                }
            }
        }
    }

    fun onWorldChange(event: WorldUnloadEvent) {
        dungeonStartTimeState.set { null }
        bloodOpenTimeState.set { null }
        bloodClearTimeState.set { null }
        bossEntryTimeState.set { null }
        bossClearTimeState.set { null }
        phase1ClearTimeState.set { null }
        phase2ClearTimeState.set { null }
        terminalClearTimeState.set { null }
        phase3ClearTimeState.set { null }
        phase4ClearTimeState.set { null }
        terraClearTimeState.set { null }
        giantsClearTimeState.set { null }
        witherDoorsState.set { 0 }
        scoreShownAtState.set { null }
    }

    object DungeonTimerHud: HudElement("Dungeon Timer", 200f, 80f) {
        override val toggleState: State<Boolean>
            get() = Skytils.config.dungeonTimerState

        private val timeElapsed = State {
            val start = dungeonStartTimeState()
            bossClearTimeState()?.let { bossClearTime ->
                dungeonTimeFormat(Duration.between(start, bossClearTime))
            } ?: scoreShownAtState()?.let { scoreShown ->
                dungeonTimeFormat(Duration.between(start, scoreShown))
            } ?: withSystemTime { time -> dungeonTimeFormat(Duration.between(start, time.getValue())) }
        }
        private val bloodOpen = State {
            bloodOpenTimeState()?.let { bloodOpenTime ->
                dungeonTimeFormat(Duration.between(dungeonStartTimeState(), bloodOpenTime))
            } ?: timeElapsed()
        }

        override fun LayoutScope.render() {
            if_(SBInfo.dungeonsState and State { dungeonStartTimeState() != Instant.MAX }) {
                column {
                    text({ "§aTime Elapsed: ${timeElapsed()}s" })
                    text({ "§7Wither Doors: ${witherDoorsState()}" })
                    text({ "§4Blood Open: ${bloodOpen()}s" })
                    ifNotNull(bloodOpenTimeState) {
                        text({ "§cWatcher Clear: ${createTimeTextState(bloodOpenTimeState, bloodClearTimeState, stateOf(null)).invoke()}" })
                    }
                    if_(State { bloodClearTimeState() != null } and State { !DungeonFeatures.dungeonFloorState().equals("E") }) {
                        text({ "§dPortal: ${createTimeTextState(bloodClearTimeState, bossEntryTimeState).invoke()}s" })
                    }
                    ifNotNull(bossEntryTimeState) {
                        text({ "§bBoss Clear: ${createTimeTextState(bossEntryTimeState, bossClearTimeState).invoke()}s" })
                    }
                }
            }
        }

        override fun LayoutScope.demoRender() {
            column {
                listOf(
                    "§aTime Elapsed: 0s",
                    "§7Wither Doors: 0",
                    "§4Blood Open: 0s",
                    "§cWatcher Clear: 0s",
                    "§dPortal: 0s",
                    "§9Boss Entry: 0s",
                    "§bBoss Clear: 0s"
                ).forEach { line ->
                    text(line)
                }
            }
        }

    }

    object NecronPhaseTimerHud : HudElement("Necron Phase Timer", 200f, 120f) {
        override val toggleState: State<Boolean>
            get() = Skytils.config.necronPhaseTimerState

        override fun LayoutScope.render() {
            if_(SBInfo.dungeonsState and State { bossEntryTimeState() != null} and State { dungeonFloorNumberState() == 7 }) {
                column {
                    text({ "§bMaxor: ${createTimeTextState(bossEntryTimeState, phase1ClearTimeState).invoke()}" })
                    ifNotNull(phase1ClearTimeState) {
                        text({ "§cStorm: ${createTimeTextState(phase1ClearTimeState, phase2ClearTimeState).invoke()}" })
                    }
                    ifNotNull(phase2ClearTimeState) {
                        text({ "§eTerminals: ${createTimeTextState(phase2ClearTimeState, terminalClearTimeState).invoke()}" })
                    }
                    ifNotNull(terminalClearTimeState) {
                        text({ "§6Goldor: ${createTimeTextState(terminalClearTimeState, phase3ClearTimeState).invoke()}" })
                    }
                    ifNotNull(phase3ClearTimeState) {
                        text({ "§4Necron: ${createTimeTextState(phase3ClearTimeState, phase4ClearTimeState).invoke()}" })
                    }
                    if_(State { phase4ClearTimeState() != null } and State { DungeonFeatures.dungeonFloorState().equals("M7") }) {
                        text({ "§7Wither King: ${createTimeTextState(phase4ClearTimeState, bossClearTimeState).invoke()}" })
                    }
                }
            }
        }

        override fun LayoutScope.demoRender() {
            column {
                listOf(
                    "§bMaxor: 0s",
                    "§cStorm: 0s",
                    "§eTerminals: 0s",
                    "§6Goldor: 0s",
                    "§4Necron: 0s",
                    "§7Wither King: 0s"
                ).forEach { line ->
                    text(line)
                }
            }
        }

    }

    object SadanPhaseTimer : HudElement("Sadan Phase Timer", 200f, 120f) {
        override val toggleState: State<Boolean>
            get() = Skytils.config.sadanPhaseTimerState

        override fun LayoutScope.render() {
            if_(SBInfo.dungeonsState and State { bossEntryTimeState() != null && dungeonFloorNumberState() == 6 }) {
                column {
                    text({ "§dTerracotta: ${createTimeTextState(bossEntryTimeState, terraClearTimeState).invoke()}" })
                    ifNotNull(terraClearTimeState) {
                        text({ "§aGiants: ${createTimeTextState(terraClearTimeState, giantsClearTimeState).invoke()}" })
                    }
                    ifNotNull(giantsClearTimeState) {
                        text({ "§cSadan: ${createTimeTextState(giantsClearTimeState, bossClearTimeState).invoke()}" })
                    }
                }
            }
        }

        override fun LayoutScope.demoRender() {
            column {
                listOf(
                    "§dTerracotta: 0s",
                    "§aGiants: 0s",
                    "§cSadan: 0s"
                ).forEach { line ->
                    text(line)
                }
            }
        }

    }
}

private fun dungeonTimeFormat(value: Duration, useMinutes: Boolean = true): String =
    (if (useMinutes) (value.toMinutesPart().takeIf { it > 0 }?.let { "${it}m" } ?: "") +
            value.toSecondsPart() else value.toSeconds().toString()) +
            if (Skytils.config.showMillisOnDungeonTimer) ".%2d".format(value.toMillisPart()) else ""

private fun createTimeTextState(start: State<Instant?>, end: State<Instant?>, fallback: State<Instant?> = DungeonTimer.scoreShownAtState) =
    State {
        val start = start()
        end()?.let { end ->
            dungeonTimeFormat(Duration.between(start, end))
        } ?: fallback()?.let { end ->
            dungeonTimeFormat(Duration.between(start, end))
        } ?: withSystemTime { instant -> dungeonTimeFormat(Duration.between(start, instant.getValue())) }
    }

private fun diff(end: Long, start: Long): Any {
    val sec = ((end - start) / 1000.0)
    return if (!Skytils.config.showMillisOnDungeonTimer) sec.roundToInt() else NumberUtil.nf.format(
        sec.roundToPrecision(
            2
        )
    )
}

private fun diff(end: Instant, start: Instant) =
    dungeonTimeFormat(Duration.between(start, end), useMinutes = false)
