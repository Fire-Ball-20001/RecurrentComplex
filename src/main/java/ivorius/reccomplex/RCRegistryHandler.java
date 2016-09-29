/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex;

import ivorius.ivtoolkit.network.CapabilityUpdateRegistry;
import ivorius.ivtoolkit.tools.MCRegistry;
import ivorius.ivtoolkit.tools.NBTCompoundObjectCapabilityStorage;
import ivorius.reccomplex.biome.RCBiomeDictionary;
import ivorius.reccomplex.blocks.*;
import ivorius.reccomplex.blocks.materials.MaterialNegativeSpace;
import ivorius.reccomplex.blocks.materials.RCMaterials;
import ivorius.reccomplex.entities.StructureEntityInfo;
import ivorius.reccomplex.files.FileTypeHandlerRegistryString;
import ivorius.reccomplex.files.RCFileSuffix;
import ivorius.reccomplex.items.*;
import ivorius.reccomplex.json.SerializableStringTypeRegistry;
import ivorius.reccomplex.operation.OperationRegistry;
import ivorius.reccomplex.random.Poem;
import ivorius.reccomplex.scripts.world.*;
import ivorius.reccomplex.structures.OperationGenerateStructure;
import ivorius.reccomplex.structures.OperationMoveStructure;
import ivorius.reccomplex.structures.StructureRegistry;
import ivorius.reccomplex.structures.generic.StructureSaveHandler;
import ivorius.reccomplex.structures.generic.gentypes.*;
import ivorius.reccomplex.structures.generic.maze.rules.MazeRuleRegistry;
import ivorius.reccomplex.structures.generic.maze.rules.saved.MazeRuleConnect;
import ivorius.reccomplex.structures.generic.maze.rules.saved.MazeRuleConnectAll;
import ivorius.reccomplex.structures.generic.placement.FactorLimit;
import ivorius.reccomplex.structures.generic.placement.FactorMatch;
import ivorius.reccomplex.structures.generic.placement.FactorRegistry;
import ivorius.reccomplex.structures.generic.placement.GenericPlacer;
import ivorius.reccomplex.structures.generic.placement.rays.*;
import ivorius.reccomplex.structures.generic.presets.*;
import ivorius.reccomplex.structures.generic.transformers.*;
import ivorius.reccomplex.structures.schematics.OperationGenerateSchematic;
import ivorius.reccomplex.utils.FMLUtils;
import ivorius.reccomplex.utils.PresetRegistry;
import ivorius.reccomplex.worldgen.inventory.GenericItemCollectionRegistry;
import ivorius.reccomplex.worldgen.inventory.ItemCollectionSaveHandler;
import ivorius.reccomplex.worldgen.inventory.RCInventoryGenerators;
import ivorius.reccomplex.worldgen.selector.NaturalStructureSelector;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.io.IOException;
import java.util.ArrayList;

import static ivorius.reccomplex.RecurrentComplex.fileTypeRegistry;
import static ivorius.reccomplex.RecurrentComplex.specialRegistry;
import static ivorius.reccomplex.blocks.RCBlocks.*;
import static ivorius.reccomplex.gui.RCCreativeTabs.tabInventoryGenerators;
import static ivorius.reccomplex.gui.RCCreativeTabs.tabStructureTools;
import static ivorius.reccomplex.items.RCItems.*;

/**
 * Created by lukas on 18.01.15.
 */
