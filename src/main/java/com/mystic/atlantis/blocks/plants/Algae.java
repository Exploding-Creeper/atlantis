package com.mystic.atlantis.blocks.plants;

import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ConnectingBlock;
import net.minecraft.block.Material;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.tag.Tag;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

import org.jetbrains.annotations.Nullable;

public class Algae extends Block implements Waterloggable {
    public static final BooleanProperty UP = ConnectingBlock.UP;
    public static final BooleanProperty NORTH = ConnectingBlock.NORTH;
    public static final BooleanProperty EAST = ConnectingBlock.EAST;
    public static final BooleanProperty SOUTH = ConnectingBlock.SOUTH;
    public static final BooleanProperty WEST = ConnectingBlock.WEST;
    public static final Map<Direction, BooleanProperty> FACING_TO_PROPERTY_MAP = ConnectingBlock.FACING_PROPERTIES.entrySet().stream().filter((facingProperty) -> {
        return facingProperty.getKey() != Direction.DOWN;
    }).collect(Util.toMap());
    private static final VoxelShape UP_AABB = Block.createCuboidShape(0.0D, 15.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape EAST_AABB = Block.createCuboidShape(0.0D, 0.0D, 0.0D, 1.0D, 16.0D, 16.0D);
    private static final VoxelShape WEST_AABB = Block.createCuboidShape(15.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape SOUTH_AABB = Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 1.0D);
    private static final VoxelShape NORTH_AABB = Block.createCuboidShape(0.0D, 0.0D, 15.0D, 16.0D, 16.0D, 16.0D);

    private final Map<BlockState, VoxelShape> stateToShapeMap;

    private static final Property<Boolean> WATERLOGGED = Properties.WATERLOGGED;

    public Algae(Settings properties) {
        super(properties
                .ticksRandomly()
                .strength(0.2F, 0.4F)
                .sounds(BlockSoundGroup.GRASS)
                .requiresTool()
                .noCollision()
                .nonOpaque());
        this.setDefaultState(this.stateManager.getDefaultState().with(UP, Boolean.FALSE).with(NORTH, Boolean.FALSE).with(EAST, Boolean.FALSE).with(SOUTH, Boolean.FALSE).with(WEST, Boolean.FALSE).with(WATERLOGGED, Boolean.TRUE));
        this.stateToShapeMap = ImmutableMap.copyOf(this.stateManager.getStates().stream().collect(Collectors.toMap(Function.identity(), Algae::getShapeForState)));
    }

    private static VoxelShape getShapeForState(BlockState state) {
        VoxelShape voxelshape = VoxelShapes.empty();
        if (state.get(UP)) {
            voxelshape = UP_AABB;
        }

        if (state.get(NORTH)) {
            voxelshape = VoxelShapes.union(voxelshape, SOUTH_AABB);
        }

        if (state.get(SOUTH)) {
            voxelshape = VoxelShapes.union(voxelshape, NORTH_AABB);
        }

        if (state.get(EAST)) {
            voxelshape = VoxelShapes.union(voxelshape, WEST_AABB);
        }

        if (state.get(WEST)) {
            voxelshape = VoxelShapes.union(voxelshape, EAST_AABB);
        }

        return voxelshape;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
        return this.stateToShapeMap.get(state);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView worldIn, BlockPos pos) {
        if(OnlyWater(worldIn, pos, state)) {
            return this.getBlocksAttachedTo(this.func_196545_h(state, worldIn, pos));
        }else{
            return false;
        }
    }

    public boolean OnlyWater(WorldView worldReader, BlockPos pos, BlockState state) {
        return !worldReader.getBlockState(pos).isIn(getAir()) || !this.canBlockStay(worldReader, pos, state);
    }

    public Tag<Block> getAir(){
        Tag<Block> air = new Tag<Block>() {
            @Override
            public boolean contains(Block element) {
                return true;
            }

            @Override
            public List<Block> values() {
                List<Block> air2 = new ArrayList<Block>();
                air2.add(Blocks.AIR);
                return air2;
            }
        };
        return air;
    }

    public boolean canBlockStay(WorldView worldReader, BlockPos pos, BlockState state) {
        return canPlaceBlockAt(worldReader, pos);
    }

    public boolean canPlaceBlockAt(WorldView worldReader, BlockPos pos) {
        return worldReader.getBlockState(pos.up()).getMaterial() != Material.WATER;
    }

    private boolean getBlocksAttachedTo(BlockState state) {
        return this.countBlocksAlgaeIsAttachedTo(state) > 0;
    }

    private int countBlocksAlgaeIsAttachedTo(BlockState state) {
        int i = 0;

        for(BooleanProperty booleanproperty : FACING_TO_PROPERTY_MAP.values()) {
            if (state.get(booleanproperty)) {
                ++i;
            }
        }

        return i;
    }

    private boolean hasAttachment(BlockView blockReader, BlockPos pos, Direction direction) {
        if (direction == Direction.DOWN) {
            return false;
        } else {
            BlockPos blockpos = pos.offset(direction);
            if (canAttachTo(blockReader, blockpos, direction)) {
                return true;
            } else if (direction.getAxis() == Direction.Axis.Y) {
                return false;
            } else {
                BooleanProperty booleanproperty = FACING_TO_PROPERTY_MAP.get(direction);
                BlockState blockstate = blockReader.getBlockState(pos.up());
                return blockstate.isOf(this) && blockstate.get(booleanproperty);
            }
        }
    }

    public static boolean canAttachTo(BlockView blockReader, BlockPos worldIn, Direction neighborPos) {
        BlockState blockstate = blockReader.getBlockState(worldIn);
        return Block.isFaceFullSquare(blockstate.getCollisionShape(blockReader, worldIn), neighborPos.getOpposite());
    }

    private BlockState func_196545_h(BlockState state, BlockView blockReader, BlockPos pos) {
        BlockPos blockpos = pos.up();
        if (state.get(UP)) {
            state = state.with(UP, canAttachTo(blockReader, blockpos, Direction.DOWN));
        }

        BlockState blockstate = null;

        for(Direction direction : Direction.Type.HORIZONTAL) {
            BooleanProperty booleanproperty = getPropertyFor(direction);
            if (state.get(booleanproperty)) {
                boolean flag = this.hasAttachment(blockReader, pos, direction);
                if (!flag) {
                    if (blockstate == null) {
                        blockstate = blockReader.getBlockState(blockpos);
                    }

                    flag = blockstate.isOf(this) && blockstate.get(booleanproperty);
                }

                state = state.with(booleanproperty, flag);
            }
        }

        return state;
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState stateIn, Direction facing, BlockState facingState, WorldAccess worldIn, BlockPos currentPos, BlockPos facingPos) {
        if (facing == Direction.DOWN) {
            return super.getStateForNeighborUpdate(stateIn, facing, facingState, worldIn, currentPos, facingPos);
        } else {
            BlockState blockstate = this.func_196545_h(stateIn, worldIn, currentPos);
            return !this.getBlocksAttachedTo(blockstate) ? Blocks.WATER.getDefaultState() : blockstate;
        }
    }

    @Override
    public void randomTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random random) {
        if (worldIn.random.nextInt(4) == 0 && worldIn.isChunkLoaded(pos)) { // Forge: check area to prevent loading unloaded chunks
            Direction direction = Direction.random(random);
            BlockPos blockpos = pos.up();
            if (direction.getAxis().isHorizontal() && !state.get(getPropertyFor(direction))) {
                if (this.hasAlgaeBelow(worldIn, pos)) {
                    BlockPos blockpos4 = pos.offset(direction);
                    BlockState blockstate4 = worldIn.getBlockState(blockpos4);
                    if (isWater(blockstate4, blockpos4)) {
                        Direction direction3 = direction.rotateYClockwise();
                        Direction direction4 = direction.rotateYCounterclockwise();
                        boolean flag = state.get(getPropertyFor(direction3));
                        boolean flag1 = state.get(getPropertyFor(direction4));
                        BlockPos blockpos2 = blockpos4.offset(direction3);
                        BlockPos blockpos3 = blockpos4.offset(direction4);
                        if (flag && canAttachTo(worldIn, blockpos2, direction3)) {
                            worldIn.setBlockState(blockpos4, this.getDefaultState().with(getPropertyFor(direction3), Boolean.TRUE), 2);
                        } else if (flag1 && canAttachTo(worldIn, blockpos3, direction4)) {
                            worldIn.setBlockState(blockpos4, this.getDefaultState().with(getPropertyFor(direction4), Boolean.TRUE), 2);
                        } else {
                            Direction direction1 = direction.getOpposite();
                            if (flag && isWaterBlock(worldIn, blockpos2) && canAttachTo(worldIn, pos.offset(direction3), direction1)) {
                                worldIn.setBlockState(blockpos2, this.getDefaultState().with(getPropertyFor(direction1), Boolean.TRUE), 2);
                            } else if (flag1 && isWaterBlock(worldIn, blockpos3) && canAttachTo(worldIn, pos.offset(direction4), direction1)) {
                                worldIn.setBlockState(blockpos3, this.getDefaultState().with(getPropertyFor(direction1), Boolean.TRUE), 2);
                            } else if ((double)worldIn.random.nextFloat() < 0.05D && canAttachTo(worldIn, blockpos4.up(), Direction.UP)) {
                                worldIn.setBlockState(blockpos4, this.getDefaultState().with(UP, Boolean.TRUE), 2);
                            }
                        }
                    } else if (canAttachTo(worldIn, blockpos4, direction)) {
                        worldIn.setBlockState(pos, state.with(getPropertyFor(direction), Boolean.TRUE), 2);
                    }

                }
            } else {
                if (direction == Direction.UP && pos.getY() < 255) {
                    if (this.hasAttachment(worldIn, pos, direction)) {
                        worldIn.setBlockState(pos, state.with(UP, Boolean.TRUE), 2);
                        return;
                    }

                    if (isWaterBlock(worldIn, blockpos)) {
                        if (!this.hasAlgaeBelow(worldIn, pos)) {
                            return;
                        }

                        BlockState blockstate3 = state;

                        for(Direction direction2 : Direction.Type.HORIZONTAL) {
                            if (random.nextBoolean() || !canAttachTo(worldIn, blockpos.offset(direction2), Direction.UP)) {
                                blockstate3 = blockstate3.with(getPropertyFor(direction2), Boolean.FALSE);
                            }
                        }

                        if (this.isFacingCardinal(blockstate3)) {
                            worldIn.setBlockState(blockpos, blockstate3, 2);
                        }

                        return;
                    }
                }

                if (pos.getY() > 0) {
                    BlockPos blockpos1 = pos.down();
                    BlockState blockstate = worldIn.getBlockState(blockpos1);
                    boolean isWater = isWater(blockstate, blockpos1);
                    if (isWater || blockstate.isOf(this)) {
                        BlockState blockstate1 = isWater ? this.getDefaultState() : blockstate;
                        BlockState blockstate2 = this.func_196544_a(state, blockstate1, random);
                        if (blockstate1 != blockstate2 && this.isFacingCardinal(blockstate2)) {
                            worldIn.setBlockState(blockpos1, blockstate2, 2);
                        }
                    }
                }

            }
        }
    }

