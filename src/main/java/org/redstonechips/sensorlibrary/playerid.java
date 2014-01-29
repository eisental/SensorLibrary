
package org.redstonechips.sensorlibrary;

import org.bukkit.Location;
import org.bukkit.event.player.PlayerMoveEvent;
import org.redstonechips.circuit.Circuit;

/**
 *
 * @author Tal Eisenberg
 */
public class playerid extends Circuit {
    private final int resetPin = 1;
    private final int disablePin = 0;
    private boolean pinDisabled = false;
    private int lastInterface = -1;

    @Override
    public void input(boolean state, int inIdx) {
        if (inIdx==resetPin && state) {
            for (int i=0; i<outputlen; i++) write(false, i);
        } else if (inIdx==disablePin) {
            pinDisabled = state;
            if (pinDisabled) {
                for (int i=0; i<outputlen; i++) write(false, i);
                SensorLibrary.deregisterPlayeridCircuit(this);
            } else {
                SensorLibrary.registerPlayeridCircuit(this);
            }
        }
    }

    @Override
    public Circuit init(String[] args) {
        if (outputlen==0 || chip.interfaceBlocks.length==0) {
            return error("Expecting at least 2 output pins and at least 1 interface block.");
        } else {
            SensorLibrary.registerPlayeridCircuit(this);
            return this;
        }
    }

    void onPlayerMove(PlayerMoveEvent event) {
        if (pinDisabled) return;

        Location to = event.getTo();
        boolean found = false;

        for (int i=0; i<chip.interfaceBlocks.length; i++) {
            Location in = chip.interfaceBlocks[i].getLocation();
            if (to.getBlockX()==in.getBlockX() && to.getBlockY()-in.getBlockY()<=1 && to.getBlockZ()==in.getBlockZ()) {
                found = true;
                if (i!=lastInterface) {
                    int pid = event.getPlayer().getEntityId();
                    if (chip.hasListeners()) {
                        debug("Player " + pid + " detected at interface block " + i);
                    }

                    writeInt(pid, 1, outputlen-1);
                    write(true, 0);
                    write(false, 0);
                    
                    lastInterface = i;
                    return;
                }
            }
        }

        if (!found) lastInterface = -1;
        // no match

    }

    @Override
    public void shutdown() {
        SensorLibrary.deregisterPlayeridCircuit(this);
    }
}
