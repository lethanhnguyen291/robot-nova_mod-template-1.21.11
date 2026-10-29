package net.teogemini.nova.ai.dialogue;

import java.util.Locale;
import static net.teogemini.nova.ai.dialogue.NovaDialogueText.tr;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import net.teogemini.nova.ai.dialogue.NovaDialogueBrain.Intent;
import net.teogemini.nova.entity.NovaEntity;
import net.teogemini.nova.entity.NovaWorkSupport;
import net.teogemini.nova.entity.companion.NovaCompanion;
import net.teogemini.nova.registry.ModBlocks;

/** Reads live state only when the owner sends a message; does not search for work targets. */
public final class NovaDialogueFacts {
    private NovaDialogueFacts() {}

    public static NovaDialogueBrain.Facts read(NovaEntity nova) { return read(nova,"vi_vn"); }

    public static NovaDialogueBrain.Facts read(NovaEntity nova,String language) {
        var memory = nova.companion().memories();
        memory.initialize(nova.getUUID().hashCode());
        int used = 0;
        for (var stack : nova.getInventory().getItems()) if (!stack.isEmpty()) used++;

        boolean swordNova = nova.isSwordNova();
        String profession = profession(nova,language);
        String toolDesc = toolDescription(nova,language);
        String chestDesc = chestDescription(nova,language);
        String envDesc = environmentDescription(nova,language);
        String ownerName = nova.getOwner() != null ? nova.getOwner().getName().getString() : tr(language,"fact.023");
        String blocker = blocker(nova,language);
        String currentStatus = status(nova,language);

        String need;
        if (nova.work().energy() <= 100) {
            need = tr(language,"fact.001" ,nova.work().energy()/10);
        } else if (swordNova) {
            if (!hasAnyUsableSword(nova)) {
                need = tr(language,"fact.024");
            } else if (!blocker.isEmpty()) {
                need = blocker;
            } else if (!nova.getOffhandItem().is(ItemTags.SWORDS)) {
                need = tr(language,"fact.025");
            } else {
                need = tr(language,"fact.003" ,nova.work().energy()/10, currentStatus);
            }
        } else if (!NovaWorkSupport.isTool(nova.getPrimaryTool())) {
            need = tr(language,"fact.026");
        } else if (!nova.companion().usableTool(nova.getPrimaryTool()) && !NovaWorkSupport.hasUsableToolOnBody(nova, nova.getPrimaryTool())) {
            need = tr(language,"fact.002" ,NovaDialogueText.itemName(language,nova.getPrimaryTool()));
        } else if (nova.getLinkedChest() == null) {
            need = tr(language,"fact.027");
        } else if (!blocker.isEmpty()) {
            need = blocker;
        } else if (used == 27) {
            need = tr(language,"fact.028");
        } else {
            need = tr(language,"fact.004" ,currentStatus);
        }

        if (!swordNova && nova.getPrimaryTool().is(ItemTags.AXES) && !nova.trees().planting().isEmpty()) {
            need += " " + tr(language,"fact.005" ,nova.trees().planting().size()).stripLeading();
        }

        String xp = nova.work().skillLevel() >= 5
                ? tr(language,"fact.029")
                : tr(language,"fact.006" ,nova.work().skillLevel(), nova.work().experience(), nova.work().nextLevelCost());

        boolean working = nova.work().enabled() && nova.getWorkAction() != NovaEntity.WORK_NONE;
        boolean injured = nova.getHealth() < nova.getMaxHealth() * 0.75f;

        return new NovaDialogueBrain.Facts(
                memory.personality().label,
                memory.bond(),
                nova.work().energy() / 10,
                String.format(Locale.ROOT, "%.1f/%.1f", nova.getHealth(), nova.getMaxHealth()),
                currentStatus,
                blocker,
                need,
                used,
                xp,
                NovaDialogueText.totals(language,memory.totals()),
                memory.journal().stream().map(s->NovaDialogueText.memory(language,s)).toList(),
                profession,
                toolDesc,
                chestDesc,
                envDesc,
                ownerName,
                swordNova,
                working,
                injured
        );
    }

    private static boolean hasAnyUsableSword(NovaEntity nova) {
        if (nova.getMainHandItem().is(ItemTags.SWORDS) && nova.companion().usableTool(nova.getMainHandItem())) return true;
        if (nova.getOffhandItem().is(ItemTags.SWORDS) && nova.companion().usableTool(nova.getOffhandItem())) return true;
        return NovaWorkSupport.hasUsableToolOnBody(nova, nova.getMainHandItem());
    }

