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
import gg.skytils.event.impl.TickEvent
import gg.skytils.event.impl.play.ChatMessageReceivedEvent
import gg.skytils.event.impl.play.WorldUnloadEvent
import gg.skytils.event.impl.render.WorldDrawEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.utils.*
import gg.essential.elementa.layoutdsl.LayoutScope
import gg.essential.elementa.state.v2.MutableState
import gg.essential.elementa.state.v2.State
import gg.essential.elementa.state.v2.mutableStateOf
import gg.skytils.skytilsmod.core.structure.v2.HudElement
import gg.skytils.skytilsmod.gui.layout.text
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.util.math.BlockPos

object SpidersDenFeatures : EventSubscriber {
    private var shouldShowArachneSpawn = false
    private var arachneName: MutableState<String?> = mutableStateOf(null)

    init {
        Skytils.guiManager.registerElement(ArachneHPHud())
    }

    override fun setup() {
        register(::onTick)
        register(::onChat)
        register(::onWorldRender)
        register(::onWorldChange)
    }

    fun onTick(event: TickEvent) {
        arachneName.set(
            if (!Utils.inSkyblock || SBInfo.mode != SkyblockIsland.SpiderDen.mode || !Skytils.config.showArachneHP.getUntracked()) null else mc.world?.entities?.find {
                val name = it.displayName?.formattedText ?: return@find false
                it is ArmorStandEntity && name.endsWith("§c❤") && (name.contains("§cArachne §") || name.contains("§5Runic Arachne §"))
            }?.displayName?.formattedText
        )
    }

    fun onChat(event: ChatMessageReceivedEvent) {
        if (!Utils.inSkyblock) return
        val unformatted = event.message.string.stripControlCodes()
        if (unformatted.startsWith("☄") && (unformatted.contains("placed an Arachne Fragment! (") || unformatted.contains(
                "placed an Arachne Crystal! Something is awakening!"
            ))
        ) {
            shouldShowArachneSpawn = true
        }
        if (unformatted.trim().startsWith("ARACHNE DOWN!")) {
            shouldShowArachneSpawn = false
        }
    }

    fun onWorldRender(event: WorldDrawEvent) {
        if (shouldShowArachneSpawn && Skytils.config.showArachneSpawn) {
            val spawnPos = BlockPos(-282, 49, -178)
            val matrixStack = UMatrixStack()
            GlStateManager._disableDepthTest()
            GlStateManager._disableCull()
            RenderUtil.renderWaypointText("Arachne Spawn", spawnPos, event.partialTicks, matrixStack)
            // disable lighting
            GlStateManager._enableDepthTest()
            GlStateManager._enableCull()
        }
    }

    fun onWorldChange(event: WorldUnloadEvent) {
        shouldShowArachneSpawn = false
        arachneName.set { null }
    }

    class ArachneHPHud : HudElement("Show Arachne HP", 200f, 30f) {
        override val toggleState: State<Boolean>
            get() = Skytils.config.showArachneHP
        override fun LayoutScope.render() {
            ifNotNull(arachneName) { name ->
                text(name)
            }
        }

        override fun LayoutScope.demoRender() {
            text("§8[§7Lv500§8] §cArachne §a17.6M§f/§a20M§c❤§r")
        }

    }
}