package com.nuk.meetinggo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
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

import static com.nuk.meetinggo.DataUtils.NOTE_BODY;
import static com.nuk.meetinggo.DataUtils.NOTE_COLOUR;
import static com.nuk.meetinggo.DataUtils.NOTE_FAVOURED;
import static com.nuk.meetinggo.DataUtils.NOTE_FONT_SIZE;
import static com.nuk.meetinggo.DataUtils.NOTE_HIDE_BODY;
import static com.nuk.meetinggo.DataUtils.NOTE_ID;
import static com.nuk.meetinggo.DataUtils.NOTE_TITLE;
import static com.nuk.meetinggo.MainFragment.checkedArray;
import static com.nuk.meetinggo.MainFragment.deleteActive;
import static com.nuk.meetinggo.MainFragment.searchActive;
import static com.nuk.meetinggo.MainFragment.setFavourite;
import static com.nuk.meetinggo.MeetingInfo.getControllable;
import static com.nuk.meetinggo.MeetingInfo.topicID;

/**
 * Adapter class for custom notes ListView
 */
public class NoteAdapter extends BaseAdapter implements ListAdapter {
    private Context context;
    private JSONArray adapterData;
    private LayoutInflater inflater;

    /**
     * Adapter constructor -> Sets class variables
     * @param context application context
     * @param adapterData JSONArray of notes
     */
    public NoteAdapter(Context context, JSONArray adapterData) {
        this.context = context;
        this.adapterData = adapterData;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    // Return number of notes
    @Override
    public int getCount() {
        if (this.adapterData != null)
            return this.adapterData.length();

        else
            return 0;
    }

    // Return note at position
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
        // Inflate custom note view if null
        if (convertView == null)
            convertView = this.inflater.inflate(R.layout.list_view_note, parent, false);

        // Initialize layout items
        RelativeLayout relativeLayout = (RelativeLayout) convertView.findViewById(R.id.relativeLayout);
        LayerDrawable roundedCard = (LayerDrawable) context.getResources().getDrawable(R.drawable.rounded_card);
        TextView titleView = (TextView) convertView.findViewById(R.id.titleView);
        TextView bodyView = (TextView) convertView.findViewById(R.id.bodyView);
        ImageButton favourite = (ImageButton) convertView.findViewById(R.id.favourite);
        ImageButton control = (ImageButton) convertView.findViewById(R.id.control);

        // Get Note object at position
        JSONObject noteObject = getItem(position);

        if (noteObject != null) {
            // If noteObject not empty -> initialize variables
            int id = 0;
            String title = context.getString(R.string.note_title);
            String body = context.getString(R.string.note_body);
            String colour = String.valueOf(context.getResources().getColor(R.color.white));
            int fontSize = 18;
            Boolean hideBody = false;
            Boolean favoured = false;
            Boolean controlled = false;

            try {
                // Get noteObject data and store in variables
                id = noteObject.getInt(NOTE_ID);
                title = noteObject.getString(NOTE_TITLE);
                body = noteObject.getString(NOTE_BODY);
                colour = noteObject.getString(NOTE_COLOUR);

                if (noteObject.has(NOTE_FONT_SIZE))
                    fontSize = noteObject.getInt(NOTE_FONT_SIZE);

                if (noteObject.has(NOTE_HIDE_BODY))
                    hideBody = noteObject.getBoolean(NOTE_HIDE_BODY);

                favoured = noteObject.getBoolean(NOTE_FAVOURED);
                controlled = getControllable(MemberInfo.memberID);

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
                if(controlled) control.setVisibility(View.INVISIBLE);
            }
            else {
                favourite.setVisibility(View.VISIBLE);
                if(controlled) control.setVisibility(View.VISIBLE);
            }

            // If presenter modes are active -> show control button; Hide otherwise
            if (controlled)
                control.setVisibility(View.VISIBLE);

            else
                control.setVisibility(View.GONE);

            titleView.setText(title);

            // If hidBody is true -> hide body of note
            if (hideBody)
                bodyView.setVisibility(View.GONE);

            // Else -> set visible note body, text to normal and set text size to 'fontSize' as sp
            else {
                bodyView.setVisibility(View.VISIBLE);
                bodyView.setText(body);
                bodyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
            }

            // If current note is selected for deletion -> highlight
            if (checkedArray.contains(position)) {
                ((GradientDrawable) roundedCard.findDrawableByLayerId(R.id.card))
                        .setColor(context.getResources().getColor(R.color.theme_primary));
            }

            // If current note is not selected -> set background colour to normal
            else {
                ((GradientDrawable) roundedCard.findDrawableByLayerId(R.id.card))
                        .setColor(Color.parseColor(colour));
            }

            // Set note background style to rounded card
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                relativeLayout.setBackground(roundedCard);
            }

            final Boolean finalFavoured = favoured;
            favourite.setOnClickListener(new View.OnClickListener() {
                // If favourite button was clicked -> change that note to favourite or un-favourite
                @Override
                public void onClick(View v) {
                    setFavourite(context, !finalFavoured, position);
                }
            });

            final int finalID = id;
            final String finalBody = body;
            control.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Bundle bundle = new Bundle();

                    bundle.putString(NOTE_BODY, finalBody);

                    Intent intent = new Intent(context, RemoteActivity.class);
                    intent.putExtras(bundle);
                    topicID = finalID;
                    context.startActivity(intent);
                }
            });
        }

        return convertView;
    }
}
