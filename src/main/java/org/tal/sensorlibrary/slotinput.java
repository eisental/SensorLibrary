package org.tal.sensorlibrary;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.eisental.common.parsing.ParsingUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.tal.redstonechips.circuit.Circuit;

public class slotinput extends Circuit {

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

        SensorLibrary.registerSlotinputCircuit(this);

        if (sender!=null) resetOutputs();
        return true;
    }

    @Override
    public void circuitShutdown() {
        SensorLibrary.deregisterSlotinputCircuit(this);
    }

    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction()!=Action.LEFT_CLICK_BLOCK && event.getAction()!=Action.RIGHT_CLICK_BLOCK) return;

        Circuit c = redstoneChips.getCircuitManager().getCircuitByStructureBlock(event.getClickedBlock().getLocation());
        if (c==this) {
            Location loc = event.getClickedBlock().getLocation();

            for (int i=0; i<interfaceBlocks.length; i++) {
                if (interfaceBlocks[i].getLocation().equals(loc)) {
                    int newDigit;
                    if (event.getAction()==Action.RIGHT_CLICK_BLOCK)
                        newDigit = event.getPlayer().getInventory().getHeldItemSlot()+1;
                    else 
                        newDigit = 0;

                    int result = replaceDigitOfInterfaceBlock(i, newDigit);
                    String sres = ChatColor.LIGHT_PURPLE.toString() + 
                                result + redstoneChips.getPrefs().getInfoColor() + ".";
                    if (numberOfSets==1) 
                        info(event.getPlayer(), c.getChipString() + ": Setting value to " + sres);
                    else
                        info(event.getPlayer(), c.getChipString() + ": Setting " + 
                                ParsingUtils.indexToOrdinal(interfaceSetIndex(i)) + " set to " + sres);

                    if (interfaceSetIndex(i)>=inputs.length || inputBits.get(interfaceSetIndex(i))) {
                        sendInt(outputStartIndex(i), outputs.length/numberOfSets, result);
                    }

                    event.setCancelled(true);

                    break;
                }
            }
        }
    }

    private int outputStartIndex(int indexOfSet) {
        return interfaceSetIndex(indexOfSet)*outputs.length/numberOfSets;
    }

    private int replaceDigitOfInterfaceBlock(int idx, int replacement) {
        int newResult = replaceDigit(results[interfaceSetIndex(idx)], indexInInterfaceSet(idx),replacement);
        results[interfaceSetIndex(idx)] = newResult;
        return newResult;
    }

    private int interfaceSetIndex(int interfaceBlockIndex) {
        return (int) Math.floor(interfaceBlockIndex/wordlength);
    }

    private int indexInInterfaceSet(int interfaceBlockIndex) {
        return interfaceBlockIndex%wordlength;
    }

    private int replaceDigit(int number,int index, int replacement) {
        if (Integer.toString(number).length()<=index) {
            number = ((int)Math.pow(10, index))*replacement + number;
            return number;
        } else {
            char[] num = Integer.toString(number).toCharArray();
            num[num.length-1-index] = Integer.toString(replacement).charAt(0);
            return Integer.valueOf(new String(num));
        }
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