    private static String profession(NovaEntity nova,String language) {
        ItemStack primary = nova.getPrimaryTool();
        if (nova.isSwordNova() || primary.is(ItemTags.SWORDS)) {
            return nova.getOffhandItem().is(ItemTags.SWORDS) ? tr(language,"fact.030") : tr(language,"fact.031");
        }
        if (primary.is(ItemTags.PICKAXES)) return tr(language,"fact.032");
        if (primary.is(ItemTags.AXES)) return tr(language,"fact.033");
        if (primary.is(ItemTags.HOES)) return tr(language,"fact.034");
        if (primary.is(ItemTags.SHOVELS)) return tr(language,"fact.035");
        return tr(language,"fact.036");
    }

    private static String formatItemStatus(ItemStack stack,String language) {
        if (stack.isEmpty()) return tr(language,"fact.037");
        String name = NovaDialogueText.itemName(language,stack);
        if (stack.isDamageableItem()) {
            int rem = Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
            return tr(language,"fact.007" ,name, rem, stack.getMaxDamage());
        }
        return stack.getCount() > 1 ? name + " x" + stack.getCount() : name;
    }

    private static String toolDescription(NovaEntity nova,String language) {
        ItemStack main = nova.getPrimaryTool();
        ItemStack off = nova.getOffhandItem();
        int spares = 0;
        for (int i = 0; i < 4; i++) {
            if (!nova.work().spareTools.getItem(i).isEmpty()) spares++;
        }
        return tr(language,"fact.008" ,formatItemStatus(main,language), formatItemStatus(off,language), spares);
    }

    private static String chestDescription(NovaEntity nova,String language) {
        if (nova.isSwordNova()) {
            return tr(language,"fact.038");
        }
        if (nova.isCarryingChest()) {
            return tr(language,"fact.039");
        }
        BlockPos linked = nova.getLinkedChest();
        int extraChests = nova.companion().memories().deposits().size();
        int buttons = nova.companion().memories().buttons().size();
        if (linked == null) {
            return tr(language,"fact.009" ,extraChests, buttons);
        }
        return tr(language,"fact.010" ,linked.getX(), linked.getY(), linked.getZ(), extraChests, buttons);
    }

    private static String environmentDescription(NovaEntity nova,String language) {
        long dayTime = Math.floorMod(nova.level().getDayTime(), 24000L);
        String timeOfDay = dayTime < 1000 ? tr(language,"fact.040")
                : dayTime < 6000 ? tr(language,"fact.041")
                : dayTime < 12000 ? tr(language,"fact.042")
                : dayTime < 13800 ? tr(language,"fact.043")
                : dayTime < 22200 ? tr(language,"fact.044")
                : tr(language,"fact.045");
        String weather = nova.level().isThundering() ? tr(language,"fact.046")
                : nova.level().isRaining() ? tr(language,"fact.047")
                : tr(language,"fact.048");
        return tr(language,"fact.011" ,timeOfDay, weather);
    }

    private static String blocker(NovaEntity nova,String language) {
        if (nova.isNoAi()) return tr(language,"fact.049");
        if (nova.companion().panicking()) return tr(language,"fact.050");
        if (nova.isCarryingChest()) return tr(language,"fact.051");
        if (nova.work().inventoryOpen()) return tr(language,"fact.052");
        if (nova.isPerformingAction()) return tr(language,"fact.053");
        if (nova.isRallying()) return tr(language,"fact.054");
        if (nova.isOrderedToSit()) return tr(language,"fact.055");
        if (nova.work().energy() <= 100) return tr(language,"fact.012" ,nova.work().energy()/10);

        if (nova.isSwordNova()) {
            if (!hasAnyUsableSword(nova)) {
                return tr(language,"fact.056");
            }
            if (!nova.work().enabled() && nova.getNovaMoveMode() == NovaEntity.MODE_WAIT) {
                return tr(language,"fact.057");
            }
            return "";
        }

        if (!nova.work().enabled()) return tr(language,"fact.058");
        if (nova.getNovaMoveMode() != NovaEntity.MODE_FREE)
            return tr(language,"fact.013" ,nova.getNovaMoveMode()==NovaEntity.MODE_WAIT ? tr(language,"fact.059") : tr(language,"fact.060"));
        var chest = nova.getLinkedChest();
        if (chest == null) return tr(language,"fact.061");
        if (!nova.level().hasChunkAt(chest)) return tr(language,"fact.062");
        if (!nova.level().getBlockState(chest).is(ModBlocks.NOVA_CHEST)) return tr(language,"fact.063");
        if (nova.level() instanceof ServerLevel level && !level.getGameRules().get(GameRules.MOB_GRIEFING))
            return tr(language,"fact.064");
        if (nova.getCompanionState() == NovaCompanion.HELP) return tr(language,"fact.065");
        if (nova.getCompanionState() == NovaCompanion.RETURNING) return tr(language,"fact.066");
        if (nova.getCompanionState() == NovaCompanion.RESTING) return tr(language,"fact.067");
        if (nova.getCompanionState() == NovaCompanion.RAIN) return tr(language,"fact.068");
        if (!nova.companion().workAllowed()) return tr(language,"fact.069");
        if (!NovaWorkSupport.isTool(nova.getPrimaryTool())) return tr(language,"fact.070");
        if (!nova.companion().usableTool(nova.getPrimaryTool()) && !NovaWorkSupport.hasUsableToolOnBody(nova, nova.getPrimaryTool()))
            return tr(language,"fact.071");
        if (nova.getPrimaryTool().is(ItemTags.AXES) && nova.trees().full())
            return tr(language,"fact.072");
        return "";
    }

