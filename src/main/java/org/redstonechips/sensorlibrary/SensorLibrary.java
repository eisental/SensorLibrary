
package org.redstonechips.sensorlibrary;

import org.redstonechips.circuit.CircuitLibrary;
import org.redstonechips.event.EventDispatcher;

/**
 *
 * @author Tal Eisenberg
 */
public class SensorLibrary extends CircuitLibrary {
    public static EventDispatcher eventDispatcher = new EventDispatcher();
    
    @Override
    public Class[] getCircuitClasses() {
        return new Class[] {photocell.class, pirsensor.class, rangefinder.class, daytime.class, slotinput.class, 
            beacon.class, spark.class, liquidlevel.class, playerid.class, vehicleid.class};
    }
    
    @Override
    public void onEnable() {
        eventDispatcher.bindToPlugin(this);
    }
    
    @Override
    public void onDisable() {
        eventDispatcher.stop();
    }
}
