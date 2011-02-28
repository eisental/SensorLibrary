package org.tal.sensorlibrary;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.tal.redstonechips.circuit.Circuit;

/**
 *
 * @author Tal Eisenberg
 */
public class rangefinder extends Circuit {
    int range = 10;
    BlockFace direction = null;
    BlockFace left, right;
    Block startBlock;
    BlockVector originVector;
    List<Location> cuboid;
    int maxOutput;

    @Override
    public void inputChange(int inIdx, boolean state) {
        if (state) {
            trigger();
        }
    }

    @Override
    protected boolean init(CommandSender sender, String[] args) {
        if (inputs.length!=1) {
            error(sender, "Expecting 1 clock input pin.");
            return false;
        }

        if (outputs.length==0) {
            error(sender, "Expecting at least 1 output pin.");
            return false;
        }

        if (interfaceBlocks.length!=1) {
            error(sender, "Expecting 1 interface block.");
            return false;
        }

        if (args.length>0) {
            try {
                range = Integer.decode(args[0]);
            } catch (NumberFormatException ne) {
                error(sender, "Bad range argument: " + args[0]);
                return false;
            }
        }

        Block iblock = world.getBlockAt(interfaceBlocks[0].getBlockX(), interfaceBlocks[0].getBlockY(), interfaceBlocks[0].getBlockZ());

        for (BlockFace b : BlockFace.values()) {
            Block block = iblock.getFace(b);
            if (block.getType()==Material.NOTE_BLOCK) {
                if (cuboid!=null) {
                    error(sender, "Expecting no more than one note block.");
                    return false;
                } else {
                    direction = b;
                    startBlock = block;
                    originVector = new BlockVector(block.getX()+0.5, block.getY()+0.5, block.getZ()+0.5);
                    createCuboid();
                }
            }
        }

        if (direction==null) {
            error(sender, "Couldn't find a note block attached to any of the interface block's faces");
            return false;
        }

        maxOutput = (int)Math.pow(2, outputs.length)-1;

        return true;
    }

    private void trigger() {
        // go over each block in the direction until we find either an entity or a non air block.

        List<Vector> objectsInRange = new ArrayList<Vector>();

        for (Location l : cuboid) {
            int type = world.getBlockTypeIdAt(l.getBlockX(), l.getBlockY(), l.getBlockZ());
            if (type!=Material.AIR.getId() && type!=Material.WATER.getId() && type!=Material.STATIONARY_WATER.getId()) {
                objectsInRange.add(new BlockVector(l.getBlockX()+0.5, l.getBlockY()+0.5, l.getBlockZ()+0.5));
                break;
            }
        }

        for (Entity e : world.getEntities()) {
            if (cuboid.contains(new Location(e.getWorld(), e.getLocation().getBlockX(), e.getLocation().getBlockY(), e.getLocation().getBlockZ()))) {
                objectsInRange.add(new Vector(e.getLocation().getX(), e.getLocation().getY(), e.getLocation().getZ()));
            }
        }

        double minDist = range*range;

        for (Vector v : objectsInRange) {
            double vdist = v.distanceSquared(originVector);
            if (vdist<minDist) {
                minDist = vdist;
            }

        }

        double dist = Math.sqrt(minDist);

        int out = (int)Math.round((dist/range)*maxOutput);
        if (hasDebuggers()) debug("Found object at " + dist + " meters.");
        this.sendInt(0, outputs.length, out);
    }

    private static BlockFace getLeftFace(BlockFace direction) {
        if (direction==BlockFace.WEST) return BlockFace.SOUTH;
        else if (direction==BlockFace.EAST) return BlockFace.NORTH;
        else if (direction==BlockFace.SOUTH) return BlockFace.EAST;
        else if (direction==BlockFace.NORTH) return BlockFace.WEST;
        else throw new IllegalArgumentException("Invalid block face: " + direction);
    }

    private static BlockFace getRightFace(BlockFace direction) {
        if (direction==BlockFace.WEST) return BlockFace.NORTH;
        else if (direction==BlockFace.EAST) return BlockFace.SOUTH;
        else if (direction==BlockFace.SOUTH) return BlockFace.WEST;
        else if (direction==BlockFace.NORTH) return BlockFace.EAST;
        else throw new IllegalArgumentException("Invalid block face: " + direction);
    }

    private void createCuboid() {
        cuboid = new ArrayList<Location>();
        if (direction!=BlockFace.UP && direction!=BlockFace.DOWN) {
            BlockFace l = getLeftFace(direction);
            BlockFace r = getRightFace(direction);
            for (int i=0; i<range; i++) { // 3x3xrange box

                Block c = startBlock.getFace(direction, i+1);
                cuboid.add(c.getLocation());
                cuboid.add(c.getFace(r).getLocation());
                cuboid.add(c.getFace(l).getLocation());

                Block u = c.getFace(BlockFace.UP);
                cuboid.add(u.getLocation());
                cuboid.add(u.getFace(r).getLocation());
                cuboid.add(u.getFace(l).getLocation());

                Block d = c.getFace(BlockFace.DOWN);
                cuboid.add(d.getLocation());
                cuboid.add(d.getFace(r).getLocation());
                cuboid.add(d.getFace(l).getLocation());
            }
        } else {
            for (int i=0; i<range; i++) {

                Block c = startBlock.getFace(direction, i+1);
                cuboid.add(c.getLocation());
                cuboid.add(c.getFace(BlockFace.EAST).getLocation());
                cuboid.add(c.getFace(BlockFace.NORTH).getLocation());
                cuboid.add(c.getFace(BlockFace.NORTH_EAST).getLocation());
                cuboid.add(c.getFace(BlockFace.NORTH_WEST).getLocation());
                cuboid.add(c.getFace(BlockFace.SOUTH).getLocation());
                cuboid.add(c.getFace(BlockFace.SOUTH_EAST).getLocation());
                cuboid.add(c.getFace(BlockFace.SOUTH_WEST).getLocation());
                cuboid.add(c.getFace(BlockFace.WEST).getLocation());

            }
        }
    }

}
