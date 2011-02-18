/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.tal.sensorlibrary;

import java.io.File;
import org.bukkit.Server;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.tal.redstonechips.circuit.CircuitLibrary;

/**
 *
 * @author Tal Eisenberg
 */
public class SensorLibrary extends CircuitLibrary {

    public SensorLibrary(PluginLoader pluginLoader, Server instance, PluginDescriptionFile desc, File folder, File plugin, ClassLoader cLoader) {
        super(pluginLoader, instance, desc, folder, plugin, cLoader);
    }

    @Override
    public Class[] getCircuitClasses() {
        return new Class[] {photocell.class, pirsensor.class, ustransceiver.class, daytime.class};
    }

}
