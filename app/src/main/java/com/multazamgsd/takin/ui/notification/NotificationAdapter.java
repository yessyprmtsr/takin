package com.multazamgsd.takin.ui.notification;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.multazamgsd.takin.R;
import com.multazamgsd.takin.model.Notification;
import com.multazamgsd.takin.util.StringHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.EventViewHolder> {
    private final Activity activity;
    private ArrayList<Notification> notif = new ArrayList<>();
    private NotificationAdapterListener notificationAdapterListener;

    public NotificationAdapter(Activity activity, NotificationAdapterListener notificationAdapterListener) {
        this.activity = activity;
        this.notificationAdapterListener = notificationAdapterListener;
    }

    private ArrayList<Notification> getNotifList() {
        return notif;
    }

    public void setListComment(ArrayList<Notification> notifComment) {
        if (notifComment == null) return;
        this.notif.clear();
        this.notif.addAll(notifComment);
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(activity).inflate(R.layout.notification_row, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        StringHelper stringHelper = new StringHelper();
        Notification notifItem = getNotifList().get(position);

        holder.tvTitle.setText(stringHelper.cutString(notifItem.getTitle(), 23));
        holder.tvContent.setText(stringHelper.cutString(notifItem.getContent(), 34));

        // Set up human readable
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = null;
        try {
            date = sdf.parse(notifItem.getTime());
            sdf.applyPattern("E, MMM d"); // Date format for: Tue, 24 Mar
            String finalDateFormat = sdf.format(date);
            holder.tvTime.setText(finalDateFormat);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return getNotifList() == null ? 0 : getNotifList().size();
    }

    public interface NotificationAdapterListener {
        void onClick(int itemPosition);
    }

    class EventViewHolder extends RecyclerView.ViewHolder {
        CardView cv;
        TextView tvTitle, tvContent, tvTime;
        RelativeLayout rlNotif;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);

            cv = itemView.findViewById(R.id.cardViewNotif);
            rlNotif = itemView.findViewById(R.id.relativeLayoutNotif);
            tvTitle = itemView.findViewById(R.id.textViewNotifTitle);
            tvContent = itemView.findViewById(R.id.textViewNotifContent);
            tvTime = itemView.findViewById(R.id.textViewTime);

            rlNotif.setOnClickListener(view -> notificationAdapterListener.onClick(getAdapterPosition()));
            cv.setOnClickListener(view -> notificationAdapterListener.onClick(getAdapterPosition()));
            itemView.setOnClickListener(v -> notificationAdapterListener.onClick(getAdapterPosition()));
        }
    }
}
