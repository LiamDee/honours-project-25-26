package org.me.gcu.focusath;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;



public class WorkerClass extends Worker {

    public WorkerClass(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        sendNoti();
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

        //TODO: make notification look nice, replace placeholder values with intended ones
        NotificationCompat.Builder notification = new NotificationCompat.Builder(getApplicationContext(), "default")
                .setContentTitle("title")
                .setContentText("message")
                .setStyle(new NotificationCompat.BigTextStyle())
                .setSmallIcon(R.drawable.ic_launcher_foreground);
        notiManager.notify(1, notification.build());
    }

}
