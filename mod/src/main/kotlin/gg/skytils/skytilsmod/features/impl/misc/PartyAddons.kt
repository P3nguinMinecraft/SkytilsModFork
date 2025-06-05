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
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.play.ChatMessageReceivedEvent
import gg.skytils.event.impl.play.ChatMessageSentEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.utils.Utils
import gg.skytils.skytilsmod.utils.formattedText
import gg.skytils.skytilsmod.utils.printDevMessage
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.MutableText
import net.minecraft.text.Style
import net.minecraft.text.Text

/**
 * Inspired by https://www.chattriggers.com/modules/v/HypixelUtilities
 */
object PartyAddons : EventSubscriber {
    private val partyStartPattern = Regex("^§6Party Members \\((\\d+)\\)§r$")
    private val playerPattern = Regex("(?<rank>§r§.(?:\\[.+?] )?)(?<name>\\w+) ?§r(?<status>§a|§c) ?● ?")
    private val party = mutableListOf<PartyMember>()
    private val partyCommands = setOf("/pl", "/party list", "/p list", "/party l")

    //0 = not awaiting, 1 = awaiting 2nd delimiter, 2 = awaiting 1st delimiter
    private var awaitingDelimiter = 0

    override fun setup() {
        register(::onCommandRun)
        register(::onChat)
    }

    fun onCommandRun(event: ChatMessageSentEvent) {
        if (!Utils.isOnHypixel || !Skytils.config.partyAddons) return
        if (event.message in partyCommands) {
            awaitingDelimiter = 2
        }
    }

    fun onChat(event: ChatMessageReceivedEvent) {
        if (!Utils.isOnHypixel || !Skytils.config.partyAddons) return
        val message = event.message.formattedText

        if (message == "§f§r" && awaitingDelimiter != 0) {
            event.cancelled = true
        } else if (partyStartPattern.matches(message)) {
            party.clear()
            event.cancelled = true
        } else if (message.startsWith("§eParty ")) {
            val playerType = when {
                message.startsWith("§eParty Leader: ") -> PartyMemberType.LEADER
                message.startsWith("§eParty Moderators: ") -> PartyMemberType.MODERATOR
                message.startsWith("§eParty Members: ") -> PartyMemberType.MEMBER
                else -> return
            }
            playerPattern.findAll(message.substringAfter(": ")).forEach {
                it.destructured.let { (rank, name, status) ->
                    printDevMessage({ "Found Party Member: rank=$rank, name=$name, status=$status" }, "PartyAddons")
                    party.add(
                        PartyMember(
                            name,
                            playerType,
                            status,
                            rank
                        )
                    )
                }
            }
            event.cancelled = true
        } else if (message.startsWith("§cYou are not currently in a party.") && awaitingDelimiter != 0) {
            party.clear()
        } else if (event.message.string.startsWith("-----") && awaitingDelimiter != 0) {
            awaitingDelimiter--
            if (awaitingDelimiter == 1 || party.isEmpty()) return

            val component = Text.literal("§aParty members (${party.size})\n")

            val self = party.first { it.name == mc.player?.name?.string }

            if (self.type == PartyMemberType.LEADER) {
                component.append(
                    createButton(
                        "§9[Warp] ",
                        "/p warp",
                        "§9Click to warp the party."
                    )
                ).append(
                    createButton(
                        "§e[All Invite] ",
                        "/p settings allinvite",
                        "§eClick to toggle all invite."
                    )
                ).append(
                    createButton(
                        "§6[Mute]\n",
                        "/p mute",
                        "§6Click to toggle mute."
                    )
                ).append(
                    createButton(
                        "§c[Kick Offline] ",
                        "/p kickoffline",
                        "§cClick to kick offline members."
                    )
                ).append(
                    createButton(
                        "§4[Disband]\n",
                        "/p disband",
                        "§4Click to disband the party."
                    )
                )
            }

            val partyLeader = party.first { it.type == PartyMemberType.LEADER }
            component.append(
                "\n${partyLeader.status}➡§r ${partyLeader.rank}${partyLeader.name}"
            )

            val mods = party.filter { it.type == PartyMemberType.MODERATOR }
            if (mods.isNotEmpty()) {
                component.append("\n§eMods")
                mods.forEach {
                    component.append(
                        "\n${it.status}➡§r ${it.rank}${it.name} "
                    )
                    if (self.type != PartyMemberType.LEADER) return@forEach
                    component.append(
                        createButton(
                            "§a[⋀] ",
                            "/p promote ${it.name}",
                            "§aPromote ${it.name}"
                        )
                    ).append(
                        createButton(
                            "§c[⋁] ",
                            "/p demote ${it.name}",
                            "§cDemote ${it.name}"
                        )
                    ).append(
                        createButton(
                            "§4[✖]",
                            "/p kick ${it.name}",
                            "§4Kick ${it.name}"
                        )
                    )
                }
            }

            val members = party.filter { it.type == PartyMemberType.MEMBER }
            if (members.isNotEmpty()) {
                component.append("\n§eMembers")
                members.forEach {
                    component.append(
                        "\n${it.status}➡§r ${it.rank}${it.name} "
                    )
                    if (self.type != PartyMemberType.LEADER) return@forEach
                    component.append(
                        createButton(
                            "§9[⋀] ",
                            "/p transfer ${it.name}",
                            "§9Transfer ${it.name}"
                        )
                    ).append(
                        createButton(
                            "§a[⋀] ",
                            "/p promote ${it.name}",
                            "§aPromote ${it.name}"
                        )
                    ).append(
                        createButton(
                            "§4[✖]",
                            "/p kick ${it.name}",
                            "§4Kick ${it.name}"
                        )
                    )
                }
            }
            UChat.chat(component)
        }
    }

    private fun createButton(text: String, command: String, hoverText: String): MutableText {
        return Text.literal(text)
            .setStyle(
                Style.EMPTY.withClickEvent(ClickEvent.RunCommand(command)).withHoverEvent(HoverEvent.ShowText(Text.literal(hoverText)))
            )
    }

    private data class PartyMember(
        val name: String,
        val type: PartyMemberType,
        val status: String,
        val rank: String
    )

    private enum class PartyMemberType {
        LEADER,
        MODERATOR,
        MEMBER
    }
}
