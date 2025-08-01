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

package gg.skytils.skytilsmod.features.impl.dungeons

import gg.essential.elementa.unstable.layoutdsl.LayoutScope
import gg.essential.elementa.unstable.state.v2.MutableState
import gg.essential.elementa.unstable.state.v2.State
import gg.essential.elementa.unstable.state.v2.mutableStateOf
import gg.essential.universal.UChat
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.TickEvent
import gg.skytils.event.impl.play.WorldUnloadEvent
import gg.skytils.event.impl.render.LivingEntityPreRenderEvent
import gg.skytils.event.impl.world.BlockStateUpdateEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.core.structure.v2.HudElement
import gg.skytils.skytilsmod.core.tickTimer
import gg.skytils.skytilsmod.gui.layout.text
import gg.skytils.skytilsmod.utils.*
import gg.skytils.skytilsmod.utils.printDevMessage
import kotlinx.coroutines.sync.Mutex
import net.minecraft.block.BlockState
import net.minecraft.entity.Entity
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.util.DyeColor
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.util.math.Box
import net.minecraft.util.math.BlockPos
import net.minecraft.util.Formatting
import java.awt.Color

object LividFinder : EventSubscriber {
    private var foundLivid = false
    var livid: Entity? = null
    private val lividTag: MutableState<Entity?> = mutableStateOf(null)
    private val lividBlock = BlockPos(13, 107, 25)
    private var lock = Mutex()

    //https://wiki.hypixel.net/Livid
    val dyeToChar: Map<DyeColor, Formatting> = mapOf(
        DyeColor.WHITE to Formatting.WHITE,
        DyeColor.MAGENTA to Formatting.LIGHT_PURPLE,
        DyeColor.RED to Formatting.RED,
        DyeColor.GRAY to Formatting.GRAY,
        DyeColor.GREEN to Formatting.DARK_GREEN,
        DyeColor.LIME to Formatting.GREEN,
        DyeColor.BLUE to Formatting.BLUE,
        DyeColor.PURPLE to Formatting.DARK_PURPLE,
        DyeColor.YELLOW to Formatting.YELLOW
    )

    val charToName: Map<Formatting, String> = mapOf(
        Formatting.YELLOW to "Arcade",
        Formatting.WHITE to "Vendetta",
        Formatting.GRAY to "Doctor",
        Formatting.DARK_GREEN to "Frog",
        Formatting.DARK_PURPLE to "Purple",
        Formatting.RED to "Hockey",
        Formatting.LIGHT_PURPLE to "Crossed",
        Formatting.GREEN to "Smile",
        Formatting.BLUE to "Scream"
    )

    override fun setup() {
        register(::onTick)
        register(::onBlockChange)
        register(::onRenderLivingPre)
        register(::onWorldChange)
    }

    fun onTick(event: TickEvent) {
        if (mc.player == null || !Utils.inDungeons || DungeonFeatures.dungeonFloorNumber != 5 || !DungeonFeatures.hasBossSpawned || !Skytils.config.findCorrectLivid) return
        val blindnessDuration = mc.player?.getStatusEffect(StatusEffects.BLINDNESS)?.duration
        if ((!foundLivid || DungeonFeatures.dungeonFloor == "M5") && blindnessDuration != null) {
            if (lock.tryLock()) {
                printDevMessage("Starting livid job", "livid")
                tickTimer(blindnessDuration) {
                    runCatching {
                        if ((mc.player?.age ?: 0) > blindnessDuration) {
                            val state = mc.world?.getBlockState(lividBlock) ?: return@runCatching
                            val color = state.dyeColor ?: return@runCatching
                            val mapped = dyeToChar[color]
                            getLivid(color, mapped)
                        } else printDevMessage("Player changed worlds?", "livid")
                    }
                    lock.unlock()
                }
            } else printDevMessage("Livid job already started", "livid")
        }

        if (lividTag.getUntracked()?.isRemoved == true || livid?.isRemoved == true) {
            printDevMessage("Livid is dead?", "livid")
        }
    }

    fun onBlockChange(event: BlockStateUpdateEvent) {
        if (mc.player == null || !Utils.inDungeons || DungeonFeatures.dungeonFloorNumber != 5 || !DungeonFeatures.hasBossSpawned || !Skytils.config.findCorrectLivid) return
        if (event.pos == lividBlock) {
            printDevMessage("Livid block changed", "livid")
            printDevMessage("block detection started", "livid")
            val color = event.update.dyeColor ?: return
            val mapped = dyeToChar[color]
            printDevMessage({ "before blind $color" }, "livid")
            val blindnessDuration = mc.player?.getStatusEffect(StatusEffects.BLINDNESS)?.duration
            tickTimer(blindnessDuration ?: 2) {
                getLivid(color, mapped)
                printDevMessage("block detection done", "livid")
            }
        }
    }

    fun onRenderLivingPre(event: LivingEntityPreRenderEvent<*, *, *>) {
        if (!Utils.inDungeons) return
        val lividTag = lividTag.getUntracked()
        if ((event.entity == lividTag) || (lividTag == null && event.entity == livid)) {
            val (x, y, z) = RenderUtil.fixRenderPos(event.x, event.y, event.z)
            val aabb = livid?.boundingBox ?: Box(
                x - 0.5,
                y - 2,
                z - 0.5,
                x + 0.5,
                y,
                z + 0.5
            )

            RenderUtil.drawOutlinedBoundingBox(
                aabb,
                Color(255, 107, 11, 255),
                3f,
                RenderUtil.getPartialTicks()
            )
        }
    }
    fun onWorldChange(event: WorldUnloadEvent) {
        lividTag.set(null)
        livid = null
        foundLivid = false
    }

    fun getLivid(blockColor: DyeColor, mappedColor: Formatting?) {
        val lividType = charToName[mappedColor]
        if (lividType == null) {
            UChat.chat("${Skytils.failPrefix} §cBlock color ${blockColor.name} is not mapped correctly. Please report this to discord.gg/skytils")
            return
        }

        mc.world?.entities?.forEach { entity ->
            if (entity !is ArmorStandEntity) return@forEach
            if (entity.customName?.formattedText?.startsWith("$mappedColor﴾ $mappedColor§lLivid") == true) {
                lividTag.set(entity)
                livid = mc.world?.players?.find { it.name.string == "$lividType Livid" }
                foundLivid = true
                return
            }
        }
        printDevMessage("No livid found!", "livid")
    }

    private val BlockState.dyeColor
        get() = block.defaultMapColor.let { mapColor -> DyeColor.entries.find { it.mapColor == mapColor } }

    private class LividHud : HudElement("Livid HP", 0.05, 0.4) {
        override val toggleState: State<Boolean>
            get() = Skytils.config.findCorrectLividState

        override fun LayoutScope.render() {
            if_(SBInfo.dungeonsState) {
                ifNotNull(lividTag) { lividTag ->
                    text(lividTag.name.formattedText.replace("§l", ""))
                }
            }
        }

        override fun LayoutScope.demoRender() {
            text("§r§f﴾ Livid §e6.9M§c❤ §f﴿")
        }

    }

    init {
        Skytils.guiManager.registerElement(LividHud())
    }
}