public class RCRegistryHandler
{
    public static void preInit(FMLPreInitializationEvent event, RecurrentComplex mod)
    {
        if (!RecurrentComplex.isLite())
        {
            tabStructureTools = new CreativeTabs("structureTools")
            {
                @Override
                public Item getTabIconItem()
                {
                    return RCItems.blockSelector;
                }
            };
            tabInventoryGenerators = new CreativeTabs("inventoryGenerators")
            {
                @Override
                public Item getTabIconItem()
                {
                    return RCItems.inventoryGenerationTag;
                }
            };
        }

        RCMaterials.materialNegativeSpace = new MaterialNegativeSpace();
        RCMaterials.materialGenericSolid = (new Material(MapColor.STONE));

        CapabilityManager.INSTANCE.register(StructureEntityInfo.class, new NBTCompoundObjectCapabilityStorage<>(StructureEntityInfo.class), StructureEntityInfo::new);

        blockSelector = new ItemBlockSelectorBlock().setUnlocalizedName("blockSelector");
        blockSelector.setCreativeTab(tabStructureTools);
        register(blockSelector, "block_selector");
        RecurrentComplex.cremapper.registerLegacyIDs(blockSelector, "blockSelector");

        blockSelectorFloating = new ItemBlockSelectorFloating().setUnlocalizedName("blockSelectorFloating");
        blockSelectorFloating.setCreativeTab(tabStructureTools);
        register(blockSelectorFloating, "block_selector_floating");
        RecurrentComplex.cremapper.registerLegacyIDs(blockSelectorFloating, "blockSelectorFloating");

        inventoryGenerationTag = (ItemInventoryGenMultiTag) new ItemInventoryGenMultiTag().setUnlocalizedName("inventoryGenerationTag");
        inventoryGenerationTag.setCreativeTab(tabInventoryGenerators);
        register(inventoryGenerationTag, "inventory_generation_tag");
        RecurrentComplex.cremapper.registerLegacyIDs(inventoryGenerationTag, "inventoryGenerationTag");

        inventoryGenerationSingleTag = (ItemInventoryGenSingleTag) new ItemInventoryGenSingleTag().setUnlocalizedName("inventoryGenerationSingleTag");
        inventoryGenerationSingleTag.setCreativeTab(tabInventoryGenerators);
        register(inventoryGenerationSingleTag, "inventory_generation_single_tag");
        RecurrentComplex.cremapper.registerLegacyIDs(inventoryGenerationSingleTag, "inventoryGenerationSingleTag");

        inventoryGenerationComponentTag = (ItemInventoryGenComponentTag) new ItemInventoryGenComponentTag().setUnlocalizedName("inventoryGenerationComponentTag");
        inventoryGenerationComponentTag.setCreativeTab(tabInventoryGenerators);
        register(inventoryGenerationComponentTag, "inventory_generation_component_tag");

        artifactGenerationTag = new ItemArtifactGenerator().setUnlocalizedName("artifactGenerationTag");
        artifactGenerationTag.setCreativeTab(tabInventoryGenerators);
        register(artifactGenerationTag, "artifact_generation_tag");
        RecurrentComplex.cremapper.registerLegacyIDs(artifactGenerationTag, "artifactGenerationTag");

        bookGenerationTag = new ItemBookGenerator().setUnlocalizedName("bookGenerationTag");
        bookGenerationTag.setCreativeTab(tabInventoryGenerators);
        register(bookGenerationTag, "book_generation_tag");
        RecurrentComplex.cremapper.registerLegacyIDs(bookGenerationTag, "bookGenerationTag");

        genericSpace = (BlockGenericSpace) new BlockGenericSpace().setUnlocalizedName("negativeSpace");
        genericSpace.setCreativeTab(tabStructureTools);
        register(genericSpace, "generic_space", ItemBlockGenericSpace.class);
        RecurrentComplex.cremapper.registerLegacyIDs(genericSpace, true, "negativeSpace");

        genericSolid = new BlockGenericSolid().setUnlocalizedName("naturalFloor");
        genericSolid.setCreativeTab(tabStructureTools);
        register(genericSolid, "generic_solid", ItemBlockGenericSolid.class);
        RecurrentComplex.cremapper.registerLegacyIDs(genericSolid, true, "naturalFloor");

        structureGenerator = new BlockStructureGenerator().setUnlocalizedName("structureGenerator");
        register(structureGenerator, "structure_generator");
        register(TileEntityStructureGenerator.class, "RCStructureGenerator", "SGStructureGenerator");
        RecurrentComplex.cremapper.registerLegacyIDs(structureGenerator, true, "structureGenerator");

        mazeGenerator = new BlockMazeGenerator().setUnlocalizedName("mazeGenerator");
        register(mazeGenerator, "maze_generator");
        register(TileEntityMazeGenerator.class, "RCMazeGenerator", "SGMazeGenerator");
        RecurrentComplex.cremapper.registerLegacyIDs(mazeGenerator, true, "mazeGenerator");

        spawnCommands = new BlockSpawnCommand().setUnlocalizedName("spawn_command");
        register(spawnCommands, "weighted_command_block");
        register(TileEntitySpawnCommand.class, "RCSpawnCommand");
        RecurrentComplex.cremapper.registerLegacyIDs(spawnCommands, true, "spawnCommand");

        spawnScript = new BlockSpawnScript().setUnlocalizedName("spawn_script");
        spawnScript.setCreativeTab(tabStructureTools);
        register(spawnScript, "spawn_script");
        register(TileEntitySpawnScript.class, "RCSpawnScript");

        inspector = new ItemInspector().setUnlocalizedName("recinspector");
        inspector.setCreativeTab(tabStructureTools);
        register(inspector, "inspector");

        // Set preset defaults
        GenericPlacerPresets.instance().register("clear", false, new GenericPlacer(), new PresetRegistry.Metadata("Clear", "Do not place anywhere"));
        GenericPlacerPresets.instance().setDefault("clear");

        DimensionMatcherPresets.instance().register("clear", false, new ArrayList<>(), new PresetRegistry.Metadata("None", "No dimensions"));
        DimensionMatcherPresets.instance().setDefault("clear");

        BiomeMatcherPresets.instance().register("clear", false, new ArrayList<>(), new PresetRegistry.Metadata("None", "No biomes"));
        BiomeMatcherPresets.instance().setDefault("clear");

        WeightedBlockStatePresets.instance().register("clear", false, new ArrayList<>(), new PresetRegistry.Metadata("None", "No blocks"));
        WeightedBlockStatePresets.instance().setDefault("clear");

        TransfomerPresets.instance().register("clear", false, new TransformerMulti.Data(), new PresetRegistry.Metadata("None", "No transformers"));
        TransfomerPresets.instance().setDefault("clear");
    }

