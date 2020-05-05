
package org.redstonechips.sensorlibrary;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerMoveEvent;
import org.redstonechips.RCPrefs;
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
    private byte distance = 1;
    private boolean sphere = false;
    
    private Boolean[] directionAim = new Boolean[] {
    		false,
    		false,
    		false,
    		false,
    		false,
    		false,
    };
    
    private static final String[] directionText = new String[] {
            "EAST",
            "WEST",
            "NORTH",
            "SOUTH",
            "ABOVE",
            "BELOW",            
    };
    
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
            	SensorLibrary.eventDispatcher.registerListener(PlayerMoveEvent.class, moveListener);
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
         		for (byte i=0; i<args.length; i++) { //go through all sign args
         			if ((args[i].toUpperCase().startsWith("D{") || args[i].toUpperCase().startsWith("DIST{")) && args[i].endsWith("}")) { //check if distance is specfied
         				
         				if (getMaxIdDistance()!=-1) {
         					maxIdDistance = getMaxIdDistance(); 
         				}         				
         				try {                            
         					distance = Byte.decode(args[i].substring(args[i].indexOf("{")+1, args[i].length()-1));
         		            if (distance==0 || distance>maxIdDistance) return error("Maximum distance exceeded: " + args[i] + ". The maximum distance is " + maxIdDistance + ". Set playerid.maxdistance in preferences.yml to extend the maximum distance.");
                            
                        } catch (NumberFormatException ne2) {
                        	return error("Bad distance argument: " + args[i] + ". Expecting a number between 1 and " + maxIdDistance);
                          }                        
         			}
         			else if (args[i].toUpperCase().equals("SPHERE")) sphere = true; //check if sphere or axis is on sign. No arg defaults to axis 
         			else if (args[i].toUpperCase().equals("AXIS")) sphere = false;
         			for (byte j=0; j<=5; j++) {
         				if (args[i].toUpperCase().equals(directionText[j])) { // if sign arg is a direction, mark that direction as true
         					directionAim[j] = true;
         				}
         			}
         		}	
         	}
        }
        SensorLibrary.eventDispatcher.registerListener(PlayerMoveEvent.class, moveListener);
        return this;
    }
    
    private final EventListener moveListener = new EventListener() {

        @Override
        public void onEvent(Event e) {
        	if (pinDisabled) return;
            PlayerMoveEvent p = (PlayerMoveEvent)e;
            Location to = p.getTo();
            boolean found = false;

            for (int i=0; i<chip.interfaceBlocks.length; i++) {
                Location in = chip.interfaceBlocks[i].getLocation();
                if (checkDistOfSphereOrAxis(to, in)) {
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
            }

            if (!found) lastInterface = -1; // no match
        }
    };
    
    private boolean checkDistOfSphereOrAxis(Location too, Location inn){  // check the distance of the player to interface block
        if (sphere) {

        	boolean distanceSphereWest = (inn.getBlockX()-too.getBlockX()<=distance && 
        								 inn.getBlockX()-too.getBlockX()>0 &&
        								  Math.abs(too.getBlockY()-inn.getBlockY())<=distance && 
           							      Math.abs(too.getBlockZ()-inn.getBlockZ())<=distance);
        	boolean distanceSphereEast = (too.getBlockX()-inn.getBlockX()<=distance && 
					 					  too.getBlockX()-inn.getBlockX()>0 &&
					 					  Math.abs(too.getBlockY()-inn.getBlockY())<=distance && 
					 					  Math.abs(too.getBlockZ()-inn.getBlockZ())<=distance);
        	boolean distanceSphereNorth = (inn.getBlockZ()-too.getBlockZ()<=distance && 
					  					   inn.getBlockZ()-too.getBlockZ()>0 &&
					  					   Math.abs(too.getBlockY()-inn.getBlockY())<=distance && 
					  					   Math.abs(too.getBlockX()-inn.getBlockX())<=distance);
        	boolean distanceSphereSouth = (too.getBlockZ()-inn.getBlockZ()<=distance &&
					   					   too.getBlockZ()-inn.getBlockZ()>0 &&
					   					   Math.abs(too.getBlockY()-inn.getBlockY())<=distance && 
					   					   Math.abs(too.getBlockX()-inn.getBlockX())<=distance);
        	boolean distanceSphereAbove = (too.getBlockY()-inn.getBlockY()<=distance && 
					   					   too.getBlockY()-inn.getBlockY()>0 &&
					   					   Math.abs(too.getBlockZ()-inn.getBlockZ())<=distance && 
					   					   Math.abs(too.getBlockX()-inn.getBlockX())<=distance);
        	boolean distanceSphereBelow = (inn.getBlockY()-too.getBlockY()<=distance && 
        								   inn.getBlockY()-too.getBlockY()>0 &&
        								   Math.abs(too.getBlockZ()-inn.getBlockZ())<=distance && 
        								   Math.abs(too.getBlockX()-inn.getBlockX())<=distance);
        	boolean distanceSphere = (distanceSphereEast&&!directionAim[1]&&!directionAim[2]&&!directionAim[3]&&!directionAim[4]&&!directionAim[5])
            		|| (distanceSphereWest&&!directionAim[0]&&!directionAim[2]&&!directionAim[3]&&!directionAim[4]&&!directionAim[5])
            		|| (distanceSphereNorth&&!directionAim[1]&&!directionAim[0]&&!directionAim[3]&&!directionAim[4]&&!directionAim[5])
            		|| (distanceSphereSouth&&!directionAim[1]&&!directionAim[2]&&!directionAim[0]&&!directionAim[4]&&!directionAim[5])
            		|| (distanceSphereAbove&&!directionAim[1]&&!directionAim[2]&&!directionAim[3]&&!directionAim[0]&&!directionAim[5])
            		|| (distanceSphereBelow&&!directionAim[1]&&!directionAim[2]&&!directionAim[3]&&!directionAim[4]&&!directionAim[0]);            
        	if (distanceSphere) return true;        	
        }
        else {
        boolean distanceCheckEast= (too.getBlockX()-inn.getBlockX()<=distance && 
        							too.getBlockX()-inn.getBlockX()>0 &&
        							too.getBlockY()-inn.getBlockY()==0 && 
        							too.getBlockZ()-inn.getBlockZ()==0); // can not use ABS as it will flag opposing sides as true instead of just one side
        
        boolean distanceCheckWest= (inn.getBlockX()-too.getBlockX()<=distance && 
        							inn.getBlockX()-too.getBlockX()>0 && 
        							too.getBlockY()-inn.getBlockY()==0 && 
        							too.getBlockZ()-inn.getBlockZ()==0);
        
        boolean distanceCheckNorth=(inn.getBlockZ()-too.getBlockZ()<=distance && 
        							inn.getBlockZ()-too.getBlockZ()>0 && 
        							too.getBlockY()-inn.getBlockY()==0 && 
        							too.getBlockX()-inn.getBlockX()==0);
        
        boolean distanceCheckSouth=(too.getBlockZ()-inn.getBlockZ()<=distance && 
        							too.getBlockZ()-inn.getBlockZ()>0 && 
        							too.getBlockY()-inn.getBlockY()==0 && 
        							too.getBlockX()-inn.getBlockX()==0);                
        
        boolean distanceCheckAbove=(too.getBlockY()-inn.getBlockY()<=distance && 
        							too.getBlockY()-inn.getBlockY()>0 && 
        							too.getBlockX()-inn.getBlockX()==0 && 
        							too.getBlockZ()-inn.getBlockZ()==0);
        
        boolean distanceCheckBelow=(inn.getBlockY()-too.getBlockY()<=distance && 
        							inn.getBlockY()-too.getBlockY()>0 && 
        							too.getBlockX()-inn.getBlockX()==0 && 
        							too.getBlockZ()-inn.getBlockZ()==0);
        
        boolean axisDistance = (distanceCheckEast&&!directionAim[1]&&!directionAim[2]&&!directionAim[3]&&!directionAim[4]&&!directionAim[5])
        		|| (distanceCheckWest&&!directionAim[0]&&!directionAim[2]&&!directionAim[3]&&!directionAim[4]&&!directionAim[5])
        		|| (distanceCheckNorth&&!directionAim[1]&&!directionAim[0]&&!directionAim[3]&&!directionAim[4]&&!directionAim[5])
        		|| (distanceCheckSouth&&!directionAim[1]&&!directionAim[2]&&!directionAim[0]&&!directionAim[4]&&!directionAim[5])
        		|| (distanceCheckAbove&&!directionAim[1]&&!directionAim[2]&&!directionAim[3]&&!directionAim[0]&&!directionAim[5])
        		|| (distanceCheckBelow&&!directionAim[1]&&!directionAim[2]&&!directionAim[3]&&!directionAim[4]&&!directionAim[0]);
        	if (axisDistance) return true;
        }
        return false;
    	
    }
    private int getMaxIdDistance() {
        Object oMaxDist = RCPrefs.getPref("playerid.maxDistance");
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
