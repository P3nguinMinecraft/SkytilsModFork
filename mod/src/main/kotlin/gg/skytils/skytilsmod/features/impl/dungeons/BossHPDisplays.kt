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
package gg.skytils.skytilsmod.features.impl.dungeons

import gg.essential.elementa.layoutdsl.LayoutScope
import gg.essential.elementa.layoutdsl.column
import gg.essential.elementa.state.v2.clear
import gg.essential.elementa.state.v2.mutableListStateOf
import gg.essential.elementa.state.v2.setAll
import gg.essential.universal.UMatrixStack
import gg.skytils.event.EventPriority
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.play.ChatMessageReceivedEvent
import gg.skytils.event.impl.play.WorldUnloadEvent
import gg.skytils.event.impl.render.WorldDrawEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.core.structure.GuiElement
import gg.skytils.skytilsmod.core.structure.v2.HudElement
import gg.skytils.skytilsmod.features.impl.dungeons.DungeonFeatures.dungeonFloorNumber
import gg.skytils.skytilsmod.gui.layout.text
import gg.skytils.skytilsmod.utils.RenderUtil
import gg.skytils.skytilsmod.utils.Utils
import gg.skytils.skytilsmod.utils.graphics.ScreenRenderer
import gg.skytils.skytilsmod.utils.graphics.SmartFontRenderer.TextAlignment
import gg.skytils.skytilsmod.utils.graphics.colors.CommonColors
import gg.skytils.skytilsmod.utils.stripControlCodes
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d
import java.awt.Color

//#if MC>12000
import gg.skytils.skytilsmod.utils.formattedText
import net.minecraft.entity.EntityType
import com.mojang.blaze3d.opengl.GlStateManager

//#endif

object BossHPDisplays : EventSubscriber {
    private var canGiantsSpawn = false
    private var giantNames = emptyList<Pair<Text, Vec3d>>()
    private var guardianRespawnTimers = mutableListStateOf<String>()
    private val guardianNameRegex = Regex("§c(Healthy|Reinforced|Chaos|Laser) Guardian §e0§c❤")
    private val timerRegex = Regex("§c ☠ §7 (.+?) §c ☠ §7")

    init {
        Skytils.guiManager.registerElement(GuardianRespawnTimer())
        GiantHPElement()
    }

    fun onChat(event: ChatMessageReceivedEvent) {
        if (!Utils.inDungeons) return
        val unformatted = event.message.string.stripControlCodes()
        if (unformatted.startsWith("[BOSS] Sadan")) {
            if (unformatted.contains("My giants! Unleashed!")) {
                canGiantsSpawn = true
            } else if (unformatted.contains("It was inevitable.") || unformatted.contains("NOOOOOOOOO")) {
                canGiantsSpawn = false
            }
        } else if (unformatted == "[BOSS] The Watcher: Plus I needed to give my new friends some space to roam...") {
            canGiantsSpawn = true
        } else if (unformatted.startsWith("[BOSS] The Watcher: You have failed to prove yourself, and have paid with your lives.") || unformatted.startsWith(
                "[BOSS] The Watcher: You have proven yourself"
            )
        ) {
            canGiantsSpawn = false
        }
    }

    fun onWorldChange(event: WorldUnloadEvent) {
        canGiantsSpawn = false
        giantNames = emptyList()
        guardianRespawnTimers.clear()
    }

