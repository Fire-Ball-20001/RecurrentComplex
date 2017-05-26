/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.world.gen.feature;

import ivorius.ivtoolkit.blocks.BlockSurfacePos;
import ivorius.ivtoolkit.math.AxisAlignedTransform2D;
import ivorius.ivtoolkit.world.chunk.gen.StructureBoundingBoxes;
import ivorius.reccomplex.RCConfig;
import ivorius.reccomplex.RecurrentComplex;
import ivorius.reccomplex.events.RCEventBus;
import ivorius.reccomplex.events.StructureGenerationEvent;
import ivorius.reccomplex.events.StructureGenerationEventLite;
import ivorius.reccomplex.utils.NBTStorable;
import ivorius.reccomplex.utils.RCAxisAlignedTransform;
import ivorius.reccomplex.world.gen.feature.structure.*;
import ivorius.reccomplex.world.gen.feature.structure.context.StructureLoadContext;
import ivorius.reccomplex.world.gen.feature.structure.context.StructurePrepareContext;
import ivorius.reccomplex.world.gen.feature.structure.context.StructureSpawnContext;
import ivorius.reccomplex.world.gen.feature.structure.generic.generation.GenerationType;
import ivorius.reccomplex.world.gen.feature.structure.generic.placement.StructurePlaceContext;
import net.minecraft.nbt.NBTBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by lukas on 19.01.15.
 */
public class StructureGenerator<S extends NBTStorable>
{
    public static final int MIN_DIST_TO_LIMIT = 1;

    @Nullable
    private WorldServer world;
    @Nullable
    private Structure<S> structure;
    @Nullable
    private String structureID;

    @Nullable
    private BlockPos lowerCoord;
    @Nullable
    private BlockSurfacePos surfacePos;
    @Nullable
    private Placer placer;
    private boolean fromCenter;

    @Nullable
    private Environment environment;
    @Nullable
    private Random random;

    @Nullable
    private AxisAlignedTransform2D transform;
    @Nullable
    private StructureBoundingBox boundingBox;
    @Nullable
    private StructureBoundingBox generationBB;
    @Nullable
    private Predicate<Vec3i> generationPredicate;

    private String generationInfoID;
    private GenerationType generationType;
    private int generationLayer = 0;

    private boolean generateAsSource = false;
    private StructureSpawnContext.GenerateMaturity generateMaturity = StructureSpawnContext.GenerateMaturity.FIRST;

    @Nullable
    private S instanceData;
    @Nullable
    private NBTBase instanceDataNBT;

    private boolean allowOverlaps = false;
    private boolean memorize = true;

    private boolean partially;

    public StructureGenerator(Structure<S> structure)
    {
        structure(structure);
    }

    public StructureGenerator()
    {

    }

    @Nonnull
    public static Placer worldHeightPlacer()
    {
        return (context, blockCollection) -> context.environment.world.getHeight(new BlockPos(context.boundingBox.getCenter())).getY();
    }

    public static String name(String structureName)
    {
        return structureName != null ? structureName : "Unknown";
    }

