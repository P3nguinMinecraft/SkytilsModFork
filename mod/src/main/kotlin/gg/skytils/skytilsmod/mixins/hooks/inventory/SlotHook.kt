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
package gg.skytils.skytilsmod.mixins.hooks.inventory

import gg.skytils.skytilsmod.features.impl.dungeons.solvers.terminals.SelectAllColorSolver
import gg.skytils.skytilsmod.features.impl.dungeons.solvers.terminals.StartsWithSequenceSolver
import gg.skytils.skytilsmod.utils.Utils
import net.minecraft.block.entity.LockableContainerBlockEntity
import net.minecraft.component.DataComponentTypes
import net.minecraft.screen.slot.Slot
import net.minecraft.item.ItemStack
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

fun markTerminalItems(slot: Slot, cir: CallbackInfoReturnable<ItemStack?>) {
    if (!Utils.inSkyblock || slot.inventory !is LockableContainerBlockEntity) return
    val original = slot.inventory.getStack(slot.index) ?: return
    if (!original.hasEnchantments() && (SelectAllColorSolver.shouldClick.contains(slot.id) ||
                StartsWithSequenceSolver.shouldClick.contains(slot.id))
    ) {
        val item = original.copy()
        val customData = item.get(DataComponentTypes.CUSTOM_DATA)?.apply { compound ->
            compound.putBoolean("SkytilsForceGlint", true)
        }
        item.set(DataComponentTypes.CUSTOM_DATA, customData)
        cir.returnValue = item
    }
}