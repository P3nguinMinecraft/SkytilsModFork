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

package gg.skytils.skytilsmod.features.impl.mining

import com.mojang.blaze3d.opengl.GlStateManager
import gg.essential.universal.UChat
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.skytils.event.EventPriority
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.TickEvent
import gg.skytils.event.impl.play.ChatMessageReceivedEvent
import gg.skytils.event.impl.play.WorldUnloadEvent
import gg.skytils.event.impl.render.LivingEntityPreRenderEvent
import gg.skytils.event.impl.render.WorldDrawEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.Skytils.prefix
import gg.skytils.skytilsmod._event.LocationChangeEvent
import gg.skytils.skytilsmod._event.PacketReceiveEvent
import gg.skytils.skytilsmod.core.tickTimer
import gg.skytils.skytilsmod.features.impl.handlers.MayorInfo
import gg.skytils.skytilsmod.utils.*
import gg.skytils.skytilsmod.utils.graphics.colors.ColorFactory
import gg.skytils.skytilsws.client.WSClient
import gg.skytils.skytilsws.shared.packet.C2SPacketCHWaypoint
import gg.skytils.skytilsws.shared.packet.C2SPacketCHWaypointsSubscribe
import gg.skytils.skytilsws.shared.structs.CHWaypointType
import kotlinx.coroutines.launch
import net.minecraft.client.network.OtherClientPlayerEntity
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIImage
import gg.essential.elementa.layoutdsl.LayoutScope
import gg.essential.elementa.layoutdsl.Modifier
import gg.essential.elementa.layoutdsl.box
import gg.essential.elementa.layoutdsl.fillParent
import gg.essential.elementa.layoutdsl.height
import gg.essential.elementa.layoutdsl.width
import gg.essential.universal.UMinecraft
import gg.essential.universal.vertex.UBufferBuilder
import gg.skytils.skytilsmod.core.structure.v2.HudElement
import gg.skytils.skytilsmod.utils.rendering.DrawHelper.writeRectCoords
import gg.skytils.skytilsmod.utils.rendering.SRenderPipelines
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import java.awt.Color
import kotlin.jvm.optionals.getOrNull

object CHWaypoints : EventSubscriber {
    var lastTPLoc: BlockPos? = null
    var waypoints = hashMapOf<String, BlockPos>()
    var waypointDelayTicks = 0
    private val SBE_DSM_PATTERN =
        Regex("\\\$(?:SBECHWP\\b|DSMCHWP):(?<stringLocation>.*?)@-(?<x>-?\\d+),(?<y>-?\\d+),(?<z>-?\\d+)")
    private val xyzPattern =
        Regex(".*?(?<user>[a-zA-Z0-9_]{3,16}):.*?(?<x>[0-9]{1,3}),? (?:y: )?(?<y>[0-9]{1,3}),? (?:z: )?(?<z>[0-9]{1,3}).*?")
    private val xzPattern =
        Regex(".*(?<user>[a-zA-Z0-9_]{3,16}):.* (?<x>[0-9]{1,3}),? (?<z>[0-9]{1,3}).*")
    val chWaypointsList = hashMapOf<String, CHInstance>()
    class CHInstance {
        val createTime = System.currentTimeMillis()
        val waypoints = hashMapOf<CHWaypointType, BlockPos>()
    }

    init {
        tickTimer(20 * 60 * 5) {
            chWaypointsList.entries.removeAll {
                System.currentTimeMillis() > it.value.createTime + 1000 * 60 * 5
            }
        }
    }

    override fun setup() {
        register(::onLocationChange)
        register(::onReceivePacket)
        register(::onChat, EventPriority.Highest)
        register(::onRenderWorld)
        register(::onRenderLivingPre)
        register(::onTick, EventPriority.Low) // priority low so it always runs after sbinfo is updated
        register(::onWorldChange, EventPriority.Highest)
    }

