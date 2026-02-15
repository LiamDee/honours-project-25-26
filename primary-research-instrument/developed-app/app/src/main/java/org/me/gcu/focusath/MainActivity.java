package org.me.gcu.focusath;

import static android.app.AppOpsManager.MODE_ALLOWED;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    //TODO: refactor "nextScreenBtn's" names to be more meaningful / compact, create app logo
    private Button permissionsBtn, showStatsBtn, emailBtn, notiEnableBtn, notiTestBtn,
            nextScreenBtn, nextScreenBtnTwo, nextScreenBtnThree, nextScreenBtnFour, nextScreenBtnFive, nextScreenBtnSix;
    private ListView appListView;
    private String CHANNEL_ID = "FocusathChannel1";
    private int NOTIFICATION_ID = 0;
    private int screen_count = 0;
    private SharedPreferences sharedPreferences, sharedPreferencesOnBoarding;
    private EditText goalEntryField, activityFieldOne, activityFieldTwo, activityFieldThree;
    private String goalEntryText, activityFieldOneText, activityFieldTwoText, activityFieldThreeText;
    private Boolean isOnboardingComplete;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        showStatsBtn = (Button)findViewById(R.id.showStatsBtn);
        appListView = (ListView)findViewById(R.id.appListView);
        emailBtn = (Button)findViewById(R.id.emailBtn);
        emailBtn.setOnClickListener(this);
        sharedPreferences = getSharedPreferences("goalString", MODE_PRIVATE);
        sharedPreferencesOnBoarding = getSharedPreferences("isOnboardingComplete", MODE_PRIVATE);

        //TODO: move to settings screen -- only here so emulated phone doesn't constantly receive notifications when opened
        WorkManager.getInstance().cancelAllWorkByTag("periodicWork");
        WorkManager.getInstance().pruneWork();

        try {
            this.loadUsage();
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }


    };
    public void onClick(View aview) {
//        prepEmail();
//        Log.d("emailTest", "yea we here");
    }
    private boolean getGrantStatus() {
        AppOpsManager appOpsManager = (AppOpsManager)getApplicationContext().getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getApplicationContext().getPackageName());
        if (mode == AppOpsManager.MODE_DEFAULT) {
            return (getApplicationContext().checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED);
        } else {
            return (mode == MODE_ALLOWED);
        }
    }

    @Override protected void onStart() {
        super.onStart();
        isOnboardingComplete = sharedPreferencesOnBoarding.getBoolean("isOnboardingComplete", false);
        if (!isOnboardingComplete) {
            if (screen_count > 1) {
                sharedPreferencesOnBoarding.edit().putBoolean("isOnboardingComplete", false).apply();
                screenCheck();
            } else {
                sharedPreferencesOnBoarding.edit().putBoolean("isOnboardingComplete", false).apply();
                screen_count = 1;
                screenCheck();
            }
        }
        else {
            if (getGrantStatus()) {
                screen_count = 0;
                showStatsBtn.setOnClickListener(view -> {

                });
            }
        }
    }
    //used to swap between screens, 100% a MUCH better, nicer, and cleaner way to do this
    // (with switch cases for example) to do this but it'll suffice
    //TODO: add validation to each onclicklistener -- make sure to check uml flowcharts, see each TODO in each onclicklistener, add toasts when necessary
    public void screenCheck() {
        //TODO: move onboarding usage tracking code to here
        if (screen_count == 0) {
            setContentView(R.layout.activity_main);
            try {
                loadUsage();
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        //note: this has been moved to be the first screen as exiting the intent returns to the first screen, TODO: update uml flowchart to reflect change
        else if (screen_count == 1){
            setContentView(R.layout.enable_user_tracking_screen);

//            String acOne = sharedPreferences.getString("activityOne", "none");
//            Log.d("currentAcOne", acOne);
//
//            String acTwo = sharedPreferences.getString("activityTwo", "none");
//            Log.d("currentAcTwo", acTwo);
//
//            String acThree = sharedPreferences.getString("activityThree", "none");
//            Log.d("currentAcThree", acThree);

            permissionsBtn = (Button)findViewById(R.id.permissionBtn);
            permissionsBtn.setOnClickListener(view -> {
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                setContentView(R.layout.enable_user_tracking_screen);
            });

            nextScreenBtnFive = (Button)findViewById(R.id.nextScreenBtnFive);
            nextScreenBtnFive.setOnClickListener(view -> {
                //commented for testing, make sure to uncomment when finished testing
//                if (!getGrantStatus()) {
//                    Toast.makeText(view.getContext(), "Please enable app usage statistics tracking to continue", Toast.LENGTH_SHORT).show();
//                    return;
//                }
                screen_count = 2;
                screenCheck();

            });
        }
        //no validation needed here
        else if (screen_count == 2) {
            setContentView(R.layout.onboarding_info_screen);
            nextScreenBtn = (Button)findViewById(R.id.nextScreenBtn);
            nextScreenBtn.setOnClickListener(view -> {
                screen_count = 3;
                screenCheck();
//                    setContentView(R.layout.activity_main);
                //}

            });

            //notiTestBtn.setOnClickListener(view -> {
//                WorkRequest workerClassReq = new OneTimeWorkRequest.Builder(WorkerClass.class).build();
//                WorkManager.getInstance(this).enqueue(workerClassReq);
            //createNotiChannel();
            //scheduleNotification(calendar);
//                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
//                        .setSmallIcon(R.drawable.ic_launcher_foreground)
//                        .setContentTitle("a noti")
//                        .setContentText("look at me im all the text");
            //if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
//                }
//                NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, builder.build());
            //});
        }
        //no validation needed here
        else if (screen_count == 3) {
            setContentView(R.layout.onboarding_info_screen_two);
            nextScreenBtnTwo = (Button)findViewById(R.id.nextScreenBtnTwo);
            nextScreenBtnTwo.setOnClickListener(view -> {
                screen_count = 4;
                screenCheck();
            });
        }
        else if (screen_count == 4) {
            setContentView(R.layout.user_goal_input_screen);
            goalEntryField = (EditText)findViewById(R.id.goalEntryField);
            nextScreenBtnThree = (Button)findViewById(R.id.nextScreenBtnThree);
            nextScreenBtnThree.setOnClickListener(view -> {

                goalEntryText = goalEntryField.getText().toString();
                if (goalEntryText.isEmpty()) {
                    Toast.makeText(view.getContext(), "Please enter a goal to continue", Toast.LENGTH_SHORT).show();
                } else {
                    sharedPreferences.edit().putString("goalEntry", goalEntryText).apply();
                    screen_count = 5;
                    screenCheck();
                }

            });
        }
        else if (screen_count == 5) {
            //Log.d("goalEntry", goalEntryText);

            setContentView(R.layout.user_activity_suggestions_screen);

//            String currentGoal = sharedPreferences.getString("goalEntry", "none");
//            Log.d("currentGoal", currentGoal);

            activityFieldOne = (EditText)findViewById(R.id.activityFieldOne);
            activityFieldTwo = (EditText)findViewById(R.id.activityFieldTwo);
            activityFieldThree = (EditText)findViewById(R.id.activityFieldThree);

            nextScreenBtnFour = (Button)findViewById(R.id.nextScreenBtnFour);
            nextScreenBtnFour.setOnClickListener(view -> {

                activityFieldOneText = activityFieldOne.getText().toString();
                activityFieldTwoText = activityFieldTwo.getText().toString();
                activityFieldThreeText = activityFieldThree.getText().toString();

                if (activityFieldOneText.isEmpty() || activityFieldTwoText.isEmpty() || activityFieldThreeText.isEmpty()) {
                    Toast.makeText(view.getContext(), "Please enter some activities to continue", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    if ((Objects.equals(activityFieldOneText, activityFieldTwoText))
                            || (Objects.equals(activityFieldTwoText, activityFieldThreeText))
                            || (Objects.equals(activityFieldOneText, activityFieldThreeText))) {
                        //TODO: reword to sound less odd
                        Toast.makeText(view.getContext(), "Your activities must be unique", Toast.LENGTH_SHORT).show();
                        return;
                    } else {
                        sharedPreferences.edit().putString("activityOne", activityFieldOneText).apply();
                        sharedPreferences.edit().putString("activityTwo", activityFieldTwoText).apply();
                        sharedPreferences.edit().putString("activityThree", activityFieldThreeText).apply();

                        screen_count = 6;
                        screenCheck();
                    }
                }
            });
        }

        //no validation necessary here
        else if (screen_count == 6) {
            setContentView(R.layout.noti_screen);
            nextScreenBtnSix = (Button)findViewById(R.id.nextScreenBtnSix);
            //this function is necessary to enable notifications on android apis above 33
            notiEnableBtn = (Button)findViewById(R.id.notiEnableBtn);
            notiEnableBtn.setOnClickListener(view -> {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ActivityCompat.requestPermissions(this, new String[]{(Manifest.permission.POST_NOTIFICATIONS)}, 12);
                    }
                }
            });

            notiTestBtn = (Button)findViewById(R.id.notiTestBtn);
            final PeriodicWorkRequest periodicWorkRequest =
                    new PeriodicWorkRequest.Builder(WorkerClass.class, 5, TimeUnit.SECONDS, 15, TimeUnit.MINUTES)
                            //TODO: adjust repeatinterval to "1, TimeUnit.WEEKS" for prod
                            .addTag("periodicWork")
                            .build();
            //TODO: add check to prevent multiple workerclass calls
            notiTestBtn.setOnClickListener(view ->
            {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(view.getContext(), "Please enable notifications to use this feature", Toast.LENGTH_SHORT).show();
                } else {
                    WorkManager.getInstance().enqueue(periodicWorkRequest);
                }
            });


            nextScreenBtnSix.setOnClickListener(view -> {
                sharedPreferencesOnBoarding.edit().putBoolean("isOnboardingComplete", true).apply();
                screen_count = 0;
                screenCheck();
            });
        }
    }


    public void loadUsage() throws PackageManager.NameNotFoundException {
        long lastWeek = System.currentTimeMillis() - (1000 * 3600 * 24 * 7);
        UsageStatsManager usm = (UsageStatsManager) this.getSystemService(USAGE_STATS_SERVICE);
        List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_WEEKLY,
                lastWeek, System.currentTimeMillis());
        appList = appList.stream().filter(app -> app.getTotalTimeInForeground() > 0).collect(Collectors.toList());

        Log.d("usagestats", appList.toString());

        if (!appList.isEmpty()) {
            Map<String, UsageStats> sortedMap = new TreeMap();
            for (UsageStats usageStats : appList) {
                sortedMap.put(usageStats.getPackageName(), usageStats);
            }
            Log.d("moreStats", sortedMap.toString());
            showUsage(sortedMap);
        }
    }

        public void showUsage(Map<String, UsageStats> sortedMap) throws PackageManager.NameNotFoundException {
        ArrayList<AppDetails> appDetailsArrayList = new ArrayList<>();
        List<UsageStats> usageStatsList = new ArrayList<>(sortedMap.values());

        Collections.sort(usageStatsList, (z1, z2) ->
                Long.compare(z1.getTotalTimeInForeground(), z2.getTotalTimeInForeground()));

        long totalTime = usageStatsList.stream().map(UsageStats::getTotalTimeInForeground).mapToLong(Long::longValue).sum();

        for (UsageStats usageStats : usageStatsList) {
            try {
                String packageName = usageStats.getPackageName();
                Drawable packageIcon = getDrawable(R.drawable.ic_launcher_background);
                String[] packageNames = packageName.split("\\.");
                String appName = packageNames[packageNames.length - 1].trim();

                if (doesAppInfoExist(usageStats)) {
                    ApplicationInfo applicationInfo = getApplicationContext().getPackageManager().getApplicationInfo(packageName, 0);
                    packageIcon = getApplicationContext().getPackageManager().getApplicationIcon(applicationInfo);
                    appName = getApplicationContext().getPackageManager().getApplicationLabel(applicationInfo).toString();
                }

                int usagePercent = (int) (usageStats.getTotalTimeInForeground() * 100 / totalTime);
                String usageTime = convertUsageTime(usageStats.getTotalTimeInForeground());

                AppDetails usageStatThing = new AppDetails(packageIcon, appName, usagePercent, usageTime);
                appDetailsArrayList.add(usageStatThing);

            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }


//            Log.d("filepath", s);
        }
        writeAppListToFile(appDetailsArrayList);
        Collections.reverse(appDetailsArrayList);
        AppAdapter appAdapter = new AppAdapter(this, appDetailsArrayList);

        ListView appListView = findViewById(R.id.appListView);
        appListView.setAdapter(appAdapter);


    }
    private void writeAppListToFile (ArrayList<AppDetails> appStuff) {
        try {
            FileOutputStream fileOutputStream = openFileOutput("myfile.txt", Context.MODE_PRIVATE);
            ObjectOutputStream outputStream = new ObjectOutputStream(fileOutputStream);
            for (int i = 0; i < appStuff.size(); i++) {
                outputStream.writeObject("\n" + appStuff.get(i).appName + "  |  " + appStuff.get(i).usageTime);
            }
            //outputStream.flush();
            outputStream.close();
            fileOutputStream.close();
            Log.d("fileDebug", "app details written to file!");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean doesAppInfoExist(UsageStats usageStats) {
        try {
            getApplicationContext().getPackageManager().getApplicationInfo(usageStats.getPackageName(), 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private String convertUsageTime(long milliseconds) {
        if (milliseconds < 0) {
            throw new IllegalArgumentException("how");
        }
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds); milliseconds -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);

        return (hours + " h " + minutes + " m ");
    }

}