package com.multazamgsd.takin.ui.notification;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import com.multazamgsd.takin.R;
import com.multazamgsd.takin.util.DatabaseHelper;
import com.multazamgsd.takin.util.GlobalConfig;
import com.pixplicity.easyprefs.library.Prefs;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NotifDetailActivity extends AppCompatActivity {
    public static String extra = "extra";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notif_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setTitle("Notification Detail");
        ((TextView) findViewById(R.id.textViewGreeting)).setText(String.format("Dear, %s", Prefs.getString(GlobalConfig.FIRST_NAME_PREFS, null)));

        String eventId = getIntent().getStringExtra(extra);
        new DatabaseHelper().getEventDetail(eventId, event -> {
            try {
                SimpleDateFormat sdf =
                        new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH); // Original date format from database
                Date date = sdf.parse(event.getDate());
                sdf.applyPattern("EEEE, MMMM d"); // Date format for: Saturday, January 12
                String finalDateFormat = sdf.format(date);
                ((TextView) findViewById(R.id.textViewEventDetailDate)).setText(finalDateFormat);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            ((TextView) findViewById(R.id.textViewEventDetailTime)).setText(String.format("%s - %s WIB", event.getTime_start(), event.getTime_end()));

            ((TextView) findViewById(R.id.textViewEventDetailPlace)).setText(event.getLocation_name());
            ((TextView) findViewById(R.id.textViewEventDetailAddress)).setText(event.getLocation_address());
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) { finish(); }
        return super.onOptionsItemSelected(item);
    }
}
