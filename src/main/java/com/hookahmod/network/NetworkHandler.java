package com.hookahmod.network;

import com.hookahmod.HookahMod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NetworkHandler {

    private NetworkHandler() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(HookahMod.MOD_ID).versioned("1.0");

        registrar.playToServer(
                ToggleMouthpiecePayload.TYPE,
                ToggleMouthpiecePayload.STREAM_CODEC,
                ToggleMouthpiecePayload::handle
        );

        registrar.playToServer(
                OpenWornHookahPayload.TYPE,
                OpenWornHookahPayload.STREAM_CODEC,
                OpenWornHookahPayload::handle
        );

        registrar.playToClient(
                HookahSyncPayload.TYPE,
                HookahSyncPayload.STREAM_CODEC,
                HookahSyncPayload::handle
        );

        registrar.playToClient(
                WornHookahSyncPayload.TYPE,
                WornHookahSyncPayload.STREAM_CODEC,
                WornHookahSyncPayload::handle
        );
    }
}