    fun onLocationChange(event: LocationChangeEvent) {
        if (event.packet.mode.getOrNull() == SkyblockIsland.CrystalHollows.mode) {
            val instance = chWaypointsList[event.packet.serverName]
            if (instance != null) {
                for ((type, position) in instance.waypoints) {
                    val loc = CrystalHollowsMap.Locations.entries.find { it.packetType == type } ?: continue

                    val locationObject = loc.loc
                    locationObject.locX = position.x.toDouble()
                    locationObject.locY = position.y.toDouble()
                    locationObject.locZ = position.z.toDouble()
                }
            }
            WSClient.sendPacketAsync(C2SPacketCHWaypointsSubscribe(event.packet.serverName))
        }
    }

    fun onReceivePacket(event: PacketReceiveEvent<*>) {
        if (!Utils.inSkyblock) return
        if (Skytils.config.crystalHollowDeathWaypoint && event.packet is PlayerPositionLookS2CPacket && mc.player != null) {
            lastTPLoc = mc.player!!.blockPos
        }
    }

    fun onChat(event: ChatMessageReceivedEvent) {
        if (!Utils.inSkyblock) return
        val unformatted = event.message.string.stripControlCodes()
        if (Skytils.config.hollowChatCoords && SBInfo.mode == SkyblockIsland.CrystalHollows.mode) {
            xyzPattern.find(unformatted)?.groups?.let {
                waypointChatMessage(it["x"]!!.value, it["y"]!!.value, it["z"]!!.value)
                return
            }
            xzPattern.find(unformatted)?.groups?.let {
                waypointChatMessage(it["x"]!!.value, "100", it["z"]!!.value)
                return
            }

            /**
             * Checks for the format used in DSM and SBE
             * $DSMCHWP:Mines of Divan@-673,117,426 ✔
             * $SBECHWP:Khazad-dûm@-292,63,281 ✔
             * $asdf:Khazad-dûm@-292,63,281 ❌
             * $SBECHWP:Khazad-dûm@asdf,asdf,asdf ❌
             */
            val cleaned = SBE_DSM_PATTERN.find(unformatted)
            if (cleaned != null) {
                val stringLocation = cleaned.groups["stringLocation"]!!.value
                val x = cleaned.groups["x"]!!.value
                val y = cleaned.groups["y"]!!.value
                val z = cleaned.groups["z"]!!.value
                CrystalHollowsMap.Locations.entries.find { it.cleanName == stringLocation }
                    ?.takeIf { !it.loc.exists() }?.let { loc ->
                        /**
                         * Sends the waypoints message except it suggests which one should be used based on
                         * the name contained in the message and converts it to the internally used names for the waypoints.
                         */
                        val text = Text.literal("§3Skytils > §eFound coordinates in a chat message, click a button to set a waypoint.\n")
                            .append(
                                Text.literal("§f${loc.displayName} ")
                                    .setStyle(
                                        Style.EMPTY.withClickEvent(ClickEvent.RunCommand("/skytilshollowwaypoint set $x $y $z ${loc.id}"))
                                            .withHoverEvent(HoverEvent.ShowText(Text.of("§eSet waypoint for ${loc.displayName}")))
                                    )
                            ).append(
                                Text.literal("§e[Custom]")
                                    .setStyle(
                                        Style.EMPTY.withClickEvent(ClickEvent.SuggestCommand("/skytilshollowwaypoint set $x $y $z n"))
                                            .withHoverEvent(HoverEvent.ShowText(Text.of("§eSet custom waypoint")))
                                    )
                            )
                        UMinecraft.getChatGUI()!!.addMessage(text)
                    }
            }
        }
        if ((Skytils.config.crystalHollowWaypoints || Skytils.config.crystalHollowMapPlaces) && Skytils.config.kingYolkarWaypoint && SBInfo.mode == SkyblockIsland.CrystalHollows.mode
            && mc.player != null && unformatted.startsWith("[NPC] King Yolkar:")
        ) {
            val yolkar = CrystalHollowsMap.Locations.KingYolkar

            val nametag = mc.world!!.entities.find { it is ArmorStandEntity && it.customName?.formattedText == "§6King Yolkar" }

            if (nametag != null) {
                val shifted = nametag.pos.subtract(200.0, 0.0, 200.0)

                val shouldSendThroughWS =
                        yolkar.loc.locX != shifted.x ||
                        yolkar.loc.locY != shifted.y ||
                        yolkar.loc.locZ != shifted.z

                yolkar.loc.setExact(shifted.x, shifted.y, shifted.z)

                if (shouldSendThroughWS) {
                    yolkar.sendThroughWS()
                }
            }
        }
        if (unformatted.startsWith("You died") || unformatted.startsWith("☠ You were killed")) {
            waypointDelayTicks =
                50 //this is to make sure the scoreboard has time to update and nothing moves halfway across the map
            if (Skytils.config.crystalHollowDeathWaypoint && SBInfo.mode == SkyblockIsland.CrystalHollows.mode && lastTPLoc != null) {
                UChat.chat(
                    Text.literal("$prefix §eClick to set a death waypoint at ${lastTPLoc!!.x} ${lastTPLoc!!.y} ${lastTPLoc!!.z}")
                        .setStyle(
                            Style.EMPTY.withClickEvent(ClickEvent.RunCommand("/sthw set ${lastTPLoc!!.x} ${lastTPLoc!!.y} ${lastTPLoc!!.z} Last Death"))
                        )
                )
            }
        } else if (unformatted.startsWith("Warp")) {
            waypointDelayTicks = 50
        }
    }

