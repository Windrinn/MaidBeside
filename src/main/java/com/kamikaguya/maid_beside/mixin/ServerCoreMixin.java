package com.kamikaguya.maid_beside.mixin;

import com.kamikaguya.maid_beside.compat.pingwheel.handler.PingHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import nx.pingwheel.common.core.ServerCore;
import nx.pingwheel.common.network.PingLocationC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCore.class)
public class ServerCoreMixin {
    @Inject(
            method = "onPingLocation",
            at = @At("TAIL"),
            remap = false
    )
    private static void onPingLocationInjected(MinecraftServer server, ServerPlayer player, PingLocationC2SPacket packet, CallbackInfo ci) {
        PingHandler.handlePingForMaids(player, packet.pos());
    }
}