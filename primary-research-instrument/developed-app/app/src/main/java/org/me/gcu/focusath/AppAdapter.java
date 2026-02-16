package org.me.gcu.focusath;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class AppAdapter extends ArrayAdapter<AppDetails> {
    public AppAdapter(Context context, ArrayList<AppDetails> usageStatsThingArrayList) {
        super(context, 0, usageStatsThingArrayList);
    }
    public View getView(int pos, View convertView, @NonNull ViewGroup parent) {
        AppDetails usageStats = getItem(pos);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.app_details_widget, parent, false);
        }
        TextView app_name_text = convertView.findViewById(R.id.app_name_text);
        TextView usage_time_text = convertView.findViewById(R.id.usage_time_text);
        //TextView usage_percent_text = convertView.findViewById(R.id.usage_percent_text);
        ImageView appIcon = convertView.findViewById(R.id.appIcon);
        //ProgressBar usageProgressBar = convertView.findViewById(R.id.usageProgressBar);

        assert usageStats != null;
        app_name_text.setText(usageStats.appName);
        usage_time_text.setText(usageStats.usageTime);
        //usage_percent_text.setText(usageStats.usagePercent + "%");
        appIcon.setImageDrawable(usageStats.appIcon);
        //usageProgressBar.setProgress(usageStats.usagePercent);

        return convertView;

    }
}
