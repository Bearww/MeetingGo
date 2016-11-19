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
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.nuk.meetinggo.DataUtils.POLL_BODY;
import static com.nuk.meetinggo.DataUtils.POLL_COLOUR;
import static com.nuk.meetinggo.DataUtils.POLL_FAVOURED;
import static com.nuk.meetinggo.DataUtils.POLL_FONT_SIZE;
import static com.nuk.meetinggo.DataUtils.POLL_HIDE_BODY;
import static com.nuk.meetinggo.DataUtils.POLL_TITLE;
import static com.nuk.meetinggo.DataUtils.POLL_ENABLED;
import static com.nuk.meetinggo.MeetingInfo.getControlable;
import static com.nuk.meetinggo.PollFragment.checkedArray;
import static com.nuk.meetinggo.PollFragment.deleteActive;
import static com.nuk.meetinggo.PollFragment.searchActive;
import static com.nuk.meetinggo.PollFragment.setFavourite;
import static com.nuk.meetinggo.PollFragment.setMode;

/**
 * Adapter class for custom polls ListView
 */
public class PollAdapter extends BaseAdapter implements ListAdapter {
    private Context context;
    private JSONArray adapterData;
    private LayoutInflater inflater;

    /**
     * Adapter constructor -> Sets class variables
     * @param context application context
     * @param adapterData JSONArray of polls
     */
    public PollAdapter(Context context, JSONArray adapterData) {
        this.context = context;
        this.adapterData = adapterData;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    // Return number of polls
    @Override
    public int getCount() {
        if (this.adapterData != null)
            return this.adapterData.length();

        else
            return 0;
    }

    // Return poll at position
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
        // Inflate custom poll view if null
        if (convertView == null)
            convertView = this.inflater.inflate(R.layout.list_view_poll, parent, false);

        // Initialize layout items
        RelativeLayout relativeLayout = (RelativeLayout) convertView.findViewById(R.id.relativeLayout);
        LayerDrawable roundedCard = (LayerDrawable) context.getResources().getDrawable(R.drawable.rounded_card);
        TextView titleView = (TextView) convertView.findViewById(R.id.titleView);
        TextView bodyView = (TextView) convertView.findViewById(R.id.bodyView);
        ImageButton favourite = (ImageButton) convertView.findViewById(R.id.favourite);
        ImageButton check = (ImageButton) convertView.findViewById(R.id.check);
        Switch pollSwitch = (Switch) convertView.findViewById(R.id.pollSwitch);

        // Get Note object at position
        JSONObject pollObject = getItem(position);

        if (pollObject != null) {
            // Check presenter or controller
            Boolean controlled = false;

            // If pollObject not empty -> initialize variables
            String title = context.getString(R.string.poll_title);
            String body = context.getString(R.string.poll_body);
            String colour = String.valueOf(context.getResources().getColor(R.color.white));
            int fontSize = 18;
            Boolean hideBody = false;
            Boolean favoured = false;
            Boolean checked = false;
            Boolean enabled = false;

            try {
                controlled = getControlable(MemberInfo.memberID);

                // Get pollObject data and store in variables
                title = pollObject.getString(POLL_TITLE);
                body = pollObject.getString(POLL_BODY);
                colour = pollObject.getString(POLL_COLOUR);

                if (pollObject.has(POLL_FONT_SIZE))
                    fontSize = pollObject.getInt(POLL_FONT_SIZE);

                if (pollObject.has(POLL_HIDE_BODY))
                    hideBody = pollObject.getBoolean(POLL_HIDE_BODY);

                favoured = pollObject.getBoolean(POLL_FAVOURED);

                if (pollObject.has(POLL_ENABLED))
                    enabled = pollObject.getBoolean(POLL_ENABLED);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            // Set favourite image resource
            if (favoured)
                favourite.setImageResource(R.mipmap.ic_fav);

            else
                favourite.setImageResource(R.mipmap.ic_unfav);

            // Set check image resource
            if (checked)
                check.setImageResource(R.drawable.ic_check_box_black_24dp);
            else
                check.setImageResource(R.drawable.ic_check_box_outline_blank_black_24dp);

            // If search or delete modes are active -> hide favourite button; Show otherwise
            if (searchActive || deleteActive) {
                favourite.setVisibility(View.INVISIBLE);
                check.setVisibility(View.INVISIBLE);
                if(controlled) pollSwitch.setVisibility(View.INVISIBLE);
            }
            else {
                favourite.setVisibility(View.VISIBLE);
                check.setVisibility(View.VISIBLE);
                if(controlled) pollSwitch.setVisibility(View.VISIBLE);
            }

            // If presenter modes are active -> show poll switch; Hide otherwise
            if (controlled) {
                pollSwitch.setChecked(enabled);
                pollSwitch.setVisibility(View.VISIBLE);
            }
            else
                pollSwitch.setVisibility(View.GONE);

            titleView.setText(title);

            // If hidBody is true -> hide body of poll
            if (hideBody)
                bodyView.setVisibility(View.GONE);

                // Else -> set visible poll body, text to normal and set text size to 'fontSize' as sp
            else {
                bodyView.setVisibility(View.VISIBLE);
                bodyView.setText(body);
                bodyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
            }

            // If current poll is selected for deletion -> highlight
            if (checkedArray.contains(position)) {
                ((GradientDrawable) roundedCard.findDrawableByLayerId(R.id.card))
                        .setColor(context.getResources().getColor(R.color.theme_primary));
            }

            // If current poll is not selected -> set background colour to normal
            else {
                ((GradientDrawable) roundedCard.findDrawableByLayerId(R.id.card))
                        .setColor(Color.parseColor(colour));
            }

            // Set poll background style to rounded card
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                relativeLayout.setBackground(roundedCard);
            }

            final Boolean finalFavoured = favoured;
            favourite.setOnClickListener(new View.OnClickListener() {
                // If favourite button was clicked -> change that poll to favourite or un-favourite
                @Override
                public void onClick(View v) {
                    setFavourite(context, !finalFavoured, position);
                }
            });

            pollSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    setMode(isChecked, position);
                }
            });
        }

        return convertView;
    }
}