    @Nonnull
    public Optional<StructureSpawnContext> generate()
    {
        Optional<StructureSpawnContext> spawnO = spawn();
        Optional<S> instanceDataO = instanceData();

        if (!spawnO.isPresent() || !instanceDataO.isPresent())
            return Optional.empty();

        StructureSpawnContext spawn = spawnO.get();
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        S instanceData = instanceDataO.get();

        Structure<S> structure = structure();
        String structureID = structureID();
        boolean firstTime = spawn.generateMaturity.isFirstTime();

        WorldServer world = spawn.environment.world;
        StructureBoundingBox boundingBox = spawn.boundingBox;

        if (!maturity().isSuggest() || (
                boundingBox.minY >= MIN_DIST_TO_LIMIT && boundingBox.maxY <= world.getHeight() - 1 - MIN_DIST_TO_LIMIT
                        && (!RCConfig.avoidOverlappingGeneration || allowOverlaps || WorldStructureGenerationData.get(world).entriesAt(boundingBox).noneMatch(WorldStructureGenerationData.Entry::blocking))
                        && !RCEventBus.INSTANCE.post(new StructureGenerationEvent.Suggest(structure, spawn))
                        && (structureID == null || !MinecraftForge.EVENT_BUS.post(new StructureGenerationEventLite.Suggest(world, structureID, boundingBox, spawn.generationLayer, firstTime)))
        ))
        {
            if (firstTime)
            {
                RCEventBus.INSTANCE.post(new StructureGenerationEvent.Pre(structure, spawn));
                if (structureID != null)
                    MinecraftForge.EVENT_BUS.post(new StructureGenerationEventLite.Pre(world, structureID, boundingBox, spawn.generationLayer, firstTime));
            }

            structure.generate(spawn, instanceData, RCConfig.getUniversalTransformer());

            if (firstTime)
            {
                RecurrentComplex.logger.trace(String.format("Generated structure '%s' in %s (%d)", name(structureID), boundingBox, world.provider.getDimension()));

                RCEventBus.INSTANCE.post(new StructureGenerationEvent.Post(structure, spawn));
                if (structureID != null)
                    MinecraftForge.EVENT_BUS.post(new StructureGenerationEventLite.Post(world, structureID, boundingBox, spawn.generationLayer, firstTime));

                if (structureID != null && memorize)
                {
                    String generationInfoID = generationType != null ? generationType.id() : null;

                    WorldStructureGenerationData.StructureEntry structureEntry = WorldStructureGenerationData.StructureEntry.complete(structureID, generationInfoID, boundingBox, spawn.transform, !partially);
                    structureEntry.blocking = structure.isBlocking();
                    structureEntry.firstTime = false; // Been there done that

                    try
                    {
                        structureEntry.instanceData = instanceData.writeToNBT();
                    }
                    catch (Exception e)
                    {
                        RecurrentComplex.logger.error(String.format("Error saving instance data for structure %s in %s", structure, boundingBox), e);
                    }

                    Collection<ChunkPos> existingChunks = WorldStructureGenerationData.get(world).addEntry(structureEntry).stream().collect(Collectors.toList());

                    // Complement in all chunks that already exist
                    if (partially)
                    {
                        maturity(StructureSpawnContext.GenerateMaturity.COMPLEMENT);

                        StructureBoundingBox oldBB = this.generationBB;
                        for (ChunkPos existingChunk : existingChunks)
                        {
                            generationBB(Structures.chunkBoundingBox(existingChunk));

                            structure.generate(spawn().get(), instanceData, RCConfig.getUniversalTransformer());
                        }
                        generationBB(oldBB);
                    }
                }
            }

            return spawnO;
        }
        else
        {
            String reason = "unknown reason";

            if (!boundingBox().isPresent())
                reason = "failed placement";

            RecurrentComplex.logger.trace(String.format("%s canceled generation at %s (%d) (%s)", structure, lowerCoord(), world.provider.getDimension(), reason));
        }

        return Optional.empty();
    }

    public StructureGenerator<S> asChild(StructureSpawnContext context, VariableDomain variableDomain)
    {
        return environment(context.environment.copy(variableDomain)).random(context.random).transform(context.transform)
                .generationBB(context.generationBB).generationPredicate(context.generationPredicate).generationLayer(context.generationLayer + 1)
                .asSource(context.generateAsSource).maturity(context.generateMaturity.isFirstTime() ? StructureSpawnContext.GenerateMaturity.FIRST : StructureSpawnContext.GenerateMaturity.COMPLEMENT);
    }

    public StructureGenerator<S> asChild(StructureSpawnContext context)
    {
        return asChild(context, null);
    }

    public StructureGenerator<S> world(@Nonnull WorldServer world)
    {
        this.world = world;
        return this;
    }

