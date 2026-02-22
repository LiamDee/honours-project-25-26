package org.me.gcu.focusath;
import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import android.content.SharedPreferences;
import androidx.work.Worker;
import androidx.work.WorkerParameters;



public class WorkerClass extends Worker {

    public SharedPreferences sharedPreferencesWorkerSent = getApplicationContext().getSharedPreferences("workerSent", MODE_PRIVATE);
    public SharedPreferences sharedPreferencesNotiSent = getApplicationContext().getSharedPreferences("notiSent", MODE_PRIVATE);
    boolean hasWorkerBeenSent = sharedPreferencesWorkerSent.getBoolean("workerSent", false);
    //TODO: rename notiSent to something that makes more sense
    boolean notiSent = sharedPreferencesNotiSent.getBoolean("notiSent", false);



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
        Log.d("workerSentTest", String.valueOf(hasWorkerBeenSent));
//        if (ActivityCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
//            Log.d("notificationWorkerClass", "notifications disabled, skipping...");
//            changeVars();
//        } else {
            sendNoti();
            changeVars();
        //}

        return Result.success();
    }
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

        if (hasWorkerBeenSent) {
            sharedPreferencesNotiSent.edit().putBoolean("notiSent", true).apply();
            Log.d("notiSentWorker", String.valueOf(notiSent));

            if (ActivityCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d("notificationWorkerClass", "notifications disabled, skipping...");
                return;
            }

            //TODO: make notification look nice, replace placeholder values with intended ones
            Log.d("notificationWorkerClass", "notification created");
            NotificationCompat.Builder notification = new NotificationCompat.Builder(getApplicationContext(), "default")
                    .setContentTitle("title")
                    .setContentText("message, restart app if open")
                    .setStyle(new NotificationCompat.BigTextStyle())
                    .setSmallIcon(R.drawable.ic_launcher_foreground);
            notiManager.notify(1, notification.build());

        } else {
            Log.d("sendNotificationWorkerClass", "week has not passed, skipping...");
        }

    }
    public void changeVars() {
//        if (hasWorkerBeenSent) {
//
//        }
        sharedPreferencesWorkerSent.edit().putBoolean("workerSent", true).apply();
        Log.d("workerSent-WorkerClass", String.valueOf(hasWorkerBeenSent));

    }

}
