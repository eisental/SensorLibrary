
package org.redstonechips.sensorlibrary;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Chunk;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.redstonechips.chip.Circuit;
import org.redstonechips.wireless.Transmitter;
import org.redstonechips.util.ChunkLocation;

/**
 *
 * @author Tal Eisenberg
 */
public class beacon extends Circuit {
    private boolean keepalive = false;
    private int radius = 0;
    
    private int loadCount = 0;
    private ChunkLocation centerChunk;
    
    private Transmitter transmitter;
    
    @Override
    public void input(boolean state, int inIdx) { 
        if (inIdx==0) keepalive = state;
    }

    @Override
    public Circuit init(String[] args) {
        if (inputlen>1 || chip.interfaceBlocks.length!=1)
            return error("Expecting 1 interface block and no more than 1 input pin.");

        if (args.length==0) return error("Broadcast channel name argument is missing.");
        else if (args.length>1) {
            try {
                radius = Integer.decode(args[1]);
                if (radius<0) return error("Bad radius argument: " + args[1]);
            } catch (NumberFormatException ne) {
                return error("Bad radius argument: " + args[1]);
            }
        }

        centerChunk = ChunkLocation.fromLocation(chip.interfaceBlocks[0].getLocation());
        transmitter = new Transmitter();
        transmitter.init(activator, args[0], 1, this);

        for (int x=centerChunk.getX()-radius; x<=centerChunk.getX()+radius; x++) {
            for (int z=centerChunk.getZ()-radius; z<=centerChunk.getZ()+radius; z++) {
                ChunkLocation loc = new ChunkLocation(x,z, centerChunk.getWorld());
                if (isChunkInRange(loc) && loc.isChunkLoaded()) {
                    loadCount++;
                }
            }
        }

        sendBit();

        SensorLibrary.registerChunkbeaconCircuit(this);

        return this;
    }

    @Override
    public void shutdown() {
        SensorLibrary.deregisterChunkbeaconCircuit(this);
    }

    void onChunkLoad(ChunkLoadEvent event) {
        if (isChunkInRange(ChunkLocation.fromChunk(event.getChunk()))) {
            loadCount++;

            sendBit();
        }
    }

    void onChunkUnload(ChunkUnloadEvent event) {
        if (isChunkInRange(ChunkLocation.fromChunk(event.getChunk()))) {
            Chunk chunk = event.getChunk();
            if (keepalive) {
                if (chip.hasListeners()) debug("Chunk (" + chunk.getX() + ", " + chunk.getZ() + ") in " + chip.world.getName()+ " is kept alive.");
                event.setCancelled(true);
            } else {
                loadCount--;
                sendBit();
            }
        }
    }

    private void sendBit() {
        boolean loaded = loadCount>0;
        
        if (chip.hasListeners()) {
            if (loaded) debug("Region loaded.");
            else debug("Region unloaded.");
        }
        
        transmitter.transmit(loaded);
    }

    private boolean isChunkInRange(ChunkLocation chunk) {
        if (chunk.getWorld().getUID()!=chip.world.getUID()) return false;

        int dx = centerChunk.getX() - chunk.getX();
        int dz = centerChunk.getZ() - chunk.getZ();
        return (dx*dx + dz*dz <= radius*radius);
    }

    private void loadChunksInRadius() {
        for (int x=centerChunk.getX()-radius; x<=centerChunk.getX()+radius; x++) {
            for (int z=centerChunk.getZ()-radius; z<=centerChunk.getZ()+radius; z++) {
                ChunkLocation loc = new ChunkLocation(x,z, centerChunk.getWorld());
                if (isChunkInRange(loc)) {
                    loc.loadChunk();
                    
                    rc.chipManager().maybeChipChunkLoaded(loc);
                }
            }
        }
    }

    @Override
    public Map<String, String> getInternalState() {
        Map<String,String> state = new HashMap<String,String>();
        state.put("keepalive", Boolean.toString(keepalive));
        return state;
    }

    @Override
    public void setInternalState(Map<String, String> state) {
        if (state.containsKey("keepalive")) {
            keepalive = Boolean.parseBoolean(state.get("keepalive"));
            if (keepalive) {
                loadChunksInRadius();
                inputs[0] = true;
            }

        }
    }

    @Override
    public boolean isStateless() {
        return false;
    }
}
