package com.multazamgsd.takin.ui.notification;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.google.gson.Gson;
import com.multazamgsd.takin.R;
import com.multazamgsd.takin.model.Notification;
import com.multazamgsd.takin.ui.all_event.AllEventAdapter;
import com.multazamgsd.takin.util.AuthHelper;
import com.multazamgsd.takin.util.DatabaseHelper;
import com.multazamgsd.takin.util.GlobalConfig;
import com.pixplicity.easyprefs.library.Prefs;

import java.util.ArrayList;

public class NotificationActivity extends AppCompatActivity {
    private LinearLayout llLoading, llContent;
    private DatabaseHelper mDatabaseHelper;
    private RecyclerView mRecyclerView;
    private NotificationAdapter mAdapter;
    private AuthHelper mAuthHelper;
    private ArrayList<Notification> mList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setTitle("Notification");

        llLoading = findViewById(R.id.linearLayoutLoading);
        llContent = findViewById(R.id.linearLayoutContent);
        mRecyclerView = findViewById(R.id.recyclerViewNotification);

        mDatabaseHelper = new DatabaseHelper();
        mAuthHelper = new AuthHelper(this);

        mAdapter = new NotificationAdapter(this, itemPosition -> {
            Intent i = new Intent(NotificationActivity.this, NotifDetailActivity.class);
            i.putExtra(NotifDetailActivity.extra, mList.get(itemPosition).getEvent_id());
            startActivity(i);
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setNestedScrollingEnabled(false);
        mRecyclerView.setAdapter(mAdapter);

        loadNotification();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) { finish(); }
        return super.onOptionsItemSelected(item);
    }

    private void loadNotification() {
        String username = Prefs.getString(GlobalConfig.FIRST_NAME_PREFS, null);
        mDatabaseHelper.getNotification(mAuthHelper.getCurrentUser().getUid(), username, result -> {
            if (result == null) {
                llLoading.setVisibility(View.GONE);
                llContent.setVisibility(View.VISIBLE);
            } else {
                llLoading.setVisibility(View.GONE);
                llContent.setVisibility(View.VISIBLE);

                mList.addAll(result);
                mAdapter.setListComment(mList);
                mAdapter.notifyDataSetChanged();
            }
        });
    }

}
