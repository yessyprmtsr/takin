package com.multazamgsd.takin.ui.home;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ShareCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.sundeepk.compactcalendarview.CompactCalendarView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.gson.Gson;
import com.multazamgsd.takin.R;
import com.multazamgsd.takin.model.Event;
import com.multazamgsd.takin.ui.all_event.AllEventActivity;
import com.multazamgsd.takin.ui.event_detail.EventDetailActivity;
import com.multazamgsd.takin.ui.main.MainActivity;
import com.multazamgsd.takin.util.AuthHelper;
import com.multazamgsd.takin.util.DatabaseHelper;
import com.multazamgsd.takin.util.DividerItemDecorator;
import com.multazamgsd.takin.util.ShadowTransformer;
import com.multazamgsd.takin.vo.AppValueObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private static final String TAG = HomeFragment.class.getSimpleName();
    private DatabaseHelper mDatabaseHelper;
    private AuthHelper mAuthHelper;

    // IMPORTANT
    public static String SECTION;
    private String uid;

    private ArrayList<Event> slideShowList = new ArrayList<>();
    private ArrayList<Event> recommendedList = new ArrayList<>();
    private ArrayList<Event> newList = new ArrayList<>();

    private RecommendedAdapter recommendedAdapter;
    private NewAdapter newAdapter;

    private RecyclerView rvEventRecommended;
    private RecyclerView rvEventNew;
    private CompactCalendarView compactCalendarView;
    private ImageView btRefreshCalendar;
    private TextView tvMonth, tvSeeAll1, tvSeeAll2;

    // Calendar container
    private RelativeLayout rlCal;
    private CardView cvCal;

    // Slide show
    private ViewPager mViewPager;
    private ShadowTransformer mShadowTransformer;
    private SlideshowPagerAdapter mPagerAdapter;

    // Bottom sheet
    private View llBottomSheet;
    private BottomSheetBehavior bottomSheetBehavior;
    private BottomSheetDialog mBottomSheetDialog;

    public HomeFragment() {}

    public static Fragment newInstance(String section) {
        SECTION = section;
        return new HomeFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvEventRecommended = view.findViewById(R.id.recyclerViewEventRecommended);
        rvEventNew = view.findViewById(R.id.recyclerViewEventNew);
        mViewPager = view.findViewById(R.id.viewPagerSlideshow);

        // Calendar container
        rlCal = view.findViewById(R.id.relativeLayoutCal);
        cvCal = view.findViewById(R.id.cardViewCal);

        tvSeeAll1 = view.findViewById(R.id.textViewSeeAll1);
        tvSeeAll2 = view.findViewById(R.id.textViewSeeAll2);

        compactCalendarView = view.findViewById(R.id.calendarView);
        tvMonth = view.findViewById(R.id.textViewMonth);
        btRefreshCalendar = view.findViewById(R.id.imageViewRefreshCalendar);

        // Bottom sheet
        llBottomSheet = view.findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(llBottomSheet);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getActivity() != null) {
            mDatabaseHelper = new DatabaseHelper();
            mAuthHelper = new AuthHelper(getActivity());

            uid = mAuthHelper.getCurrentUser().getUid();
            if (SECTION.equals(AppValueObject.HOME.getValue())) {
                showHideCalendarContainer(false);
            } else {
                showHideCalendarContainer(true);
            }

            setSlideshow();
            setCalendar();
            setRecommendedList();
            setNewList();
            loadData();
        }
    }

    private void setCalendar() {
        compactCalendarView.setFirstDayOfWeek(Calendar.SUNDAY);
        compactCalendarView.setUseThreeLetterAbbreviation(true);

        // Set date to today
        SimpleDateFormat dateFormatForMonth;
        dateFormatForMonth = new SimpleDateFormat("MMMM - yyyy", Locale.getDefault());
        compactCalendarView.setCurrentDate(Calendar.getInstance(Locale.getDefault()).getTime());
        tvMonth.setText(dateFormatForMonth.format(compactCalendarView.getFirstDayOfCurrentMonth()));

        // define a listener to receive callbacks when certain events happen.
        compactCalendarView.setListener(new CompactCalendarView.CompactCalendarViewListener() {
            @Override
            public void onDayClick(Date dateClicked) {
                List<com.github.sundeepk.compactcalendarview.domain.Event> events = compactCalendarView.getEvents(dateClicked);
                showBottomSheet(dateClicked, events);
            }

            @Override
            public void onMonthScroll(Date firstDayOfNewMonth) {
                tvMonth.setText(dateFormatForMonth.format(firstDayOfNewMonth));
            }
        });

        btRefreshCalendar.setOnClickListener(v -> getUserRegisteredEvent());
        getUserRegisteredEvent();
    }

    private void getUserRegisteredEvent() {
        compactCalendarView.removeAllEvents();
        mDatabaseHelper.getRegisteredEvent(mAuthHelper.getCurrentUser().getUid(), result -> {
            if (result == null) {
                return;
            }
            for (Event e : result) {
                // Converting date to milis
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    Date date = sdf.parse(e.getDate());
                    long milis = date.getTime();
                    com.github.sundeepk.compactcalendarview.domain.Event ev = new com.github.sundeepk.compactcalendarview.domain.Event(Color.BLACK, milis, e);
                    compactCalendarView.addEvent(ev);
                } catch (ParseException e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    private void showBottomSheet(Date date, List<com.github.sundeepk.compactcalendarview.domain.Event> calendarEvents) {
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }

        // Defining sheet dialog component
        final View view = getLayoutInflater().inflate(R.layout.bottom_sheet_calendar, null);
        //Parsing date to readable format
        try {
            SimpleDateFormat sdf =
                    new SimpleDateFormat("E MMM dd hh:mm:ss zzzz yyyy", java.util.Locale.ENGLISH);
            Date dates = sdf.parse(String.valueOf(date));
            sdf.applyPattern("dd"); // Date format for: Tue, 24 Mar
            ((TextView) view.findViewById(R.id.textViewDate)).setText(sdf.format(dates));
            sdf.applyPattern("MMM");
            ((TextView) view.findViewById(R.id.textViewMonth)).setText(sdf.format(dates));
            sdf.applyPattern("yyyy");
            ((TextView) view.findViewById(R.id.textViewYear)).setText(sdf.format(dates));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        RecyclerView rvCalendarEvent = view.findViewById(R.id.recyclerViewCalendarEvent);
        TextView tvNoEvent = view.findViewById(R.id.textViewNoEvent);
        if (calendarEvents.size() == 0) {
            // Showing no event text
            tvNoEvent.setVisibility(View.VISIBLE);
            rvCalendarEvent.setVisibility(View.GONE);
        } else {
            tvNoEvent.setVisibility(View.GONE);
            rvCalendarEvent.setVisibility(View.VISIBLE);

            // Setup list
            ArrayList<Event> calendarEventList = new ArrayList<>();
            CalendarEventAdapter ceAdapter = new CalendarEventAdapter(getActivity(), itemPosition -> {
                detailIntent(calendarEventList.get(itemPosition));
            });
            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
            rvCalendarEvent.setLayoutManager(layoutManager);
            rvCalendarEvent.setHasFixedSize(true);
            rvCalendarEvent.setNestedScrollingEnabled(false);
            rvCalendarEvent.setAdapter(ceAdapter);

            // Getting event data
            String data = new Gson().toJson(calendarEvents);
            JSONArray calendarEventArray = null;
            try {
                calendarEventArray = new JSONArray(data);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            for (int i = 0; i < calendarEventArray.length(); i++) {
                JSONObject row = null;
                JSONObject eventObject = null;
                try {
                    row = calendarEventArray.getJSONObject(i);
                    eventObject = (JSONObject) row.get("data");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                try {
                    Event oneEvent = new Event();
                    oneEvent.setDate(eventObject.getString("date"));
                    oneEvent.setDescription(eventObject.getString("description"));
                    oneEvent.setLiked(eventObject.getBoolean("isLiked"));
                    oneEvent.setLocation_address(eventObject.getString("location_address"));
                    oneEvent.setLocation_lat(eventObject.getString("location_lat"));
                    oneEvent.setLocation_long(eventObject.getString("location_long"));
                    oneEvent.setLocation_name(eventObject.getString("location_name"));
                    oneEvent.setPhoto_url(eventObject.getString("photo_url"));
                    oneEvent.setPoint(eventObject.getString("point"));
                    oneEvent.setPrice(eventObject.getString("price"));
                    oneEvent.setPublisher(eventObject.getString("publisher"));
                    oneEvent.setTicket_sold(eventObject.getString("ticket_sold"));
                    oneEvent.setTicket_total(eventObject.getString("ticket_total"));
                    oneEvent.setTime_start(eventObject.getString("time_start"));
                    oneEvent.setTitle(eventObject.getString("title"));
                    oneEvent.setType(eventObject.getString("type"));

                    calendarEventList.add(oneEvent);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            ceAdapter.setListEvents(calendarEventList);
        }

        mBottomSheetDialog = new BottomSheetDialog(getActivity());
        mBottomSheetDialog.setContentView(view);
        ((View) view.getParent()).setBackgroundColor(getResources().getColor(android.R.color.transparent));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBottomSheetDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        mBottomSheetDialog.show();
        mBottomSheetDialog.setOnDismissListener(dialog -> mBottomSheetDialog = null);
    }

    private void setSlideshow() {
        mPagerAdapter = new SlideshowPagerAdapter(getActivity(), itemPosition -> detailIntent(slideShowList.get(itemPosition)));
        mShadowTransformer = new ShadowTransformer(mViewPager, mPagerAdapter);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setPageTransformer(false, mShadowTransformer);
    }

    private void setNewList() {
        tvSeeAll1.setOnClickListener(v -> {
            Intent i = new Intent(getActivity(), AllEventActivity.class);
            i.putExtra(AllEventActivity.EXTRA_EVENT, SECTION);
            startActivity(i);
        });

        newAdapter = new NewAdapter(getActivity(), new NewAdapter.eventAdapterListener() {
            @Override
            public void onEventClick(int itemPosition) {
                detailIntent(newList.get(itemPosition));
            }

            @Override
            public void onEventLike(int itemPosition) {
                doLikeNewItem(itemPosition);
            }

            @Override
            public void onEventShare(int itemPosition) {
                shareItem(newList.get(itemPosition));
            }
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
        RecyclerView.ItemDecoration dividerItemDecoration = new DividerItemDecorator(ContextCompat.getDrawable(getActivity(), R.drawable.divider));
        rvEventNew.setLayoutManager(layoutManager);
        rvEventNew.addItemDecoration(dividerItemDecoration);
        rvEventNew.setHasFixedSize(true);
        rvEventNew.setNestedScrollingEnabled(false);
        rvEventNew.setAdapter(newAdapter);
    }

    private void setRecommendedList() {
        tvSeeAll2.setOnClickListener(v -> {
            Intent i = new Intent(getActivity(), AllEventActivity.class);
            i.putExtra(AllEventActivity.EXTRA_EVENT, SECTION);
            startActivity(i);
        });

        recommendedAdapter = new RecommendedAdapter(getActivity(), new RecommendedAdapter.eventAdapterListener() {
            @Override
            public void onEventClick(int itemPosition) {
                detailIntent(recommendedList.get(itemPosition));
            }

            @Override
            public void onEventLike(int itemPosition) {
                doLikeRecommendedItem(itemPosition);
            }

            @Override
            public void onEventShare(int itemPosition) {
                shareItem(recommendedList.get(itemPosition));
            }
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
        RecyclerView.ItemDecoration dividerItemDecoration = new DividerItemDecorator(ContextCompat.getDrawable(getActivity(), R.drawable.divider));
        rvEventRecommended.setLayoutManager(layoutManager);
        rvEventRecommended.addItemDecoration(dividerItemDecoration);
        rvEventRecommended.setHasFixedSize(true);
        rvEventRecommended.setNestedScrollingEnabled(false);
        rvEventRecommended.setAdapter(recommendedAdapter);
    }

    private void loadData() {
        Query query;
        if (SECTION.equals(AppValueObject.HOME.getValue())) {
            query = FirebaseFirestore.getInstance().collection("event");
        } else {
            query = FirebaseFirestore.getInstance()
                        .collection("event")
                        .whereEqualTo("type", SECTION);
        }
        query.limit(7).get().addOnCompleteListener(task -> {
            ArrayList<Event> result = new ArrayList<>();
            if (task.isSuccessful()) {
                for(DocumentSnapshot doc : task.getResult()){
                    Event e = doc.toObject(Event.class);
                    e.setId(doc.getId());
                    result.add(e);
                }

                // Divide into 2 lists
                for (int i=0; i < result.size(); i++) {
                    if (i <= 2) {
                        slideShowList.add(result.get(i));
                    } else if (i >= 3 && i <= 4) {
                        recommendedList.add(result.get(i));
                    } else if (i >= 4) {
                        newList.add(result.get(i));
                    }
                }

                // Set to slideshow
                mPagerAdapter.setSlideshowList(slideShowList);
                mPagerAdapter.notifyDataSetChanged();
                mShadowTransformer.enableScaling(true);

                // Set to recommended rv
                recommendedAdapter.setListEvents(recommendedList);
                recommendedAdapter.notifyDataSetChanged();

                // Set to new rv
                newAdapter.setListEvents(newList);
                newAdapter.notifyDataSetChanged();
            } else {
                Log.d(TAG, "Error getting documents: ", task.getException());
            }
        });
    }

    private void detailIntent(Event dataToSend) {
        Intent i = new Intent(getActivity(), EventDetailActivity.class);
        i.putExtra(EventDetailActivity.EXTRA_EVENT, dataToSend);
        startActivity(i);
    }

    private void shareItem(Event event) {
        int ticketAvailable = Integer.parseInt(event.getTicket_total()) - Integer.parseInt(event.getTicket_sold());
        ShareCompat.IntentBuilder
                .from(getActivity())
                .setType("text/plain")
                .setChooserTitle("Share event")
                .setText(
                        String.format("Ayo daftar event %s di aplikasi Takin, hanya %s tiket tersedia!",
                                event.getTitle(),
                                String.valueOf(ticketAvailable)))
                .startChooser();
    }

    private void doLikeRecommendedItem(int itemPosition) {
        String eventId = recommendedList.get(itemPosition).getId();
        if (recommendedList.get(itemPosition).isLiked()) {  // Do Unlike
            // Checking isEventAlreadyLiked?
            mDatabaseHelper.checkEventLiked(eventId, uid, result -> {
                boolean isLiked = result != null;
                if (!isLiked) {
                    // No need to unlike, because this event is not liked by this user
                    Event thisEvent = recommendedList.get(itemPosition);
                    thisEvent.setLiked(false);
                    recommendedList.set(itemPosition, thisEvent);
                    recommendedAdapter.notifyItemChanged(itemPosition);
                    return;
                }

                // Do unlike event
                mDatabaseHelper.doUnlikeEvent(eventId, uid, task -> {
                    Toast.makeText(getActivity(), "Deleted from liked event", Toast.LENGTH_LONG).show();

                    Event thisEvent = recommendedList.get(itemPosition);
                    thisEvent.setLiked(false);
                    recommendedList.set(itemPosition, thisEvent);
                    recommendedAdapter.notifyItemChanged(itemPosition);
                });
            });
        } else { // Do like
            // Checking isEventAlreadyLiked?
            mDatabaseHelper.checkEventLiked(eventId, uid, result -> {
                boolean isLiked = result != null;
                if (isLiked) {
                    // No need to like, because this event is already liked by this user
                    Event thisEvent = recommendedList.get(itemPosition);
                    thisEvent.setLiked(true);
                    recommendedList.set(itemPosition, thisEvent);
                    recommendedAdapter.notifyItemChanged(itemPosition);
                    return;
                }

                // Do like event
                mDatabaseHelper.doLikeEvent(eventId, uid, task -> {
                    Toast.makeText(getActivity(), "Added to liked event", Toast.LENGTH_LONG).show();

                    Event thisEvent = recommendedList.get(itemPosition);
                    thisEvent.setLiked(true);
                    recommendedList.set(itemPosition, thisEvent);
                    recommendedAdapter.notifyItemChanged(itemPosition);
                });
            });
        }
    }

    private void doLikeNewItem(int itemPosition) {
        String eventId = newList.get(itemPosition).getId();
        if (newList.get(itemPosition).isLiked()) {  // Do Unlike
            // Checking isEventAlreadyLiked?
            mDatabaseHelper.checkEventLiked(eventId, uid, result -> {
                boolean isLiked = result != null;
                if (!isLiked) {
                    // No need to unlike, because this event is not liked by this user
                    Event thisEvent = newList.get(itemPosition);
                    thisEvent.setLiked(false);
                    newList.set(itemPosition, thisEvent);
                    newAdapter.notifyItemChanged(itemPosition);
                    return;
                }

                // Do unlike event
                mDatabaseHelper.doUnlikeEvent(eventId, uid, task -> {
                    Toast.makeText(getActivity(), "Deleted from liked event", Toast.LENGTH_LONG).show();

                    Event thisEvent = newList.get(itemPosition);
                    thisEvent.setLiked(false);
                    newList.set(itemPosition, thisEvent);
                    newAdapter.notifyItemChanged(itemPosition);
                });
            });
        } else { // Do like
            // Checking isEventAlreadyLiked?
            mDatabaseHelper.checkEventLiked(eventId, uid, result -> {
                boolean isLiked = result != null;
                if (isLiked) {
                    // No need to like, because this event is already liked by this user
                    Event thisEvent = newList.get(itemPosition);
                    thisEvent.setLiked(true);
                    newList.set(itemPosition, thisEvent);
                    newAdapter.notifyItemChanged(itemPosition);
                    return;
                }

                // Do like event
                mDatabaseHelper.doLikeEvent(eventId, uid, task -> {
                    Toast.makeText(getActivity(), "Added to liked event", Toast.LENGTH_LONG).show();

                    Event thisEvent = newList.get(itemPosition);
                    thisEvent.setLiked(true);
                    newList.set(itemPosition, thisEvent);
                    newAdapter.notifyItemChanged(itemPosition);
                });
            });
        }
    }

    private void showHideCalendarContainer(boolean show) {
        if (show) {
            rlCal.setVisibility(View.VISIBLE);
            cvCal.setVisibility(View.VISIBLE);
        } else {
            rlCal.setVisibility(View.GONE);
            cvCal.setVisibility(View.GONE);
        }
    }
}