package dev.evocosmetics.mixin.client;

import com.cobblemon.mod.common.client.render.SpriteType;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableModel;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository;
import dev.evocosmetics.client.AppearanceRedirect;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import java.util.function.Supplier;

/**
 * Cobblemon's world renderer, party/PC/summary GUI, battle overlay and item renderer all resolve
 * poser/texture/layers/sprite through these five (Identifier, PosableState) lookups (verified in the
 * 1.8.1 bytecode). When the render state carries an evolution-cosmetic marker aspect, we re-run the same
 * lookup for the ancestor species with the ancestor's aspect set, then restore the state.
 */
@Mixin(value = VaryingModelRepository.class, remap = false)
public abstract class VaryingModelRepositoryMixin {

    @Shadow public abstract PosableModel getPoser(Identifier name, PosableState state);
    @Shadow public abstract Identifier getTexture(Identifier name, PosableState state);
    @Shadow public abstract Identifier getTextureNoSubstitute(Identifier name, PosableState state);
    @Shadow public abstract Iterable<?> getLayers(Identifier name, PosableState state);
    @Shadow public abstract Identifier getSprite(Identifier name, PosableState state, SpriteType type);

    @Inject(method = "getPoser", at = @At("HEAD"), cancellable = true)
    private void evocosmetics$poser(Identifier name, PosableState state, CallbackInfoReturnable<PosableModel> cir) {
        AppearanceRedirect.Redirect r = AppearanceRedirect.resolve(name, state);
        if (r != null) {
            cir.setReturnValue(evocosmetics$run(state, r, () -> getPoser(r.speciesId(), state)));
        }
    }

    @Inject(method = "getTexture", at = @At("HEAD"), cancellable = true)
    private void evocosmetics$texture(Identifier name, PosableState state, CallbackInfoReturnable<Identifier> cir) {
        AppearanceRedirect.Redirect r = AppearanceRedirect.resolve(name, state);
        if (r != null) {
            cir.setReturnValue(evocosmetics$run(state, r, () -> getTexture(r.speciesId(), state)));
        }
    }

    @Inject(method = "getTextureNoSubstitute", at = @At("HEAD"), cancellable = true)
    private void evocosmetics$textureNoSub(Identifier name, PosableState state, CallbackInfoReturnable<Identifier> cir) {
        AppearanceRedirect.Redirect r = AppearanceRedirect.resolve(name, state);
        if (r != null) {
            cir.setReturnValue(evocosmetics$run(state, r, () -> getTextureNoSubstitute(r.speciesId(), state)));
        }
    }

    @Inject(method = "getLayers", at = @At("HEAD"), cancellable = true)
    private void evocosmetics$layers(Identifier name, PosableState state, CallbackInfoReturnable<Iterable<?>> cir) {
        AppearanceRedirect.Redirect r = AppearanceRedirect.resolve(name, state);
        if (r != null) {
            cir.setReturnValue(evocosmetics$run(state, r, () -> getLayers(r.speciesId(), state)));
        }
    }

    @Inject(method = "getSprite", at = @At("HEAD"), cancellable = true)
    private void evocosmetics$sprite(Identifier name, PosableState state, SpriteType type, CallbackInfoReturnable<Identifier> cir) {
        AppearanceRedirect.Redirect r = AppearanceRedirect.resolve(name, state);
        if (r != null) {
            cir.setReturnValue(evocosmetics$run(state, r, () -> getSprite(r.speciesId(), state, type)));
        }
    }

    /** Runs the original lookup for the ancestor with the ancestor's aspects, then restores the state. */
    private static <T> T evocosmetics$run(PosableState state, AppearanceRedirect.Redirect r, Supplier<T> lookup) {
        Set<String> original = state.getCurrentAspects();
        AppearanceRedirect.enter();
        try {
            state.setCurrentAspects(r.aspects());
            return lookup.get();
        } finally {
            state.setCurrentAspects(original);
            AppearanceRedirect.exit();
        }
    }
}
