package de.tobifleig.lxc.plaf.impl.android;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import de.tobifleig.lxc.R;

public class PCVersionActivity extends KeepServiceRunningActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // load layout
        setContentView(R.layout.activity_pcversion);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        // handle input
        findViewById(R.id.pcversion_centerbox).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.pcversion_url))));
            }
        });
    }

}
