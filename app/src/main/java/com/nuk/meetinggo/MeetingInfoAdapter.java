package com.nuk.meetinggo;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

import static com.nuk.meetinggo.DataUtils.MEETING_CHAIRMAN;
import static com.nuk.meetinggo.DataUtils.MEETING_DATE;
import static com.nuk.meetinggo.DataUtils.MEETING_FAVOURED;
import static com.nuk.meetinggo.DataUtils.MEETING_TIME;
import static com.nuk.meetinggo.DataUtils.MEETING_TITLE;
import static com.nuk.meetinggo.MainFragment.checkedArray;
import static com.nuk.meetinggo.MainFragment.deleteActive;
import static com.nuk.meetinggo.MainFragment.searchActive;
import static com.nuk.meetinggo.MainFragment.setFavourite;

/**
 * Adapter class for custom meetings ListView
 */
public class MeetingInfoAdapter extends BaseAdapter implements ListAdapter {
    private Context context;
    private JSONArray adapterData;
    private LayoutInflater inflater;
    
    /**
     * Adapter constructor -> Sets class variables
     * @param context application context
     * @param adapterData JSONArray of meetings
     */
    public MeetingInfoAdapter(Context context, JSONArray adapterData) {
        this.context = context;
        this.adapterData = adapterData;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    // Return number of meetings
    @Override
    public int getCount() {
        if (this.adapterData != null)
            return this.adapterData.length();

        else
            return 0;
    }

    // Return meeting at position
    @Override
    public JSONObject getItem(int position) {
        if (this.adapterData != null)
            return this.adapterData.optJSONObject(position);

        else
            return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }


    // View inflater
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        // Inflate custom meeting view if null
        if (convertView == null)
            convertView = this.inflater.inflate(R.layout.list_view_meeting, parent, false);

        // Initialize layout items
        RelativeLayout relativeLayout = (RelativeLayout) convertView.findViewById(R.id.relativeLayout);
        LayerDrawable roundedCard = (LayerDrawable) context.getResources().getDrawable(R.drawable.rounded_card);
        TextView titleView = (TextView) convertView.findViewById(R.id.titleView);
        TextView bodyView = (TextView) convertView.findViewById(R.id.bodyView);
        TextView chairmanView = (TextView) convertView.findViewById(R.id.chairmanView); 
        ImageButton favourite = (ImageButton) convertView.findViewById(R.id.favourite);

        // Get Meeting object at position
        final JSONObject meetingObject = getItem(position);

        if (meetingObject != null) {
            // If meetingObject not empty -> initialize variables
            String title = context.getString(R.string.meeting_title);
            String date = "";
            String time = context.getString(R.string.meeting_body);
            String meetingTime = "";
            String chairman = "";
            String colour = String.valueOf(context.getResources().getColor(R.color.white));
            int fontSize = 18;
            Boolean favoured = false;
            Boolean sameDate = false;

            try {
                // Get meetingObject data and store in variables
                title = meetingObject.getString(MEETING_TITLE);
                date = meetingObject.getString(MEETING_DATE);
                time = meetingObject.getString(MEETING_TIME);
                chairman = meetingObject.getString(MEETING_CHAIRMAN);
                favoured = meetingObject.getBoolean(MEETING_FAVOURED);

                Calendar calendar = Calendar.getInstance();
                if (date.equals(calendar.get(Calendar.YEAR) + "-" +
                        calendar.get(Calendar.MONTH) + "-" + calendar.get(Calendar.DAY_OF_MONTH)))
                    meetingTime = time;
                else
                    meetingTime = date;

            } catch (JSONException e) {
                e.printStackTrace();
            }


            // Set favourite image resource
            if (favoured)
                favourite.setImageResource(R.mipmap.ic_fav);

            else
                favourite.setImageResource(R.mipmap.ic_unfav);


            // If search or delete modes are active -> hide favourite button; Show otherwise
            if (searchActive || deleteActive) {
                favourite.setVisibility(View.INVISIBLE);
            }
            else {
                favourite.setVisibility(View.VISIBLE);
            }

            titleView.setText(title);

            
            // Set visible meeting body, text to normal and set text size to 'fontSize' as sp
            bodyView.setText(meetingTime);
            bodyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);

            // Set visible meeting chairman, text to normal and set text size to 'fontSize' as sp
            chairmanView.setText(chairman);
            chairmanView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);

            // If current meeting is selected for deletion -> highlight
            if (checkedArray.contains(position)) {
                ((GradientDrawable) roundedCard.findDrawableByLayerId(R.id.card))
                        .setColor(context.getResources().getColor(R.color.theme_primary));
            }

            // If current meeting is not selected -> set background colour to normal
            else {
                if (colour.contains("#"))
                    ((GradientDrawable) roundedCard.findDrawableByLayerId(R.id.card))
                            .setColor(Color.parseColor(colour));
                else
                    ((GradientDrawable) roundedCard.findDrawableByLayerId(R.id.card))
                            .setColor(Integer.parseInt(colour));
            }

            // Set meeting background style to rounded card
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                relativeLayout.setBackground(roundedCard);
            }

            final Boolean finalFavoured = favoured;
            favourite.setOnClickListener(new View.OnClickListener() {
                // If favourite button was clicked -> change that meeting to favourite or un-favourite
                @Override
                public void onClick(View v) {
                    setFavourite(context, !finalFavoured, position);
                }
            });
        }

        return convertView;
    }
}
