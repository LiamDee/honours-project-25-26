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
    boolean notiSent = sharedPreferencesNotiSent.getBoolean("notiSent", false);



    public WorkerClass(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    /// every week, runs this function, which runs two other functions
    public Result doWork() {

        Log.d("workerSentTest", String.valueOf(hasWorkerBeenSent));
        sendNoti();
        changeVars();
        return Result.success();
    }

    /// function used to send notification
    public void sendNoti() {
        /// to send notifications on API levels > 26, a notification channel must be registered
        NotificationManager notiManager = (NotificationManager)getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "FocusathChannel";
            String desc = "App notification channel for Focusath";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("default", name, importance);
            channel.setDescription(desc);
            notiManager.createNotificationChannel(channel);
        }

        /// checks if a worker has been sent, if so, proceeds to next if statement
        if (hasWorkerBeenSent) {
            sharedPreferencesNotiSent.edit().putBoolean("notiSent", true).apply();
            Log.d("notiSentWorker", String.valueOf(notiSent));

            /// if app notifications are disabled, exits function
            if (ActivityCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d("notificationWorkerClass", "notifications disabled, skipping...");
                return;
            }

            /// builds notification, then sends it to user
            Log.d("notificationWorkerClass", "notification created");
            NotificationCompat.Builder notification = new NotificationCompat.Builder(getApplicationContext(), "default")
                    .setContentTitle("Your weekly check-in is now available!")
                    .setContentText("An update on your app usage is available, open the app to find out more, or please restart the app if it's already open. \n Ensure you also send the email with your app usage statistics.")
                    .setStyle(new NotificationCompat.BigTextStyle())
                    .setSmallIcon(R.mipmap.app_icon_round);
            notiManager.notify(1, notification.build());

        } else {
            Log.d("sendNotificationWorkerClass", "week has not passed, skipping...");
        }

    }
    /// function used to change sharedpreference value
    public void changeVars() {

        sharedPreferencesWorkerSent.edit().putBoolean("workerSent", true).apply();
        Log.d("workerSent-WorkerClass", String.valueOf(hasWorkerBeenSent));

    }

}
