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

import static com.nuk.meetinggo.DataUtils.ANSWER_CONTENT;
import static com.nuk.meetinggo.DataUtils.ANSWER_FAVOURED;
import static com.nuk.meetinggo.DataUtils.ANSWER_OWNER;
import static com.nuk.meetinggo.ViewQuestionFragment.setFavourite;

/**
 * Adapter class for custom answers ListView
 */
public class AnswerAdapter extends BaseAdapter implements ListAdapter {
    private Context context;
    private JSONArray adapterData;
    private LayoutInflater inflater;

    /**
     * Adapter constructor -> Sets class variables
     * @param context application context
     * @param adapterData JSONArray of answers
     */
    public AnswerAdapter(Context context, JSONArray adapterData) {
        this.context = context;
        this.adapterData = adapterData;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    // Return number of answers
    @Override
    public int getCount() {
        if (this.adapterData != null)
            return this.adapterData.length();

        else
            return 0;
    }

    // Return answer at position
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
        // Inflate custom answer view if null
        if (convertView == null)
            convertView = this.inflater.inflate(R.layout.list_view_question, parent, false);

        // Initialize layout items
        RelativeLayout relativeLayout = (RelativeLayout) convertView.findViewById(R.id.relativeLayout);
        LayerDrawable roundedCard = (LayerDrawable) context.getResources().getDrawable(R.drawable.rounded_card);
        TextView titleView = (TextView) convertView.findViewById(R.id.titleView);
        TextView bodyView = (TextView) convertView.findViewById(R.id.bodyView);
        ImageButton favourite = (ImageButton) convertView.findViewById(R.id.favourite);

        // Get Ask object at position
        JSONObject answerObject = getItem(position);

        if (answerObject != null) {
            // If answerObject not empty -> initialize variables
            String title = context.getString(R.string.question_title);
            String body = context.getString(R.string.question_body);
            String colour = "#FFFFFF";
            int fontSize = 18;
            Boolean favoured = false;

            try {
                // Get answerObject data and store in variables
                title = answerObject.getString(ANSWER_CONTENT);
                body = answerObject.getString(ANSWER_OWNER);

                favoured = answerObject.getBoolean(ANSWER_FAVOURED);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            // Set favourite image resource
            if (favoured)
                favourite.setImageResource(R.mipmap.ic_fav);

            else
                favourite.setImageResource(R.mipmap.ic_unfav);

            titleView.setText(title);

            bodyView.setVisibility(View.VISIBLE);
            bodyView.setText(body);
            bodyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);

            ((GradientDrawable) roundedCard.findDrawableByLayerId(R.id.card))
                    .setColor(Color.parseColor(colour));

            // Set answer background style to rounded card
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                relativeLayout.setBackground(roundedCard);
            }

            final Boolean finalFavoured = favoured;
            favourite.setOnClickListener(new View.OnClickListener() {
                // If favourite button was clicked -> change that answer to favourite or un-favourite
                @Override
                public void onClick(View v) {
                    setFavourite(context, !finalFavoured, position);
                }
            });
        }

        return convertView;
    }
}
