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

import android.content.Context;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import player.ulib.UNavigation;
import player.ulib.UTrig;

public class OpenAir
{
    public static ArrayList<OpenAirRec> airspacelst = null;
    public String _region, region = "null";
    private Context context;

    //
    // Constructor
    //
    public OpenAir(Context context)
    {
        this.context = context;
        airspacelst = new ArrayList();
    }

    public static ArrayList<OpenAirRec> getAptSelect(float lat, float lon, int range, int nr)
    {
        //ArrayList<Apt> nearestAptList = null;
        ArrayList<OpenAirRec> nearestAptList = new ArrayList();

        for (OpenAirRec currProduct : airspacelst) {
            // add code to determine  the <nr> apts in range
            double deltaLat = lat - currProduct.clat;
            double deltaLon = lon - currProduct.clon;
            //double d =  Math.hypot(deltaLon, deltaLat);  // in degree, 1 deg = 60 nm
            double d = Math.sqrt(deltaLon * deltaLon + deltaLat * deltaLat);  // faster then hypot, see www

            if (d < range) {
                nearestAptList.add(currProduct);
            }
        }
        return nearestAptList;
    }
    // ------------------------------------------ */

    //-------------------------------------------------------------------------
    // use the lat lon to determine which region file is active
    //
    public String getRegionDatabaseName(float lat, float lon)
    {
        // TODO: 2017-10-03 make work for individual countries
        String sRegion = "null";
        if ((lat <= -10) && (lon > -20) && (lon < +60)) {
            sRegion = "zar";
        }
        else if ((lat <= -10) && (lon > +60) /*&& (lon < 60)*/) {
            sRegion = "aus";
        }
        else if ((lat > +20) && (lon >= -20) && (lon < +30)) {
            sRegion = "eur";
        }
        else if ((lat > +20) && (lon >= -20) && (lon > +30)) {
            sRegion = "rus";
        }
        else if ((lat > -10) && (lon < -60) && (lat > +25) && (lat < +49)) {
            sRegion = "usa";
        }
        else if ((lat > -10) && (lon < -60) && (lat > +49)) {
            sRegion = "can";
        }
        return sRegion;
    }

