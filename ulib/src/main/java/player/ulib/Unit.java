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

public class Unit
{

    public static final float NM_M = 1852f;
    public static final float NM_KM = 1.852f;
    public static final float NM_FT = 6076.115f;

    public static final float M_FT = 3.28084f;
    public static final float M_INCH = 39.37008f;

    //---------------------------------
    // Distance
    // Meter
    public static class Meter
    {
        public static float toNauticalMile(float m)
        {
            return m / NM_M;
        }

        public static float toFeet(float m)
        {
            return m * M_FT;
        }

        public static float toInch(float m)
        {
            return m * M_INCH;
        }
    }

    // Feet
    public static class Feet
    {
        public static float toMeter(float ft)
        {
            return ft / M_FT;
        }

        public static float toNauticalMile(float ft)
        {
            return ft / NM_FT;
        }
    }

    // Nautical Miles
    public static class NauticalMile
    {
        // m
        public static float toMeter(float nm)
        {
            return nm * NM_M;
        }

        // km
        public static float toKiloMeter(float nm)
        {
            return nm * NM_KM;
        }

        public static float toFeet(float nm)
        {
            return nm * NM_FT;
        }
    }

    //---------------------------------
    //Speed
    //

    // Meters per Second
    public static class MeterPerSecond
    {
        // kph
        public static float toKilometerPerHour(float ms)
        {
            return ms * 3.6f;
        }

        // kts
        public static float toKnots(float ms)
        {
            return ms * 1.943844f; // 3600 / NM_M = 1.943844f
        }

        // fps
        public static float toFeetPerSecond(float ms)
        {
            return ms * M_FT;
        }

        // fpm
        public static float toFeetPerMinute(float ms)
        {
            return ms * 196.8504f; // 60 * FT_M = 196.8504f
        }
    }

    // Knots (Nautical Mile Per Hour)
    public static class Knot {
        public static float toMeterPerSecond(float kt)
        {
            return kt * 0.5144444f; // NM_M / 3600 = 1.0.5144444f
        }

        public static float toKilometerPerHour(float kt)
        {
            return kt * NM_KM;
        }
    }
}
