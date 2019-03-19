/*
 * Copyright (C) 2017 Player One
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

// Standard imports

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;


/*

a
 +------------------------------------+
 |            b                       |     a = demTopLeftLat,  demTopLeftLon
 |             +---------+            |     b = x0, y0
 |             |         |            |     c = lat0, lon0
 |             |  c +    |            |
 |             |         |            |
 |             +---------+            |
 |                                    |
 |             |< BUFX  >|            |
 |                                    |
 +------------------------------------+

 Note: BUFX should fit completely in the DEM tile
       Consider this when choosing size
*/

public class DemGTOPO30
{
    public final static float DEM_HORIZON = 20; // nm
    public static final int BUFX = 600;   //800 = 400nm square ie at least  200nm in each direction
    public static final int BUFY = BUFX;
    static final int MAX_ELEV = 6000;  // in meters
    public static short buff[][] = new short[BUFX][BUFY];
    public static boolean demDataValid = false;
    public static float gamma = 1;
    static DemColor colorTbl[] = new DemColor[MAX_ELEV];  // 600*3 = r*3
    static float demTopLeftLat = -10;
    static float demTopLeftLon = +100;
    static private int x0;   // center of the BUFX tile
    static private int y0;   // center of the BUFX tile
    final int maxcol = 4800;
    final int maxrow = 6000;
    final int TILE_WIDTH = 40;     // width in degrees, must be integer
    final int TILE_HEIGHT = 50;    // height in degrees, must be integer
    public String region = "null.null";
    public float lat0;
    public float lon0;
    Context context;

    //-------------------------------------------------------------------------
    // Construct a new default loader with no flags set
    //
    public DemGTOPO30()
    {
    }

    public DemGTOPO30(Context context)
    {
        this.context = context;
        setGamma(1);
    }

    public static void setGamma(float g)
    {
        gamma = g;

        //for (short i = 0; i < colorTbl.length; i++) colorTbl[i] = calcColor(i);
        for (short i = 0; i < colorTbl.length; i++) colorTbl[i] = calcHSVColor(i); //optimal so far!
    }

    public static short getElev(float lat, float lon)
    {
        // Do we return bad data and let the program continue
        // or do we wait for valid data ?
        //while (!demDataValid)
        //  ; // do nothing and wait for valid data

        // check if buff is valid ?

        // *60 -> min * 2 -> 30 arcsec = 1/2 a min
        int y = (int) ((demTopLeftLat - lat) * 120) - y0;
        int x = (int) ((lon - demTopLeftLon) * 120) - x0;

        if ((x < 0) || (y < 0) || (x >= BUFX) || (y >= BUFY))
            return 0;
        else return buff[x][y];
    }

    //-------------------------------------------------------------------------
    // Utility function to calculate above ground altitude
    // in m using the DEM
    public static float calculateAgl(float lat, float lon, float alt)
    {
        if (demDataValid) return Math.max(0, alt - (int) (getElev(lat, lon)));
        else return 0;
    }


    public static DemColor getColor(short c)
    {
        if (c < MAX_ELEV)
            return colorTbl[c];
        else
            return colorTbl[MAX_ELEV - 1];
    }

    private static DemColor calcHSVColor(short c)
    {

        int r = 600;  // 600m=2000ft
        int MaxColor = 128;
        float hsv[] = {0, 0, 0};
        int colorBase;
        int min_v = 25;

        int v = MaxColor * c / r;

        if (v > 3 * MaxColor) {
            // mountain
            v %= MaxColor;
            colorBase = Color.rgb(MaxColor - v, MaxColor, MaxColor);
        } else if (v > 2 * MaxColor) {
            // highveld
            v %= MaxColor;
            colorBase = Color.rgb(MaxColor, MaxColor, v); // keep building to white
        } else if (v > MaxColor) {
            // inland
            v %= MaxColor;
            colorBase = Color.rgb(v, MaxColor, 0);
        } else if (v > 0) {
            // coastal plain
            if (v > min_v)
                colorBase = Color.rgb(0, v, 0);
            else {
                // close to the sea (lower than 25m),
                // is a special case otherwise it gets too dark
                colorBase = Color.rgb(min_v - v, min_v + (min_v - v), min_v - v);
            }
        } else {
            // the ocean
            colorBase = Color.rgb(0, 0, MaxColor); //bright blue ocean
        }

        // this allows us to adjust hue, sat and val
        Color.colorToHSV(colorBase, hsv);
        hsv[0] = hsv[0];  // hue 0..360
        hsv[1] = hsv[1];  // sat 0..1
        hsv[2] = hsv[2];  // val 0..1

        if (hsv[2] > 0.25) {
            hsv[0] = hsv[0] - ((hsv[2] - 0.25f) * 60);  // adjust the hue max 15%,  hue 0..360
            hsv[2] = 0.30f; // clamp the value, val 0..1
        }

        /*
        On HSV model, H (hue) define the base color, S (saturation) control the amount of gray
        and V controls the brightness. So, if you enhance V and decrease S at same time, you gets
        more luminance
        */
        hsv[1] = hsv[1] / gamma;  // sat 0..1
        hsv[2] = hsv[2] * gamma;  // val 0..1

        int color = Color.HSVToColor(hsv);
        return new DemColor((float) Color.red(color) / 255, (float) Color.green(color) / 255, (float) Color.blue(color) / 255);
    }

