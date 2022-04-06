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

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.JigsawConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.StructureFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.structures.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class DungeonQuestStructureFeature extends StructureFeature<DungeonQuestConfiguration> {

   public DungeonQuestStructureFeature() {
      super(DungeonQuestConfiguration.CODEC, DungeonQuestStructureFeature::place);
   }

   private static @NotNull Optional<PieceGenerator<DungeonQuestConfiguration>> place(PieceGeneratorSupplier.Context<DungeonQuestConfiguration> context) {
      if (!checkLocation(context)) {
         return Optional.empty();
      } else {
         BlockPos position = context.chunkPos().getMiddleBlockPosition(0);
         int minBuildHeight = context.heightAccessor().getMinBuildHeight();
         int yPos = (context.chunkGenerator().getFirstFreeHeight(position.getX(), position.getZ(),
               Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor()) / 2)
               + minBuildHeight;
         position = new BlockPos(position.getX(), Math.max(yPos, minBuildHeight + 30), position.getZ());
         JigsawConfiguration jigsawConfiguration = context.config().jigsawConfiguration(context.registryAccess());
         Optional<PieceGenerator<JigsawConfiguration>> pieceGenerator = JigsawPlacement.addPieces(
               new PieceGeneratorSupplier.Context<>(context.chunkGenerator(), context.biomeSource(), context.seed(),
                     context.chunkPos(), jigsawConfiguration, context.heightAccessor(),
                     context.validBiome(), context.structureManager(), context.registryAccess()),
               PoolElementStructurePiece::new, position,
               false, false);
         return pieceGenerator.isEmpty() ? Optional.empty() :
               Optional.of((structurePieceBuilder, pieceGeneratorContext) ->
                     pieceGenerator.get().generatePieces(structurePieceBuilder, convertContext(pieceGeneratorContext, jigsawConfiguration)));
      }
   }

   private static boolean checkLocation(@NotNull PieceGeneratorSupplier.Context<DungeonQuestConfiguration> context) {
      return hasFeatureChunkInRange(context.config().structureFeature(), context.seed(), context.chunkPos().x, context.chunkPos().z, context.config().distance(), context.chunkGenerator(), context);
   }

   private static boolean hasFeatureChunkInRange(StructureFeature<?> structure, long seed, int chunkX, int chunkZ, int range, ChunkGenerator chunkGenerator, @NotNull PieceGeneratorSupplier.Context<DungeonQuestConfiguration> context) {
      if (structure != null) {
         StructureFeatureConfiguration structureFeatureConfiguration = chunkGenerator.getSettings().getConfig(structure);
         if(structureFeatureConfiguration == null) {
            return false;
         }

         for (int i = chunkX - range; i <= chunkX + range; ++i) {
            for (int j = chunkZ - range; j <= chunkZ + range; ++j) {
               ChunkPos structurePos = structure.getPotentialFeatureChunk(structureFeatureConfiguration, seed, i, j);

               if (i == structurePos.x && j == structurePos.z) {
                  return true;
               }
            }
         }

         // Either the above method, which has the issue that there might be only a possible, but no actual placement
         // Or the above method, where we're missing a Server Level and might (which is most likely) cause a server crash due to circle references
         // ServerLevel serverLevel = ?
         // BlockPos startPos = context.chunkPos().getMiddleBlockPosition(0);
         // BlockPos structurePos = context.chunkGenerator().findNearestMapFeature(serverLevel, structure, startPos, range, false);
         // return structurePos != null && structurePos.closerThan(startPos, range);

      }
      return false;
   }

   private static PieceGenerator.@NotNull Context<JigsawConfiguration> convertContext(PieceGenerator.@NotNull Context<DungeonQuestConfiguration> context, JigsawConfiguration jigsawConfiguration) {
      return new PieceGenerator.Context<>(
            jigsawConfiguration,
            context.chunkGenerator(),
            context.structureManager(),
            context.chunkPos(),
            context.heightAccessor(),
            context.random(),
            context.seed()
      );
   }

   @Override
   @NotNull
   public GenerationStep.Decoration step() {
      return GenerationStep.Decoration.UNDERGROUND_STRUCTURES;
   }
}