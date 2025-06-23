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

package gg.skytils.skytilsmod.features.impl.dungeons.catlas.core

import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.unstable.layoutdsl.LayoutScope
import gg.essential.elementa.unstable.layoutdsl.Modifier
import gg.essential.elementa.unstable.layoutdsl.height
import gg.essential.elementa.unstable.layoutdsl.width
import gg.essential.elementa.unstable.state.v2.State
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.vertex.UBufferBuilder
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.core.structure.v2.HudElement
import gg.skytils.skytilsmod.features.impl.dungeons.DungeonTimer
import gg.skytils.skytilsmod.features.impl.dungeons.catlas.core.map.*
import gg.skytils.skytilsmod.features.impl.dungeons.catlas.handlers.DungeonInfo
import gg.skytils.skytilsmod.features.impl.dungeons.catlas.handlers.DungeonMapColorParser
import gg.skytils.skytilsmod.features.impl.dungeons.catlas.handlers.DungeonScanner
import gg.skytils.skytilsmod.features.impl.dungeons.catlas.utils.MapUtils
import gg.skytils.skytilsmod.features.impl.dungeons.catlas.utils.RenderUtils
import gg.skytils.skytilsmod.gui.layout.text
import gg.skytils.skytilsmod.listeners.DungeonListener
import gg.skytils.skytilsmod.utils.SBInfo
import gg.skytils.skytilsmod.utils.SkyblockIsland
import gg.skytils.skytilsmod.utils.Utils
import gg.skytils.skytilsmod.utils.rendering.DrawHelper
import gg.skytils.skytilsmod.utils.rendering.SRenderPipelines
import net.minecraft.util.Identifier
import net.minecraft.util.math.RotationAxis
import net.minecraft.util.profiler.Profilers
import java.awt.Color

object CatlasElement : UIContainer() {

    private val neuGreen = Identifier.of("catlas:textures/checkmarks/neu/green_check.png")
    private val neuWhite = Identifier.of("catlas:textures/checkmarks/neu/white_check.png")
    private val neuCross = Identifier.of("catlas:textures/checkmarks/neu/cross.png")
    private val neuQuestion = Identifier.of("catlas:textures/checkmarks/neu/question.png")
    private val defaultGreen = Identifier.of("catlas:textures/checkmarks/default/green_check.png")
    private val defaultWhite = Identifier.of("catlas:textures/checkmarks/default/white_check.png")
    private val defaultCross = Identifier.of("catlas:textures/checkmarks/default/cross.png")
    private val defaultQuestion = Identifier.of("catlas:textures/checkmarks/default/question.png")

    var dynamicRotation = 0f

