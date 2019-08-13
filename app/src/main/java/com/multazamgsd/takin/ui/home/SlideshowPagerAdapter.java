package com.multazamgsd.takin.ui.home;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.viewpager.widget.PagerAdapter;

import com.bumptech.glide.request.RequestOptions;
import com.multazamgsd.takin.R;
import com.multazamgsd.takin.model.Event;
import com.multazamgsd.takin.util.GlideApp;

import java.util.ArrayList;
import java.util.List;

public class SlideshowPagerAdapter extends PagerAdapter implements CardAdapter {
    private ArrayList<Event> mList;
    private List<CardView> mViews;

    private SlideshowInterface mInterface;
    private Context context;

    private float mBaseElevation;

    public SlideshowPagerAdapter(Context context, SlideshowInterface itf) {
        this.context = context;
        this.mInterface = itf;

        mList = new ArrayList<>();
        mViews = new ArrayList<>();
    }

    public void setSlideshowList(ArrayList<Event> listEvents) {
        if (listEvents == null) return;
        this.mList.clear();
        this.mList.addAll(listEvents);

        for (int i = 0; i < listEvents.size(); i++) {
            mViews.add(null);
        }
    }

    @Override
    public int getCount() {
        return mList != null ? mList.size() : 0;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        View imageLayout = LayoutInflater.from(context).inflate(R.layout.slideshow_item, container, false);
        ImageView imageView = imageLayout.findViewById(R.id.imageView);
        GlideApp.with(context)
                .load(mList.get(position).getPhoto_url())
                .apply(RequestOptions.placeholderOf(R.drawable.ic_image_grey_24dp)
                        .error(R.drawable.ic_image_grey_24dp))
                .into(imageView);
        container.addView(imageLayout);
        CardView cardView = imageLayout.findViewById(R.id.cardView);

        if (mBaseElevation == 0) {
            mBaseElevation = cardView.getCardElevation();
        }
        cardView.setMaxCardElevation(mBaseElevation * MAX_ELEVATION_FACTOR);
        mViews.set(position, cardView);
        imageLayout.setOnClickListener(v -> mInterface.onClick(position));
        return imageLayout;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
        return view.equals(o);
    }

    public interface SlideshowInterface {
        void onClick(int itemPosition);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
        mViews.set(position, null);
    }

    // CardView implementation
    @Override
    public float getBaseElevation() {
        return mBaseElevation;
    }

    @Override
    public CardView getCardViewAt(int position) {
        return mViews.get(position);
    }
}
