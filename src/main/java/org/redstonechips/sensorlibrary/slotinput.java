package org.redstonechips.sensorlibrary;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.redstonechips.parsing.Parsing;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.redstonechips.RCPrefs;
import org.redstonechips.chip.Chip;
import org.redstonechips.circuit.Circuit;
import org.redstonechips.event.EventListener;

public class slotinput extends Circuit {

    PlayerInteractListener eventListener;
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
    public Circuit init(String[] args) {
        try {
            numberOfSets = (args.length>0)?Integer.decode(args[0]):1;            
            if (numberOfSets<0) return error("Bad sign argument: " + args[0]);
        } catch (NumberFormatException ne) {
            return error("Bad sign argument: " + args[0]);
        }

        if (chip.interfaceBlocks.length==0) 
            return error("Expecting at least 1 interface block.");
        if (chip.interfaceBlocks.length%numberOfSets!=0) 
            return error("Number of interface blocks must be dividable by " + numberOfSets + ".");
        if(outputlen==0) 
            return error("Expecting at least 1 output pin.");
        if(outputlen%numberOfSets!=0)
            return error("Number of outputs must be dividable by " + numberOfSets + ". Found " + outputlen + " outputs.");

        wordlength = chip.interfaceBlocks.length/numberOfSets;
        results = new int[numberOfSets];
        Arrays.fill(results, 0);

        eventListener = new PlayerInteractListener();
        SensorLibrary.eventDispatcher.registerListener(org.bukkit.event.player.PlayerInteractEvent.class, eventListener);

        if (activator!=null) clearOutputs();
        return this;
    }

    @Override
    public void shutdown() {
        SensorLibrary.eventDispatcher.unregisterListener(eventListener);
    }

    class PlayerInteractListener implements EventListener {

        @Override
        public void onEvent(Event e) {
            PlayerInteractEvent event = (PlayerInteractEvent)e;
            if (event.getAction()!=Action.LEFT_CLICK_BLOCK && event.getAction()!=Action.RIGHT_CLICK_BLOCK) return;

            Chip c = rc.chipManager().getAllChips().getByStructureBlock(event.getClickedBlock().getLocation());
            if (c!=chip) return;
            Location loc = event.getClickedBlock().getLocation();

            for (int i=0; i<chip.interfaceBlocks.length; i++) {
                if (chip.interfaceBlocks[i].getLocation().equals(loc)) {
                    int newDigit;
                    if (event.getAction()==Action.RIGHT_CLICK_BLOCK)
                        newDigit = event.getPlayer().getInventory().getHeldItemSlot()+1;
                    else 
                        newDigit = 0;

                    int result = replaceDigitOfInterfaceBlock(i, newDigit);
                    String sres = ChatColor.LIGHT_PURPLE.toString() + result + RCPrefs.getInfoColor() + ".";

                    if (numberOfSets==1) 
                        infoForSender(event.getPlayer(), chip + ": Setting value to " + sres);
                    else
                        infoForSender(event.getPlayer(), chip + ": Setting " + 
                                Parsing.indexToOrdinal(interfaceSetIndex(i)) + " set to " + sres);

                    if (interfaceSetIndex(i)>=inputlen || inputs[interfaceSetIndex(i)]) {
                        writeInt(result, outputStartIndex(i), outputlen/numberOfSets);
                    }

                    event.setCancelled(true);

                    break;
                }
            }
        }
    }

    private int outputStartIndex(int indexOfSet) {
        return interfaceSetIndex(indexOfSet)*outputlen/numberOfSets;
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
    public void input(boolean state, int inIdx) {
        if(state && inIdx < results.length) {
            writeInt(results[inIdx], inIdx*outputlen/numberOfSets, outputlen/numberOfSets);
        }
    }

    @Override
    public boolean isStateless() {
        return false;
    }
}
