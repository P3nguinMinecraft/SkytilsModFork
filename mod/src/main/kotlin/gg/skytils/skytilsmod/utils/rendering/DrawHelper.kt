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

package gg.skytils.skytilsmod.utils.rendering

import com.mojang.blaze3d.systems.RenderSystem
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.vertex.UBufferBuilder
import gg.skytils.skytilsmod.Skytils.mc
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.render.DiffuseLighting
import net.minecraft.client.render.OverlayTexture
import net.minecraft.item.ItemDisplayContext
import net.minecraft.item.ItemStack
import net.minecraft.util.Colors
import net.minecraft.util.Identifier
import net.minecraft.util.math.Box
import java.awt.Color

object DrawHelper {
    private val textureManager = mc.textureManager

    /**
     * Applies the camera offset to the given matrices.
     * This is useful for rendering things in world space, as it will negate the camera position.
     */
    fun cameraOffset(matrices: UMatrixStack) {
        matrices.translate(mc.gameRenderer.camera.pos.negate())
    }

    /**
    * Writes a cube outline to the given buffer. Draw must still be called manually.
    * Buffer must be created with [gg.essential.universal.UGraphics.DrawMode.LINES]
    */
    fun writeOutlineCube(
        buffer: UBufferBuilder,
        matrices: UMatrixStack,
        box: Box,
        color: Color
    ) {
        box.apply {
            buffer.pos(matrices, minX, minY, minZ).color(color).endVertex()
            buffer.pos(matrices, maxX, minY, minZ).color(color).endVertex()

            buffer.pos(matrices, maxX, minY, minZ).color(color).endVertex()
            buffer.pos(matrices, maxX, minY, maxZ).color(color).endVertex()

            buffer.pos(matrices, maxX, minY, maxZ).color(color).endVertex()
            buffer.pos(matrices, minX, minY, maxZ).color(color).endVertex()

            buffer.pos(matrices, minX, minY, maxZ).color(color).endVertex()
            buffer.pos(matrices, minX, minY, minZ).color(color).endVertex()

            buffer.pos(matrices, minX, maxY, minZ).color(color).endVertex()
            buffer.pos(matrices, maxX, maxY, minZ).color(color).endVertex()

            buffer.pos(matrices, maxX, maxY, minZ).color(color).endVertex()
            buffer.pos(matrices, maxX, maxY, maxZ).color(color).endVertex()

            buffer.pos(matrices, maxX, maxY, maxZ).color(color).endVertex()
            buffer.pos(matrices, minX, maxY, maxZ).color(color).endVertex()

            buffer.pos(matrices, minX, maxY, maxZ).color(color).endVertex()
            buffer.pos(matrices, minX, maxY, minZ).color(color).endVertex()

            buffer.pos(matrices, minX, minY, minZ).color(color).endVertex()
            buffer.pos(matrices, minX, maxY, minZ).color(color).endVertex()

            buffer.pos(matrices, maxX, minY, minZ).color(color).endVertex()
            buffer.pos(matrices, maxX, maxY, minZ).color(color).endVertex()

            buffer.pos(matrices, maxX, minY, maxZ).color(color).endVertex()
            buffer.pos(matrices, maxX, maxY, maxZ).color(color).endVertex()

            buffer.pos(matrices, minX, minY, maxZ).color(color).endVertex()
            buffer.pos(matrices, minX, maxY, maxZ).color(color).endVertex()   
        }
    }

