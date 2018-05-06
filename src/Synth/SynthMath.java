package Synth;

import processing.core.PApplet;
import processing.core.PVector;

public class SynthMath
{
    public static PVector lerpVector(PVector A, PVector B, float t)
    {
        if (t > 1) t = 1;
        if (t < 0) t = 0;
        assert (t <= 1 && t >= 0);
        return PVector.add(PVector.mult(A, t), PVector.mult(B, 1.0f - t));
    }

    public static void lineHeading(PVector origin, PVector vector, PApplet app)
    {
        app.pushMatrix();
        app.translate(origin.x, origin.y, origin.z);
        app.line(0, 0, 0, 50 * vector.x, 50 * vector.y, 50 * vector.z);
        app.popMatrix();
    }

    public static float mix(float t)
    {
        //6t^5−15t^4+10t^3
        //No discontinuities at endpoints of derivative
        if (t > 1) t = 1;
        if (t < 0) t = 0;
        assert (t <= 1 && t >= 0);
        return (6 * (t * t * t * t * t) - 15 * t * t * t * t + 10 * t * t * t);

    }

    public static float highCentreRemap(float t)
    {
        if (t > 1) t = 1;
        if (t < 0) t = 0;
        assert (t <= 1 && t >= 0);
        if (t >= 0.5)
        {
            return mix(-2 * (t-1));
        }
        else
        {
            return mix(2 * t);
        }
    }

    public static PVector multVec(PVector A, PVector B)
    {
        return new PVector(A.x * B.x, A.y * B.y, A.z * B.z);
    }

}
