package data.scripts.hullmods;

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.WeaponRangeModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import data.scripts.everyframe.SWP_BlockedHullmodDisplayScript;
import java.util.HashSet;
import java.util.Set;
import org.lwjgl.util.vector.Vector2f;

public class SWP_PDConversion extends BaseHullMod {

    public static int OP_INCREASE_SMALL = 1;
    public static int OP_INCREASE_MEDIUM = 2;
    public static int OP_INCREASE_LARGE = 4;
    public static float DAMAGE_BONUS_PERCENT = 25f;
    public static float HIT_STRENGTH_BONUS_PERCENT = 50f;
    public static float RANGE_BONUS_PERCENT = 25f;

    private static final Set<String> BLOCKED_HULLMODS = new HashSet<>(1);

    static {
        BLOCKED_HULLMODS.add("pointdefenseai");
    }

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getMod(Stats.SMALL_PD_MOD).modifyFlat(id, OP_INCREASE_SMALL);
        stats.getDynamic().getMod(Stats.MEDIUM_PD_MOD).modifyFlat(id, OP_INCREASE_MEDIUM);
        stats.getDynamic().getMod(Stats.LARGE_PD_MOD).modifyFlat(id, OP_INCREASE_LARGE);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        for (String tmp : BLOCKED_HULLMODS) {
            if (ship.getVariant().getHullMods().contains(tmp)) {
                ship.getVariant().removeMod(tmp);
                SWP_BlockedHullmodDisplayScript.showBlocked(ship);
            }
        }

        ship.addListener(new PDCDamageDealtMod());
        ship.addListener(new PDCWeaponRangeMod());
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            weapon.setPD(false);
        }
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize, ShipAPI ship) {
        if (index == 0) {
            return "" + OP_INCREASE_SMALL;
        }
        if (index == 1) {
            return "" + OP_INCREASE_MEDIUM;
        }
        if (index == 2) {
            return "" + OP_INCREASE_LARGE;
        }
        if (index == 3) {
            return "" + (int) Math.round(RANGE_BONUS_PERCENT) + "%";
        }
        if (index == 4) {
            return "" + (int) Math.round(DAMAGE_BONUS_PERCENT) + "%";
        }
        if (index == 5) {
            return "" + (int) Math.round(HIT_STRENGTH_BONUS_PERCENT) + "%";
        }
        return null;
    }

    @Override
    public boolean affectsOPCosts() {
        return true;
    }

    private boolean enoughOPToInstall(ShipAPI ship) {
        if ((ship == null) || (ship.getVariant() == null)) {
            return true;
        }
        if (ship.getVariant().hasHullMod(spec.getId())) {
            return true;
        }

        FleetMemberAPI member = ship.getFleetMember();
        MutableCharacterStatsAPI stats = null;
        if ((member != null) && (member.getFleetCommanderForStats() != null)) {
            stats = member.getFleetCommanderForStats().getFleetCommanderStats();
        }

        int unusedOP = ship.getVariant().getUnusedOP(stats);
        int opRequiredToInstall = spec.getCostFor(ship.getHullSize());
        for (String slotId : ship.getVariant().getFittedWeaponSlots()) {
            WeaponSpecAPI weaponSpec = ship.getVariant().getWeaponSpec(slotId);
            if (weaponSpec.getAIHints().contains(AIHints.PD)) {
                switch (weaponSpec.getSize()) {
                    case SMALL:
                        opRequiredToInstall += OP_INCREASE_SMALL;
                        break;
                    case MEDIUM:
                        opRequiredToInstall += OP_INCREASE_MEDIUM;
                        break;
                    case LARGE:
                        opRequiredToInstall += OP_INCREASE_LARGE;
                        break;
                    default:
                        break;
                }
            }
        }

        return unusedOP >= opRequiredToInstall;
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        if ((ship != null) && ship.getVariant().hasHullMod("pointdefenseai")) {
            return "Incompatible with Integrated Point Defense AI";
        }
        if (!enoughOPToInstall(ship)) {
            return "Insufficient ordnance points due to increased PD cost";
        }
        return super.getUnapplicableReason(ship);
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        if ((ship != null) && ship.getVariant().hasHullMod("pointdefenseai")) {
            return false;
        }
        if (!enoughOPToInstall(ship)) {
            return false;
        }
        return super.isApplicableToShip(ship);
    }

    private static class PDCDamageDealtMod implements DamageDealtModifier {

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            WeaponAPI weapon = null;
            if (param instanceof DamagingProjectileAPI) {
                weapon = ((DamagingProjectileAPI) param).getWeapon();
            } else if (param instanceof BeamAPI) {
                weapon = ((BeamAPI) param).getWeapon();
            } else if (param instanceof MissileAPI) {
                weapon = ((MissileAPI) param).getWeapon();
            }

            String id = "swp_pdc_dam_mod";
            if (weapon == null) {
                if (damage.getStats() != null) {
                    damage.getStats().getHitStrengthBonus().unmodify(id);
                }
                return null;
            }
            if (!weapon.getSpec().getAIHints().contains(AIHints.PD)) {
                if (damage.getStats() != null) {
                    damage.getStats().getHitStrengthBonus().unmodify(id);
                }
                return null;
            }

            damage.getModifier().modifyPercent(id, DAMAGE_BONUS_PERCENT);
            if (damage.getStats() != null) {
                damage.getStats().getHitStrengthBonus().modifyPercent(id, HIT_STRENGTH_BONUS_PERCENT);
            }

            return id;
        }
    }

    private static class PDCWeaponRangeMod implements WeaponRangeModifier {

        @Override
        public float getWeaponRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
            if (!weapon.getSpec().getAIHints().contains(AIHints.PD) || (weapon.getType() == WeaponType.MISSILE)) {
                return 0f;
            }
            return RANGE_BONUS_PERCENT / 100f;
        }

        @Override
        public float getWeaponRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
            return 1f;
        }

        @Override
        public float getWeaponRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
            return 0f;
        }
    }
}
