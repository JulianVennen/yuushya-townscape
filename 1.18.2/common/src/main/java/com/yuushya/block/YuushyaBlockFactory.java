package com.yuushya.block;

import com.yuushya.block.blockstate.YuushyaBlockStates;
import com.yuushya.collision.CollisionFileReader;
import com.yuushya.entity.ChairEntityUtils;
import com.yuushya.registries.YuushyaRegistries;
import com.yuushya.registries.YuushyaRegistryData;
import com.yuushya.utils.YuushyaUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.yuushya.block.FaceBlock.getPositionOfFaceX;
import static com.yuushya.block.FaceBlock.getPositionOfFaceZ;
import static com.yuushya.block.PoleBlock.getPositionOfPole;
import static com.yuushya.block.blockstate.YuushyaBlockStates.*;
import static com.yuushya.utils.YuushyaUtils.toBlockMaterial;
import static com.yuushya.utils.YuushyaUtils.toSound;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.*;

public class YuushyaBlockFactory{

    private static final Map<Integer,VoxelShape> yuushyaVoxelShapes =new HashMap<>();
    public static Map<Integer, VoxelShape> getYuushyaVoxelShapes() {
        return yuushyaVoxelShapes;
    }

    public static class BlockWithClassType extends AbstractYuushyaBlock{
        public String classType;
        private final Map<BlockState,VoxelShape> voxelShapeCache = new HashMap<>();
        public BlockWithClassType(Properties properties, Integer tipLines, String classType) {
            super(properties, tipLines);
            this.classType=classType;
        }

        public boolean isTheSameType(BlockWithClassType block){
            return classType.equals(block.classType);
        }

        @Override
        public VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
            if(!voxelShapeCache.containsKey(blockState)){
                if(!getYuushyaVoxelShapes().containsKey(Block.getId(blockState))){
                    CollisionFileReader.readCollisionToVoxelShape(voxelShapeCache,blockState.getBlock(),Registry.BLOCK.getKey(blockState.getBlock()).toString());
                }
                VoxelShape shape = getYuushyaVoxelShapes().getOrDefault(Block.getId(blockState),Shapes.empty());
                voxelShapeCache.put(blockState,shape);
                return shape;
            }
            return voxelShapeCache.get(blockState);
        }

//        @Override
//        public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
//            return Shapes.create(this.getCollisionShape(state,level,pos,context).bounds());
//        }

//        @Override
//        public VoxelShape getCollisionShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
//            return this.hasCollision ? Shapes.block() : Shapes.empty();
//        }
        @Override
        public BlockState rotate(BlockState state, Rotation rotation) {
            if(state.hasProperty(FACING))
                state = state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
            else if(state.hasProperty(HORIZONTAL_FACING))
                state = state.setValue(HORIZONTAL_FACING, rotation.rotate(state.getValue(HORIZONTAL_FACING)));
            if(state.hasProperty(AXIS)){
                state = switch (rotation) {
                    case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> switch (state.getValue(AXIS)) {
                        case X -> state.setValue(AXIS, Direction.Axis.Z);
                        case Z -> state.setValue(AXIS, Direction.Axis.X);
                        default -> state;
                    };
                    default -> state;
                };
            }
            return state;
        }

