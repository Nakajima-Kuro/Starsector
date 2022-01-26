package data.missions.swp_duelofthecentury;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.characters.FullName.Gender;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.dark.shaders.light.LightShader;
import org.dark.shaders.light.StandardLight;
import org.dark.shaders.post.PostProcessShader;
import org.lwjgl.util.vector.Vector3f;

public class MissionDefinition implements MissionDefinitionPlugin {

    public static final List<String[]> ENEMIES = new ArrayList<>();
    public static final String PLAYER_VARIANT = "swp_excelsior_pro";

    public static boolean HARD_MODE = false;
    public static int RANDOM_COUNT = -1;    // 10;
    protected boolean campaignMode;    // set to true if mission is being created from an arcade machine in campaign

    /*
		Team changes to consider:
		- give Shamash and Versant the boot for being too weak now (7 and 8 DP respectively)
		- add Seeker's Endymion (though it's pretty weak, being based off old Hyperion)
     */
    static {
        // name, required mod's ID, variant, fallback variant
        ENEMIES.add(new String[]{"Mozart", null, "hyperion_Strike", null});
        ENEMIES.add(new String[]{"Tartini", "exigency", "exigency_indra_Standard", "hyperion_Attack"});
        ENEMIES.add(new String[]{"Beethoven", "underworld", "uw_venomx_eli", "swp_hyperion_flamer"});
        ENEMIES.add(new String[]{"Vivaldi", "blackrock_driveyards", "brdy_imaginos_shock", "swp_hyperion_shocker"});
        ENEMIES.add(new String[]{"Mendelssohn", "shadow_ships", "ms_shamash_EMP", "swp_hyperion_assault"});
        ENEMIES.add(new String[]{"Paganini", "Templars", "tem_jesuit_est", "swp_hyperion_blaster"});
        ENEMIES.add(new String[]{"Dvorak", "Imperium", "ii_maximus_str", "swp_hyperion_nullifier"});
        ENEMIES.add(new String[]{"Saint-Saens", "SCY", "SCY_stymphalianbird_combat", "swp_hyperion_assassin"});
        ENEMIES.add(new String[]{"Haydn", "diableavionics", "diableavionics_versant_standard", "swp_hyperion_meltdowner"});
        ENEMIES.add(new String[]{"Rachmaninoff", "ORA", "ora_ascension_control", "swp_hyperion_berserker"});
        ENEMIES.add(new String[]{"Debussy", "timid_xiv", "eis_valorous_standard", "swp_hyperion_stunner"});
    }

    public void addEnemyShips(MissionDefinitionAPI api) {
        List<String[]> enemies = new ArrayList<>();

        if (RANDOM_COUNT <= 0 || RANDOM_COUNT >= ENEMIES.size()) {
            enemies.addAll(ENEMIES);
        } else {
            WeightedRandomPicker<String[]> picker = new WeightedRandomPicker<>();
            WeightedRandomPicker<String[]> pickerFallback = new WeightedRandomPicker<>();

            // pick random ships up to the specified number, prioritizing those for which we have the mod
            for (String[] candidate : ENEMIES) {
                String modId = candidate[1];

                if (modId != null && !Global.getSettings().getModManager().isModEnabled(modId)) {
                    pickerFallback.add(candidate);
                } else {
                    picker.add(candidate);
                }
            }

            for (int i = 0; i < RANDOM_COUNT; i++) {
                enemies.add(!picker.isEmpty() ? picker.pickAndRemove() : pickerFallback.pickAndRemove());
            }
            // TODO: maybe make an actual data object so we don't have to get the index just to sort
            Collections.sort(enemies, new Comparator<String[]>() {
                @Override
                public int compare(String[] arg0, String[] arg1) {
                    return Integer.compare(ENEMIES.indexOf(arg0), ENEMIES.indexOf(arg1));
                }
            });
        }

        for (String[] entry : enemies) {
            String name = entry[0];
            String modId = entry[1];
            String variantId = entry[2];
            String fallback = entry[3];

            if (modId != null && !Global.getSettings().getModManager().isModEnabled(modId)) {
                variantId = fallback;
            }

            addEnemyShip(api, variantId, name);
        }
    }

    public void addEnemyShip(MissionDefinitionAPI api, String variantId, String name) {
        FleetMemberAPI member = api.addToFleet(FleetSide.ENEMY, variantId, FleetMemberType.SHIP, "Silent " + name, false);

        /* Way too hard otherwise */
        if (HARD_MODE) {
            PersonAPI officer = OfficerManagerEvent.createOfficer(Global.getSector().getFaction(Factions.TRITACHYON), 5, FleetFactoryV3.getSkillPrefForShip(member), true, null, true, true, 1, new Random());
            switch (officer.getPersonalityAPI().getId()) {
                case "timid":
                    officer.setPersonality("steady");
                    break;
                case "cautious":
                    officer.setPersonality("aggressive");
                    break;
                default:
                    officer.setPersonality("reckless");
                    break;
            }
            member.setCaptain(officer);
            float maxCR = member.getRepairTracker().getMaxCR();
            member.getRepairTracker().setCR(maxCR);
        }
    }

