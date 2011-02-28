package org.tal.sensorlibrary;

import java.util.Calendar;
import org.bukkit.command.CommandSender;
import org.tal.redstonechips.circuit.Circuit;

/**
 *
 * @author Tal Eisenberg
 */
public class daytime extends Circuit {
    enum TimeField { HOUR, MINUTE, SECOND, SECONDOFDAY, TICKOFDAY, TICK }


    private boolean earthtime = false;
    private int maxval;
    private int maxtime;
    private TimeField timeField;

    @Override
    public void inputChange(int inIdx, boolean state) {
        if (state) {
            int time;
            if (earthtime) {
                Calendar now = Calendar.getInstance();
                if (timeField==TimeField.SECOND)
                    time = now.get(Calendar.SECOND);
                else if(timeField == TimeField.SECONDOFDAY)
                    time = now.get(Calendar.SECOND) + now.get(Calendar.MINUTE)*60 + now.get(Calendar.HOUR_OF_DAY) * 60;
                else if (timeField==TimeField.MINUTE)
                    time = now.get(Calendar.MINUTE);
                else if (timeField==TimeField.HOUR)
                    time = now.get(Calendar.HOUR_OF_DAY);
                else time = -1;
            } else {
                if (timeField==TimeField.TICKOFDAY)
                    time = (int)world.getTime();
                else if(timeField == TimeField.TICK)
                    time = (int)(world.getTime()%1000);
                else if (timeField==TimeField.HOUR)
                    time = (int)(world.getTime()/1000d);
                else time = -1;
            }

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
    protected boolean init(CommandSender sender, String[] args) {
        if (inputs.length!=1) {
            error(sender, "Expecting 1 clock input.");
            return false;
        }

        if (args.length>0) {
            if (args[0].equalsIgnoreCase("earthtime")) earthtime = true;
            else if (args[0].equalsIgnoreCase("gametime")) earthtime = false;
        }

        if (args.length>1) {
            try {
                timeField = TimeField.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException ie) {
                error(sender, "Unknown time field: " + args[1]);
                return false;
            }
        } else {
            if (earthtime) timeField=TimeField.SECOND;
            else timeField=TimeField.TICK;
        }

        maxval = (int)(Math.pow(2, outputs.length)-1);
        if (earthtime) {
            if (timeField==TimeField.TICK || timeField==TimeField.TICKOFDAY) {
                error(sender, "Invalid time field when using earth time: " + timeField.name());
                return false;
            } else if (timeField == TimeField.HOUR)
                maxtime = 23; // number of hours per real day.
            else if (timeField==TimeField.MINUTE)
                maxtime = 59; // number of minutes per hour.
            else if (timeField==TimeField.SECONDOFDAY)
                maxtime = 86399; // number of seconds per real day.
            else if (timeField==TimeField.SECOND)
                maxtime = 59; // number of seconds per minute.

        }
        else {
            if (timeField==TimeField.MINUTE || timeField==TimeField.SECOND || timeField==TimeField.SECONDOFDAY) {
                error(sender, "Invalid time field when using game time: " + timeField.name());
                return false;
            } else if (timeField==TimeField.HOUR)
                maxtime = 23;
            else if (timeField==TimeField.TICKOFDAY)
                maxtime = 23999; // number of game ticks per game day.
            else if (timeField==TimeField.TICK)
                maxtime = 999; // number of game ticks per game hour.
        }

        return true;
    }

}
