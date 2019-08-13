package com.multazamgsd.takin.ui.all_event;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ShareCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.multazamgsd.takin.R;
import com.multazamgsd.takin.model.Event;
import com.multazamgsd.takin.ui.event_detail.EventDetailActivity;
import com.multazamgsd.takin.util.AuthHelper;
import com.multazamgsd.takin.util.DatabaseHelper;

import java.util.ArrayList;

public class AllEventActivity extends AppCompatActivity {
    public static final String EXTRA_EVENT = "extra_event";
    private String INTENT_PURPOSE;
    private String uid, listTitle;

    private Toolbar toolbar;
    private ArrayList<Event> mList;

    private LinearLayout llMain, llLoading;
    private EditText etSearch;
    private TextView tvPurpose;
    private RecyclerView mRecyclerView;

    private DatabaseHelper mDatabaseHelper;
    private AuthHelper mAuthHelper;
    private AllEventAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_event);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);
        toolbar.setPadding(12,0,24,0);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        llMain = findViewById(R.id.linearLayoutMain);
        llLoading = findViewById(R.id.linearLayoutLoading);
        etSearch = findViewById(R.id.editTextSearch);
        tvPurpose = findViewById(R.id.textViewPurpose);
        mRecyclerView = findViewById(R.id.recyclerViewAllEvent);
        mList = new ArrayList<>();
        setLoading(true);

        mDatabaseHelper = new DatabaseHelper();
        mAuthHelper = new AuthHelper(this);
        uid = mAuthHelper.getCurrentUser().getUid();

        // Setup recyclerview
        mAdapter = new AllEventAdapter(this, eventAdapterListener);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setNestedScrollingEnabled(false);
        mRecyclerView.setAdapter(mAdapter);

        INTENT_PURPOSE = getIntent().getStringExtra(EXTRA_EVENT);
        switch (INTENT_PURPOSE) {
            case "seminar":
                listTitle = "Seminar";
                getEventByType("seminar");
                break;
            case "committee":
                listTitle = "Committee";
                getEventByType("committee");
                break;
            case "contest":
                listTitle = "Contest";
                getEventByType("contest");
                break;
            case "liked":
                listTitle = "Liked";
                getMyLikedEvent();
                break;
            case "booked":
                listTitle = "Registered";
                getBookedEvent();
                break;
            case "search":
                listTitle = "Search Result";
                break;
        }
    }

    private void getBookedEvent() {
        mDatabaseHelper.getRegisteredEvent(uid, result -> {
            if (result != null) {
                mList.clear();
                mList.addAll(result);

                mAdapter.setListEvents(mList);
                mAdapter.notifyDataSetChanged();
            }
            setLoading(false);
        });
    }

    private void getMyLikedEvent() {
        mDatabaseHelper.getLikedEvent(uid, result -> {
            if (result != null) {
                mList.clear();
                mList.addAll(result);

                mAdapter.setListEvents(mList);
                mAdapter.notifyDataSetChanged();
            }
            setLoading(false);
        });
    }

    private void getEventByType(String type) {
        mDatabaseHelper.getEventListByType(type, result -> {
            mList.clear();
            mList.addAll(result);

            mAdapter.setListEvents(mList);
            mAdapter.notifyDataSetChanged();

            setLoading(false);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_event_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) { finish(); }
        if (id == R.id.action_add) { finish(); }
        return super.onOptionsItemSelected(item);
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            llLoading.setVisibility(View.VISIBLE);
            llMain.setVisibility(View.GONE);
        } else {
            llLoading.setVisibility(View.GONE);
            llMain.setVisibility(View.VISIBLE);
            tvPurpose.setText(String.format("%s (%s)", listTitle, String.valueOf(mList.size())));
        }
    }

    private AllEventAdapter.eventAdapterListener eventAdapterListener = new AllEventAdapter.eventAdapterListener() {

        @Override
        public void onEventShare(int itemPosition) {
            shareItem(mList.get(itemPosition));
        }

        @Override
        public void onEventClick(int itemPosition) {
            detailIntent(mList.get(itemPosition));
            Log.d("Intent", String.valueOf(itemPosition));
        }

        @Override
        public void onEventLike(int itemPosition) {
            if (mList.get(itemPosition).isLiked()) {
                // Do unlike event
                mDatabaseHelper.doUnlikeEvent(mList.get(itemPosition).getId(), uid, task -> {
                    Toast.makeText(AllEventActivity.this, "Deleted from liked event", Toast.LENGTH_LONG).show();

                    Event thisEvent = mList.get(itemPosition);
                    thisEvent.setLiked(false);
                    mList.set(itemPosition, thisEvent);
                    mAdapter.notifyItemChanged(itemPosition);
                });
            } else {
                // Do like event
                mDatabaseHelper.doLikeEvent(mList.get(itemPosition).getId(), uid, task -> {
                    Toast.makeText(AllEventActivity.this, "Added to liked event", Toast.LENGTH_LONG).show();

                    Event thisEvent = mList.get(itemPosition);
                    thisEvent.setLiked(true);
                    mList.set(itemPosition, thisEvent);
                    mAdapter.notifyItemChanged(itemPosition);
                });
            }
        }
    };

    private void detailIntent(Event dataToSend) {
        Intent i = new Intent(this, EventDetailActivity.class);
        i.putExtra(EventDetailActivity.EXTRA_EVENT, dataToSend);
        Log.d("Intent", new Gson().toJson(dataToSend));
        startActivity(i);
    }

    private void shareItem(Event event) {
        int ticketAvailable = Integer.parseInt(event.getTicket_total()) - Integer.parseInt(event.getTicket_sold());
        ShareCompat.IntentBuilder
                .from(this)
                .setType("text/plain")
                .setChooserTitle("Share event")
                .setText(
                        String.format("Ayo daftar event %s di aplikasi Takin, hanya %s tiket tersedia!",
                                event.getTitle(),
                                String.valueOf(ticketAvailable)))
                .startChooser();
    }
}