    public static void register(Item item, String id)
    {
        item.setRegistryName(id);

        if (!RecurrentComplex.isLite())
            GameRegistry.register(item);
        else
            specialRegistry.register(item.getRegistryName(), item);
    }

    public static void register(Block block, String id, ItemBlock item)
    {
        block.setRegistryName(id);
        item.setRegistryName(id);

        if (!RecurrentComplex.isLite())
        {
            GameRegistry.register(block);
            GameRegistry.register(item);
        }
        else
        {
            specialRegistry.register(block.getRegistryName(), block);
            specialRegistry.register(item.getRegistryName(), item);
        }
    }

    public static void register(Block block, String id)
    {
        register(block, id, new ItemBlock(block));
    }

    @Deprecated
    public static void register(Block block, String id, Class<? extends ItemBlock> itemClass, Object... itemArgs)
    {
        ItemBlock item = FMLUtils.constructItem(block, itemClass, itemArgs);
        register(block, id, item != null ? item : new ItemBlock(block));
    }

    public static void register(Class<? extends TileEntity> tileEntity, String id, String... alternatives)
    {
        if (!RecurrentComplex.isLite())
            GameRegistry.registerTileEntityWithAlternatives(tileEntity, id, alternatives);
        else
        {
            specialRegistry.register(id, tileEntity);
            for (String aid : alternatives) specialRegistry.register(aid, tileEntity);
        }
    }

