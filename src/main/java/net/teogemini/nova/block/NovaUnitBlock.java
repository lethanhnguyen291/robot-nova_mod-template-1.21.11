package net.teogemini.nova.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.teogemini.nova.registry.NovaMaterials;
import net.teogemini.nova.entity.NovaSummoning;

/** Facing also controls the six texture maps. Only a newly placed head initiates assembly. */
public final class NovaUnitBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<NovaUnitBlock> CODEC=simpleCodec(NovaUnitBlock::new);
    public NovaUnitBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected MapCodec<NovaUnitBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());}
    @Override protected BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState state,Mirror mirror){return state.rotate(mirror.getRotation(state.getValue(FACING)));}
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moved){
        super.onPlace(state,level,pos,old,moved);
        if(level instanceof ServerLevel&&!old.is(this)&&state.is(NovaMaterials.NOVA_HEAD_UNIT))level.scheduleTick(pos,this,1);
    }
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        if(state.is(NovaMaterials.NOVA_HEAD_UNIT))NovaSummoning.summon(level,pos);
    }
}
