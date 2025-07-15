package org.jcs.egm.registry;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import org.jcs.egm.registry.ModItems;

public class UruTier {
    public static final Tier INSTANCE = new Tier() {
        @Override
        public int getUses() { return 0; } // Unbreakable
        @Override
        public float getSpeed() { return 8.0f; } // Slightly slower than Netherite
        @Override
        public float getAttackDamageBonus() { return 10.0f; }
        @Override
        public int getLevel() { return 4; } // Netherite level
        @Override
        public int getEnchantmentValue() { return 30; }
        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(ModItems.INGOT_URU.get());
        }
    };
}
