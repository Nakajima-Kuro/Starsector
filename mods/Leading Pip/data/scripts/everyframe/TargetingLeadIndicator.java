package data.scripts.everyframe;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponGroupAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.input.InputEventType;
import java.awt.Color;
import java.io.IOException;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.JSONUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

public class TargetingLeadIndicator extends BaseEveryFrameCombatPlugin {

    private static Color ALLIED_COLOR;
    private static Color ENEMY_COLOR;
    private static Color FRIENDLY_COLOR;
    private static Color HULK_COLOR;
    private static float LEAD_ALPHA;
    private static float LEAD_SIZE;
    private static float LEAD_THICKNESS;
    private static int LEAD_TOGGLE_KEY;
    private static Color NEUTRAL_COLOR;
    private static final String SETTINGS_FILE = "LEADING_PIP.ini";

    private static boolean enabled = true;

    public static Vector2f intercept(Vector2f point, float speed, Vector2f target, Vector2f targetVel) {
        Vector2f difference = new Vector2f(target.x - point.x, target.y - point.y);

        float a = targetVel.x * targetVel.x + targetVel.y * targetVel.y - speed * speed;
        float b = 2 * (targetVel.x * difference.x + targetVel.y * difference.y);
        float c = difference.x * difference.x + difference.y * difference.y;

        Vector2f solutionSet = quad(a, b, c);

        Vector2f intercept = null;
        if (solutionSet != null) {
            float bestFit = Math.min(solutionSet.x, solutionSet.y);
            if (bestFit < 0) {
                bestFit = Math.max(solutionSet.x, solutionSet.y);
            }
            if (bestFit > 0) {
                intercept = new Vector2f(target.x + targetVel.x * bestFit, target.y + targetVel.y * bestFit);
            }
        }

        return intercept;
    }

    public static Vector2f quad(float a, float b, float c) {
        Vector2f solution = null;
        if (Float.compare(Math.abs(a), 0) == 0) {
            if (Float.compare(Math.abs(b), 0) == 0) {
                solution = (Float.compare(Math.abs(c), 0) == 0) ? new Vector2f(0, 0) : null;
            } else {
                solution = new Vector2f(-c / b, -c / b);
            }
        } else {
            float d = b * b - 4 * a * c;
            if (d >= 0) {
                d = (float) Math.sqrt(d);
                float e = 2 * a;
                solution = new Vector2f((-b - d) / e, (-b + d) / e);
            }
        }
        return solution;
    }

    public static void reloadSettings() throws IOException, JSONException {
        JSONObject settings = Global.getSettings().loadJSON(SETTINGS_FILE);

        LEAD_TOGGLE_KEY = settings.getInt("toggleKey");

        final boolean vanillaColors = settings.getBoolean("useVanillaColors");
        LEAD_ALPHA = (float) settings.getDouble("indicatorAlpha");
        LEAD_SIZE = (float) settings.getDouble("indicatorSize");
        LEAD_THICKNESS = (float) settings.getDouble("indicatorThickness");
        FRIENDLY_COLOR = vanillaColors ? Global.getSettings().getColor("textFriendColor")
                : JSONUtils.toColor(settings.getJSONArray("friendlyColor"));
        ALLIED_COLOR = vanillaColors ? Global.getSettings().getColor("yellowTextColor")
                : JSONUtils.toColor(settings.getJSONArray("alliedColor"));
        ENEMY_COLOR = vanillaColors ? Global.getSettings().getColor("textEnemyColor")
                : JSONUtils.toColor(settings.getJSONArray("enemyColor"));
        NEUTRAL_COLOR = vanillaColors ? Global.getSettings().getColor("textNeutralColor")
                : JSONUtils.toColor(settings.getJSONArray("neutralColor"));
        HULK_COLOR = vanillaColors ? Global.getSettings().getColor("textGrayColor")
                : JSONUtils.toColor(settings.getJSONArray("hulkColor"));
    }

