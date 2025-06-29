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

package gg.skytils.skytilsmod.features.impl.dungeons.catlas.utils

import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.vertex.UBufferBuilder
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.features.impl.dungeons.catlas.core.CatlasConfig
import gg.skytils.skytilsmod.features.impl.dungeons.catlas.core.CatlasElement
import gg.skytils.skytilsmod.features.impl.dungeons.catlas.core.DungeonMapPlayer
import gg.skytils.skytilsmod.features.impl.dungeons.catlas.handlers.DungeonScanner
import gg.skytils.skytilsmod.utils.DungeonClass
import gg.skytils.skytilsmod.utils.ItemUtil
import gg.skytils.skytilsmod.utils.Utils
import gg.skytils.skytilsmod.utils.ifNull
import gg.skytils.skytilsmod.utils.rendering.DrawHelper
import gg.skytils.skytilsmod.utils.rendering.SRenderPipelines
import net.minecraft.util.Identifier
import net.minecraft.util.math.RotationAxis
import java.awt.Color

object RenderUtils {
    private val mapIcons = Identifier.of("catlas:textures/marker.png")

    private fun addQuadVertices(bufferBuilder: UBufferBuilder, matrices: UMatrixStack, x: Double, y: Double, w: Double, h: Double, color: Color) {
        bufferBuilder.pos(matrices, x, y + h, 0.0).color(color).endVertex()
        bufferBuilder.pos(matrices, x + w, y + h, 0.0).color(color).endVertex()
        bufferBuilder.pos(matrices, x + w, y, 0.0).color(color).endVertex()
        bufferBuilder.pos(matrices, x, y, 0.0).color(color).endVertex()
    }

    fun renderRect(matrices: UMatrixStack, x: Double, y: Double, w: Double, h: Double, color: Color) {
        if (color.alpha == 0) return

        val buffer = UBufferBuilder.create(UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_COLOR)
        addQuadVertices(buffer, matrices, x, y, w, h, color)
        buffer.build()?.drawAndClose(SRenderPipelines.guiPipeline)
    }

    fun renderRectBorder(matrices: UMatrixStack, x: Double, y: Double, w: Double, h: Double, thickness: Double, color: Color) {
        if (color.alpha == 0) return
        val buffer = UBufferBuilder.create(UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_COLOR)
        addQuadVertices(buffer, matrices, x - thickness, y, thickness, h, color)
        addQuadVertices(buffer, matrices, x - thickness, y - thickness, w + thickness * 2, thickness, color)
        addQuadVertices(buffer, matrices, x + w, y, thickness, h, color)
        addQuadVertices(buffer, matrices, x - thickness, y + h, w + thickness * 2, thickness, color)
        buffer.build()?.drawAndClose(SRenderPipelines.guiPipeline)
    }

    fun renderCenteredText(matrices: UMatrixStack, text: List<String>, x: Int, y: Int, color: Int) {
        if (text.isEmpty()) return
        matrices.push()
        matrices.translate(x.toFloat(), y.toFloat(), 0f)
        matrices.scale(CatlasConfig.textScale, CatlasConfig.textScale, 1f)

        if (CatlasConfig.mapRotate) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(mc.player!!.yaw + 180f))
        } else if (CatlasConfig.mapDynamicRotate) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-CatlasElement.dynamicRotation))
        }

        val fontHeight = mc.textRenderer.fontHeight + 1
        val yTextOffset = text.size * fontHeight / -2

        text.withIndex().forEach { (index, text) ->
            UGraphics.drawString(matrices, text, 0f, (yTextOffset + index * fontHeight).toFloat(), color, true)
        }

        if (CatlasConfig.mapDynamicRotate) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(CatlasElement.dynamicRotation))
        }

        matrices.pop()
    }

    fun drawPlayerHead(matrices: UMatrixStack, name: String, player: DungeonMapPlayer) {
        try {
            matrices.push()

            val localName = mc.gameProfile.name
            // Translates to the player's location which is updated every tick.
            if (player.isOurMarker || name == localName) {
                matrices.translate(
                    (mc.player!!.x - DungeonScanner.startX + 15) * MapUtils.coordMultiplier + MapUtils.startCorner.first,
                    (mc.player!!.z - DungeonScanner.startZ + 15) * MapUtils.coordMultiplier + MapUtils.startCorner.second,
                    0.0
                )
            } else {
                player.teammate.player?.also { entityPlayer ->
                    // If the player is loaded in our view, use that location instead (more precise)
                    matrices.translate(
                        (entityPlayer.x - DungeonScanner.startX + 15) * MapUtils.coordMultiplier + MapUtils.startCorner.first,
                        (entityPlayer.z - DungeonScanner.startZ + 15) * MapUtils.coordMultiplier + MapUtils.startCorner.second,
                        0.0
                    )
                }.ifNull {
                    matrices.translate(player.mapX.toFloat(), player.mapZ.toFloat(), 0f)
                }
            }

            // Apply head rotation and scaling
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(player.yaw + 180f))
            matrices.scale(CatlasConfig.playerHeadScale, CatlasConfig.playerHeadScale, 1f)


            if (CatlasConfig.mapVanillaMarker && (player.isOurMarker || name == localName)) {
                matrices.push()
                DrawHelper.drawTexture(matrices, SRenderPipelines.guiTexturePipeline, mapIcons,
                    -6.0, -6.0,
                    0.0, 0.0,
                    12.0, 12.0,
                    12.0, 12.0
                )
                matrices.pop()
            } else {
                // Render box behind the player head
                val borderColor = when (player.teammate.dungeonClass) {
                    DungeonClass.ARCHER -> CatlasConfig.colorPlayerArcher
                    DungeonClass.BERSERK -> CatlasConfig.colorPlayerBerserk
                    DungeonClass.HEALER -> CatlasConfig.colorPlayerHealer
                    DungeonClass.MAGE -> CatlasConfig.colorPlayerMage
                    DungeonClass.TANK -> CatlasConfig.colorPlayerTank
                    else -> Color.BLACK
                }

                renderRect(matrices, -6.0, -6.0, 12.0, 12.0, borderColor)
                matrices.translate(0f, 0f, 0.1f)

                matrices.push()
                val scale = 1f - CatlasConfig.playerBorderPercentage
                matrices.scale(scale, scale, scale)

                DrawHelper.drawTexture(matrices, SRenderPipelines.guiTexturePipeline, player.skin, -6.0, -6.0, 8.0, 8.0, 12.0, 12.0, 64.0, 64.0)
                if (player.renderHat) {
                    DrawHelper.drawTexture(matrices, SRenderPipelines.guiTexturePipeline, player.skin, -6.0, -6.0, 40.0, 8.0, 12.0, 12.0, 64.0, 64.0)
                }
                matrices.pop()
            }

            // Handle player names
            if (CatlasConfig.playerHeads == 2 || CatlasConfig.playerHeads == 1 && Utils.equalsOneOf(
                    ItemUtil.getSkyBlockItemID(mc.player!!.mainHandStack),
                    "SPIRIT_LEAP", "INFINITE_SPIRIT_LEAP", "HAUNT_ABILITY"
                )
            ) {
                if (!CatlasConfig.mapRotate) {
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-player.yaw + 180f))
                }
                matrices.translate(0f, 10f, 0f)
                matrices.scale(CatlasConfig.playerNameScale, CatlasConfig.playerNameScale, 1f)
                UGraphics.drawString(matrices, name, -mc.textRenderer.getWidth(name) / 2f, 0f, 0xffffff, true)
                matrices.pop()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            matrices.pop()
        }
    }
}
