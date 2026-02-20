package org.me.gcu.focusath;
import static android.content.Context.MODE_PRIVATE;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import android.content.SharedPreferences;
import androidx.work.Worker;
import androidx.work.WorkerParameters;



public class WorkerClass extends Worker {

    public SharedPreferences sharedPreferencesWeekElapsed = getApplicationContext().getSharedPreferences("weekElapsed", MODE_PRIVATE);
    public SharedPreferences sharedPreferencesNotiSent = getApplicationContext().getSharedPreferences("notiSent", MODE_PRIVATE);


    public WorkerClass(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
//        try {
//            Thread.sleep(10000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
        sendNoti();
        sharedPreferencesWeekElapsed.edit().putBoolean("weekElapsed", true).apply();
        return Result.success();
    }
    //TODO: possibly rework into another method for functionality without having notifications enabled
    public void sendNoti() {
        NotificationManager notiManager = (NotificationManager)getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "FocusathChannel";
            String desc = "App notification channel for Focusath";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("default", name, importance);
            channel.setDescription(desc);
            //NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notiManager.createNotificationChannel(channel);
        }

        boolean hasItBeenAWeek = sharedPreferencesWeekElapsed.getBoolean("weekElapsed", false);
        Log.d("beenAWeek", String.valueOf(hasItBeenAWeek));

        if (hasItBeenAWeek) {
            //TODO: make notification look nice, replace placeholder values with intended ones
            NotificationCompat.Builder notification = new NotificationCompat.Builder(getApplicationContext(), "default")
                    .setContentTitle("title")
                    .setContentText("message")
                    .setStyle(new NotificationCompat.BigTextStyle())
                    .setSmallIcon(R.drawable.ic_launcher_foreground);
            notiManager.notify(1, notification.build());

            sharedPreferencesNotiSent.edit().putBoolean("notiSent", true).apply();
        }
        boolean notiSent = sharedPreferencesNotiSent.getBoolean("notiSent", false);
        Log.d("notiSentWorker", String.valueOf(notiSent));


    }

}
