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

package player.efis;

import player.efis.common.DemColor;
import player.efis.common.DemGTOPO30;
import player.efis.common.AircraftData;
import player.efis.common.EFISRenderer;
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

public class Renderer extends EFISRenderer implements GLSurfaceView.Renderer
{
    private static final String TAG = "Renderer";
    protected boolean ServiceableAh;      // Flag to indicate AH failure

    public Renderer(Context context)
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
        roseTextScale = 1f;
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

        // Create a rotation for the horizon
        Matrix.setRotateM(mRotationMatrix, 0, rollRotation, 0, 0, 1.0f);

        // Combine the rotation matrix with the projection and camera view
        // Note that the mMVPMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
        Matrix.multiplyMM(scratch1, 0, mMVPMatrix, 0, mRotationMatrix, 0);
        Matrix.multiplyMM(scratch2, 0, mMVPMatrix, 0, mRotationMatrix, 0);

        // Pitch
        if (Layout == layout_t.LANDSCAPE) {
            portraitOffset = 0;
            // Slide pitch to current value
            Matrix.translateM(scratch1, 0, 0, pitchTranslation, 0); // apply the pitch
        }
        else {
            portraitOffset = 0.40f;  // the magic number for portrait offset
            // Slide pitch to current value adj for portrait
            float Adjust = pixH2 * portraitOffset;                           //portraitOffset set to 0.4
            Matrix.translateM(scratch1, 0, 0, pitchTranslation + Adjust, 0); // apply the pitch and offset
        }

        // Slide ALT to current value
        Matrix.translateM(altMatrix, 0, mMVPMatrix, 0, 0, -MSLTranslation, 0); // apply the altitude

        // Slide IAS to current value
        Matrix.translateM(iasMatrix, 0, mMVPMatrix, 0, 0, -IASTranslation, 0); // apply the altitude

        zfloat = 0;

        if (displayDEM && !fatFingerActive) {
            // Make the blue sky for the DEM.
            // Note: it extends a little below the horizon when AGL is positive
            renderDEMSky(scratch1);
            if ((AGLValue > 0) && (DemGTOPO30.demDataValid))
                renderDEMTerrain(scratch1);  // underground is not valid
        }
        else if (displayAHColors) renderAHColors(scratch1);

        renderPitchMarkers(scratch1);

        // FPV only means anything if we have speed and rate of climb, ie altitude
        if (displayFPV) renderFPV(scratch1);      // must be on the same matrix as the Pitch
        if (displayAirport) renderAPT(scratch1);  // must be on the same matrix as the Pitch
        if (true) renderTargets(scratch1);        // TODO: 2018-08-31 Add control tof targets
        if (displayHITS) renderHITS(scratch1);    // will not keep in the viewport

        // Flight Director - FD
        if (displayFlightDirector) {
            // Create a rotation for the Flight director
            Matrix.setRotateM(mFdRotationMatrix, 0, rollRotation + FDRotation, 0, 0, 1.0f);  // fd rotation
            Matrix.multiplyMM(fdMatrix, 0, mMVPMatrix, 0, mFdRotationMatrix, 0);

            if (Layout == layout_t.LANDSCAPE) {
                // Slide FD to current value
                Matrix.translateM(fdMatrix, 0, 0, pitchTranslation - FDTranslation, 0); // apply the altitude
            }
            else {
                // Slide pitch to current value adj for portrait
                float Adjust = pixH2 * portraitOffset;
                // Slide FD to current value
                Matrix.translateM(fdMatrix, 0, 0, pitchTranslation - FDTranslation + Adjust, 0); // apply the altitude
            }
            renderFlightDirector(fdMatrix);
        }

        // Remote Magnetic Inidicator - RMI
        if (displayRMI) {
            float xlx;
            float xly;

            // Add switch for orientation
            if (Layout == layout_t.LANDSCAPE) {
                // Landscape
                xlx = -0.74f * pixW2; // top left
                xly = 0.50f * pixH2;  // top left
                roseScale = 0.44f;
                GLES20.glViewport(0, 0, pixW, pixH);
            }
            else {
                //Portrait
                xlx = 0;
                xly = -0.44f * pixH2;
                roseScale = 0.52f;
            }

            Matrix.translateM(mMVPMatrix, 0, xlx, xly, 0);
            // Create a rotation for the RMI
            Matrix.setRotateM(mRmiRotationMatrix, 0, DIValue, 0, 0, 1);  // compass rose rotation
            Matrix.multiplyMM(rmiMatrix, 0, mMVPMatrix, 0, mRmiRotationMatrix, 0);
            renderBearingTxt(mMVPMatrix);
            renderFixedCompassMarkers(mMVPMatrix);
            Matrix.translateM(mMVPMatrix, 0, -xlx, -xly, 0);

            renderCompassRose(rmiMatrix);
            renderBearing(rmiMatrix);
            renderAutoWptDetails(mMVPMatrix);
            GLES20.glViewport(0, 0, pixW, pixH);  // fullscreen
        }


