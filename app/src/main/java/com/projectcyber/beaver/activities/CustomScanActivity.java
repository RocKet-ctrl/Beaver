package com.projectcyber.beaver.activities;

import android.app.ProgressDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.projectcyber.beaver.R;
import com.projectcyber.beaver.adapters.AppsListAdapter;

public class CustomScanActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;
    public static List<ApplicationInfo> apps;
    boolean withSysApps;

    private PackageManager packageManager = null;
    ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        withSysApps = getIntent().getBooleanExtra("withSysApps", false);

        ActionBar actionBar = this.getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(this.getString(R.string.custom_scan));
        }

        setContentView(R.layout.activity_result);

        recyclerView = findViewById(R.id.resultList);

        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        new LoadApplications().execute();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onRestoreInstanceState(savedInstanceState, persistentState);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        NavUtils.navigateUpFromSameTask(this);
    }

    class LoadApplications extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            packageManager = getPackageManager();
            pd = new ProgressDialog(CustomScanActivity.this);
            pd.setMessage("Loading applications...");
            pd.show();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            List<ApplicationInfo> appList = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
            apps= new ArrayList<>();
            if(withSysApps){
                apps = appList;
            } else {
                for (ApplicationInfo appInfo : appList) {
                    if (appInfo!=null && (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                        apps.add(appInfo);
                    }
                }
            }
            Collections.sort(apps, new ApplicationInfo.DisplayNameComparator(packageManager));
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (pd != null && pd.isShowing()) {
                pd.dismiss();
            }
            AppsListAdapter appsListAdapter = new AppsListAdapter(CustomScanActivity.this, apps);
            recyclerView.setAdapter(appsListAdapter);
            super.onPostExecute(aVoid);
        }
    }

}