        @Override
        public BlockState mirror(BlockState state, Mirror mirror) {
            if(state.hasProperty(FACING))
                return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
            else if(state.hasProperty(HORIZONTAL_FACING))
                return state.setValue(HORIZONTAL_FACING, mirror.mirror(state.getValue(HORIZONTAL_FACING)));
            return super.mirror(state,mirror);
        }
    }

    public static BlockBehaviour.Properties getBlockProperties(BlockBehaviour.Properties blockProperties, YuushyaRegistryData.Block.Properties yuushyaBlockProperties){
        blockProperties = blockProperties.strength(yuushyaBlockProperties.hardness,yuushyaBlockProperties.resistance);
        if ( yuushyaBlockProperties.sound!=null&&!yuushyaBlockProperties.sound.isEmpty()) blockProperties=blockProperties.sound(toSound(yuushyaBlockProperties.sound));
        if (yuushyaBlockProperties.lightLevel!=0) blockProperties=blockProperties.lightLevel((state)->yuushyaBlockProperties.lightLevel);
        if (!yuushyaBlockProperties.hasCollision) blockProperties=blockProperties.noCollission();
        if (yuushyaBlockProperties.isDelicate) blockProperties=blockProperties.instabreak();
        if (!yuushyaBlockProperties.isSolid) blockProperties=blockProperties.noOcclusion();
        return blockProperties;
    }

    public static BlockBehaviour.Properties getBlockProperties(YuushyaRegistryData.Block.Properties yuushyaBlockProperties){
        if(yuushyaBlockProperties == null) return BlockBehaviour.Properties.of(Material.METAL);
        BlockBehaviour.Properties blockProperties = BlockBehaviour.Properties
                .of(toBlockMaterial(yuushyaBlockProperties.material));
        return getBlockProperties(blockProperties,yuushyaBlockProperties);
    }
    private static List<? extends Property<?>> getBlockStateProperties(YuushyaRegistryData.Block.BlockState yuushyaBlockState){
        if (yuushyaBlockState==null||yuushyaBlockState.states==null) return List.of();
        return yuushyaBlockState.states.stream().map(YuushyaBlockStates::toBlockStateProperty).toList();
    }

    public static Block create(YuushyaRegistryData.Block yuushyaBlock){
        return create(getBlockProperties(yuushyaBlock.properties),yuushyaBlock);
    }
    public static Block create(BlockBehaviour.Properties properties,YuushyaRegistryData.Block yuushyaBlock){
        //套装，依然可以用在自定义的classType里
        if (yuushyaBlock.autoGenerated==null) {
            yuushyaBlock.autoGenerated=new YuushyaRegistryData.Block.AutoGenerated();
            yuushyaBlock.autoGenerated.collision="block";
        }
        AbstractYuushyaBlockType kitType = null;
        if (yuushyaBlock.blockstate.kit !=null&&!yuushyaBlock.blockstate.kit.isEmpty()){
            switch (yuushyaBlock.blockstate.kit){
                case "normal"->{
                    kitType = new NormalBlock();}
                case "attachment" -> {
                    kitType = new AttachmentBlock();}
                case "line" -> {
                    kitType = new LineBlock();}
                case "face" -> {
                    kitType = new FaceBlock();}
                case "pole" -> {
                    kitType = new PoleBlock();}
                case "tri_part"->{
                    kitType = new TriPartBlock();}
                case "tube"->{
                    kitType = new TubeBlock();}
                case "compact"->{
                    kitType = new CompactBlock();}
                case "repeat"->{
                    kitType = new RepeatBlock();}
                case "block"->{}
                case "VanillaSlabBlock"->{
                    return new SlabBlock(properties){
                        @Override//注释栏数
                        public void appendHoverText(@NotNull ItemStack itemStack, @Nullable BlockGetter blockGetter, @NotNull List<Component> tooltips, @NotNull TooltipFlag tooltipFlag) {
                            for(int i=1;i<=yuushyaBlock.properties.lines;i++) tooltips.add(new TranslatableComponent(this.getDescriptionId()+".line"+i));
                        }};}
                case "VanillaDoorBlock"->{
                    return new DoorBlock(properties){
                        @Override//注释栏数
                        public void appendHoverText(@NotNull ItemStack itemStack, @Nullable BlockGetter blockGetter, @NotNull List<Component> tooltips, @NotNull TooltipFlag tooltipFlag) {
                            for(int i=1;i<=yuushyaBlock.properties.lines;i++) tooltips.add(new TranslatableComponent(this.getDescriptionId()+".line"+i));
                        }};}
                case "VanillaStairBlock"->{
                    BlockState blockState= Blocks.OAK_PLANKS.defaultBlockState();
                    if (yuushyaBlock.properties.parent!=null){
                        //TODO:这里可能会崩
                        if (yuushyaBlock.properties.parent.contains(":"))
                            blockState= Registry.BLOCK.get(new ResourceLocation(yuushyaBlock.properties.parent)).defaultBlockState();
                        else
                            blockState= YuushyaRegistries.BLOCKS.get(yuushyaBlock.properties.parent).get().defaultBlockState();
                    }
                    return new StairBlock(blockState,properties){
                        @Override//注释栏数
                        public void appendHoverText(@NotNull ItemStack itemStack, @Nullable BlockGetter blockGetter, @NotNull List<Component> tooltips, @NotNull TooltipFlag tooltipFlag) {
                            for(int i=1;i<=yuushyaBlock.properties.lines;i++) tooltips.add(new TranslatableComponent(this.getDescriptionId()+".line"+i));
                        }};}
            }
        }
        List<? extends Property<?>> blockStateProperties=getBlockStateProperties(yuushyaBlock.blockstate);
        //classType 用于一些内定的方块//TODO:还是算了，直接让kit承担内定方块的重任
        switch (yuushyaBlock.classType){
            case "CableBlock"->{return new CableBlock(properties,yuushyaBlock.properties.lines,"CableBlock");}
            case "" -> {return new Block(properties);}
        }
        AbstractYuushyaBlockType finalKitType = kitType;
        return new BlockWithClassType(properties,yuushyaBlock.properties.lines,yuushyaBlock.classType){
            {
                this.registerDefaultState(YuushyaBlockStates.getDefaultBlockState(this.stateDefinition.any()));
                if (finalKitType != null) finalKitType.defaultBlockState = this.defaultBlockState();
            }
            @Override
            protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> stateBuilder) {
                if(finalKitType !=null) stateBuilder.add(finalKitType.getBlockStateProperty().toArray(Property[]::new));
                else stateBuilder.add(blockStateProperties.toArray(Property[]::new));
                if(yuushyaBlock.blockstate!=null && yuushyaBlock.blockstate.forms!=null&&!blockStateProperties.contains(FORM8)){
                    int forms = yuushyaBlock.blockstate.forms.size();
                    if(forms>1) stateBuilder.add(YuushyaBlockStates.forms(forms));
                }
            }

            @Override
            public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
                if(finalKitType!=null) finalKitType.setPlacedBy(level, pos, state, placer, stack);
                else super.setPlacedBy(level, pos, state, placer, stack);
            }

            @Override
            public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
                if(finalKitType!=null) finalKitType.playerWillDestroy(level, pos, state, player);
                else super.playerWillDestroy(level, pos, state, player);
            }

            @Override
            @Nullable
            public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
                if(finalKitType!=null) return finalKitType.getStateForPlacement(blockPlaceContext);
                LevelAccessor levelAccessor=blockPlaceContext.getLevel();
                BlockPos blockPos=blockPlaceContext.getClickedPos();
                BlockState res=this.defaultBlockState();
                //from FaceAttachedHorizontalDirectionalBlock
                if (res.hasProperty(BlockStateProperties.ATTACH_FACE) && res.hasProperty(BlockStateProperties.HORIZONTAL_FACING)){
                    Direction direction = blockPlaceContext.getNearestLookingDirection();
                    res= direction.getAxis() == Direction.Axis.Y
                            ? res
                            .setValue(BlockStateProperties.ATTACH_FACE, direction == Direction.UP ? AttachFace.CEILING : AttachFace.FLOOR)
                            .setValue(BlockStateProperties.HORIZONTAL_FACING, blockPlaceContext.getHorizontalDirection())
                            : res
                            .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.WALL)
                            .setValue(BlockStateProperties.HORIZONTAL_FACING, direction.getOpposite());
                }
                else if(res.hasProperty(BlockStateProperties.HORIZONTAL_FACING)){
                    res = blockPlaceContext.getClickedFace().getAxis() == Direction.Axis.Y
                            ? res.setValue(HORIZONTAL_FACING, blockPlaceContext.getHorizontalDirection())
                            : res.setValue(HORIZONTAL_FACING, blockPlaceContext.getClickedFace().getOpposite());
                }
                if(res.hasProperty(XPOS)) res = res.setValue(XPOS, getPositionOfFaceX(this.defaultBlockState(),levelAccessor,blockPos));
                if(res.hasProperty(ZPOS)) res = res.setValue(ZPOS, getPositionOfFaceZ(this.defaultBlockState(),levelAccessor,blockPos));
                if(res.hasProperty(YPOS)) res =res.setValue(YPOS, getPositionOfPole(this.defaultBlockState(),levelAccessor,blockPos));
