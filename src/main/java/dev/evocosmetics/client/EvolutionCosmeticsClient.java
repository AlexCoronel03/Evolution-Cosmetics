package dev.evocosmetics.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class EvolutionCosmeticsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Species/evolution data is synced on join; drop cached resolutions whenever it may have changed.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> AppearanceRedirect.clearCache());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> AppearanceRedirect.clearCache());
    }
}
