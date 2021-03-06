package com.mystic.atlantis.items.tools;

import com.mystic.atlantis.init.ItemInit;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.ToolMaterial;

public class AquamarineShovel extends ShovelItem {
    public AquamarineShovel(ToolMaterial tier, int attack) {
        super(tier, attack, -3.2F, new Settings()
                .maxCount(1)
                .maxDamageIfAbsent(tier.getDurability()));
    }
}