    private static void glColor(Color color, float alphaMult) {
        GL11.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(),
                (byte) (color.getAlpha() * alphaMult));
    }

    private CombatEngineAPI engine;
    private ShipAPI player;

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null || engine.getCombatUI() == null) {
            return;
        }

        for (InputEventAPI event : events) {
            if (event.isConsumed()) {
                continue;
            }

            if ((event.getEventType() == InputEventType.KEY_DOWN) && (event.getEventValue() == LEAD_TOGGLE_KEY)) {
                enabled = !enabled;
                event.consume();
                break;
            }
        }

        if (!enabled) {
            return;
        }

        if (!engine.isUIShowingHUD()) {
            return;
        }

        if (engine.getCombatUI().isShowingCommandUI()) {
            return;
        }

        player = engine.getPlayerShip();
        if (player == null || !engine.isEntityInPlay(player)) {
            return;
        }

        ShipAPI target = player.getShipTarget();

        if (target == null) {
            return;
        }

        if (player.getVariant() == null || player.getVariant().getWeaponGroups() == null
                || player.getVariant().getWeaponGroups().isEmpty()
                || player.getHullSpec().getHullId().contentEquals("shuttlepod")) {
            return;
        }

        WeaponGroupAPI group = player.getSelectedGroupAPI();
        if (group == null || group.getWeaponsCopy() == null || group.getWeaponsCopy().isEmpty()) {
            return;
        }

        float avgSpeed = 0.01f;
        Vector2f avgLoc = new Vector2f(0, 0);
        float numberOfWeapons = 0;
        for (WeaponAPI weapon : group.getWeaponsCopy()) {
            if (!weapon.isDisabled() && !weapon.isBeam() && !weapon.isBurstBeam() && weapon.getProjectileSpeed() >= 50f) {
                avgSpeed += weapon.getProjectileSpeed();
                Vector2f.add(avgLoc, weapon.getLocation(), avgLoc);
                numberOfWeapons += 1f;
            }
        }

        Vector2f loc;
        if (numberOfWeapons <= 0) {
            loc = new Vector2f(target.getLocation().x, target.getLocation().y);
        } else {
            avgSpeed /= numberOfWeapons;
            avgLoc.x /= numberOfWeapons;
            avgLoc.y /= numberOfWeapons;

            Vector2f relativeVelocity = Vector2f.sub(target.getVelocity(), player.getVelocity(), null);
            loc = intercept(avgLoc, avgSpeed, target.getLocation(), relativeVelocity);
            if (loc == null) {
                Vector2f projection = new Vector2f(target.getVelocity());
                float scalar = MathUtils.getDistance(target.getLocation(), player.getLocation()) / avgSpeed;
                projection.scale(scalar);
                Vector2f.add(target.getLocation(), projection, loc);
            }
        }

        if (loc == null) {
            return;
        }

        ViewportAPI viewport = engine.getViewport();

        float scaleFactor = LEAD_SIZE * viewport.getViewMult();

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        final int width = (int) (Display.getWidth() * Display.getPixelScaleFactor()), height
                = (int) (Display.getHeight()
                * Display.getPixelScaleFactor());
        GL11.glViewport(0, 0, width, height);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(viewport.getLLX(), viewport.getLLX() + viewport.getVisibleWidth(), viewport.getLLY(),
                viewport.getLLY() + viewport.getVisibleHeight(), -1,
                1);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glTranslatef(0.01f, 0.01f, 0);

        GL11.glLineWidth(LEAD_THICKNESS);
        if (target.isHulk()) {
            glColor(HULK_COLOR, LEAD_ALPHA * (1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity()));
        } else if (target.isAlly()) {
            glColor(ALLIED_COLOR, LEAD_ALPHA * (1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity()));
        } else if (target.getOwner() == player.getOwner()) {
            glColor(FRIENDLY_COLOR, LEAD_ALPHA * (1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity()));
        } else if (target.getOwner() + player.getOwner() == 1) {
            glColor(ENEMY_COLOR, LEAD_ALPHA * (1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity()));
        } else {
            glColor(NEUTRAL_COLOR, LEAD_ALPHA * (1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity()));
        }
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(loc.x, loc.y + scaleFactor);
        GL11.glVertex2f(loc.x + scaleFactor, loc.y);
        GL11.glVertex2f(loc.x, loc.y - scaleFactor);
        GL11.glVertex2f(loc.x - scaleFactor, loc.y);
        GL11.glEnd();

        GL11.glDisable(GL11.GL_BLEND);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();

        GL11.glPopAttrib();
    }

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
    }
}