    public static boolean isWaterBlock(World worldIn, BlockPos pos)
    {
        return isWater(worldIn.getBlockState(pos), pos);
    }

    public static boolean isWater(BlockState state, BlockPos pos)
    {
        return state.getMaterial() == Material.WATER;
    }

    private BlockState func_196544_a(BlockState state, BlockState state2, Random rand) {
        for(Direction direction : Direction.Type.HORIZONTAL) {
            if (rand.nextBoolean()) {
                BooleanProperty booleanproperty = getPropertyFor(direction);
                if (state.get(booleanproperty)) {
                    state2 = state2.with(booleanproperty, Boolean.TRUE);
                }
            }
        }

        return state2;
    }

    private boolean isFacingCardinal(BlockState state) {
        return state.get(NORTH) || state.get(EAST) || state.get(SOUTH) || state.get(WEST);
    }

    private boolean hasAlgaeBelow(BlockView blockReader, BlockPos pos) {
        int i = 4;
        Iterable<BlockPos> iterable = BlockPos.iterate(pos.getX() - 4, pos.getY() - 1, pos.getZ() - 4, pos.getX() + 4, pos.getY() + 1, pos.getZ() + 4);
        int j = 5;

        for(BlockPos blockpos : iterable) {
            if (blockReader.getBlockState(blockpos).isOf(this)) {
                --j;
                if (j <= 0) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean canReplace(BlockState state, ItemPlacementContext useContext) {
        BlockState blockstate = useContext.getWorld().getBlockState(useContext.getBlockPos());
        if (blockstate.isOf(this)) {
            return this.countBlocksAlgaeIsAttachedTo(blockstate) < FACING_TO_PROPERTY_MAP.size();
        } else {
            return super.canReplace(state, useContext);
        }
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        BlockState blockstate = context.getWorld().getBlockState(context.getBlockPos());
        boolean flag = blockstate.isOf(this);
        BlockState blockstate1 = flag ? blockstate : this.getDefaultState();

        for(Direction direction : context.getPlacementDirections()) {
            if (direction != Direction.DOWN) {
                BooleanProperty booleanproperty = getPropertyFor(direction);
                boolean flag1 = flag && blockstate.get(booleanproperty);
                if (!flag1 && this.hasAttachment(context.getWorld(), context.getBlockPos(), direction)) {
                    return blockstate1.with(booleanproperty, Boolean.TRUE);
                }
            }
        }

        return flag ? blockstate1 : null;
    }

    @Override
    public int getOpacity(BlockState state, BlockView worldIn, BlockPos pos) {
        return 255;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(UP, NORTH, EAST, SOUTH, WEST, WATERLOGGED);
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rot) {
        switch(rot) {
            case CLOCKWISE_180:
                return state.with(NORTH, state.get(SOUTH)).with(EAST, state.get(WEST)).with(SOUTH, state.get(NORTH)).with(WEST, state.get(EAST));
            case COUNTERCLOCKWISE_90:
                return state.with(NORTH, state.get(EAST)).with(EAST, state.get(SOUTH)).with(SOUTH, state.get(WEST)).with(WEST, state.get(NORTH));
            case CLOCKWISE_90:
                return state.with(NORTH, state.get(WEST)).with(EAST, state.get(NORTH)).with(SOUTH, state.get(EAST)).with(WEST, state.get(SOUTH));
            default:
                return state;
        }
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirrorIn) {
        switch(mirrorIn) {
            case LEFT_RIGHT:
                return state.with(NORTH, state.get(SOUTH)).with(SOUTH, state.get(NORTH));
            case FRONT_BACK:
                return state.with(EAST, state.get(WEST)).with(WEST, state.get(EAST));
            default:
                return super.mirror(state, mirrorIn);
        }
    }

    public static BooleanProperty getPropertyFor(Direction side) {
        return FACING_TO_PROPERTY_MAP.get(side);
    }
}
