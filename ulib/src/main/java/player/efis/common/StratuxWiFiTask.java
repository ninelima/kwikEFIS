package player.efis.common;

import android.os.AsyncTask;

import com.stratux.stratuvare.utils.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

import player.ulib.UTime;

public class StratuxWiFiTask extends AsyncTask<String, Void, Void>
{
    protected static final int CONNECTED = 1;
    protected static final int CONNECTING = 2;
    protected static final int DISCONNECTED = 0;
    static Semaphore mutex = new Semaphore(1, true);
    private static boolean bCageAhrs = false;
    // Public Stratux vars
    public long StratuxTimeStamp;
    public int GPSSatellites;
    public int GPSSatellitesTracked;
    public int GPSSatellitesSeen;
    public double AHRSPitch;
    public double AHRSRoll;
    public double AHRSGyroHeading;
    public double AHRSMagHeading;
    public double AHRSSlipSkid;
    public double AHRSTurnRate;
    public double AHRSGLoad;
    public double AHRSGLoadMin;
    public double AHRSGLoadMax;
    public int AHRSStatus;
    public double GPSLongitude;
    public double GPSLatitude;
    public double GPSAltitudeMSL;
    public double GPSTrueCourse;
    public double GPSTurnRate;
    public double GPSGroundSpeed;
    public double BaroTemperature;
    public double BaroPressureAltitude;
    public double BaroVerticalSpeed;
    //public String BaroLastMeasurementTime;
    public boolean proximityAlert;// = false;
    DatagramSocket mSocket;
    private boolean mRunning;
    private int mPort = 4000;
    private int mState;
    private String id;
    private LinkedList<String> trafficList = new LinkedList<>();
    private boolean mGpsPositionValid;
    private boolean mBatteryLow;
    private boolean mDeviceRunning;

    public StratuxWiFiTask(String id)
    {
        this.id = id;
        mState = DISCONNECTED;
    }

    private static void doSleep(int ms)
    {
        // Wait ms milliseconds
        try {
            Thread.sleep(ms);
        }
        catch (Exception ignored) {
        }
    }

    public static void doCageAhrs()
    {
        // Wait ms milliseconds
        bCageAhrs = true;
    }

    private static String postHttp(String addr)
    {
        return doHttp(addr, "POST");
    }

    private static String doHttp(String addr, String method)
    {
        URL url;
        StringBuilder response = new StringBuilder();
        try {
            url = new URL(addr);
        }
        catch (MalformedURLException e) {
            throw new IllegalArgumentException("invalid url");
        }

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(false);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod(method); //"GET"
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");

            // handle the response
            int status = conn.getResponseCode();
            if (status != 200) {
                throw new IOException("Post failed with error code " + status);
            }
            else {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (conn != null) {
                conn.disconnect();
            }
            //Here is your json in string format
            return response.toString();
        }
    }

    protected Void doInBackground(String... urls)
    {
        mRunning = true;
        mainExecutionLoop();
        return null;
    }

    protected void onPostExecute()
    {
        // TODO: check this.exception
        // TODO: do something with the feed
    }

