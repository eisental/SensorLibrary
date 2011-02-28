/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.tal.sensorlibrary;

import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;
import org.tal.redstonechips.RedstoneChips;
import org.tal.redstonechips.circuit.CircuitLibrary;

/**
 *
 * @author Tal Eisenberg
 */
public class SensorLibrary extends JavaPlugin {
    public static final Logger logger = Logger.getLogger("Minecraft");

    public SensorLibrary() {
        new CircuitLibrary() {
            @Override
            public Class[] getCircuitClasses() {
                return new Class[] {photocell.class, pirsensor.class, rangefinder.class, daytime.class};
            }
        };
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
    }

}
