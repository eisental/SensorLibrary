package org.tal.sensorlibrary;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.material.MaterialData;
import org.tal.redstonechips.circuit.Circuit;
import org.tal.redstonechips.util.Locations;

/**
 *
 * @author Tal Eisenberg
 */
public class rangefinder extends Circuit {
    int range;
    boolean scaleToFit = false;
    BlockFace direction = null;
    BlockFace left, right;
    Location origin;
    int[] cuboidSize = new int[]{3, 3};
    List<Location> cuboid;
    int maxOutput;
    
    static private final NumberFormat debugFormat;
    static {
        debugFormat = NumberFormat.getNumberInstance();
        debugFormat.setMaximumFractionDigits(2);
    }
    
    
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
                if (args[1].equalsIgnoreCase("scale")) scaleToFit = true;
                else {
                    String[] split = args[1].split("x");
                    try {
                        int x = Integer.parseInt(split[0]);
                        cuboidSize[0] = x;
                        cuboidSize[1] = split.length == 1 ? x : Integer.parseInt(split[1]);
                    } catch (NumberFormatException ne) {
                        error(sender, "Bad size argument: " + args[1]);
                    }

                    if (args.length>2 && args[2].equalsIgnoreCase("scale")) {
                        scaleToFit = true;
                    }
                }
            }
        } else range = 10;

        Location in = interfaceBlocks[0].getLocation();

        try {
            BlockFace face = findDirectionBlock(in);
            direction = face;
            findOriginVector();
            createCuboid(Locations.getFace(in, face));

            maxOutput = (int)Math.pow(2, outputs.length)-1;
            if (range >= maxOutput) scaleToFit = true;
            List<Location> locs = new ArrayList<Location>();
            locs.addAll(Arrays.asList(structure));
            locs.add(Locations.getFace(in, direction));
            structure = locs.toArray(new Location[locs.size()]);

            return true;
        } catch (IllegalArgumentException ie) {
            error(sender, ie.getMessage());
            return false;
        }
    }

    private void findOriginVector() {
        origin = findFaceCenter(interfaceBlocks[0].getLocation(), direction);
    }

    private Location findFaceCenter(Location l, BlockFace face) {
        if (face==BlockFace.DOWN) {
            return new Location(world, l.getBlockX()+0.5, l.getBlockY(), l.getBlockZ()+0.5);
        } else if (face==BlockFace.UP) {
            return new Location(world, l.getBlockX()+0.5, l.getBlockY()+1, l.getBlockZ()+0.5);
        } else if (face==BlockFace.NORTH) {
            return new Location(world, l.getBlockX(), l.getBlockY()+0.5, l.getBlockZ()+0.5);
        } else if (face==BlockFace.SOUTH) {
            return new Location(world, l.getBlockX()+1, l.getBlockY()+0.5, l.getBlockZ()+0.5);
        } else if (face==BlockFace.EAST) {
            return new Location(world, l.getBlockX()+0.5, l.getBlockY()+0.5, l.getBlockZ());
        } else if (face==BlockFace.WEST) {
            return new Location(world, l.getBlockX()+0.5, l.getBlockY()+0.5, l.getBlockZ()+1);
        } else throw new IllegalArgumentException("Invalid direction: " + face.name());
    }

    private static final BlockFace[] faces = new BlockFace[] { BlockFace.NORTH, BlockFace.WEST, BlockFace.EAST, BlockFace.SOUTH, BlockFace.UP, BlockFace.DOWN };

    private BlockFace findDirectionBlock(Location l) throws IllegalArgumentException {
        MaterialData interfaceBlockType = redstoneChips.getPrefs().getInterfaceBlockType();
        Block block = l.getBlock();
        BlockFace ret = null;

        for (BlockFace face : faces) {
            Block b = block.getRelative(face);
            if (b.getType()==interfaceBlockType.getItemType()
                        && (b.getData()==interfaceBlockType.getData() || interfaceBlockType.getData()==-1)) {
                if (ret==null)
                    ret = face;
                else throw new IllegalArgumentException("Found too many interface blocks attached to each other.");
            }

        }

        if (ret==null)
            throw new IllegalArgumentException("Couldn't find another interface block attached to any of the interface block's faces.");
        return ret;
    }

    private void trigger() {
        List<Location> objectsInRange = new ArrayList<Location>();
        BlockFace oppositeFace = direction.getOppositeFace();
        for (Location l : cuboid) { 
            int type = world.getBlockTypeIdAt(l.getBlockX(), l.getBlockY(), l.getBlockZ());
            if (type!=Material.AIR.getId() && type!=Material.WATER.getId() && type!=Material.STATIONARY_WATER.getId()) {
                objectsInRange.add(findFaceCenter(l, oppositeFace));
                break;
            }
        }

        for (Entity e : world.getEntities()) {
            if (cuboid.contains(new Location(e.getWorld(), e.getLocation().getBlockX(), e.getLocation().getBlockY(), e.getLocation().getBlockZ()))) {
                objectsInRange.add(e.getLocation());
            }
        }

        double dist = findDistance(objectsInRange);

        int out;

        if (scaleToFit)
            if (dist>0)
                out = (int)Math.round((dist/range)*maxOutput);
            else out = 0;
        else {
            out = (int)dist;
            if (out == 0 && dist > 0)
                out = 1;
            else if (dist > range)
                out = 0;            
        }

        if (dist<=range && dist>0) {
            if (hasListeners()) debug("Found object at " + debugFormat.format(dist) + " meters.");                        
        } else debug("No object found in range.");
        
        this.sendInt(0, outputs.length, out);        
    }

    private void createCuboid(Location startBlock) {
        cuboid = new ArrayList<Location>();
        if (direction==BlockFace.UP || direction==BlockFace.DOWN) {
            BlockFace direction2 = BlockFace.EAST;
            BlockFace direction2b = BlockFace.WEST;
            Location t1 = Locations.getFace(startBlock, direction);
            for (int i=0; i<range; i++) {
                Location t2 = t1;
                Location t2b = t1;
                for (int i2=1; i2<cuboidSize[0];) {
                    t2 = Locations.getFace(t2, direction2);
                    cuboid.add(t2);
                    cuboidAddDirection(cuboid, t2, BlockFace.NORTH, 0, (int)Math.ceil((cuboidSize[1]-1)/2d));
                    i2++;
                    if(i2<cuboidSize[0])
                    {
                        t2b = Locations.getFace(t2b, direction2b);
                        cuboid.add(t2b);
                        cuboidAddDirection(cuboid, t2b, BlockFace.SOUTH, 0, (int)((cuboidSize[1]-1)/2));
                        i2++;
                    }
                }
                cuboid.add(t1);
                t1 = Locations.getFace(t1, direction);
            }
        } else {
            BlockFace direction2 = null;
            BlockFace direction2b = null;
            if(direction == BlockFace.EAST || direction == BlockFace.WEST)
                direction2 = BlockFace.NORTH;
            else if(direction == BlockFace.NORTH || direction == BlockFace.SOUTH)
                direction2 = BlockFace.EAST;
            direction2b = direction2.getOppositeFace();
            Location t1 = Locations.getFace(startBlock, direction);
            for (int i=0; i<range; i++) {
                Location t2 = t1;
                Location t2b = t1;
                for (int i2=1; i2<cuboidSize[0];) {
                    t2 = Locations.getFace(t2, direction2);
                    cuboid.add(t2);
                    cuboidAddDirection(cuboid, t2, BlockFace.UP, 0, (int)Math.ceil((cuboidSize[1]-1)/2d));
                    i2++;
                    if(i2<cuboidSize[0])
                    {
                        t2b = Locations.getFace(t2b, direction2b);
                        cuboid.add(t2b);
                        cuboidAddDirection(cuboid, t2b, BlockFace.DOWN, 0, (int)((cuboidSize[1]-1)/2));
                        i2++;
                    }
                }
                cuboid.add(t1);
                t1 = Locations.getFace(t1, direction);
            }
        }
    }

    private void cuboidAddDirection(List<Location> cuboid, Location l, BlockFace direction, int i, int max) {
        if (i < max) {
            l = Locations.getFace(l, direction);
            cuboid.add(l);
            cuboidAddDirection(cuboid, l, direction, ++i, max);
        }
    }

    private double findDistance(List<Location> objectsInRange) {
        double minDist = range+1;

        if(direction == BlockFace.EAST || direction == BlockFace.WEST) {
            for (Location loc : objectsInRange) {
                double vdist = Math.abs(origin.getZ()-loc.getZ());
                if (vdist<minDist) {
                    minDist = vdist;
                }
            }

        } else if(direction == BlockFace.NORTH || direction == BlockFace.SOUTH) {
            for (Location loc : objectsInRange) {
                double vdist = Math.abs(origin.getX()-loc.getX());
                if (vdist<minDist) {
                    minDist = vdist;
                }
            }

        } else if(direction == BlockFace.UP || direction == BlockFace.DOWN) {
            for (Location loc : objectsInRange) {
                double vdist = Math.abs(origin.getY()-loc.getY());
                if (vdist<minDist) {
                    minDist = vdist;
                }
            }

        }

        return minDist>range?0:minDist;
    }
}