    private static String status(NovaEntity nova,String language) {
        if (nova.pathClearing().busy()) return tr(language,"fact.014" ,profession(nova,language));
        if (nova.companion().panicking()) return tr(language,"fact.073");
        if (nova.isCarryingChest()) return tr(language,"fact.074");
        if (nova.isRallying()) return tr(language,"fact.075");
        if (nova.isPerformingAction()) return tr(language,"fact.076");
        if (nova.isOrderedToSit()) return tr(language,"fact.077");

        if (nova.isSwordNova()) {
            LivingEntity target = nova.getTarget();
            String weaponMode = nova.getOffhandItem().is(ItemTags.SWORDS) ? tr(language,"fact.078") : tr(language,"fact.079");
            if (target != null && target.isAlive()) {
                return tr(language,"fact.015" ,weaponMode, NovaDialogueText.entityName(language,target));
            }
            if (!hasAnyUsableSword(nova)) {
                return tr(language,"fact.080");
            }
            if (nova.work().enabled()) {
                return tr(language,"fact.016" ,weaponMode);
            }
            return nova.getNovaMoveMode() == NovaEntity.MODE_FOLLOW
                    ? tr(language,"fact.017" ,weaponMode)
                    : tr(language,"fact.018" ,weaponMode);
        }

        if (nova.getCompanionState() == NovaCompanion.RETURNING) return tr(language,"fact.081");
        if (nova.getCompanionState() == NovaCompanion.RESTING) return tr(language,"fact.082");
        if (nova.getCompanionState() == NovaCompanion.HELP) return tr(language,"fact.083");
        if (nova.getCompanionState() == NovaCompanion.RAIN) return tr(language,"fact.084");
        return switch (nova.getWorkAction()) {
            case NovaEntity.WORK_CHOP -> tr(language,"fact.085");
            case NovaEntity.WORK_MINE -> tr(language,"fact.086");
            case NovaEntity.WORK_FARM -> tr(language,"fact.087");
            case NovaEntity.WORK_DIG -> tr(language,"fact.088");
            case NovaEntity.WORK_PLANT -> tr(language,"fact.089");
            case NovaEntity.WORK_CHEST -> tr(language,"fact.090");
            default -> switch (nova.getNovaMoveMode()) {
                case NovaEntity.MODE_FOLLOW -> tr(language,"fact.019" ,profession(nova,language));
                case NovaEntity.MODE_WAIT -> tr(language,"fact.020" ,profession(nova,language));
                default -> nova.work().enabled()
                        ? tr(language,"fact.021" ,profession(nova,language))
                        : tr(language,"fact.022" ,profession(nova,language));
            };
        };
    }

    public static void react(NovaEntity nova, Intent intent) {
        if (nova.companion().panicking() || nova.isCarryingChest() || nova.isPerformingAction()
                || nova.getWorkAction() != NovaEntity.WORK_NONE) return;
        switch (intent) {
            case HELLO, THANKS, APOLOGY -> { nova.showNovaFace(0, 40); nova.companion().gesture("nod"); }
            case PRAISE, EXPERIENCE -> { nova.showNovaFace(5, 45); nova.companion().gesture("proud"); }
            case COMFORT, SMALLTALK -> { nova.showNovaFace(1, 40); nova.companion().gesture("nod"); }
            case EQUIPMENT, CHEST, ENVIRONMENT, STATUS -> nova.companion().gesture("inspect");
            default -> { }
        }
    }
}
