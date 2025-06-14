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

import gg.essential.universal.UGraphics
import gg.essential.universal.render.URenderPipeline
import gg.essential.universal.shader.BlendState

object SRenderPipelines {
    private val translucentBlendState = BlendState(BlendState.Equation.ADD, BlendState.Param.SRC_ALPHA, BlendState.Param.ONE_MINUS_SRC_ALPHA, BlendState.Param.ONE)

    val guiPipeline = URenderPipeline.builderWithDefaultShader("skytils:pipeline/gui",
        UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_COLOR
    ).apply {
        blendState = translucentBlendState
        depthTest = URenderPipeline.DepthTest.LessOrEqual
    }.build()

    val guiTexturePiepline = URenderPipeline.builderWithDefaultShader("skytils:pipeline/gui_texture",
        UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_TEXTURE_COLOR
    ).apply {
        blendState = translucentBlendState
        depthTest = URenderPipeline.DepthTest.LessOrEqual
    }.build()

    val linesPipeline = URenderPipeline.builderWithDefaultShader("skytils:pipeline/lines",
        UGraphics.DrawMode.LINES, UGraphics.CommonVertexFormats.POSITION_COLOR
    ).apply {
        blendState = BlendState.ALPHA
        culling = false
    }.build()

    val noDepthLinesPipeline = URenderPipeline.builderWithDefaultShader("skytils:pipeline/no_depth_lines",
        UGraphics.DrawMode.LINES, UGraphics.CommonVertexFormats.POSITION_COLOR
    ).apply {
        depthTest = URenderPipeline.DepthTest.Always
        blendState = BlendState.ALPHA
        culling = false
    }.build()

    val noDepthBoxPipeline = URenderPipeline.builderWithDefaultShader("skytils:pipeline/no_depth_box",
        UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_COLOR
    ).apply {
        depthTest = URenderPipeline.DepthTest.Always
        blendState = BlendState.ALPHA
        culling = false
    }.build()
}