        if (Layout == layout_t.PORTRAIT) {
            // Slide pitch to current value adj for portrait
            int Adjust = (int) (pixH2 * portraitOffset);
            GLES20.glViewport(0,  Adjust, pixW, pixH); // Portrait //
        }
        renderFixedHorizonMarkers();
        renderRollMarkers(scratch2);

        //-----------------------------
        if (Layout == layout_t.LANDSCAPE)
            GLES20.glViewport(pixW / 30, pixH / 30, pixW - pixW / 15, pixH - pixH / 15); //Landscape
        else
            GLES20.glViewport(pixW / 100, pixH * 40 / 100, pixW - pixW / 50, pixH - pixH * 42 / 100); // Portrait

        if (displayTape) {
            renderALTMarkers(altMatrix);
            renderASIMarkers(iasMatrix);
            renderVSIMarkers(mMVPMatrix);
        }

        {
            int Adjust = (int) (pixH2 * portraitOffset);
            GLES20.glViewport(0, Adjust, pixW, pixH + Adjust); // Portrait //

            float xlx;
            float xly;

            //if (displayTape == true) renderFixedVSIMarkers(mMVPMatrix); // todo: maybe later

            xlx = 1.14f * pixM2;
            xly = -0.5f * portraitOffset * pixH2; // half of tape viewport //-0.7f * pixH2;
            Matrix.translateM(mMVPMatrix, 0, xlx, xly, 0);
            renderFixedALTMarkers(mMVPMatrix);
            Matrix.translateM(mMVPMatrix, 0, -xlx, -xly, 0);

            xly = -0.5f * pixH2;
            Matrix.translateM(mMVPMatrix, 0, xlx, xly, 0);
            renderFixedRADALTMarkers(mMVPMatrix);   // AGL
            Matrix.translateM(mMVPMatrix, 0, -xlx, -xly, 0);

            xlx = -1.10f * pixM2;
            xly = -0.5f * portraitOffset * pixH2; // half of tape viewport //-0.7f * pixH2;
            Matrix.translateM(mMVPMatrix, 0, xlx, xly, 0);
            renderFixedASIMarkers(mMVPMatrix);
            Matrix.translateM(mMVPMatrix, 0, -xlx, -xly, 0);

            GLES20.glViewport(0, 0, pixW, pixH);  // fullscreen

            xlx = 0;
            xly = +0.90f * pixH2;
            Matrix.translateM(mMVPMatrix, 0, xlx, xly, 0);
            renderFixedDIMarkers(mMVPMatrix);
            renderHDGValue(mMVPMatrix);
            Matrix.translateM(mMVPMatrix, 0, -xlx, -xly, 0);
        }


        //-----------------------------
        renderTurnMarkers(mMVPMatrix);
        renderSlipBall(mMVPMatrix);
        renderGForceValue(mMVPMatrix);

        if (displayInfoPage) {
            renderAncillaryDetails(mMVPMatrix);
            renderBatteryPct(mMVPMatrix);
        }

        if (!ServiceableDevice) renderUnserviceableDevice(mMVPMatrix);
        //if (!ServiceableAh) renderUnserviceablePage(mMVPMatrix);
        if (!ServiceableAh) renderUnserviceableAh(mMVPMatrix);
        if (!ServiceableAlt) renderUnserviceableAlt(mMVPMatrix);
        if (!ServiceableAsi) renderUnserviceableAsi(mMVPMatrix);
        if (!ServiceableDi) {
            renderUnserviceableDi(mMVPMatrix);
            renderUnserviceableCompassRose(mMVPMatrix);
        }
        if (bBannerActive) renderBannerMsg(mMVPMatrix);
        if (bSimulatorActive) renderSimulatorActive(mMVPMatrix);


