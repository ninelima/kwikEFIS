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

import android.hardware.SensorManager;

import java.util.Timer;
import java.util.TimerTask;

import player.ulib.DigitalFilter;

public class SensorComplementaryFilter
{
    private final static float dt = 0.01f;    // 10 ms sample rate
    public static final int TIME_CONSTANT = (int) (dt * 1000);
    private final float ACCELEROMETER_SENSITIVITY = 1.0f;
    private final float GYROSCOPE_SENSITIVITY = 1.0f;
    orientation_t orientation = orientation_t.VERTICAL_LANDSCAPE;
    float[] accData = {0, 0, 0};
    float[] gyrData = {0, 0, 0};
    //
    // Calculate augmented bank angle given rate of turn and velocity
    //
    DigitalFilter filterRollAcc = new DigitalFilter(32); //4
    private float M_PI = 3.14159265359f;
    private float pitch;
    private float roll;
    private float pitchAcc, rollAcc;
    private float loadfactor;

    public SensorComplementaryFilter()
    {
        // wait for two second until gyroscope and magnetometer/accelerometer
        // data is initialised then schedule the complementary filter task
        Timer complTimer = new Timer();
        complTimer.scheduleAtFixedRate(new calculateFilterTask(), 2000, TIME_CONSTANT);
    }

    public void setOrientation(orientation_t ori)
    {
        orientation = ori;
    }

    public void setAccel(float[] sensorValues)
    {
        System.arraycopy(sensorValues, 0, accData, 0, 3);
        loadfactor = (float) Math.sqrt(accData[0] * accData[0] + accData[1] * accData[1] + accData[2] * accData[2]) / SensorManager.GRAVITY_EARTH;
    }

    public void getAccel(float[] sensorValues)
    {
        System.arraycopy(accData, 0, sensorValues, 0, 3);
    }

    public void setGyro(float[] sensorValues)
    {
        System.arraycopy(sensorValues, 0, gyrData, 0, 3);
    }

    public void getGyro(float[] sensorValues)
    {
        System.arraycopy(gyrData, 0, sensorValues, 0, 3);
    }

    public float getPitch() {
        return pitch;
    }

    public float getPitchRate()
    {
        switch (orientation) {
            case HORIZONTAL_LANDSCAPE:
                return (gyrData[1] * 180f / M_PI) * dt;  // Angle around the Y-axis

            case VERTICAL_LANDSCAPE:
                return (gyrData[1] * 180f / M_PI) * dt; // Angle around the X-axis

            default:
                return 0;
        }
    }

    public float getRoll()
    {
        return roll;
    }

    public float getPitchAcc()
    {
        return pitchAcc;
    }

    public float getRollAcc()
    {
        return rollAcc;
    }

    public float getLoadFactor()
    {
        return loadfactor;
    }

    public void primePitch()
    {
        pitch = pitchAcc;
    }

    public void primeRoll()
    {
        roll = rollAcc;
    }

    //
    // Utility for calculation of bank based on rate of turn and speed.
    // Correction for slip is made using the accelerometer data
    //
    public float calculateBankAngle(float rot, float speed)
    {
        float bank = 0;

        if (speed > 2.0) {
            // Bank angle using the speed
            // For a coordinated turn:
            //   tan (b) = w * v / g
            float roll_centripetal = (float) (Math.atan2(rot * speed, SensorManager.GRAVITY_EARTH) * 180 / Math.PI);

            bank = roll_centripetal;  // no corrections

            // Apply in a correction for any slip / skid
            float roll_accel = filterRollAcc.runningAverage(this.getRollAcc());
            bank = (roll_centripetal - roll_accel);  // correct slip with acceleration sensor value
        }
        return bank;
    }

    //
    // Utility for calculation of pitch based on rate of climb and speed.
    //
    public float calculatePitchAngle(float roc, float speed)
    {
        float pitch = 0;
        pitch = (float) (Math.atan2(roc, speed) * 180.0f / Math.PI);

        return pitch;
    }

    // Free running task implementing  the actual
    // complimentary filter
    class calculateFilterTask extends TimerTask
    {
        public void run()
        {
            //0.98f is the default
            float ALPHA = 0.998f;
            switch (orientation) {
                case HORIZONTAL_LANDSCAPE:
                    // Integrate the gyroscope data -> int(angularSpeed) = angle
                    roll += (gyrData[0] * 180 / M_PI) * dt;   // Angle around the X-axis
                    pitch -= (gyrData[1] * 180 / M_PI) * dt;  // Angle around the Y-axis

                    // Turning around the X axis results in a vector on the Y-axis
                    rollAcc = (float) (Math.atan2(accData[1], accData[2]) * 180 / M_PI);
                    roll = roll * ALPHA + rollAcc * (1 - ALPHA);

                    // Turning around the Y axis results in a vector on the X-axis
                    pitchAcc = (float) (Math.atan2(accData[0], accData[2]) * 180 / M_PI);
                    pitch = pitch * ALPHA + pitchAcc * (1 - ALPHA);
                    break;

                case VERTICAL_LANDSCAPE:
                    // Integrate the gyroscope data -> int(angularSpeed) = angle
                    roll += (gyrData[2] * 180 / M_PI) * dt;  // Angle around the Z-axis
                    pitch += (gyrData[1] * 180 / M_PI) * dt;  // Angle around the Y-axis

                    // Turning around the Z axis results in a vector on the X-axis
                    rollAcc = (float) -(Math.atan2(accData[1], accData[0]) * 180 / M_PI);
                    roll = roll * ALPHA + rollAcc * (1 - ALPHA);

                    // Turning around the X axis results in a vector on the Y-axis
                    pitchAcc = (float) (Math.atan2(accData[2], accData[0]) * 180 / M_PI);
                    pitch = pitch * ALPHA + pitchAcc * (1 - ALPHA);
                    break;
                case VERTICAL_PORTRAIT:
                    // Integrate the gyroscope data -> int(angularSpeed) = angle
                    roll += (gyrData[2] * 180 / M_PI) * dt;  // Angle around the Z-axis
                    pitch -= (gyrData[0] * 180 / M_PI) * dt;  // Angle around the X-axis

                    // Turning around the Z axis results in a vector on the X-axis
                    rollAcc = (float) +(Math.atan2(accData[0], accData[1]) * 180 / M_PI);
                    roll = roll * ALPHA + rollAcc * (1 - ALPHA);

                    // Turning around the X axis results in a vector on the Y-axis
                    pitchAcc = (float) (Math.atan2(accData[2], accData[1]) * 180 / M_PI);
                    pitch = pitch * ALPHA + pitchAcc * (1 - ALPHA);
                    break;
                case HORIZONTAL_PORTRAIT:
                default:
                    // Integrate the gyroscope data -> int(angularSpeed) = angle
                    roll += (gyrData[1] * 180 / M_PI) * dt;  // Angle around the Y-axis
                    pitch += (gyrData[0] * 180 / M_PI) * dt; // Angle around the X-axis

                    // Turning around the Y axis results in a vector on the X-axis
                    rollAcc = (float) -(Math.atan2(accData[0], accData[2]) * 180 / M_PI);
                    roll = roll * ALPHA + rollAcc * (1 - ALPHA);

                    // Turning around the X axis results in a vector on the Y-axis
                    pitchAcc = (float) (Math.atan2(accData[1], accData[2]) * 180 / M_PI);
                    pitch = pitch * ALPHA + pitchAcc * (1 - ALPHA);
                    break;
            }
        }
    }
}
