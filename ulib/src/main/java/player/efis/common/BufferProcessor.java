/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted
provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions
    * and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of
    *     conditions and the following disclaimer in the documentation and/or other materials
    *     provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
    *     AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
    *     WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
    *     IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
    *     INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
    *     PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
    *     INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
    *     LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
    *     OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package player.efis.common;

import com.stratux.stratuvare.gdl90.BasicReportMessage;
import com.stratux.stratuvare.gdl90.Constants;
import com.stratux.stratuvare.gdl90.DataBuffer;
import com.stratux.stratuvare.gdl90.Decode;
import com.stratux.stratuvare.gdl90.FisBuffer;
import com.stratux.stratuvare.gdl90.HeartbeatMessage;
import com.stratux.stratuvare.gdl90.Id413Product;
import com.stratux.stratuvare.gdl90.Id6364Product;
import com.stratux.stratuvare.gdl90.LongReportMessage;
import com.stratux.stratuvare.gdl90.OwnshipGeometricAltitudeMessage;
import com.stratux.stratuvare.gdl90.OwnshipMessage;
import com.stratux.stratuvare.gdl90.Product;
import com.stratux.stratuvare.gdl90.TrafficReportMessage;
import com.stratux.stratuvare.gdl90.UplinkMessage;
import com.stratux.stratuvare.nmea.Ownship;
import com.stratux.stratuvare.nmea.RTMMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;

/**
 * @author zkhan
 */
public class BufferProcessor
{

    DataBuffer dbuffer =
            new DataBuffer(16384);
    com.stratux.stratuvare.nmea.DataBuffer nbuffer =
            new com.stratux.stratuvare.nmea.DataBuffer(16384);
    Decode decode =
            new Decode();
    com.stratux.stratuvare.nmea.Decode ndecode =
            new com.stratux.stratuvare.nmea.Decode();
    Ownship nmeaOwnship = new Ownship();

    /**
     * @param buffer
     * @param red
     */
    public void put(byte[] buffer, int red)
    {
        nbuffer.put(buffer, red);
        dbuffer.put(buffer, red);
    }

