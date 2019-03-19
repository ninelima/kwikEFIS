package player.efis.common;

import java.util.ArrayList;

public class OpenAirRec
{
    public float clat; // lose this later
    public float clon; // lose this later

    public String ac;  //AC A
    public String an; //AN ADELAIDE CTA A [H24]
    public int al; //AL FL180
    public int ah; //AH FL245

    public ArrayList<OpenAirPoint> pointList;

    public OpenAirRec()
    {
        pointList = new ArrayList();
    }
}
