/*
 *  Copyright 2011 RoboViz
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package rv.ui.screens;

import java.awt.Font;
import java.awt.geom.Rectangle2D;
import javax.media.opengl.GL2;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;
import js.jogl.view.Viewport;
import rv.Viewer;
import rv.comm.rcssserver.GameState;
import rv.world.Team;
import rv.world.WorldModel;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.gl2.GLUT;

public class GameStateOverlay implements Screen {

    private class GameStateBar {
        private static final int barHeight     = 24;
        private static final int nameWidth     = 220;
        private static final int timeWidth     = 78;
        private static final int timepad       = 11;
        private static final int scoreBoxWidth = 56;
        private static final int ypad          = 4;
        private static final int playModeWidth = 2 * nameWidth + scoreBoxWidth + timeWidth + 6;

        private TextRenderer     tr1;
        private TextRenderer     tr2;

        private int              x, y;

        public GameStateBar(int x, int y) {
            this.x = x;
            this.y = y;
            tr1 = new TextRenderer(new Font("Arial", Font.PLAIN, 22), true, false);
            tr2 = new TextRenderer(new Font("Arial", Font.PLAIN, 16), true, false);
        }

        void render(GL2 gl, GameState gs, int screenW, int screenH) {

            String teamL = gs.getTeamLeft() == null ? "<Left>" : gs.getTeamLeft();
            String teamR = gs.getTeamRight() == null ? "<Right>" : gs.getTeamRight();

            String scoreText = gs.getScoreLeft() + ":" + gs.getScoreRight();
            String timeText = String.format("%.1f", gs.getTime());

            // truncate team names that are too long to fit within bounds
            while (tr1.getBounds(teamL).getWidth() > nameWidth - 4)
                teamL = teamL.substring(0, teamL.length() - 1);
            while (tr1.getBounds(teamR).getWidth() > nameWidth - 4)
                teamR = teamR.substring(0, teamR.length() - 1);

            double lxpad = (nameWidth - tr1.getBounds(teamL).getWidth()) / 2;
            double rxpad = (nameWidth - tr1.getBounds(teamR).getWidth()) / 2;
            double sxpad = (scoreBoxWidth - tr1.getBounds(scoreText).getWidth()) / 2;

            drawGradientBar(gl, x - 3, y - 24, playModeWidth, 24, 0.5f,
                    new float[] { 0, 0, 0, 0.5f }, new float[] { 0, 0, 0, 0 }, false);

            gl.glBegin(GL2.GL_QUADS);
            gl.glColor4f(0, 0, 0, 0.5f);
            drawBox(gl, x - 3, y - 3, 2 * nameWidth + scoreBoxWidth + timeWidth + 6, barHeight + 6);
            drawBox(gl, x - 3, y - 3, 2 * nameWidth + scoreBoxWidth + timeWidth + 6,
                    barHeight * 0.6f);
            float[] lc = viewer.getWorldModel().getLeftTeam().getTeamMaterial().getDiffuse();
            gl.glColor4f(lc[0] * 0.8f, lc[1] * 0.8f, lc[2] * 0.8f, 0.65f);
            drawBox(gl, x, y, nameWidth, barHeight);
            gl.glColor4f(0.2f, 0.2f, 0.2f, 0.65f);
            drawBox(gl, x + nameWidth, y, scoreBoxWidth, barHeight);
            gl.glColor4f(1, .3f, .3f, 0.65f);
            float[] rc = viewer.getWorldModel().getRightTeam().getTeamMaterial().getDiffuse();
            gl.glColor4f(rc[0] * 0.8f, rc[1] * 0.8f, rc[2] * 0.8f, 0.65f);
            drawBox(gl, x + nameWidth + scoreBoxWidth, y, nameWidth, barHeight);
            gl.glEnd();

            tr1.beginRendering(screenW, screenH);
            tr1.draw(teamL, (int) (x + lxpad), y + ypad);
            tr1.draw(scoreText, (int) (x + nameWidth + sxpad), y + ypad);
            tr1.draw(teamR, (int) (x + nameWidth + scoreBoxWidth + rxpad), y + ypad);
            tr1.draw(timeText, x + 2 * nameWidth + scoreBoxWidth + timepad, y + ypad);
            tr1.endRendering();

            tr2.setColor(0.9f, 0.9f, 0.9f, 1);
            tr2.beginRendering(screenW, screenH);
            tr2.draw("Playmode: " + gs.getPlayMode(), x, y - 20);
            tr2.endRendering();
        }
    }

    private Viewer       viewer;
    private TextRenderer textRenderer;
    private GameStateBar gsBar;

    public GameStateOverlay(Viewer viewer) {
        this.viewer = viewer;
        Font font = new Font("Arial", Font.PLAIN, 20);
        textRenderer = new TextRenderer(font, true, false);
        gsBar = new GameStateBar(20, 20);
    }

    public void setEnabled(GLCanvas canvas, boolean enabled) {
    }

    /**
     * Draws a rectangle with a single linear gradient
     * 
     * @param x
     *            - left coordinate of rectangle
     * @param y
     *            - bottom coordinate of rectangle
     * @param w
     *            - width in pixels
     * @param h
     *            - height in pixels
     * @param gStart
     *            - percentage of bar width to start gradient at (0 - 1)
     */
    void drawGradientBar(GL2 gl, float x, float y, float w, float h, float gStart,
            float[] startColor, float[] endColor, boolean flipVertical) {

        gStart = 1 - gStart;
        float gx = w * gStart;

        float[][] v = { { x, y }, { x + gx, y }, { x + w, y }, { x + w, y + h }, { x + gx, y + h },
                { x, y + h } };

        gl.glBegin(GL2.GL_QUADS);
        gl.glColor4fv(startColor, 0);
        if (flipVertical) {
            gl.glVertex2fv(v[4], 0);
            gl.glVertex2fv(v[1], 0);
            gl.glVertex2fv(v[2], 0);
            gl.glVertex2fv(v[3], 0);
            gl.glVertex2fv(v[1], 0);
            gl.glVertex2fv(v[4], 0);
            gl.glColor4fv(endColor, 0);
            gl.glVertex2fv(v[5], 0);
            gl.glVertex2fv(v[0], 0);
        } else {
            gl.glVertex2fv(v[5], 0);
            gl.glVertex2fv(v[0], 0);
            gl.glVertex2fv(v[1], 0);
            gl.glVertex2fv(v[4], 0);
            gl.glVertex2fv(v[4], 0);
            gl.glVertex2fv(v[1], 0);
            gl.glColor4fv(endColor, 0);
            gl.glVertex2fv(v[2], 0);
            gl.glVertex2fv(v[3], 0);
        }
        gl.glEnd();
    }

    @Override
    public void render(GL2 gl, GLU glu, GLUT glut, Viewport vp) {

        gsBar.y = vp.h - gsBar.barHeight - 20;
        gsBar.render(gl, viewer.getWorldModel().getGameState(), vp.w, vp.h);

    }

    static void drawBox(GL2 gl, float x, float y, float w, float h) {
        gl.glVertex2f(x, y);
        gl.glVertex2f(x + w, y);
        gl.glVertex2f(x + w, y + h);
        gl.glVertex2f(x, y + h);
    }
}
