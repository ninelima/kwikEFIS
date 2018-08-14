/*
 * Copyright (C) 2016 Player One
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package player.efis.mfd;

import java.util.Iterator;
import player.efis.common.AirspaceClass;
import player.efis.common.DemColor;
import player.efis.common.DemGTOPO30;
import player.efis.common.AircraftData;
import player.efis.common.EFISRenderer;
import player.efis.common.OpenAir;
import player.efis.common.OpenAirPoint;
import player.efis.common.OpenAirRec;
import player.efis.common.Point;
import player.gles20.Line;
import player.gles20.PolyLine;
import player.gles20.Polygon;
import player.gles20.Square;
import player.gles20.Triangle;
import player.ulib.*;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import player.gles20.GLText;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

public class MFDRenderer extends EFISRenderer implements GLSurfaceView.Renderer
{
    private static final String TAG = "MFDRenderer";
    public MFDRenderer(Context context)
    {
        super(context);
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config)
    {
        // Set the background frame color
        GLES20.glClearColor(backShadeR, backShadeG, backShadeB, 1.0f);

        mTriangle = new Triangle();
        mSquare = new Square();
        mLine = new Line();
        mPolyLine = new PolyLine();
        mPolygon = new Polygon();

        // Create the GLText
        glText = new GLText(context.getAssets());
        roseTextScale = 2f;
    }

    @Override
    public void onDrawFrame(GL10 gl)
    {
        // Draw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Set the camera position (View matrix)
        if (displayMirror)
            Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);  // Mirrored View
        else
            Matrix.setLookAtM(mViewMatrix, 0, 0, 0, +3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);   // Normal View

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

        zfloat = 0;

        if (displayDEM && !fatFingerActive) renderDEMTerrain(mMVPMatrix);  // fatFingerActive just for performance

        // Remote Magnetic Inidicator - RMI
        if (displayRMI) {
            float xlx;
            float xly;

            // Add switch for orientation
            if (Layout == layout_t.LANDSCAPE) {
                // Landscape
                xlx = 0;               // top left
                xly = -1.80f * pixH2;  // top left
                roseScale = 1.9f;
                GLES20.glViewport(0, pixH2, pixW, pixH);
            }
            else {
                //Portrait
                xlx = 0;
                xly = -0.20f * pixH2;  
                roseScale = 1.9f;
            }

            Matrix.translateM(mMVPMatrix, 0, xlx, xly, 0);
            // Create a rotation for the RMI
            Matrix.setRotateM(mRmiRotationMatrix, 0, DIValue, 0, 0, 1);  // compass rose rotation
            Matrix.multiplyMM(rmiMatrix, 0, mMVPMatrix, 0, mRmiRotationMatrix, 0);
            renderFixedCompassMarkers(mMVPMatrix);
            Matrix.translateM(mMVPMatrix, 0, -xlx, -xly, 0);

            renderCompassRose(rmiMatrix);
            GLES20.glViewport(0, 0, pixW, pixH);  // fullscreen
        }

        if (displayAirspace) renderAirspace(mMVPMatrix);
        //if (displayAirport) renderAPT(mMVPMatrix);  // must be on the same matrix as the Pitch
        //if (displayAirport) renderACT(mMVPMatrix);  // must be on the same matrix as the Pitch

        //-----------------------------
        if (displayFlightDirector) {
            if (autoZoomActive) setAutoZoom();
            renderDctTrack(mMVPMatrix);
            renderAutoWptDetails(mMVPMatrix);
        }
        renderMapScale(mMVPMatrix);  // do before the DI

        if (displayTape == true) {
            float xlx;
            float xly;

            //if (displayTape == true) renderFixedVSIMarkers(mMVPMatrix); // todo: maybe later
			
            xlx = 0.99f * pixW2;
            xly = -0.3f * pixM2;
			
            Matrix.translateM(mMVPMatrix, 0, xlx, 0, 0);
            renderFixedALTMarkers(mMVPMatrix);
            Matrix.translateM(mMVPMatrix, 0, 0, xly, 0);
            renderFixedRADALTMarkers(mMVPMatrix); // AGL
            Matrix.translateM(mMVPMatrix, 0, -xlx, -xly, 0);


            xlx = -0.99f * pixW2;
            Matrix.translateM(mMVPMatrix, 0, xlx, 0, 0);
            renderFixedASIMarkers(mMVPMatrix);
            Matrix.translateM(mMVPMatrix, 0, -xlx, -0, 0);
			
            renderFixedDIMarkers(mMVPMatrix);
            renderHDGValue(mMVPMatrix);
        }

        //-----------------------------
        if (displayInfoPage) {
            renderAncillaryDetails(mMVPMatrix);
            renderBatteryPct(mMVPMatrix);

            // North Que
            {
                float xlx = -0.84f * pixW2;
                float xly = +0.88f * pixH2;

                Matrix.translateM(mMVPMatrix, 0, xlx, xly, 0);
                Matrix.setRotateM(mRmiRotationMatrix, 0, DIValue, 0, 0, 1);  // compass rose rotation
                Matrix.multiplyMM(rmiMatrix, 0, mMVPMatrix, 0, mRmiRotationMatrix, 0);
                Matrix.translateM(mMVPMatrix, 0, -xlx, -xly, 0);
                renderNorthQue(rmiMatrix);
            }
        }
        if (bCalibrating) renderCalibrate(mMVPMatrix);
        if (bSimulatorActive) renderSimulatorActive(mMVPMatrix);

        renderACSymbol(mMVPMatrix);

        // Do this last so that every else wil be dimmed for fatfinger entry
        if (displayFlightDirector) {
            renderSelWptDetails(mMVPMatrix);
            renderSelWptValue(mMVPMatrix);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height)
    {
        // Adjust the viewport based on geometry changes, such as screen rotation
        GLES20.glViewport(0, 0, width, height);

        // this projection matrix is applied to  object coordinates in the onDrawFrame() method
        //b2 Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
        //Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 2, 7); // - this apparently fixed for the Samsung S2?

        //b2 start
        // Capture the window scaling for use by the rendering functions
        pixW = width;
        pixH = height;
        pixW2 = pixW / 2;
        pixH2 = pixH / 2;

        if (pixW < pixH) pixM = pixW;
        else pixM = pixH;

        // because the ascpect ratio is different in landscape and portrait (due to menu bar)
        // we just fudge it as 85% throughout,  looks OK in landscape as well
        pixM = pixM * 88 / 100;
        pixM2 = pixM / 2;

        setSpinnerParams(); // Set up the spinner locations and SelWpt display

        // Set the window size specific scales, positions and sizes (nothing dynamic yet...)
        pitchInView = 25.0f;     // degrees to display from horizon to top of viewport
        IASInView = 40.0f;       // IAS units to display from center to top of viewport
        MSLInView = 300.0f;      // IAS units to display from center to top of viewport

        // this projection matrix is applied to  object coordinates in the onDrawFrame() method
        float ratio = (float) width / height;
        //Matrix.frustumM(mProjectionMatrix, 0, -ratio*pixH2, ratio*pixH2, -pixH2, pixH2, 3f, 7f); // all the rest
        Matrix.frustumM(mProjectionMatrix, 0, -ratio * pixH2, ratio * pixH2, -pixH2, pixH2, 2.99f, 75f); //hack for Samsung G2

        // Create the GLText
        // --debug glText = new GLText(context.getAssets()); - moved to onsurfacecreated

        // Load the font from file (set size + padding), creates the texture
        // NOTE: after a successful call to this the font is ready for rendering!
        // glText.load( "Roboto-Regular.ttf", 14, 2, 2 );  // Create Font (Height: 14 Pixels / X+Y Padding 2 Pixels)
        glText.load("square721_cn_bt_roman.ttf", pixM * 14 / 734, 2, 2);  // Create Font (Height: 14 Pixels / X+Y Padding 2 Pixels)

        // enable texture + alpha blending
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }


    @Override
    protected Point project(float relbrg, float dme)
    {
        return new Point(
                mMapZoom * dme * UTrig.icos(90-(int)relbrg),
                mMapZoom * dme * UTrig.isin(90-(int)relbrg)
        );
    } // end of project

    @Override
    protected Point project(float relbrg, float dme, float elev)
    {
        return new Point(
                mMapZoom * dme * UTrig.icos(90-(int)relbrg),
                mMapZoom * dme * UTrig.isin(90-(int)relbrg)
        );
    } // end of project


/*    {
        float pixPerDegree = pixM / pitchInView;

        // note: we take apt elevation into account
        //float y1 = (float) (-Math.toDegrees(UTrig.fastArcTan2(MSLValue - z1 * 3.28084f, dme_ft)) * pixPerDegree);

        float dme_ft = dme * 6080;
        float y = (float) (-Math.toDegrees(UTrig.fastArcTan2(MSLValue - elev * 3.28084f, dme_ft)) * pixPerDegree);

        //relbrg = 0; //bugbug

        return new Point(
                (float) (+pixPerDegree * relbrg),
                (float) y
        );
    } // end of project
*/


    //-------------------------------------------------------------------------
    // Set the spinner control parameters
    //
    public void setSpinnerParams()
    {
        // This code determines where the spinner control
        // elements are displayed. Used by WPT and ALT
        if (Layout == layout_t.LANDSCAPE) {
            // Landscape --------------
            lineAutoWptDetails =   +0.50f;
            lineAncillaryDetails = -0.30f;

            if (fatFingerActive) {
                selWptDec = 0.75f * pixH2;
                selWptInc = 0.45f * pixH2;
                selAltDec = -0.45f * pixH2;
                selAltInc = -0.75f * pixH2;

                lineC = 0.2f;
                leftC = -0.55f;
                spinnerStep = 0.25f;
                spinnerTextScale = 2.0f;
            }
            else {
                // Top
                selWptDec = 0.90f * pixH2;
                selWptInc = 0.74f * pixH2;
                selAltDec = -0.74f * pixH2;
                selAltInc = -0.90f * pixH2;

                lineC = 0.50f;
                leftC = 0.6f;
                spinnerStep = 0.1f;
                spinnerTextScale = 1f;
            }
        }
        else {
            // Portrait ---------------
            lineAutoWptDetails = -0.60f;
            lineAncillaryDetails = -0.85f;

            if (fatFingerActive) {
                selWptDec = 0.7f * pixH2;
                selWptInc = 0.4f * pixH2;
                selAltDec = -0.4f * pixH2;
                selAltInc = -0.7f * pixH2;

                lineC = 0.15f;
                leftC = -0.75f;
                spinnerStep = 0.5f;
                spinnerTextScale = 2f;
            }
            else {
                selWptDec = -0.60f * pixH2; 
                selWptInc = -0.71f * pixH2; 
                selAltDec = -0.80f * pixH2;
                selAltInc = -0.91f * pixH2;

                lineC = -0.82f; 
                leftC = 0.6f;
                spinnerStep = 0.1f;
                spinnerTextScale = 1f;
            }
        }
    }

    //-------------------------------------------------------------------------
    // Render the Digital Elevation Model (DEM).
    //
    // This is the meat and potatoes of the synthetic vision implementation
    // The loops are very performance intensive, therefore all the hardcoded
    // magic numbers
    //
    protected void renderDEMTerrain(float[] matrix)
    {
        float z, x1, y1, z1;
        float lat, lon;
        z = zfloat;

        float dme;             //in nm
        float step = 0.50f;    //in nm, normally this should be = gridy
        float agl_ft;          //in Feet

        float demRelBrg;       // = DIValue + Math.toDegrees(Math.atan2(deltaLon, deltaLat));
        float caution;
        final float cautionMin = 0.2f;
        final float IASValueThreshold = AircraftData.Vx; //1.5f * Vs0;

        float m = 1;  // 1 = normal
        if (mMapZoom < 10) m = 2;
        if (mMapZoom < 5) m = 4;

        for (dme = 0; dme <= 700f / mMapZoom; dme = dme + m*step) { // DEM_HORIZON=20, was 30
            float _x1=0, _y1=0;
            for (demRelBrg = -180; demRelBrg <= 180; demRelBrg = demRelBrg + 2*m*step) { //1
                lat = LatValue + dme / 60 * UTrig.icos((int) (DIValue + demRelBrg));
                lon = LonValue + dme / 60 * UTrig.isin((int) (DIValue + demRelBrg));
                z1 = DemGTOPO30.getElev(lat, lon);

                x1 = mMapZoom * (dme * UTrig.icos(90-(int)demRelBrg));
                y1 = mMapZoom * (dme * UTrig.isin(90-(int)demRelBrg));
                if ((_x1 != 0) || (_y1 != 0)) {
                    // float wid = mMapZoom * ((1.4148f*m*step) * UTrig.isin(90 - 0)); // simplified below
                    float wid = mMapZoom * ((1.4148f*m*step)); // simplified version, sin(90) = 1
                    DemColor color = DemGTOPO30.getColor((short) z1);
                    // Handle Monochrome
                    if (colorTheme == 2) {
                        color.red = 0;
                        color.blue = 0;
                    }
                    caution = cautionMin + (color.red + color.green + color.blue);
                    agl_ft = MSLValue - z1 * 3.28084f;  // in ft
                    if (agl_ft > 1000) mLine.SetColor(color.red, color.green, color.blue, 1);                     // Enroute
                    else if (IASValue < IASValueThreshold) mLine.SetColor(color.red, color.green, color.blue, 1); // Taxi or  approach
                    else if (agl_ft > 200) mLine.SetColor(caution, caution, 0, 1f);  // Proximity notification
                    else mLine.SetColor(caution, 0, 0, 1f);                          // Proximity warning

                    mLine.SetWidth(wid);
                    mLine.SetVerts(
                            _x1, _y1, z,
                             x1,  y1, z
                    );
                    mLine.draw(matrix);
                }
                _x1 = x1;
                _y1 = y1;
            }
        }
    }

    //-------------------------------------------------------------------------
    // Airspace
    //
    protected void renderAirspace(float[] matrix)
    {
        float z, pixPerDegree;
        float x1, y1;
        float _x1, _y1;

        z = zfloat;

        // 0.16667 deg lat  = 10 nm
        // 0.1 approx 5nm
        float dme = 0;           // =  60 * 6080 * Math.hypot(deltaLon, deltaLat);  // ft
        float _dme = 6080000;    // 1,000 nm in ft
        float airspacepntRelBrg; // = DIValue + Math.toDegrees(Math.atan2(deltaLon, deltaLat));
        DemColor color;

        nrAirspaceFound = 0;
        Iterator<OpenAirRec> it = OpenAir.airspacelst.iterator();
        while (it.hasNext()) {
            OpenAirRec currAirspace;
            try {
                currAirspace = it.next();
            }
            //catch (ConcurrentModificationException e) {
            catch (Exception e) {
                break;
            }
            _x1 = 0; _y1 = 0;
            String airspaceDesc = String.format("%s LL FL%d", currAirspace.ac, currAirspace.al);

            // Set the individual airspace colors
            if      (currAirspace.ac.equals("A") && AirspaceClass.A) color = new DemColor(0.37f, 0.62f, 0.42f); // ?
            else if (currAirspace.ac.equals("B") && AirspaceClass.B) color = new DemColor(0.37f, 0.42f, 0.62f); // Dk mod Powder blue 0.6
            else if (currAirspace.ac.equals("C") && AirspaceClass.C) color = new DemColor(0.37f, 0.42f, 0.62f); // Dk mod Powder blue 0.6
            else if (currAirspace.ac.equals("P") && AirspaceClass.P) color = new DemColor(0.45f, 0.20f, 0.20f);
            else if (currAirspace.ac.equals("R") && AirspaceClass.R) color = new DemColor(0.45f, 0.20f, 0.20f);
            else if (currAirspace.ac.equals("Q") && AirspaceClass.Q) color = new DemColor(0.25f, 0.10f, 0.10f);
            else if (currAirspace.ac.equals("CTR") && AirspaceClass.CTR) color = new DemColor(0.4f, 0.4f, 0.4f); // grey
            else continue; //color = new DemColor(0.4f, 0.4f, 0.4f);

            // Handle Monochrome
            if (colorTheme == 2) {
                color.red = 0;
                color.blue = 0;
            }

            Iterator<OpenAirPoint> it2 = currAirspace.pointList.iterator();
            while (it2.hasNext()) {
                OpenAirPoint currAirPoint;
                try {
                    currAirPoint = it2.next();
                }
                //catch (ConcurrentModificationException e) {
                catch (Exception e) {
                    break;
                }

                dme = UNavigation.calcDme(LatValue, LonValue, currAirPoint.lat, currAirPoint.lon); // in ft

                // Apply selection criteria
                if (dme > AptSeekRange *2)
                    break;

                airspacepntRelBrg = UNavigation.calcRelBrg(LatValue, LonValue, currAirPoint.lat, currAirPoint.lon, DIValue);
                x1 = mMapZoom * (dme * UTrig.icos(90 - (int) airspacepntRelBrg));
                y1 = mMapZoom * (dme * UTrig.isin(90 - (int) airspacepntRelBrg));

                if (_x1 != 0 || _y1 != 0) {
                    mLine.SetWidth(8);
                    mLine.SetColor(color.red, color.green, color.blue, 0.85f);
                    mLine.SetVerts(
                            _x1, _y1, z,
                            x1, y1,  z
                    );
                    mLine.draw(matrix);
                }
                else {
                    // Draw the airspace description at the first coordinate
                    glText.begin(color.red, color.green, color.blue, 0.95f, matrix);
                    glText.setScale(1.5f);
                    glText.drawCY(airspaceDesc, x1, y1 + glText.getCharHeight() / 2);
                    glText.end();
                }
                _x1 = x1;
                _y1 = y1;

                if (Math.abs(dme) < Math.abs(_dme)) {
                    // closest apt (dme)
                    float absBrg = UNavigation.calcAbsBrg(LatValue, LonValue, currAirPoint.lat, currAirPoint.lon);
                    float relBrg = UNavigation.calcRelBrg(LatValue, LonValue, currAirPoint.lat, currAirPoint.lon, DIValue);

                    setAutoWptValue(airspaceDesc);
                    setAutoWptDme(dme);
                    setAutoWptBrg(absBrg);
                    setAutoWptRelBrg(relBrg);
                    _dme = dme;
                }
            }
        }
    }
}
