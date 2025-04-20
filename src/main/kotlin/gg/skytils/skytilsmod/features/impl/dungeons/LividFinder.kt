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

import gg.essential.universal.UChat
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.Companion.mc
import gg.skytils.skytilsmod.core.structure.GuiElement
import gg.skytils.skytilsmod.core.tickTask
import gg.skytils.skytilsmod.core.tickTimer
import gg.skytils.skytilsmod.events.impl.BlockChangeEvent
import gg.skytils.skytilsmod.utils.RenderUtil
import gg.skytils.skytilsmod.utils.Utils
import gg.skytils.skytilsmod.utils.graphics.ScreenRenderer
import gg.skytils.skytilsmod.utils.graphics.SmartFontRenderer
import gg.skytils.skytilsmod.utils.graphics.colors.CommonColors
import gg.skytils.skytilsmod.utils.printDevMessage
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.sync.Mutex
import net.minecraft.block.BlockStainedGlass
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.item.EnumDyeColor
import net.minecraft.potion.Potion
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumChatFormatting
import net.minecraft.world.World
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import java.awt.Color

object LividFinder {
    private var foundLivid = false
    var livid: Entity? = null
    private var lividTag: Entity? = null
    private val lividBlock = BlockPos(13, 107, 25)
    private var lock = Mutex()

    //https://wiki.hypixel.net/Livid
    val dyeToChar: Map<EnumDyeColor, EnumChatFormatting> = mapOf(
        EnumDyeColor.WHITE to EnumChatFormatting.WHITE,
        EnumDyeColor.MAGENTA to EnumChatFormatting.LIGHT_PURPLE,
        EnumDyeColor.RED to EnumChatFormatting.RED,
        EnumDyeColor.GRAY to EnumChatFormatting.GRAY,
        EnumDyeColor.GREEN to EnumChatFormatting.DARK_GREEN,
        EnumDyeColor.LIME to EnumChatFormatting.GREEN,
        EnumDyeColor.BLUE to EnumChatFormatting.BLUE,
        EnumDyeColor.PURPLE to EnumChatFormatting.DARK_PURPLE,
        EnumDyeColor.YELLOW to EnumChatFormatting.YELLOW
    )

    val charToName: Map<EnumChatFormatting, String> = mapOf(
        EnumChatFormatting.YELLOW to "Arcade",
        EnumChatFormatting.WHITE to "Vendetta",
        EnumChatFormatting.GRAY to "Doctor",
        EnumChatFormatting.DARK_GREEN to "Frog",
        EnumChatFormatting.DARK_PURPLE to "Purple",
        EnumChatFormatting.RED to "Hockey",
        EnumChatFormatting.LIGHT_PURPLE to "Crossed",
        EnumChatFormatting.GREEN to "Smile",
        EnumChatFormatting.BLUE to "Scream"
    )

    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START || mc.thePlayer == null || !Utils.inDungeons || DungeonFeatures.dungeonFloorNumber != 5 || !DungeonFeatures.hasBossSpawned || !Skytils.config.findCorrectLivid) return

        val blindnessDuration = mc.thePlayer.getActivePotionEffect(Potion.blindness)?.duration
        if ((!foundLivid || DungeonFeatures.dungeonFloor == "M5") && blindnessDuration != null) {
            if (lock.tryLock()) {
                printDevMessage("Starting livid job", "livid")
                tickTimer(blindnessDuration) {
                    runCatching {
                        if (mc.thePlayer.ticksExisted > blindnessDuration) {
                            val state = mc.theWorld.getBlockState(lividBlock)
                            val color = state.getValue(BlockStainedGlass.COLOR)
                            val mapped = dyeToChar[color]
                            getLivid(color, mapped)
                        } else printDevMessage("Player changed worlds?", "livid")
                    }
                    lock.unlock()
                }
            } else printDevMessage("Livid job already started", "livid")
        }

        if (lividTag?.isDead == true || livid?.isDead == true) {
            printDevMessage("Livid is dead?", "livid")
        }
    }

    @SubscribeEvent
    fun onBlockChange(event: BlockChangeEvent) {
        if (mc.thePlayer == null || !Utils.inDungeons || DungeonFeatures.dungeonFloorNumber != 5 || !DungeonFeatures.hasBossSpawned || !Skytils.config.findCorrectLivid) return
        if (event.pos == lividBlock) {
            printDevMessage("Livid block changed", "livid")
            printDevMessage("block detection started", "livid")
            val color = event.update.getValue(BlockStainedGlass.COLOR)
            val mapped = dyeToChar[color]
            printDevMessage({ "before blind ${color}" }, "livid")
            val blindnessDuration = mc.thePlayer.getActivePotionEffect(Potion.blindness)?.duration
            tickTimer(blindnessDuration ?: 2) {
                getLivid(color, mapped)
                printDevMessage("block detection done", "livid")
            }
        }
    }

    @SubscribeEvent
    fun onRenderLivingPre(event: RenderLivingEvent.Pre<*>) {
        if (!Utils.inDungeons) return
        if ((event.entity == lividTag) || (lividTag == null && event.entity == livid)) {
            val (x, y, z) = RenderUtil.fixRenderPos(event.x, event.y, event.z)
            val aabb = livid?.entityBoundingBox ?: AxisAlignedBB(
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

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Unload) {
        lividTag = null
        livid = null
        foundLivid = false
    }

    fun getLivid(blockColor: EnumDyeColor, mappedColor: EnumChatFormatting?) {
        val lividType = charToName[mappedColor]
        if (lividType == null) {
            UChat.chat("${Skytils.failPrefix} §cBlock color ${blockColor.name} is not mapped correctly. Please report this to discord.gg/skytils")
            return
        }

        for (entity in mc.theWorld.loadedEntityList) {
            if (entity !is EntityArmorStand) continue
            if (entity.customNameTag.startsWith("$mappedColor﴾ $mappedColor§lLivid")) {
                lividTag = entity
                livid = mc.theWorld.playerEntities.find { it.name == "$lividType Livid" }
                foundLivid = true
                return
            }
        }
        printDevMessage("No livid found!", "livid")
    }

    internal class LividGuiElement : GuiElement("Livid HP", x = 0.05f, y = 0.4f) {
        override fun render() {
            val player = mc.thePlayer
            val world: World? = mc.theWorld
            if (toggled && Utils.inDungeons && player != null && world != null) {
                if (lividTag == null) return

                val leftAlign = scaleX < sr.scaledWidth / 2f
                val alignment = if (leftAlign) SmartFontRenderer.TextAlignment.LEFT_RIGHT else SmartFontRenderer.TextAlignment.RIGHT_LEFT
                ScreenRenderer.fontRenderer.drawString(
                    lividTag!!.name.replace("§l", ""),
                    if (leftAlign) 0f else width.toFloat(),
                    0f,
                    CommonColors.WHITE,
                    alignment,
                    textShadow
                )
            }
        }

        override fun demoRender() {
            val leftAlign = scaleX < sr.scaledWidth / 2f
            val text = "§r§f﴾ Livid §e6.9M§c❤ §f﴿"
            val alignment = if (leftAlign) SmartFontRenderer.TextAlignment.LEFT_RIGHT else SmartFontRenderer.TextAlignment.RIGHT_LEFT
            ScreenRenderer.fontRenderer.drawString(
                text,
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
            get() = ScreenRenderer.fontRenderer.getStringWidth("§r§f﴾ Livid §e6.9M§c❤ §f﴿")

        override val toggled: Boolean
            get() = Skytils.config.findCorrectLivid

        init {
            Skytils.guiManager.registerElement(this)
        }
    }

    init {
        LividGuiElement()
    }
}