package com.multazamgsd.takin.ui.event_detail;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ShareCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.multazamgsd.takin.R;
import com.multazamgsd.takin.model.Event;
import com.multazamgsd.takin.util.AuthHelper;
import com.multazamgsd.takin.util.DatabaseHelper;
import com.multazamgsd.takin.util.DividerItemDecorator;
import com.multazamgsd.takin.util.GlideApp;
import com.multazamgsd.takin.util.HideKeyboard;
import com.multazamgsd.takin.util.StringHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;

public class EventDetailActivity extends AppCompatActivity {
    public static final String EXTRA_EVENT = "extra_event";
    private static final String TAG = EventDetailActivity.class.getSimpleName();
    public static Activity activity;

    private String uid;
    private Menu menu;
    private boolean isEventLiked = false;

    private Boolean descriptionExpand = false;
    private Integer ticketAvailable;
    private String uppercaseEventType;
    private MoreEventAdapter mAdapter;
    private ArrayList<Event> eventList = new ArrayList<>();

    private ImageView ivEvent;
    private TextView tvTitle, tvPublisher, tvDescription; // Main Card
    private TextView tvDate, tvTime, tvPlace, tvAddress, tvTicketAvailability, tvPoint, tvPrice, tvSeeMore; // Schedule info card
    private TextView tvEventType;
    private RecyclerView rvMoreEvent;
    private Toolbar toolbar;
    private Event event;

    // Get current user
    private AuthHelper mAuthHelper = new AuthHelper(EventDetailActivity.this);
    private DatabaseHelper mDatabaseHelper = new DatabaseHelper();

    //Comment section
    private CircleImageView ivProfileComment;
    private EditText etNewComment;
    private TextView tvCommentCount, tvNoComment;
    private RecyclerView rvComment;
    private CommentAdapter mCommentAdapter;

    // Floating button
    Button btGetTicket, btSendComment;

