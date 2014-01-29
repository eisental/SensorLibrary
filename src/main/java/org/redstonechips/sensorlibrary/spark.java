
package org.redstonechips.sensorlibrary;

import org.redstonechips.circuit.Circuit;
import org.redstonechips.util.BooleanArrays;

/**
 *
 * @author Tal Eisenberg
 */
public class spark extends Circuit {
    boolean bin = false, fake = false;
    int dataPin;

    @Override
    public void input(boolean state, int inIdx) {
        if (bin) {
            if (inIdx==0 && state) {
                int idx = (int)BooleanArrays.toUnsignedInt(inputs, 1, inputlen-1);
                if (idx<chip.interfaceBlocks.length)
                    strike(idx);
            }
        } else if (state) strike(inIdx);
    }

    private void strike(int idx) {
            if (fake) chip.world.strikeLightningEffect(chip.interfaceBlocks[idx].getLocation());
            else chip.world.strikeLightning(chip.interfaceBlocks[idx].getLocation());        
    }
    
    @Override
    public Circuit init(String[] args) {
        bin = false; fake = false;
                
        if (args.length>0) {
            if (args[args.length-1].equalsIgnoreCase("fake"))
                fake = true;
            
            if (args[0].equalsIgnoreCase("bin")) {
                bin = true;
            } 
        }
        
        if (chip.interfaceBlocks.length==0) return error("Expecting at least 1 interface block.");

        if (bin) {
            int expectedInputs = (int)Math.ceil(Math.log(chip.interfaceBlocks.length)/Math.log(2))+1;
            
            if (inputlen<expectedInputs) return error("Expecting at least " + expectedInputs + " inputs for " + chip.interfaceBlocks.length + " interface block(s)");
        } else {
            if (chip.interfaceBlocks.length!=inputlen) return error("Expecting the same amount of interface blocks and input pins.");
        }

        return this;
    }

    @Override
    public boolean isStateless() {
        return false;
    }
}
