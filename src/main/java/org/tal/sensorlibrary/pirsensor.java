package org.tal.sensorlibrary;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.tal.redstonechips.circuit.Circuit;
import org.tal.redstonechips.util.Locations;

/**
 *
 * @author: Tal Eisenberg, Vecht
 */
public class pirsensor extends Circuit {

    private Location center;
    private int radius = 10;
    private boolean checkCube = false;
    private Class[] checkedEntities = new Class[0];
    private static String classPath = "org.bukkit.entity.";

    @Override
    public void inputChange(int inIdx, boolean state) {
        if (state) {
            // clock pin triggered
            boolean alarm = false;            

            for (Object o : world.getEntitiesByClass(checkedEntities)) {
                Entity e = (Entity)o;

                Location l = e.getLocation();

                if (checkCube) {

                    double absX = Math.abs(l.getX() - center.getX());
                    double absY = Math.abs(l.getY() - center.getY());
                    double absZ = Math.abs(l.getZ() - center.getZ());

                    if (absX <= radius && absY <= radius && absZ <= radius) {
                        alarm = true;
                    }
                } else if (Locations.isInRadius(center, l, radius)) {
                    alarm = true;
                }

                if (hasListeners() && alarm) {
                    debug("PIR sensor triggered by " + e.getClass().getSimpleName());
                }
            }
            sendOutput(0, alarm);
        }
    }

    @Override
    protected boolean init(CommandSender sender, String[] args) {
        if (interfaceBlocks.length != 1) {
            error(sender, "Expecting 1 interface block.");
            return false;
        }

        if (inputs.length != 1) {
            error(sender, "Expecting 1 clock input pin.");
            return false;
        }

        if (outputs.length != 1) {
            error(sender, "Expecting 1 alarm output.");
            return false;
        }

        List<Class> entityClasses = new ArrayList<Class>();
        
        if (args.length > 0) {
            for (String arg : args) {
                if (!processArgs(arg, entityClasses)) {
                    error(sender, "Bad sign argument: " + arg);
                    return false;
                }
            }
        }

        if (entityClasses.isEmpty()) {
            entityClasses.add(LivingEntity.class);
        }
        
        checkedEntities = entityClasses.toArray(checkedEntities);
        center = interfaceBlocks[0].getLocation();

        return true;
    }

    private boolean processArgs(String arg, List<Class> entityClasses) {

        //Case: argument is "cube"
        if (arg.equals("cube")) {
            checkCube = true;
            return true;
        }

        //Case: argument is the detection radius
        try {
            radius = Integer.decode(arg);
            return true;
        } catch (NumberFormatException e) {;
        }

        //Case: argument is an entity type
        try {
            entityClasses.add((Class<? extends Entity>)Class.forName(classPath + arg, false, this.getClass().getClassLoader()));
            return true;
        } catch (ClassNotFoundException e) {;
        }

        //No cases match, print bad argument error
        return false;
    }
}