    private StringHelper stringHelper;
    private GoogleMap locationMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);
        activity = this;

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);
        toolbar.setPadding(12,0,24,0);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Load string helper for trimming
        stringHelper = new StringHelper();

        // Set main info data
        setUpMainData();

        //  Check is event liked
        isEventLiked();

        // Setting up page title
        uppercaseEventType = event.getType().substring(0, 1).toUpperCase() + event.getType().substring(1);
        setTitle(uppercaseEventType);

        // Set schedule card info data
        setUpDetailData();

        // Defining google maps
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.locationMap);
        mapFragment.getMapAsync(googleMap -> {
            locationMap = googleMap;

            double lat = Double.parseDouble(event.getLocation_lat());
            double lng = Double.parseDouble(event.getLocation_long());

            LatLng locationPin = new LatLng(lat, lng);
            locationMap.addMarker(new MarkerOptions()
                    .position(locationPin)
                    .title(event.getLocation_name()));
            locationMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locationPin, 15.0f));
        });

        // Defining comment section
        setUpCommentSection();

        // Defining more event section list
        setUpMoreEvent();

        btGetTicket = findViewById(R.id.buttonGetTicket);
        btGetTicket.setOnClickListener(v -> {
            Intent i = new Intent(EventDetailActivity.this, RegistrationActivity.class);
            i.putExtra(RegistrationActivity.EXTRA_EVENT, event);
            startActivity(i);
        });
    }

    private void setUpMainData() {
        ivEvent = findViewById(R.id.imageViewDetailEvent);
        tvTitle = findViewById(R.id.textViewDetailEventTitle);
        tvPublisher = findViewById(R.id.textViewDetailEventPublisher);
        tvDescription = findViewById(R.id.textViewDetailEventDesc);
        tvSeeMore = findViewById(R.id.textViewSeeMore);
        tvSeeMore.setOnClickListener(v -> showLessDescription());

        event = getIntent().getParcelableExtra(EXTRA_EVENT);
        uid = mAuthHelper.getCurrentUser().getUid();
        GlideApp.with(this)
                .load(event.getPhoto_url())
                .apply(RequestOptions
                        .placeholderOf(R.drawable.ic_image_grey_24dp)
                        .error(R.drawable.ic_image_grey_24dp))
                .into(ivEvent);
        tvTitle.setText(event.getTitle());
        tvPublisher.setText(String.format("Published by %s", event.getPublisher()));
        tvTitle.setText(event.getTitle());
        tvDescription.setText(stringHelper.cutString(event.getDescription(), 183));
    }

    private void setUpDetailData() {
        tvDate = findViewById(R.id.textViewEventDetailDate);
        tvTime = findViewById(R.id.textViewEventDetailTime);
        tvPlace = findViewById(R.id.textViewEventDetailPlace);
        tvAddress = findViewById(R.id.textViewEventDetailAddress);
        tvTicketAvailability = findViewById(R.id.textViewEventDetailTicketAvailability);
        tvPoint = findViewById(R.id.textViewEventDetailTAKTotal);
        tvPrice = findViewById(R.id.textViewEventDetailPrice);

        // Parsing date
        try {
            SimpleDateFormat sdf =
                    new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH); // Original date format from database
            Date date = sdf.parse(event.getDate());
            sdf.applyPattern("EEEE, MMMM d"); // Date format for: Saturday, January 12
            String finalDateFormat = sdf.format(date);
            tvDate.setText(finalDateFormat); // Set to textView
        } catch (ParseException e) {
            e.printStackTrace();
        }
        tvTime.setText(String.format("%s - %s WIB", event.getTime_start(), event.getTime_end()));
        tvPlace.setText(event.getLocation_name());
        tvAddress.setText(stringHelper.cutString(event.getLocation_address(), 63));
        ticketAvailable = Integer.parseInt(event.getTicket_total()) - Integer.parseInt(event.getTicket_sold());
        tvTicketAvailability.setText(String.format("%s / %s Tickets", ticketAvailable ,event.getTicket_total()));
        tvPoint.setText(String.format("%s TAK", event.getPoint()));
        tvPrice.setText(stringHelper.priceOrFree(event.getPrice()));
    }

    private void setUpMoreEvent() {
        tvEventType = findViewById(R.id.textViewMoreEvent);
        tvEventType.setText(String.format("More %s", uppercaseEventType));
        rvMoreEvent = findViewById(R.id.recyclerViewMoreEvent);
        mAdapter = new MoreEventAdapter(this, new MoreEventAdapter.eventAdapterListener() {
            @Override
            public void onEventClick(int itemPosition) {
                Log.d(TAG, String.valueOf(itemPosition));
                Intent i = new Intent(EventDetailActivity.this, EventDetailActivity.class);
                i.putExtra(EXTRA_EVENT, eventList.get(itemPosition));
                startActivity(i);
            }

            @Override
            public void onEventLike(int itemPosition) {

            }

            @Override
            public void onEventShare(int itemPosition) {

            }
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false);
        rvMoreEvent.setLayoutManager(layoutManager);
        rvMoreEvent.setAdapter(mAdapter);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference eventRef = db.collection("event");
        eventRef.whereEqualTo("type", event.getType()).limit(6).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for(DocumentSnapshot doc : task.getResult()){
                    Event e = doc.toObject(Event.class);
                    e.setId(doc.getId());
                    if (!e.getId().equals(event.getId())) {
                        eventList.add(e);
                    }
                }
                mAdapter.setListEvents(eventList);
                mAdapter.notifyDataSetChanged();
            } else {
                Log.d(TAG, "Error getting documents: ", task.getException());
            }
        });
    }

    private void showLessDescription() {
        if (!descriptionExpand) {
            descriptionExpand = true;
            tvSeeMore.setText("Show less");
            tvDescription.setText(event.getDescription());
        } else {
            descriptionExpand = false;
            tvSeeMore.setText("See more");
            tvDescription.setText(stringHelper.cutString(event.getDescription(), 183));
        }
    }

    private void setUpCommentSection() {
        tvCommentCount = findViewById(R.id.textViewCommentCount);
        rvComment = findViewById(R.id.recyclerViewComment);
        tvNoComment = findViewById(R.id.textViewNoComment);
        etNewComment = findViewById(R.id.editTextNewComment);
        ivProfileComment = findViewById(R.id.imageViewProfileComment);
        btSendComment = findViewById(R.id.buttonSendComment);
        // When user writing comment, show send button
        etNewComment.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                btSendComment.setVisibility(View.VISIBLE);
                btGetTicket.setVisibility(View.GONE);
            } else {
                btSendComment.setVisibility(View.GONE);
                btGetTicket.setVisibility(View.VISIBLE);
            }
        });

        // Prepare recyclerview
        mCommentAdapter = new CommentAdapter(this, new CommentAdapter.CommentAdapterListener() {
            @Override
            public void onCommentLike(int itemPosition) {

            }

            @Override
            public void onCommentDislike(int itemPosition) {

            }

            @Override
            public void onCommentReply(int itemPosition) {

            }
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        RecyclerView.ItemDecoration dividerItemDecoration = new DividerItemDecorator(ContextCompat.getDrawable(this, R.drawable.divider));
        rvComment.setLayoutManager(layoutManager);
        rvComment.addItemDecoration(dividerItemDecoration);
        rvComment.setHasFixedSize(true);
        rvComment.setNestedScrollingEnabled(false);
        rvComment.setAdapter(mCommentAdapter);
        loadEventComment();

        // Send comment action
        btSendComment.setOnClickListener(v -> {
            String newComment = etNewComment.getText().toString();
            if ((newComment != null) && (!newComment.equals(""))) {
                btSendComment.setText("Sending comment...");
                btSendComment.setEnabled(false);

                mDatabaseHelper.sendEventComment(event.getId(), uid, newComment, task -> {
                    btSendComment.setText("Send comment");
                    btSendComment.setEnabled(true);
                    new HideKeyboard(EventDetailActivity.this).run();
                    btSendComment.setVisibility(View.GONE);
                    btGetTicket.setVisibility(View.VISIBLE);

                    if (task.isSuccessful()) {
                        etNewComment.setText("");
                        loadEventComment();
                    } else {
                        Toast.makeText(getApplicationContext(),"An error occurred", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void loadEventComment() {
        // Count comment of this event
        mDatabaseHelper.countEventComment(event.getId(), commentCount -> {
            tvCommentCount.setText(String.format("Comments (%s)", commentCount));
            if (Integer.parseInt(commentCount) > 0) {
                tvNoComment.setVisibility(View.GONE);
                rvComment.setVisibility(View.VISIBLE);

                mDatabaseHelper.loadEventComment(event.getId(), task -> {
                        mCommentAdapter.setListComment(task);
                        mCommentAdapter.notifyDataSetChanged();
                });
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        MenuInflater findMenuItems = getMenuInflater();
        findMenuItems.inflate(R.menu.detail_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void isEventLiked() {
        mDatabaseHelper.checkEventLiked(event.getId(), uid, commentId -> {
            isEventLiked = commentId != null;
            menu.getItem(1).setIcon(getResources().getDrawable(
                    isEventLiked ? R.drawable.ic_button_love_fill : R.drawable.ic_button_love_white
            ));
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.action_like:
                if (isEventLiked) {
                    mDatabaseHelper.doUnlikeEvent(event.getId(), uid, task -> {
                        Toast.makeText(this, "Deleted from liked event", Toast.LENGTH_LONG).show();
                        isEventLiked();
                    });
                } else {
                    mDatabaseHelper.doLikeEvent(event.getId(), uid, task -> {
                        Toast.makeText(this, "Added to liked event", Toast.LENGTH_LONG).show();
                        isEventLiked();
                    });
                }
                break;
            case R.id.action_share:
                ShareCompat.IntentBuilder
                        .from(EventDetailActivity.this)
                        .setType("text/plain")
                        .setChooserTitle("Share event")
                        .setText(String.format("Ayo daftar event %s di aplikasi Takin, hanya %s tiket tersedia!", event.getTitle(), String.valueOf(ticketAvailable)))
                        .startChooser();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        etNewComment.setSelected(false);
        btSendComment.setVisibility(View.GONE);
        btGetTicket.setVisibility(View.VISIBLE);
        super.onBackPressed();
    }
}
