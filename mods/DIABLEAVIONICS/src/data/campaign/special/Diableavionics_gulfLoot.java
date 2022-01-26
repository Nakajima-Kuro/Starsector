package data.campaign.special;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import static data.scripts.util.Diableavionics_stringsManager.txt;
import data.scripts.util.MagicCampaign;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

/**
 * @author Tartiflette
 */

public class Diableavionics_gulfLoot implements FleetEventListener{
    
    private final String GULF_DROP_ALREADY = "$gulf_drop";
    
    @Override
    public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
        
        // ignore that whole ordeal if the Virtuous already dropped
        if(Global.getSector().getMemoryWithoutUpdate().contains(GULF_DROP_ALREADY) 
                && Global.getSector().getMemoryWithoutUpdate().getBoolean(GULF_DROP_ALREADY)){
            return;            
        }
        
        if(fleet.getFlagship()==null || !fleet.getFlagship().getHullSpec().getBaseHullId().equals("diableavionics_IBBgulf")){
            
            //remove the fleet if flag is dead
            if(!fleet.getMembersWithFightersCopy().isEmpty()){
                SectorEntityToken source = fleet.getCurrentAssignment().getTarget();
                fleet.clearAssignments();
                fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, source , 9999);
            }
            
            //boss is dead, 
            boolean salvaged=false;
            for (FleetMemberAPI f : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()){
                if(f.getHullId().equals("diableavionics_IBBgulf")) salvaged=true;
                
                //set memkey that the wreck must never spawn
                Global.getSector().getMemoryWithoutUpdate().set(GULF_DROP_ALREADY,true); 
            }
            
            //spawn a derelict if it wasn't salvaged
            if(!salvaged){
                /*
                //find a reference for the orbit
                StarSystemAPI system = fleet.getStarSystem();
                Vector2f location = fleet.getLocation();

                SectorEntityToken referenceOrbit=null;
                float closestOrbit = 99999999;
                //find nearest orbit to match
                for(SectorEntityToken e : system.getAllEntities()){
                    //skip non orpiting objects
                    if(e.getOrbit()==null)continue;
                    
                    //skip stars
                    if(e.isStar())continue;

                    //find closest point on orbit
                    Vector2f closestPoint = MathUtils.getPoint(
                            e.getOrbitFocus().getLocation(),
                            e.getCircularOrbitRadius(),
                            VectorUtils.getAngle(e.getOrbitFocus().getLocation(), location)
                    );

                    //closest orbit becomes the reference
                    if(MathUtils.getDistanceSquared(closestPoint, location)<closestOrbit){
                        referenceOrbit=e;
                    }
                }
                
                SectorEntityToken orbitCenter;
                Float angle,radius,period;
                if(referenceOrbit!=null){
                    orbitCenter=referenceOrbit.getOrbitFocus();
                    angle=VectorUtils.getAngle(referenceOrbit.getOrbitFocus().getLocation(),location);
                    radius=MathUtils.getDistance(referenceOrbit.getOrbitFocus().getLocation(),location);
                    period=referenceOrbit.getCircularOrbitPeriod()*(MathUtils.getDistance(referenceOrbit.getOrbitFocus().getLocation(),location)/referenceOrbit.getCircularOrbitRadius());                   
                } else {
                    orbitCenter=system.getCenter();
                    angle=VectorUtils.getAngle(system.getCenter().getLocation(),location);
                    radius=MathUtils.getDistance(system.getCenter(),location);
                    period=MathUtils.getDistance(system.getCenter(),location)/2;
                }
                */
                
                //make sure there is a valid location to avoid spawning in the sun
                Vector2f location = fleet.getLocation();
                if(location==new Vector2f()){
                    location = primaryWinner.getLocation();
                }
                
                //spawn the derelict object
                SectorEntityToken wreck = MagicCampaign.createDerelict(
                        "diableavionics_IBBgulf_Hull",
                        ShipRecoverySpecial.ShipCondition.WRECKED,
                        false,
                        -1,
                        true,
                        //orbitCenter,angle,radius,period);
                        fleet.getStarSystem().getCenter(),VectorUtils.getAngle(new Vector2f(), location),location.length(),360);                
                MagicCampaign.placeOnStableOrbit(wreck, true);
                wreck.setName(txt("gulfShip"));
                wreck.setFacing((float)Math.random()*360f);
                //set memkey that the wreck must never spawn
                Global.getSector().getMemoryWithoutUpdate().set(GULF_DROP_ALREADY,true); 
            }
        }
    }

    @Override
    public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, CampaignEventListener.FleetDespawnReason reason, Object param) {
        fleet.removeEventListener(this);
    }
}