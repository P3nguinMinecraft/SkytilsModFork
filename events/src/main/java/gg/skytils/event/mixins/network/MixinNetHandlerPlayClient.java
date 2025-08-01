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

package gg.skytils.event.mixins.network;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import gg.skytils.event.EventsKt;
import gg.skytils.event.impl.network.ClientConnectEvent;
import gg.skytils.event.impl.play.ActionBarReceivedEvent;
import gg.skytils.event.impl.play.ChatMessageReceivedEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinNetHandlerPlayClient {

    @Inject(method = "<init>", at = @At("RETURN"))
    public void onConnect(CallbackInfo ci) {
        MinecraftClient.getInstance().submit(() -> {
            EventsKt.postSync(new ClientConnectEvent());
        });
    }

    //#if MC<12000
    //$$ @Inject(method = "onGameMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;addMessage(Lnet/minecraft/text/Text;)V"), cancellable = true)
    //$$ public void onChat(CallbackInfo ci, @Local(argsOnly = true) LocalRef<GameMessageS2CPacket> packet) {
    //$$     ChatMessageReceivedEvent event = new ChatMessageReceivedEvent(packet.get().getMessage());
    //$$     if (EventsKt.postCancellableSync(event)) {
    //$$         ci.cancel();
    //$$     }
    //$$     packet.set(new GameMessageS2CPacket(event.getMessage(), packet.get().getType()));
    //$$ }
    //$$
    //$$ @Inject(method = "onGameMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;setOverlayMessage(Lnet/minecraft/text/Text;Z)V"), cancellable = true)
    //$$ public void onActionbar(CallbackInfo ci, @Local(argsOnly = true) LocalRef<GameMessageS2CPacket> packet) {
    //$$     ActionBarReceivedEvent event = new ActionBarReceivedEvent(packet.get().getMessage());
    //$$     if (EventsKt.postCancellableSync(event)) {
    //$$         ci.cancel();
    //$$     }
    //$$     packet.set(new GameMessageS2CPacket(event.getMessage(), packet.get().getType()));
    //$$ }
    //#endif
}
