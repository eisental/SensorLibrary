package org.redstonechips.sensorlibrary;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.redstonechips.circuit.Circuit;
import org.redstonechips.util.Locations;

/**
 *
 * @author: Tal Eisenberg, Vecht
 */
public class pirsensor extends Circuit {

    private Location center;
    private int radius = 10;
    private boolean checkCube = false;
    private Class[] checkedEntities = new Class[0];
    private static final String classPath = "org.bukkit.entity.";

    @Override
    public void input(boolean state, int inIdx) {
        if (state) {
            // clock pin triggered
            boolean alarm = false;            

            for (Object o : chip.world.getEntitiesByClass(checkedEntities)) {
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

                if (chip.hasListeners() && alarm) {
                    debug("PIR sensor triggered by " + e.getClass().getSimpleName());
                }
            }
            write(alarm, 0);
        }
    }

    @Override
    public Circuit init(String[] args) {
        if (chip.interfaceBlocks.length != 1) return error("Expecting 1 interface block.");
        if (inputlen != 1) return error("Expecting 1 clock input pin.");
        if (outputlen != 1) return error("Expecting 1 alarm output.");

        List<Class> entityClasses = new ArrayList<Class>();
        
        if (args.length > 0) {
            for (String arg : args)
                if (!processArgs(arg, entityClasses)) return error("Bad sign argument: " + arg);            
        }

        if (entityClasses.isEmpty()) {
            entityClasses.add(LivingEntity.class);
        }
        
        checkedEntities = entityClasses.toArray(checkedEntities);
        center = chip.interfaceBlocks[0].getLocation();

        return this;
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
        } catch (NumberFormatException e) {}

        //Case: argument is an entity type
        try {
            entityClasses.add((Class<? extends Entity>)Class.forName(classPath + arg, false, this.getClass().getClassLoader()));
            return true;
        } catch (ClassNotFoundException e) {}

        //No cases match, print bad argument error
        return false;
    }
}