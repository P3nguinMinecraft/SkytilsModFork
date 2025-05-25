/*
 * Skytils - Hypixel Skyblock Quality of Life Mod
 * Copyright (C) 2020-2025 Skytils
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

package gg.skytils.skytilsmod.gui.components

import gg.essential.elementa.UIComponent
import gg.essential.elementa.state.v2.State
import gg.essential.elementa.state.v2.stateOf
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UMinecraft
import net.minecraft.client.font.TextRenderer
import net.minecraft.text.Text
import java.awt.Color
import kotlin.text.isEmpty

class UIMCText(
    val text: State<Text>,
    val shadow: State<Color?> = stateOf(null) // null if no shadow
) : UIComponent() {

    override fun draw(matrixStack: UMatrixStack) {
        val text = text.getUntracked()
        if (text.string.isEmpty())
            return

        beforeDrawCompat(matrixStack)


        UGraphics.enableBlend()
        drawText(matrixStack)
        super.draw(matrixStack)
    }

    // Legacy forge (1.8.9)
//    private fun drawText(matrixStack: UMatrixStack) {
//        val text = text.getUntracked().formattedText
//        val x = getLeft()
//        val y = getTop()
//        val color = getColor()
//        val scale = getWidth() / textWidthState.get()
//        getFontProvider().drawString(matrixStack, text, color, x, y, 10f, scale)
//    }

    private fun drawText(matrixStack: UMatrixStack) {
        val text = text.getUntracked()
        val vertexConsumer = UMinecraft.getMinecraft().bufferBuilders.entityVertexConsumers
        shadow.getUntracked()?.let { shadowColor ->
            UMinecraft.getMinecraft().textRenderer.draw(text, 1f, 1f, shadowColor.rgb, false, matrixStack.peek().model, vertexConsumer, TextRenderer.TextLayerType.NORMAL, 0, 15728880)
        }
        UMinecraft.getMinecraft().textRenderer.draw(text, 0f, 0f, getColor().rgb, false, matrixStack.peek().model, vertexConsumer, TextRenderer.TextLayerType.NORMAL, 0, 15728880)
        vertexConsumer.draw()
    }


}