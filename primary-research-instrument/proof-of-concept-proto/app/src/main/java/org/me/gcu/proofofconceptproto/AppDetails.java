package org.me.gcu.proofofconceptproto;
import android.graphics.drawable.Drawable;
public class AppDetails {
    public Drawable appIcon;
    public String appName;
    public int usagePercent;
    public String usageTime;

    public AppDetails(Drawable appIcon, String appName, int usagePercent, String usageTime) {
        this.appIcon = appIcon;
        this.appName = appName;
        this.usagePercent = usagePercent;
        this.usageTime = usageTime;
    }
}
