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
import gg.essential.elementa.layoutdsl.column
import gg.essential.elementa.state.v2.MutableState
import gg.essential.elementa.state.v2.combinators.and
import gg.essential.elementa.state.v2.mutableStateOf
import gg.essential.universal.UChat
import gg.essential.universal.UDesktop
import gg.skytils.event.EventSubscriber
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod._event.PacketReceiveEvent
import gg.skytils.skytilsmod.core.SoundQueue
import gg.skytils.skytilsmod.core.structure.v2.HudElement
import gg.skytils.skytilsmod.features.impl.events.GriffinBurrows
import gg.skytils.skytilsmod.features.impl.handlers.AuctionData
import gg.skytils.skytilsmod.features.impl.trackers.Tracker
import gg.skytils.skytilsmod.gui.layout.text
import gg.skytils.skytilsmod.utils.*
import gg.skytils.skytilsmod.utils.NumberUtil.nf
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket
import java.io.Reader
import java.io.Writer
import java.util.*
import kotlin.math.pow
import kotlinx.serialization.Serializable
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Style
import net.minecraft.text.Text
import java.awt.Color
import kotlin.jvm.optionals.getOrDefault
import kotlin.jvm.optionals.getOrNull

object MythologicalTracker : EventSubscriber, Tracker("mythological") {

    private val rareDugDrop = Regex("^RARE DROP! You dug out a (.+)!$")
    private val mythCreatureDug = Regex("^(?:Oi|Uh oh|Yikes|Woah|Oh|Danger|Good Grief)! You dug out (?:a )?(.+)!$")

    private val seenUUIDs = WeakHashMap<String, Boolean>().asSet

    val burrowsDugState = mutableStateOf(0L)

    init {
        Skytils.guiManager.registerElement(MythologicalTrackerHud)
    }

    @Suppress("UNUSED")
    enum class BurrowDrop(
        val itemId: String,
        val itemName: String,
        val rarity: ItemRarity,
        val isChat: Boolean = false,
        val mobDrop: Boolean = false,
        var droppedTimes: MutableState<Long> = mutableStateOf(0L)
    ) {
        REMEDIES("ANTIQUE_REMEDIES", "Antique Remedies", ItemRarity.EPIC),

        // this does have a chat message but it's just Enchanted Book
        CHIMERA("ENCHANTED_BOOK-ULTIMATE_CHIMERA-1", "Chimera", ItemRarity.COMMON),
        COINS("COINS", "Coins", ItemRarity.LEGENDARY, isChat = true),
        PLUSHIE("CROCHET_TIGER_PLUSHIE", "Crochet Tiger Plushie", ItemRarity.EPIC),
        COG("CROWN_OF_GREED", "Crown of Greed", ItemRarity.LEGENDARY, true),
        STICK("DAEDALUS_STICK", "Daedalus Stick", ItemRarity.LEGENDARY, mobDrop = true),
        SHELMET("DWARF_TURTLE_SHELMET", "Dwarf Turtle Shelmet", ItemRarity.RARE),
        FEATHER("GRIFFIN_FEATHER", "Griffin Feather", ItemRarity.RARE, isChat = true),
        RELIC("MINOS_RELIC", "Minos Relic", ItemRarity.EPIC),
        WASHED("WASHED_UP_SOUVENIR", "Washed-up Souvenir", ItemRarity.LEGENDARY, true);

        companion object {
            fun getFromId(id: String?): BurrowDrop? {
                return entries.find { it.itemId == id }
            }

            fun getFromName(name: String?): BurrowDrop? {
                return entries.find { it.itemName == name }
            }
        }
    }