    ///*
    public final boolean Parse(DataInputStream reader)//, OperationEnvironment operation)
    {
        boolean ignore = false;

        float arcLat = 0;
        float arcLon = 0;
        int angleInc = +10; // deg clockwise
        int angleDir = +1;

        String line;

        // Iterate through the lines
        try {
            OpenAirRec rec = null;

            while ((line = reader.readLine()) != null) {
                //android.util.Log.d("mmap", line);
                line = line.toUpperCase();
                line.trim();

                // Skip empty lines and comments
                if (line.contains("*")) line = line.substring(0, line.indexOf('*'));
                if (line.isEmpty() || line.startsWith("*")) {
                    continue;
                }

                if (line.startsWith("AC")) {
                    if (rec != null) airspacelst.add(rec);
                    rec = new OpenAirRec();
                    rec.ac = line.substring(3);
                }
                else if (line.startsWith("AN")) {
                    rec.an = line.substring(3);
                }
                else if (line.startsWith("AL")) {
                    if (line.contains("FL")) {
                        String s = line.replaceAll("[^0-9]", "");
                        rec.al = 100 * Integer.valueOf(s);
                    }
                    if (line.contains("FT")) {
                        String s = line.replaceAll("[^0-9]", "");
                        rec.al = Integer.valueOf(s);
                    }
                }
                else if (line.startsWith("AH")) {
                    if (line.contains("FL")) {
                        String s = line.replaceAll("[^0-9]", "");
                        rec.ah = 100 * Integer.valueOf(s);
                    }
                    if (line.contains("FT")) {
                        String s = line.replaceAll("[^0-9]", "");
                        rec.ah = Integer.valueOf(s);
                    }
                }
                // DP = Polygon point
                else if (line.startsWith("DP")) {
                    String s = line.substring(3).replaceAll(" ", "");  // kill spaces
                    String slat = s.split("[NS]")[0];
                    String slon = s.split("[NS]")[1].split("[EW]")[0];
                    float lat = UNavigation.DMStoD(slat);
                    if (s.contains("S")) lat = -lat;
                    float lon = UNavigation.DMStoD(slon);
                    if (s.contains("W")) lon = -lon;
                    OpenAirPoint pnt = new OpenAirPoint(lat, lon);
                    rec.pointList.add(pnt);
                    //
                    rec.clat = lat;
                    rec.clon = lon;
                }
                // DB = Arc from point 1 to point 2 centered on V X=
                else if (line.startsWith("DB")) {
                    String s0 = line.substring(3).replaceAll(" ", "");  // kill spaces
                    String s[] = s0.split(",");

                    // Point 1
                    String slat = s[0].split("[NS]")[0];
                    String slon = s[0].split("[NS]")[1].split("[EW]")[0];
                    float lat1 = UNavigation.DMStoD(slat);
                    if (s[0].contains("S")) lat1 = -lat1;
                    float lon1 = UNavigation.DMStoD(slon);
                    if (s[0].contains("W")) lon1 = -lon1;

                    float dme = UNavigation.calcDme(arcLat, arcLon, lat1, lon1);
                    float absBrg1 = UNavigation.calcAbsBrg(arcLat, arcLon, lat1, lon1);

                    OpenAirPoint pnt = new OpenAirPoint(lat1, lon1);
                    rec.pointList.add(pnt);

                    // Point 2
                    slat = s[1].split("[NS]")[0];
                    slon = s[1].split("[NS]")[1].split("[EW]")[0];
                    float lat2 = UNavigation.DMStoD(slat);
                    if (s[1].contains("S")) lat2 = -lat2;
                    float lon2 = UNavigation.DMStoD(slon);
                    if (s[1].contains("W")) lon2 = -lon2;
                    float absBrg2 = UNavigation.calcAbsBrg(arcLat, arcLon, lat2, lon2);

                    float lat, lon;
                    absBrg1 = UNavigation.compassRose180(absBrg1);
                    absBrg2 = UNavigation.compassRose180(absBrg2);

                    // do the arc
                    if (angleDir == -1) {
                        for (float i = absBrg1; i >= absBrg2; i = i - angleInc) {
                            lat = arcLat + dme / 60f * UTrig.isin(90 - (int) i);
                            lon = arcLon + dme / 60f * UTrig.icos(90 - (int) i);

                            pnt = new OpenAirPoint(lat, lon);
                            rec.pointList.add(pnt);
                        }
                    }
                    if (angleDir == +1) {
                        for (float i = absBrg1; i <= absBrg2; i = i + angleInc) {
                            lat = arcLat + dme / 60f * UTrig.isin(90 - (int) i);
                            lon = arcLon + dme / 60f * UTrig.icos(90 - (int) i);

                            pnt = new OpenAirPoint(lat, lon);
                            rec.pointList.add(pnt);
                        }
                    }

                    // Add point 2
                    pnt = new OpenAirPoint(lat2, lon2);
                    rec.pointList.add(pnt);
                }
                // DC = Circle centered on V X=
                else if (line.startsWith("DC")) {
                    // TODO
                    String s = line.substring(3).replaceAll(" ", "");  // kill spaces
                    float dme = Float.valueOf(s);

                    float lat, lon;
                    for (float i = 0; i <= 360; i = i + angleInc) {
                        lat = arcLat + dme / 60f * UTrig.isin(90 - (int) i);
                        lon = arcLon + dme / 60f * UTrig.icos(90 - (int) i);

                        OpenAirPoint pnt = new OpenAirPoint(lat, lon);
                        rec.pointList.add(pnt);
                    }
                }
                // V D = Arc direction
                else if (line.startsWith("V D=")) {
                    if (line.contains("-")) {
                        angleDir = -1; // deg counterclockwise
                    }
                    if (line.contains("+")) {
                        angleDir = +1; // deg clockwise
                    }
                }
                // V X = Arc center
                else if (line.startsWith("V X=")) {
                    String s = line.substring(4).replaceAll(" ", "");  // kill spaces
                    String slat = s.split("[NS]")[0];
                    String slon = s.split("[NS]")[1].split("[EW]")[0];
                    arcLat = UNavigation.DMStoD(slat);
                    if (s.contains("S")) arcLat = -arcLat;
                    arcLon = UNavigation.DMStoD(slon);
                    if (s.contains("W")) arcLon = -arcLon;
                }

/*
                if (filetype == AirspaceFileType.UNKNOWN) {
                    filetype = GlobalMembers.DetectFileType(line);
                }
*/

                // Parse the line
/*
                if (filetype == AirspaceFileType.OPENAIR) {
                    tangible.RefObject<String> tempRef_line = new tangible.RefObject<String>(line);
                    if (!GlobalMembers.ParseLine(airspaces, tempRef_line, temp_area)
                            && !GlobalMembers.ShowParseWarning(line_num, line, operation)) {
                        line = tempRef_line.argValue;
                        return false;
                    } else {
                        line = tempRef_line.argValue;
                    }
                }
*/

/*
                if (filetype == AirspaceFileType.TNP) {
                    StringParser<Byte> input = new StringParser<Byte>(line);
                    tangible.RefObject<Boolean> tempRef_ignore = new tangible.RefObject<Boolean>(ignore);
                    if (!GlobalMembers.ParseLineTNP(airspaces, input, temp_area, tempRef_ignore)
                            && !GlobalMembers.ShowParseWarning(line_num, line, operation)) {
                        ignore = tempRef_ignore.argValue;
                        return false;
                    } else {
                        ignore = tempRef_ignore.argValue;
                    }
                }
*/
                // Update the ProgressDialog
/*
                if ((line_num & 0xff) == 0) {
                    operation.SetProgressPosition(reader.Tell() * 1024 / file_size);
                }
*/
            }
            // Add the very last rec
            if (rec != null) airspacelst.add(rec);

        }
        catch (IOException e) {
            e.printStackTrace();
        }

/*
        if (filetype == AirspaceFileType.UNKNOWN) {
            operation.SetErrorMessage(("Unknown airspace filetype"));
            return false;
        }
*/

        // Process final area (if any)
        //temp_area.AddPolygon(airspaces);
        // openairList.add() // todo

        return true;
    }

    public void loadDatabase(float lat, float lon)
    {
        region = getRegionDatabaseName(lat, lon);
        if (!region.equals(_region))
            loadDatabase(region);
        _region = region;
    }

    public void loadDatabase(String database)
    {
        region = database;

        airspacelst.clear();
        try {
            //InputStream in_s = context.getAssets().open(region + "/airspace.txt.air");
            InputStream in_s = context.getAssets().open("airspace/" + region + ".txt.air");

            DataInputStream in = new DataInputStream(in_s);
            Parse(in);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printProducts(ArrayList<OpenAirRec> list)
    {
        String content = "";
        for (OpenAirRec currProduct : list) {
            //content = content + "\nName :" + currProduct.name + "\n";
            //content = content + "Cmt :" + currProduct.cmt + "\n";
            //content = content + "Color :" +  currProduct.wpt + "n";
            System.out.println(content);
        }
        //Log.v("b2", "b2 - " + content);
        //TextView display = (TextView) findViewById(R.id.info);
        //display.setText(content);
    }
}
