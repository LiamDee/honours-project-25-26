package org.me.gcu.focusath;

import static android.app.AppOpsManager.MODE_ALLOWED;

import android.Manifest;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    //TODO: refactor "nextScreenBtn's" names to be more meaningful / compact, create app logo
    //TODO: check every view to ensure no placeholder text remains in prod
    private Button permissionsBtn, showStatsBtn, emailBtn, notiEnableBtn, notiTestBtn,
            nextScreenBtn, nextScreenBtnTwo, nextScreenBtnThree, nextScreenBtnFour, nextScreenBtnFive, nextScreenBtnSix,
            settingsScreenBtn, editGoalBtn, submitNewGoalBtn, backToMainBtn, backToSettingsBtn, backToMainBtn2,
            redefineGoalYesBtn, redefineGoalNoBtn, backToMainBtn3, previousGraphBtn, currentGraphBtn, editActivitiesBtn;
    private ListView appListView;
    private int screen_count = 0;
    //TODO: rename notiSent to something that makes more sense

    ///sharedPreferencesWorkerSent and sharedPreferencesNotiSent are initially from WorkerClass
    public SharedPreferences sharedPreferences, sharedPreferencesOnBoarding, sharedPreferencesUsageTime, sharedPreferencesWorkerSent, sharedPreferencesNotiSent;
    private EditText goalEntryField, activityFieldOne, activityFieldTwo, activityFieldThree, newGoalEntryField;
    private String goalEntryText, activityFieldOneText, activityFieldTwoText, activityFieldThreeText, newGoalEntryText;
    private Boolean isOnboardingComplete, notiSent, isRedefiningGoal, isEditingActivities;
    private File fileToEmail;
    private TextView goalText, activitiesText, goalTextView, currentGoalText;
    private ImageView helpIcon;
    private long totalTime;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EdgeToEdge.enable(this);
        appListView = (ListView)findViewById(R.id.appListView);
        sharedPreferences = getSharedPreferences("goalString", MODE_PRIVATE);
        sharedPreferencesOnBoarding = getSharedPreferences("isOnboardingComplete", MODE_PRIVATE);
        sharedPreferencesUsageTime = getSharedPreferences("oldUsageTime", MODE_PRIVATE);

        isRedefiningGoal = false;
        isEditingActivities = false;

        sharedPreferencesNotiSent = getSharedPreferences("notiSent", MODE_PRIVATE);
        sharedPreferencesWorkerSent = getSharedPreferences("workerSent", MODE_PRIVATE);

        boolean hasWorkerBeenSent = sharedPreferencesWorkerSent.getBoolean("workerSent", false);
        Log.d("workerSentMain", String.valueOf(hasWorkerBeenSent));

        //TODO: remove in prod -- only here so emulated phone doesn't constantly receive notifications when opened
        //WorkManager.getInstance().cancelAllWorkByTag("periodicWork");
        //WorkManager.getInstance().pruneWork();




        try {
            this.loadUsage();
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }


    };
    public void onClick(View aview) {

    }
    /// function used to check if permission to access app usage stats has been given
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


        /// checks if onboarding process has completed (always false on app install), if it has been completed, proceed to main screen as normal
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

        /// checks if - is true, if so, proceeds to check current usage with old usage, then sends user to appropriate screen depending on usage
        /// then sets old usage time to current usage time
        /// if false, sends user to main screen
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
                sharedPreferencesUsageTime.edit().putLong("oldUsageTime", totalTime).apply();

                boolean notiSent = sharedPreferencesNotiSent.getBoolean("notiSent", false);
                Log.d("afterNotiSentMain", String.valueOf(notiSent));

            } else {
                if (getGrantStatus()) {
                    screen_count = 0;
                    screenCheck();
                }

            }
        }
    }
    ///used to swap between screens, 100% a MUCH better, nicer, and cleaner way to do this
    /// (with switch cases for example) to do this but it'll suffice
    ///TODO: add validation to each onclicklistener -- make sure to check uml flowcharts, see each TODO in each onclicklistener, add toasts when necessary
    public void screenCheck() {
        if (screen_count == 0) {
            setContentView(R.layout.activity_main);
            settingsScreenBtn = (Button)findViewById(R.id.settingsScreenBtn);
            previousGraphBtn = (Button)findViewById(R.id.previousGraphBtn);
            helpIcon = (ImageView)findViewById(R.id.helpIcon);

            ///help alert dialog if the user needs assistance
            helpIcon.setOnClickListener(view -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Help");
                builder.setMessage("If you discover any issues, or simply have any questions about the app, please refer to the Quick Start Guide, or email me via ldebuf300@caledonian.ac.uk.")
                        .setCancelable(true)
                        .setPositiveButton("Got it", (dialog, which) -> dialog.dismiss());
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            });

            ///goes to settings screen
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

            //TODO: change to .toHours
            long currentTime = TimeUnit.MILLISECONDS.toSeconds(totalTime); //note: this will only properly update on the next interval point
            long oldTime = TimeUnit.MILLISECONDS.toSeconds(sharedPreferencesUsageTime.getLong("oldUsageTime", 0));

            Log.d("currTime", String.valueOf(currentTime));
            Log.d("oldTime", String.valueOf(oldTime));

            ///check to compare usage time from current week to last week
            if (oldTime == 0 || oldTime == currentTime) {
                timeDiffText.setText("No usage difference to compare, please check back later");
            }

            else if (currentTime > oldTime) {
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

            Log.d("oldTime", String.valueOf(oldTime));


            ///goes to graph showing user usage from last week
            previousGraphBtn.setOnClickListener(view -> {
                screen_count = 12;
                screenCheck();
            });


            //Log.d("total time", String.valueOf(totalTime));
        }

        ///starter screen on install, gives user information about the app
        else if (screen_count == 1){
            setContentView(R.layout.onboarding_info_screen);
            nextScreenBtn = (Button)findViewById(R.id.nextScreenBtn);
            nextScreenBtn.setOnClickListener(view -> {
                screen_count = 2;
                screenCheck();
            });
        }
        ///similar to previous screen, gives more info about the app
        else if (screen_count == 2) {

            setContentView(R.layout.onboarding_info_screen_two);
            nextScreenBtnTwo = (Button)findViewById(R.id.nextScreenBtnTwo);
            nextScreenBtnTwo.setOnClickListener(view -> {
                screen_count = 3;
                screenCheck();
            });
        }

        ///asks the user to give the app permission to usage stats (app cant work properly without it)
        else if (screen_count == 3) {

            setContentView(R.layout.enable_user_tracking_screen);

            permissionsBtn = (Button)findViewById(R.id.permissionBtn);
            permissionsBtn.setOnClickListener(view -> {
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                setContentView(R.layout.enable_user_tracking_screen);
            });

            ///if permission hasn't been granted, shows toast and doesnt proceed to next screen
            nextScreenBtnFive = (Button)findViewById(R.id.nextScreenBtnFive);
            nextScreenBtnFive.setOnClickListener(view -> {
                if (!getGrantStatus()) {
                    Toast.makeText(view.getContext(), "Please enable access to app usage statistics to continue", Toast.LENGTH_SHORT).show();
                    return;
                }
                screen_count = 4;
                screenCheck();
            });
        }

        ///screen used to define (and redefine) a goal
        else if (screen_count == 4) {
            setContentView(R.layout.user_goal_input_screen);
            goalEntryField = (EditText)findViewById(R.id.goalEntryField);
            goalTextView = (TextView)findViewById(R.id.goalTextView);
            nextScreenBtnThree = (Button)findViewById(R.id.nextScreenBtnThree);

            /// if user is currently redefining goal (via weekly check), changes the text accordingly
            if (isRedefiningGoal == true) {
                goalTextView.setText("Type in a new goal in the field below, note that your new goal cannot be identical to your old one.");
            }

            nextScreenBtnThree.setOnClickListener(view -> {
                /// if goal field is empty, asks user to enter a goal to proceed
                goalEntryText = goalEntryField.getText().toString();
                if (goalEntryText.isEmpty()) {
                    Toast.makeText(view.getContext(), "Please enter a goal to continue", Toast.LENGTH_SHORT).show();
                }

                /// only runs this part of the code if user is currently redefining goal
                else if (isRedefiningGoal == true) {

                    String oldGoal =  sharedPreferences.getString("goalEntry", "none");
                    /// checks entered goal with current goal, if identical, alerts user that new goal can't be the same as new one
                    if (oldGoal.equals(goalEntryText)) {
                        Toast.makeText(view.getContext(), "New goal cannot be identical to old goal", Toast.LENGTH_SHORT).show();
                    }
                    /// updates goal, alerts user to updated goal, then goes to next screen
                    else {
                        sharedPreferences.edit().putString("goalEntry", goalEntryText).apply();
                        Toast.makeText(view.getContext(), "Goal has been updated to: " + goalEntryText, Toast.LENGTH_LONG).show();
                        screen_count = 11;
                        screenCheck();
                        isRedefiningGoal = false;
                    }
                }

                /// sets entered goal as current goal, then proceeds to next screen
                else {
                    sharedPreferences.edit().putString("goalEntry", goalEntryText).apply();
                    screen_count = 5;
                    screenCheck();
                }

            });
        }
        /// screen used to get activities from user
        else if (screen_count == 5) {

            setContentView(R.layout.user_activity_suggestions_screen);

            activityFieldOne = (EditText)findViewById(R.id.activityFieldOne);
            activityFieldTwo = (EditText)findViewById(R.id.activityFieldTwo);
            activityFieldThree = (EditText)findViewById(R.id.activityFieldThree);

            /// if user is currently editing activity, prefill edittext fields with given info
            if (isEditingActivities == true) {
                String activityOne = sharedPreferences.getString("activityOne", "none");
                String activityTwo = sharedPreferences.getString("activityTwo", "none");
                String activityThree = sharedPreferences.getString("activityThree", "none");

                activityFieldOne.setText(activityOne);
                activityFieldTwo.setText(activityTwo);
                activityFieldThree.setText(activityThree);

            }

            nextScreenBtnFour = (Button)findViewById(R.id.nextScreenBtnFour);
            nextScreenBtnFour.setOnClickListener(view -> {

                activityFieldOneText = activityFieldOne.getText().toString();
                activityFieldTwoText = activityFieldTwo.getText().toString();
                activityFieldThreeText = activityFieldThree.getText().toString();

                /// if any activity field is empty, ask user to fill in all fields
                if (activityFieldOneText.isEmpty() || activityFieldTwoText.isEmpty() || activityFieldThreeText.isEmpty()) {
                    Toast.makeText(view.getContext(), "Please enter an activity in all fields to continue", Toast.LENGTH_SHORT).show();
                } else {
                    /// if any inputted activities are identical to one another, ask user to ensure activities are unique
                    if ((Objects.equals(activityFieldOneText, activityFieldTwoText))
                            || (Objects.equals(activityFieldTwoText, activityFieldThreeText))
                            || (Objects.equals(activityFieldOneText, activityFieldThreeText))) {
                        Toast.makeText(view.getContext(), "Your activities must be unique from one another", Toast.LENGTH_SHORT).show();
                    } else {
                        /// sets entered activities, then proceeds to next screen
                        sharedPreferences.edit().putString("activityOne", activityFieldOneText).apply();
                        sharedPreferences.edit().putString("activityTwo", activityFieldTwoText).apply();
                        sharedPreferences.edit().putString("activityThree", activityFieldThreeText).apply();

                        /// if currently editing activities, send toast indicating change, then return to settings screen
                        if (isEditingActivities == true) {
                            Toast.makeText(view.getContext(), "Activities have been updated", Toast.LENGTH_LONG).show();
                            screen_count = 7;
                            screenCheck();
                            isEditingActivities = false;
                            return;
                        }

                        screen_count = 6;
                        screenCheck();
                    }
                }
            });
        }

        ///  screen used to recommend user to enable notifications and queue workrequest
        else if (screen_count == 6) {
            setContentView(R.layout.noti_screen);
            nextScreenBtnSix = (Button)findViewById(R.id.nextScreenBtnSix);
            notiEnableBtn = (Button)findViewById(R.id.notiEnableBtn);

            ///this function is necessary to enable notifications on android apis above 33
            notiEnableBtn.setOnClickListener(view -> {
                /// if notifications aren't enabled, asks user if they want to enable notifications
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ActivityCompat.requestPermissions(this, new String[]{(Manifest.permission.POST_NOTIFICATIONS)}, 12);
                    }
                }
            });

            /// builds periodic work request -- repeats in 1 week intervals
            final PeriodicWorkRequest periodicWorkRequest =
                    new PeriodicWorkRequest.Builder(WorkerClass.class, 5, TimeUnit.SECONDS, 15, TimeUnit.MINUTES)
                            //TODO: adjust repeatinterval to "1, TimeUnit.WEEKS" for prod
                            .addTag("periodicWork")
                            .build();

            /// sets isOnboardingComplete to true to indicate that the onboarding process is completed, then queues the workrequest with WorkManager,
            /// then sends user to main screen, then logs the old usage time
            nextScreenBtnSix.setOnClickListener(view -> {
                sharedPreferencesOnBoarding.edit().putBoolean("isOnboardingComplete", true).apply();

                //may also put this at end of evaluation screens, due to an issue with the work request not being sent properly
                WorkManager.getInstance().enqueue(periodicWorkRequest);
                screen_count = 0;
                screenCheck();
                sharedPreferencesUsageTime.edit().putLong("oldUsageTime", totalTime).apply();
                Log.d("totalTime", String.valueOf(totalTime));
            });
        }

        /// settings screen where user can compose email, or edit goal
        else if (screen_count == 7) {
            setContentView(R.layout.settings_screen);
            emailBtn = (Button)findViewById(R.id.emailBtn);

            /// composes an email containing usage stats (see prepEmail() for more info)
            emailBtn.setOnClickListener(view -> {
                prepEmail();
            });
            editGoalBtn = (Button)findViewById(R.id.editGoalBtn);

            /// sends user to edit goal screen
            editGoalBtn.setOnClickListener(view -> {
                screen_count = 8;
                screenCheck();
            });

            editActivitiesBtn = (Button)findViewById(R.id.editActivitiesBtn);
            editActivitiesBtn.setOnClickListener(view -> {
                isEditingActivities = true;
                screen_count = 5;
                screenCheck();
            });

            /// sends user back to main screen
            backToMainBtn = (Button)findViewById(R.id.backToMainBtn);
            backToMainBtn.setOnClickListener(view -> {
                screen_count = 0;
                screenCheck();
            });

        }

        /// screen used to redefine goal via settings screen
        else if (screen_count == 8) {
            setContentView(R.layout.redefine_goal_screen);
            submitNewGoalBtn = (Button)findViewById(R.id.submitNewGoalBtn);
            backToSettingsBtn = (Button)findViewById(R.id.backToSettingsBtn);
            newGoalEntryField = (EditText)findViewById(R.id.newGoalEntryField);
            currentGoalText = (TextView)findViewById(R.id.currentGoalText);
            String oldGoal =  sharedPreferences.getString("goalEntry", "none");

            currentGoalText.setText("Your current goal is: " + oldGoal);

            submitNewGoalBtn.setOnClickListener(view -> {
                newGoalEntryText = newGoalEntryField.getText().toString();
                /// checks if new goal field is empty, then displays message asking user to enter a goal
                if (newGoalEntryText.isEmpty()) {
                    Toast.makeText(view.getContext(), "Please enter a goal to continue", Toast.LENGTH_SHORT).show();

                /// if new goal is the same as old goal, lets user know that their new goal can't be the same as their old goal
                } else if (oldGoal.equals(newGoalEntryText)) {
                    Toast.makeText(view.getContext(), "New goal cannot be identical to old goal", Toast.LENGTH_SHORT).show();
                }
                /// updates goal, then goes back to settings screen
                else {
                    sharedPreferences.edit().putString("goalEntry", newGoalEntryText).apply();
                    Toast.makeText(view.getContext(), "Goal has been updated to: " + newGoalEntryText, Toast.LENGTH_LONG).show();
                    screen_count = 7;
                    screenCheck();
                }
            });

            /// goes back to settings screen
            backToSettingsBtn.setOnClickListener(view -> {
                screen_count = 7;
                screenCheck();
            });


        }
        /// screen used to give appraisal to user if their current usage is lower than last weeks usage
        else if (screen_count == 9) {
            setContentView(R.layout.appraise_screen);
            backToMainBtn2 = (Button)findViewById(R.id.backToMainBtn2);

            goalText = (TextView)findViewById(R.id.goalText);
            String goalName = sharedPreferences.getString("goalEntry", "none");
            goalText.setText(goalName);

            /// sends user back to main screen
            backToMainBtn2.setOnClickListener(view -> {
                screen_count = 0;
                screenCheck();
            });
        }
        /// screen used to give advice to user if their current usage is higher than lask weeks usage
        else if (screen_count == 10) {
            setContentView(R.layout.usage_evaluation_screen);
            redefineGoalYesBtn = (Button)findViewById(R.id.redefineGoalYesBtn);
            redefineGoalNoBtn = (Button)findViewById(R.id.redefineGoalNoBtn);

            goalText = (TextView)findViewById(R.id.goalText);
            String goalName = sharedPreferences.getString("goalEntry", "none");
            goalText.setText(goalName);

            /// if user doesn't want to redefine goal, sends user to next advice screen (activity_suggestion_screen)
            redefineGoalNoBtn.setOnClickListener(view -> {
                screen_count = 11;
                screenCheck();
            });

            /// if user wants to redefine goal, sets variable isRedefiningGoal to true, sends user to goal entry screen
            redefineGoalYesBtn.setOnClickListener(view -> {
                isRedefiningGoal = true;
                screen_count = 4;
                screenCheck();
            });

        }
        /// screen used to suggest user replace phone usage with inputted activities (and some of my own)
        else if (screen_count == 11) {
            setContentView(R.layout.activity_suggestion_screen);
            backToMainBtn3 = (Button)findViewById(R.id.backToMainBtn3);
            activitiesText = (TextView)findViewById(R.id.activitiesText);

            /// creates a string array containing a list of activities (see strings.xml), then picks two at random to add to the activity suggestion text
            String[] activities = getResources().getStringArray(R.array.activities_array);
            Random random = new Random();
            int randomNumberOne = random.nextInt(activities.length - 1);
            int randomNumberTwo = random.nextInt(activities.length - 1);

            String randomActivityOne = activities[randomNumberOne];
            String randomActivityTwo = activities[randomNumberTwo];

            String userActivityOne = sharedPreferences.getString("activityOne", "none");
            String userActivityTwo = sharedPreferences.getString("activityTwo", "none");
            String userActivityThree = sharedPreferences.getString("activityThree", "none");

            Log.d("randActivityOne", randomActivityOne);
            Log.d("randActivityTwo", randomActivityTwo);

            Log.d("userActivityOne", userActivityOne);
            Log.d("userActivityTwo", userActivityTwo);
            Log.d("userActivityThree", userActivityThree);

            activitiesText.setText(userActivityOne + ", " + randomActivityOne + ", " + userActivityTwo + ", " + randomActivityTwo + ", " + userActivityThree);

            /// sends user back to main screen
            backToMainBtn3.setOnClickListener(view -> {
                screen_count = 0;
                screenCheck();
            });
        }
        /// screen used to display last weeks usage stats
        else if (screen_count == 12) {
            setContentView(R.layout.previous_week);
            ListView previousWeekList = (ListView)findViewById(R.id.prevWeekList);
            currentGraphBtn = (Button)findViewById(R.id.currentGraphBtn);

            /// retrieves usage stats from prior week via appDataDetailsPrev.csv and adds to an array list
            ArrayList<String> previousWeekArrayList = new ArrayList<>();
            try {
                String fileNamePrevWeek = "appDataDetailsPrev.csv";
                File exFileDirPrevWeek = this.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                File fileToShow = new File (exFileDirPrevWeek, fileNamePrevWeek);

                CSVReader csvReader = new CSVReaderBuilder(new FileReader(fileToShow)).withSkipLines(1).build();
                String[] nextLine;
                while ((nextLine = csvReader.readNext()) != null) {
                    Log.d("prevWeek", nextLine[0]);
                    previousWeekArrayList.add(nextLine[0]);
                }
            }
            /// if appDataDetailsPrev.csv doesn't exist (is only created on a weekly basis), shows alert asking user to check back later
            catch (CsvValidationException | IOException e) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("No data to show");
                builder.setMessage("No previous data exists to show, please check back at a later date.")
                        .setCancelable(true)
                        .setPositiveButton("Got it", (dialog, which) -> dialog.dismiss());
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
                e.printStackTrace();
            }
            /// adds list to listview
            ArrayAdapter<String> previousWeekArrayAdapter = new ArrayAdapter<String>(this, R.layout.previous_week_details, previousWeekArrayList);
            previousWeekList.setAdapter(previousWeekArrayAdapter);

            /// sends user back to main screen
            currentGraphBtn.setOnClickListener(view -> {
                screen_count = 0;
                screenCheck();
            });
        }
    }


    /// function used to load usage stats
    public void loadUsage() throws PackageManager.NameNotFoundException {
        long lastWeek = System.currentTimeMillis() - (1000 * 3600 * 24 * 7);
        UsageStatsManager usm = (UsageStatsManager) this.getSystemService(USAGE_STATS_SERVICE);

        /// gets list of app usage data from the last week
        List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_WEEKLY,
                lastWeek, System.currentTimeMillis());
        appList = appList.stream().filter(app -> app.getTotalTimeInForeground() > 0).collect(Collectors.toList());

        Log.d("usagestats", appList.toString());

        /// if the applist isnt empty, maps it to a treemap, then calls the showusage() function
        if (!appList.isEmpty()) {
            Map<String, UsageStats> sortedMap = new TreeMap<>();
            for (UsageStats usageStats : appList) {
                sortedMap.put(usageStats.getPackageName(), usageStats);
            }
            Log.d("moreStats", sortedMap.toString());
            showUsage(sortedMap);
        }
    }

        /// function used to display usage in the form of a list view
        public void showUsage(Map<String, UsageStats> sortedMap) throws PackageManager.NameNotFoundException {
        ArrayList<AppDetails> appDetailsArrayList = new ArrayList<>();
        List<UsageStats> usageStatsList = new ArrayList<>(sortedMap.values());

        Collections.sort(usageStatsList, (z1, z2) ->
                Long.compare(z1.getTotalTimeInForeground(), z2.getTotalTimeInForeground()));

        /// sets totalTime based on the sum of app usage time
        totalTime = usageStatsList.stream().map(UsageStats::getTotalTimeInForeground).mapToLong(Long::longValue).sum();

        /// for each app,
        for (UsageStats usageStats : usageStatsList) {
            try {
                String packageName = usageStats.getPackageName();
                Drawable packageIcon = getDrawable(R.mipmap.ic_launcher_round);
                String[] packageNames = packageName.split("\\.");
                String appName = packageNames[packageNames.length - 1].trim();

                /// checks if information about the current app exists, if so, changes app icon and name to corresponding details
                if (doesAppInfoExist(usageStats)) {
                    ApplicationInfo applicationInfo = getApplicationContext().getPackageManager().getApplicationInfo(packageName, 0);
                    packageIcon = getApplicationContext().getPackageManager().getApplicationIcon(applicationInfo);
                    appName = getApplicationContext().getPackageManager().getApplicationLabel(applicationInfo).toString();
                }

                /// gets percentage and usage time of each app
                int usagePercent = (int) (usageStats.getTotalTimeInForeground() * 100 / totalTime);
                String usageTime = convertUsageTime(usageStats.getTotalTimeInForeground());

                /// adds app details to the arraylist
                AppDetails usageStatThing = new AppDetails(packageIcon, appName, usagePercent, usageTime);
                appDetailsArrayList.add(usageStatThing);



            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

        }
        /// reverses array list to show most used app first, then calls function to create file to store app usage data
        Collections.reverse(appDetailsArrayList);
        generateFile(appDetailsArrayList);

        /// adds array list of app details to listview
        AppAdapter appAdapter = new AppAdapter(this, appDetailsArrayList);

        ListView appListView = findViewById(R.id.appListView);
        appListView.setAdapter(appAdapter);


    }


    /// function used to create the file(s) containing app usage stats
    private void generateFile(ArrayList<AppDetails> appDetails) {
        String fileName = "appDataDetails.csv";
        File exFileDir = this.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        fileToEmail = new File (exFileDir, fileName);

        boolean notiSent = sharedPreferencesNotiSent.getBoolean("notiSent", false);

        /// part of function only runs on weekly check
        if (notiSent) {
            /// if appDataDetails.csv already exists, creates a similar file, appDataDetailsPrev.csv
            if (fileToEmail.exists()) {
                Log.d("fileExists", "file " + fileName + " already exists, creating file for previous week");
                String fileNamePrevWeek = "appDataDetailsPrev.csv";
                File exFileDirPrevWeek = this.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                fileToEmail = new File (exFileDirPrevWeek, fileNamePrevWeek);

                /// writes the details of the current app to the file
                try {
                    FileWriter fileWriter = new FileWriter(fileToEmail);
                    for (int i = 0; i < appDetails.size(); i++) {
                        fileWriter.append("\n").append(appDetails.get(i).appName)
                                .append("  |  ").append(appDetails.get(i).usageTime)
                                .append("  |  ").append(String.valueOf(appDetails.get(i).usagePercent))
                                .append("%");                    }
                    fileWriter.flush();
                    fileWriter.close();
                    Log.d("fileStuffPrevWeek", "File " + fileNamePrevWeek + " created successfully, located at: " + exFileDirPrevWeek);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
        }

        /// writes the details of the current app to the file
        try {
            FileWriter fileWriter = new FileWriter(fileToEmail);
            for (int i = 0; i < appDetails.size(); i++) {
                fileWriter.append("\n").append(appDetails.get(i).appName)
                        .append("  |  ").append(appDetails.get(i).usageTime)
                        .append("  |  ").append(String.valueOf(appDetails.get(i).usagePercent))
                        .append("%");
            }
            fileWriter.flush();
            fileWriter.close();
            Log.d("fileStuff", "File " + fileName + " created successfully, located at: " + exFileDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /// function used to check if information about the app exists
    private boolean doesAppInfoExist(UsageStats usageStats) {
        try {
            getApplicationContext().getPackageManager().getApplicationInfo(usageStats.getPackageName(), 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /// converts raw usage time (measured in milliseconds) to hours and minutes, and returns both values with attached strings
    private String convertUsageTime(long milliseconds) {
        if (milliseconds < 0) {
            throw new IllegalArgumentException("IllegalArgument");
        }
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds); milliseconds -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);

        return (hours + "hr " + minutes + "min ");
    }
    /// function used to prepare the email containing usage stats for the week
    public void prepEmail() {
        Uri fileUri = FileProvider.getUriForFile(this, getPackageName()+".provider", fileToEmail);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("vnd.android.cursor.dir/email");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"ldebuf300@caledonian.ac.uk" });
        intent.putExtra(Intent.EXTRA_STREAM, fileUri);
        intent.putExtra(Intent.EXTRA_SUBJECT, "Weekly email with my app usage stats");
        startActivity(Intent.createChooser(intent, "Send email using: "));
    }

}