    @Suppress("UNUSED")
    enum class BurrowMob(
        val mobName: String,
        val mobId: String,
        val plural: Boolean = false,
        var dugTimes: MutableState<Long> = mutableStateOf(0L)
    ) {

        GAIA("Gaia Construct", "GAIA_CONSTRUCT"),
        CHAMP("Minos Champion", "MINOS_CHAMPION"),
        HUNTER("Minos Hunter", "MINOS_HUNTER"),
        INQUIS("Minos Inquisitor", "MINOS_INQUISITOR"),
        MINO("Minotaur", "MINOTAUR"),
        LYNX("Siamese Lynxes", "SIAMESE_LYNXES", plural = true);

        companion object {
            fun getFromId(id: String?): BurrowMob? {
                return entries.find { it.mobId == id }
            }

            fun getFromName(name: String?): BurrowMob? {
                return entries.find { it.mobName == name }
            }
        }
    }

    override fun setup() {
        register(::onReceivePacket)
    }

    fun onReceivePacket(event: PacketReceiveEvent<*>) {
        if (!Utils.inSkyblock || (!Skytils.config.trackMythEvent && !Skytils.config.broadcastMythCreatureDrop)) return
        when (event.packet) {
            is ChatMessageS2CPacket -> {
                if (!Skytils.config.trackMythEvent) return
                val unformatted = event.packet.unsignedContent?.string?.stripControlCodes() ?: return
                if (unformatted.startsWith("RARE DROP! You dug out a ")) {
                    rareDugDrop.matchEntire(unformatted)?.let {
                        (BurrowDrop.getFromName(it.groups[1]?.value ?: return) ?: return).droppedTimes.set { it + 1 }
                        markDirty<MythologicalTracker>()
                    }
                } else if (unformatted.startsWith("Wow! You dug out ") && unformatted.endsWith(
                        " coins!"
                    )
                ) {
                    BurrowDrop.COINS.droppedTimes.set { it + unformatted.replace(Regex("[^\\d]"), "").toLong() }
                } else if (unformatted.contains("! You dug out ")) {
                    mythCreatureDug.matchEntire(unformatted)?.let { matchResult ->
                        val mob = BurrowMob.getFromName(matchResult.groups[1]?.value ?: return) ?: return
                        mob.dugTimes.set { it + 1 }
                        markDirty<MythologicalTracker>()
                    }
                } else if (unformatted.endsWith("/4)") && (unformatted.startsWith("You dug out a Griffin Burrow! (") || unformatted.startsWith(
                        "You finished the Griffin burrow chain! (4"
                    ))
                ) {
                    burrowsDugState.set { it + 1 }
                    markDirty<MythologicalTracker>()
                } else if (unformatted.startsWith("RARE DROP! ")) {
                    for (drop in BurrowDrop.entries) {
                        if (!drop.mobDrop) continue
                        if (unformatted.startsWith("RARE DROP! ${drop.itemName}")) {
                            drop.droppedTimes.set { it + 1 }
                            markDirty<MythologicalTracker>()
                            break
                        }
                    }
                }
            }

            is ScreenHandlerSlotUpdateS2CPacket -> {
                val item = event.packet.stack ?: return
                val player = mc.player ?: return
                if (event.packet.syncId != 0 || player.age <= 1 || mc.player?.currentScreenHandler != mc.player?.playerScreenHandler) return
                val drop = BurrowDrop.getFromId(AuctionData.getIdentifier(item)) ?: return
                if (drop.isChat || drop.mobDrop) return
                val extraAttr = ItemUtil.getExtraAttributes(item) ?: return
                if (!extraAttr.contains("timestamp")) return
                if (!seenUUIDs.add(extraAttr.getString("uuid").getOrNull())) return
                val time = extraAttr.getLong("timestamp").getOrDefault(0)
                if (System.currentTimeMillis() - time > 6000) return
                if (Skytils.config.broadcastMythCreatureDrop) {
                    val text = "§6§lRARE DROP! ${drop.rarity.baseColor}${drop.itemName} §b(Skytils User Luck!)"
                    if (Skytils.config.autoCopyRNGDrops) UDesktop.setClipboardString(text.stripControlCodes())
                    Text.literal(text)
                        .setStyle(
                            Style.EMPTY.withClickEvent(ClickEvent.RunCommand("/skytilscopy ${text.stripControlCodes()}"))
                                .withHoverEvent(HoverEvent.ShowText(Text.literal("§aClick to copy to clipboard.")))
                        ).run(UChat::chat)
                    SoundQueue.addToQueue(
                        SoundQueue.QueuedSound(
                            "note.pling",
                            2.0.pow(-9.0 / 12).toFloat(),
                            volume = 0.5f
                        )
                    )
                    SoundQueue.addToQueue(
                        SoundQueue.QueuedSound(
                            "note.pling",
                            2.0.pow(-2.0 / 12).toFloat(),
                            ticks = 4,
                            volume = 0.5f
                        )
                    )
                    SoundQueue.addToQueue(
                        SoundQueue.QueuedSound(
                            "note.pling",
                            2.0.pow(1.0 / 12).toFloat(),
                            ticks = 8,
                            volume = 0.5f
                        )
                    )
                    SoundQueue.addToQueue(
                        SoundQueue.QueuedSound(
                            "note.pling",
                            2.0.pow(3.0 / 12).toFloat(),
                            ticks = 12,
                            volume = 0.5f
                        )
                    )
                }
                if (Skytils.config.trackMythEvent) {
                    drop.droppedTimes.set { it + 1 }
                    markDirty<MythologicalTracker>()
                }
            }
        }
    }

