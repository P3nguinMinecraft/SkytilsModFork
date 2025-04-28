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

plugins {
    kotlin("jvm") apply false
    id("gg.essential.loom") version "1.9.+" apply false
    id("gg.essential.multi-version.root") apply false
}

buildscript {
    configurations {
        all {
            resolutionStrategy {
                dependencySubstitution {
                    substitute(module("com.github.replaymod:preprocessor")).using(module("com.github.skytils:preprocessor:0a04a5668b0db4fbb4efefaaf0e3fd73f695ee28"))
                    substitute(module("com.github.Fallen-Breath:remap")).using(module("com.github.Skytils:remap:2480d8a1b9"))

                    // for Loom 1.9 since we're not using Kotlin 2.0
                    substitute(module("org.jetbrains.kotlin:kotlin-metadata-jvm")).using(module("org.jetbrains.kotlin:kotlin-metadata-jvm:2.0.20"))
                }
            }
        }
    }
}
