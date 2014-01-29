
package org.redstonechips.sensorlibrary;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.redstonechips.circuit.CircuitLibrary;

/**
 *
 * @author Tal Eisenberg
 */
public class SensorLibrary extends CircuitLibrary implements Listener {
    private static final List<slotinput> slotinputCircuits = new ArrayList<slotinput>();
    private static final List<beacon> chunkbeaconCircuits = new ArrayList<beacon>();
    private static final List<vehicleid> vehicleidCircuits = new ArrayList<vehicleid>();
    private static final List<playerid> playeridCircuits = new ArrayList<playerid>();

    @Override
    public Class[] getCircuitClasses() {
        return new Class[] {photocell.class, pirsensor.class, rangefinder.class, daytime.class, slotinput.class, 
            beacon.class, spark.class, vehicleid.class, playerid.class };
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
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

    static boolean deregisterVehicleidCircuit(vehicleid circuit) {
        return vehicleidCircuits.remove(circuit);
    }
	
	static void registerPlayeridCircuit(playerid circuit) {
        if (!playeridCircuits.contains(circuit))
            playeridCircuits.add(circuit);
    }

    static boolean deregisterPlayeridCircuit(playerid circuit) {
        return playeridCircuits.remove(circuit);
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        for (slotinput circuit : slotinputCircuits)
            circuit.onPlayerInteract(event);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        for (playerid circuit : playeridCircuits)
            circuit.onPlayerMove(event);
    }
    
    @EventHandler (priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        for (beacon circuit : chunkbeaconCircuits)
            circuit.onChunkLoad(event);
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (beacon circuit : chunkbeaconCircuits)
            circuit.onChunkUnload(event);
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onVehicleMove(VehicleMoveEvent event) {
        for (vehicleid circuit : vehicleidCircuits)
            circuit.onVehicleMove(event);
    }    
}