    override fun resetLoot() {
        burrowsDugState.set { 0L }
        BurrowDrop.entries.forEach { it.droppedTimes.set(0L) }
        BurrowMob.entries.forEach { it.dugTimes.set(0L) }
    }

    // TODO: 5/3/2022 fix this
    @Serializable
    data class TrackerSave(
        @SerialName("dug")
        val burrowsDug: Long,
        @SerialName("items")
        val drops: Map<String, Long>,
        val mobs: Map<String, Long>
    )

    override fun read(reader: Reader) {
        val save = json.decodeFromString<TrackerSave>(reader.readText())
        burrowsDugState.set { save.burrowsDug }
        BurrowDrop.entries.forEach { it.droppedTimes.set(save.drops[it.itemId] ?: 0L) }
        BurrowMob.entries.forEach { it.dugTimes.set(save.mobs[it.mobId] ?: 0L) }
    }

    override fun write(writer: Writer) {
        writer.write(
            json.encodeToString(
                TrackerSave(
                    burrowsDugState.getUntracked(),
                    BurrowDrop.entries.associate { it.itemId to it.droppedTimes.getUntracked() },
                    BurrowMob.entries.associate { it.mobId to it.dugTimes.getUntracked() }
                )
            )
        )
    }

    override fun setDefault(writer: Writer) {
        write(writer)
    }

    object MythologicalTrackerHud : HudElement("Mythological Tracker", 150f, 120f) {
        override fun LayoutScope.render() {
            if_(SBInfo.skyblockState and GriffinBurrows.hasSpadeInHotbarState and { SBInfo.modeState() == SkyblockIsland.Hub.mode }) {
                column {
                    text({ "Burrows Dug§f: ${nf.format(burrowsDugState())}" }, Modifier.color(Color.YELLOW))
                    BurrowMob.entries.forEach { mob ->
                        if_({ mob.dugTimes() == 0L }) {
                            text({ "${mob.mobName}§f: ${nf.format(mob.dugTimes())}" }, Modifier.color(Color.CYAN))
                        }
                    }
                    BurrowDrop.entries.forEach { item ->
                        if_({ item.droppedTimes() == 0L }) {
                            text({ "${item.rarity.baseColor}${item.itemName}§f: §r${nf.format(item.droppedTimes())}" }, Modifier.color(Color.CYAN))
                        }
                    }
                }
            }
        }

        override fun LayoutScope.demoRender() {
            column {
                text("Burrows Dug§f: 1000", Modifier.color(Color.YELLOW))
                BurrowMob.entries.forEach { mob ->
                    text("${mob.mobName}§f: 100", Modifier.color(Color.CYAN))
                }
                BurrowDrop.entries.forEach { item ->
                    text("${item.rarity.baseColor}${item.itemName}§f: §r100", Modifier.color(Color.CYAN))
                }
            }
        }

    }

}