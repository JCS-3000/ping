package org.jcs.egm.stones.stone_mind;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;

import java.lang.reflect.Field;

public class MobMoveControlUtil {
    private static Field moveControlField;

    static {
        // Print all declared fields of Mob (for mapping debugging)
        System.out.println("[EGM DEBUG] Listing all Mob fields:");
        Field[] fields = Mob.class.getDeclaredFields();
        for (Field f : fields) {
            System.out.println("[EGM DEBUG] Mob field: " + f.getName() + " type=" + f.getType().getName());
        }

        // Try to find the MoveControl field (mapping may change its name)
        try {
            // 1. Try vanilla name first
            try {
                moveControlField = Mob.class.getDeclaredField("moveControl");
                moveControlField.setAccessible(true);
                System.out.println("[EGM DEBUG] Found moveControl field by name: " + moveControlField.getName());
            } catch (NoSuchFieldException ex) {
                // 2. Fallback: find first MoveControl field by type
                for (Field f : fields) {
                    if (MoveControl.class.isAssignableFrom(f.getType())) {
                        moveControlField = f;
                        moveControlField.setAccessible(true);
                        System.out.println("[EGM DEBUG] Found moveControl field by TYPE: " + moveControlField.getName());
                        break;
                    }
                }
                if (moveControlField == null) {
                    throw new RuntimeException("Unable to locate MoveControl field on Mob!");
                }
            }
        } catch (Throwable t) {
            System.out.println("[EGM ERROR] Could not find moveControl field: " + t);
            t.printStackTrace();
        }
    }

    public static void setMoveControl(Mob mob, MoveControl control) {
        try {
            moveControlField.set(mob, control);
            System.out.println("[EGM DEBUG] Successfully set MoveControl to: " + control.getClass().getName());
        } catch (Throwable e) {
            System.out.println("[EGM ERROR] Failed to set MoveControl! " + e);
            e.printStackTrace();
        }
    }
}