    /**
     * @return
     */
    //public LinkedList<String> decode(Preferences pref) {
    public LinkedList<String> decode()
    {

        LinkedList<String> objs = new LinkedList<>();

        byte[] buf;

        while (null != (buf = nbuffer.get())) {
            com.stratux.stratuvare.nmea.Message m = ndecode.decode(buf);
            if (m instanceof RTMMessage) {
                // Make a GPS location message from ADSB ownship message.
                JSONObject object = new JSONObject();
                RTMMessage tm = (RTMMessage) m;
                try {
                    object.put("type", "traffic");
                    object.put("longitude", (double) tm.mLon);
                    object.put("latitude", (double) tm.mLat);
                    object.put("speed", (double) (tm.mSpeed));
                    object.put("bearing", (double) tm.mDirection);
                    object.put("altitude", ((double) tm.mAltitude));
                    object.put("callsign", "");
                    object.put("address", tm.mIcaoAddress);
                    object.put("time", tm.getTime());
                }
                catch (JSONException e1) {
                    continue;
                }
                objs.add(object.toString());

            }
            else if (nmeaOwnship.addMessage(m)) {

                /*
                 * Make a GPS location message from ADSB ownship message.
                 */
                JSONObject object = new JSONObject();
                Ownship om = nmeaOwnship;
                try {
                    object.put("type", "ownship");
                    object.put("longitude", (double) om.mLon);
                    object.put("latitude", (double) om.mLat);
                    object.put("speed", (double) (om.mHorizontalVelocity));
                    object.put("bearing", (double) om.mDirection);
                    object.put("altitude", ((double) om.mAltitude));
                    object.put("time", om.getTime());
                }
                catch (JSONException e1) {
                    continue;
                }
                objs.add(object.toString());
            }
        }

        while (null != (buf = dbuffer.get())) {
            // * Get packets, decode
            com.stratux.stratuvare.gdl90.Message m = decode.decode(buf);
            // * Post on UI thread.
            if (m instanceof TrafficReportMessage) {

                // * Make a GPS location message from ADSB ownship message.
                JSONObject object = new JSONObject();
                TrafficReportMessage tm = (TrafficReportMessage) m;
                try {
                    object.put("type", "traffic");
                    object.put("longitude", (double) tm.mLon);
                    object.put("latitude", (double) tm.mLat);
                    object.put("speed", (double) (tm.mHorizVelocity));
                    object.put("bearing", (double) tm.mHeading);
                    object.put("altitude", ((double) tm.mAltitude));
                    object.put("callsign", tm.mCallSign);
                    object.put("address", tm.mIcaoAddress);
                    object.put("time", tm.getTime());
                }
                catch (JSONException e1) {
                    continue;
                }
                objs.add(object.toString());
            }
            else if (m instanceof BasicReportMessage) {

                // * Make a GPS location message from ADSB ownship message.
                JSONObject object = new JSONObject();
                BasicReportMessage tm = (BasicReportMessage) m;
                try {
                    object.put("type", "traffic");
                    object.put("longitude", tm.mLon);
                    object.put("latitude", tm.mLat);
                    object.put("speed", (double) (tm.mSpeed));
                    object.put("bearing", tm.mHeading);
                    object.put("altitude", ((double) tm.mAltitude));
                    object.put("callsign", tm.mCallSign);
                    object.put("address", tm.mIcaoAddress);
                    object.put("time", tm.getTime());
                }
                catch (JSONException e1) {
                    continue;
                }

                objs.add(object.toString());
            }
            else if (m instanceof LongReportMessage) {
                // Make a GPS location message from ADSB ownship message. xxxx
                JSONObject object = new JSONObject();
                LongReportMessage tm = (LongReportMessage) m;
                try {
                    object.put("type", "traffic");
                    object.put("longitude", tm.mLon);
                    object.put("latitude", tm.mLat);
                    object.put("speed", (double) (tm.mSpeed));
                    object.put("bearing", tm.mHeading);
                    object.put("altitude", ((double) tm.mAltitude));
                    object.put("callsign", tm.mCallSign);
                    object.put("address", tm.mIcaoAddress);
                    object.put("time", tm.getTime());
                } catch (JSONException e1) {
                    continue;
                }
                objs.add(object.toString());
            }
            else if (m instanceof OwnshipGeometricAltitudeMessage) {
                JSONObject object = new JSONObject();
                try {
                    object.put("type", "geoaltitude");

                    int altitude = ((OwnshipGeometricAltitudeMessage) m).mAltitudeWGS84;
                    if (altitude == Integer.MIN_VALUE) {
                        // invalid
                        continue;
                    }
                    object.put("altitude", (double) altitude);
                    object.put("time", m.getTime());
                }
                catch (JSONException e1) {
                    continue;
                }
                objs.add(object.toString());
            }
            else if (m instanceof UplinkMessage) {
                //
                // Send an uplink nexrad message
                //
                float lat, lon;
                int TISid;
                FisBuffer fis = ((UplinkMessage) m).getFis();
                if (null == fis) {
                    continue;
                }
                // Get the tower longitude/latitude from the FisBuffer for passing to Stratuvare
                lat = fis.getLat();
                lon = fis.getLon();
                TISid = fis.getTISid();
                LinkedList<Product> pds = fis.getProducts();
                for (Product p : pds) {
                    if (p instanceof Id6364Product) {
                        Id6364Product pn = (Id6364Product) p;
                        JSONObject object = new JSONObject();

                        JSONArray arrayEmpty = new JSONArray();
                        JSONArray arrayData = new JSONArray();

                        int[] data = pn.getData();
                        if (null != data) {
                            for (int aData : data) {
                                arrayData.put(aData);
                            }
                        }
                        LinkedList<Integer> empty = pn.getEmpty();
                        if (null != empty) {
                            for (int e : empty) {
                                arrayEmpty.put(e);
                            }
                        }

                        try {
                            object.put("type", "nexrad");
                            object.put("time", pn.getTime().getTimeInMillis());
                            object.put("conus", pn.isConus());
                            object.put("blocknumber", (long) pn.getBlockNumber());
                            object.put("x", Constants.COLS_PER_BIN);
                            object.put("y", Constants.ROWS_PER_BIN);
                            object.put("empty", arrayEmpty);
                            object.put("data", arrayData);
                        }
                        catch (JSONException e1) {
                            continue;
                        }

                        objs.add(object.toString());
                    }
                    /*
                     * Text product
                     */

                    else if (p instanceof Id413Product) {
                        Id413Product pn = (Id413Product) p;
                        JSONObject object = new JSONObject();

                        StringBuilder data = new StringBuilder(pn.getData());
                        String type = pn.getHeader();
                        long time = pn.getTime().getTimeInMillis();

                        /*
                         * Clear garbage spaces etc. Convert to Stratuvare format
                         */

                        try {
/*
                            if (type.equals("METAR") || type.equals("SPECI")) {
                                object.put("flight_category", MetarFlightCategory.getFlightCategory(pn.getLocation(), pn.getData()));
                            }
*/
                            if (type.equals("WINDS")) {

                                String tokens[] = data.toString().split("\n");
                                if (tokens.length < 2) {
                                    // * Must have line like
                                    // * MSY 230000Z  FT 3000 6000    F9000   C12000  G18000  C24000  C30000  D34000  39000   Y
                                    // * and second line like
                                    // * 1410 2508+10 2521+07 2620+01 3037-12 3041-26 304843 295251 29765
                                    continue;
                                }

                                tokens[0] = tokens[0].replaceAll("\\s+", " ");
                                tokens[1] = tokens[1].replaceAll("\\s+", " ");
                                String winds[] = tokens[1].split(" ");
                                String alts[] = tokens[0].split(" ");

                                /*
                                 * Start from 3rd entry - alts
                                 */
                                data = new StringBuilder();
                                boolean found = false;
                                for (int i = 2; i < alts.length; i++) {
                                    if (alts[i].contains("3000") && !alts[i].contains("30000")) {
                                        data.append(winds[i - 2]).append(",");
                                        found = true;
                                    }
                                }
                                if (!found) {
                                    data.append(",");
                                }
                                found = false;
                                for (int i = 2; i < alts.length; i++) {
                                    if (alts[i].contains("6000")) {
                                        data.append(winds[i - 2]).append(",");
                                        found = true;
                                    }
                                }
                                if (!found) {
                                    data.append(",");
                                }
                                found = false;
                                for (int i = 2; i < alts.length; i++) {
                                    if (alts[i].contains("9000") && !alts[i].contains("39000")) {
                                        data.append(winds[i - 2]).append(",");
                                        found = true;
                                    }
                                }
                                if (!found) {
                                    data.append(",");
                                }
                                found = false;
                                for (int i = 2; i < alts.length; i++) {
                                    if (alts[i].contains("12000")) {
                                        data.append(winds[i - 2]).append(",");
                                        found = true;
                                    }
                                }
                                if (!found) {
                                    data.append(",");
                                }
                                found = false;
                                for (int i = 2; i < alts.length; i++) {
                                    if (alts[i].contains("18000")) {
                                        data.append(winds[i - 2]).append(",");
                                        found = true;
                                    }
                                }
                                if (!found) {
                                    data.append(",");
                                }
                                found = false;
                                for (int i = 2; i < alts.length; i++) {
                                    if (alts[i].contains("24000")) {
                                        data.append(winds[i - 2]).append(",");
                                        found = true;
                                    }
                                }
                                if (!found) {
                                    data.append(",");
                                }
                                found = false;
                                for (int i = 2; i < alts.length; i++) {
                                    if (alts[i].contains("30000")) {
                                        data.append(winds[i - 2]).append(",");
                                        found = true;
                                    }
                                }
                                if (!found) {
                                    data.append(",");
                                }
                                found = false;
                                for (int i = 2; i < alts.length; i++) {
                                    if (alts[i].contains("34000")) {
                                        data.append(winds[i - 2]).append(",");
                                        found = true;
                                    }
                                }
                                if (!found) {
                                    data.append(",");
                                }
                                found = false;
                                for (int i = 2; i < alts.length; i++) {
                                    if (alts[i].contains("39000")) {
                                        data.append(winds[i - 2]).append(",");
                                        found = true;
                                    }
                                }
                                if (!found) {
                                    data.append(",");
                                }
                            }
                        }
                        catch (Exception e) {
                            continue;
                        }

                        try {
                            object.put("type", pn.getHeader());
                            object.put("time", time);
                            object.put("location", pn.getLocation());
                            object.put("data", data.toString());
                            object.put("towerlon", lon);
                            object.put("towerlat", lat);
                            object.put("tisid", TISid);
                        }
                        catch (JSONException e1) {
                            continue;
                        }
                        objs.add(object.toString());
                    }
                }
            }
            else if (m instanceof OwnshipMessage) {

                /*
                 * Make a GPS location message from ADSB ownship message.
                 */
                JSONObject object = new JSONObject();
                OwnshipMessage om = (OwnshipMessage) m;
                try {
                    object.put("type", "ownship");
                    object.put("longitude", (double) om.mLon);
                    object.put("latitude", (double) om.mLat);
                    object.put("speed", (double) (om.mHorizontalVelocity));
                    object.put("bearing", (double) om.mDirection);
                    object.put("time", om.getTime());
                    object.put("altitude", (double) om.mAltitude);
                }
                catch (JSONException e1) {
                    continue;
                }
                objs.add(object.toString());
            }
            // b2 start
            else if (m instanceof HeartbeatMessage) {

                /*
                int mHour;
                int mMinute;
                int mSecond;
                Boolean mGpsPositionValid;
                Boolean mBatteryLow;
                Boolean mDeviceRunning;
                */

                JSONObject object = new JSONObject();
                HeartbeatMessage om = (HeartbeatMessage) m;
                try {
                    object.put("type", "heartbeat");
                    //object.put("hour", (int) om.mHour);
                    //object.put("minute", (double) om.mLat);
                    //object.put("second", (double) (om.mHorizontalVelocity));

                    object.put("timestamp", om.mTimeStamp);
                    object.put("gpsvalid", (boolean) om.mGpsPositionValid);
                    object.put("lowbattery", (boolean) om.mBatteryLow);
                    object.put("running", (boolean) om.mDeviceRunning);
                }
                catch (JSONException e1) {
                    continue;
                }
                objs.add(object.toString());
            }
            // b2 start

        }
        return objs;
    }
}
