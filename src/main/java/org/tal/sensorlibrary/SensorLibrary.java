
package org.tal.sensorlibrary;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldListener;
import org.tal.redstonechips.circuit.CircuitLibrary;

/**
 *
 * @author Tal Eisenberg
 */
public class SensorLibrary extends CircuitLibrary {
    private static List<slotinput> slotdigitsCircuits = new ArrayList<slotinput>();
    private static List<beacon> chunkbeaconCircuits = new ArrayList<beacon>();

    @Override
    public Class[] getCircuitClasses() {
        return new Class[] {photocell.class, pirsensor.class, rangefinder.class, daytime.class, slotinput.class, beacon.class, spark.class};
    }

    @Override
    public void onEnable() {
        PlayerListener playerListener = new PlayerListener() {
            @Override
            public void onPlayerInteract(PlayerInteractEvent event) {
                for (slotinput circuit : slotdigitsCircuits)
                    circuit.onPlayerInteract(event);
            }
        };

        WorldListener chunkListener = new WorldListener() {

            @Override
            public void onChunkLoad(ChunkLoadEvent event) {
                for (beacon circuit : chunkbeaconCircuits)
                    circuit.onChunkLoad(event);
            }

            @Override
            public void onChunkUnload(ChunkUnloadEvent event) {
                for (beacon circuit : chunkbeaconCircuits)
                    circuit.onChunkUnload(event);
            }

        };
        getServer().getPluginManager().registerEvent(Type.PLAYER_INTERACT, playerListener, Priority.Normal, this);
        getServer().getPluginManager().registerEvent(Type.CHUNK_LOAD, chunkListener, Priority.Monitor, this);
        getServer().getPluginManager().registerEvent(Type.CHUNK_UNLOAD, chunkListener, Priority.Normal, this);
    }

    static void registerSlotdigitsCircuit(slotinput circuit) {
        slotdigitsCircuits.add(circuit);
    }

    static void deregisterSlotdigitsCircuit(slotinput circuit) {
        slotdigitsCircuits.remove(circuit);
    }

    static void registerChunkbeaconCircuit(beacon circuit) {
        chunkbeaconCircuits.add(circuit);
    }

    static boolean deregisterChunkbeaconCircuit(beacon circuit) {
        return chunkbeaconCircuits.remove(circuit);
    }

}
