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

import java.io.File

setOf("skytils", "catlas").forEach {
    File("../mod/src/main/resources/assets/$it/lang/").apply {
        println("Converting .lang files to .json in $absolutePath")
        if (exists()) {
            listFiles { dir, name -> name.endsWith(".lang") }?.forEach { file ->
                val jsonFile = File(file.parentFile, file.name.replace(".lang", ".json").lowercase())
                val lines = file.readLines()
                val jsonContent = lines.joinToString(",\n") { line ->
                    val parts = line.split("=", limit = 2)
                    if (parts.size == 2) {
                        "    \"${parts[0].trim()}\": \"${parts[1].trim().replace("\"", "\\\"")}\""
                    } else {
                        ""
                    }
                }
                jsonFile.writeText("{\n$jsonContent\n}")
                println("Converted ${file.name} to ${jsonFile.name}")
            }
        }
    }
}