    @Override
    public void defineMission(MissionDefinitionAPI api) {
        api.initFleet(FleetSide.PLAYER, "TTS", FleetGoal.ATTACK, false);
        api.initFleet(FleetSide.ENEMY, "ISS", FleetGoal.ATTACK, true);

        api.setFleetTagline(FleetSide.PLAYER, "Captain Lee's stolen Tri-Tachyon prototype");
        api.setFleetTagline(FleetSide.ENEMY, "\"The Silent Hand\" elite mercenary squad");

        api.setHyperspaceMode(true);

        api.addBriefingItem("Defeat \"The Silent Hand\".");
        api.addBriefingItem("Captain Lee's skills are impeccable; the Excelsior is particularly effective in battle.");
        api.addBriefingItem("Flux will decrease more slowly over time due to flux synchonicity from the nearby star.");

        if (!campaignMode) {
            FleetMemberAPI member = api.addToFleet(FleetSide.PLAYER, PLAYER_VARIANT, FleetMemberType.SHIP, "TTS Excelsior", true);

            FactionAPI pirates = Global.getSettings().createBaseFaction(Factions.PIRATES);
            PersonAPI officer = pirates.createRandomPerson(Gender.MALE);
            officer.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
            officer.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 1);
            officer.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
            officer.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
            officer.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
            officer.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
            officer.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 1);
            officer.getStats().setSkillLevel(Skills.WOLFPACK_TACTICS, 1);
            officer.getStats().setSkillLevel(Skills.PHASE_CORPS, 1);
            officer.getStats().setSkillLevel(Skills.TACTICAL_DRILLS, 1);
            officer.getStats().setLevel(7);
            officer.setFaction(Factions.PIRATES);
            officer.setPersonality(Personalities.RECKLESS);
            officer.getName().setFirst("Mann");
            officer.getName().setLast("Lee");
            officer.getName().setGender(Gender.MALE);
            officer.setPortraitSprite("graphics/swp/portraits/swp_lee.png");
            member.setCaptain(officer);
            float maxCR = member.getRepairTracker().getMaxCR();
            member.getRepairTracker().setCR(maxCR);

            addEnemyShips(api);
        }

        float width = 10000f;
        float height = 10000f;
        api.initMap(-width / 2f, width / 2f, -height / 2f, height / 2f);

        api.addAsteroidField(0f, 0f, (float) Math.random() * 360f, width, 30f, 200f, 500);
        api.addPlugin(new Plugin());
    }

    public void setCampaignMode(boolean mode) {
        campaignMode = mode;
    }

    public final static class Plugin extends BaseEveryFrameCombatPlugin {

        private boolean reallyStarted = false;
        private boolean started = false;

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (!started) {
                started = true;
                return;
            }
            if (!reallyStarted) {
                reallyStarted = true;

                StandardLight sun = new StandardLight();
                sun.setType(3);
                sun.setDirection((Vector3f) (new Vector3f(-1f, -1f, -0.2f)).normalise());
                sun.setIntensity(0f);
                sun.setSpecularIntensity(2f);
                sun.setColor(new Color(212, 91, 22));
                sun.makePermanent();
                LightShader.addLight(sun);

                sun = new StandardLight();
                sun.setType(3);
                sun.setDirection((Vector3f) (new Vector3f(0f, 0f, -1f)).normalise());
                sun.setIntensity(0.75f);
                sun.setSpecularIntensity(0f);
                sun.setColor(new Color(212, 91, 22));
                sun.makePermanent();
                LightShader.addLight(sun);

                PostProcessShader.setSaturation(false, 1.1f);
                PostProcessShader.setLightness(false, 0.9f);
                PostProcessShader.setContrast(false, 1.1f);
                PostProcessShader.setNoise(false, 0.1f);
            }

            if (Global.getCombatEngine().isPaused()) {
                return;
            }

            for (ShipAPI ship : Global.getCombatEngine().getShips()) {
                if (ship.getHullSpec().getHullId().startsWith("swp_excelsior")) {
                    ship.getFluxTracker().increaseFlux(30f * amount, true);
                }
            }
        }

        @Override
        public void init(CombatEngineAPI engine) {
        }
    };
}
