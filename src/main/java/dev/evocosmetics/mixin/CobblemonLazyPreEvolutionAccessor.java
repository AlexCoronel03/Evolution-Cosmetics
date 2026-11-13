package dev.evocosmetics.mixin;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.pokemon.evolution.CobblemonLazyPreEvolution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Cobblemon stores a pre-evolution as raw text such as {@code "meowth galarian"}. Its {@code getForm()} only honours a
 * {@code form=} property, so for the bare word {@code galarian} (an aspect) it answers with the standard form. This
 * exposes the parsed properties so the resolver can recover those aspects. Pure read access, server side logic only.
 */
@Mixin(value = CobblemonLazyPreEvolution.class, remap = false)
public interface CobblemonLazyPreEvolutionAccessor {
    @Invoker("getProperties")
    PokemonProperties evocosmetics$getProperties();
}
