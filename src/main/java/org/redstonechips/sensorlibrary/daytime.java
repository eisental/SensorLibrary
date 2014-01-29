package org.redstonechips.sensorlibrary;

import java.util.Calendar;
import org.bukkit.World;
import org.redstonechips.circuit.Circuit;

/**
 *
 * @author Tal Eisenberg
 */
public class daytime extends Circuit {
    enum TimeField {
        SECOND(23999,59), SECONDOFDAY(23999, 86399), MINUTE(59,59), MINUTEOFDAY(1439,1439), HOUR(23, 23), TICK(23999, 86399),
        MINUTE1(9,9), MINUTE10(5,5), HOUR1(9, 9), HOUR10(2, 2), SECOND1(9,9), SECOND10(5,5);

        public int gameMax, earthMax;

        TimeField(int gameMax, int earthMax) {
            this.gameMax = gameMax;
            this.earthMax = earthMax;
        }

        int maxTime(boolean earthtime) {
            if (earthtime) return earthMax;
            else return gameMax;
        }
    }

    private static final double ticksPerHour = 1000d;
    private static final double ticksPerMinute = ticksPerHour/60d; //16.6666666...

    private boolean earthtime = false;
    private int maxval;

    private int hoursOffset = 0;
    
    private TimeField timeField;
    private World w;

    @Override
    public void input(boolean state, int inIdx) {
        if (state) {
            int time;

            if (earthtime) {
                Calendar now = Calendar.getInstance();
                if (timeField==TimeField.SECOND)
                    time = now.get(Calendar.SECOND);
                else if(timeField == TimeField.SECONDOFDAY || timeField==TimeField.TICK)
                    time = now.get(Calendar.SECOND) + now.get(Calendar.MINUTE)*60 + now.get(Calendar.HOUR_OF_DAY) * 60;
                else if (timeField==TimeField.MINUTE)
                    time = now.get(Calendar.MINUTE);
                else if (timeField==TimeField.MINUTEOFDAY)
                    time = now.get(Calendar.MINUTE) + now.get(Calendar.HOUR_OF_DAY)*60;
                else if (timeField==TimeField.HOUR) {
                    time = now.get(Calendar.HOUR_OF_DAY) + hoursOffset;
                    if (time>=24) time = time - 24;
                } else if (timeField==TimeField.HOUR1) { // hour of day, ones digit
                    time = (now.get(Calendar.HOUR_OF_DAY) + hoursOffset);
                    if (time>=24) time = time - 24;
                    time = time % 10;
                } else if (timeField==TimeField.HOUR10) { // hour of day, tens digit
                    time = (now.get(Calendar.HOUR_OF_DAY) + hoursOffset);
                    if (time>=24) time = time - 24;
                    time = time / 10;
                } else if (timeField==TimeField.MINUTE1) { // minute of hour, ones digit
                    time = now.get(Calendar.MINUTE) % 10;
                } else if (timeField==TimeField.MINUTE10) { // minute of hour, tens digit
                    time = now.get(Calendar.MINUTE) / 10;
                } else if (timeField==TimeField.SECOND1) { // second of minute, ones digit
                    time = now.get(Calendar.SECOND) % 10;
                } else if (timeField==TimeField.SECOND10) {
                    time = now.get(Calendar.SECOND) / 10;
                } else time = -1;
            } else {
                if (timeField==TimeField.SECONDOFDAY || timeField==TimeField.TICK || timeField==TimeField.SECOND)
                    time = (int)w.getTime();
                else if (timeField == TimeField.MINUTEOFDAY)
                    time = (int)Math.round(w.getTime()/ticksPerMinute);
                else if (timeField == TimeField.MINUTE)
                    time = (int)Math.round((w.getTime()%1000)/ticksPerMinute);
                else if (timeField == TimeField.HOUR) {
                    time = (int)(w.getTime()/ticksPerHour) + hoursOffset;
                    if (time>=24) time = time - 24;
                    
                } else if (timeField==TimeField.HOUR1) { // hour of day, ones digit
                    time = ((int)(w.getTime()/ticksPerHour) + hoursOffset);
                    if (time>=24) time = time - 24;
                    time = time % 10;
                    
                } else if (timeField==TimeField.HOUR10) { // hour of day, tens digit
                    time = ((int)(w.getTime()/ticksPerHour) + hoursOffset);
                    if (time>=24) time = time - 24;
                    time = time / 10;
                    
                } else if (timeField==TimeField.MINUTE1) { // minute of hour, ones digit
                    time = ((int)Math.round((w.getTime()%1000)/ticksPerMinute)) % 10;
                } else if (timeField==TimeField.MINUTE10) { // minute of hour, tens digit
                    time = ((int)Math.round((w.getTime()%1000)/ticksPerMinute)) / 10;
                } else time = -1;
            }

            time = Math.min(time, timeField.maxTime(earthtime));
            
            if (chip.hasListeners()) debug("Time is " + time);

            int output;
            int maxTime = timeField.maxTime(earthtime);

            if (maxTime>maxval) {
                float c = (float)(maxval+1)/(float)(maxTime+1);

                output = (int)(c*time);
            } else
                output = time;

            writeInt(output, 0, outputlen);
        }
    }

    @Override
    public Circuit init(String[] args) {
        if (inputlen!=1) return error("Expecting 1 clock input.");

        if (args.length>0) {
            String errorMsg = "Expecting <earthtime|gametime>[:<hour-offset>]";
            int colonIdx = args[0].indexOf(":");
            String timetype;
            if (colonIdx==-1) {
                timetype = args[0];
                hoursOffset = 0;
            } else {
                timetype = args[0].substring(0, colonIdx);
                try {
                    hoursOffset = Integer.parseInt(args[0].substring(colonIdx+1));
                } catch (NumberFormatException e) {
                    return error(errorMsg);
                }
            }
            
            if (timetype.equalsIgnoreCase("earthtime")) earthtime = true;
            else if (timetype.equalsIgnoreCase("gametime")) earthtime = false;
            else return error(errorMsg);            
        }

        if (args.length>1) {
            try {
                timeField = TimeField.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException ie) {
                return error("Unknown time field: " + args[1]);
            }
        } else {
            timeField=TimeField.TICK;
        }

        if (!earthtime && (timeField==TimeField.SECOND1 || timeField==TimeField.SECOND10))
            return error("second1 or second10 time fields are not allowed when using gametime.");

        if (args.length>2) {
            w = rc.getServer().getWorld(args[2]);
            if (w == null) return error("Unknown world name: " + args[2]);
        } else {
            w = chip.world;
        }

        maxval = (int)(Math.pow(2, outputlen)-1);        

        return this;
    }
}