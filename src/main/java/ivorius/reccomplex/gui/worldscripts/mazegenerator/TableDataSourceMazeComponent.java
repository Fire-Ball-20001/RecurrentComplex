/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.gui.worldscripts.mazegenerator;

import ivorius.ivtoolkit.maze.classic.MazeRoom;
import ivorius.ivtoolkit.tools.IvTranslations;
import ivorius.reccomplex.gui.table.TableDelegate;
import ivorius.reccomplex.gui.table.TableNavigator;
import ivorius.reccomplex.gui.table.cell.TableCellMultiBuilder;
import ivorius.reccomplex.gui.table.datasource.TableDataSourceSegmented;
import ivorius.reccomplex.gui.worldscripts.mazegenerator.reachability.TableDataSourceMazeReachability;
import ivorius.reccomplex.world.gen.feature.structure.generic.maze.SavedMazeComponent;
import ivorius.reccomplex.world.gen.feature.structure.generic.maze.SavedMazeReachability;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import java.util.function.Function;

/**
 * Created by lukas on 26.04.15.
 */
public class TableDataSourceMazeComponent extends TableDataSourceSegmented
{
    public static final int[] DEFAULT_MAX_COMPONENT_SIZE = {100, 100, 100};

    protected SavedMazeComponent component;

    protected TableNavigator navigator;
    protected TableDelegate tableDelegate;

    protected Function<MazeRoom, BlockPos> realWorldMapper;

    public TableDataSourceMazeComponent(SavedMazeComponent component, TableNavigator navigator, TableDelegate tableDelegate)
    {
        this.component = component;
        this.navigator = navigator;
        this.tableDelegate = tableDelegate;
        addManagedSegment(0, new TableDataSourceConnector(component.defaultConnector, IvTranslations.get("reccomplex.maze.connector.default")));

        addManagedSegment(1, TableCellMultiBuilder.create(navigator, tableDelegate)
                .addNavigation(() -> new TableDataSourceSelection(component.rooms, DEFAULT_MAX_COMPONENT_SIZE, tableDelegate, navigator, false)
                        .visualizing(realWorldMapper))
                .buildDataSource(IvTranslations.get("reccomplex.generationInfo.mazeComponent.rooms"), IvTranslations.getLines("reccomplex.generationInfo.mazeComponent.rooms.tooltip")));

        addManagedSegment(2, TableCellMultiBuilder.create(navigator, tableDelegate)
                .addNavigation(() -> new TableDataSourceMazePathConnectionList(component.exitPaths, tableDelegate, navigator, component.rooms))
                .enabled(() -> component.rooms.size() > 0)
                .buildDataSource(IvTranslations.get("reccomplex.generationInfo.mazeComponent.exits"), IvTranslations.getLines("reccomplex.generationInfo.mazeComponent.exits.tooltip")));

        addManagedSegment(3, TableCellMultiBuilder.create(navigator, tableDelegate)
                .addNavigation(() -> new TableDataSourceMazeReachability(component.reachability, tableDelegate, navigator, SavedMazeReachability.buildExpected(component), component.rooms))
                .enabled(() -> component.rooms.size() > 0)
                .buildDataSource(IvTranslations.get("reccomplex.generationInfo.mazeComponent.reachability"), IvTranslations.formatLines("reccomplex.reachability.tooltip")));

    }

    public TableDataSourceMazeComponent visualizing(Function<MazeRoom, BlockPos> realWorldMapper)
    {
        this.realWorldMapper = realWorldMapper;
        return this;
    }

    @Nonnull
    @Override
    public String title()
    {
        return "Maze Component";
    }

    @Override
    public int numberOfSegments()
    {
        return 4;
    }
}
