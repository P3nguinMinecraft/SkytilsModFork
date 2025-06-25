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
package gg.skytils.skytilsmod.gui.updater

import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.RelativeConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.constraints.SubtractiveConstraint
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.vigilance.utils.onLeftClick
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.IO
import gg.skytils.skytilsmod.core.UpdateChecker
import gg.skytils.skytilsmod.gui.components.SimpleButton
import gg.skytils.skytilsmod.utils.containsAny
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.launch
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPSignatureList
import org.bouncycastle.openpgp.PGPUtil
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider
import java.awt.Color
import java.io.File
import java.security.Security
import java.util.zip.ZipFile

class UpdateGui(restartNow: Boolean) : WindowScreen(ElementaVersion.V2, newGuiScale = 2) {
    companion object {
        var complete = BasicState(false)
    }

    private var progress = BasicState(0f)
    private var stage = BasicState("Downloading")
    private var failed = BasicState(false)

    private val backBtn = SimpleButton("", h = true, w = true).apply {
        constrain {
            x = CenterConstraint()
            y = RelativeConstraint(2/3f)
            width = 200.pixels
            height = 20.pixels
        }
        onLeftClick {
            client?.setScreen(null)
        }
        text.bindText(failed.zip(complete).map { (f, c) ->
            if (f || c) "Back" else "Cancel"
        })
    } childOf window

    private val statusText = UIText(failed.zip(complete).map { (f, c) ->
        if (f) "§cUpdate download failed"
        else if (c) "§aUpdate download complete"
        else ""
    }).apply {
        constrain {
            x = CenterConstraint()
            y = CenterConstraint()
        }
    } childOf window

    private val progressBar = UIBlock(Color(-0x3f3f40)).apply {
        constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = 202.pixels
            height = basicHeightConstraint {  getFontProvider().getBaseLineHeight() + 4 + 2 }
        }

        val progressFill = UIBlock(Color(-0x34c2cb)).apply {
            constrain {
                x = 1.pixels
                y = RelativeConstraint()
                width = RelativeConstraint(progress)
                height = basicHeightConstraint { parent.getHeight() - 2 }
            }
        } childOf this


        val progressRemaining = UIBlock(Color.WHITE).apply {
            constrain {
                x = SiblingConstraint()
                y = RelativeConstraint()
                width = SubtractiveConstraint(RelativeConstraint(progress.map { 1 - it }), 1.pixels)
                height = basicHeightConstraint { parent.getHeight() - 2 }
            }
        } childOf this

        val progressLabel = UIText("%d%%".format(progress.map { (it * 100).toInt().coerceIn(0, 100) })).apply {
            constrain {
                x = CenterConstraint()
                y = 3.pixels
                color = Color.BLACK.toConstraint()
            }
        } childOf this
    } childOf window

    private val statusLabel = UIText(stage.map { "${it}..." }).apply {
        constrain {
            x = CenterConstraint()
            y = SiblingConstraint(2f + getFontProvider().getBaseLineHeight(), true)
        }
    } childOf window

    private fun doUpdate(restartNow: Boolean) {
        try {
            val directory = File(Skytils.modDir, "updates")
            val updateObj = UpdateChecker.updateGetter.updateObj
            if (updateObj == null) {
                println("Update object is null, cannot proceed with update")
                failed.set(true)
                return
            }
            val url = UpdateChecker.updateDownloadURL
            val jarName = UpdateChecker.updateAsset!!.filename
            IO.launch(CoroutineName("Skytils-update-downloader-thread")) {
                val updateFile = downloadUpdate(url, directory)
                stage.set("Downloading signature")
                val modrinthSignFile = updateObj.files.find { it.filename.containsAny(".asc", ".sig") }
                if (modrinthSignFile == null) {
                    println("No signature file found for the update")
                    failed.set(true)
                    return@launch
                } else {
                    println("Signature file found: ${modrinthSignFile.filename}")
                }
                val signFile = downloadUpdate(modrinthSignFile.url, directory, modrinthSignFile.filename)
                if (!failed.get()) {
                    if (updateFile != null && signFile != null) {
                        var zip: ZipFile? = null
                        val signFileInputStream = if (signFile.extension == "zip") {
                            stage.set("Extracting signature")
                            zip = ZipFile(signFile)
                            val sigEntry = zip.entries().asSequence().first { it.name.endsWith(".asc") || it.name.endsWith(".sig") }
                            zip.getInputStream(sigEntry)
                        } else signFile.inputStream()
                        stage.set("Verifying signature")
                        val finger = JcaKeyFingerprintCalculator()

                        fun getKeyRingCollection(fileName: String): PGPPublicKeyRingCollection =
                            this::class.java.classLoader.getResourceAsStream("assets/skytils/$fileName.gpg")!!.use {
                                PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(it), finger)
                            }

                        val keys = listOf(
                            getKeyRingCollection("my-name-is-jeff"),
                            getKeyRingCollection("sychic")
                        )

                        val sig = PGPUtil.getDecoderStream(signFileInputStream).use { (JcaPGPObjectFactory(it).nextObject() as PGPSignatureList).first() }
                        zip?.close()
                        val key = keys.firstNotNullOfOrNull { it.getPublicKey(sig.keyID) }
                        if (key != null) {
                            sig.init(JcaPGPContentVerifierBuilderProvider().setProvider(Security.getProvider("BC") ?: BouncyCastleProvider().also(Security::addProvider)), key)
                            sig.update(updateFile.readBytes())
                            if (sig.verify()) {
                                signFile.deleteOnExit()
                                UpdateChecker.scheduleCopyUpdateAtShutdown(jarName)
                                if (restartNow) {
                                    client?.scheduleStop()
                                }
                                complete.set(true)
                            } else {
                                failed.set(true)
                                println("Signature verification failed")
                            }
                        } else {
                            println("Key not found")
                            failed.set(true)
                        }
                    } else {
                        println("Files are missing")
                        failed.set(true)
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private suspend fun downloadUpdate(urlString: String, directory: File, fileName: String? = null): File? {
        try {
            val url = Url(urlString)

            val st = Skytils.client.get(url) {
                expectSuccess = false
                onDownload { bytesSentTotal, contentLength ->
                    if (contentLength != 0L)
                        progress.set(bytesSentTotal / contentLength.toFloat())
                }
                timeout {
                    connectTimeoutMillis = null
                    requestTimeoutMillis = null
                    socketTimeoutMillis = null
                }
            }
            if (st.status != HttpStatusCode.OK) {
                failed.set(true)
                println("$url returned status code ${st.status}")
                return null
            }
            if (!directory.exists() && !directory.mkdirs()) {
                failed.set(true)
                println("Couldn't create update file directory")
                return null
            }
            val fileSaved = File(directory, fileName ?: url.pathSegments.last().decodeURLPart())
            val writeChannel = fileSaved.writeChannel()
            if (client?.currentScreen !== this@UpdateGui || st.bodyAsChannel().copyAndClose(writeChannel) == 0L) {
                failed.set(true)
                return null
            }
            println("Downloaded update to $fileSaved")
            return fileSaved
        } catch (ex: Exception) {
            ex.printStackTrace()
            failed.set(true)
        }
        return null
    }

    init {
        doUpdate(restartNow)

        failed.zip(complete).onSetValue { (f, c) ->
            Window.enqueueRenderOperation {
                if (f || c) {
                    progressBar.hide(instantly = true)
                    statusLabel.hide(instantly = true)
                }
            }
        }
    }
}