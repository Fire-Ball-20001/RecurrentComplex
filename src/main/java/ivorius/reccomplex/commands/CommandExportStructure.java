/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.commands;

import ivorius.ivtoolkit.blocks.BlockArea;
import ivorius.reccomplex.files.SimpleCustomizableRegistry;
import net.minecraft.command.CommandException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import ivorius.ivtoolkit.tools.IvWorldData;
import ivorius.reccomplex.RCConfig;
import ivorius.reccomplex.entities.StructureEntityInfo;
import ivorius.reccomplex.network.PacketEditStructureHandler;
import ivorius.reccomplex.structures.StructureRegistry;
import ivorius.reccomplex.structures.StructureInfo;
import ivorius.reccomplex.structures.generic.GenericStructureInfo;
import ivorius.reccomplex.utils.ServerTranslations;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Created by lukas on 25.05.14.
 */
public class CommandExportStructure extends CommandBase
{
    @Override
    public String getCommandName()
    {
        return RCConfig.commandPrefix + "export";
    }

    public int getRequiredPermissionLevel()
    {
        return 4;
    }

    @Override
    public String getCommandUsage(ICommandSender var1)
    {
        return ServerTranslations.usage("commands.strucExport.usage");
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender commandSender, String[] args) throws CommandException
    {
        EntityPlayerMP player = getCommandSenderAsPlayer(commandSender);

        BlockArea area;

//        if (args.length >= 6)
//        {
//            x = commandSender.getPlayerCoordinates().posX;
//            y = commandSender.getPlayerCoordinates().posY;
//            z = commandSender.getPlayerCoordinates().posZ;
//            x = MathHelper.floor_double(func_110666_a(commandSender, (double) x, args[0]));
//            y = MathHelper.floor_double(func_110666_a(commandSender, (double) y, args[1]));
//            z = MathHelper.floor_double(func_110666_a(commandSender, (double) z, args[2]));
//
//            width = Integer.valueOf(args[3]);
//            height = Integer.valueOf(args[4]);
//            length = Integer.valueOf(args[5]);
//        }
//        else
        {
            StructureEntityInfo structureEntityInfo = RCCommands.getStructureEntityInfo(player);

            if (structureEntityInfo.hasValidSelection())
            {
                area = new BlockArea(structureEntityInfo.selectedPoint1, structureEntityInfo.selectedPoint2);
            }
            else
            {
                throw ServerTranslations.wrongUsageException("commands.selectModify.noSelection");
            }
        }

        GenericStructureInfo genericStructureInfo;
        String structureID;

        if (args.length >= 1)
        {
            genericStructureInfo = getGenericStructureInfo(args[0]);
            structureID = args[0];
        }
        else
        {
            genericStructureInfo = GenericStructureInfo.createDefaultStructure();
            structureID = "NewStructure";
            genericStructureInfo.metadata.authors = commandSender.getName();
        }

        BlockPos lowerCoord = area.getLowerCorner();
        BlockPos higherCoord = area.getHigherCorner();

        IvWorldData data = IvWorldData.capture(player.getEntityWorld(), new BlockArea(lowerCoord, higherCoord), true);
        genericStructureInfo.worldDataCompound = data.createTagCompound(lowerCoord);
        PacketEditStructureHandler.openEditStructure(genericStructureInfo, structureID, player);
    }

    @Override
    public List<String> getTabCompletionOptions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos)
    {
        if (args.length == 1)
        {
            return getListOfStringsMatchingLastWord(args, StructureRegistry.INSTANCE.ids());
        }

        return null;
    }

    public static GenericStructureInfo getGenericStructureInfo(String name) throws CommandException
    {
        StructureInfo structureInfo = StructureRegistry.INSTANCE.get(name);

        if (structureInfo == null)
            throw ServerTranslations.commandException("commands.structure.notRegistered", name);

        GenericStructureInfo genericStructureInfo = structureInfo.copyAsGenericStructureInfo();

        if (genericStructureInfo == null)
            throw ServerTranslations.commandException("commands.structure.notGeneric", name);

        return genericStructureInfo;
    }
}
