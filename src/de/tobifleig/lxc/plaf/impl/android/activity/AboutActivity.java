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
package de.tobifleig.lxc.plaf.impl.android.activity;

import android.content.Intent;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import de.tobifleig.lxc.LXC;
import de.tobifleig.lxc.R;

public class AboutActivity extends KeepServiceRunningActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // load layout
        setContentView(R.layout.activity_about);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        final LayoutInflater inflater = getLayoutInflater();

        // fill with content
        ListView contentList = (ListView) findViewById(R.id.about_content);
        contentList.setAdapter(new ListAdapter() {

            @Override
            public void unregisterDataSetObserver(DataSetObserver observer) {
                // ignore
            }

            @Override
            public void registerDataSetObserver(DataSetObserver observer) {
                // ignore
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }

            @Override
            public int getViewTypeCount() {
                return 6;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View cell = inflater.inflate(R.layout.about_element, null);
                TextView header = (TextView) cell.findViewById(R.id.about_header);
                TextView text = (TextView) cell.findViewById(R.id.about_text);
                switch (position) {
                case 0:
                    header.setText(R.string.about_version);
                    text.setText(LXC.versionString);
                    break;
                case 1:
                    header.setText(R.string.about_copyright);
                    text.setText(R.string.about_copyright_text);
                    break;
                case 2:
                    header.setText(R.string.about_mailme);
                    text.setText(R.string.about_mailme_text);
                    break;
                case 3:
                    header.setText(R.string.about_license);
                    text.setText(R.string.about_license_text);
                    break;
                case 4:
                    header.setText(R.string.about_source);
                    text.setText(R.string.about_source_text);
                    break;
                case 5:
                    header.setText(R.string.about_twitter);
                    text.setText(R.string.about_twitter_text);
                    break;
                case 6:
                    header.setText(R.string.about_version_internal);
                    text.setText(Integer.toString(LXC.versionId));
                    break;
                }
                return cell;
            }

            @Override
            public int getItemViewType(int position) {
                return Adapter.IGNORE_ITEM_VIEW_TYPE;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public Object getItem(int position) {
                return null;
            }

            @Override
            public int getCount() {
                return 7;
            }

            @Override
            public boolean isEnabled(int position) {
                return true;
            }

            @Override
            public boolean areAllItemsEnabled() {
                return true;
            }
        });

        // handle input
        contentList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                case 0:
                case 1:
                    // do nothing
                    break;
                case 2:
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.about_mailme_text))));
                    break;
                case 3:
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.about_license_text))));
                    break;
                case 4:
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.about_source_text))));
                    break;
                case 5:
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.about_twitter_text))));
                    break;
                case 6:
                    // do nothing
                    break;
                }
            }

        });

    }

}
