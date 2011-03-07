package org.tal.sensorlibrary;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.tal.redstonechips.circuit.Circuit;

/**
 *
 * @author Tal Eisenberg
 */
public class pirsensor extends Circuit {
    private Vector center;
    private int radius = 10;

    @Override
    public void inputChange(int inIdx, boolean state) {
        if (state) {
            // clock pin triggered
            boolean alarm = false;

            for (LivingEntity e : world.getLivingEntities()) {
                Location l = e.getLocation();
                Vector v = new Vector(l.getX(), l.getY(), l.getZ());

                if (v.isInSphere(center, radius)) {
                    // pir triggered
                    alarm = true;
                    if (hasDebuggers()) debug("PIR sensor triggered.");
                    break;
                }
            }

            sendOutput(0, alarm);
        }
    }

    @Override
    protected boolean init(CommandSender sender, String[] args) {
        if (interfaceBlocks.length!=1) {
            error(sender, "Expecting 1 interface block.");
            return false;
        }

        if (inputs.length!=1) {
            error(sender, "Expecting 1 clock input pin.");
            return false;
        }

        if (outputs.length!=1) {
            error(sender, "Expecting 1 alarm output.");
            return false;
        }

        if (args.length>0) {
            try {
                radius = Integer.decode(args[0]);
            } catch (NumberFormatException ne) {
                error(sender, "Bad sensitivity sign argument: " + args[0]);
                return false;
            }
        }

        Location i = interfaceBlocks[0];
        center = i.toVector();
        return true;
    }

}