    LinkedList<String> getTargetList()
    {
        try {
            mutex.acquire();
            try {
                return new LinkedList<>(trafficList);
            }
            finally {
                mutex.release();
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isGpsValid()
    {
        try {
            mutex.acquire();
            try {
                return mGpsPositionValid;
            }
            finally {
                mutex.release();
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isDeviceRunning()
    {
        try {
            mutex.acquire();
            try {
                return mDeviceRunning;
            }
            finally {
                mutex.release();
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void mainExecutionLoop()
    {
        byte[] buffer = new byte[8192];
        BufferProcessor bp = new BufferProcessor();

        //
        // This state machine will keep trying to connect to
        // ADBS/GPS receiver
        //
        while (mRunning) {
            if (bCageAhrs) {
                bCageAhrs = false;
                calibrateAhrs();
                cageAhrs();
            }

            //  Read
            int nrBytesRead = read(buffer);

            if (nrBytesRead <= 0) {
                // Set the flags to worst case
                try {
                    mutex.acquire();
                    try {
                        mGpsPositionValid = false;
                        //mDeviceRunning = false;
                        //mBatteryLow = true;
                    }
                    finally {
                        mutex.release();
                    }
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Wait a sec
                doSleep(1000);

                // Try and re-connect
                disconnect();
                connect(Integer.toString(mPort), false);
                continue;
            }

            //  Put both in Decode and ADSB buffers
            bp.put(buffer, nrBytesRead);

            try {
                mutex.acquire();
                try {
                    LinkedList<String> objs = bp.decode();
                    long unixTime = UTime.getUtcTimeMillis();
                    long lastTime = 0;

                    /*-------------------------------------
                    // < debug - add ghost AC traffic

                    // fixed target
                    JSONObject object = new JSONObject();

                    //LongReportMessage tm = (LongReportMessage) m;
                    try {
                        object.put("type", "traffic");
                        object.put("longitude", (double) 115.83);
                        object.put("latitude", (double) -31.80);
                        object.put("speed", (double) 123.0);
                        object.put("bearing", (double) 348.7);
                        //object.put("altitude", (double) Unit.Meter.toFeet(75));
                        object.put("altitude", 2000);
                        object.put("callsign", (String) "GHOST-0");
                        object.put("address", (int) 777);
                        object.put("time", (long) unixTime);
                    }
                    catch (JSONException e1) {
                        continue;
                    }
                    objs.add(object.toString());
                    // debug >
                    //-------------------------------------*/

                    // Extract traffic
                    for (String s : objs) {
                        try {
                            long deltaT = 0;
                            JSONObject js = new JSONObject(s);
                            // Traffic
                            if (js.getString("type").contains("traffic")) {
                                // See if it exists in the list and
                                // delete the entry first
                                for (int i = 0; i < trafficList.size(); i++) {
                                    String t = trafficList.get(i);
                                    JSONObject jt = new JSONObject(t);
                                    deltaT = unixTime - jt.getLong("time");

                                    if ((jt.getInt("address") == js.getInt("address"))
                                            || (deltaT > 20 * 1000)) {
                                        trafficList.remove(i);
                                    }
                                }
                                trafficList.add(s);
                            }

                            // Heartbeat
                            if (js.getString("type").contains("heartbeat")) {
                                StratuxTimeStamp = js.getLong("timestamp");
                                mGpsPositionValid = js.getBoolean("gpsvalid");
                                mBatteryLow = js.getBoolean("lowbattery");
                                mDeviceRunning = js.getBoolean("running");
                            }

                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }


                    //----------------------------------------------
                    // use the Http
                    try {
                        // Situation
                        String situation = getSituation();

                        JSONObject jObject;
                        jObject = new JSONObject(situation);

                        GPSSatellites = jObject.getInt("GPSSatellites");
                        GPSSatellitesTracked = jObject.getInt("GPSSatellitesTracked");
                        GPSSatellitesSeen = jObject.getInt("GPSSatellitesSeen");

                        AHRSPitch = jObject.getDouble("AHRSPitch");
                        AHRSRoll = jObject.getDouble("AHRSRoll");
                        AHRSGyroHeading = jObject.getDouble("AHRSGyroHeading");
                        AHRSMagHeading = jObject.getDouble("AHRSMagHeading");
                        AHRSSlipSkid = jObject.getDouble("AHRSSlipSkid"); // in degrees
                        AHRSTurnRate = jObject.getDouble("AHRSTurnRate"); // degrees/second
                        AHRSGLoad = jObject.getDouble("AHRSGLoad");
                        AHRSGLoadMin = jObject.getDouble("AHRSGLoadMin");
                        AHRSGLoadMax = jObject.getDouble("AHRSGLoadMax");

                        GPSLatitude = jObject.getDouble("GPSLatitude");
                        GPSLongitude = jObject.getDouble("GPSLongitude");
                        GPSAltitudeMSL = jObject.getDouble("GPSAltitudeMSL");
                        GPSTrueCourse = jObject.getDouble("GPSTrueCourse");
                        GPSTurnRate = jObject.getDouble("GPSTurnRate");
                        GPSGroundSpeed = jObject.getDouble("GPSGroundSpeed");

                        BaroTemperature = jObject.getDouble("BaroTemperature");
                        BaroPressureAltitude = jObject.getDouble("BaroPressureAltitude");
                        BaroVerticalSpeed = jObject.getDouble("BaroVerticalSpeed");

                        // Status
                        //String status = getDeviceStatus();
                        //jObject = new JSONObject(status);

                        /*
                        Log.v("b2", //" AHRSSlipSkid " + AHRSSlipSkid  +
                                    //" AHRSGLoad " + AHRSGLoad  +
                                    " AHRSTurnRate " + AHRSTurnRate +
                                    //" BaroTemperature " + BaroTemperature +
                                    " GPSTurnRate " + GPSTurnRate
                        );
                        // */

                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                finally {
                    mutex.release();
                }
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        disconnect();
    }

    private int read(byte[] buffer)
    {
        DatagramPacket pkt = new DatagramPacket(buffer, buffer.length);
        try {
            mSocket.receive(pkt);
        }
        catch (Exception e) {
            return -1;
        }

        //saveToFile(pkt.getLength(), buffer);
        return pkt.getLength();
    }

    //
    // Stratux getSituation post
    //
    private String getSituation()
    {
        return getHttp("http://192.168.10.1/getSituation");
    }

    //
    // Stratux getSituation post
    //
    private String getDeviceStatus()
    {
        return getHttp("http://192.168.10.1/getStatus");
    }

    //
    // Stratux "level" attitude display. Submit a blank POST to this URL.
    //
    private void cageAhrs()
    {
        postHttp("http://192.168.10.1/cageAHRS");
    }

    //
    // Stratux sensor calibration. Submit a blank POST to this URL.
    //
    private void calibrateAhrs()
    {
        postHttp("http://192.168.10.1/calibrateAHRS");
    }

    private String getHttp(String addr)
    {
        return doHttp(addr, "GET");
    }

    public boolean connect(String to, boolean secure)
    {
        try {
            mPort = Integer.parseInt(to);
        }
        catch (Exception e) {
            return false;
        }

        Logger.Logit(id + "Making socket to listen");

        try {
            mSocket = new DatagramSocket(mPort);
            mSocket.setBroadcast(true);
        }
        catch (Exception e) {
            Logger.Logit(id + "Failed! Connecting socket " + e.getMessage());
            return false;
        }
        mState = CONNECTED;
        return true; //connectConnection();
    }

    public String getParam()
    {
        return Integer.toString(mPort);
    }


    public void disconnect()
    {
        try {
            mSocket.close();
            mState = DISCONNECTED;
        }
        catch (Exception e) {
            e.printStackTrace();
            Logger.Logit(id + "Error stream close");
        }
    }

    public void finish()
    {
        mRunning = false;
    }

    protected boolean isRunning()
    {
        return mRunning;
    }

    protected boolean isStopped()
    {
        return !mRunning;
    }
}

    /*{
        "GPSLastFixSinceMidnightUTC": 67337.6,
            "GPSLatitude": 39.108533,
            "GPSLongitude": -76.770862,
            "GPSFixQuality": 2,
            "GPSHeightAboveEllipsoid": 115.51,
            "GPSGeoidSep": -17.523,
            "GPSSatellites": 5,
            "GPSSatellitesTracked": 11,
            "GPSSatellitesSeen": 8,
            "GPSHorizontalAccuracy": 10.2,
            "GPSNACp": 9,
            "GPSAltitudeMSL": 170.10767,
            "GPSVerticalAccuracy": 8,
            "GPSVerticalSpeed": -0.6135171,
            "GPSLastFixLocalTime": "0001-01-01T00:06:44.24Z",
            "GPSTrueCourse": 0,
            "GPSTurnRate": 0,
            "GPSGroundSpeed": 0.77598433056951,
            "GPSLastGroundTrackTime": "0001-01-01T00:06:44.24Z",
            "GPSTime": "2017-09-26T18:42:17Z",
            "GPSLastGPSTimeStratuxTime": "0001-01-01T00:06:43.65Z",
            "GPSLastValidNMEAMessageTime": "0001-01-01T00:06:44.24Z",
            "GPSLastValidNMEAMessage": "$PUBX,04,184426.00,260917,240266.00,1968,18,-177618,-952.368,21*1A",
            "GPSPositionSampleRate": 0,
            "BaroTemperature": 37.02,
            "BaroPressureAltitude": 153.32,
            "BaroVerticalSpeed": 1.3123479,
            "BaroLastMeasurementTime": "0001-01-01T00:06:44.23Z",
            "AHRSPitch": -0.97934145732801,
            "AHRSRoll": -2.2013729217108,
            "AHRSGyroHeading": 187741.08073052,
            "AHRSMagHeading": 3276.7,
            "AHRSSlipSkid": 0.52267604604907,
            "AHRSTurnRate": 3276.7,
            "AHRSGLoad": 0.99847599584255,
            "AHRSGLoadMin": 0.99815989027411,
            "AHRSGLoadMax": 1.0043409597397,
            "AHRSLastAttitudeTime": "0001-01-01T00:06:44.28Z",
            "AHRSStatus": 7
    }
    */

/*
    protected RSSFeed doInBackground(String... urls)
        {
            try {
                URL url = new URL(urls[0]);
                SAXParserFactory factory = SAXParserFactory.newInstance();
                SAXParser parser = factory.newSAXParser();
                XMLReader xmlreader = parser.getXMLReader();
                RssHandler theRSSHandler = new RssHandler();
                xmlreader.setContentHandler(theRSSHandler);
                InputSource is = new InputSource(url.openStream());
                xmlreader.parse(is);

                return theRSSHandler.getFeed();
            } catch (Exception e) {
                this.exception = e;

                return null;
            } finally {
                is.close();
            }
        }

        protected void onPostExecute (RSSFeed feed)
        {
            // TODO: check this.exception
            // TODO: do something with the feed
        }
*/

                    /*-------------------------------------
                    // < debug - add ghost AC traffic

                    // fixed target
                    JSONObject object = new JSONObject();

                    //LongReportMessage tm = (LongReportMessage) m;
                    try {
                        object.put("type", "traffic");
                        object.put("longitude", (double) 115.83);
                        object.put("latitude", (double) -31.80);
                        object.put("speed", (double) 123.0);
                        object.put("bearing", (double) 348.7);
                        //object.put("altitude", (double) Unit.Meter.toFeet(75));
                        object.put("altitude", 4321);
                        object.put("callsign", (String) "GHOST-0");
                        object.put("address", (int) 777);
                        object.put("time", (long) unixTime);
                    }
                    catch (JSONException e1) {
                        continue;
                    }
                    objs.add(object.toString());


                    // 2 moving targets
                    if (unixTime - pt1 > 4000) {
                        pt1 = unixTime;
                        b = b + 0.001f;

                        object = new JSONObject();
                        //LongReportMessage tm = (LongReportMessage) m;
                        try {
                            object.put("type", "traffic");
                            object.put("longitude", (double) 115.9 - b / 5);
                            object.put("latitude", (double) -32.2 + b);
                            object.put("speed", (double) 123.0);
                            object.put("bearing", (double) 348.7);
                            object.put("altitude", (double) 4321);
                            object.put("callsign", (String) "GHOST-1");
                            object.put("address", (int) 555);
                            object.put("time", (long) unixTime);
                        }
                        catch (JSONException e1) {
                            continue;
                        }
                        objs.add(object.toString());
                    }

                    if (unixTime - pt2 > 1000) {
                        pt2 = unixTime;
                        a = a + 0.001f;

                        object = new JSONObject();
                        //LongReportMessage tm = (LongReportMessage) m;
                        try {
                            object.put("type", "traffic");
                            object.put("longitude", (double) 115.7 + a / 5);
                            object.put("latitude", (double) -32.2 + a);
                            object.put("speed", (double) 246.0);
                            object.put("bearing", (double) 11.3);
                            object.put("altitude", (double) 7654);
                            object.put("callsign", (String) "GHOST-2");
                            object.put("address", (int) 666);
                            object.put("time", (long) unixTime);
                        }
                        catch (JSONException e1) {
                            continue;
                        }
                        objs.add(object.toString());
                    }
                    // debug >
                    //-------------------------------------*/

