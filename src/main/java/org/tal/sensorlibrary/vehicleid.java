
package org.tal.sensorlibrary;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.tal.redstonechips.circuit.Circuit;

/**
 *
 * @author Tal Eisenberg
 */
public class vehicleid extends Circuit {
    private int resetPin = 1;
    private int disablePin = 0;
    private boolean disabled = false;
    private int lastInterface = -1;

    @Override
    public void inputChange(int inIdx, boolean state) {
        if (inIdx==resetPin && state) {
            for (int i=0; i<outputs.length; i++) sendOutput(i, false);
        } else if (inIdx==disablePin) {
            disabled = state;
            if (disabled) {
                for (int i=0; i<outputs.length; i++) sendOutput(i, false);
                SensorLibrary.deregisterVehicelidCircuit(this);
            } else {
                SensorLibrary.registerVehicleidCircuit(this);
            }
        }
    }

    @Override
    protected boolean init(CommandSender sender, String[] args) {
        if (outputs.length==0 || interfaceBlocks.length==0) {
            error(sender, "Expecting at least 2 output pins and at least 1 interface block.");
            return false;
        }

        SensorLibrary.registerVehicleidCircuit(this);
        return true;
    }

    void onVehicleMove(VehicleMoveEvent event) {
        if (disabled) return;

        Location to = event.getTo();
        boolean found = false;

        for (int i=0; i<interfaceBlocks.length; i++) {
            Location in = interfaceBlocks[i];
            if (to.getBlockX()==in.getBlockX() && to.getBlockY()-in.getBlockY()<=1 && to.getBlockZ()==in.getBlockZ()) {
                found = true;
                if (i!=lastInterface) {
                    int vid = event.getVehicle().getEntityId();
                    if (hasDebuggers()) {
                        debug("Vehicle " + vid + " detected at interface block " + i);
                    }

                    sendInt(1, outputs.length-1, vid);
                    sendOutput(0, true);
                    sendOutput(0, false);
                    
                    lastInterface = i;
                    return;
                }
            }
        }

        if (!found) lastInterface = -1;
        // no match

    }

    @Override
    public void circuitShutdown() {
        SensorLibrary.deregisterVehicelidCircuit(this);
    }
}
