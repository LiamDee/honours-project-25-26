package org.me.gcu.focusath;

import static android.app.AppOpsManager.MODE_ALLOWED;

import android.Manifest;
import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    //TODO: refactor "nextScreenBtn's" names to be more meaningful / compact, create app logo
    //TODO: check every view to ensure no placeholder text remains in prod
    private Button permissionsBtn, showStatsBtn, emailBtn, notiEnableBtn, notiTestBtn,
            nextScreenBtn, nextScreenBtnTwo, nextScreenBtnThree, nextScreenBtnFour, nextScreenBtnFive, nextScreenBtnSix,
            settingsScreenBtn, editGoalBtn, disableReminderBtn, submitNewGoalBtn, backToMainBtn, backToSettingsBtn, backToMainBtn2,
            redefineGoalYesBtn, redefineGoalNoBtn, backToMainBtn3;
    private ListView appListView;
    private String CHANNEL_ID = "FocusathChannel1";
    private int NOTIFICATION_ID = 0;
    private int screen_count = 0;
    public SharedPreferences sharedPreferences, sharedPreferencesOnBoarding, sharedPreferencesUsageTime;
    private EditText goalEntryField, activityFieldOne, activityFieldTwo, activityFieldThree, newGoalEntryField;
    private String goalEntryText, activityFieldOneText, activityFieldTwoText, activityFieldThreeText, newGoalEntryText;
    private Boolean isOnboardingComplete, notiSent, isRedefiningGoal;
    private File fileToEmail;
    private long totalTime;
    //TODO: rename notiSent to something that makes more sense
    //initially from WorkerClass
    public SharedPreferences sharedPreferencesWorkerSent, sharedPreferencesNotiSent;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EdgeToEdge.enable(this);
        //showStatsBtn = (Button)findViewById(R.id.showStatsBtn);
        appListView = (ListView)findViewById(R.id.appListView);
        sharedPreferences = getSharedPreferences("goalString", MODE_PRIVATE);
        sharedPreferencesOnBoarding = getSharedPreferences("isOnboardingComplete", MODE_PRIVATE);
        sharedPreferencesUsageTime = getSharedPreferences("oldUsageTime", MODE_PRIVATE);

        isRedefiningGoal = false;

        sharedPreferencesNotiSent = getSharedPreferences("notiSent", MODE_PRIVATE);
        sharedPreferencesWorkerSent = getSharedPreferences("workerSent", MODE_PRIVATE);

        boolean hasWorkerBeenSent = sharedPreferencesWorkerSent.getBoolean("workerSent", false);
        Log.d("workerSentMain", String.valueOf(hasWorkerBeenSent));

        //TODO: remove in prod -- only here so emulated phone doesn't constantly receive notifications when opened
        //WorkManager.getInstance().cancelAllWorkByTag("periodicWork");
        //WorkManager.getInstance().pruneWork();


