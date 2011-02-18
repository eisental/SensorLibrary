package org.tal.sensorlibrary;

import java.util.Calendar;
import org.bukkit.entity.Player;
import org.tal.redstonechips.circuit.Circuit;

/**
 *
 * @author Tal Eisenberg
 */
public class daytime extends Circuit {
    boolean earthtime = false;
    int maxval;
    int maxtime;

    @Override
    public void inputChange(int inIdx, boolean state) {
        if (state) {
            int time;
            if (earthtime) {
                Calendar now = Calendar.getInstance();
                time = now.get(Calendar.SECOND) + now.get(Calendar.MINUTE)*60 + now.get(Calendar.HOUR_OF_DAY) * 60 * 24;
            } else time = (int)world.getTime();

            if (hasDebuggers()) debug("Time is " + time);

            int output;
            if (time>maxval)
                output = Math.round(((float)time/(float)maxtime)*maxval);
            else
                output = time;

            sendInt(0, outputs.length, output);
        }
    }

    @Override
    protected boolean init(Player player, String[] args) {
        if (inputs.length!=1) {
            error(player, "Expecting 1 clock input.");
            return false;
        }

        earthtime = (args.length!=0 && args[0].equalsIgnoreCase("earthtime"));
        maxval = (int)(Math.pow(2, outputs.length)-1);
        if (earthtime) maxtime = 86400; // number of seconds per real day.
        else maxtime = 24000; // number of game ticks per game day.

        return true;
    }

}
