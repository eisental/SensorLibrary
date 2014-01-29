
package org.redstonechips.sensorlibrary;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.redstonechips.chip.Circuit;
import org.redstonechips.chip.io.InterfaceBlock;

/**
 *
 * @author Tal Eisenberg
 */
public class photocell extends Circuit {
    private static final BlockFace[] lightFaces = new BlockFace[] { BlockFace.DOWN, BlockFace.UP, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };

    private Location[] lightBlocks;

    @Override
    public void input(boolean state, int inIdx) {
        Block interfaceBlock = chip.interfaceBlocks[0].getLocation().getBlock();
        int level = averageLightLevelAround(interfaceBlock);
        if (chip.hasListeners()) debug("Average light level is " + level + " (in the range of 0-15).");

        if (outputlen==3) level = Math.round(level/2);
        else if (outputlen==2) level = Math.round(level/4);
        else if (outputlen==1) level = Math.round(level/8);

        this.writeInt(level, 0, outputlen);
    }

    @Override
    public Circuit init(String[] args) {
        if (inputlen!=1) error("Expecting 1 clock input pin.");
        if (outputlen<1 || outputlen>4) return error("Expecting 1 to 4 output data pins.");
        if (chip.interfaceBlocks.length==0) return error("Expecting at least 1 interface block.");
        else {
            lightBlocks = findLightBlocks();
            return this;
        }
    }

    private int averageLightLevelAround(Block block) {
        int ret = 0;
        for (Location lightBlock : lightBlocks) {
            ret += chip.world.getBlockAt(lightBlock).getLightLevel();
        }

        return Math.round(ret / lightBlocks.length);
    }

    private boolean isPartOfStructure(Block b) {
        for (Location s : chip.structure) {
            if (s.equals(b.getLocation())) {
                return true;
            }
        }

        return false;
    }

    private Location[] findLightBlocks() {
        List<Location> blocks = new ArrayList<Location>();

        for (InterfaceBlock i : chip.interfaceBlocks) {
            Block iBlock = i.getLocation().getBlock();
            for (BlockFace face : lightFaces) {
                Block faceBlock = iBlock.getRelative(face);
                if (!this.isPartOfStructure(faceBlock))
                    blocks.add(faceBlock.getLocation());
            }
        }

        return blocks.toArray(new Location[blocks.size()]);
    }
}
