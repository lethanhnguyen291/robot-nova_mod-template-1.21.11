package net.teogemini.nova.entity.ai.work;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.teogemini.nova.entity.NovaEntity;
import net.teogemini.nova.entity.NovaWorkSupport;
import net.teogemini.nova.registry.ModItems;

public class NovaSwordCombatGoal extends Goal {
    private final NovaEntity nova;
    private final double speed;
    private LivingEntity target;
    private int ticks;
    private int attackCooldown;
    private boolean useOffhandNext;
    private long lastAlertTime;

    public NovaSwordCombatGoal(NovaEntity nova, double speed) {
        this.nova = nova;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public boolean canUse() {
        if (!nova.isSwordNova()) return false;
        if (!nova.isAlive() || nova.isNoAi() || !nova.isTame()) return false;
        if (nova.isOrderedToSit() || nova.isRallying() || nova.work().inventoryOpen()) return false;
        if (nova.work().energy() <= 0) return false;

        // Kiểm tra vũ khí
        if (!checkWeapons()) {
            return false;
        }

        // Tìm mục tiêu theo thứ tự ưu tiên: Bảo vệ chủ nhân -> Tự vệ -> Tuần tra diệt quái -> Đi săn
        this.target = findTarget();
        return this.target != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!nova.isSwordNova() || !nova.isAlive() || nova.isNoAi() || !nova.isTame()) return false;
        if (nova.isOrderedToSit() || nova.isRallying() || nova.work().inventoryOpen()) return false;
        if (this.target == null || !this.target.isAlive() || !isValidTarget(this.target, nova.getOwner())) return false;
        if (!checkWeapons()) return false;

        LivingEntity anchor = nova.getOwner() != null ? nova.getOwner() : nova;
        return anchor.distanceToSqr(this.target) <= 32.0 * 32.0;
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.attackCooldown = 0;
        this.useOffhandNext = false;
        this.nova.setNovaRunning(true);
        if (this.target != null) {
            this.nova.getNavigation().moveTo(this.target, speed);
        }
    }

    @Override
    public void tick() {
        if (!checkWeapons()) {
            stop();
            return;
        }

        if (target == null || !target.isAlive() || !isValidTarget(target, nova.getOwner())) {
            if (target != null && !target.isAlive()) {
                nova.addExperience(3);
                NovaWorkSupport.collectNearby(nova);
            }
            target = findTarget();
            if (target == null) {
                stop();
                return;
            }
        }

        nova.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double distSqr = nova.distanceToSqr(target);
        double reachSqr = getAttackReachSqr(target);

        ticks++;
        if (attackCooldown > 0) attackCooldown--;

        if (distSqr <= reachSqr) {
            nova.getNavigation().stop();
            if (attackCooldown <= 0) {
                performAttack();
            }
        } else {
            if (ticks % 10 == 0 || nova.getNavigation().isDone()) {
                nova.getNavigation().moveTo(target, speed);
            }
        }
    }

    @Override
    public void stop() {
        this.nova.clearWorkAnimation();
        this.nova.setNovaRunning(false);
        this.nova.getNavigation().stop();
        this.target = null;
        this.attackCooldown = 0;
    }

