//By Deathfly
package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import java.awt.Color;
import java.util.List;

//hmm, seems like I done most things in Neutrino_EveryFrameCombatPlugin so all I needs to done here is make this system can use while phasing
public class NeutrinoPhaseBombReleaseStats extends BaseShipSystemScript {

    private WeaponAPI launcher;
    private boolean fired = false;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (fired) {
            return;
        }
        ShipAPI ship = null;
//        ShipSystemAPI cloak;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
//            cloak = ship.getPhaseCloak();
//            if (cloak == null) {
//                return;
//            }
        } else {
            return;
        }
        //initialize at first time 
        if (launcher == null) {
            List<WeaponAPI> weapons = ship.getAllWeapons();
            for (WeaponAPI weapon : weapons) {
                if (weapon.getId().equals("neutrino_graviton_inverter")) {
                    launcher = weapon;
                }
            }
        }
        ship.getSystem().setAmmo(launcher.getAmmo());
//        while (launcher.getAmmo() > 0) {
        if (launcher.getAmmo() > 0) {
            Global.getCombatEngine().spawnProjectile(ship, launcher, "neutrino_graviton_inverter", ship.getLocation(), ship.getFacing(), ship.getVelocity());
            launcher.setAmmo(launcher.getAmmo() - 1);
            ship.getSystem().setAmmo(launcher.getAmmo());
        } else {
            return;
        }
//        }
        ship.setJitter(this.toString() + 1, new Color(255, 175, 255, 100), 1f, 3, 5);
        ship.setJitterUnder(this.toString() + 2, new Color(255, 175, 255, 200), 1f, 10, 20);
//        if (ship.getShipTarget() != null && ship.getShipTarget() instanceof ShipAPI) {
//            ShipAPI target = (ShipAPI) ship.getShipTarget();
//            target.setJitter(this.toString() + 1, new Color(255, 50, 100, 200), 1f, 5, 20);
//            target.setJitterUnder(this.toString() + 2, new Color(255, 50, 100, 200), 1f, 10, 20);
//        }
//        Global.getSoundPlayer().playSound("neutrino_phase_missile_firing", 0.65f, 1f, ship.getLocation(), ship.getVelocity());
        fired = true;
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        fired = false;
    }
}
