package com.projectcyber.beaver.data;

import android.os.Bundle;

import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.aboutlibraries.ui.LibsActivity;
import com.projectcyber.beaver.R;

public class CustomAboutLibrariesActivity extends LibsActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setIntent(new LibsBuilder()
        .withActivityTitle(getResources().getString(R.string.open_source_libraries))
        .withAutoDetect(true)
        .withAboutIconShown(true)
        .withLicenseShown(true)
        .intent(this));
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
