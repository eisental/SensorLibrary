
package org.tal.sensorlibrary;

import java.util.ArrayList;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.tal.redstonechips.circuit.CircuitLibrary;

/**
 *
 * @author Tal Eisenberg
 */
public class SensorLibrary extends CircuitLibrary {
    private static ArrayList<slotdigits> slotdigitsCircuits = new ArrayList<slotdigits>();

    @Override
    public Class[] getCircuitClasses() {
        return new Class[] {photocell.class, pirsensor.class, rangefinder.class, daytime.class, slotdigits.class};
    }

    @Override
    public void onEnable() {
        PlayerListener playerListener = new PlayerListener() {
            @Override
            public void onPlayerInteract(PlayerInteractEvent event) {
                for (slotdigits circuit : slotdigitsCircuits)
                    circuit.onPlayerInteract(event);
            }
        };

        getServer().getPluginManager().registerEvent(Type.PLAYER_INTERACT, playerListener, Priority.Normal, this);
    }

    static void registerSlotdigitsCircuit(slotdigits circuit) {
        slotdigitsCircuits.add(circuit);
    }

    static void deregisterSlotdigitsCircuit(slotdigits circuit) {
        slotdigitsCircuits.remove(circuit);
    }

}
