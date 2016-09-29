/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.structures.generic.maze;

import ivorius.ivtoolkit.blocks.BlockPositions;
import ivorius.ivtoolkit.math.AxisAlignedTransform2D;
import ivorius.ivtoolkit.tools.NBTCompoundObject;
import ivorius.reccomplex.structures.StructureInfo;
import ivorius.reccomplex.structures.StructureRegistry;
import ivorius.reccomplex.utils.NBTStorable;
import ivorius.reccomplex.worldgen.StructureGenerator;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

/**
 * Created by lukas on 16.04.15.
 */
public class PlacedStructure implements NBTCompoundObject
{
    public String structureID;
    public String generationInfoID;
    public AxisAlignedTransform2D transform;
    public BlockPos lowerCoord;

    public NBTStorable instanceData;

    public PlacedStructure(String structureID, String generationInfoID, AxisAlignedTransform2D transform, BlockPos lowerCoord, NBTStorable instanceData)
    {
        this.structureID = structureID;
        this.generationInfoID = generationInfoID;
        this.transform = transform;
        this.lowerCoord = lowerCoord;
        this.instanceData = instanceData;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        structureID = compound.getString("structureID");
        generationInfoID = compound.hasKey(generationInfoID, Constants.NBT.TAG_STRING) ? compound.getString("generationInfoID") : null;
        transform = AxisAlignedTransform2D.from(compound.getInteger("rotation"), compound.getBoolean("mirrorX"));
        lowerCoord = BlockPositions.readFromNBT("lowerCoord", compound);

        StructureInfo<?> structureInfo = StructureRegistry.INSTANCE.get(structureID);

        instanceData = compound.hasKey("instanceData", Constants.NBT.TAG_COMPOUND) && structureInfo != null
                ? new StructureGenerator<>().structure((StructureInfo<NBTStorable>) structureInfo).instanceData(compound.getTag("instanceData"))
                .transform(transform).lowerCoord(lowerCoord).instanceData().orElse(null)
                : null;
    }

    @Override
    public void writeToNBT(NBTTagCompound compound)
    {
        compound.setString("structureID", structureID);
        if (generationInfoID != null) compound.setString("generationInfoID", generationInfoID);
        compound.setInteger("rotation", transform.getRotation());
        compound.setBoolean("mirrorX", transform.isMirrorX());
        BlockPositions.writeToNBT("lowerCoord", lowerCoord, compound);
        if (instanceData != null)
            compound.setTag("instanceData", instanceData.writeToNBT());
    }
}
