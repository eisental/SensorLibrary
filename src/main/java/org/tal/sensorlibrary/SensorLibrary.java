/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.tal.sensorlibrary;

import org.tal.redstonechips.circuit.CircuitLibrary;

/**
 *
 * @author Tal Eisenberg
 */
public class SensorLibrary extends CircuitLibrary {
    @Override
    public Class[] getCircuitClasses() {
        return new Class[] {photocell.class, pirsensor.class, rangefinder.class, daytime.class};
    }
}
