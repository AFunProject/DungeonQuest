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

package com.silverminer.dungeon_quest;

import com.google.common.collect.*;
import com.silverminer.dungeon_quest.data.ConfiguredStructureFeatures;
import com.silverminer.dungeon_quest.structure.StructureRegistration;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.StructureSettings;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.StructureFeatureConfiguration;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Mod(DungeonQuest.MODID)
public class DungeonQuest {
   private static final Logger LOGGER = LogManager.getLogger();
   public static final String MODID = "dungeon_quest";
   public static String VERSION = "N/A";

   public DungeonQuest() {
      ModList.get().getModContainerById(DungeonQuest.MODID)
            .ifPresent(container -> VERSION = container.getModInfo().getVersion().toString());
      LOGGER.info("Dungeon Quest " + VERSION + " initialized");
      StructureRegistration.STRUCTURE_FEATURE_REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
      MinecraftForge.EVENT_BUS.addListener(DungeonQuest::onWorldLoad);
   }

   @Contract("_ -> new")
   public static @NotNull ResourceLocation location(String path) {
      return new ResourceLocation(MODID, path);
   }

   public static void registerStructureSeparationSettings() {
      StructureFeature<?> structure = StructureRegistration.UNDERGROUND.get();
      StructureFeature.STRUCTURES_REGISTRY.put(StructureRegistration.UNDERGROUND.getId().toString(), structure);

      StructureFeatureConfiguration structureSeparationSettings = new StructureFeatureConfiguration(34, 11, 10387312);

      StructureSettings.DEFAULTS = ImmutableMap.<StructureFeature<?>, StructureFeatureConfiguration>builder()
            .putAll(StructureSettings.DEFAULTS).put(structure, structureSeparationSettings).build();

      BuiltinRegistries.NOISE_GENERATOR_SETTINGS.entrySet().forEach(settings -> {
         Map<StructureFeature<?>, StructureFeatureConfiguration> structureMap = settings.getValue().structureSettings()
               .structureConfig();
         if (structureMap instanceof ImmutableMap) {
            Map<StructureFeature<?>, StructureFeatureConfiguration> tempMap = new HashMap<>(structureMap);
            tempMap.put(structure, structureSeparationSettings);
            settings.getValue().structureSettings().structureConfig = tempMap;
         } else {
            structureMap.put(structure, structureSeparationSettings);
         }
      });
   }

   /**
    * A big thank you to TelepathicGrunt that wrote this method (See https://github.com/TelepathicGrunt/StructureTutorialMod/blob/1.18.0-Forge-Jigsaw/src/main/java/com/telepathicgrunt/structuretutorial/StructureTutorialMain.java)
    */
   private static void onWorldLoad(WorldEvent.@NotNull Load event) {
      if(event.getWorld() instanceof ServerLevel serverLevel) {
         ChunkGenerator chunkGenerator = serverLevel.getChunkSource().getGenerator();

         // Skip superflat worlds to prevent issues with it. Plus, users don't want structures clogging up their superflat worlds.
         if (chunkGenerator instanceof FlatLevelSource && serverLevel.dimension().equals(Level.OVERWORLD)) {
            return;
         }

         StructureSettings worldStructureConfig = chunkGenerator.getSettings();
         HashMap<StructureFeature<?>, HashMultimap<ConfiguredStructureFeature<?, ?>, ResourceKey<Biome>>> STStructureToMultiMap = new HashMap<>();
         ImmutableSet<ResourceKey<Biome>> overworldBiomes = ImmutableSet.<ResourceKey<Biome>>builder()
                    .add(Biomes.FOREST)
                    .add(Biomes.MEADOW)
                    .add(Biomes.PLAINS)
                    .add(Biomes.SAVANNA)
                    .add(Biomes.SNOWY_PLAINS)
                    .add(Biomes.SWAMP)
                    .add(Biomes.SUNFLOWER_PLAINS)
                    .add(Biomes.TAIGA)
                    .build();
            overworldBiomes.forEach(biomeKey -> associateBiomeToConfiguredStructure(STStructureToMultiMap, ConfiguredStructureFeatures.QUEST_DUNGEON, biomeKey));

         // Grab the map that holds what ConfigureStructures a structure has and what biomes it can spawn in.
         // Requires AccessTransformer  (see resources/META-INF/accesstransformer.cfg)
         ImmutableMap.Builder<StructureFeature<?>, ImmutableMultimap<ConfiguredStructureFeature<?, ?>, ResourceKey<Biome>>> tempStructureToMultiMap = ImmutableMap.builder();
         worldStructureConfig.configuredStructures.entrySet().stream().filter(entry -> !STStructureToMultiMap.containsKey(entry.getKey())).forEach(tempStructureToMultiMap::put);

         // Add our structures to the structure map/multimap and set the world to use this combined map/multimap.
         STStructureToMultiMap.forEach((key, value) -> tempStructureToMultiMap.put(key, ImmutableMultimap.copyOf(value)));

         // Requires AccessTransformer  (see resources/META-INF/accesstransformer.cfg)
         worldStructureConfig.configuredStructures = tempStructureToMultiMap.build();
      }
   }

   /**
    * Helper method that handles setting up the map to multimap relationship to help prevent issues.
    * A big thank you to TelepathicGrunt that wrote this method (See https://github.com/TelepathicGrunt/StructureTutorialMod/blob/1.18.0-Forge-Jigsaw/src/main/java/com/telepathicgrunt/structuretutorial/StructureTutorialMain.java)
    */
   private static void associateBiomeToConfiguredStructure(@NotNull Map<StructureFeature<?>, HashMultimap<ConfiguredStructureFeature<?, ?>, ResourceKey<Biome>>> STStructureToMultiMap, @NotNull ConfiguredStructureFeature<?, ?> configuredStructureFeature, ResourceKey<Biome> biomeRegistryKey) {
      STStructureToMultiMap.putIfAbsent(configuredStructureFeature.feature, HashMultimap.create());
      HashMultimap<ConfiguredStructureFeature<?, ?>, ResourceKey<Biome>> configuredStructureToBiomeMultiMap = STStructureToMultiMap.get(configuredStructureFeature.feature);
      if(configuredStructureToBiomeMultiMap.containsValue(biomeRegistryKey)) {
         LOGGER.error("""
                    Detected 2 ConfiguredStructureFeatures that share the same base StructureFeature trying to be added to same biome. One will be prevented from spawning.
                    This issue happens with vanilla too and is why a Snowy Village and Plains Village cannot spawn in the same biome because they both use the Village base structure.
                    The two conflicting ConfiguredStructures are: {}, {}
                    The biome that is attempting to be shared: {}
                """,
               BuiltinRegistries.CONFIGURED_STRUCTURE_FEATURE.getId(configuredStructureFeature),
               BuiltinRegistries.CONFIGURED_STRUCTURE_FEATURE.getId(configuredStructureToBiomeMultiMap.entries().stream().filter(e -> e.getValue() == biomeRegistryKey).findFirst().get().getKey()),
               biomeRegistryKey
         );
      }
      else{
         configuredStructureToBiomeMultiMap.put(configuredStructureFeature, biomeRegistryKey);
      }
   }
}