    /**
     * Writes a filled cube to the given buffer. Draw must still be called manually.
     * Buffer must be created with [gg.essential.universal.UGraphics.DrawMode.TRIANGLES]
     */
    fun writeFilledCube(
        buffer: UBufferBuilder,
        matrices: UMatrixStack,
        box: Box,
        color: Color
    ) {
        box.apply {
            buffer.pos(matrices, minX, minY, maxZ).color(color).endVertex()
            buffer.pos(matrices, maxX, minY, maxZ).color(color).endVertex()
            buffer.pos(matrices, maxX, maxY, maxZ).color(color).endVertex()
            buffer.pos(matrices, maxX, maxY, maxZ).color(color).endVertex()
            buffer.pos(matrices, minX, maxY, maxZ).color(color).endVertex()
            buffer.pos(matrices, minX, minY, maxZ).color(color).endVertex()

            buffer.pos(matrices, maxX, minY, minZ).color(color).endVertex()
            buffer.pos(matrices, minX, minY, minZ).color(color).endVertex()
            buffer.pos(matrices, minX, maxY, minZ).color(color).endVertex()
            buffer.pos(matrices, minX, maxY, minZ).color(color).endVertex()
            buffer.pos(matrices, maxX, maxY, minZ).color(color).endVertex()
            buffer.pos(matrices, maxX, minY, minZ).color(color).endVertex()

            buffer.pos(matrices, minX, maxY, maxZ).color(color).endVertex()
            buffer.pos(matrices, maxX, maxY, maxZ).color(color).endVertex()
            buffer.pos(matrices, maxX, maxY, minZ).color(color).endVertex()
            buffer.pos(matrices, maxX, maxY, minZ).color(color).endVertex()
            buffer.pos(matrices, minX, maxY, minZ).color(color).endVertex()
            buffer.pos(matrices, minX, maxY, maxZ).color(color).endVertex()

            buffer.pos(matrices, minX, minY, minZ).color(color).endVertex()
            buffer.pos(matrices, maxX, minY, minZ).color(color).endVertex()
            buffer.pos(matrices, maxX, minY, maxZ).color(color).endVertex()
            buffer.pos(matrices, maxX, minY, maxZ).color(color).endVertex()
            buffer.pos(matrices, minX, minY, maxZ).color(color).endVertex()
            buffer.pos(matrices, minX, minY, minZ).color(color).endVertex()

            buffer.pos(matrices, maxX, minY, maxZ).color(color).endVertex()
            buffer.pos(matrices, maxX, minY, minZ).color(color).endVertex()
            buffer.pos(matrices, maxX, maxY, minZ).color(color).endVertex()
            buffer.pos(matrices, maxX, maxY, minZ).color(color).endVertex()
            buffer.pos(matrices, maxX, maxY, maxZ).color(color).endVertex()
            buffer.pos(matrices, maxX, minY, maxZ).color(color).endVertex()

            buffer.pos(matrices, minX, minY, minZ).color(color).endVertex()
            buffer.pos(matrices, minX, minY, maxZ).color(color).endVertex()
            buffer.pos(matrices, minX, maxY, maxZ).color(color).endVertex()
            buffer.pos(matrices, minX, maxY, maxZ).color(color).endVertex()
            buffer.pos(matrices, minX, maxY, minZ).color(color).endVertex()
            buffer.pos(matrices, minX, minY, minZ).color(color).endVertex()
        }
    }


    /**
     * Writes a texture quad to the buffer. Draw must still be called manually.
     * Buffer must be created with [gg.essential.universal.UGraphics.DrawMode.QUADS]
     */
    fun drawTexture(
        matrices: UMatrixStack,
        buffer: UBufferBuilder,
        sprite: Identifier,
        x: Double,
        y: Double,
        u: Double = 0.0,
        v: Double = 0.0,
        width: Double,
        height: Double,
        textureWidth: Double = width,
        textureHeight: Double = height,
        color: Color = Color.WHITE
    ) {
        val abstractTexture = textureManager.getTexture(sprite)
        abstractTexture.setFilter(false, false)
        RenderSystem.setShaderTexture(0, abstractTexture.glTexture)
        val x2 = x + width
        val y2 = y + height
        val u1 = u / textureWidth
        val u2 = (u + width) / textureWidth
        val v1 = v / textureHeight
        val v2 = (v + height) / textureHeight
        buffer.pos(matrices, x, y, 0.0).tex(u1, v1).color(color).endVertex()
        buffer.pos(matrices, x, y2, 0.0).tex(u1, v2).color(color).endVertex()
        buffer.pos(matrices, x2, y2, 0.0).tex(u2, v2).color(color).endVertex()
        buffer.pos(matrices, x2, y, 0.0).tex(u2, v1).color(color).endVertex()
    }

    fun drawNametag(matrices: UMatrixStack, text: String, x: Double, y: Double, z: Double, shadow: Boolean = true, scale: Float = 1f, background: Boolean = true, throughWalls: Boolean = false) {
        matrices.push()
        matrices.translate(x, y + 0.5, z)
        matrices.multiply(mc.entityRenderDispatcher.rotation)
        matrices.scale(0.025f, -0.025f, 0.025f)

        matrices.scale(scale, scale, scale)
        val centerPos = UGraphics.getStringWidth(text) / -2f
        val backgroundColor = if (!background) 0 else (mc.options.getTextBackgroundOpacity(0.25f) * 255).toInt() shl 24
        mc.textRenderer.draw(
            text,
            centerPos,
            0f,
            Colors.WHITE,
            shadow,
            matrices.peek().model,
            mc.bufferBuilders.entityVertexConsumers,
            if (throughWalls) TextRenderer.TextLayerType.SEE_THROUGH else TextRenderer.TextLayerType.NORMAL,
            backgroundColor,
            15728880
        )
        matrices.pop()
    }

    fun drawItemOnGUI(matrices: UMatrixStack, stack: ItemStack, x: Double, y: Double, z: Double = 0.0) {
        if (stack.isEmpty) return
        matrices.push()
        matrices.translate(x + 8, y + 8, (150 + z))

        matrices.scale(16.0f, -16.0f, 16.0f)

        mc.itemRenderer.renderItem(stack, ItemDisplayContext.GUI, 15728880, OverlayTexture.DEFAULT_UV, matrices.toMC(), mc.bufferBuilders.entityVertexConsumers, mc.world, 0)

        matrices.pop()
    }
}