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

package gg.skytils.skytilsmod.mixins.transformers.entity;

import gg.skytils.skytilsmod.mixins.extensions.ExtensionEntity;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public class MixinEntity_GlowExtension implements ExtensionEntity {

    @Unique
    private boolean skytils$forceGlow = false;

    @Unique
    private Integer skytils$glowColorOverride = null;

    @Override
    public boolean getSkytilsForceGlow() {
        return skytils$forceGlow;
    }

    @Override
    public void setSkytilsForceGlow(boolean b) {
        this.skytils$forceGlow = b;
    }

    @Override
    public @Nullable Integer getSkytilsGlowColorOverride() {
        return skytils$glowColorOverride;
    }

    @Override
    public void setSkytilsGlowColorOverride(@Nullable Integer integer) {
        this.skytils$glowColorOverride = integer;
    }
}