    private fun waypointChatMessage(x: String, y: String, z: String) {
        val message = Text.literal(
            "$prefix §eFound coordinates in a chat message, click a button to set a waypoint.\n"
        )
        for (loc in CrystalHollowsMap.Locations.entries) {
            if (loc.loc.exists()) continue
            message.append(
                Text.literal("${loc.displayName.substring(0, 2)}[${loc.displayName}] ")
                    .setStyle(
                        Style.EMPTY.withClickEvent(ClickEvent.RunCommand("/sthw set $x $y $z ${loc.id}"))
                            .withHoverEvent(HoverEvent.ShowText(Text.literal("§eSet waypoint for ${loc.cleanName}")))
                    )
            )
        }
        message.append(
            Text.literal("§e[Custom]")
                .setStyle(
                    Style.EMPTY.withClickEvent(ClickEvent.SuggestCommand("/sthw set $x $y $z Name"))
                        .withHoverEvent(HoverEvent.ShowText(Text.literal("§eSet waypoint for custom location")))
                )
        )
        UMinecraft.getChatGUI()!!.addMessage(message)
    }

    fun onRenderWorld(event: WorldDrawEvent) {
        if (!Utils.inSkyblock) return
        val matrixStack = UMatrixStack()
        if (Skytils.config.crystalHollowWaypoints && SBInfo.mode == SkyblockIsland.CrystalHollows.mode) {
            GlStateManager._disableDepthTest()
            for (loc in CrystalHollowsMap.Locations.entries) {
                loc.loc.drawWaypoint(loc.cleanName, event.partialTicks, matrixStack)
            }
            RenderUtil.renderWaypointText("Crystal Nucleus", 513.5, 107.0, 513.5, event.partialTicks, matrixStack)
            for ((key, value) in waypoints)
                RenderUtil.renderWaypointText(key, value, event.partialTicks, matrixStack)
            GlStateManager._enableDepthTest()
        }
    }

    fun onRenderLivingPre(event: LivingEntityPreRenderEvent<*, *, *>) {
        if (!Utils.inSkyblock) return
        if (Skytils.config.crystalHollowWaypoints &&
            event.entity is OtherClientPlayerEntity &&
            event.entity.name.string == "Team Treasurite" &&
            mc.player!!.canSee(event.entity) &&
            event.entity.baseMaxHealth == if (MayorInfo.allPerks.contains("DOUBLE MOBS HP!!!")) 2_000_000.0 else 1_000_000.0
        ) {
            val corleone = CrystalHollowsMap.Locations.Corleone
            if (!corleone.loc.exists()) {
                corleone.loc.set()
                corleone.sendThroughWS()
            } else corleone.loc.set()
        }
    }