//TODO：pos horizon那些都没做
                return res;
            }
            @Override
            public boolean canSurvive(BlockState blockState, LevelReader levelReader, BlockPos blockPos) {
                if(finalKitType!=null) return finalKitType.canSurvive(blockState,levelReader,blockPos);
                return super.canSurvive(blockState,levelReader,blockPos);
            }

            @Override
            public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
                if(finalKitType!=null) return finalKitType.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
                //TODO: 像上面那样写出来
                return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
            }

            @Override
            public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
                YuushyaRegistryData.Block.Usage usage = yuushyaBlock.usage;
                if(usage!=null){
                    if(!level.isClientSide&&usage.sound!=null&&!usage.sound.isBlank()&&player.getItemInHand(hand).isEmpty()){
                        SoundEvent soundEvent = Registry.SOUND_EVENT.get(new ResourceLocation(usage.sound));
                        level.playSound(null, pos, soundEvent, SoundSource.BLOCKS, 1f, 0.2f);
                    }
                    if(usage.sitPos!=null&&usage.sitPos.size()==3&&player.getItemInHand(hand).isEmpty()){
                        return ChairEntityUtils.use(new Vec3(usage.sitPos.get(0),usage.sitPos.get(1),usage.sitPos.get(2)) ,state,level,pos,player,hand,hit);
                    }
                    if(usage.cycleForms!=null&&!usage.cycleForms.isEmpty()&&player.getItemInHand(hand).isEmpty()){
                        Property<?> property = YuushyaUtils.getFormFromState(state);
                        if(property!=null){
                            do{
                                state = state.cycle(property);
                            } while(!usage.cycleForms.contains(state.getValue(property)));
                            level.setBlock(pos, state, 2);
                            return InteractionResult.sidedSuccess(level.isClientSide);
                        }
                    }
                }
                return super.use(state,level,pos,player,hand,hit);
            }

            @Override
            public OffsetType getOffsetType() {
                return YuushyaUtils.toOffsetType(yuushyaBlock.properties.offset);
            }

        };
    }
    public static boolean isTheSameBlock(BlockState state1, BlockState state2) {return state2.getBlock()==state1.getBlock(); }
    public static boolean isTheSameFacing(BlockState blockState1,BlockState blockState2){return blockState1.getValue(HORIZONTAL_FACING)==blockState2.getValue(HORIZONTAL_FACING);}
    public static boolean isTheSameLine(BlockState blockState1,BlockState blockState2){return blockState1.getValue(HORIZONTAL_FACING)==blockState2.getValue(HORIZONTAL_FACING)||blockState1.getValue(HORIZONTAL_FACING)==blockState2.getValue(HORIZONTAL_FACING).getOpposite();}

}
