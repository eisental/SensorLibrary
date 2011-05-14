
package org.tal.sensorlibrary;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.vehicle.VehicleListener;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldListener;
import org.tal.redstonechips.circuit.CircuitLibrary;

/**
 *
 * @author Tal Eisenberg
 */
public class SensorLibrary extends CircuitLibrary {
    private static List<slotinput> slotinputCircuits = new ArrayList<slotinput>();
    private static List<beacon> chunkbeaconCircuits = new ArrayList<beacon>();
    private static List<vehicleid> vehicleidCircuits = new ArrayList<vehicleid>();

    @Override
    public Class[] getCircuitClasses() {
        return new Class[] {photocell.class, pirsensor.class, rangefinder.class, daytime.class, slotinput.class, beacon.class, spark.class, vehicleid.class};
    }

    @Override
    public void onEnable() {
        PlayerListener playerListener = new PlayerListener() {
            @Override
            public void onPlayerInteract(PlayerInteractEvent event) {
                for (slotinput circuit : slotinputCircuits)
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

        VehicleListener vehicleListener = new VehicleListener() {
            @Override
            public void onVehicleMove(VehicleMoveEvent event) {
                for (vehicleid circuit : vehicleidCircuits)
                    circuit.onVehicleMove(event);
            }
        };

        getServer().getPluginManager().registerEvent(Type.PLAYER_INTERACT, playerListener, Priority.Normal, this);
        getServer().getPluginManager().registerEvent(Type.CHUNK_LOAD, chunkListener, Priority.Monitor, this);
        getServer().getPluginManager().registerEvent(Type.CHUNK_UNLOAD, chunkListener, Priority.Normal, this);
        getServer().getPluginManager().registerEvent(Type.VEHICLE_MOVE, vehicleListener, Priority.Monitor, this);
    }

    static void registerSlotinputCircuit(slotinput circuit) {
        if (!slotinputCircuits.contains(circuit))
            slotinputCircuits.add(circuit);
    }

    static boolean deregisterSlotinputCircuit(slotinput circuit) {
        return slotinputCircuits.remove(circuit);
    }

    static void registerChunkbeaconCircuit(beacon circuit) {
        if (!chunkbeaconCircuits.contains(circuit))
            chunkbeaconCircuits.add(circuit);
    }

    static boolean deregisterChunkbeaconCircuit(beacon circuit) {
        return chunkbeaconCircuits.remove(circuit);
    }

    static void registerVehicleidCircuit(vehicleid circuit) {
        if (!vehicleidCircuits.contains(circuit))
            vehicleidCircuits.add(circuit);
    }

    static boolean deregisterVehicelidCircuit(vehicleid circuit) {
        return vehicleidCircuits.remove(circuit);
    }
}
