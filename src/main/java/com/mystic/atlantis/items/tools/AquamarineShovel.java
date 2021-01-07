package com.mystic.atlantis.items.tools;

import com.mystic.atlantis.init.ItemInit;
import net.minecraft.item.IItemTier;
import net.minecraft.item.ShovelItem;

public class AquamarineShovel extends ShovelItem {
    public AquamarineShovel(IItemTier tier, int attack) {
        super(tier, attack, -3.2F, new Properties()
                .maxStackSize(1)
                .defaultMaxDamage(tier.getMaxUses())
                .group(ItemInit.CREATIVE_TAB_ATLANTIS));
    }
}