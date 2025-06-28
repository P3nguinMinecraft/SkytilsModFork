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
package gg.skytils.skytilsmod.features.impl.crimson

import gg.skytils.event.EventPriority
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.play.WorldUnloadEvent
import gg.skytils.event.impl.screen.GuiContainerForegroundDrawnEvent
import gg.skytils.event.impl.screen.GuiContainerSlotClickEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.IO
import gg.skytils.skytilsmod.core.MC
import gg.skytils.skytilsmod.features.impl.handlers.AuctionData
import gg.skytils.skytilsmod.features.impl.handlers.KuudraPriceData
import gg.skytils.skytilsmod.mixins.transformers.accessors.AccessorGuiContainer
import gg.skytils.skytilsmod.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.mojang.blaze3d.systems.RenderSystem
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.CopyConstraintFloat
import gg.essential.elementa.dsl.boundTo
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.unstable.layoutdsl.LayoutScope
import gg.essential.elementa.unstable.layoutdsl.column
import gg.essential.elementa.unstable.layoutdsl.layout
import gg.essential.elementa.unstable.state.v2.add
import gg.essential.elementa.unstable.state.v2.clear
import gg.essential.elementa.unstable.state.v2.combinators.map
import gg.essential.elementa.unstable.state.v2.mutableSetState
import gg.essential.elementa.unstable.state.v2.mutableStateOf
import gg.essential.elementa.unstable.state.v2.toList
import gg.essential.universal.UMatrixStack
import gg.skytils.event.impl.screen.ScreenOpenEvent
import gg.skytils.skytilsmod.core.structure.v2.HudElement
import gg.skytils.skytilsmod.gui.layout.text
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.screen.GenericContainerScreenHandler
import net.minecraft.item.ItemStack
import java.awt.Color
import kotlin.jvm.optionals.getOrNull


/**
 * Modified version of [gg.skytils.skytilsmod.features.impl.dungeons.DungeonChestProfit]
 */
object KuudraChestProfit : EventSubscriber {
    private val element = KuudraChestProfitHud()
    init {
        Skytils.guiManager.registerElement(element)
    }
    private val essenceRegex = Regex("§d(?<type>\\w+) Essence §8x(?<count>\\d+)")

    override fun setup() {
        register(::onGUIDrawnEvent)
        register(::onWorldChange)
        register(::onSlotClick, EventPriority.Highest)
        register(::onGuiOpen)
    }

    fun onGUIDrawnEvent(event: GuiContainerForegroundDrawnEvent) {
        if (!Skytils.config.kuudraChestProfit.getUntracked() || !KuudraFeatures.kuudraOver || KuudraFeatures.myFaction == null) return

        if (event.chestName.endsWith(" Chest")) {
            val matrixStack = UMatrixStack.Compat.get()
            matrixStack.push()
            matrixStack.translate(
                (-(event.gui as AccessorGuiContainer).guiLeft).toDouble(),
                -(event.gui as AccessorGuiContainer).guiTop.toDouble(),
                299.0
            )
            matrixStack.runWithGlobalState {
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
                // disable lighting
                val matrixStack = UMatrixStack.Compat.get()
                element.inChestComponent.drawCompat(matrixStack)
            }
            matrixStack.pop()
        }
    }

    // TODO: Ensure this rewrite works as expected
    fun onGuiOpen(event: ScreenOpenEvent) {
        if (element.currentlyDisplayingChest.getUntracked() != null && event.screen == null) {
            element.currentlyDisplayingChest.set(null)
        } else {
            if (!Skytils.config.kuudraChestProfit.getUntracked() || !KuudraFeatures.kuudraOver || KuudraFeatures.myFaction == null) return
            (event.screen as? GenericContainerScreen)?.let { container ->
                val inv = container.screenHandler.inventory
                val chestName = container.title.string
                if (chestName.endsWith(" Chest")) {
                    val chestType = KuudraChest.getFromName(chestName) ?: return
                    val openChest = inv.getStack(31) ?: return
                    if (openChest.name.formattedText == "§aOpen Reward Chest" && chestType.items.getUntracked().isEmpty()) {
                        runCatching {
                            val key = getKeyNeeded(ItemUtil.getItemLore(openChest))
                            chestType.keyNeeded = key
                            for (i in 9..17) {
                                val lootSlot = inv.getStack(i) ?: continue
                                chestType.addItem(lootSlot)
                            }
                            if (key != null && Skytils.config.kuudraChestProfitCountsKey) {
                                val faction = KuudraFeatures.myFaction ?: error("Failed to get Crimson Faction")
                                val keyCost = key.getPrice(faction)
                                chestType.items.add(KuudraChestLootItem(1, "${key.rarity.baseColor}${key.displayName} §7(${faction.color}${faction.identifier}§7)", -keyCost))
                                chestType.value.set { it - keyCost }
                            }
                        }
                    }
                    element.currentlyDisplayingChest.set(chestType)
                }
            }
        }
    }

    private fun getKeyNeeded(lore: List<String>): KuudraKey? {
        for (i in 0..<lore.size-1) {
            val line = lore[i]
            if (line == "§7Cost") {
                val cost = lore[i+1]
                if (cost == "§aThis Chest is Free!") return null
                return KuudraKey.entries.first { it.displayName == cost.stripControlCodes() }
            }
        }
        error("Could not find key needed for chest")
    }

