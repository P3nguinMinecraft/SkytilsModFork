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
package gg.skytils.skytilsmod.features.impl.spidersden

import com.mojang.blaze3d.opengl.GlStateManager
import gg.essential.universal.UMatrixStack
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.render.WorldDrawEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod._event.MainThreadPacketReceiveEvent
import gg.skytils.skytilsmod._event.PacketSendEvent
import gg.skytils.skytilsmod.features.impl.trackers.Tracker
import gg.skytils.skytilsmod.utils.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.SetSerializer
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket
import net.minecraft.util.math.Box
import net.minecraft.util.math.BlockPos
import net.minecraft.particle.ParticleTypes
import java.awt.Color
import java.io.Reader
import java.io.Writer

object RelicWaypoints : Tracker("found_spiders_den_relics"), EventSubscriber {
    val relicLocations = hashSetOf<BlockPos>()
    val foundRelics = hashSetOf<BlockPos>()
    private val rareRelicLocations = hashSetOf<BlockPos>()

    override fun setup() {
        register(::onReceivePacket)
        register(::onSendPacket)
        register(::onWorldRender)
    }

    fun onReceivePacket(event: MainThreadPacketReceiveEvent<*>) {
        if (!Utils.inSkyblock) return
        if (event.packet is ParticleS2CPacket) {
            if (Skytils.config.rareRelicFinder) {
                event.packet.apply {
                    if (parameters == ParticleTypes.WITCH && count == 2 && shouldForceSpawn() && speed == 0f && offsetX == 0.3f && offsetY == 0.3f && offsetZ == 0.3f) {
                        rareRelicLocations.add(BlockPos.ofFloored(x, y, z))
                    }
                }
            }
        }
    }

    fun onSendPacket(event: PacketSendEvent<*>) {
        if (!Utils.inSkyblock) return
        if (SBInfo.mode != SkyblockIsland.SpiderDen.mode) return
        if (event.packet is PlayerInteractBlockC2SPacket) {
            val pos = event.packet.blockHitResult.blockPos
            if (relicLocations.contains(pos)) {
                foundRelics.add(pos)
                rareRelicLocations.remove(pos)
                markDirty<RelicWaypoints>()
            }
        }
    }

    fun onWorldRender(event: WorldDrawEvent) {
        if (!Utils.inSkyblock) return
        if (SBInfo.mode != SkyblockIsland.SpiderDen.mode) return
        val (viewerX, viewerY, viewerZ) = RenderUtil.getViewerPos(event.partialTicks)
        val matrixStack = UMatrixStack()

        if (Skytils.config.relicWaypoints) {
            for (relic in relicLocations) {
                if (foundRelics.contains(relic)) continue
                val x = relic.x - viewerX
                val y = relic.y - viewerY
                val z = relic.z - viewerZ
                val distSq = x * x + y * y + z * z
                GlStateManager._disableDepthTest()
                GlStateManager._disableCull()
                RenderUtil.drawFilledBoundingBox(
                    matrixStack,
                    Box(x, y, z, x + 1, y + 1, z + 1),
                    Color(114, 245, 82),
                    1f
                )
                matrixStack.push()
                matrixStack.translate(x, y+1, z)
                if (distSq > 5 * 5) RenderUtil.renderBeaconBeam(
                    matrixStack,
                    Color(114, 245, 82).rgb,
                    event.partialTicks
                )
                matrixStack.pop()
                RenderUtil.renderWaypointText("Relic", relic, event.partialTicks, matrixStack)
                // disable lighting
                GlStateManager._enableDepthTest()
                GlStateManager._enableCull()
            }
        }
        if (Skytils.config.rareRelicFinder) {
            for (relic in rareRelicLocations) {
                val x = relic.x - viewerX
                val y = relic.y - viewerY
                val z = relic.z - viewerZ
                val distSq = x * x + y * y + z * z
                GlStateManager._disableDepthTest()
                GlStateManager._disableCull()
                RenderUtil.drawFilledBoundingBox(
                    matrixStack,
                    Box(x, y, z, x + 1, y + 1, z + 1),
                    Color(152, 41, 222),
                    1f
                )
                matrixStack.push()
                matrixStack.translate(x, y+1, z)
                if (distSq > 5 * 5) RenderUtil.renderBeaconBeam(
                    matrixStack,
                    Color(152, 41, 222).rgb,
                    event.partialTicks
                )
                matrixStack.pop()
                RenderUtil.renderWaypointText("Rare Relic", relic, event.partialTicks, matrixStack)
                // disable lighting
                GlStateManager._enableDepthTest()
                GlStateManager._enableCull()
            }
        }
    }

    override fun resetLoot() {
        foundRelics.clear()
    }

    override fun read(reader: Reader) {
        foundRelics.clear()
        foundRelics.addAll(json.decodeFromString(ListSerializer(BlockPosCSV), reader.readText()))
    }

    override fun write(writer: Writer) {
        writer.write(json.encodeToString(SetSerializer(BlockPosCSV), foundRelics))
    }

    override fun setDefault(writer: Writer) {
        writer.write("[]")
    }
}