    //-----------------------------
    //color stuff - moved from renderer and renamed
    //
    private DemColor calcColor(short c)
    {
        float red = 0;
        float blue = 0;
        float green = 0;

        final float r = 600; // Earth mean terrain elevation is 840m
        final float max = 0.5f;
        final float max_green = max * 0.587f;
        final float min_green = 0.2f;

        // elevated terrain
        red = 0f;
        blue = 0f;
        green = (float) c / r;
        if (green > max) {
            green = max;
            red = (c - 1 * r) / r;
            if (red > max) {
                red = max;
                blue = (c - 2 * r) / r;
                if (blue > max) {
                    blue = max;
                }
            }
        }
        else if (green == 0) {
            // assume ocean
            green = 0;
            red = 0;
            blue = 0.26f;
        }
        else if (green < min_green) {
            // beach, special case
            red = min_green - green;
            blue = min_green - green;
            green = min_green + min_green - green;
        }

        // HSV  allows adjustment hue, sat and val
        float hsv[] = {0, 0, 0};
        int colorBase = Color.rgb((int) (red * 255), (int) (green * 255), (int) (blue * 255));
        Color.colorToHSV(colorBase, hsv);
        hsv[0] = hsv[0];  // hue 0..360
        hsv[1] = hsv[1];  // sat 0..1
        hsv[2] = hsv[2];  // val 0..1

        if (hsv[2] > 0.25) {
            hsv[0] = hsv[0] - ((hsv[2] - 0.25f) * 60);  // adjust the hue max 15%,  hue 0..360
            hsv[2] = 0.25f; // clamp the value, val 0..1
        }
        int color = Color.HSVToColor(hsv);

        return new DemColor((float) Color.red(color) / 255, (float) Color.green(color) / 255, (float) Color.blue(color) / 255);
    }
    //-----------------------------

    public void setBufferCenter(float lat, float lon)
    {
        lat0 = lat;
        lon0 = lon;
        x0 = (int) (Math.abs(lon0 - demTopLeftLon) * 60 * 2) - BUFX / 2;
        y0 = (int) (Math.abs(lat0 - demTopLeftLat) * 60 * 2) - BUFY / 2;
    }


    //-------------------------------------------------------------------------
    // use the lat lon to determine which tile is active
    //
    public String setDEMRegionTile(float lat, float lon)
    {
        demTopLeftLat = 90 - (int) (90 - lat) / TILE_HEIGHT * TILE_HEIGHT;
        demTopLeftLon = -180 + (int) (lon + 180) / TILE_WIDTH * TILE_WIDTH;

        return String.format("%c%03d%c%02d",
                demTopLeftLon < 0 ? 'W' : 'E',
                (int) Math.abs(demTopLeftLon),
                demTopLeftLat < 0 ? 'S' : 'N',
                (int) Math.abs(demTopLeftLat));

    }

    //-------------------------------------------------------------------------
    // use the lat lon to determine which region file is active
    //
    public String getRegionDatabaseName(float lat, float lon)
    {
        String sRegion = "null.null";

        if ((lat <= -10) && (lon > -20)) {
            sRegion = "zar.aus";
        }
        else if ((lat > 20) && (lon > -20)) {
            sRegion = "eur.rus";
        }
        else if ((lat > -10) && (lon <= -60)) {
            sRegion = "usa.can";
        }
        else if ((lat <= -10) && (lon <= -20)) {
            sRegion = "pan.arg";
        }
        else if ((lat <= 20) && (lon > -20)
                && (lat > -10)) {
            sRegion = "sah.jap";
        }

        return sRegion;
    }

    //-------------------------------------------------------------------------
    // Fill the entire with a single value
    //
    private void fillBuffer(short c)
    {
        for (int y = 0; y < BUFY; y++) {
            for (int x = 0; x < BUFX; x++) {
                buff[x][y] = c;  // fill in the buffer
            }
        }
    }

    //-------------------------------------------------------------------------
    // Check if a lat lon in valid
    //
    private boolean isValidLocation(float lat, float lon)
    {
        if (Math.abs(lat) > 90) return false;
        return !(Math.abs(lon) > 180);
    }

