
package org.tal.sensorlibrary;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Chunk;
import org.bukkit.command.CommandSender;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.tal.redstonechips.channel.TransmittingCircuit;
import org.tal.redstonechips.util.BitSet7;
import org.tal.redstonechips.util.ChunkLocation;

/**
 *
 * @author Tal Eisenberg
 */
public class beacon extends TransmittingCircuit {
    private BitSet7 bit;
    private boolean keepalive = false;
    private int radius = 0;
    
    private int loadCount = 0;
    private ChunkLocation centerChunk;

    @Override
    public void inputChange(int inIdx, boolean state) { 
        if (inIdx==0) keepalive = state;
    }

    @Override
    protected boolean init(CommandSender sender, String[] args) {
        if (inputs.length>1 || interfaceBlocks.length!=1) {
            error(sender, "Expecting 1 interface block and no more than 1 input pin.");
            return false;
        }

        if (args.length==0) {
            error(sender, "Broadcast channel name argument is missing.");
            return false;
        }

        if (args.length>1) {
            try {
                radius = Integer.decode(args[1]);

                if (radius<0) {
                    error(sender, "Bad radius argument: " + args[1]);
                    return false;
                }
            } catch (NumberFormatException ne) {
                error(sender, "Bad radius argument: " + args[1]);
                return false;
            }
        }

        bit = new BitSet7(1);
        centerChunk = ChunkLocation.fromLocation(interfaceBlocks[0]);

        initWireless(sender, args[0]);

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

        return true;
    }

    @Override
    public void circuitShutdown() {
        SensorLibrary.deregisterChunkbeaconCircuit(this);
        redstoneChips.removeTransmitter(this);
    }

    @Override
    public int getChannelLength() {
        return 1;
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
                if (hasDebuggers()) debug("Chunk (" + chunk.getX() + ", " + chunk.getZ() + ") in " + world .getName()+ " is kept alive.");
                event.setCancelled(true);
            } else {
                loadCount--;
                sendBit();
            }
        }
    }

    private void sendBit() {
        boolean loaded = loadCount>0;
        if (bit.get(0)==loaded) return;
        
        if (hasDebuggers()) {
            if (loaded) debug("Region loaded.");
            else debug("Region unloaded.");
        }
        
        bit.set(0, loaded);
        
        getChannel().transmit(bit, getStartBit(), 1);
    }

    private boolean isChunkInRange(ChunkLocation chunk) {
        if (chunk.getWorld().getId()!=world.getId()) return false;

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
                    
                    redstoneChips.getCircuitManager().updateOnChunkLoad(loc);
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
                inputBits.set(0);
            }

        }
    }

    @Override
    protected boolean isStateless() {
        return false;
    }
}
