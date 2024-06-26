

package com.projectcyber.beaver.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.MenuItem;
import com.projectcyber.beaver.R;
import com.projectcyber.beaver.adapters.AppsAdapter;
import com.projectcyber.beaver.data.AppInfo;

import java.util.ArrayList;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


public class ResultActivity extends AppCompatActivity {
    RecyclerView resultList;
    RecyclerView.LayoutManager layoutManager;
    AppsAdapter appsAdapter;
    public static ArrayList<AppInfo> apps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = this.getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(this.getString(R.string.report));
        }

        setContentView(R.layout.activity_result);

        resultList = findViewById(R.id.resultList);

        layoutManager = new LinearLayoutManager(this);
        resultList.setLayoutManager(layoutManager);

        appsAdapter = new AppsAdapter(this, apps);
        resultList.setAdapter(appsAdapter);
    }

    private void sendMessage() {
        //Log.d("sender", "Broadcasting message");
        Intent intent = new Intent("uninstall");
        intent.putExtra("uninstall", "yes");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                //Log.d("TAG", "onActivityResult: user accepted the (un)install");
                sendMessage();
            }
        }
    }
}