    //-------------------------------------------------------------------------
    // Check if a lat lon in on the current tile
    //
    public boolean isOnTile(float lat, float lon)
    {
        return (lat <= demTopLeftLat)
                && (lat > demTopLeftLat - TILE_HEIGHT)
                && (lon >= demTopLeftLon)
                && (lon < demTopLeftLon + TILE_WIDTH);
    }

    //-------------------------------------------------------------------------
    // Check if specific application is installed of not
    //
    private boolean isAppInstalledOrNot(String uri)
    {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            return true;
        }
        catch (PackageManager.NameNotFoundException e) {
        }
        return false;
    }


    public void loadDemBuffer(float lat, float lon)
    {
        demDataValid = false;

        // Automatic region determination with getRegionDatabaseName
        //   not 100% sure if this is such a good idea. It works but there
        //   are may be some some unintended behaviour. For now leave the code,
        //   but wpt.north.west disable the call here.
        region = getRegionDatabaseName(lat, lon);
        String DemFilename = setDEMRegionTile(lat, lon);
        setBufferCenter(lat, lon);

        // Check to see if player.efis.data.nnn.mmm (datapac) is installed
        if (!isAppInstalledOrNot("player.efis.data." + region)) {
            Toast.makeText(context, "DataPac (player.efis.data."
                    + region + ") not installed.\nSynthetic vision not available", Toast.LENGTH_LONG).show();
            return;
        }

        if (isValidLocation(lat, lon)) {
            Toast.makeText(context, "DEM terrain loading", Toast.LENGTH_SHORT).show();
            fillBuffer((short) 0);

            try {
                // We have 3 possible mechanisms for data. Leave them commented out
                // here as a reference for later. 
/*
                // read from local directory "/data/ ...
                File storage = Environment.getExternalStorageDirectory();
                File file = new File(storage + "/data/player.efis.pfd/terrain/" + DemFilename + ".DEM");
                FileInputStream inp = new FileInputStream(file);
                DataInputStream demFile = new DataInputStream(inp);
                //
*/

/*
                // read from local "assets"
                InputStream inp = context.getAssets().open("terrain/" + DemFilename + ".DEM");
                DataInputStream demFile = new DataInputStream(inp);
                //
*/

                // read from a datapac "assets"
                Context otherContext = context.createPackageContext("player.efis.data." + region, 0);
                InputStream inp = otherContext.getAssets().open("terrain/" + DemFilename + ".DEM");
                DataInputStream demFile = new DataInputStream(inp);

                final int NUM_BYTES_IN_SHORT = 2;
                short c;
                int x, y;
                int x1, y1, x2, y2;

                // buffer is wholly in tile
                x1 = x0;
                x2 = x0 + BUFX;
                y1 = y0;
                y2 = y0 + BUFY;

                // Handle borders
                // Buffer west and north
                if (x0 < 0) x1 = 0;
                if (y0 < 0) y1 = 0;

                // Buffer east and south
                if (x0 + BUFX > maxcol) x2 = maxcol;
                if (y0 + BUFY > maxrow) y2 = maxrow;

                demFile.skipBytes(NUM_BYTES_IN_SHORT * (maxcol * y1));
                demFile.skipBytes(NUM_BYTES_IN_SHORT * (x1));
                for (y = y1; y < y2; y++) {
                    for (x = x1; x < x2; x++) {
                        c = demFile.readShort();
                        // deliberately avoid 0
                        if (c > 0) buff[x - x0][y - y0] = c;
                        else buff[x - x0][y - y0] = 0;
                    }
                    demFile.skipBytes(NUM_BYTES_IN_SHORT * (maxcol - x2));
                    demFile.skipBytes(NUM_BYTES_IN_SHORT * (x1));
                }
                demFile.close();
                demDataValid = true;
            }
            catch (PackageManager.NameNotFoundException e) {
                // thrown by: context.createPackageContext
                Toast.makeText(context, "Data pac file not found: " + region, Toast.LENGTH_LONG).show();
                demDataValid = false;
                fillBuffer((short) 0);
                e.printStackTrace();
                // Try to fix the problem
                region = getRegionDatabaseName(lat, lon);
            }
            catch (IOException e) {
                // thrown by: otherContext.getAssets().open
                Toast.makeText(context, "Terrain file error: " + region + "/" + DemFilename, Toast.LENGTH_LONG).show();
                demDataValid = false;
                fillBuffer((short) 0);
                e.printStackTrace();
            }
            //catch (Exception e) { }
        }
        else {
            // Not a valid requested location
            demTopLeftLat = -9999;
            demTopLeftLon = -9999;
            x0 = -9999;
            y0 = -9999;
        }
    }
}
