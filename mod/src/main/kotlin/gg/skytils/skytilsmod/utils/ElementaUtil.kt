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

package gg.skytils.skytilsmod.utils

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.input.UITextInput
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.events.UIClickEvent
import gg.essential.elementa.unstable.util.selfAndParents
import gg.essential.universal.UKeyboard
import gg.essential.vigilance.gui.settings.SettingComponent
import java.awt.Color

/**
 * When the tab key is pressed in this text field, [other] will grab the window's focus.
 */
fun UITextInput.setTabTarget(other: UIComponent) {
    onKeyType { _, keyCode ->
        if (keyCode == UKeyboard.KEY_TAB) other.grabWindowFocus()
    }
}

/**
 * Limit this [UITextInput] to only allow numerical characters and minus signs ("-")
 */
fun UITextInput.limitToNumericalCharacters() = apply {
    onKeyType { _, _ ->
        setText(getText().filter { c -> c.isDigit() || c == '-' })
    }
}

/**
 * Sets this [UITextInput] to [invalidColor] when a non-numeric value is entered and focus is lost.
 * Resets to the default color when the input is made valid.
 */
fun UITextInput.colorIfNumeric(validColor: Color, invalidColor: Color) = apply {
    onKeyType { _, _ -> setColor(if (getText().isInteger()) validColor else invalidColor) }
}

/**
 * Gets the children of a UIContainer excluding all Vigilance components.
 * Used to get a list of all waypoints in a category, because categories contain
 * controls as well as multiple [UIContainer] objects for the category's waypoints.
 */
val UIComponent.childContainers
    get() = this.childrenOfType<UIContainer>().filter { it !is SettingComponent }

/**
 * Determines whether a mouse click was consumed or not
 */
fun UIComponent.clickMouse(x: Double, y: Double, button: Int): Boolean {
    var consumed = false
    val finalListener: UIComponent.(UIClickEvent) -> Unit = { event ->
        if (event.propagationStopped && event.target != this@clickMouse) {
            consumed = true
        }
    }
    val target = hitTest(x.toFloat(), y.toFloat())
    target.selfAndParents().forEach { component ->
        component.mouseClickListeners.add(finalListener)
    }
    mouseClick(x, y, button)
    target.selfAndParents().forEach { component ->
        component.mouseClickListeners.remove(finalListener)
    }

    return consumed
}