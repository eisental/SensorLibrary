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
    int[] cuboidSize = new int[]{3, 3};
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
            if (args.length>1) {
                String[] split = args[1].split("x");
                int x = Integer.parseInt(split[0]);
                cuboidSize[0] = x;
                cuboidSize[1] = split.length == 1 ? x : Integer.parseInt(split[1]);
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
        double dist = isInRange(objectsInRange);

        int out =  (int)dist;
        if(dist > 15)
            out = (int)((dist/range)*maxOutput);
        if(out == 0 && dist > 0)
            out = 1;
        else if(dist > range)
            out = 0;
        if (hasDebuggers()) debug("Found object at " + dist + " meters.");
        this.sendInt(0, outputs.length, out);
    }

    private void createCuboid() {
        cuboid = new ArrayList<Location>();
        if (direction==BlockFace.UP || direction==BlockFace.DOWN) {
            BlockFace direction2 = BlockFace.EAST;
            BlockFace direction2b = BlockFace.WEST;
            Block t1 = startBlock.getFace(direction);
            for (int i=0; i<range; i++) {
                Block t2 = t1;
                Block t2b = t1;
                for (int i2=1; i2<cuboidSize[0];) {
                    t2 = t2.getFace(direction2);
                    cuboid.add(t2.getLocation());
                    cuboidAddDirection(cuboid, t2, BlockFace.NORTH, 0, (int)Math.ceil((cuboidSize[1]-1)/2d));
                    i2++;
                    if(i2<cuboidSize[0])
                    {
                        t2b = t2b.getFace(direction2b);
                        cuboid.add(t2b.getLocation());
                        cuboidAddDirection(cuboid, t2b, BlockFace.SOUTH, 0, (int)((cuboidSize[1]-1)/2));
                        i2++;
                    }
                }
                cuboid.add(t1.getLocation());
                t1 = t1.getFace(direction);
            }
        } else {
            BlockFace direction2 = null;
            BlockFace direction2b = null;
            if(direction == BlockFace.EAST || direction == BlockFace.WEST)
                direction2 = BlockFace.NORTH;
            else if(direction == BlockFace.NORTH || direction == BlockFace.SOUTH)
                direction2 = BlockFace.EAST;
            direction2b = direction2.getOppositeFace();
            Block t1 = startBlock.getFace(direction);
            for (int i=0; i<range; i++) {
                Block t2 = t1;
                Block t2b = t1;
                for (int i2=1; i2<cuboidSize[0];) {
                    t2 = t2.getFace(direction2);
                    cuboid.add(t2.getLocation());
                    cuboidAddDirection(cuboid, t2, BlockFace.UP, 0, (int)Math.ceil((cuboidSize[1]-1)/2d));
                    i2++;
                    if(i2<cuboidSize[0])
                    {
                        t2b = t2b.getFace(direction2b);
                        cuboid.add(t2b.getLocation());
                        cuboidAddDirection(cuboid, t2b, BlockFace.DOWN, 0, (int)((cuboidSize[1]-1)/2));
                        i2++;
                    }
                }
                cuboid.add(t1.getLocation());
                t1 = t1.getFace(direction);
            }
        }
    }
    private void cuboidAddDirection(List<Location> cuboid, Block b, BlockFace direction, int i, int max)
    {
        if(i < max)
        {
            b = b.getFace(direction);
            cuboid.add(b.getLocation());
            cuboidAddDirection(cuboid, b, direction, ++i, max);
        }
    }
    private double isInRange(List<Vector> objectsInRange)
    {
        double minDist = range+1;
        if(direction == BlockFace.EAST || direction == BlockFace.WEST)
        {
            for (Vector v : objectsInRange) {
                double vdist = Math.abs(originVector.getZ()-v.getZ());
                if (vdist<minDist) {
                    minDist = vdist;
                }
            }
        }
        else if(direction == BlockFace.NORTH || direction == BlockFace.SOUTH)
        {
            for (Vector v : objectsInRange) {
                double vdist = Math.abs(originVector.getX()-v.getX());
                if (vdist<minDist) {
                    minDist = vdist;
                }
            }
        }
        else if(direction == BlockFace.UP || direction == BlockFace.DOWN)
        {
            for (Vector v : objectsInRange) {
                double vdist = Math.abs(originVector.getY()-v.getY());
                if (vdist<minDist) {
                    minDist = vdist;
                }
            }
        }
        return minDist;
    }
}