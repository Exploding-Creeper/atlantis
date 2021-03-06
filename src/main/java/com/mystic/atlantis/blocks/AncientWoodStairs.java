package com.mystic.atlantis.blocks;


import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.tool.attribute.v1.FabricToolTags;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.StairsBlock;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class AncientWoodStairs extends StairsBlock {

    public AncientWoodStairs(BlockState baseBlockState, FabricBlockSettings settings) {
        super(baseBlockState, settings
                .sounds(BlockSoundGroup.WOOD)
                .breakByTool(FabricToolTags.AXES, 2)
                .requiresTool()
                .strength(3.0F, 6.0F));
    }
}