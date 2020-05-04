
package org.redstonechips.sensorlibrary;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.BlockState;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.redstonechips.RedstoneChips;
import org.redstonechips.circuit.Circuit;
import org.redstonechips.event.EventListener;
import org.redstonechips.util.Locations;
import org.redstonechips.wireless.Transmitter;

/**
 *
 * @author taleisenberg
 */
public class liquidlevel extends Circuit {
    private static final Set<Material> liquids = new HashSet<>();
    static {
        liquids.add(Material.WATER);
        //liquids.add(Material.STATIONARY_WATER);
        liquids.add(Material.LAVA);
        //liquids.add(Material.STATIONARY_LAVA);
    }
    
    Map<Location,Byte> sides = new HashMap<>();
    Transmitter transmitter;

    double scale;
    
    @Override
    public Circuit init(String[] args) {
        if (!(chip.interfaceBlocks.length>0)) return error("Expecting at least one interface block.");
                
        // check for a channel argument.
        if (args.length>0 && args[0].startsWith("#")) {
            transmitter = new Transmitter();
            transmitter.init(activator, args[0], 4, this);
        }
        
        SensorLibrary.eventDispatcher.registerListener(BlockFromToEvent.class, fromToListener);
        SensorLibrary.eventDispatcher.registerListener(BlockPhysicsEvent.class, physicsListener);
        SensorLibrary.eventDispatcher.registerListener(BlockPlaceEvent.class, placeAndBreakListener);
        SensorLibrary.eventDispatcher.registerListener(BlockBreakEvent.class, placeAndBreakListener);
        SensorLibrary.eventDispatcher.registerListener(PlayerBucketFillEvent.class, bucketListener);
        SensorLibrary.eventDispatcher.registerListener(PlayerBucketEmptyEvent.class, bucketListener);
        
        double maxOutput = Math.pow(2, outputlen) - 1.0;
        scale = maxOutput/8.0;
        
        // store side locations of 1st interface block.

        for (Location side : Locations.getSides(chip.interfaceBlocks[0].getLocation())) {
            sides.put(side, (byte)0);
            updateSide(side);
        }
        
        return this;
        
    }
    
    private final EventListener physicsListener = new EventListener() {

        @Override
        public void onEvent(Event e) {
            BlockPhysicsEvent pe = (BlockPhysicsEvent)e;
            final Block b = pe.getBlock();
            final Location l = b.getLocation();
            if (sides.containsKey(l)) {
                RedstoneChips.inst().getServer().getScheduler().runTaskLater(RedstoneChips.inst(), new Runnable() {
                    @Override
                    public void run() {
                        updateSide(l);
                    }
                }, 6);
            }
        }
        
    };
            
    private final EventListener fromToListener = new EventListener() {
        @Override
        public void onEvent(Event e) {
            Location eventLoc = ((BlockFromToEvent)e).getToBlock().getLocation();
            if (sides.containsKey(eventLoc)) {
                updateSide(eventLoc);
            }
        }            
    };
    
    private final EventListener placeAndBreakListener = new EventListener() {
        @Override
        public void onEvent(Event e) {
            Location eventLoc = ((BlockEvent)e).getBlock().getLocation();
            if (sides.containsKey(eventLoc)) {
                updateSide(eventLoc);
            }
        }            
    };
    
    private final EventListener bucketListener = new EventListener() {        
        @Override
        public void onEvent(Event e) {
            PlayerBucketEvent bfe = (PlayerBucketEvent)e;
            Block b = bfe.getBlockClicked();
            final Location updatedLoc = Locations.getFace(b.getLocation(), bfe.getBlockFace());

            if (sides.containsKey(updatedLoc)) {
                updateSideLater(updatedLoc);
            }
        }
    };
    
    @Override
    public void shutdown() {
        SensorLibrary.eventDispatcher.unregisterListener(fromToListener);
        SensorLibrary.eventDispatcher.unregisterListener(physicsListener);
        SensorLibrary.eventDispatcher.unregisterListener(placeAndBreakListener);
        SensorLibrary.eventDispatcher.unregisterListener(bucketListener);        
    }

    private void updateSideLater(final Location side) {
        RedstoneChips.inst().getServer().getScheduler().runTaskLater(RedstoneChips.inst(), new Runnable() {
            @Override
            public void run() {
                updateSide(side);
            }
        }, 1);        
    }
    
    private void updateSide(Location side) {
        byte level = findWaterLevel(side);
        byte curlevel = sides.get(side);
        if (level!=curlevel) {
            sides.put(side, level);
            outputHighestLevel(sides);
        }
    }

    private byte findWaterLevel(Location l) {
        BlockState b = l.getBlock().getState();
        if (liquids.contains(b.getType())) {
                byte data = b.getData().getData();
                System.out.println("old = " + b);
                //System.out.println("new = " + b.getBlockData().getLevel());
                if (data>7) // falling liquid. full water block.
                    return 8;
                else                        
                    return (byte)(8-b.getData().getData());
        } else {
            return 0;
        }        
    }

    private void outputHighestLevel(Map<Location, Byte> sides) {
        int highest = 0;
        for (byte s : sides.values()) {
            if (s>highest) highest = s;
        }
        if (outputlen==1) { // outputs 0 when 0, otherwise outputs 1.
            if (chip.hasListeners()) debug("Water level updated: " + highest);
            writeWaterLevel(highest>0?1:0);
        } else if (scale<1) { // scale output to fit number of outputs.
            int scaled = (int)Math.round(scale * highest);
            if (chip.hasListeners()) debug("Water level updated: " + highest + " (scaled to " + scaled + ")");
            writeWaterLevel(scaled);
        } else {
            if (chip.hasListeners()) debug("Water level updated: " + highest);
            writeWaterLevel(highest);
        }
    }
    
    private void writeWaterLevel(int level) {
        if (outputlen>0)
            this.writeInt(level, 0, outputlen);
        
        if (transmitter!=null)
            transmitter.transmit(level, 0, 4);

    }
    
    @Override
    public void input(boolean state, int inIdx) {}
}
