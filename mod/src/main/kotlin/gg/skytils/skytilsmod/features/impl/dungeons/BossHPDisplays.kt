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
import gg.skytils.skytilsmod.features.impl.dungeons.DungeonFeatures.dungeonFloorNumber
import gg.skytils.skytilsmod.utils.RenderUtil
import gg.skytils.skytilsmod.utils.Utils
import gg.skytils.skytilsmod.utils.graphics.ScreenRenderer
import gg.skytils.skytilsmod.utils.graphics.SmartFontRenderer.TextAlignment
import gg.skytils.skytilsmod.utils.graphics.colors.CommonColors
import gg.skytils.skytilsmod.utils.stripControlCodes
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.util.IChatComponent
import net.minecraft.util.Vec3
import java.awt.Color

object BossHPDisplays : EventSubscriber {
    private var canGiantsSpawn = false
    private var giantNames = emptyList<Pair<IChatComponent, Vec3>>()
    private var guardianRespawnTimers = emptyList<String>()
    private val guardianNameRegex = Regex("§c(Healthy|Reinforced|Chaos|Laser) Guardian §e0§c❤")
    private val timerRegex = Regex("§c ☠ §7 (.+?) §c ☠ §7")

    init {
        GiantHPElement()
        GuardianRespawnTimer()
    }

    fun onChat(event: ChatMessageReceivedEvent) {
        if (!Utils.inDungeons) return
        val unformatted = event.message.unformattedText.stripControlCodes()
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
        guardianRespawnTimers = emptyList()
    }

    fun onTick(event: gg.skytils.event.impl.TickEvent) {
        if (!Utils.inDungeons) return
        val world = mc.theWorld

        if (world != null && canGiantsSpawn && (Skytils.config.showGiantHPAtFeet || Skytils.config.showGiantHP)) {
            val hasSadanPlayer = world.playerEntities.any {
                "Sadan " == it.name
                //#if MC>=11602
                //$$ .string
                //#endif
            }
            giantNames = world.loadedEntityList.filterIsInstance<EntityArmorStand>().filter {
                val name = it.displayName?.unformattedText ?: return@filter false
                name.contains("❤") && (!hasSadanPlayer && name.contains("﴾ Sadan") || (name.contains("Giant") && dungeonFloorNumber?.let { it >= 6 } == true) || GiantHPElement.GIANT_NAMES.any {
                    name.contains(
                        it
                    )
                })
            }.mapNotNull { entity ->
                entity.displayName?.let { name ->
                    Pair(name, entity.positionVector.addVector(0.0, -10.0, 0.0))
                }
            }
        } else giantNames = emptyList()

        if (Skytils.config.showGuardianRespawnTimer && DungeonFeatures.hasBossSpawned && dungeonFloorNumber == 3 && world != null) {
            guardianRespawnTimers = mutableListOf<String>().apply {
                for (entity in world.loadedEntityList) {
                    if (size >= 4) break
                    if (entity !is EntityArmorStand) continue
                    val name = entity.customNameTag
                    if (name.startsWith("§c ☠ §7 ") && name.endsWith(" §c ☠ §7")) {
                        val nameTag = mc.theWorld.getEntitiesWithinAABB(
                            EntityArmorStand::class.java,
                            entity.entityBoundingBox.expand(2.0, 5.0, 2.0)
                        ).find {
                            it.customNameTag.endsWith(" Guardian §e0§c❤")
                        } ?: continue
                        guardianNameRegex.find(nameTag.customNameTag)?.let {
                            timerRegex.find(name)?.let {
                                add("${it.groupValues[1]}: ${it.groupValues[1]}")
                            }
                        }
                    }
                }
            }
        }
    }

    fun onRenderWorld(event: WorldDrawEvent) {
        if (!Utils.inDungeons || !Skytils.config.showGiantHPAtFeet) return
        val matrixStack = UMatrixStack()
        GlStateManager.disableCull()
        GlStateManager.disableDepth()
        for ((name, pos) in giantNames) {
            RenderUtil.drawLabel(
                pos,
                name.formattedText,
                Color.WHITE,
                event.partialTicks,
                matrixStack
            )
        }
        GlStateManager.enableDepth()
        GlStateManager.enableCull()
    }

    class GuardianRespawnTimer : GuiElement("Guardian Respawn Timer", x = 200, y = 30) {
        override fun render() {
            if (toggled && guardianRespawnTimers.isNotEmpty()) {
                RenderUtil.drawAllInList(this, guardianRespawnTimers)
            }
        }

        override fun demoRender() {

            val leftAlign = scaleX < sr.scaledWidth / 2f
            val alignment = if (leftAlign) TextAlignment.LEFT_RIGHT else TextAlignment.RIGHT_LEFT
            ScreenRenderer.fontRenderer.drawString(
                "Guardian Respawn Timer Here",
                if (leftAlign) 0f else 0f + width,
                0f,
                CommonColors.WHITE,
                alignment,
                textShadow
            )
        }

        override val height: Int
            get() = ScreenRenderer.fontRenderer.FONT_HEIGHT
        override val width: Int
            get() = ScreenRenderer.fontRenderer.getStringWidth("Guardian Respawn Timer Here")

        override val toggled: Boolean
            get() = Skytils.config.showGuardianRespawnTimer

        init {
            Skytils.guiManager.registerElement(this)
        }
    }

    class GiantHPElement : GuiElement("Show Giant HP", x = 200, y = 30) {
        override fun render() {
            if (toggled && giantNames.isNotEmpty()) {
                RenderUtil.drawAllInList(
                    this,
                    (giantNames.takeIf { it.size == 1 } ?: giantNames.filter { !it.first.unformattedText.contains("Sadan") }).map {
                        it.first.formattedText
                    }
                )
            }
        }

        override fun demoRender() {
            RenderUtil.drawAllInList(this, GIANT_NAMES.map { "$it §a19.5M§c❤" })
        }

        override val height: Int
            get() = ScreenRenderer.fontRenderer.FONT_HEIGHT * GIANT_NAMES.size
        override val width: Int
            get() = ScreenRenderer.fontRenderer.getStringWidth("§3§lThe Diamond Giant §a19.5M§c❤")

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