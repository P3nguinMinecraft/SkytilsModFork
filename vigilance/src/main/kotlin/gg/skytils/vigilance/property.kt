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

package gg.skytils.vigilance

import gg.essential.elementa.unstable.state.v2.*
import gg.essential.vigilance.Vigilant
import gg.essential.vigilance.data.*

class StateBackedPropertyValue<T>(internal val state: MutableState<T>) : PropertyValue() {

    override fun getValue(instance: Vigilant): Any? {
        return state.getUntracked()
    }

    override fun setValue(value: Any?, instance: Vigilant) {
        @Suppress("unchecked_cast")
        state.set(value as T)
    }

}

fun <T> Vigilant.property(
    attributes: PropertyAttributesExt,
    defaultValue: T
): MutableState<T> {
    val state: MutableState<T> = mutableStateOf(defaultValue)
    registerProperty(
        PropertyData(
            attributes,
            StateBackedPropertyValue(state),
            this
        )
    )
    return state
}