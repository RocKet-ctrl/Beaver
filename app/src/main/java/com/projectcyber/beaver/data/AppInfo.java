

package com.projectcyber.beaver.data;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Comparator;

public class AppInfo implements Parcelable {
    public String appName;
    public String packageName;
    private String filePath;
    public String prediction;
    public Drawable appIcon = null;
    public int isSystemApp = 0;
    public float predictionScore;
    public ArrayList<String> permissionList;
    Context context;
    public String sha256Hash;

    public AppInfo(Parcel source) {
        appName = source.readString();
        packageName = source.readString();
        filePath = source.readString();
        prediction = source.readString();
        isSystemApp=source.readInt();
        predictionScore = source.readFloat();
        permissionList = source.readArrayList(String.class.getClassLoader());
        sha256Hash = source.readString();
    }

    public AppInfo(String appName, String packName, String sourceDir,int isSystemApp) {
        this.appName = appName;
        this.packageName = packName;
        this.filePath = sourceDir;
        this.isSystemApp = isSystemApp;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(appName);
        dest.writeString(packageName);
        dest.writeString(filePath);
        dest.writeString(prediction);
        dest.writeInt(isSystemApp);
        dest.writeFloat(predictionScore);
        dest.writeList(permissionList);
        dest.writeString(sha256Hash);
    }

    public Drawable loadIcon(Context context) {
        try {
            return context.getPackageManager().getPackageArchiveInfo(filePath, 0).applicationInfo.loadIcon(context.getPackageManager());
        } catch (NullPointerException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Comparator<AppInfo> appNameComparator = new Comparator<AppInfo>() {
        @Override
        public int compare(AppInfo o1, AppInfo o2) {
            String appName1 = o1.appName.toLowerCase();
            String appName2 = o2.appName.toLowerCase();
            return appName1.compareTo(appName2);
        }
    };

    public static final Parcelable.Creator<AppInfo> CREATOR = new Parcelable.Creator<AppInfo>() {
        public AppInfo getAppInfo(Context context, android.content.pm.ApplicationInfo app) {
            return new AppInfo(
                    app.loadLabel(context.getPackageManager()).toString(),
                    app.packageName,
                    app.publicSourceDir,
                    app.flags & ApplicationInfo.FLAG_SYSTEM
            );
        }

        @Override
        public AppInfo createFromParcel(Parcel source) {
            return new AppInfo(source);
        }

        @Override
        public AppInfo[] newArray(int size) {
            return new AppInfo[size];
        }
    };
}
