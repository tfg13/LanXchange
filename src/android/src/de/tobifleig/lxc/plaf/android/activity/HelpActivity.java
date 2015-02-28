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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.tobifleig.lxc.R;

public class HelpActivity extends KeepServiceRunningActivity {

    private final int[] faqEntries = new int[]{
            R.string.help_send, R.string.help_send_text,
            R.string.help_receive, R.string.help_receive_text,
            R.string.help_nofiles, R.string.help_nofiles_text,
            R.string.help_nofiles2, R.string.help_nofiles2_text,
            R.string.help_download_location, R.string.help_download_location_text,
            R.string.help_multiple, R.string.help_multiple_text,
            R.string.help_filetypes, R.string.help_filetypes_text,
            R.string.help_android_selector_dumb, R.string.help_android_selector_dumb_text,
            R.string.help_android_selector_nofolder, R.string.help_android_selector_nofolder_text,
            R.string.help_download_select_target, R.string.help_download_select_target_text,
            R.string.help_share_crossplatform, R.string.help_share_crossplatform_text,
            R.string.help_pcversion, R.string.help_pcversion_text,
            R.string.help_bugfeature, R.string.help_bugfeature_text
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // load layout
        setContentView(R.layout.activity_help);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        LayoutInflater inflater = getLayoutInflater();
        // load content
        LinearLayout root = (LinearLayout) findViewById(R.id.help_root);
        for (int i = 0; i < faqEntries.length; i += 2) {
            LinearLayout element = (LinearLayout) inflater.inflate(R.layout.help_element, null);
            TextView question = (TextView) element.findViewById(R.id.help_question);
            final TextView answer = (TextView) element.findViewById(R.id.help_answer);
            question.setText(faqEntries[i]);
            answer.setText(faqEntries[i + 1]);
            answer.setVisibility(View.GONE);
            root.addView(element);
            question.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    // toggle answer visibility
                    if (answer.getVisibility() == View.GONE) {
                        answer.setVisibility(View.VISIBLE);
                    } else {
                        answer.setVisibility(View.GONE);
                    }
                }
            });
            answer.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    // can only be clicked when visible
                    answer.setVisibility(View.GONE);
                }
            });
        }
    }

}
