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
package gg.skytils.skytilsmod.features.impl.misc

import gg.essential.universal.UChat
import gg.skytils.event.EventPriority
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.play.ChatMessageReceivedEvent
import gg.skytils.event.impl.play.ChatMessageSentEvent
import gg.skytils.event.impl.screen.GuiContainerPreDrawSlotEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.failPrefix
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.Skytils.prefix
import gg.skytils.skytilsmod._event.PacketSendEvent
import gg.skytils.skytilsmod.utils.ItemUtil.getItemLore
import gg.skytils.skytilsmod.utils.ItemUtil.getSkyBlockItemID
import gg.skytils.skytilsmod.utils.RenderUtil.highlight
import gg.skytils.skytilsmod.utils.SBInfo
import gg.skytils.skytilsmod.utils.Utils
import gg.skytils.skytilsmod.utils.setHoverText
import gg.skytils.skytilsmod.utils.stripControlCodes
import gg.essential.universal.UMatrixStack
import gg.skytils.skytilsmod.utils.formattedText
import gg.skytils.skytilsmod.utils.setClick
import net.minecraft.text.ClickEvent
import net.minecraft.screen.GenericContainerScreenHandler
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.text.Text

object PetFeatures : EventSubscriber {
    val petItems = HashMap<String, Boolean>()
    private val SUMMON_PATTERN = Regex("§r§aYou summoned your §r(?<pet>.+)§r§a!§r")
    private val AUTOPET_PATTERN =
        Regex("§cAutopet §eequipped your §7\\[Lvl (?<level>\\d+)] (?<pet>.+)§e! §a§lVIEW RULE§r")
    private var lastPetConfirmation: Long = 0
    private var lastPetLockNotif: Long = 0
    var lastPet: String? = null

    override fun setup() {
        register(::onChat, EventPriority.Highest)
        register(::onDraw, EventPriority.Low)
        register(::onSendPacket)
        register(::onSendChatMessage)
    }

    fun onChat(event: ChatMessageReceivedEvent) {
        if (!Utils.inSkyblock) return
        val message = event.message.formattedText
        if (message.startsWith("§r§aYou despawned your §r§")) {
            lastPet = null
        } else if (message.startsWith("§r§aYou summoned your §r")) {
            SUMMON_PATTERN.find(message)?.groups?.get("pet")?.value?.stripControlCodes().let {
                if (it == null) UChat.chat("$failPrefix §cSkytils failed to capture equipped pet.")
                else lastPet = it
            }
        } else if (message.startsWith("§cAutopet §eequipped your §7[Lvl ")) {
            AUTOPET_PATTERN.find(message)?.groups?.get("pet")?.value?.stripControlCodes().let {
                if (it == null) UChat.chat("$failPrefix §cSkytils failed to capture equipped pet.")
                else lastPet = it
            }
        }
    }

    fun onDraw(event: GuiContainerPreDrawSlotEvent) {
        if (!Utils.inSkyblock || event.container !is GenericContainerScreenHandler) return
        if (Skytils.config.highlightActivePet && (SBInfo.lastOpenContainerName?.startsWith("Pets") == true) && event.slot.hasStack() && event.slot.id in 10..43) {
            val item = event.slot.stack
            if (getItemLore(item).any { line -> line.startsWith("§7§cClick to despawn") }) {
                val matrixStack = UMatrixStack.Compat.get()
                matrixStack.push()
                matrixStack.translate(0f, 0f, 3f)
                matrixStack.runWithGlobalState {
                    event.slot highlight Skytils.config.activePetColor
                }
                matrixStack.pop()
            }
        }
    }

    fun onSendPacket(event: PacketSendEvent<*>) {
        if (!Utils.inSkyblock) return
        if (Skytils.config.petItemConfirmation && (event.packet is PlayerInteractEntityC2SPacket || event.packet is PlayerInteractBlockC2SPacket)) {
            val item = mc.player?.mainHandStack ?: return
            val itemId = getSkyBlockItemID(item) ?: return
            if (itemId !in petItems) {
                val isPetItem =
                    (itemId.contains("PET_ITEM") && !itemId.endsWith("_DROP")) || itemId.endsWith("CARROT_CANDY") || itemId.startsWith(
                        "PET_SKIN_"
                    ) || getItemLore(item).asReversed().any {
                        it.contains("PET ITEM")
                    }
                petItems[itemId] = isPetItem
            }
            if (petItems[itemId] == true) {
                if (System.currentTimeMillis() - lastPetConfirmation > 5000) {
                    event.cancelled = true
                    if (System.currentTimeMillis() - lastPetLockNotif > 10000) {
                        lastPetLockNotif = System.currentTimeMillis()
                        UChat.chat(
                            Text.literal("$prefix §cSkytils stopped you from using that pet item! §6Click this message to disable the lock.").setHoverText(
                                "Click to disable the pet item lock for 5 seconds."
                            ).setClick(ClickEvent.RunCommand("/disableskytilspetitemlock"))
                        )
                    }
                } else {
                    lastPetConfirmation = 0
                }
            }
        }
    }

    fun onSendChatMessage(event: ChatMessageSentEvent) {
        if (event.message == "/disableskytilspetitemlock" && !event.addToHistory) {
            lastPetConfirmation = System.currentTimeMillis()
            UChat.chat("$prefix §aYou may now apply pet items for 5 seconds.")
            event.cancelled = true
        }
    }
}