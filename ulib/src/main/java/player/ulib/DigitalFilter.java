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

public class DigitalFilter
{
    private final float ALPHA = 0.25f; // .25 if ALPHA = 1 OR 0, no filter applies.
    private int BUF_SIZE = 16;
    private float[] x;
    private float[] y;

    public DigitalFilter()
    {
        x = new float[BUF_SIZE];
        y = new float[BUF_SIZE];
    }

    // Low Pass filter
    // the smoothing factor, ALPHA = deltaT/(tau + deltaT)
    // If ALPHA =  0.5, then the tau time constant is equal to the sampling period.
    // If ALPHA << 0.5  then tau is significantly larger than the sampling interval, and
    // deltaT is approximately equal to ALPHA * tau
    //
    // ALPHA = deltaT / (TAU + deltaT)
    // fc = ALPHA / ((1 - ALPHA) * 2 * PI * deltaT)

    public DigitalFilter(int bufferSize)
    {
        BUF_SIZE = bufferSize;
        x = new float[BUF_SIZE];
        y = new float[BUF_SIZE];
    }

    public float[] lowPass(float[] y, float[] x)
    {
        if (y == null) return x;
        y[0] = x[0];
        for (int i = 1; i < x.length; i++) {
            y[i] = ALPHA * x[i] + (1 - ALPHA) * y[i - 1];
            //y[i] = y[i-1] + ALPHA * (x[i] - y[i-1]);  // more efficient but less readable
        }
        return y;
    }

    public float lowPass(float a)
    {
        return runningAverage(a);
    }

    // High Pass filter
    // If α = 0.5 , then the  TAU  time constant equal to the sampling period. If
    // α ≪ 0.5 , then  TAU is significantly smaller than
    // the sampling interval, and R C ≈ α Δ T
    //
    // ALPHA = deltaT / (TAU + deltaT)
    // fc = (1 - ALPHA) / (2 * PI * ALPHA * deltaT)

    public float[] highPass(float[] y, float[] x)
    {
        if (y == null) return x;
        y[0] = x[0];
        for (int i = 1; i < x.length; i++) {
            y[i] = ALPHA * y[i - 1] + ALPHA * (x[i] - x[i - 1]);
            //y[i] := α * (y[i-1] + x[i] - x[i-1]); // more efficient but less readable

        }
        return y;
    }


    private void push(float a)
    {
        x[0] = a;
        if (BUF_SIZE - 1 >= 0) System.arraycopy(x, 0, x, 1, BUF_SIZE - 1);
    }

    public float runningAverage(float a)
    {
        float total = x[0] = a;

        for (int i = BUF_SIZE - 1; i > 0; i--) {
            x[i] = x[i - 1];
            total = total + x[i];
        }
        return (total / BUF_SIZE);
    }

    public double runningAverage(double a)
    {
        double total = x[0] = (float) a;

        for (int i = BUF_SIZE - 1; i > 0; i--) {
            x[i] = x[i - 1];
            total = total + x[i];
        }
        return total / BUF_SIZE;
    }
}
