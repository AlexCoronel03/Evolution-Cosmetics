package dev.evocosmetics.mixin;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonServerDelegate;
import com.cobblemon.mod.common.pokemon.Pokemon;
import dev.evocosmetics.resolve.AppearanceRedirect;
import dev.evocosmetics.resolve.CosmeticMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;

/**
 * Server-side only. Every tick {@code PokemonServerDelegate#updateTrackedValues} copies the Pokémon's species and
 * aspects into the entity's synced data ({@code PokemonEntity.SPECIES} / {@code ASPECTS}); the Cobblemon client reads
 * exactly those values to pick the model and texture. For each of the two it uses the private {@code getMock()}
 * override (the Illusion/Transform mechanism) when non-null and falls back to the real Pokémon otherwise.
 *
 * <p>We supply a {@code getMock()} result that only carries species + aspects. Because nothing else is overridden and
 * no {@code MocKEffect} is registered, the entity's real species/form, hitbox, movement, Pokédex scan and, above all,
 * the {@link Pokemon} object itself stay untouched. Vanilla Cobblemon clients need no extra code.
 */
@Mixin(value = PokemonServerDelegate.class, remap = false)
public abstract class PokemonServerDelegateMixin {

    @Shadow public abstract PokemonEntity getEntity();

    @Inject(method = "getMock", at = @At("RETURN"), cancellable = true)
    private void evocosmetics$exposeAncestor(CallbackInfoReturnable<PokemonProperties> cir) {
        if (cir.getReturnValue() != null) {
            return; // a real Illusion/Transform effect is active: never interfere with it
        }
        PokemonEntity entity = getEntity();
        Pokemon pokemon = entity == null ? null : entity.getPokemon();
        if (pokemon == null || pokemon.getAspects().isEmpty()) {
            return;
        }
        if (CosmeticMode.fromAspects(pokemon.getAspects()) == null) {
            return; // fast path for every Pokémon without an Everstone/Eviolite cosmetic
        }
        AppearanceRedirect.Redirect redirect =
                AppearanceRedirect.resolve(pokemon.getSpecies(), pokemon.getForm(), pokemon.getAspects());
        if (redirect == null) {
            return;
        }
        PokemonProperties exposed = new PokemonProperties();
        exposed.setSpecies(redirect.speciesId().toString());
        exposed.setAspects(new HashSet<>(redirect.aspects()));
        cir.setReturnValue(exposed);
    }
}
