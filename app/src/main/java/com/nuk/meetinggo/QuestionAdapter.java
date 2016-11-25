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

import static com.nuk.meetinggo.DataUtils.QUESTION_BODY;
import static com.nuk.meetinggo.DataUtils.QUESTION_COLOUR;
import static com.nuk.meetinggo.DataUtils.QUESTION_FAVOURED;
import static com.nuk.meetinggo.DataUtils.QUESTION_FONT_SIZE;
import static com.nuk.meetinggo.DataUtils.QUESTION_TITLE;
import static com.nuk.meetinggo.QuestionFragment.checkedArray;
import static com.nuk.meetinggo.QuestionFragment.deleteActive;
import static com.nuk.meetinggo.QuestionFragment.searchActive;
import static com.nuk.meetinggo.QuestionFragment.setFavourite;

/**
 * Adapter class for custom questions ListView
 */
public class QuestionAdapter extends BaseAdapter implements ListAdapter {
    private Context context;
    private JSONArray adapterData;
    private LayoutInflater inflater;

    /**
     * Adapter constructor -> Sets class variables
     * @param context application context
     * @param adapterData JSONArray of questions
     */
    public QuestionAdapter(Context context, JSONArray adapterData) {
        this.context = context;
        this.adapterData = adapterData;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    // Return number of questions
    @Override
    public int getCount() {
        if (this.adapterData != null)
            return this.adapterData.length();

        else
            return 0;
    }

    // Return question at position
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
        // Inflate custom question view if null
        if (convertView == null)
            convertView = this.inflater.inflate(R.layout.list_view_question, parent, false);

        // Initialize layout items
        RelativeLayout relativeLayout = (RelativeLayout) convertView.findViewById(R.id.relativeLayout);
        LayerDrawable roundedCard = (LayerDrawable) context.getResources().getDrawable(R.drawable.rounded_card);
        TextView titleView = (TextView) convertView.findViewById(R.id.titleView);
        TextView bodyView = (TextView) convertView.findViewById(R.id.bodyView);
        ImageButton favourite = (ImageButton) convertView.findViewById(R.id.favourite);

        // Get Ask object at position
        JSONObject questionObject = getItem(position);

        if (questionObject != null) {
            // If questionObject not empty -> initialize variables
            String title = context.getString(R.string.question_title);
            String body = context.getString(R.string.question_body);
            String colour = "#FFFFFF";
            int fontSize = 18;
            Boolean favoured = false;

            try {
                // Get questionObject data and store in variables
                title = questionObject.getString(QUESTION_TITLE);
                body = questionObject.getString(QUESTION_BODY);
                colour = questionObject.getString(QUESTION_COLOUR);

                if (questionObject.has(QUESTION_FONT_SIZE))
                    fontSize = questionObject.getInt(QUESTION_FONT_SIZE);

                favoured = questionObject.getBoolean(QUESTION_FAVOURED);

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

            bodyView.setVisibility(View.VISIBLE);
            bodyView.setText(body);
            bodyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);

            // If current question is selected for deletion -> highlight
            if (checkedArray.contains(position)) {
                ((GradientDrawable) roundedCard.findDrawableByLayerId(R.id.card))
                        .setColor(context.getResources().getColor(R.color.theme_primary));
            }

            // If current question is not selected -> set background colour to normal
            else {
                ((GradientDrawable) roundedCard.findDrawableByLayerId(R.id.card))
                        .setColor(Color.parseColor(colour));
            }

            // Set question background style to rounded card
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                relativeLayout.setBackground(roundedCard);
            }

            final Boolean finalFavoured = favoured;
            favourite.setOnClickListener(new View.OnClickListener() {
                // If favourite button was clicked -> change that question to favourite or un-favourite
                @Override
                public void onClick(View v) {
                    setFavourite(context, !finalFavoured, position);
                }
            });
        }

        return convertView;
    }
}
