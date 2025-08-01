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

package gg.skytils.skytilsmod.mixins.transformers.item;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gg.skytils.skytilsmod.features.impl.dungeons.DungeonFeatures;
import gg.skytils.skytilsmod.utils.Utils;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BowItem.class)
public abstract class MixinItemBow {
/*    @WrapOperation(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;method_0_7937(Lnet/minecraft/item/Item;)Z"))
    private boolean onItemRightClick$hasItem(PlayerInventory instance, Item itemIn, Operation<Boolean> original) {
        boolean hasItem = original.call(instance, itemIn);

        if (itemIn != Items.ARROW || hasItem || !Utils.INSTANCE.getInDungeons()) return hasItem;

        ItemStack intended = DungeonFeatures.INSTANCE.getIntendedItemStack();
        ItemStack fake = DungeonFeatures.INSTANCE.getFakeDungeonMap();
        if (fake != null && intended != null) {
            return intended.getItem() == itemIn && instance.getStack(8) == fake;
        }

        return false;
    }*/
}