        // Do this last so that every else wil be dimmed for fatfinger entry
        if (displayFlightDirector || displayRMI || displayHITS) {
            renderSelWptDetails(mMVPMatrix);
            renderSelWptValue(mMVPMatrix);
            renderSelAltValue(mMVPMatrix);
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
    protected void renderUnserviceableDevice(float[] matrix)
    {
        renderUnserviceableAh(matrix);
        renderUnserviceableCompassRose(matrix);
        renderUnserviceableDi(matrix);
        renderUnserviceableAlt(matrix);
        renderUnserviceableAsi(matrix);
    }

    //
    // project
    //
    // relbrg in degrees
    // dme in nm
    // elev in m
    @Override
    protected Point project(float relbrg, float dme)
    {
        float pixPerDegree = pixM / pitchInView;
        // note: we do not take apt elevation into account
        return new Point(
            (float) (+pixPerDegree * relbrg),
            (float) (-pixPerDegree * Math.toDegrees(Math.atan2(MSLValue, Unit.NauticalMile.toFeet(dme))))
        );
    }


    @Override
    protected Point project(float relbrg, float dme, float elev)
    {
        float pixPerDegree = pixM / pitchInView;

        // note: we take apt elevation into account
        //float y1 = (float) (-Math.toDegrees(UTrig.fastArcTan2(MSLValue - z1 * 3.28084f, dme_ft)) * pixPerDegree);

        //float dme_ft = dme * 6080;
        //float y = (float) (-Math.toDegrees(UTrig.fastArcTan2(MSLValue - elev * 3.28084f, dme_ft)) * pixPerDegree);
        float y =  (float) (-pixPerDegree * Math.toDegrees(Math.atan2(MSLValue - Unit.Meter.toFeet(elev), Unit.NauticalMile.toFeet(dme))));

        return new Point(
                (float) (+pixPerDegree * relbrg),
                (float) y
        );
    }

    //-------------------------------------------------------------------------
    // Set the spinner control parameters
    //
    public void setSpinnerParams()
    {
        // This code determines where the spinner control
        // elements are displayed. Used by WPT and ALT
        if (Layout == layout_t.LANDSCAPE) {
            // Landscape --------------
            lineAutoWptDetails = 0.00f;
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
                selWptDec = -0.30f * pixH2;
                selWptInc = -0.41f * pixH2;
                selAltDec = -0.80f * pixH2;
                selAltInc = -0.91f * pixH2;

                lineC = -0.55f;
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
        float z, pixPerDegree, x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, zav;
        float lat, lon;
        pixPerDegree = pixM / pitchInView;
        z = zfloat;

        float dme;             //in nm
        float step = 0.50f;    //in nm, normally this should be = gridy
        float agl_ft;          //in Feet

        // oversize 20% a little to help with
        // bleed through caused by itrig truncating
        float gridy = 0.5f;    //in nm
        float gridx = 1.0f;    //in degree

        float dme_ft;            // =  60 * 6080 * Math.hypot(deltaLon, deltaLat);  // ft
        float demRelBrg;         // = DIValue + Math.toDegrees(Math.atan2(deltaLon, deltaLat));
        float caution;
        final float cautionMin = 0.2f;
        final float IASValueThreshold = AircraftData.Vx; //1.5f * Vs0;

        mSquare.SetWidth(1);

        for (dme = 0; dme <= DemGTOPO30.DEM_HORIZON; dme += step) {
            for (demRelBrg = -25; demRelBrg < 25; demRelBrg = demRelBrg + 1) {

                dme_ft = dme * 6080;
                lat = LatValue + dme / 60 * UTrig.icos((int) (DIValue + demRelBrg));
                lon = LonValue + dme / 60 * UTrig.isin((int) (DIValue + demRelBrg));
                z1 = DemGTOPO30.getElev(lat, lon);
                x1 = demRelBrg * pixPerDegree;
                y1 = (float) (-Math.toDegrees(UTrig.fastArcTan2(MSLValue - z1 * 3.28084f, dme_ft)) * pixPerDegree);

                lat = LatValue + dme / 60 * UTrig.icos((int) (DIValue + demRelBrg + gridx));
                lon = LonValue + dme / 60 * UTrig.isin((int) (DIValue + demRelBrg + gridx));
                z2 = DemGTOPO30.getElev(lat, lon);
                x2 = (demRelBrg + gridx) * pixPerDegree;
                y2 = (float) (-Math.toDegrees(UTrig.fastArcTan2(MSLValue - z2 * 3.28084f, dme_ft)) * pixPerDegree);

                dme_ft = (dme + gridy) * 6080;
                lat = LatValue + (dme + gridy) / 60 * UTrig.icos((int) (DIValue + demRelBrg + gridx));
                lon = LonValue + (dme + gridy) / 60 * UTrig.isin((int) (DIValue + demRelBrg + gridx));
                z3 = DemGTOPO30.getElev(lat, lon);
                x3 = (demRelBrg + gridx) * pixPerDegree;
                y3 = (float) (-Math.toDegrees(UTrig.fastArcTan2(MSLValue - z3 * 3.28084f, dme_ft)) * pixPerDegree);

                lat = LatValue + (dme + gridy) / 60 * UTrig.icos((int) (DIValue + demRelBrg));
                lon = LonValue + (dme + gridy) / 60 * UTrig.isin((int) (DIValue + demRelBrg));
                z4 = DemGTOPO30.getElev(lat, lon);
                x4 = (demRelBrg) * pixPerDegree;
                y4 = (float) (-Math.toDegrees(UTrig.fastArcTan2(MSLValue - z4 * 3.28084f, dme_ft)) * pixPerDegree);

                //
                //  77%
                //
                //   Triangle #2   Triangle #1
                //    +             +--+
                //    |\             \ |
                //    | \             \|
                //    +--+             +
                //

                // Triangle #1 --------------
                zav = z1;  // in m asml
                DemColor color = DemGTOPO30.getColor((short) zav);

                // Handle Monochrome
                if (colorTheme == 2) {
                    color.red = 0;
                    color.blue = 0;
                }

                caution = cautionMin + (color.red + color.green + color.blue);
                agl_ft = MSLValue - zav * 3.28084f;  // in ft

                if (agl_ft > 1000) mTriangle.SetColor(color.red, color.green, color.blue, 1);                     // Enroute
                else if (IASValue < IASValueThreshold) mTriangle.SetColor(color.red, color.green, color.blue, 1); // Taxi or approach
                else if (agl_ft > 200) mTriangle.SetColor(caution, caution, 0, 1f);                               // Proximity notification (yellow)
                else mTriangle.SetColor(caution, 0, 0, 1f);                                                       // Proximity warning (red)

                mTriangle.SetVerts(
                        x1, y1, z,
                        x2, y2, z,
                        x4, y4, z);
                mTriangle.draw(matrix);

                // Triangle #2 --------------
                zav = (z1 + z2) / 2; // take the simple average
                color = DemGTOPO30.getColor((short) zav);

                // Handle Monochrome
                if (colorTheme == 2) {
                    color.red = 0;
                    color.blue = 0;
                }

                caution = cautionMin + (color.red + color.green + color.blue);
                agl_ft = MSLValue - zav * 3.28084f;  // in ft

                if (agl_ft > 1000) mTriangle.SetColor(color.red, color.green, color.blue, 1);                     // Enroute
                else if (IASValue < IASValueThreshold) mTriangle.SetColor(color.red, color.green, color.blue, 1); // Taxi or  approach
                else if (agl_ft > 200) mTriangle.SetColor(caution, caution, 0, 1f);  // Proximity notification
                else mTriangle.SetColor(caution, 0, 0, 1f);                          // Proximity warning

                mTriangle.SetVerts(
                        x2, y2, z,
                        x3, y3, z,
                        x4, y4, z);
                mTriangle.draw(matrix);

                /*
                //
                //  69%
                //
                //   Square
                //   4    3
                //    +--+
                //    |  |
                //    |  |
                //    +--+
                //   1    2

                    zav = z1;  // use the
                    getColor((short) zav);
                    agl_ft = MSLValue - zav*3.28084f;  // in ft

                    if (agl_ft > 100) mSquare.SetColor(red, green, blue, 1);                      // Enroute
                    else if (IASValue < IASValueThreshold) mTriangle.SetColor(red, green, blue, 1); // Taxi or  apporach
                    else mSquare.SetColor(caution, 0, 0, 1f);                                     // Proximity warning

                    float[] squarePoly = {
                            x1, y1, z,
                            x2, y2, z,
                            x3, y3, z,
                            x4, y4, z
                    };
                    mSquare.SetVerts(squarePoly);
                    mSquare.draw(matrix);
                */

            }
        }
    }

    //-------------------------------------------------------------------------
    // Airspace
    //
    protected void renderAirspace(float[] matrix)
    {
        // Maybe later
    }


    //---------------------------------------------------------------------------
    // EFIS serviceability ... aka the Red X's
    //

    // Artificial Horizon serviceability
    public void setServiceableAh()
    {
        ServiceableAh = true;
    }

    public void setUnServiceableAh()
    {
        ServiceableAh = false;
    }

}
