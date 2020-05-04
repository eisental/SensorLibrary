
package org.redstonechips.sensorlibrary;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.redstonechips.RCPrefs;
import org.redstonechips.circuit.Circuit;
import org.redstonechips.event.EventListener;

/*
 *
 * @author Tal Eisenberg
 */
public class vehicleid extends Circuit {
    private final int resetPin = 1;
    private final int disablePin = 0;
    private boolean pinDisabled = false;
    private int lastInterface = -1;
    private byte distance = 1;
    private boolean sphere = false;

    @Override
    public void input(boolean state, int inIdx) {
        if (inIdx==resetPin && state) {
            for (int i=0; i<outputlen; i++) write(false, i);
            if (chip.hasListeners()) debug(chip.toString() + " reset.");
        } else if (inIdx==disablePin) {
            pinDisabled = state;
            if (pinDisabled) {
            	if (chip.hasListeners()) debug(chip.toString() + " disabled.");
                for (int i=0; i<outputlen; i++) write(false, i);
                SensorLibrary.eventDispatcher.unregisterListener(moveListener);
            } else {
            	if (chip.hasListeners()) debug(chip.toString() + " enabled."); 
            	SensorLibrary.eventDispatcher.registerListener(VehicleMoveEvent.class, moveListener);
            }
        }
    }

    @Override
    public Circuit init(String[] args) {
      	sphere = false;
       	int maxIdDistance = 3;
    	if (outputlen==0 || chip.interfaceBlocks.length==0) return error("Expecting at least 2 output pins and at least 1 interface block.");
        else {
         	if (args.length>0) {
         		for (byte i=0; i<args.length; i++) {
         			if ((args[i].toUpperCase().startsWith("D{") || args[i].toUpperCase().startsWith("DIST{")) && args[i].endsWith("}")) {
         				
         				if (getMaxIdDistance()!=-1) {
         					maxIdDistance = getMaxIdDistance(); 
         				}         				
         				try {                            
         					distance = Byte.decode(args[i].substring(args[i].indexOf("{")+1, args[i].length()-1));
         		            if (distance==0 || distance>maxIdDistance) return error("Bad distance argument: " + args[0] + ". Expecting a number between 1 and " + maxIdDistance + ". Set vehicleid.maxdistance in preferences.yml to extend the maximum distance.");
                            
                        } catch (NumberFormatException ne2) {
                        	return error("Bad distance argument: " + args[0] + ". Expecting a number between 1 and " + maxIdDistance);
                          }                        
         			}
         			else if (args[i].toUpperCase().equals("SPHERE")) sphere = true;
         			else if (args[i].toUpperCase().equals("AXIS")) sphere = false;
         		}	
         	}
        }
        SensorLibrary.eventDispatcher.registerListener(VehicleMoveEvent.class, moveListener);
        return this;
    }

    private final EventListener moveListener = new EventListener() {
    	
    	@Override
    	public void onEvent(Event e) {
        	if (pinDisabled) return;
        	VehicleMoveEvent v = (VehicleMoveEvent)e;
        	Location to = v.getTo();
        	boolean found = false;
        
        	for (int i=0; i<chip.interfaceBlocks.length; i++) {
            	Location in = chip.interfaceBlocks[i].getLocation();
            	if (checkSphereOrAxis(to, in)) {
                	found = true;
                	if (i!=lastInterface) {
                    	int vid = v.getVehicle().getEntityId();
                    	if (chip.hasListeners()) {
                    		debug("Vehicle " + vid + " detected at interface block " + i);
                    	}
                    
                    	writeInt(vid, 1, outputlen-1);
                    	write(true, 0);
                    	write(false, 0);
                    
                    	lastInterface = i;
                    	return;
                	}
            	}
        	}
        
        	if (!found) lastInterface = -1; // no match
    	}
    };
   
    private boolean checkSphereOrAxis(Location too, Location inn){    	
        if (sphere) {
        	boolean distanceSphere = (Math.abs(too.getBlockX()-inn.getBlockX())<=distance && Math.abs(too.getBlockY()-inn.getBlockY())<=distance && Math.abs(too.getBlockZ()-inn.getBlockZ())<=distance);
        	if (distanceSphere) return true;
        }
        else if (!sphere) {
        boolean distanceCheckRight = (Math.abs(too.getBlockX()-inn.getBlockX())<=distance && too.getBlockY()-inn.getBlockY()==0 && too.getBlockZ()-inn.getBlockZ()==0);
        boolean distanceCheckLeft = (Math.abs(inn.getBlockX() - too.getBlockX())<=distance && too.getBlockY()-inn.getBlockY()==0 && too.getBlockZ()-inn.getBlockZ()==0);
        boolean distanceCheckFront = (Math.abs(too.getBlockZ()-inn.getBlockZ())<=distance && too.getBlockY()-inn.getBlockY()==0 && too.getBlockX()-inn.getBlockX()==0);
        boolean distanceCheckBack = (Math.abs(inn.getBlockZ()-too.getBlockZ())<=distance && too.getBlockY()-inn.getBlockY()==0 && too.getBlockX()-inn.getBlockX()==0);                
        boolean distanceCheckAbove = (Math.abs(too.getBlockY()-inn.getBlockY())<=distance && too.getBlockX()-inn.getBlockX()==0 && too.getBlockZ()-inn.getBlockZ()==0);
        boolean distanceCheckBelow = (Math.abs(inn.getBlockY()-too.getBlockY())<=distance && too.getBlockX()-inn.getBlockX()==0 && too.getBlockZ()-inn.getBlockZ()==0);
        boolean axisDistance = distanceCheckRight || distanceCheckLeft || distanceCheckFront || distanceCheckBack || distanceCheckAbove ||  distanceCheckBelow;
        	if (axisDistance) return true;
        }
        return false;
    	
    }
    private int getMaxIdDistance() {
        Object oMaxDist = RCPrefs.getPref("vehicleid.maxDistance");
        if (oMaxDist != null && oMaxDist instanceof Integer) return (Integer)oMaxDist;
        else return -1;
    }

    @Override
    public void shutdown() {
    	SensorLibrary.eventDispatcher.unregisterListener(moveListener);
    }
    @Override
    public void disable() {
    	SensorLibrary.eventDispatcher.unregisterListener(moveListener);
    }
    @Override
    public void destroyed(){
    	SensorLibrary.eventDispatcher.unregisterListener(moveListener);
    }
}