//        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
//        StrictMode.setVmPolicy(builder.build());



        try {
            this.loadUsage();
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }


    };
    public void onClick(View aview) {

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

        notiSent = sharedPreferencesNotiSent.getBoolean("notiSent", false);
        Log.d("notiSentMain", String.valueOf(notiSent));

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
            if (notiSent) {
                Log.d("wasNotiSentMain", "noti has been sent");
                try {
                    loadUsage();
                } catch (PackageManager.NameNotFoundException e) {
                    throw new RuntimeException(e);
                }
                long currentTime =  TimeUnit.MILLISECONDS.toSeconds(totalTime);
                long oldTime = TimeUnit.MILLISECONDS.toSeconds(sharedPreferencesUsageTime.getLong("oldUsageTime", 0));

                if (currentTime > oldTime) {
                    Log.d("changeScreen1", "screen time has increased, different screen will appear here");
                    screen_count = 10;
                    screenCheck();
                } else {
                    Log.d("changeScreen2", "screen time has droppped, different screen will appear here");
                    screen_count = 9;
                    screenCheck();
                }

                sharedPreferencesNotiSent.edit().putBoolean("notiSent", false).apply();

                boolean notiSent = sharedPreferencesNotiSent.getBoolean("notiSent", false);
                Log.d("afterNotiSentMain", String.valueOf(notiSent));
            } else {
                if (getGrantStatus()) {
                    screen_count = 0;
                    screenCheck();
//                showStatsBtn.setOnClickListener(view -> {
//
//                });
            }

            }
        }
    }
    //used to swap between screens, 100% a MUCH better, nicer, and cleaner way to do this
    // (with switch cases for example) to do this but it'll suffice
    //TODO: add validation to each onclicklistener -- make sure to check uml flowcharts, see each TODO in each onclicklistener, add toasts when necessary
    public void screenCheck() {
        if (screen_count == 0) {
            setContentView(R.layout.activity_main);
            settingsScreenBtn = (Button)findViewById(R.id.settingsScreenBtn);
            settingsScreenBtn.setOnClickListener(view ->{
                screen_count = 7;
                screenCheck();
            });
            try {
                loadUsage();
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException(e);
            }
            TextView timeDiffText = (TextView)findViewById(R.id.timeDiffText);

            //TODO: change to hours
            long currentTime = TimeUnit.MILLISECONDS.toSeconds(totalTime);
            long oldTime = TimeUnit.MILLISECONDS.toSeconds(sharedPreferencesUsageTime.getLong("oldUsageTime", 0));

            Log.d("currTime", String.valueOf(currentTime));
            Log.d("oldTime", String.valueOf(oldTime));

            if (currentTime >= oldTime) {
                long timeDiff = currentTime - oldTime;
                Log.d("timeDiff1", String.valueOf(timeDiff));
                int timePercent = Math.round(((float)timeDiff / currentTime) * 100);
                timeDiffText.setText(timePercent + "% increase from last week");
            }

            else {
                long timeDiff = oldTime - currentTime;
                int timePercent = Math.round(((float)timeDiff / oldTime) * 100);
                Log.d("timeDiff2", String.valueOf(timeDiff));
                timeDiffText.setText(timePercent + "% decrease from last week");
            }

            if (oldTime == 0) {
                timeDiffText.setText("No difference to compare, please check back later");
            }

            //long time2 = TimeUnit.MILLISECONDS.toHours(149573890);

            //Log.d("time1", String.valueOf(time1));
            //Log.d("time2", String.valueOf(time2));

            //long timeDiff = time2 - time1;
            //int comp = Math.round(((float) timeDiff / time1) * 100);


            Log.d("oldTime", String.valueOf(oldTime)); //should be 131441149


            //Log.d("total time", String.valueOf(totalTime));
            //Log.d("comp", String.valueOf(comp));
            //timeDiffText.setText(comp + "% increase from last week");
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
                }

                else if (isRedefiningGoal == true) {
                    //TODO: possibly change paragraph text to make more sense given the context
                    String oldGoal =  sharedPreferences.getString("goalEntry", "none");
                    if (oldGoal.equals(goalEntryText)) {
                        Toast.makeText(view.getContext(), "New goal cannot be identical to old goal", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        sharedPreferences.edit().putString("goalEntry", goalEntryText).apply();
                        Toast.makeText(view.getContext(), "Goal has been updated to: " + goalEntryText, Toast.LENGTH_LONG).show();
                        screen_count = 11;
                        screenCheck();
                        isRedefiningGoal = false;
                    }
                }

                else {
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
//            notiTestBtn.setOnClickListener(view ->
//            {
//                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
//                    Toast.makeText(view.getContext(), "Please enable notifications to use this feature", Toast.LENGTH_SHORT).show();
//                } else {
//                    WorkManager.getInstance().enqueue(periodicWorkRequest);
//                }
//            });


            nextScreenBtnSix.setOnClickListener(view -> {
                sharedPreferencesOnBoarding.edit().putBoolean("isOnboardingComplete", true).apply();
                WorkManager.getInstance().enqueue(periodicWorkRequest);
                screen_count = 0;
                screenCheck();
                sharedPreferencesUsageTime.edit().putLong("oldUsageTime", totalTime).apply();
                Log.d("totalTime", String.valueOf(totalTime));
            });
        }
        else if (screen_count == 7) {
            setContentView(R.layout.settings_screen);
            emailBtn = (Button)findViewById(R.id.emailBtn);
            emailBtn.setOnClickListener(view -> {
                prepEmail();
                //Log.d("emailTest", "yea we here");
            });
            editGoalBtn = (Button)findViewById(R.id.editGoalBtn);
            editGoalBtn.setOnClickListener(view -> {
                screen_count = 8;
                screenCheck();
            });
            disableReminderBtn = (Button)findViewById(R.id.disableReminderBtn);
            disableReminderBtn.setOnClickListener(view -> {

                WorkManager.getInstance().cancelAllWorkByTag("periodicWork");
                WorkManager.getInstance().pruneWork();
                Toast.makeText(view.getContext(), "Notification reminders have been disabled", Toast.LENGTH_LONG).show();

            });
            backToMainBtn = (Button)findViewById(R.id.backToMainBtn);
            backToMainBtn.setOnClickListener(view -> {
                screen_count = 0;
                screenCheck();
            });

        }
        else if (screen_count == 8) {
            setContentView(R.layout.redefine_goal_screen);
            submitNewGoalBtn = (Button)findViewById(R.id.submitNewGoalBtn);
            backToSettingsBtn = (Button)findViewById(R.id.backToSettingsBtn);
            newGoalEntryField = (EditText)findViewById(R.id.newGoalEntryField);

            submitNewGoalBtn.setOnClickListener(view -> {
                String oldGoal =  sharedPreferences.getString("goalEntry", "none");
                newGoalEntryText = newGoalEntryField.getText().toString();
                if (newGoalEntryText.isEmpty()) {
                    Toast.makeText(view.getContext(), "Please enter a goal to continue", Toast.LENGTH_SHORT).show();
                } else if (oldGoal.equals(newGoalEntryText)) {
                    Toast.makeText(view.getContext(), "New goal cannot be identical to old goal", Toast.LENGTH_SHORT).show();
                }
                else {
                    sharedPreferences.edit().putString("goalEntry", newGoalEntryText).apply();
                    Toast.makeText(view.getContext(), "Goal has been updated to: " + newGoalEntryText, Toast.LENGTH_LONG).show();
                    screen_count = 7;
                    screenCheck();
                }
            });
            backToSettingsBtn.setOnClickListener(view -> {
                screen_count = 7;
                screenCheck();
            });


        } else if (screen_count == 9) {
            setContentView(R.layout.appraise_screen);
            backToMainBtn2 = (Button)findViewById(R.id.backToMainBtn2);
            backToMainBtn2.setOnClickListener(view -> {
                screen_count = 0;
                screenCheck();
            });
        } else if (screen_count == 10) {
            //TODO: add usage percentage to text
            setContentView(R.layout.usage_evaluation_screen);
            redefineGoalYesBtn = (Button)findViewById(R.id.redefineGoalYesBtn);
            redefineGoalNoBtn = (Button)findViewById(R.id.redefineGoalNoBtn);

            redefineGoalNoBtn.setOnClickListener(view -> {
                screen_count = 11;
                screenCheck();
            });

            redefineGoalYesBtn.setOnClickListener(view -> {
                isRedefiningGoal = true;
                screen_count = 4;
                screenCheck();
            });

        } else if (screen_count == 11) {
            setContentView(R.layout.activity_suggestion_screen);
            backToMainBtn3 = (Button)findViewById(R.id.backToMainBtn3);

            backToMainBtn3.setOnClickListener(view -> {
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

        totalTime = usageStatsList.stream().map(UsageStats::getTotalTimeInForeground).mapToLong(Long::longValue).sum();

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



        //writeAppListToFile(appDetailsArrayList);

        generateFile(appDetailsArrayList);

        Collections.reverse(appDetailsArrayList);
        AppAdapter appAdapter = new AppAdapter(this, appDetailsArrayList);

        ListView appListView = findViewById(R.id.appListView);
        appListView.setAdapter(appAdapter);


    }



//    private void writeAppListToFile (ArrayList<AppDetails> appStuff) {
//        try {
//            FileOutputStream fileOutputStream = openFileOutput("myfile.txt", Context.MODE_PRIVATE);
//            ObjectOutputStream outputStream = new ObjectOutputStream(fileOutputStream);
//            for (int i = 0; i < appStuff.size(); i++) {
//                outputStream.writeObject("\n" + appStuff.get(i).appName + "  |  " + appStuff.get(i).usageTime);
//            }
//            //outputStream.flush();
//            outputStream.close();
//            fileOutputStream.close();
//            Log.d("fileDebug", "app details written to file!");
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }

    private void generateFile(ArrayList<AppDetails> appDetails) {
        String fileName = "appDataDetails.csv";
        File exFileDir = this.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        fileToEmail = new File (exFileDir, fileName);

        //prep for req-1.2 //TODO: add graphs for prior usage -- ensure new file creation occurs on weekly basis
        if (fileToEmail.exists()) {
            Log.d("fileExists", "file already exists");
        }
        try {
            FileWriter fileWriter = new FileWriter(fileToEmail);
            for (int i = 0; i < appDetails.size(); i++) {
                fileWriter.append("\n").append(appDetails.get(i).appName).append("  |  ").append(appDetails.get(i).usageTime);
            }
            fileWriter.flush();
            fileWriter.close();
            Log.d("fileStuff", "File created successfully, located at: " + exFileDir);
        } catch (IOException e) {
            e.printStackTrace();
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
            throw new IllegalArgumentException("IllegalArgument");
        }
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds); milliseconds -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);

        return (hours + "h " + minutes + "m ");
    }
    public void prepEmail() {
//        Log.d("emailTest", "yea we here...again");
//        Intent intent = new Intent(Intent.ACTION_SENDTO);
//        intent.setData(Uri.parse("mailto:"));
//        intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"ldebuf300@caledonian.ac.uk" });
//        intent.putExtra(Intent.EXTRA_SUBJECT, "Test Email");
//        File file = getFileStreamPath("myfile.txt");
//        Log.d("file dir", String.valueOf(getFilesDir()));
////        String s = file.getAbsolutePath();
////        File file1 = new File(s);
////        String fileName = "myfile.txt";
////        File file = new File(getFilesDir(), "myfile.txt");
////        File extDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
//        Uri uri = Uri.fromFile(file);
//        intent.putExtra(Intent.EXTRA_STREAM, uri);
//
//        //-- this would be the file containing usage data
////        if (intent.resolveActivity(getPackageManager()) != null) {
////            startActivity(intent);
////        }
//        Intent intent1 = Intent.createChooser(intent, "Send using: ");
//        intent1.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//        startActivity(intent1);
        Uri fileUri = FileProvider.getUriForFile(this, getPackageName()+".provider", fileToEmail);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("vnd.android.cursor.dir/email");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"ldebuf300@caledonian.ac.uk" });
        intent.putExtra(Intent.EXTRA_STREAM, fileUri);
        intent.putExtra(Intent.EXTRA_SUBJECT, "Test Email");
        startActivity(Intent.createChooser(intent, "Send email using: "));


    }

}