package com.projectcyber.beaver.scanners;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.XmlResourceParser;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.projectcyber.beaver.R;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.projectcyber.beaver.activities.ResultActivity;
import com.projectcyber.beaver.data.AppInfo;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ScannerTask extends AsyncTask<Void, String, Void> {
    private final WeakReference<Context> contextRef;
    private final WeakReference<Activity> activityRef;

    private Interpreter tflite = null;
    private JSONArray p_jArray = null;
    private JSONArray i_jArray = null;
    private final ArrayList<AppInfo> goodware = new ArrayList<>();
    private final ArrayList<AppInfo> malware = new ArrayList<>();
    private final ArrayList<AppInfo> unknown = new ArrayList<>();
    private final ArrayList<AppInfo> risky = new ArrayList<>();
    private final ArrayList<AppInfo> scannedApps = new ArrayList<>();

    private WeakReference<ProgressBar> pb;
    private WeakReference<TextView> st1;
    private WeakReference<TextView> st2;
    private WeakReference<TextView> pTxt;

    private int installedAppsCount;
    private int status = 0;

    private boolean withSysApps;

    private NotificationManagerCompat notificationManager;
    private final int NOTIFICATION_ID = 100;

    public ScannerTask(Context context, Activity activity) {
        contextRef = new WeakReference<>(context);
        this.activityRef = new WeakReference<>(activity);
    }

    public void setProgressBar(ProgressBar progressBar) {
        this.pb = new WeakReference<>(progressBar);
    }

    public void setPercentText(TextView perTxt) {
        this.pTxt = new WeakReference<>(perTxt);
    }

    public void setStatusText(TextView statusText) {
        this.st1 = new WeakReference<>(statusText);
    }

    public void setSecondaryStatusText(TextView secondaryStatusText) {
        this.st2 = new WeakReference<>(secondaryStatusText);
    }

    public void setWithSysApps(boolean prefValue) {
        this.withSysApps = prefValue;
    }

    @Override
    protected Void doInBackground(Void... param) {
        float[] inputVal = new float[2000];

        try {
            tflite = new Interpreter(loadModelFile(), null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            // Loading the features.json from assets folder. Refer loadJSONFromAsset() function for more details
            JSONObject obj = new JSONObject(loadJSONFromAsset());
            p_jArray = obj.getJSONArray("permissions");// This array stores permissions from features.json file
            i_jArray = obj.getJSONArray("intents");// This array  stores intents from features.json file
        } catch (JSONException e) {
            e.printStackTrace();
        }
        final PackageManager pm = contextRef.get().getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo packageInfo : packages) {
            boolean flag = false;// flag is true if and only if the app under scan contains at least one permission or intent-filter defined in features.json
            String scanningAppName = packageInfo.loadLabel(pm).toString();
            publishProgress(scanningAppName);
            AppInfo app = new AppInfo(packageInfo.loadLabel(pm).toString(), packageInfo.packageName, packageInfo.publicSourceDir,packageInfo.flags & ApplicationInfo.FLAG_SYSTEM);
            app.appIcon = packageInfo.loadIcon(pm);

            if ((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1 && !withSysApps) {// checking if it is a system app.
                continue;
            } else {
                //Log.d(TAG, "Installed package :" + packageInfo.packageName);
                try {
                    
                    ArrayList<String> appPermissionsList = getListOfPermissions(contextRef.get().createPackageContext(packageInfo.packageName, 0));

                    ArrayList<String> appIntentsList = getListOfIntents(contextRef.get().createPackageContext(packageInfo.packageName, 0));

                    String str;
                    if (appPermissionsList.size() == 0 && appIntentsList.size() == 0) {
                        //Log.d(TAG,"No permissions and intents found. Skipping...");
                        app.prediction = contextRef.get().getString(R.string.unknown);
                        unknown.add(app);
                        continue;
                    }

                    // The following for loops are used to create the input feature vector
                    for (int i = 0; i < p_jArray.length(); i++) {
                        str = p_jArray.optString(i);
                        if ((appPermissionsList.contains(str))) {
                            inputVal[i] = 1;
                            flag = true;
                            //Log.d(scanningAppName,"Check Permissions: "+ str + " is present in appsPermissionsList.");
                        } else {
                            inputVal[i] = 0;
                            ///Log.d(scanningAppName,"Check Permissions: "+ str + " is NOT present in appsPermissionsList.");
                        }
                    }


                    for (int i = 0; i < i_jArray.length(); i++) {
                        str = i_jArray.optString(i);
                        if ((appIntentsList.contains(str))) {
                            inputVal[i + 489] = 1;
                            flag = true;
                            //Log.d(scanningAppName,"Check Intents:"+ str + " is present in appsIntentsList.");
                        } else {
                            inputVal[i + 489] = 0;
                            //Log.d(scanningAppName,"Check Intents:"+ str + " is NOT present in appsIntentsList.");
                        }
                    }
                    //Log.d("Info:", "feature vector is created.");
                    //Log.d(scanningAppName, scanningAppName+" feature vector:"+ Arrays.toString(inputVal));
                    if(!flag){
                        app.prediction = contextRef.get().getString(R.string.unknown);
                        unknown.add(app);
                        continue;
                    }

                    // To store output from the model
                    float[][] outputVal = new float[1][1];

                    // Run the model
                    tflite.run(inputVal, outputVal);

                    //long endTime = System.currentTimeMillis();
                    //elapsedTime = endTime - startTime;
                    //totalTime += elapsedTime;
                    //Log.d(TIMER, "Elapsed Time: " + Float.toString(elapsedTime));


                    float inferredValue = outputVal[0][0];
                    app.predictionScore=inferredValue;
                    app.permissionList=appPermissionsList;

                    if (inferredValue > 0.75) {
                        app.prediction = contextRef.get().getString(R.string.malware);
                        malware.add(app);
                    } else if (inferredValue > 0.5) {
                        app.prediction = contextRef.get().getString(R.string.risky);
                        risky.add(app);
                    } else {
                        app.prediction = contextRef.get().getString(R.string.safe);
                        goodware.add(app);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
            if (isCancelled()) {
                break;
            }
        }
        return null;
    }

    @Override
    protected void onCancelled() {
        notificationManager.cancel(NOTIFICATION_ID);
        super.onCancelled();
    }

    @Override
    protected void onProgressUpdate(String... values) {
        st1.get().setText(values[0]);
        status += 1;
        int percentCompleted = (int) (((float) status / installedAppsCount) * 100f);
        pTxt.get().setText(String.format("%s%%", Integer.toString(percentCompleted)));
        pb.get().setProgress(status);
        st2.get().setText(String.format("%s of %s", Integer.toString(status), Integer.toString(installedAppsCount)));
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        putDateInSharedPreference();
        Collections.sort(malware, AppInfo.appNameComparator);
        Collections.sort(risky, AppInfo.appNameComparator);
        Collections.sort(unknown, AppInfo.appNameComparator);
        Collections.sort(goodware, AppInfo.appNameComparator);
        scannedApps.addAll(malware);
        scannedApps.addAll(risky);
        scannedApps.addAll(unknown);
        scannedApps.addAll(goodware);
        //Log.i(TIMER, "Average Time: " + Float.toString(totalTime / count));
        ResultActivity.apps = scannedApps;
        Intent resultScreen = new Intent(contextRef.get(), ResultActivity.class);

        notificationManager.cancel(NOTIFICATION_ID);

        //resultScreen.putParcelableArrayListExtra("appslist", scannedApps);
        this.activityRef.get().finish();
        contextRef.get().startActivity(resultScreen);
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onPreExecute() {
        //Lock screen orientation to prevent UI freeze
        int currentOrientation = activityRef.get().getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_PORTRAIT)
            activityRef.get().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else
            activityRef.get().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        installedAppsCount = contextRef.get().getPackageManager().getInstalledApplications(0).size();
        pb.get().setMax(installedAppsCount);

        notificationManager = NotificationManagerCompat.from(contextRef.get());
        String CHANNEL_ID = "channel_100";
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(contextRef.get(), CHANNEL_ID);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_NAME = "Project Beaver";
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(mChannel);
        }
        notificationBuilder.setSmallIcon(R.drawable.malwareicon)
                .setOngoing(true)
                .setContentTitle("Beaver")
                .setContentText(contextRef.get().getString(R.string.scanningApplications))
                .setProgress(0, 0, true);
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());

    }

    private String loadJSONFromAsset() {
        String json;
        try {
            InputStream is = contextRef.get().getAssets().open("features.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            //noinspection ResultOfMethodCallIgnored
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = contextRef.get().getAssets().openFd("saved_model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private static ArrayList<String> getListOfPermissions(final Context context) {
        ArrayList<String> arr = new ArrayList<>();

        try {
            final AssetManager am = context.createPackageContext(context.getPackageName(), 0).getAssets();
            final Method addAssetPath = am.getClass().getMethod("addAssetPath", String.class);
            final int cookie = (Integer) addAssetPath.invoke(am, context.getPackageManager().getApplicationInfo(context.getPackageName(), 0).sourceDir);
            final XmlResourceParser xmlParser = am.openXmlResourceParser(cookie, "AndroidManifest.xml");
            int eventType = xmlParser.next();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if ((eventType == XmlPullParser.START_TAG) && "uses-permission".equals(xmlParser.getName())) {
                    for (byte i = 0; i < xmlParser.getAttributeCount(); i++) {
                        if (xmlParser.getAttributeName(i).equals("name")) {
                            arr.add(xmlParser.getAttributeValue(i));
                        }
                    }
                }
                eventType = xmlParser.next();
            }
            xmlParser.close();
        } catch (final XmlPullParserException exception) {
            exception.printStackTrace();
        } catch (final PackageManager.NameNotFoundException exception) {
            exception.printStackTrace();
        } catch (final IOException exception) {
            exception.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return arr;

    }

    private static ArrayList<String> getListOfIntents(final Context context) {
        ArrayList<String> arr = new ArrayList<>();

        try {
            final AssetManager am = context.createPackageContext(context.getPackageName(), 0).getAssets();
            final Method addAssetPath = am.getClass().getMethod("addAssetPath", String.class);
            final int cookie = (Integer) addAssetPath.invoke(am, context.getPackageManager().getApplicationInfo(context.getPackageName(), 0).sourceDir);
            final XmlResourceParser xmlParser = am.openXmlResourceParser(cookie, "AndroidManifest.xml");
            int eventType = xmlParser.next();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if ((eventType == XmlPullParser.START_TAG) && "action".equals(xmlParser.getName())) {
                    for (byte i = 0; i < xmlParser.getAttributeCount(); i++) {
                        if (xmlParser.getAttributeName(i).equals("name")) {
                            arr.add(xmlParser.getAttributeValue(i));
                        }
                    }
                }
                eventType = xmlParser.next();
            }
            xmlParser.close();
        } catch (final XmlPullParserException exception) {
            exception.printStackTrace();
        } catch (final PackageManager.NameNotFoundException exception) {
            exception.printStackTrace();
        } catch (final IOException exception) {
            exception.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return arr;
    }

    private void putDateInSharedPreference() {
        String curDateTime = java.text.DateFormat.getDateTimeInstance().format(new Date());
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(contextRef.get());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("lastScan", curDateTime);
        editor.apply();
    }
}
