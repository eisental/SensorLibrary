
package org.redstonechips.sensorlibrary;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerMoveEvent;
import org.redstonechips.circuit.Circuit;
import org.redstonechips.event.EventListener;

/*
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
                SensorLibrary.eventDispatcher.unregisterListener(moveListener);
            } else {
            	SensorLibrary.eventDispatcher.registerListener(PlayerMoveEvent.class, moveListener);
            }
        }
    }

    @Override
    public Circuit init(String[] args) {
        System.out.println("init ran");
    	if (outputlen==0 || chip.interfaceBlocks.length==0) {
            return error("Expecting at least 2 output pins and at least 1 interface block.");
        } else {
        	//SensorLibrary.eventDispatcher.registerPlayerListener(PlayerMoveEvent.class, playerListener);
        	SensorLibrary.eventDispatcher.registerListener(PlayerMoveEvent.class, moveListener);
        	//SensorLibrary.registerPlayeridCircuit(this);
            return this;
        }
    }
    
    private final EventListener moveListener = new EventListener() {

        @Override
        public void onEvent(Event e) {
            PlayerMoveEvent p = (PlayerMoveEvent)e;
        	if (pinDisabled) return;

            Location to = p.getTo();
            boolean found = false;

            for (int i=0; i<chip.interfaceBlocks.length; i++) {
                Location in = chip.interfaceBlocks[i].getLocation();                
                boolean distanceCheckRight = (to.getBlockX()-in.getBlockX()==1 && to.getBlockY()-in.getBlockY()==0 && to.getBlockZ()-in.getBlockZ()==0);
                boolean distanceCheckLeft = (in.getBlockX() - to.getBlockX()==1 && to.getBlockY()-in.getBlockY()==0 && to.getBlockZ()-in.getBlockZ()==0);
                boolean distanceCheckFront = (to.getBlockZ()-in.getBlockZ()==1 && to.getBlockY()-in.getBlockY()==0 && to.getBlockX()-in.getBlockX()==0);
                boolean distanceCheckBack = (in.getBlockZ()-to.getBlockZ()==1 && to.getBlockY()-in.getBlockY()==0 && to.getBlockX()-in.getBlockX()==0);                
                boolean distanceCheckAbove = (to.getBlockY()-in.getBlockY()==1 && to.getBlockX()-in.getBlockX()==0 && to.getBlockZ()-in.getBlockZ()==0);
                boolean distanceCheckBelow = (in.getBlockY()-to.getBlockY()==1 && to.getBlockX()-in.getBlockX()==0 && to.getBlockZ()-in.getBlockZ()==0);
                boolean withinDistance = distanceCheckRight || distanceCheckLeft || distanceCheckFront || distanceCheckBack || distanceCheckAbove ||  distanceCheckBelow;
                System.out.println("withinDistance = " + withinDistance);
                if (withinDistance) {
                    found = true;
                    if (i!=lastInterface) {
                        int pid = p.getPlayer().getEntityId();
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
                else {
                	writeInt(0, 1, outputlen-1);

                }
            }

            if (!found) lastInterface = -1;
            // no match
        }

        
    };

     
    @Override
    public void shutdown() {
    	SensorLibrary.eventDispatcher.unregisterListener(moveListener);
    }
}
