/*
 * Copyright 2009, 2010, 2011, 2012, 2013, 2014 Tobias Fleig (tobifleig gmail com)
 *
 * All rights reserved.
 *
 * This file is part of LanXchange.
 *
 * LanXchange is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LanXchange is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LanXchange. If not, see <http://www.gnu.org/licenses/>.
 */
package de.tobifleig.lxc.plaf.android.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.view.View;
import de.tobifleig.lxc.LXC;
import de.tobifleig.lxc.R;
import de.tobifleig.lxc.plaf.android.ui.AboutElement;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AboutActivity extends KeepServiceRunningActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // load layout
        setContentView(R.layout.activity_about);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // fill in dynamic content
        ((AboutElement) findViewById(R.id.about_version)).setText(LXC.versionString);
        ((AboutElement) findViewById(R.id.about_version_internal)).setText(Integer.toString(LXC.versionId));


        // handle input
        findViewById(R.id.about_mailme).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto",getResources().getString(R.string.about_mailme_text), null)));
            }
        });
        findViewById(R.id.about_license).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.about_license_text))));
            }
        });
        findViewById(R.id.about_source).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.about_source_text))));
            }
        });
        findViewById(R.id.about_twitter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.about_twitter_text))));
            }
        });
        findViewById(R.id.about_debug_showlog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri fileUri = FileProvider.getUriForFile(getApplicationContext(), "de.tobifleig.lxc.fileprovider", new File(getCacheDir(), "lxc.log"));
                final Intent openIntent = new Intent();
                openIntent.setAction(Intent.ACTION_VIEW);
                openIntent.setDataAndType(fileUri, "text/plain");
                openIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(openIntent);
            }
        });
        findViewById(R.id.about_debug_sendlogs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // send all existing log files
                // this uses a trick: resolve responding activities based on ACTION_SENDTO (to get email apps only),
                // but send using ACTION_SEND_MULTIPLE to make file attachments work
                ArrayList<Uri> existingLogs = new ArrayList<>();
                // search for existing log files
                File baseDir = getCacheDir();
                File current = new File(baseDir, "lxc.log");
                if (current.exists()) {
                    existingLogs.add(FileProvider.getUriForFile(getApplicationContext(), "de.tobifleig.lxc.fileprovider", current));
                }
                for (int i = 1; i < 3; i++) {
                    File oldlog = new File(baseDir, "lxc_oldlog" + i + ".log");
                    if (oldlog.exists()) {
                        existingLogs.add(FileProvider.getUriForFile(getApplicationContext(), "de.tobifleig.lxc.fileprovider", oldlog));
                    }
                }
                Intent probeIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + Uri.encode("mail@lanxchange.com")));
                probeIntent.putExtra(Intent.EXTRA_SUBJECT, "LanXchange debug log files");
                List<ResolveInfo> resolveInfos = getPackageManager().queryIntentActivities(probeIntent, 0);
                List<LabeledIntent> intents = new ArrayList<>();
                for (ResolveInfo resolveInfo : resolveInfos) {
                    Intent mailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                    mailIntent.setComponent(new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name));
                    mailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"mail@lanxchange.com"});
                    mailIntent.putExtra(Intent.EXTRA_SUBJECT, "LanXchange debug log files");
                    mailIntent.putExtra(Intent.EXTRA_TEXT, "This is an automatic mail to the developer of LanXchange.\n" +
                                                                    "LanXchange added the log files as attachments to this email.\n" +
                                                                    "Please describe your problem briefly, then press send.");
                    mailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, existingLogs);
                    mailIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intents.add(new LabeledIntent(mailIntent, resolveInfo.activityInfo.packageName, resolveInfo.loadLabel(getPackageManager()), resolveInfo.icon));
                }
                Intent chooser = Intent.createChooser(intents.remove(intents.size() - 1), "Send email with log files...");
                chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toArray(new LabeledIntent[intents.size()]));
                startActivity(chooser);
            }
        });
        findViewById(R.id.about_license_icons).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://creativecommons.org/licenses/by/4.0/")));
            }
        });
        findViewById(R.id.about_license_support).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.apache.org/licenses/LICENSE-2.0")));
            }
        });
        findViewById(R.id.about_license_filechooser).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.apache.org/licenses/LICENSE-2.0")));
            }
        });
    }

}