    fun onTick(event: TickEvent) {
        if (!Utils.inSkyblock) return
        if ((Skytils.config.crystalHollowWaypoints || Skytils.config.crystalHollowMapPlaces) && SBInfo.mode == SkyblockIsland.CrystalHollows.mode
            && waypointDelayTicks == 0 && mc.player != null
        ) {
            CrystalHollowsMap.Locations.cleanNameToLocation[SBInfo.location]?.let {
                if (!it.loc.exists()) {
                    it.loc.set()
                    it.sendThroughWS()
                } else it.loc.set()
            }
        } else if (waypointDelayTicks > 0)
            waypointDelayTicks--
    }

    fun onWorldChange(event: WorldUnloadEvent) {
        val instance = chWaypointsList.getOrPut(SBInfo.server ?: "") { CHInstance() }
        CrystalHollowsMap.Locations.entries.forEach {
            if (it.loc.exists()) {
                instance.waypoints[it.packetType] = BlockPos(it.loc.locX!!, it.loc.locY!!, it.loc.locZ!!)
            }
            it.loc.reset()
        }
        waypoints.clear()
        waypointDelayTicks = 50
    }

    object CrystalHollowsMap : HudElement("Crystal Hollows Map", 0f, 0f) {
        override fun LayoutScope.render() {
            box(Modifier.width(128f).height(128f)) {
                UIImage.ofResource("/assets/skytils/crystalhollowsmap.png")(Modifier.fillParent())
                MapAddons(Modifier.fillParent())
            }
        }

        object MapAddons : UIComponent() {
            override fun draw(matrixStack: UMatrixStack) {
                beforeDrawCompat(matrixStack)
                matrixStack.push()
                val buffer = UBufferBuilder.create(UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_TEXTURE)
                // Locations
                if (Skytils.config.crystalHollowMapPlaces) {
                    Locations.entries.forEach { location ->
                        location.loc.drawOnMap(location.size, location.color, buffer, matrixStack)
                    }
                }
                // Player Marker
                mc.player?.let { player ->
                    val x = (player.x - 202).coerceIn(0.0, 624.0)
                    val y = (player.y - 202).coerceIn(0.0, 624.0)
                    val playerScale = Skytils.config.crystalHollowsMapPlayerScale
                    matrixStack.push()
                    matrixStack.translate(x, y, 0.0)

                    // Rotate about the center to match the player's yaw
                    matrixStack.rotate((player.headYaw + 180f) % 360f, 0f, 0f, 1f)
                    matrixStack.scale(playerScale, playerScale, 1f)
                    val textureStart = 0.0
                    val textureEnd = 0.25
                    buffer.pos(matrixStack, -8.0, -8.0, 100.0).tex(textureStart, textureStart).endVertex()
                    buffer.pos(matrixStack, -8.0, 8.0, 100.0).tex(textureStart, textureEnd).endVertex()
                    buffer.pos(matrixStack, -8.0, 8.0, 100.0).tex(textureEnd, textureEnd).endVertex()
                    buffer.pos(matrixStack, 8.0, -8.0, 100.0).tex(textureEnd, textureStart).endVertex()
                }
                buffer.build()?.drawAndClose(SRenderPipelines.guiPipeline)
                matrixStack.pop()
                super.draw(matrixStack)
            }
        }

        override fun LayoutScope.demoRender() {
            box(Modifier.width(128f).height(128f)) {
                UIImage.ofResource("/assets/skytils/crystalhollowsmap.png")(Modifier.fillParent())
            }
        }

