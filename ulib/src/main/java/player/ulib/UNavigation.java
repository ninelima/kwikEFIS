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

package player.ulib;

public class UNavigation {


    //-------------------------------------------------------------------------
    // Calculate the DME distance in nm
    //
    public static float calcDme(float lat1, float lon1, float lat2, float lon2)
    {
        float deltaLat = lat2 - lat1;
        float deltaLon = lon2 - lon1;

        //d =  364800 * Math.hypot(deltaLon, deltaLat);  // in ft, 1 deg of lat  6080 * 60 = 364,80 note hypot uses convergenge and is very slow.
        return (float) (60 * Math.sqrt(deltaLon * deltaLon + deltaLat * deltaLat));

    }

    //-------------------------------------------------------------------------
    // Calculate the Relative Bearing in degrees
    //
    public static float calcRelBrg(float lat1, float lon1, float lat2, float lon2, float hdg)
    {
        float deltaLat = lat2 - lat1;
        float deltaLon = lon2 - lon1;

        float relBrg = (float) (Math.toDegrees(Math.atan2(deltaLon, deltaLat)) - hdg) % 360;  // the relative bearing to the apt
        if (relBrg > 180) relBrg = relBrg - 360;
        if (relBrg < -180) relBrg = relBrg + 360;
        return relBrg;
    }


    //-------------------------------------------------------------------------
    // Calculate the Absolute Bearing in degrees
    //
    public static float calcAbsBrg(float lat1, float lon1, float lat2, float lon2)
    {
        float deltaLat = lat2 - lat1;
        float deltaLon = lon2 - lon1;

        float absBrg = (float) (Math.toDegrees(Math.atan2(deltaLon, deltaLat))) % 360;
        while (absBrg < 0) absBrg += 360;
        return absBrg;
    }

    //-------------------------------------------------------------------------
    // Calculate the Distance to the Horizon in m
    // h in meters
    //
    public static float calcHorizonMetric(float h)  // in m
    {
        return (float) (3570 * Math.sqrt(h)); // in m
    }


    //-------------------------------------------------------------------------
    // Calculate the Distance to the Horizon in nm
    // h in ft
    //
    public static float calcHorizonNautical(float h) // in ft
    {
        return (float) Math.sqrt(h); // in nm
    }

    //-------------------------------------------------------------------------
    // Utility function to normalize an angle from any angle to
    // 0 to +180 and 0 to -180
    //
    public static float compassRose180(float angle)
    {
        angle = (angle) % 360;

        if (angle > 180) angle = angle - 360;
        if (angle < -180) angle = angle + 360;
        return angle;
    }

    //-------------------------------------------------------------------------
    // Utility function to normalize an angle from any angle to
    // 0 to +360
    //
    public static float compassRose360(float angle)
    {
        angle = (angle) % 360;

        if (angle < 0) angle = angle + 360;
        return angle;
    }

    //-------------------------------------------------------------------------
    // Utility function to convert a DMS string to a Decimal D (float)
    //Eg. 36:24:22 = 36.4061
    //
    public static float DMStoD(String dms)
    {
        float d = Float.valueOf(dms.split(":")[0]);
        float m = Float.valueOf(dms.split(":")[1]);
        float s = Float.valueOf(dms.split(":")[2]);
        return d + m / 60f + s / 3600f;
    }
}
