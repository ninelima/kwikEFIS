/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.widget.Toast;

import player.efis.common.StratuxWiFiTask;
import player.efis.common.prefs_t;

//
// A view container where OpenGL ES graphics can be drawn on screen.
// This view can also be used to capture touch events, such as a user
// interacting with drawn objects.
//
public class SurfaceView extends GLSurfaceView
{
    private static final int MIN_DISTANCE = 150;
    public final player.efis.Renderer mRenderer;
    // normally this would be private but we want to access the sel wpt from main activity
    //private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
    private float mPreviousX;
    private float mPreviousY;

    public SurfaceView(Context context)
    {
        super(context);

        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);

        // Set the Renderer for drawing on the GLSurfaceView
        mRenderer = new player.efis.Renderer(context); // = new MyGLRenderer();  --b2
        setRenderer(mRenderer);

        // Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e)
    {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        float x = e.getX();
        float y = e.getY();

        switch (e.getAction()) {
            // We don't use movement messages, maybe later ...
/*
            case MotionEvent.ACTION_MOVE:

                float dx = x - mPreviousX;
                float dy = y - mPreviousY;

                // reverse direction of rotation above the mid-line
                if (y > getHeight() / 2) {
                    dx = dx * -1;
                }

                // reverse direction of rotation to left of the mid-line
                if (x < getWidth() / 2) {
                    dy = dy * -1;
                }

                requestRender();
                break;
*/

            case MotionEvent.ACTION_DOWN:
                mRenderer.setActionDown(x, y);
                requestRender();

                mPreviousX = x;
                mPreviousY = y;
                break;

            case MotionEvent.ACTION_UP:
                float deltaX = x - mPreviousX;
                float deltaY = y - mPreviousY;

                if (Math.abs(deltaY) > MIN_DISTANCE) {
                    if (mRenderer.isAutoZoomActive())
                        Toast.makeText(getContext(), "Auto Zoom OFF", Toast.LENGTH_SHORT).show();
                    setAutoZoomActive(false);
                    if (deltaY < 0) {
                        // swipe up
                        zoomIn();
                    }
                    else {
                        // swipe down
                        zoomOut();
                    }
                }
                else if (Math.abs(deltaX) > MIN_DISTANCE) {
                    if (!mRenderer.isAutoZoomActive())
                        Toast.makeText(getContext(), "Auto Zoom ON", Toast.LENGTH_SHORT).show();
                    setAutoZoomActive(true);
                    if (deltaY > 0) {
                        // swipe right
                    }
                    else {
                        // swipe left
                    }
                }
                else {
                    // consider as something else - a screen tap for example
                }
                break;
        }
        requestRender();
        return true;
    }

    // Pitch
    public void setPitch(float degrees)
    {
        mRenderer.setPitch(degrees);
        requestRender();
    }

    // Roll / Bank
    public void setRoll(float degrees)
    {
        mRenderer.setRoll(degrees);
        requestRender();
    }

    // Heading / Course indicator
    public void setHeading(float degrees)
    {
        mRenderer.setHeading(degrees);
        requestRender();
    }

    // Altimeter
    public void setALT(int value)
    {
        mRenderer.setALT(value);
        requestRender();
    }

    // Radio Altimeter (agl)
    public void setAGL(int value)
    {
        mRenderer.setAGL(value);
        requestRender();
    }


    // Air Speed Indicator
    public void setASI(float value)
    {
        mRenderer.setASI(value);
        requestRender();
    }

    // Percentage battery remaining
    public void setBatteryPct(float value)
    {
        mRenderer.setBatteryPct(value);
        requestRender();
    }

    // Automatic Waypoint
    public void setWPT(String wpt)
    {
        mRenderer.setAutoWptValue(wpt);
        requestRender();
    }

    // The DME - Distance Measuring Equipment
    public void setDME(float dme)
    {
        mRenderer.setAutoWptDme(dme);
        requestRender();
    }

    // Ground Speed
    public void setGS(float gs)
    {
        //mRenderer.setGS(gs);
        //requestRender();
    }


    //
    // Red X's
    //

    // The entire EFIS
    public void setUnServiceableDevice()
    {
        mRenderer.setUnServiceableDevice();
        requestRender();
    }

    public void setServiceableDevice()
    {
        mRenderer.setServiceableDevice();
        requestRender();
    }

    // Moving Map
    public void setUnServiceableMap()
    {
        mRenderer.setUnServiceableMap();
        requestRender();
    }

    public void setServiceableMap()
    {
        mRenderer.setServiceableMap();
        requestRender();
    }

    // Altimeter
    public void setUnServiceableAlt()
    {
        mRenderer.setUnServiceableAlt();
        requestRender();
    }

    public void setServiceableAlt()
    {
        mRenderer.setServiceableAlt();
        requestRender();
    }

    // Airspeed
    public void setUnServiceableAsi()
    {
        mRenderer.setUnServiceableAsi();
        requestRender();
    }

    public void setServiceableAsi()
    {
        mRenderer.setServiceableAsi();
        requestRender();
    }

    // Direction Indicator
    public void setUnServiceableDi()
    {
        mRenderer.setUnServiceableDi();
        requestRender();
    }

    public void setServiceableDi()
    {
        mRenderer.setServiceableDi();
        requestRender();
    }

    public void setLatLon(float lat, float lon)
    {
        mRenderer.setLatLon(lat, lon);
        requestRender();
    }

    public void setGpsStatus(String gpsstatus)
    {
        mRenderer.setGpsStatus(gpsstatus);
        requestRender();
    }


    public void setDisplayAirport(boolean display)
    {
        mRenderer.setDisplayAirport(display);
        requestRender();
    }

    void setDisplayAirspace(boolean display)
    {
        mRenderer.setDisplayAirspace(display);
        requestRender();
    }

    void setBannerMsg(boolean cal, String msg)
    {
        mRenderer.setBannerMsg(cal, msg);
        requestRender();
    }

    public void setSimulatorActive(boolean demo, String msg)
    {
        mRenderer.setSimulatorActive(demo, msg);
        requestRender();
    }

    public void setPrefs(prefs_t pref, boolean value)
    {
        mRenderer.setPrefs(pref, value);
        requestRender();
    }

    public void setTheme(int value)
    {
        if (value == 2) mRenderer.setThemeGreen();
        else if (value == 1) mRenderer.setThemeLight();
        else mRenderer.setThemeDark(); // the default
        requestRender();
    }

    //
    // ADSB targets
    //
    public void setTargets(StratuxWiFiTask Stratux)
    {
        mRenderer.setTargets(Stratux);
        requestRender();
    }

    //
    // Active Device
    //
    public void setActiveDevice(String device)
    {
        mRenderer.setActiveDevice(device);
        requestRender();
    }


    //
    // Map Zooming
    //
    public void setMapZoom(float zoom)
    {
        mRenderer.setMapZoom(zoom);
        requestRender();
    }

    public void zoomIn()
    {
        mRenderer.zoomIn();
        requestRender();
    }

    public void zoomOut()
    {
        mRenderer.zoomOut();
        requestRender();
    }

    public void setAutoZoomActive(boolean active)
    {
        mRenderer.setAutoZoomActive(active);
        requestRender();
    }
}