    private fun setupRotate(matrices: UMatrixStack) {
        UGraphics.enableScissor(0, 0, 128, 128)
        matrices.translate(64.0, 64.0, 0.0)
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-mc.player!!.yaw + 180f))

        if (CatlasConfig.mapCenter) {
            matrices.translate(
                -((mc.player!!.x - DungeonScanner.startX + 15) * MapUtils.coordMultiplier + MapUtils.startCorner.first - 2),
                -((mc.player!!.z - DungeonScanner.startZ + 15) * MapUtils.coordMultiplier + MapUtils.startCorner.second - 2),
                0.0
            )
        } else {
            matrices.translate(-64.0, -64.0, 0.0)
        }
    }

    private fun renderRooms(matrices: UMatrixStack) {
        matrices.push()
        matrices.translate(MapUtils.startCorner.first.toFloat(), MapUtils.startCorner.second.toFloat(), 0f)

        val connectorSize = (DungeonMapColorParser.quarterRoom.takeUnless { it == -1 } ?: 4)
        val checkmarkSize = when (CatlasConfig.mapCheckmark) {
            1 -> 8.0 // default
            else -> 10.0 // neu
        }

        for (y in 0..10) {
            for (x in 0..10) {
                val tile = DungeonInfo.dungeonList[y * 11 + x]

                if (tile is Unknown || (tile is Room && tile.state == RoomState.UNDISCOVERED)) continue
                if (tile is Door && getDoorState(tile, y, x) == RoomState.UNDISCOVERED) continue

                val xOffset = (x shr 1) * (MapUtils.mapRoomSize + connectorSize)
                val yOffset = (y shr 1) * (MapUtils.mapRoomSize + connectorSize)

                val xEven = x and 1 == 0
                val yEven = y and 1 == 0

                when {
                    xEven && yEven -> if (tile is Room) {
                        RenderUtils.renderRect(
                            xOffset.toDouble(),
                            yOffset.toDouble(),
                            MapUtils.mapRoomSize.toDouble(),
                            MapUtils.mapRoomSize.toDouble(),
                            tile.color
                        )
                    }

                    !xEven && !yEven -> {
                        RenderUtils.renderRect(
                            xOffset.toDouble(),
                            yOffset.toDouble(),
                            (MapUtils.mapRoomSize + connectorSize).toDouble(),
                            (MapUtils.mapRoomSize + connectorSize).toDouble(),
                            tile.color
                        )
                    }

                    else -> drawRoomConnector(
                        xOffset, yOffset, connectorSize, tile is Door, !xEven, tile.color
                    )
                }

                if (tile is Room && tile.state == RoomState.UNOPENED && CatlasConfig.mapCheckmark != 0) {
                    drawCheckmark(matrices, tile, xOffset.toFloat(), yOffset.toFloat(), checkmarkSize)
                }
            }
        }
        matrices.pop()
    }

    private fun getDoorState(door: Door, row: Int, column: Int): RoomState {
        val rooms = getConnectingRooms(door, row, column) ?: return RoomState.UNDISCOVERED
        if (rooms.toList().any { it.state == RoomState.UNDISCOVERED }) return RoomState.UNDISCOVERED
        return RoomState.PREVISITED
    }

    private fun getConnectingRooms(door: Door, row: Int, column: Int): Pair<Room, Room>? {
        val vertical = column % 2 == 0
        val connectingTiles = runCatching {
            if (vertical) {
                DungeonInfo.dungeonList[(row - 1) * 11 + column] to DungeonInfo.dungeonList[(row + 1) * 11 + column]
            } else {
                DungeonInfo.dungeonList[row * 11 + column - 1] to DungeonInfo.dungeonList[row * 11 + column + 1]
            }
        }.getOrNull() ?: return null
        return (connectingTiles.first as? Room ?: return null) to (connectingTiles.second as? Room ?: return null)
    }

    private fun renderText(matrices: UMatrixStack) {
        matrices.push()
        matrices.translate(MapUtils.startCorner.first.toFloat(), MapUtils.startCorner.second.toFloat(), 0f)

        val checkmarkSize = when (CatlasConfig.mapCheckmark) {
            1 -> 8.0 // default
            else -> 10.0 // neu
        }

        DungeonInfo.uniqueRooms.values.forEach { unq ->
            val room = unq.mainRoom
            if (room.state == RoomState.UNDISCOVERED || room.state == RoomState.UNOPENED) return@forEach
            val halfRoom = (DungeonMapColorParser.halfRoom.takeUnless { it == -1 } ?: 8)
            val size = MapUtils.mapRoomSize + (DungeonMapColorParser.quarterRoom.takeUnless { it == -1 } ?: 4)
            val checkPos = unq.getCheckmarkPosition()
            val namePos = unq.getNamePosition()
            val xOffsetCheck = (checkPos.first / 2f) * size
            val yOffsetCheck = (checkPos.second / 2f) * size
            val xOffsetName = (namePos.first / 2f) * size
            val yOffsetName = (namePos.second / 2f) * size

            val color = if (CatlasConfig.mapColorText) when (room.state) {
                RoomState.GREEN -> 0x55FF55
                RoomState.CLEARED -> 0xFFFFFF
                RoomState.FAILED -> 0xFF0000
                RoomState.PREVISITED -> 0x555555
                else -> 0xAAAAAA
            } else 0xFFFFFF

            val secretCount = room.data.secrets
            val roomType = room.data.type
            val hasSecrets = secretCount > 0

            val secretText = when (CatlasConfig.foundRoomSecrets) {
                0 -> secretCount.toString()
                1 -> if (secretCount == 0 && (unq.foundSecrets ?: 0) == 0) "0" else "${unq.foundSecrets ?: "?"}/${secretCount}"
                2 -> unq.foundSecrets?.toString() ?: "?"
                else -> error("Invalid foundRoomSecrets value")
            }

            if (hasSecrets && (CatlasConfig.mapRoomSecrets == 2 || CatlasConfig.mapRoomSecrets == 3 && room.state != RoomState.GREEN)) {
                matrices.push()
                matrices.translate(
                    xOffsetCheck + halfRoom.toFloat(),
                    yOffsetCheck + 2 + halfRoom.toFloat(),
                    0f
                )
                matrices.scale(2f, 2f, 1f)
                RenderUtils.renderCenteredText(listOf(secretText), 0, 0, color)
                matrices.pop()
            } else if (CatlasConfig.mapCheckmark != 0) {
                drawCheckmark(matrices, room, xOffsetCheck, yOffsetCheck, checkmarkSize)
            }

            val name = mutableListOf<String>()

            if ((CatlasConfig.mapRoomNames != 0 && roomType == RoomType.PUZZLE) || (CatlasConfig.mapRoomNames >= 2 && roomType == RoomType.TRAP) || (CatlasConfig.mapRoomNames == 3 && Utils.equalsOneOf(
                    roomType,
                    RoomType.NORMAL, RoomType.RARE, RoomType.CHAMPION
                ))
            ) {
                name.addAll(room.data.name.split(" "))
            }
            if (room.data.type == RoomType.NORMAL && CatlasConfig.mapRoomSecrets == 1) {
                name.add(secretText)
            }
            // Offset + half of roomsize
            RenderUtils.renderCenteredText(
                name,
                (xOffsetName + halfRoom).toInt(),
                (yOffsetName + halfRoom).toInt(),
                color
            )
        }
        matrices.pop()
    }

    private fun getCheckmark(state: RoomState, type: Int): Identifier? {
        return when (type) {
            1 -> when (state) {
                RoomState.CLEARED -> defaultWhite
                RoomState.GREEN -> defaultGreen
                RoomState.FAILED -> defaultCross
                RoomState.UNOPENED -> defaultQuestion
                else -> null
            }

            2 -> when (state) {
                RoomState.CLEARED -> neuWhite
                RoomState.GREEN -> neuGreen
                RoomState.FAILED -> neuCross
                RoomState.UNOPENED -> neuQuestion
                else -> null
            }

            else -> null
        }
    }

    private fun drawCheckmark(matrices: UMatrixStack, tile: Tile, xOffset: Float, yOffset: Float, checkmarkSize: Double) {
        getCheckmark(tile.state, CatlasConfig.mapCheckmark)?.let {
            val buffer = UBufferBuilder.create(UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_TEXTURE_COLOR)
            DrawHelper.drawTexture(matrices, buffer, it, xOffset + (MapUtils.mapRoomSize - checkmarkSize) / 2, yOffset + (MapUtils.mapRoomSize - checkmarkSize) / 2, width = checkmarkSize, height = checkmarkSize)
            buffer.build()?.drawAndClose(SRenderPipelines.guiTexturePiepline)
        }
    }

    private fun renderPlayerHeads() {
        if (DungeonTimer.bossEntryTime != null) return
        DungeonListener.team.forEach { (name, teammate) ->
            if (!teammate.dead || teammate.mapPlayer.isOurMarker) {
                RenderUtils.drawPlayerHead(name, teammate.mapPlayer)
            }
        }
    }

    private fun drawRoomConnector(
        x: Int,
        y: Int,
        doorWidth: Int,
        doorway: Boolean,
        vertical: Boolean,
        color: Color,
    ) {
        val doorwayOffset = if (MapUtils.mapRoomSize == 16) 5 else 6
        val width = if (doorway) 6 else MapUtils.mapRoomSize
        var x1 = if (vertical) x + MapUtils.mapRoomSize else x
        var y1 = if (vertical) y else y + MapUtils.mapRoomSize
        if (doorway) {
            if (vertical) y1 += doorwayOffset else x1 += doorwayOffset
        }
        RenderUtils.renderRect(
            x1.toDouble(),
            y1.toDouble(),
            (if (vertical) doorWidth else width).toDouble(),
            (if (vertical) width else doorWidth).toDouble(),
            color
        )
    }

    override fun draw(matrixStack: UMatrixStack) {
        beforeDrawCompat(matrixStack)
        if (SBInfo.mode != SkyblockIsland.Dungeon.mode || mc.player == null || mc.world == null) return
        if (DungeonTimer.dungeonStartTime == null && !CatlasConfig.mapShowBeforeStart) return
        if (CatlasConfig.mapHideInBoss && DungeonTimer.bossEntryTime != null) return

        val profiler = Profilers.get()

        matrixStack.runWithGlobalState {
            profiler.push("border")

            RenderUtils.renderRect(
                0.0, 0.0, 128.0, 128.0, CatlasConfig.mapBackground
            )

            RenderUtils.renderRectBorder(
                0.0,
                0.0,
                128.0,
                128.0,
                CatlasConfig.mapBorderWidth.toDouble(),
                CatlasConfig.mapBorder
            )

            profiler.pop()

            if (CatlasConfig.mapRotate) {
                matrixStack.push()
                setupRotate(matrixStack)
            } else if (CatlasConfig.mapDynamicRotate) {
                matrixStack.translate(64.0, 64.0, 0.0)
                matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(dynamicRotation))
                matrixStack.translate(-64.0, -64.0, 0.0)
            }

            profiler.push("rooms")
            renderRooms(matrixStack)
            profiler.swap("text")
            renderText(matrixStack)
            profiler.swap("heads")
            renderPlayerHeads()
            profiler.pop()

            if (CatlasConfig.mapRotate) {
                UGraphics.disableScissor()
                matrixStack.pop()
            } else if (CatlasConfig.mapDynamicRotate) {
                matrixStack.translate(64.0, 64.0, 0.0)
                matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-dynamicRotation))
                matrixStack.translate(-64.0, -64.0, 0.0)
            }
        }

        super.draw(matrixStack)
    }
}

object CatlasHudElement : HudElement("Dungeon Map", 0f, 0f) {
    override val toggleState: State<Boolean>
        get() = CatlasConfig.mapEnabled
    override fun LayoutScope.render() {
        CatlasElement(Modifier.width(128f).height(128f))
    }

    override fun LayoutScope.demoRender() {
        UIBlock(Color.RED)(Modifier.width(128f).height(128f)) {
            text("Dungeon Map")
        }
    }
}