    @Nonnull
    public WorldServer world()
    {
        WorldServer world = this.world != null ? this.world : environment != null ? environment.world : null;
        if (world == null) throw new IllegalStateException("No world!");
        return world;
    }

    @Nonnull
    public Random random()
    {
        return this.random != null ? this.random : world().rand;
    }

    public StructureGenerator<S> structure(@Nonnull Structure<S> structure)
    {
        this.structure = structure;
        return this;
    }

    public StructureGenerator<S> structureID(@Nonnull String structureID)
    {
        this.structureID = structureID;
        return this;
    }

    @Nonnull
    public Structure<S> structure()
    {
        //noinspection unchecked
        Structure<S> structure = this.structure != null ? this.structure : structureID != null ? (Structure<S>) StructureRegistry.INSTANCE.get(structureID) : null;
        if (structure == null) throw new IllegalStateException();
        return structure;
    }

    @Nullable
    public String structureID()
    {
        return this.structureID != null ? this.structureID : this.structure != null ? StructureRegistry.INSTANCE.id(structure) : null;
    }

    public StructureGenerator<S> lowerCoord(@Nonnull BlockPos lowerCoord)
    {
        this.lowerCoord = lowerCoord;
        return this;
    }

    @Nonnull
    public Optional<BlockPos> lowerCoord()
    {
        if (lowerCoord != null)
            return Optional.of(lowerCoord);
        else
            return boundingBox().map(StructureBoundingBoxes::min);
    }

    public StructureGenerator<S> randomPosition(@Nonnull BlockSurfacePos surfacePos, @Nullable Placer placer)
    {
        this.surfacePos = surfacePos;
        this.placer = placer != null ? placer : worldHeightPlacer();
        return this;
    }

    public StructureGenerator<S> fromCenter(boolean fromCenter)
    {
        this.fromCenter = fromCenter;
        return this;
    }

    public StructureGenerator<S> environment(@Nonnull Environment environment)
    {
        this.environment = environment;
        return this;
    }

    @Nonnull
    public Environment environment()
    {
        return environment != null ? environment : Environment.inNature(world(), surfaceBoundingBox(),
                generationType != null ? generationType : generationInfoID != null ? structure().generationInfo(generationInfoID) : null);
    }

    public StructureGenerator<S> random(@Nonnull Random random)
    {
        this.random = random;
        return this;
    }

    public StructureGenerator<S> transform(@Nonnull AxisAlignedTransform2D transform)
    {
        this.transform = transform;
        return this;
    }

    @Nonnull
    public AxisAlignedTransform2D transform()
    {
        if (this.transform != null)
            return this.transform;
        else
        {
            Structure<S> structure = structure();
            Random random = random();

            AxisAlignedTransform2D transform = AxisAlignedTransform2D.from(structure.isRotatable() ? random.nextInt(4) : 0, structure.isMirrorable() && random.nextBoolean());
            return this.transform = transform;
        }
    }

    public StructureGenerator<S> boundingBox(@Nonnull StructureBoundingBox boundingBox)
    {
        this.boundingBox = boundingBox;
        return this;
    }

    public Optional<StructureBoundingBox> boundingBox()
    {
        return boundingBox(true);
    }

    @Nonnull
    public StructureBoundingBox surfaceBoundingBox()
    {
        //noinspection OptionalGetWithoutIsPresent
        return boundingBox(false).get();
    }

    @Nonnull
    protected Optional<StructureBoundingBox> boundingBox(boolean placed)
    {
        StructureBoundingBox boundingBox = this.boundingBox != null ? this.boundingBox : null;

        if (boundingBox == null)
        {
            int[] size = structureSize();

            if (this.lowerCoord != null)
                boundingBox = Structures.structureBoundingBox(fromCenter ? lowerCoord.subtract(new Vec3i(size[0] / 2, 0, size[2] / 2)) : lowerCoord, size);
            else if (surfacePos != null && placer != null)
            {
                boundingBox = Structures.structureBoundingBox((fromCenter ? surfacePos.subtract(size[0] / 2, size[2] / 2) : surfacePos).blockPos(0), size);

                if (placed)
                {
                    int y = placer.place(place(), structure().blockCollection());
                    if (y < 0) return Optional.empty();
                    boundingBox.minY += y;
                    boundingBox.maxY += y;
                }
            }
            else
                throw new IllegalStateException("No place!");
        }

        if (placed)
            this.boundingBox = boundingBox;

        return Optional.of(boundingBox);
    }

