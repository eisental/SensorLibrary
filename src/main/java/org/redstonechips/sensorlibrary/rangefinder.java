package org.redstonechips.sensorlibrary;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.redstonechips.circuit.Circuit;
import org.redstonechips.util.Locations;
import org.redstonechips.wireless.Transmitter;

/**
 *
 * @author Tal Eisenberg
 */
public class rangefinder extends Circuit {
    private int range = 10, maxOutput, dataOutputCount;

    private boolean scaleToFit = false;
    
    private BlockFace direction;
    
    private Location origin, corner1, corner2;        
    
    private Transmitter transmitter;
    
    @Override
    public void input(boolean state, int inIdx) {
        if (state) trigger();
    }

    @Override
    public Circuit init(String[] args) {
        int cuboidSize[] = new int[] {3,3};
                
        // -- parse arguments --
        if (args.length>0) {
            List<String> arglist = new ArrayList<String>(Arrays.asList(args));
            
            // -- 'scale' --            
            scaleToFit = arglist.remove("scale");
            
            // -- #channel --                        
            for (String arg : arglist) {
                if (arg.charAt(0)=='#') {
                    initTransmitter(activator, arg);
                    arglist.remove(arg);                    
                    break;
                }
            }
            
            if (!arglist.isEmpty()) {
                // -- range --
                try {
                    range = Integer.decode(arglist.get(0));
                } catch (NumberFormatException ne) {
                    return error("Bad range argument: " + arglist.get(0));
                }

                // -- size --
                if (arglist.size()>1) {
                    try {
                        cuboidSize = parseSize(arglist.get(1));
                    } catch (NumberFormatException ne) {
                        return error("Bad size argument: " + arglist.get(1));
                    }
                }
            }
        }

        // -- validate I/O --
        if (inputlen!=1) return error("Expecting 1 clock input pin.");
        if (chip.interfaceBlocks.length!=1) return error("Expecting 1 interface block."); 
        
        if (transmitter==null) {
            if (outputlen<2) return error("Expecting at least 2 output pins.");
            dataOutputCount = outputlen-1;
        } 
        
        if (dataOutputCount<=0) return error("Found 0 data outputs pins or channel bits.");
        
        // -- auto down-scale when range exceeds outputs --
        maxOutput = (int)Math.pow(2, dataOutputCount)-1;
        if (range >= maxOutput) scaleToFit = true;
        
        // -- calculate detection cuboid --
        try {
            Location il = chip.interfaceBlocks[0].getLocation();
            BlockFace face = Locations.getDirectionBlock(il.getBlock());
            direction = face;
            origin = findOriginVector();

            createCuboid(Locations.getFace(il, face), direction, cuboidSize[0], cuboidSize[1], range);
            //for (Location l : cuboid) l.getBlock().setType(Material.GLASS);
                        
            // add 2nd interface block to the chip structure
            List<Location> locs = new ArrayList<Location>(Arrays.asList(chip.structure));
            locs.add(Locations.getFace(il, direction));
            chip.structure = locs.toArray(new Location[0]);
            
        } catch (IllegalArgumentException ie) {            
            this.shutdown();
            return error(ie.getMessage());
        }
        
        info("range: " + range + " output-size: " + dataOutputCount + " bits. scale " + (scaleToFit?"on.":"off."));        
        return this;        
    }

    private int[] parseSize(String str) {
        String[] split = str.split("x");                    
        int cuboidWidth = Integer.parseInt(split[0]);
        int cuboidHeight = split.length == 1 ? cuboidWidth : Integer.parseInt(split[1]);
        return new int[] {cuboidWidth, cuboidHeight};
    }
    
    private void initTransmitter(CommandSender sender, String channelString) {
        int length;
        String channel;
        
        int aposIdx = channelString.indexOf("'");
        if (aposIdx!=-1) {
            String lenString = channelString.substring(aposIdx+1);
            channel = channelString.substring(0, aposIdx);
            try {
                length = Integer.parseInt(lenString);
            } catch (NumberFormatException e) {
                error("Bad channel string: " + channelString);
                return;
            }
        } else {
            length = (int)Math.ceil(Math.log(range)/Math.log(2));
            channel = channelString;
        }
        
        transmitter = new Transmitter();
        transmitter.init(sender, channel, length, this);
        dataOutputCount = length;        
    }

    @Override
    public void shutdown() {
        if (transmitter!=null) transmitter.shutdown();
    }
    
    // -------------- Object detection ----------------
    
    private final List<Location> objectsInRange = new ArrayList<Location>();
    private static final double playerHeight = 1.8;
    
    static private final NumberFormat debugFormat;
    static {
        debugFormat = NumberFormat.getNumberInstance();
        debugFormat.setMaximumFractionDigits(2);
    }
    
