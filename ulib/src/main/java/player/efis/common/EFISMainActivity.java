/*
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

package player.efis.common;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.text.format.Time;
import android.widget.Toast;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import player.ulib.DigitalFilter;
import player.ulib.UTrig;
import player.ulib.Unit;

public class EFISMainActivity extends Activity //implements Listener, SensorEventListener, LocationListener
{

    protected static final long GPS_UPDATE_PERIOD = 0;   //ms // 400
    protected static final long GPS_UPDATE_DISTANCE = 0; //ms // 1
    private static final float STD_RATE = 0.0524f;       // = rate 1 = 3deg/s
    //-------------------------------------------------------------------------
    // Utility function to calculate rate of climb
    // Rate of climb in m/s
    protected static Time time = new Time();    // Time class
    //-------------------------------------------------------------------------
    // Utility function to determine the direction of the turn and try to eliminate
    // the jitter around zero a little bit
    // Determine the direction of the turn based on the rotation and try to eliminate the jitter around zero a little bit
    // -1 for left turn
    // +1 for right turn
    //  0 for no turn
    static int rs = 0;  // variable to keep the running count
    private static long _time_a;
    private static float _altitude;    // previous altitude
    //-------------------------------------------------------------------------
    // Utility function to calculate rate of turn
    // Rate of turn in rad/s
    private static float _course;    // previous course
    private static long _time_c;
    private static float _rateOfTurn;
    //
    // Stratux handler - not used anymore
    //
    protected final int STRATUX_OK = 0;
    protected final int STRATUX_TASK = -1;
    protected final int STRATUX_DEVICE = -2;
    protected final int STRATUX_GPS = -3;
    protected final int STRATUX_WIFI = -4;
    protected final int STRATUX_SERVICE = -5;
    protected MediaPlayer mpCautionTraffic;
    protected MediaPlayer mpCautionTerrain;
    protected MediaPlayer mpFiveHundred;
    protected MediaPlayer mpSinkRate;
    protected MediaPlayer mpStall;
    protected SensorComplementaryFilter sensorComplementaryFilter;
    // location members
    protected LocationManager locationManager;
    protected String provider;
    protected GpsStatus mGpsStatus = null;
    protected boolean bSimulatorActive = false;
    protected boolean bStratuxActive = false;
    protected boolean bLockedMode = false;
    protected boolean bHudMode = false;
    protected boolean bLandscapeMode = false;
    protected int colorTheme; // 0=Normal, 1=High Contrast, 2=Monochrome
    // Location abstracts
    //_gps_lat = -33.98f;  _gps_lon =   18.82f; // Stellenbosh
    protected float gps_lat;// = -34f;            // in decimal degrees
    protected float gps_lon;// = +19f;            // in decimal degrees
    protected float gps_altitude;       // in m
    protected float gps_agl;            // in m
    protected float gps_speed;          // in m/s
    protected float gps_course;         // in radians
    protected float gps_rateOfClimb;    // in m/s
    protected float gps_rateOfTurn;     // in rad/s
    protected boolean hasSpeed;
    protected boolean hasGps;
    protected float orientationAzimuth;
    protected float orientationPitch;
    protected float orientationRoll;
    protected int gps_insky;
    protected int gps_infix;
    protected float sensorBias;           // gyroscope / GPS bias

    /*private MediaPlayer mpCautionTerrain;
    private MediaPlayer mpFiveHundred;
    private MediaPlayer mpSinkRate;
    private MediaPlayer mpStall;*/
    protected Gpx mGpx;                   // wpt database
    protected DemGTOPO30 mDemGTOPO30;     // dem database
    // Digital filters
    protected DigitalFilter filterRateOfTurnGyro = new DigitalFilter(16); //8
    protected DigitalFilter filterSlip = new DigitalFilter(32);           //32
    protected DigitalFilter filterRoll = new DigitalFilter(8);            //16
    protected DigitalFilter filterPitch = new DigitalFilter(8);           //16
    protected DigitalFilter filterRateOfClimb = new DigitalFilter(4);     //8
    protected DigitalFilter filterfpvX = new DigitalFilter(256);          //128
    protected DigitalFilter filterfpvY = new DigitalFilter(256);          //128
    protected DigitalFilter filterG = new DigitalFilter(32);              //32
    protected DigitalFilter filterGpsSpeed = new DigitalFilter(6);        //4
    protected DigitalFilter filterGpsAltitude = new DigitalFilter(6);     //4
    protected DigitalFilter filterGpsCourse = new DigitalFilter(8);       //4
    // Stratux Wifi
    protected WifiManager wifiManager;
    protected StratuxWiFiTask mStratux;
    protected long PrevStratuxTimeStamp;// = Long.MAX_VALUE;
    //-------------------------------------------------------------------------
    // Utility function to do a simple simulation for demo mode
    // It acts like a crude flight simulator
    //
    protected float _gps_lat = 00.00f;
    protected float _gps_lon = 00.00f;   // null island
    protected float _gps_agl = 0; // meters
    protected long sim_ms;
    //for landscape mode
    // private float azimuthValue;
    protected float rollValue;
    protected float pitchValue;
    protected float gyro_rateOfTurn;
    protected float loadfactor;
    protected float slipValue;
    protected int ctr = 0;
    float _gps_course = 0.96f;    // in radians
    float _gps_altitude = 3000;   // meters
    float _gps_speed = 0;         // m/s
    long _sim_ms = 0;
    Random sim_rand = new Random();
    boolean sim_primed = false;
    // Create a Timer
    Timer timer = new Timer();

    protected void setGpsStatus() {
        gps_insky = 0;
        gps_infix = 0;
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mGpsStatus = locationManager.getGpsStatus(mGpsStatus);
            Iterable<GpsSatellite> sats = mGpsStatus.getSatellites();
            for (GpsSatellite s : sats) {
                gps_insky += 1;
                if (s.usedInFix()) gps_infix += 1;
            }
        }
    }

    protected void killProcess(String process) {
        // Required the following permission:
        // <uses-permission android:name="android.permission.KILL_BACKGROUND_PROCESSES" />
        ActivityManager am = (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
        am.killBackgroundProcesses(process);
    }

    protected boolean connectWiFi(String ssid) {
        // Connect to wifi
        Toast.makeText(this, "Stratux: Connecting ...", Toast.LENGTH_SHORT).show();
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", ssid);
        //wifiConfig.preSharedKey = String.format("\"%s\"", key); // not used for Stratux
        wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

        // WifiManager
        wifiManager = (WifiManager) getApplicationContext().getApplicationContext().getSystemService(WIFI_SERVICE);
        int netId = wifiManager.addNetwork(wifiConfig);
        wifiManager.disconnect();
        wifiManager.enableNetwork(netId, true);
        boolean rv = wifiManager.reconnect();

        if (checkWiFiStatus(ssid)) {
            Toast.makeText(this, "Stratux: Connected", Toast.LENGTH_SHORT).show();
            return true;
        } else
            return false;
    }

    protected boolean checkWiFiStatus(String ssid) {
        try {
            WifiInfo info = wifiManager.getConnectionInfo();
            return (info.getSupplicantState() == SupplicantState.COMPLETED)
                    && (info.getSSID().contains(ssid));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    protected int handleStratux() {
        if (bSimulatorActive) return STRATUX_OK;

        if (checkWiFiStatus("stratux")) {
            // We have a wifi connection to "stratux"
            // check for task and pulse
            if (mStratux == null) return STRATUX_TASK;
            if (!mStratux.isRunning()) return STRATUX_TASK;
            if (!mStratux.isDeviceRunning()) return STRATUX_DEVICE;

            gps_infix = mStratux.GPSSatellites;
            gps_insky = mStratux.GPSSatellitesSeen;
            //gps_insky = mStratux.GPSSatellitesTracked;

            pitchValue = (float) mStratux.AHRSPitch;
            rollValue = (float) mStratux.AHRSRoll;

            // We have pulse/task check for GPS
            if (mStratux.isGpsValid()) {
                // and we have valid GPS (also implies running Stratux)
                gps_lat = (float) mStratux.GPSLatitude;
                gps_lon = (float) mStratux.GPSLongitude;
                gps_altitude = Unit.Feet.toMeter((float) mStratux.GPSAltitudeMSL);
                gps_agl = DemGTOPO30.calculateAgl(gps_lat, gps_lon, gps_altitude);
                gps_speed = Unit.Knot.toMeterPerSecond((float) mStratux.GPSGroundSpeed);
                //slipValue = (float) -Math.toRadians(mStratux.AHRSSlipSkid);
                slipValue = (float) -mStratux.AHRSSlipSkid; // in degrees (todo make all radians)
                loadfactor = (float) mStratux.AHRSGLoad;        // in gunits
                gps_rateOfTurn = (float) Math.toRadians(mStratux.GPSTurnRate);
                if (mStratux.AHRSTurnRate == 3276.7)  // 3276.7 is the magic number from Stratus to show invalid
                    gyro_rateOfTurn = 0;
                else
                    gyro_rateOfTurn = (float) Math.toRadians(mStratux.AHRSTurnRate); // check this

                hasGps = true;

                if (gps_speed > 5) {
                    hasSpeed = true;
                    gps_course = (float) Math.toRadians(mStratux.GPSTrueCourse);
                } else hasSpeed = false;
                return STRATUX_OK;
            } else {
                return STRATUX_GPS;
            }
        } else {
            if (ctr % 100 == 0) {
                hasGps = false;
                hasSpeed = false;
                connectWiFi("stratux");  // force the connection to stratux
            }
            return STRATUX_WIFI;
        }
    }

    private int getTurnDirection(float rotValue) {
        if (Math.signum(rotValue) > 0) rs++;
        else rs--;

        final int JITTER_COUNT = 10;
        int turnDirection;
        if (rs > JITTER_COUNT) {
            rs = JITTER_COUNT;
            return 1;
        } else if (rs < -JITTER_COUNT) {
            rs = -JITTER_COUNT;
            return -1;
        } else return 0;
    }

    protected float calculateRateOfClimb(float altitude) {
        float rateOfClimb = 0;
        long deltaT;
        float deltaAlt = altitude - _altitude;

        time.setToNow();
        long time_a = time.toMillis(true);
        deltaT = time_a - _time_a;
        if (deltaT > 0) {
            rateOfClimb = 1000 * filterRateOfClimb.runningAverage(deltaAlt / (float) deltaT); // m/s
            _time_a = time_a;
            _altitude = altitude; // save the previous altitude
        }
        return rateOfClimb;
    }

    protected float calculateRateOfTurn(float course) {
        float rateOfTurn = 0;
        long deltaT;
        float deltaCrs = course - _course;

        // Handle the case around 0
        if (Math.abs(deltaCrs) > UTrig.M_PI_4) {
            _course = course;   // save the previous course
            return _rateOfTurn; // result would be rubbish, just return the previous rot
        }

        time.setToNow();
        long time_c = time.toMillis(true);
        deltaT = time_c - _time_c;
        if (deltaT > 0) {
            rateOfTurn = 1000 * deltaCrs / deltaT; // rad/s
            _time_c = time_c; // save the previous time
        } else {
            rateOfTurn = _rateOfTurn;
        }

        _course = course;         // save the previous course
        _rateOfTurn = rateOfTurn; // save the previous rate of turn
        return rateOfTurn;
    }

    //-------------------------------------------------------------------------
    // Utility function to calculate the remaining
    // battery in percentage.
    protected float getRemainingBattery() {
        // Get the battery percentage
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        return level / (float) scale;
    }

    //-------------------------------------------------------------------------
    // Utility function to Check GPS status
    //
    // We assume no GPS is available if there has not been a valid
    // altitude update in 10 seconds
    //
    protected boolean isGPSAvailable() {
        return gps_infix > 3;
    }

    //-------------------------------------------------------------------------
    // Utility function to Restart the App
    //
    // It is pretty brutal should save the persistent data first via onStop
    //
    private void restartEFISApp() {
        onStop();

        Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    protected void Simulate() {
        //pitchValue = -sensorComplementaryFilter.getPitch();
        //rollValue = -sensorComplementaryFilter.getRoll();

        hasSpeed = true;
        hasGps = true;

        final float setSpeed = Unit.Knot.toMeterPerSecond(AircraftData.Vno);

        if (Math.abs(pitchValue) > 10) {
            _gps_speed -= 0.01f * pitchValue;
            if (_gps_speed > setSpeed) _gps_speed = setSpeed;
            if (_gps_speed < -setSpeed) _gps_speed = -setSpeed;
        } else {
            _gps_speed *= 0.99998;  // decay to zero
        }
        gps_speed = setSpeed + _gps_speed;
        gps_rateOfClimb = (pitchValue * gps_speed / 50);

        _gps_altitude += (gps_rateOfClimb / 10);
        if (_gps_altitude < -100) _gps_altitude = -100;   // meters
        if (_gps_altitude > 10000) _gps_altitude = 10000; // meters
        gps_altitude = _gps_altitude;

        if (gps_speed != 0) {
            _gps_course += (rollValue * gps_speed / 1e6f);
            while (_gps_course > (UTrig.M_2PI)) _gps_course %= (UTrig.M_2PI);
            while (_gps_course < 0) _gps_course += (UTrig.M_2PI);
        }
        gps_course = _gps_course;

        time.setToNow();
        sim_ms = time.toMillis(true);
        float deltaT = (float) (sim_ms - _sim_ms) / 1000f / 3600f / 1.85f / 60f;  // in sec and scaled from meters to nm to degree

        /*
        //------------------------------------------------------------------------------------------
        // todo: Hardcoded for debugging
        //
        //deltaT = 0.0000124f;  // Ludicrous Speed
        //deltaT = 0.00000124f; // Warp Speed ~ 490m/s - mach 1.5
        deltaT = 0.000000224f;  // Super Speed2
        //deltaT = 0; // freeze time, ie force stationary

        // YCMH 090 from Perth
        if (!sim_primed) {
            //_gps_lat = -25.656874f; float _gps_lon =   28.221832f; // Wonderboom
            //_gps_lat = -34.259918f; float _gps_lon = 115.45f; // South of Valsbaai -34.359918f
            //_gps_lat = -31.9f;  _gps_lon = 115.45f;  // Australia north of Rottnest
            //_gps_lat = -33.0f;   _gps_lon = 28; //-28;// = -33; // South Africa - East London
            _gps_lat = -33.98f; _gps_lon = 18.82f; // Stellenbosh, South Africa

            //_gps_lat = +50f;  _gps_lon = -124f; // Vancouver
            //_gps_lat =  40.7f;   _gps_lon = -111.82f;  // Salt Lake City
            //_gps_lat =  48.14f;  _gps_lon = 11.57f;    // Munich
            //_gps_lat =  47.26f;  _gps_lon = 11.34f;    //Innsbruck
            //_gps_lat =  55.67f;  _gps_lon = 12.57f;    // Copenhagen
            //_gps_lat =  46.93f;  _gps_lon =  7.45f;    // Bern
            //_gps_lat = -33.00;  _gps_lon =  -71.00f;   // Chile, Santiago
            //_gps_lat = -34.8f;  _gps_lon =  -56.0f;    // Motevideo
            //_gps_lat = -10.8f;  _gps_lon =  -65.35f;   // Emilio Beltran
            //_gps_lat = 00.26f;  _gps_lon = 00.34f;   // close to null island
            //_gps_lat = 55.86f; _gps_lon = 37.6f;   // Moscow

            sim_primed = true;
        }

        //Random rnd = new Random();
        //gps_course = _gps_course = (float) Math.toRadians(50);// 50 // + (float) rnd.nextGaussian() / 200;
        //gps_speed = _gps_speed = setSpeed;//100;  // m/s
        //gps_altitude = Unit.Feet.toMeter(8000); //2048; //900; //3048; //Meter
        //rollValue = 0;// (float) rnd.nextGaussian() / 5;
        //pitchValue = 0;//(float) rnd.nextGaussian() / 20;
        //
        // todo: Hardcoded for debugging
        //------------------------------------------------------------------------------------------
        // */

        _sim_ms = sim_ms;
        if ((deltaT > 0) && (deltaT < 0.0000125)) {
            gps_lon = _gps_lon += deltaT * gps_speed * Math.sin(gps_course);
            gps_lat = _gps_lat += deltaT * gps_speed * Math.cos(gps_course);

            if (gps_lon > 180) gps_lon = -180;
            if (gps_lon < -180) gps_lon = 180;
            if (gps_lat > 90) gps_lat = -90;
            if (gps_lat < -90) gps_lat = 90;
        }
        gps_agl = DemGTOPO30.calculateAgl(gps_lat, gps_lon, gps_altitude);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final int FPS = 40;
        TimerTask updateStratux = new UpdateStratuxTask();
        timer.scheduleAtFixedRate(updateStratux, 0, 1000 / FPS);
    }
    //And then add the new task to the Timer with some update interval

    //Then you extend the timer task
    class UpdateStratuxTask extends TimerTask {
        public void run() {
            //calculate the new position of myBall
            //handleStratux();
        }
    }
}
/*
new AlertDialog.Builder(this)
        .setMessage("Hello world!")
        .setPositiveButton("OK",null)
        .show();
*/