    public int[] structureSize()
    {
        return RCAxisAlignedTransform.applySize(transform(), structure().size());
    }

    @Nullable
    public StructureBoundingBox generationBB()
    {
        return generationBB;
    }

    public StructureGenerator<S> generationBB(@Nullable StructureBoundingBox generationBB)
    {
        this.generationBB = generationBB;
        return this;
    }

    @Nullable
    public Predicate<Vec3i> generationPredicate()
    {
        return generationPredicate;
    }

    public StructureGenerator<S> generationPredicate(@Nullable Predicate<Vec3i> generationPredicate)
    {
        this.generationPredicate = generationPredicate;
        return this;
    }

    public StructureGenerator<S> generationInfo(@Nullable GenerationType generationType)
    {
        this.generationType = generationType;
        return this;
    }

    public StructureGenerator<S> generationInfo(@Nullable String generationInfo)
    {
        this.generationInfoID = generationInfo;
        return this;
    }

    public StructureGenerator<S> generationLayer(int generationLayer)
    {
        this.generationLayer = generationLayer;
        return this;
    }

    public StructureGenerator<S> asSource(boolean generateAsSource)
    {
        this.generateAsSource = generateAsSource;
        return this;
    }

    public StructureGenerator<S> maturity(StructureSpawnContext.GenerateMaturity generateMaturity)
    {
        this.generateMaturity = generateMaturity;
        return this;
    }

    public StructureSpawnContext.GenerateMaturity maturity()
    {
        return generateMaturity;
    }

    public StructureGenerator<S> instanceData(S s)
    {
        instanceData = s;
        return this;
    }

    public StructureGenerator<S> instanceData(NBTBase nbt)
    {
        this.instanceDataNBT = nbt;
        return this;
    }

    @Nonnull
    public Optional<S> instanceData()
    {
        return this.instanceData != null ? Optional.of(this.instanceData)
                : this.instanceDataNBT != null ? load().map(load -> structure().loadInstanceData(load, this.instanceDataNBT, RCConfig.getUniversalTransformer()))
                : prepare().map(prepare -> structure().prepareInstanceData(prepare, RCConfig.getUniversalTransformer()));
    }

    public StructureGenerator<S> memorize(boolean memorize)
    {
        this.memorize = memorize;
        return this;
    }

    public StructureGenerator<S> partially(boolean partially, ChunkPos pos)
    {
        this.partially = partially;
        generationBB(partially ? Structures.chunkBoundingBox(pos) : null);
        return this;
    }

    public StructureGenerator<S> allowOverlaps(boolean allowOverlaps)
    {
        this.allowOverlaps = allowOverlaps;
        return this;
    }

    @Nonnull
    public StructurePlaceContext place()
    {
        return new StructurePlaceContext(random(), environment(), transform(), surfaceBoundingBox());
    }

    @Nonnull
    public Optional<StructurePrepareContext> prepare()
    {
        return boundingBox().map(bb -> new StructurePrepareContext(transform(), bb, generateAsSource, environment(), random(), generateMaturity));
    }

    @Nonnull
    public Optional<StructureLoadContext> load()
    {
        return boundingBox().map(bb -> new StructureLoadContext(transform(), bb, generateAsSource));
    }

    @Nonnull
    public Optional<StructureSpawnContext> spawn()
    {
        return boundingBox().map(bb -> new StructureSpawnContext(environment(), random(), transform(), bb, generationBB, generationPredicate, generationLayer, generateAsSource, generateMaturity));
    }

}