    public static void load(FMLInitializationEvent event, RecurrentComplex mod)
    {
        MCRegistry mcRegistry = RecurrentComplex.specialRegistry;

        CapabilityUpdateRegistry.INSTANCE.register(StructureEntityInfo.CAPABILITY_KEY, StructureEntityInfo.CAPABILITY);

        RCBiomeDictionary.registerTypes();

        fileTypeRegistry.put(RCFileSuffix.STRUCTURE, new StructureSaveHandler.Loader());
        fileTypeRegistry.put(RCFileSuffix.INVENTORY_GENERATION_COMPONENT, new FileTypeHandlerRegistryString<>(RCFileSuffix.INVENTORY_GENERATION_COMPONENT,
                GenericItemCollectionRegistry.INSTANCE, ItemCollectionSaveHandler.INSTANCE::fromJSON));
        fileTypeRegistry.put(RCFileSuffix.POEM_THEME, new FileTypeHandlerRegistryString<>(RCFileSuffix.POEM_THEME,
                Poem.THEME_REGISTRY, Poem.Theme::fromFile));
        fileTypeRegistry.put(RCFileSuffix.NATURAL_CATEGORY, new FileTypeHandlerRegistryString<>(RCFileSuffix.NATURAL_CATEGORY,
                NaturalStructureSelector.CATEGORY_REGISTRY, NaturalStructureSelector.SimpleCategory.class));
        fileTypeRegistry.put(RCFileSuffix.BIOME_PRESET, BiomeMatcherPresets.instance().loader());
        fileTypeRegistry.put(RCFileSuffix.DIMENSION_PRESET, DimensionMatcherPresets.instance().loader());
        fileTypeRegistry.put(RCFileSuffix.BLOCK_PRESET, WeightedBlockStatePresets.instance().loader());
        fileTypeRegistry.put(RCFileSuffix.PLACER_PRESET, GenericPlacerPresets.instance().loader());
        fileTypeRegistry.put(RCFileSuffix.TRANSFORMER_PRESET, TransfomerPresets.instance().loader());

        WorldScriptRegistry worldScriptRegistry = WorldScriptRegistry.INSTANCE;
        worldScriptRegistry.register("multi", WorldScriptMulti.class);
        worldScriptRegistry.register("strucGen", WorldScriptStructureGenerator.class);
        worldScriptRegistry.register("mazeGen", WorldScriptMazeGenerator.class);
        worldScriptRegistry.register("command", WorldScriptCommand.class);

        SerializableStringTypeRegistry<Transformer> transformerRegistry = StructureRegistry.TRANSFORMERS;
        transformerRegistry.registerType("multi", TransformerMulti.class, new TransformerMulti.Serializer());
        transformerRegistry.registerType("worldscript", TransformerWorldScript.class, new TransformerWorldScript.Serializer(mcRegistry));
        transformerRegistry.registerType("villagereplace", TransformerVillageSpecific.class, new TransformerVillageSpecific.Serializer(mcRegistry));
        transformerRegistry.registerType("natural", TransformerNatural.class, new TransformerNatural.Serializer(mcRegistry));
        transformerRegistry.registerType("naturalAir", TransformerNaturalAir.class, new TransformerNaturalAir.Serializer(mcRegistry));
        transformerRegistry.registerType("pillar", TransformerPillar.class, new TransformerPillar.Serializer(mcRegistry));
        transformerRegistry.registerType("replaceAll", TransformerReplaceAll.class, new TransformerReplaceAll.Serializer(mcRegistry));
        transformerRegistry.registerType("replace", TransformerReplace.class, new TransformerReplace.Serializer(mcRegistry));
        transformerRegistry.registerType("ruins", TransformerRuins.class, new TransformerRuins.Serializer(mcRegistry));
        transformerRegistry.registerType("negativeSpace", TransformerNegativeSpace.class, new TransformerNegativeSpace.Serializer(mcRegistry));
        transformerRegistry.registerType("ensureBlocks", TransformerEnsureBlocks.class, new TransformerEnsureBlocks.Serializer(mcRegistry));
        transformerRegistry.registerType("propertyReplace", TransformerProperty.class, new TransformerProperty.Serializer(mcRegistry));

        SerializableStringTypeRegistry<StructureGenerationInfo> genInfoRegistry = StructureRegistry.GENERATION_INFOS;
        genInfoRegistry.registerType("natural", NaturalGenerationInfo.class, new NaturalGenerationInfo.Serializer());
        genInfoRegistry.registerType("structureList", StructureListGenerationInfo.class, new StructureListGenerationInfo.Serializer());
        genInfoRegistry.registerType("mazeComponent", MazeGenerationInfo.class, new MazeGenerationInfo.Serializer());
        genInfoRegistry.registerType("static", StaticGenerationInfo.class, new StaticGenerationInfo.Serializer());
        genInfoRegistry.registerType("vanilla", VanillaStructureGenerationInfo.class, new VanillaStructureGenerationInfo.Serializer());
        genInfoRegistry.registerType("sapling", SaplingGenerationInfo.class, new SaplingGenerationInfo.Serializer());
        genInfoRegistry.registerType("decoration", VanillaDecorationGenerationInfo.class, new VanillaDecorationGenerationInfo.Serializer());

        SerializableStringTypeRegistry<GenericPlacer.Factor> placerFactorRegistry = FactorRegistry.INSTANCE.getTypeRegistry();
        placerFactorRegistry.registerType("limit", FactorLimit.class, new FactorLimit.Serializer());
        placerFactorRegistry.registerType("match", FactorMatch.class, new FactorMatch.Serializer());

        SerializableStringTypeRegistry<FactorLimit.Ray> rayRegistry = FactorLimit.getRayRegistry();
        rayRegistry.registerType("dynpos", RayDynamicPosition.class, null);
        rayRegistry.registerType("move", RayMove.class, null);
        rayRegistry.registerType("matcher", RayMatcher.class, new RayMatcher.Serializer());
        rayRegistry.registerType("average", RayAverageMatcher.class, new RayAverageMatcher.Serializer());
        rayRegistry.registerType("dynmove", RayDynamicMove.class, null);

        MazeRuleRegistry mazeRuleRegistry = MazeRuleRegistry.INSTANCE;
        mazeRuleRegistry.register("connect", MazeRuleConnect.class);
        mazeRuleRegistry.register("connectall", MazeRuleConnectAll.class);

        OperationRegistry.register("strucGen", OperationGenerateStructure.class);
        OperationRegistry.register("schemGen", OperationGenerateSchematic.class);
        OperationRegistry.register("strucMove", OperationMoveStructure.class);

//        GameRegistry.registerWorldGenerator(new WorldGenStructures(), 50);
        RCInventoryGenerators.registerVanillaInventoryGenerators();
//        MapGenStructureIO.func_143031_a(GenericVillagePiece.class, "RcGSP");
//        VillagerRegistry.instance().registerVillageCreationHandler(new GenericVillageCreationHandler("DesertHut"));
    }

    protected static <T> void dumpAll(PresetRegistry<T> presets)
    {
        presets.allIDs().forEach(s -> savePreset(presets, s));
    }

    private static <T> void savePreset(PresetRegistry<T> registry, String s)
    {
        try
        {
            registry.save(s, true);
        }
        catch (IOException e)
        {
            RecurrentComplex.logger.error("Error saving preset: " + s, e);
        }
    }
}
