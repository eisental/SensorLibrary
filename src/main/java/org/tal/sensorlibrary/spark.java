
package org.tal.sensorlibrary;

import org.bukkit.command.CommandSender;
import org.tal.redstonechips.circuit.Circuit;
import org.tal.redstonechips.util.BitSetUtils;

/**
 *
 * @author Tal Eisenberg
 */
public class spark extends Circuit {
    boolean bin = false;
    int dataPin;

    @Override
    public void inputChange(int inIdx, boolean state) {
        if (bin) {
            if (inIdx==0 && state) {
                int idx = BitSetUtils.bitSetToUnsignedInt(inputBits, 1, inputs.length-1);
                if (idx<interfaceBlocks.length)
                    world.strikeLightning(interfaceBlocks[idx]);
            }
        } else if (state) world.strikeLightning(interfaceBlocks[inIdx]);
    }

    @Override
    protected boolean init(CommandSender sender, String[] args) {
        bin = false;
        if (args.length>0) {
            if (args[0].equalsIgnoreCase("bin")) {
                bin = true;
            } else {
                error(sender, "Bad argument: " + args[0]);
                return false;
            }
        }
        if (interfaceBlocks.length==0) {
            error(sender, "Expecting at least 1 interface block.");
            return false;
        }

        if (bin) {
            if (Math.pow(2,inputs.length-1)<interfaceBlocks.length) {
                int expectedInputs = (int)Math.ceil(Math.log(interfaceBlocks.length)/Math.log(2))+1;
                error(sender, "Expecting at least " + expectedInputs + " inputs for " + interfaceBlocks.length + " interface block(s)");
                return false;
            }
        } else {
            if (interfaceBlocks.length!=inputs.length) {
                error(sender, "Expecting the same amount of interface blocks and input pins.");
                return false;
            }
        }

        return true;
    }

    @Override
    protected boolean isStateless() {
        return false;
    }

}
