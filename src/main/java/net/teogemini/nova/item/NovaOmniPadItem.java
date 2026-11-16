package net.teogemini.nova.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.teogemini.nova.network.NovaOmniPadNetwork;

public class NovaOmniPadItem extends Item {
    public static final int MAX_RESERVE_ENERGY = 10000; // 1000% (10 full NOVA charges)
    public static final int DEFAULT_RESERVE_ENERGY = 2000; // 200% initial charge from NOVA Battery
    private static final String KEY_RESERVE = "OmniPadReserveEnergy";

    public NovaOmniPadItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.RARE));
    }

    public static int getReserveEnergy(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return DEFAULT_RESERVE_ENERGY;
        }
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(KEY_RESERVE)) {
            return DEFAULT_RESERVE_ENERGY;
        }
        return Mth.clamp(tag.getIntOr(KEY_RESERVE, DEFAULT_RESERVE_ENERGY), 0, MAX_RESERVE_ENERGY);
    }

    public static void setReserveEnergy(ItemStack stack, int energy) {
        if (stack.isEmpty()) return;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        tag.putInt(KEY_RESERVE, Mth.clamp(energy, 0, MAX_RESERVE_ENERGY));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isSpectator() || !player.isAlive()) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.35F, 1.65F);
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 1.35F);
            NovaOmniPadNetwork.sendSync(serverPlayer, true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        return use(context.getLevel(), player, context.getHand());
    }
}