    private void performAttack() {
        boolean hasOffhandSword = nova.getOffhandItem().is(ItemTags.SWORDS)
                && nova.companion().usableTool(nova.getOffhandItem());

        InteractionHand hand = (hasOffhandSword && useOffhandNext) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        useOffhandNext = !useOffhandNext;
        attackCooldown = hasOffhandSword ? 10 : 16; // Cầm 2 kiếm chém nhanh hơn!

        nova.swing(hand);
        nova.playWorkAnimation(NovaEntity.WORK_CHOP);

        ItemStack weapon = hand == InteractionHand.MAIN_HAND ? nova.getMainHandItem() : nova.getOffhandItem();
        float damage = calculateSwordDamage(weapon);

        if (nova.level() instanceof ServerLevel serverLevel) {
            boolean hit = target.hurtServer(serverLevel, nova.damageSources().mobAttack(nova), damage);
            if (hit) {
                target.knockback(0.35, nova.getX() - target.getX(), nova.getZ() - target.getZ());
                nova.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 1.0F);
                if (!weapon.isEmpty()) {
                    EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                    weapon.hurtAndBreak(1, nova, slot);
                }
                nova.work().tickEnergy(true);
                if (!target.isAlive()) {
                    nova.addExperience(3);
                    NovaWorkSupport.collectNearby(nova);
                    target = null;
                }
            }
        }
    }

    private float calculateSwordDamage(ItemStack sword) {
        float base = 4.0F;
        if (sword.isEmpty()) return base;
        if (sword.is(Items.WOODEN_SWORD) || sword.is(Items.GOLDEN_SWORD)) base += 4.0F;
        else if (sword.is(Items.STONE_SWORD)) base += 5.0F;
        else if (sword.is(Items.IRON_SWORD)) base += 6.0F;
        else if (sword.is(Items.DIAMOND_SWORD)) base += 7.0F;
        else if (sword.is(Items.NETHERITE_SWORD) || sword.is(ModItems.NOVA_SWORD)) base += 8.0F;
        else base += 6.0F;
        return base;
    }

    private double getAttackReachSqr(LivingEntity target) {
        double d = nova.getBbWidth() * 2.0F + target.getBbWidth();
        return Math.max(d * d, 4.5);
    }

    private boolean checkWeapons() {
        if (!NovaWorkSupport.hasAnyUsableSword(nova)) {
            LivingEntity owner = nova.getOwner();
            if (owner != null) {
                nova.getNavigation().moveTo(owner, 1.3);
                if (owner instanceof Player player && nova.level().getGameTime() - lastAlertTime > 200) {
                    lastAlertTime = nova.level().getGameTime();
                    NovaWorkSupport.message(nova, player, "Mình bị hư hết vũ khí rồi, cấp vũ khí cho mình đi!");
                }
            }
            return false;
        }

        if (!nova.companion().usableTool(nova.getMainHandItem())) {
            NovaWorkSupport.swapToolFromBody(nova);
        }
        NovaWorkSupport.ensureOffhandSword(nova);
        return true;
    }

    private LivingEntity findTarget() {
        LivingEntity owner = nova.getOwner();

        // 1. BẢO VỆ CHỦ NHÂN (Ưu tiên cao nhất - cơ chế giống chó sói trong Minecraft)
        if (owner != null && owner.isAlive()) {
            LivingEntity hurtBy = owner.getLastHurtByMob();
            if (isValidTarget(hurtBy, owner)) return hurtBy;

            LivingEntity hurt = owner.getLastHurtMob();
            if (isValidTarget(hurt, owner)) return hurt;
        }

        // 2. TỰ VỆ (Kẻ vừa tấn công Nova)
        LivingEntity selfHurtBy = nova.getLastHurtByMob();
        if (isValidTarget(selfHurtBy, owner)) return selfHurtBy;

        // 3. TUẦN TRA & DIỆT QUÁI (Enemy / Monster trong phạm vi 16 khối)
        AABB patrolBox = nova.getBoundingBox().inflate(16.0, 8.0, 16.0);
        List<LivingEntity> monsters = nova.level().getEntitiesOfClass(
                LivingEntity.class, patrolBox,
                e -> (e instanceof Enemy) && isValidTarget(e, owner)
        );
        if (!monsters.isEmpty()) {
            LivingEntity closest = null;
            double closestDist = Double.MAX_VALUE;
            for (LivingEntity m : monsters) {
                double d = nova.distanceToSqr(m);
                if (d < closestDist) {
                    closestDist = d;
                    closest = m;
                }
            }
            if (closest != null) return closest;
        }

        // 4. ĐI SĂN (Săn động vật hoang dã khi không có quái vật gần đó)
        AABB huntBox = nova.getBoundingBox().inflate(12.0, 6.0, 12.0);
        List<Animal> animals = nova.level().getEntitiesOfClass(
                Animal.class, huntBox,
                e -> isHuntableAnimal(e) && isValidTarget(e, owner)
        );
        if (!animals.isEmpty()) {
            Animal closestAnimal = null;
            double closestDist = Double.MAX_VALUE;
            for (Animal a : animals) {
                double d = nova.distanceToSqr(a);
                if (d < closestDist) {
                    closestDist = d;
                    closestAnimal = a;
                }
            }
            return closestAnimal;
        }

        return null;
    }

    private boolean isValidTarget(LivingEntity e, LivingEntity owner) {
        if (e == null || !e.isAlive() || e.isSpectator()) return false;
        if (e == nova || e == owner) return false;
        if (e instanceof Player) return false; // Không bao giờ đánh người chơi
        if (e instanceof NovaEntity) return false; // Không bao giờ đánh Nova khác
        if (e instanceof TamableAnimal tamed && tamed.isTame()) {
            if (owner != null && tamed.isOwnedBy(owner)) return false; // Không đánh pet của chủ nhân
        }
        if (e instanceof OwnableEntity ownable && ownable.getOwner() != null) {
            if (owner != null && ownable.getOwner() == owner) return false;
        }

        LivingEntity anchor = owner != null ? owner : nova;
        if (anchor.distanceToSqr(e) > 32.0 * 32.0) return false;
        return nova.level().getWorldBorder().isWithinBounds(e.blockPosition());
    }

    private boolean isHuntableAnimal(LivingEntity e) {
        if (!(e instanceof Animal animal)) return false;
        if (animal.isBaby()) return false; // Bảo vệ con non
        if (animal instanceof TamableAnimal tamed && tamed.isTame()) return false;
        if (animal instanceof OwnableEntity ownable && ownable.getOwner() != null) return false;
        return true;
    }
}