    fun onTick(event: gg.skytils.event.impl.TickEvent) {
        if (!Utils.inDungeons) return
        val world = mc.world

        if (world != null && canGiantsSpawn && (Skytils.config.showGiantHPAtFeet || Skytils.config.showGiantHP)) {
            val hasSadanPlayer = world.players.any {
                "Sadan " == it.name
                //#if MC>=11602
                .string
                //#endif
            }
            giantNames = world.entities.filterIsInstance<ArmorStandEntity>().filter {
                val name = it.displayName?.string ?: return@filter false
                name.contains("❤") && (!hasSadanPlayer && name.contains("﴾ Sadan") || (name.contains("Giant") && dungeonFloorNumber?.let { it >= 6 } == true) || GiantHPElement.GIANT_NAMES.any {
                    name.contains(
                        it
                    )
                })
            }.mapNotNull { entity ->
                entity.displayName?.let { name ->
                    Pair(name, entity.pos.add(0.0, -10.0, 0.0))
                }
            }
        } else giantNames = emptyList()

        if (Skytils.config.showGuardianRespawnTimer && DungeonFeatures.hasBossSpawned && dungeonFloorNumber == 3 && world != null) {
            guardianRespawnTimers.setAll(mutableListOf<String>().apply {
                for (entity in world.entities) {
                    if (size >= 4) break
                    if (entity !is ArmorStandEntity) continue
                    val name = entity.customName
                    //#if MC>12000
                        ?.formattedText
                    //#endif
                        ?: continue
                    if (name.startsWith("§c ☠ §7 ") && name.endsWith(" §c ☠ §7")) {
                        val nameTag = mc.world?.getEntitiesByType<ArmorStandEntity>(
                            EntityType.ARMOR_STAND,
                            entity.boundingBox.expand(2.0, 5.0, 2.0)
                        ) {
                            it.customName?.string?.endsWith(" Guardian §e0§c❤") == true
                        }?.firstOrNull() ?: continue
                        guardianNameRegex.find(nameTag.customName!!.string)?.let {
                            timerRegex.find(name)?.let {
                                add("${it.groupValues[1]}: ${it.groupValues[1]}")
                            }
                        }
                    }
                }
            })
        }
    }

    fun onRenderWorld(event: WorldDrawEvent) {
        if (!Utils.inDungeons || !Skytils.config.showGiantHPAtFeet) return
        val matrixStack = UMatrixStack()
        GlStateManager._disableCull()
        GlStateManager._disableDepthTest()
        for ((name, pos) in giantNames) {
            RenderUtil.drawLabel(
                pos,
                name.formattedText,
                Color.WHITE,
                event.partialTicks,
                matrixStack
            )
        }
        GlStateManager._enableCull()
        GlStateManager._enableDepthTest()
    }

    class GuardianRespawnTimer : HudElement("Guardian Respawn Timer", x = 200f, y = 30f) {
        override fun LayoutScope.render() {
            column {
                forEach(guardianRespawnTimers) { timer ->
                    text(timer)
                }
            }
        }

        override fun LayoutScope.demoRender() {
            text("Guardian Respawn Timer Here")
        }

    }

    class GiantHPElement : GuiElement("Show Giant HP", x = 200, y = 30) {
        override fun render() {
            if (toggled && giantNames.isNotEmpty()) {
                RenderUtil.drawAllInList(
                    this,
                    (giantNames.takeIf { it.size == 1 } ?: giantNames.filter { !it.first.string.contains("Sadan") }).map {
                        it.first.method_10865()
                    }
                )
            }
        }

        override fun demoRender() {
            RenderUtil.drawAllInList(this, GIANT_NAMES.map { "$it §a19.5M§c❤" })
        }

        override val height: Int
            get() = ScreenRenderer.fontRenderer.field_0_2811 * GIANT_NAMES.size
        override val width: Int
            get() = ScreenRenderer.fontRenderer.getWidth("§3§lThe Diamond Giant §a19.5M§c❤")

        override val toggled: Boolean
            get() = Skytils.config.showGiantHP

        companion object {
            val GIANT_NAMES =
                setOf(
                    "§3§lThe Diamond Giant",
                    "§c§lBigfoot",
                    "§4§lL.A.S.R.",
                    "§d§lJolly Pink Giant",
                    "§d§lMutant Giant"
                )
        }

        init {
            Skytils.guiManager.registerElement(this)
        }
    }

    override fun setup() {
        register(::onChat, EventPriority.Highest)
        register(::onWorldChange)
        register(::onTick)
        register(::onRenderWorld)
    }
}