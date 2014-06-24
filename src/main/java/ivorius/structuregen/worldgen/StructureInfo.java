/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.structuregen.worldgen;

import ivorius.structuregen.ivtoolkit.math.AxisAlignedTransform2D;
import ivorius.structuregen.ivtoolkit.blocks.BlockCoord;
import ivorius.structuregen.worldgen.genericStructures.GenericStructureInfo;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import java.util.Random;

/**
 * Created by lukas on 24.05.14.
 */
public interface StructureInfo
{
    void generate(World world, Random random, BlockCoord coord, AxisAlignedTransform2D transform, int layer);

    void generateSource(World world, Random random, BlockCoord coord, int layer);

    int generationY(World world, Random random, int x, int z);

    int[] structureBoundingBox();

    boolean isRotatable();

    boolean isMirrorable();

    int generationWeightInBiome(BiomeGenBase biome);

    String generationCategory();

    GenericStructureInfo copyAsGenericStructureInfo();

    boolean areDependenciesResolved();
}
