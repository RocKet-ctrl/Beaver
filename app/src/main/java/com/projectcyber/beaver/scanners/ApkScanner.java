package com.projectcyber.beaver.scanners;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.os.AsyncTask;
import android.os.Build;

import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.ref.WeakReference;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import androidx.annotation.RequiresApi;
import com.projectcyber.beaver.R;
import com.projectcyber.beaver.activities.AppDetails;
import com.projectcyber.beaver.utils.AppConstants;
import com.projectcyber.beaver.utils.Sha256HashExtractor;

public class ApkScanner extends AsyncTask<Void, String, Void> {
    private final WeakReference<Context> contextRef;
    ProgressDialog pd;

    private Interpreter tflite = null;
    private JSONArray p_jArray = null;
    private JSONArray i_jArray = null;

    private final String filePath;
    private String packageName;
    private String appName;
    private String prediction;

    private float predictionScore;

    private ArrayList<String> appPermissionsList= new ArrayList<>();
    private String sha256Hash;

    public ApkScanner(Context context, String path) {
        contextRef = new WeakReference<>(context);
        this.filePath = path;
    }

    @Override
    protected void onPreExecute() {
        pd = new ProgressDialog(contextRef.get());
        pd.setMessage("Scanning...");
        pd.show();

        super.onPreExecute();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected Void doInBackground(Void... voids) {
        float[] inputVal = new float[2000];

        try {
            tflite = new Interpreter(loadModelFile(), null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            // Loading the features.json from assets folder.
            JSONObject obj = new JSONObject(loadJSONFromAsset());
            p_jArray = obj.getJSONArray("permissions");// This array stores permissions from features.json file
            i_jArray = obj.getJSONArray("intents");// This array  stores intents from features.json file
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            boolean flag = false;
            ApkFile apkFile = new ApkFile(new File(filePath));
            String manifestXml = apkFile.getManifestXml();
            ApkMeta apkMeta = apkFile.getApkMeta();
            packageName = apkMeta.getPackageName();
            appName = apkMeta.getLabel();
            appPermissionsList = getListOfPermissions(manifestXml);
            ArrayList<String> appIntentsList = getListOfIntents(manifestXml);

            String str;

            if (appPermissionsList.size() == 0 && appIntentsList.size() == 0) {
                prediction = contextRef.get().getString(R.string.unknown);
                return null;
            }

            for (int i = 0; i < p_jArray.length(); i++) {
                str = p_jArray.optString(i);
                if ((appPermissionsList.contains(str))) {
                    inputVal[i] = 1;
                    flag = true;
                } else {
                    inputVal[i] = 0;
                }
            }


            for (int i = 0; i < i_jArray.length(); i++) {
                str = i_jArray.optString(i);
                if ((appIntentsList.contains(str))) {
                    inputVal[i + 489] = 1;
                    flag = true;
                } else {
                    inputVal[i + 489] = 0;
                }
            }

            if(!flag) {
                prediction = contextRef.get().getString(R.string.unknown);
                return null;
            }

            String hash = Sha256HashExtractor.getSha256Hash(filePath);
            if (hash != null) {
                sha256Hash = hash;
            }

            float[][] outputVal = new float[1][1];

            // Run the model
            tflite.run(inputVal, outputVal);
            float inferredValue = outputVal[0][0];
            predictionScore=inferredValue;
            if (inferredValue > 0.75) {
                prediction = contextRef.get().getString(R.string.malware);
            } else if (inferredValue > 0.5) {
                prediction = contextRef.get().getString(R.string.risky);
            } else {
                prediction = contextRef.get().getString(R.string.safe);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    @Override
    protected void onPostExecute(Void aVoid) {
        Intent intent =new Intent(contextRef.get(), AppDetails.class);
        intent.putExtra("appName",appName);
        intent.putExtra("packageName",packageName);
        intent.putExtra("result",prediction);
        intent.putExtra("prediction",predictionScore);
        intent.putExtra("scan_mode","apk_scan");
        intent.putStringArrayListExtra("permissionList",appPermissionsList);
        intent.putExtra(AppConstants.SHA_256_HASH, sha256Hash);

        if (pd != null && pd.isShowing()) {
            pd.dismiss();
        }

        contextRef.get().startActivity(intent);
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = contextRef.get().getAssets().openFd("saved_model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
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

    private static ArrayList<String> getListOfPermissions(String manifestXml) {
        ArrayList<String> arr = new ArrayList<>();

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new StringReader(manifestXml));
            int eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if ((eventType == XmlPullParser.START_TAG) && "uses-permission".equals(xpp.getName())) {
                    for (byte i = 0; i < xpp.getAttributeCount(); i++) {
                        if (xpp.getAttributeName(i).equals("name")) {
                            arr.add(xpp.getAttributeValue(i));
                        }
                    }
                }
                eventType = xpp.next();
            }
        } catch (final XmlPullParserException | IOException exception) {
            exception.printStackTrace();
        }
        return arr;
    }

    private static ArrayList<String> getListOfIntents(String manifestXml) {
        ArrayList<String> arr = new ArrayList<>();

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new StringReader(manifestXml));
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if ((eventType == XmlPullParser.START_TAG) && "action".equals(xpp.getName())) {
                    for (byte i = 0; i < xpp.getAttributeCount(); i++) {
                        if (xpp.getAttributeName(i).equals("name")) {
                            arr.add(xpp.getAttributeValue(i));
                        }
                    }
                }
                eventType = xpp.next();
            }
        } catch (final XmlPullParserException | IOException exception) {
            exception.printStackTrace();
        }
        return arr;
    }
}