        enum class Locations(val displayName: String, val id: String, val color: Color, val packetType: CHWaypointType, val size: Int = 50) {
            LostPrecursorCity("§fLost Precursor City", "internal_city", ColorFactory.WHITE, CHWaypointType.LostPrecursorCity),
            JungleTemple("§aJungle Temple", "internal_temple", ColorFactory.GREEN, CHWaypointType.JungleTemple),
            GoblinQueensDen("§eGoblin Queen's Den", "internal_den", ColorFactory.YELLOW, CHWaypointType.GoblinQueensDen),
            MinesOfDivan("§9Mines of Divan", "internal_mines", ColorFactory.BLUE, CHWaypointType.MinesOfDivan),
            KingYolkar("§6King Yolkar", "internal_king", ColorFactory.ORANGE, CHWaypointType.KingYolkar,25),
            KhazadDum("§cKhazad-dûm", "internal_bal", ColorFactory.RED, CHWaypointType.KhazadDum),
            FairyGrotto("§dFairy Grotto", "internal_fairy", ColorFactory.PINK, CHWaypointType.FairyGrotto, 26),
            Corleone("§bCorleone", "internal_corleone", ColorFactory.AQUA, CHWaypointType.Corleone, 26);

            val loc = LocationObject()
            val cleanName = displayName.stripControlCodes()

            companion object {
                val cleanNameToLocation = entries.associateBy { it.cleanName }
            }

            fun sendThroughWS() {
                if (loc.exists()) {
                    WSClient.wsClient.launch {
                        val worldTime = mc.world?.realWorldTime ?: return@launch
                        WSClient.sendPacket(C2SPacketCHWaypoint(serverId = SBInfo.server ?: "", serverTime = worldTime, packetType, loc.locX!!.toInt(), loc.locY!!.toInt(), loc.locZ!!.toInt()))
                    }
                }
            }
        }

    }

    init {
        Skytils.guiManager.registerElement(CrystalHollowsMap)
    }

    class LocationObject {
        var locX: Double? = null
        var locY: Double? = null
        var locZ: Double? = null
        private var locMinX: Double = 1100.0
        private var locMinY: Double = 1100.0
        private var locMinZ: Double = 1100.0
        private var locMaxX: Double = -100.0
        private var locMaxY: Double = -100.0
        private var locMaxZ: Double = -100.0

        fun reset() {
            locX = null
            locY = null
            locZ = null
            locMinX = 1100.0
            locMinY = 1100.0
            locMinZ = 1100.0
            locMaxX = -100.0
            locMaxY = -100.0
            locMaxZ = -100.0
        }

        fun set() {
            val player = mc.player ?: return
            locMinX = (player.x - 200).coerceIn(0.0, 624.0).coerceAtMost(locMinX)
            locMinY = player.y.coerceIn(0.0, 256.0).coerceAtMost(locMinY)
            locMinZ = (player.z - 200).coerceIn(0.0, 624.0).coerceAtMost(locMinZ)
            locMaxX = (player.x - 200).coerceIn(0.0, 624.0).coerceAtLeast(locMaxX)
            locMaxY = player.y.coerceIn(0.0, 256.0).coerceAtLeast(locMaxY)
            locMaxZ = (player.z - 200).coerceIn(0.0, 624.0).coerceAtLeast(locMaxZ)
            locX = (locMinX + locMaxX) / 2
            locY = (locMinY + locMaxY) / 2
            locZ = (locMinZ + locMaxZ) / 2
        }

        fun setExact(x: Double, y: Double, z: Double) {
            locX = x
            locMinX = x
            locMaxX = x

            locY = y
            locMinY = y
            locMaxY = y

            locZ = z
            locMinZ = z
            locMaxZ = z
        }

        fun exists(): Boolean {
            return locX != null && locY != null && locZ != null
        }

        fun drawWaypoint(text: String, partialTicks: Float, matrixStack: UMatrixStack) {
            if (exists())
                RenderUtil.renderWaypointText(text, locX!! + 200, locY!!, locZ!! + 200, partialTicks, matrixStack)
        }

        fun drawOnMap(size: Int, color: Color, buffer: UBufferBuilder, matrixStack: UMatrixStack) {
            if (exists()) {
                writeRectCoords(
                    matrixStack,
                    buffer,
                    locX!! - size,
                    locZ!! - size,
                    locX!! + size,
                    locZ!! + size,
                    color
                )
            }
        }

        override fun toString(): String {
            return String.format("%.0f", locX?.plus(200)) + " " + String.format(
                "%.0f",
                locY
            ) + " " + String.format(
                "%.0f",
                locZ?.plus(200)
            )
        }
    }
}