    private fun getEssenceValue(text: String): Double? {
        if (!Skytils.config.kuudraChestProfitIncludesEssence) return null
        val groups = essenceRegex.matchEntire(text)?.groups ?: return null
        val type = groups["type"]?.value?.uppercase() ?: return null
        val count = groups["count"]?.value?.toInt() ?: return null
        return (AuctionData.lowestBINs["ESSENCE_$type"] ?: 0.0) * count
    }

    fun onWorldChange(event: WorldUnloadEvent) {
        KuudraChest.entries.forEach(KuudraChest::reset)
    }

    fun onSlotClick(event: GuiContainerSlotClickEvent) {
        if (SBInfo.mode == SkyblockIsland.KuudraHollow.mode || event.container !is GenericContainerScreenHandler) return
        if (event.slotId in 9..17 && event.chestName.endsWith(" Chest") && KuudraChest.getFromName(event.chestName) != null) {
            event.cancelled = true
        }
    }

    private enum class KuudraChest(var displayText: String, var displayColor: Color) {
        FREE("Free Chest", Color.RED),
        PAID("Paid Chest", Color.GREEN);

        var keyNeeded: KuudraKey? = null
        val value = mutableStateOf(0.0)
        val items = mutableSetState<KuudraChestLootItem>()

        fun reset() {
            keyNeeded = null
            value.set(0.0)
            items.clear()
        }

        fun addItem(item: ItemStack) {
            IO.launch {
                val identifier = AuctionData.getIdentifier(item)
                val extraAttr = ItemUtil.getExtraAttributes(item)
                var displayName = item.name.formattedText

                val itemValue = if (identifier == null) {
                    getEssenceValue(displayName) ?: return@launch
                } else if ((extraAttr?.getCompound("attributes")?.getOrNull()?.keys?.size ?: 0) > 1) {
                    val priceData = KuudraPriceData.getOrFetchAttributePricedItem(item)
                    if (priceData != null && priceData != KuudraPriceData.AttributePricedItem.EMPTY && priceData != KuudraPriceData.AttributePricedItem.FAILURE) {
                        priceData.price
                    } else {
                        if (priceData != null) {
                            displayName += "§c (Failed to fetch price ${if (priceData == KuudraPriceData.AttributePricedItem.FAILURE) "from API" else ", not on AH"})"
                        } else {
                            displayName += "§c (Failed to fetch price, using LBIN)"
                        }
                        AuctionData.lowestBINs[identifier] ?: 0.0
                    }
                } else {
                    AuctionData.lowestBINs[identifier] ?: 0.0
                }
                withContext(Dispatchers.MC) {
                    items.add(KuudraChestLootItem(item.count, displayName, itemValue))

                    value.set { it + itemValue }
                }
            }
        }

        companion object {
            fun getFromName(name: String?): KuudraChest? {
                if (name.isNullOrBlank()) return null
                return entries.find {
                    it.displayText == name
                }
            }
        }
    }

    private data class KuudraChestLootItem(var stackSize: Int, var displayText: String, var value: Double) : Comparable<KuudraChestLootItem> {
        override fun compareTo(other: KuudraChestLootItem): Int = value.compareTo(other.value)
    }
    private class KuudraChestProfitHud : HudElement("Kuudra Chest Profit", 200f, 120f) {
        override val toggleState = Skytils.config.kuudraChestProfit

        override fun LayoutScope.render() {
            if_(SBInfo.modeState.map { it == SkyblockIsland.KuudraHollow.mode }) {
                column {
                    KuudraChest.entries.forEach { chest ->
                        text({ "${chest.displayText}§f: §${(if (chest.value() > 0) "a" else "c")}${NumberUtil.format(chest.value())}" })
                    }
                }
            }
        }

        val currentlyDisplayingChest = mutableStateOf<KuudraChest?>(null)
        val inChestComponent = UIContainer().constrain {
            x = CopyConstraintFloat() boundTo component
            y = CopyConstraintFloat() boundTo component
        }.apply {
            layout {
                ifNotNull(currentlyDisplayingChest) { chest ->
                    column {
                        text(chest.displayText + "§f: §" + (if (chest.value.getUntracked() > 0) "a" else "c") + NumberUtil.nf.format(chest.value))
                        forEach(chest.items.toList()) { item ->
                            text("§8${item.stackSize} §r".toStringIfTrue(item.stackSize > 1) + item.displayText + "§f: §${if (item.value >= 0) 'a' else 'c'}" + NumberUtil.nf.format(item.value))
                        }
                    }
                }
            }
        }

        override fun LayoutScope.demoRender() {
            column {
                KuudraChest.entries.forEach { chest -> text("${chest.displayText}: §a+300M") }
            }
        }

    }

    enum class KuudraKey(val displayName: String, val rarity: ItemRarity, val coinCost: Int, val materialCost: Int) {
        BASIC("Kuudra Key", ItemRarity.RARE, 200000, 2),
        HOT("Hot Kuudra Key", ItemRarity.EPIC, 400000, 6),
        BURNING("Burning Kuudra Key", ItemRarity.EPIC, 750000, 20),
        FIERY("Fiery Kuudra Key", ItemRarity.EPIC, 1500000, 60),
        INFERNAL("Infernal Kuudra Key", ItemRarity.LEGENDARY,3000000, 120);

        companion object {
            // all keys cost 2 CORRUPTED_NETHER_STAR but nether stars are coop-soulbound
            const val starConstant = 2
        }

        // treat NPC discounts as negligible
        fun getPrice(faction: CrimsonFaction): Double {
            val keyMaterialCost = AuctionData.lowestBINs[faction.keyMaterial] ?: 0.0

            return coinCost + keyMaterialCost * materialCost
        }
    }
}