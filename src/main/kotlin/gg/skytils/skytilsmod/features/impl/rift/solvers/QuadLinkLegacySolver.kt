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

package gg.skytils.skytilsmod.features.impl.rift.solvers

import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.Companion.mc
import gg.skytils.skytilsmod.events.impl.GuiContainerEvent
import gg.skytils.skytilsmod.utils.withAlpha
import net.minecraft.client.gui.Gui
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.inventory.ContainerChest
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

object QuadLinkLegacySolver {
    const val guiTitle = "Quad Link Legacy - Wizardman"
    const val oppSlot = 18
    const val ourSlot = 26

    // he takes up the middle 7 slots for each row of the chest
    // this is [row][column]
    val boardSlots = (0 until 6).map { it*9+1..it*9+7 }
    val flatBoardSlots = boardSlots.flatten()

    var ourItem: ItemStack? = null
    var oppItem: ItemStack? = null
    var bestColumn = -1

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Unload) {
        if (event.world != mc.theWorld) return
        reset()
    }

    @SubscribeEvent
    fun onGuiContainerEvent(event: GuiContainerEvent) {
        when (event) {
            is GuiContainerEvent.CloseWindowEvent -> reset()
            is GuiContainerEvent.ForegroundDrawnEvent -> {
                if (!Skytils.config.quadLinkLegacySolver) return
                val container = event.container as? ContainerChest ?: return
                if (event.chestName != guiTitle) return

                if (ourItem == null) {
                    ourItem = container.getSlot(ourSlot).stack
                    oppItem = container.getSlot(oppSlot).stack

                    check(ourItem != null && oppItem != null) { "Our item or opponent's item is null" }
                }

                // if null, it means the placing animation is happening
                // if painting, it means we are waiting for the move
                // if item stack, it means they are waiting for us
                // if there is glass, the game is over
                if (flatBoardSlots.map { container.getSlot(it).stack }.any {
                    it == null ||
                    it.item == Items.painting ||
                    it.item == Item.getItemFromBlock(Blocks.stained_glass) ||
                    !(it.getIsItemStackEqual(ourItem) || it.getIsItemStackEqual(oppItem) || it.item == Items.item_frame)
                }) {
                    bestColumn = -1
                    return
                }

                if (bestColumn == -1) {
                    // read the board in
                    for (column in 0 until 7) {
                        for (row in 0 until 6) {
                            val slot = boardSlots[row].elementAt(column)
                            val item = container.getSlot(slot).stack
                            if (item.getIsItemStackEqual(ourItem)) {
                                board[column][row] = true
                            } else if (item.getIsItemStackEqual(oppItem)) {
                                board[column][row] = false
                            } else if (item.item == Items.item_frame) {
                                board[column][row] = null
                            }
                        }
                    }

                    val result = negamax(1000, isOurs = true)
                    bestColumn = result.first
                }

                if (bestColumn != -1) {
                    val topSlot = container.getSlot(bestColumn+1)
                    Gui.drawRect(
                        topSlot.xDisplayPosition,
                        topSlot.yDisplayPosition,
                        topSlot.xDisplayPosition + 16,
                        topSlot.yDisplayPosition + 16 * 6,
                        Color.RED.withAlpha(100)
                    )
                }
            }
        }
    }

    fun reset() {
        board.forEach { it.fill(null) }
        ourItem = null
        oppItem = null
        bestColumn = -1
    }

    /**
      * board[column][row]
      * boolean? = null if empty, true if our piece, false if opponent's piece
     */
    val board: Array<Array<Boolean?>> = Array(7) { arrayOfNulls(6) }

    /**
     * Makes a move in Connect 4
     * @return If the move was successful
    */
    fun makeMove(column: Int, ourPiece: Boolean): Boolean {
        check(column in 0 until 7) { "Column must be between 0 and 6" }
        board[column].forEachIndexed { index, b ->
            if (b == null) {
                board[column][index] = ourPiece
                return true
            }
        }
        return false
    }

    /**
     * Removes the top piece on a column in Connect 4
     * @return If the move was successful
    */
    fun popMove(column: Int): Boolean {
        check(column in 0 until 7) { "Column must be between 0 and 6" }
        for (row in 5 downTo 0) {
            if (board[column][row] != null) {
                board[column][row] = null
                return true
            }
        }
        return false
    }


    /**
    * @return true if we won, false if opponent won, null if no one won
    */
    fun getWinner(): Boolean? {
        // Check horizontal
        for (row in 0 until 6) {
            for (column in 0 until 4) {
                if (board[column][row] != null &&
                    board[column][row] == board[column + 1][row] &&
                    board[column][row] == board[column + 2][row] &&
                    board[column][row] == board[column + 3][row]
                ) {
                    return board[column][row]
                }
            }
        }

        // Check vertical
        for (column in 0 until 7) {
            for (row in 0 until 3) {
                if (board[column][row] != null &&
                    board[column][row] == board[column][row + 1] &&
                    board[column][row] == board[column][row + 2] &&
                    board[column][row] == board[column][row + 3]
                ) {
                    return board[column][row]
                }
            }
        }

        // Check diagonal
        for (column in 0 until 4) {
            for (row in 0 until 3) {
                if (board[column][row] != null &&
                    board[column][row] == board[column + 1][row + 1] &&
                    board[column][row] == board[column + 2][row + 2] &&
                    board[column][row] == board[column + 3][row + 3]
                ) {
                    return board[column][row]
                }
            }
        }

        for (column in 0 until 4) {
            for (row in 3 until 6) {
                if (board[column][row] != null &&
                    board[column][row] == board[column + 1][row - 1] &&
                    board[column][row] == board[column + 2][row - 2] &&
                    board[column][row] == board[column + 3][row - 3]
                ) {
                    return board[column][row]
                }
            }
        }

        return null
    }

    /**
     * @return The best move to make and the score of that move
     */
    fun negamax(depth: Int, alpha: Int = Int.MIN_VALUE, beta: Int = Int.MAX_VALUE, isOurs: Boolean): Pair<Int, Int> {
        // TODO: find better base case score
        if (depth == 0) return -1 to 0
        val winner = getWinner()
        if (winner != null) {
            return -1 to if (winner) depth else -depth
        }

        var bestScore = Int.MIN_VALUE
        var a = alpha
        var bestMove = -1

        for (column in 0 until 7) {
            if (makeMove(column, isOurs)) {
                val score = -negamax(depth - 1, -beta, -a, !isOurs).second
                popMove(column)
                if (score >= beta) return column to score

                if (score > bestScore) {
                    bestScore = score
                    bestMove = column
                }
                a = maxOf(a, score)
                if (alpha >= beta) break
            }
        }

        return bestMove to bestScore
    }
}