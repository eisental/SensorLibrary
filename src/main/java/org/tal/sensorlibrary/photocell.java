
package org.tal.sensorlibrary;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.tal.redstonechips.circuit.Circuit;

/**
 *
 * @author Tal Eisenberg
 */
public class photocell extends Circuit {
    private static final BlockFace[] lightFaces = new BlockFace[] { BlockFace.DOWN, BlockFace.UP, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };

    private Location[] lightBlocks;

    @Override
    public void inputChange(int inIdx, boolean state) {
        Block interfaceBlock = world.getBlockAt(interfaceBlocks[0].getBlockX(), interfaceBlocks[0].getBlockY(), interfaceBlocks[0].getBlockZ());
        int level = averageLightLevelAround(interfaceBlock);
        if (hasDebuggers()) debug("Average light level is " + level + " (in the range of 0-15).");

        if (outputs.length==3) level = Math.round(level/2);
        else if (outputs.length==2) level = Math.round(level/4);
        else if (outputs.length==1) level = Math.round(level/8);

        this.sendInt(0, outputs.length, level);
    }

    @Override
    protected boolean init(CommandSender sender, String[] args) {
        if (inputs.length!=1) {
            error(sender, "Expecting 1 clock input pin.");
            return false;
        }

        if (outputs.length<1 || outputs.length>4) {
            error(sender, "Expecting 1 to 4 output data pins.");
            return false;
        }

        if (interfaceBlocks.length==0) {
            error(sender, "Expecting at least 1 interface block.");
            return false;
        }

        lightBlocks = findLightBlocks();

        return true;
    }

    private int averageLightLevelAround(Block block) {
        int ret = 0;
        for (Location lightBlock : lightBlocks) {
            ret += world.getBlockAt(lightBlock).getLightLevel();
        }

        return Math.round(ret / lightBlocks.length);
    }

    private boolean isPartOfStructure(Block b) {
        for (Location s : structure) {
            if (s.equals(b.getLocation())) {
                return true;
            }
        }

        return false;
    }

    private Location[] findLightBlocks() {
        List<Location> blocks = new ArrayList<Location>();

        for (Location l : interfaceBlocks) {
            Block iBlock = world.getBlockAt(l);
            for (BlockFace face : lightFaces) {
                Block faceBlock = iBlock.getFace(face);
                if (!this.isPartOfStructure(faceBlock))
                    blocks.add(faceBlock.getLocation());
            }
        }

        return blocks.toArray(new Location[blocks.size()]);
    }


}
