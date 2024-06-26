package com.projectcyber.beaver.adapters;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cn.nekocode.badge.BadgeDrawable;
import com.projectcyber.beaver.R;
import com.projectcyber.beaver.scanners.AppScanner;

public class AppsListAdapter extends RecyclerView.Adapter {

    Context context;
    private final List<ApplicationInfo> apps;
    private final PackageManager packageManager;

    public AppsListAdapter(Context context, List<ApplicationInfo> appsList) {
        this.context = context;
        apps = appsList;
        packageManager = context.getPackageManager();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appLabel;
        TextView packageName;
        ImageView uninstallButton;

        ViewHolder(View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.itemIcon);
            appLabel = itemView.findViewById(R.id.itemLabel);
            packageName = itemView.findViewById(R.id.itemSecondaryLabel);
            uninstallButton = itemView.findViewById(R.id.uninstallButton);
        }

        void bindAppInfo(final ApplicationInfo applicationInfo) {
            appLabel.setText(applicationInfo.loadLabel(packageManager));
            appIcon.setImageDrawable(applicationInfo.loadIcon(packageManager));
            uninstallButton.setVisibility(View.INVISIBLE);
            if ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1) {
                final BadgeDrawable drawable2 =
                        new BadgeDrawable.Builder()
                                .type(BadgeDrawable.TYPE_ONLY_ONE_TEXT)
                                .badgeColor(0xff336699)
                                .text1(context.getApplicationContext().getString(R.string.system_app))
                                .build();

                SpannableString spannableString =
                        new SpannableString(TextUtils.concat(applicationInfo.packageName, "  ", drawable2.toSpannable()));
                packageName.setText(spannableString);
            } else {
                packageName.setText(applicationInfo.packageName);
            }

            itemView.setOnClickListener(v -> {
                final AppScanner scanner = new AppScanner(context, applicationInfo.packageName, "custom_scan");
                scanner.execute();
            });
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_result_list_item, parent, false);
        return new AppsListAdapter.ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AppsListAdapter.ViewHolder vh = (AppsListAdapter.ViewHolder) holder;
        vh.bindAppInfo(apps.get(position));
    }

    @Override
    public int getItemCount() {
        return ((apps == null) ? 0 : apps.size());
    }
}