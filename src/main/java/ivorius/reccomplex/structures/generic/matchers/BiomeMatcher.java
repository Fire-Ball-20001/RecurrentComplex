/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.structures.generic.matchers;

import com.google.common.collect.Lists;
import ivorius.ivtoolkit.tools.IvGsonHelper;
import ivorius.reccomplex.json.RCGsonHelper;
import ivorius.reccomplex.utils.FunctionExpressionCache;
import ivorius.reccomplex.utils.algebra.RCBoolAlgebra;
import joptsimple.internal.Strings;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

/**
 * Created by lukas on 19.09.14.
 */
public class BiomeMatcher extends FunctionExpressionCache<Boolean, Biome, Object> implements Predicate<Biome>
{
    public static final String BIOME_NAME_PREFIX = "name=";
    public static final String BIOME_ID_PREFIX = "id=";
    public static final String BIOME_TYPE_PREFIX = "type=";

    public BiomeMatcher(String expression)
    {
        super(RCBoolAlgebra.algebra(), true, TextFormatting.GREEN + "Any Biome", expression);

        addTypes(new BiomeNameVariableType(BIOME_NAME_PREFIX, ""));
        addTypes(new BiomeIDVariableType(BIOME_ID_PREFIX, ""), t -> t.alias("", ""));
        addTypes(new BiomeDictVariableType(BIOME_TYPE_PREFIX, ""), t -> t.alias("$", ""));

        testVariables();
    }

    public static String ofTypes(BiomeDictionary.Type... biomeTypes)
    {
        return BIOME_TYPE_PREFIX + Strings.join(Lists.transform(Arrays.asList(biomeTypes), input -> input != null ? IvGsonHelper.serializedName(input) : null), " & " + BIOME_TYPE_PREFIX);
    }

    public static Set<Biome> gatherAllBiomes()
    {
        Set<Biome> set = new HashSet<>();

        for (Biome biome : Biome.REGISTRY)
        {
            if (biome != null)
                set.add(biome);
        }

        for (BiomeDictionary.Type type : BiomeDictionary.Type.values())
        {
            try
            {
                Collections.addAll(set, BiomeDictionary.getBiomesForType(type));
            }
            catch (Exception ignored) // list f'd up by a biome mod
            {

            }
        }

        return set;
    }

    public Validity variableValidity()
    {
        return validity(gatherAllBiomes());
    }

    @Nonnull
    public String getDisplayString()
    {
        return getDisplayString(gatherAllBiomes());
    }

    @Override
    public boolean test(final Biome input)
    {
        return evaluate(input);
    }

    protected class BiomeNameVariableType extends VariableType<Boolean, Biome, Object>
    {
        public BiomeNameVariableType(String prefix, String suffix)
        {
            super(prefix, suffix);
        }

        @Override
        public Boolean evaluate(String var, Biome biome)
        {
            return biome.getBiomeName().equals(var);
        }

        @Override
        public Validity validity(final String var, final Object biomes)
        {
            return Biome.REGISTRY.getKeys().stream().map(Biome.REGISTRY::getObject).anyMatch(b -> b.getBiomeName().equals(var))
                    ? Validity.KNOWN : Validity.UNKNOWN;
        }
    }

    protected class BiomeIDVariableType extends VariableType<Boolean, Biome, Object>
    {
        public BiomeIDVariableType(String prefix, String suffix)
        {
            super(prefix, suffix);
        }

        @Override
        public Boolean evaluate(String var, Biome biome)
        {
            return Biome.REGISTRY.getObject(new ResourceLocation(var)) == biome;
        }

        @Override
        public Validity validity(final String var, final Object biomes)
        {
            return Biome.REGISTRY.containsKey(new ResourceLocation(var))
                    ? Validity.KNOWN : Validity.UNKNOWN;
        }
    }

    protected class BiomeDictVariableType extends VariableType<Boolean, Biome, Object>
    {
        public BiomeDictVariableType(String prefix, String suffix)
        {
            super(prefix, suffix);
        }

        @Override
        public Boolean evaluate(String var, Biome biome)
        {
            BiomeDictionary.Type type = RCGsonHelper.enumForNameIgnoreCase(var, BiomeDictionary.Type.values());
            return type != null && BiomeDictionary.isBiomeOfType(biome, type);
        }

        @Override
        public Validity validity(String var, Object biomes)
        {
            return RCGsonHelper.enumForNameIgnoreCase(var, BiomeDictionary.Type.values()) != null
                    ? Validity.KNOWN : Validity.UNKNOWN;
        }
    }
}
