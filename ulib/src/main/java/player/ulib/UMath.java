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

public class UMath {

    //-------------------------------------------------------------------------
    // determine whether two numbers are "approximately equal" by seeing if they
    // are within a certain "tolerance percentage," with `tolerancePercentage` given
    // as a percentage (such as 10.0 meaning "10%").
    //
    // @param tolerancePercentage 1 = 1%, 2.5 = 2.5%, etc.
    //
    public static boolean approximatelyEqual(float desiredValue, float actualValue, float tolerancePercentage)
    {
        float diff = Math.abs(desiredValue - actualValue);         //  1000 - 950  = 50
        float tolerance = tolerancePercentage / 100 * desiredValue;  //  20/100*1000 = 200
        return diff < tolerance;                                   //  50<200      = true
    }

    public static float clamp(float val, float min, float max)
    {
        return Math.max(min, Math.min(max, val));
    }

    public static float round(float value, int precision)
    {
        int scale = (int) Math.pow(10, precision);
        return (float) Math.round(value * scale) / scale;
    }

    public static double round(double value, int precision)
    {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }
}