    private void trigger() {
        objectsInRange.clear();
        BlockFace oppositeFace = direction.getOppositeFace();
        for (int x = corner1.getBlockX(); x<=corner2.getBlockX(); x++) 
            for (int y = corner1.getBlockY(); y<=corner2.getBlockY(); y++) 
                for (int z = corner1.getBlockZ(); z<=corner2.getBlockZ(); z++) {
                    Material type = chip.world.getBlockAt(x, y, z).getType();

                    if (type!=Material.AIR && type!=Material.WATER) {
                        objectsInRange.add(Locations.getFaceCenter(chip.world, x,y,z, oppositeFace));
                    }
                    
                }

        for (Entity e : chip.world.getEntities()) {
            Location l = e.getLocation(); 
            //System.out.println(corner1 + ", " + corner2 + ": " + l);
            if (l.getX()>=corner1.getX() && l.getX()<=corner2.getX() &&
                l.getY()>=corner1.getY() && l.getY()<=corner2.getY() &&
                l.getZ()>=corner1.getZ() && l.getZ()<=corner2.getZ()) {
                    if (e.getType()==EntityType.PLAYER) {
                        Location lp = l.clone();
                        switch (direction) {
                            case UP:
                                lp.subtract(0, playerHeight, 0);
                            case DOWN:
                                lp.add(0, playerHeight, 0);
                                break;
                            case EAST:
                                lp.add(-0.8, 0, 0);
                                break;
                            case WEST:
                                lp.add(-0.2, 0, 0);
                                break;
                            case SOUTH:
                                lp.add(0,0,-0.8);
                                break;
                            case NORTH:
                                lp.add(0,0,-0.2);
                                break;
						default:
							break;
                           
                           
                        }
                        objectsInRange.add(lp);
                    } else {
                        objectsInRange.add(l);
                    }
            }
        }

        if (objectsInRange.isEmpty()) {
            foundNothing();
        } else {            
            double dist = findDistance(objectsInRange);
            if (dist>range) foundNothing();
            else {
                foundSomething(dist);
            }
        }
    }

    private void foundNothing() {
        if (chip.hasListeners()) debug("No object found in range.");
        if (transmitter==null) {
            this.writeInt(0, 1, dataOutputCount);
            this.write(false, 0);
        }
    }

    private void foundSomething(double dist) {
        int out;
        if (scaleToFit) out = (dist<=0 ? 0 : (int)Math.round((dist / range) * maxOutput));                    
        else out = (int)Math.floor(dist);

        if (chip.hasListeners()) debug("Found object at " + debugFormat.format(dist) + " meters.");
        
        if (transmitter==null) {
            this.writeInt(out, 1, dataOutputCount);
            this.write(true, 0);        
        } else {
            transmitter.transmit(out, 0, dataOutputCount);
        }
    }
    
    // -------------------- Detection area cuboid ---------------------
    
    private void createCuboid(Location start, BlockFace direction, int width, int height, int length) {
        
        int minx = width==1 ? 0 : -(int)(width/2);
        int maxx = width+minx-1;        
        int miny = height==1 ? 0 : -(int)(height/2);
        int maxy = height+miny-1;
        
        corner1 = start.clone();
        corner2 = start.clone();
        if (direction==BlockFace.UP) { // y
            corner1.add(minx, 1, miny);
            corner2.add(maxx, length+1, maxy);
        } else if (direction==BlockFace.DOWN) { // -y
            corner1.add(minx, -length-1, miny);
            corner2.add(maxx, -1, maxy);
        } else if (direction==BlockFace.EAST) { // +x
            corner1.add(1, miny, minx);
            corner2.add(length+1, maxy, maxx);
        } else if (direction==BlockFace.WEST) { // -x
            corner1.add(-length-0.2, miny, minx);
            corner2.add(-0.2, maxy, maxx);
        } else if (direction==BlockFace.SOUTH) { // +z
            corner1.add(minx, miny, 1);
            corner2.add(maxx, maxy, length+1);
        } else if (direction==BlockFace.NORTH) { // -z
            corner1.add(minx, miny, -length-1);
            corner2.add(maxx, maxy, -0.2);
        }
        
        /*
        for (int x = corner1.getBlockX(); x<=corner2.getBlockX(); x++) 
            for (int y = corner1.getBlockY(); y<=corner2.getBlockY(); y++) 
                for (int z = corner1.getBlockZ(); z<=corner2.getBlockZ(); z++) {
                    corner1.getWorld().getBlockAt(x, y, z).setType(Material.GLASS);
                }
        */
    }

    private Location findOriginVector() {
        Location loc = chip.interfaceBlocks[0].getLocation();
        loc = Locations.getFace(loc, direction);
        return Locations.getFaceCenter(loc, direction);
    }
    
    private double findDistance(List<Location> objectsInRange) {        
        double closest = range+1;

        if(direction == BlockFace.SOUTH || direction == BlockFace.NORTH) {
            for (Location loc : objectsInRange) {
                double dist = Math.abs(origin.getZ()-loc.getZ());
                if (dist<closest) closest = dist;
            }
        } else if (direction == BlockFace.EAST || direction == BlockFace.WEST) {
            for (Location loc : objectsInRange) {
                double dist = Math.abs(origin.getX()-loc.getX());
                if (dist<closest) closest = dist;
            }
        } else if (direction == BlockFace.UP || direction == BlockFace.DOWN) {
            for (Location loc : objectsInRange) {
                double dist = Math.abs(origin.getY()-loc.getY());
                if (dist<closest) closest = dist;
            }
        }
        return closest;
    }
}