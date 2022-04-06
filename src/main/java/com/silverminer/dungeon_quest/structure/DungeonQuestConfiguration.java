/*
 * Copyright © 2022 AFunProject
 *
 * Permission is hereby granted, free of charge,
 * to any person obtaining a copy of this software
 * and associated documentation files (the “Software”),
 * to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.silverminer.dungeon_quest.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.JigsawConfiguration;
import net.minecraft.world.level.levelgen.feature.structures.StructureTemplatePool;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;

public record DungeonQuestConfiguration(ResourceLocation startPool, int maxDepth,
                                        StructureFeature<?> structureFeature,
                                        int distance) implements FeatureConfiguration {
   private static final Codec<StructureFeature<?>> STRUCTURE_FEATURE_CODEC = ResourceLocation.CODEC.flatXmap(
         resourceLocation ->
               Optional.ofNullable(ForgeRegistries.STRUCTURE_FEATURES.getValue(resourceLocation))
                     .map(DataResult::success).orElse(DataResult.error(resourceLocation + " is no valid StructureFeature")),
         spawnCriteria -> Optional.ofNullable(ForgeRegistries.STRUCTURE_FEATURES.getKey(spawnCriteria))
               .map(DataResult::success).orElse(DataResult.error("This StructureFeature isn't registered")));
   public static final Codec<DungeonQuestConfiguration> CODEC = RecordCodecBuilder.create(dungeonQuestConfigurationInstance ->
         dungeonQuestConfigurationInstance.group(
                     ResourceLocation.CODEC.fieldOf("start_pool").forGetter(DungeonQuestConfiguration::startPool),
                     Codec.intRange(1, 7).fieldOf("size").forGetter(DungeonQuestConfiguration::maxDepth),
                     STRUCTURE_FEATURE_CODEC.fieldOf("structure").forGetter(DungeonQuestConfiguration::structureFeature),
                     Codec.intRange(1, 32).fieldOf("max_distance").forGetter(DungeonQuestConfiguration::distance))
               .apply(dungeonQuestConfigurationInstance, DungeonQuestConfiguration::new));

   public JigsawConfiguration jigsawConfiguration(RegistryAccess registryAccess) {
      Registry<StructureTemplatePool> structureTemplatePoolRegistry = registryAccess.registryOrThrow(Registry.TEMPLATE_POOL_REGISTRY);
      return new JigsawConfiguration(() -> structureTemplatePoolRegistry.get(this.startPool()), this.maxDepth());
   }
}