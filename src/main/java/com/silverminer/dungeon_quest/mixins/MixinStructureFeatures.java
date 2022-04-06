/*
 * Silverminer007
 * Copyright (c) 2022.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.silverminer.dungeon_quest.mixins;

import com.silverminer.dungeon_quest.data.ConfiguredStructureFeatures;
import net.minecraft.data.worldgen.StructureFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.BiConsumer;

@Mixin(StructureFeatures.class)
public class MixinStructureFeatures {
   @Inject(method = "registerStructures", at = @At(value = "HEAD"))
   private static void onRegisterStructures(BiConsumer<ConfiguredStructureFeature<?, ?>, ResourceKey<Biome>> registry, CallbackInfo callbackInfo) {
      /*for (ResourceKey<Biome> biome : List.of(Biomes.TAIGA, Biomes.DESERT, Biomes.PLAINS, Biomes.MEADOW, Biomes.SNOWY_PLAINS)) {
         registry.accept(ConfiguredStructureFeatures.QUEST_DUNGEON, biome);
      }*/
   }
}
