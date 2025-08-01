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

package gg.skytils.event.mixins.item;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import gg.skytils.event.EventsKt;
import gg.skytils.event.impl.item.ItemTooltipEvent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

import java.util.List;

@Mixin(ItemStack.class)
public class MixinItemStack {
    @ModifyReturnValue(method = "getTooltip", at = @At("RETURN"), slice = @Slice(from = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z")))
    private List<Text> getTooltip(List<Text> original, @Local(argsOnly = true) TooltipType type) {
        ItemTooltipEvent event = new ItemTooltipEvent((ItemStack) (Object) this, original, type.isAdvanced());
        EventsKt.postSync(event);
        return event.getTooltip();
    }
}
