package org.tal.sensorlibrary;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.command.CommandSender;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.tal.redstonechips.circuit.Circuit;

public class slotdigits extends Circuit {

    int numberOfSets, wordlength;
    int[] results;

    @Override
    public void setInternalState(Map<String,String> state) {
        String[] split = state.get("values").split(",");
        for(int i=0;i<split.length;i++) {
            results[i]=Integer.decode(split[i]);
        }
    }

    @Override
    public Map<String,String> getInternalState() {
        String joined="";
        for(int number : results)
        {
                joined += new Integer(number).toString()+",";
        }
        HashMap<String,String> save = new HashMap<String,String>();
        save.put("values", joined);
        return save;
    }

    @Override
    protected boolean init(final CommandSender sender, String[] args) {
        try {
            numberOfSets = (args.length>0)?Integer.decode(args[0]):1;
            
            if (numberOfSets<0) {
                error(sender, "Bad sign argument: " + args[0]);
                return false;
            }
        } catch (NumberFormatException ne) {
            error(sender, "Bad sign argument: " + args[0]);
            return false;
        }

        if(interfaceBlocks.length==0) {
            error(sender,"Expecting at least 1 interface block.");
            return false;
        }

        if(interfaceBlocks.length%numberOfSets!=0) {
            error(sender,"Number of interface blocks must be dividable by " + numberOfSets + ".");
            return false;
        }

        if(outputs.length==0) {
            error(sender,"Expecting at least 1 output pin.");
            return false;
        }

        if(outputs.length%numberOfSets!=0) {
            error(sender,"Number of outputs must be dividable by " + numberOfSets + ". Found " + outputs.length + " outputs.");
            return false;
        }

        wordlength = interfaceBlocks.length/numberOfSets;
        results = new int[numberOfSets];
        Arrays.fill(results, 0);

        SensorLibrary.registerSlotdigitsCircuit(this);

        return true;
    }

    @Override
    public void circuitDestroyed() {
        SensorLibrary.deregisterSlotdigitsCircuit(this);
    }

    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction()==Action.LEFT_CLICK_BLOCK) {
            for(int interfaceBlockIndex=0; interfaceBlockIndex<interfaceBlocks.length;interfaceBlockIndex++) {
                if (interfaceBlocks[interfaceBlockIndex].equals(event.getClickedBlock().getLocation())) {
                    int result = replaceDigitOfInterfaceBlock(interfaceBlockIndex,event.getPlayer().getInventory().getHeldItemSlot()+1);
                    info(event.getPlayer(), this.getCircuitClass() + ": " + result);

                    if (interfaceSetIndex(interfaceBlockIndex)>=inputs.length || inputBits.get(interfaceSetIndex(interfaceBlockIndex))) {
                        sendInt(outputStartIndex(interfaceBlockIndex),outputs.length/numberOfSets,result);
                    }
                }
            }
        } else if (event.getAction()==Action.RIGHT_CLICK_BLOCK) {
            for(int interfaceBlockIndex=0; interfaceBlockIndex<interfaceBlocks.length;interfaceBlockIndex++) {
                if (interfaceBlocks[interfaceBlockIndex].equals(event.getClickedBlock().getLocation())) {
                    int result = replaceDigitOfInterfaceBlock(interfaceBlockIndex,0);
                    info(event.getPlayer(), this.getCircuitClass() + ": " + result);
                    
                    if (interfaceSetIndex(interfaceBlockIndex)>=inputs.length || inputBits.get(interfaceSetIndex(interfaceBlockIndex))) {
                        sendInt(outputStartIndex(interfaceBlockIndex),outputs.length/numberOfSets,result);
                    }

                    event.setCancelled(true);
                }
            }
        }
    }

    private int outputStartIndex(int indexOfSet) {
        return interfaceSetIndex(indexOfSet)*outputs.length/numberOfSets;
    }

    private int replaceDigitOfInterfaceBlock(int interfaceBlockIndex, int replacement) {
        int newResult=replaceDigit(results[interfaceSetIndex(interfaceBlockIndex)], indexInInterfaceSet(interfaceBlockIndex),replacement);
        results[interfaceSetIndex(interfaceBlockIndex)] = newResult;
        return Math.round(newResult);
    }

    private int interfaceSetIndex(int interfaceBlockIndex) {
        return (int) Math.floor(interfaceBlockIndex/wordlength);
    }

    private int indexInInterfaceSet(int interfaceBlockIndex) {
        return interfaceBlockIndex%wordlength;
    }

    private int replaceDigit(int number,int index, int replacement) {
        float remainder = (float)(number/Math.pow(10, index))%1;
        number /= Math.pow(10, index+1);
        number *=10;
        number += replacement;
        number *= Math.pow(10, index);
        number += remainder*Math.pow(10, index);
        return Math.round(number);
    }

    @Override
    public void inputChange(int index,boolean state) {
        if(state && index < results.length) {
            sendInt(index*outputs.length/numberOfSets,outputs.length/numberOfSets,results[index]);
        }
    }

    @Override
    protected boolean isStateless() {
        return false;
    }
}
