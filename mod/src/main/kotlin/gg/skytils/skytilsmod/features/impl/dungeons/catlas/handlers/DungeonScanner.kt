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

package gg.skytils.skytilsmod.features.impl.dungeons.catlas.handlers

import gg.essential.universal.UChat
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.features.impl.dungeons.DungeonFeatures.dungeonFloorNumber
import gg.skytils.skytilsmod.features.impl.dungeons.catlas.core.map.*
import gg.skytils.skytilsmod.features.impl.dungeons.catlas.handlers.DungeonScanner.scan
import gg.skytils.skytilsmod.features.impl.dungeons.catlas.utils.HeightProvider
import gg.skytils.skytilsmod.features.impl.dungeons.catlas.utils.ScanUtils
import gg.skytils.skytilsmod.listeners.DungeonListener
import gg.skytils.skytilsmod.utils.SBInfo
import gg.skytils.skytilsmod.utils.printDevMessage
import gg.skytils.skytilsws.shared.packet.C2SPacketDungeonRoom
import net.minecraft.block.Blocks
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

//#if MC>=12000
import net.minecraft.world.chunk.ChunkStatus
//#endif

/**
 * Handles everything related to scanning the dungeon. Running [scan] will update the instance of [DungeonInfo].
 */
object DungeonScanner {

    /**
     * The size of each dungeon room in blocks.
     */
    const val roomSize = 32

    /**
     * The starting coordinates to start scanning (the north-west corner).
     */
    const val startX = -185
    const val startZ = -185

    private var lastScanTime = 0L
    var isScanning = false
    var hasScanned = false

    val shouldScan: Boolean
        get() = !isScanning && !hasScanned && System.currentTimeMillis() - lastScanTime >= 250 && dungeonFloorNumber != null

    //#if MC<11300
    //$$ private val entranceDoorBlock = Blocks.MONSTER_EGG
    //#else
    private val entranceDoorBlock = Blocks.INFESTED_CHISELED_STONE_BRICKS
    //#endif

    //#if MC<11300
    //$$ private val bloodDoorBlock = Blocks.STAINED_HARDENED_CLAY
    //#else
    private val bloodDoorBlock = Blocks.RED_TERRACOTTA
    //#endif

    fun scan() {
        val world = mc.world ?: return
        isScanning = true
        var allChunksLoaded = true

        // Scans the dungeon in a 11x11 grid.
        for (x in 0..10) {
            for (z in 0..10) {
                // Translates the grid index into world position.
                val xPos = startX + x * (roomSize shr 1)
                val zPos = startZ + z * (roomSize shr 1)

                //#if MC==10809
                //$$ if (!world.method_0_271(xPos shr 4, zPos shr 4).method_12229()) {
                //#else
                if (!world.chunkManager.isChunkLoaded(xPos shr 4, zPos shr 4) || HeightProvider.getHeight(x, z) == null) {
                //#endif
                    // The room being scanned has not been loaded in.
                    allChunksLoaded = false
                    continue
                }

                // This room has already been added in a previous scan.
                if (DungeonInfo.dungeonList[x + z * 11].run {
                        this !is Unknown && (this as? Room)?.data?.name != "Unknown"
                    }) continue

                scanRoom(world, xPos, zPos, z, x)?.let {
                    DungeonInfo.dungeonList[z * 11 + x] = it
                    if (it is Room && it.data.name != "Unknown") {
                        SBInfo.server?.let { server ->
                            printDevMessage({ "Sending room data to channel: ${it.data.name}" }, "dungeonws")
                            val result = DungeonListener.outboundRoomQueue.trySend(
                                C2SPacketDungeonRoom(server, it.data.name, xPos, zPos, x, z, it.core, it.isSeparator)
                            )
                            if (result.isFailure) {
                                UChat.chat("${Skytils.failPrefix} §cFailed to send room data to server. ${result.isClosed}")
                            }
                        }
                    }
                }
            }
        }

        if (allChunksLoaded) {
            DungeonInfo.roomCount = DungeonInfo.dungeonList.filter { it is Room && !it.isSeparator }.size
            hasScanned = true
        }

        lastScanTime = System.currentTimeMillis()
        isScanning = false
    }

    private fun scanRoom(world: World, x: Int, z: Int, row: Int, column: Int): Tile? {
        val height = (HeightProvider.getHeight(x, z) ?: Integer.MIN_VALUE) + 1
        if (height <= 0) return null

        val rowEven = row and 1 == 0
        val columnEven = column and 1 == 0

        return when {
            // Scanning a room
            rowEven && columnEven -> {
                val roomCore = ScanUtils.getCore(x, z)
                Room(x, z, ScanUtils.getRoomData(roomCore) ?: return null).apply {
                    core = roomCore
                    addToUnique(row, column)
                }
            }

            // Can only be the center "block" of a 2x2 room.
            !rowEven && !columnEven -> {
                DungeonInfo.dungeonList[column - 1 + (row - 1) * 11].let {
                    if (it is Room) {
                        Room(x, z, it.data).apply {
                            isSeparator = true
                            addToUnique(row, column)
                        }
                    } else null
                }
            }

            // Doorway between rooms
            // Old trap has a single block at 82
            height == 74 || height == 82 -> {
                Door(
                    x, z,
                    // Finds door type from door block
                    type = when (world.getBlockState(BlockPos(x, 69, z)).block) {
                        Blocks.COAL_BLOCK -> {
                            DungeonInfo.witherDoors++
                            DoorType.WITHER
                        }

                        entranceDoorBlock -> DoorType.ENTRANCE
                        bloodDoorBlock -> DoorType.BLOOD
                        else -> DoorType.NORMAL
                    }
                )
            }

            // Connection between large rooms
            else -> {
                DungeonInfo.dungeonList[if (rowEven) row * 11 + column - 1 else (row - 1) * 11 + column].let {
                    if (it !is Room) {
                        null
                    } else if (it.data.type == RoomType.ENTRANCE) {
                        Door(x, z, DoorType.ENTRANCE)
                    } else {
                        Room(x, z, it.data).apply {
                            isSeparator = true
                            uniqueRoom = DungeonInfo.uniqueRooms[data.name]
                        }
                    }
                }
            }
        }
    }
}
