package com.nuk.meetinggo;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.nuk.meetinggo.DataUtils.OPTION_CONTENT;
import static com.nuk.meetinggo.DataUtils.OPTION_VOTES;
import static com.nuk.meetinggo.EditPollFragment.addOption;
import static com.nuk.meetinggo.EditPollFragment.changeOption;
import static com.nuk.meetinggo.EditPollFragment.deleteOption;
import static com.nuk.meetinggo.EditPollFragment.isLastOption;
import static com.nuk.meetinggo.EditPollFragment.optionChanged;
import static com.nuk.meetinggo.ViewPollFragment.checkedArray;
import static com.nuk.meetinggo.PollFragment.editActive;
import static com.nuk.meetinggo.ViewPollFragment.pollActive;

/**
 * Adapter class for custom options ListView
 */
public class OptionAdapter extends BaseAdapter implements ListAdapter {
    private Context context;
    private JSONArray adapterData;
    private LayoutInflater inflater;

    /**
     * Adapter constructor -> Sets class variables
     * @param context application context
     * @param adapterData JSONArray of options
     */
    public OptionAdapter(Context context, JSONArray adapterData) {
        this.context = context;
        this.adapterData = adapterData;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    // Return number of options
    @Override
    public int getCount() {
        if (this.adapterData != null)
            return this.adapterData.length();

        else
            return 0;
    }

    // Return option at position
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
        // Inflate custom option view if null
        if (convertView == null)
            convertView = this.inflater.inflate(R.layout.list_view_option, parent, false);

        // Initialize layout items
        RelativeLayout relativeLayout = (RelativeLayout) convertView.findViewById(R.id.relativeLayout);
        LayerDrawable roundedCard = (LayerDrawable) context.getResources().getDrawable(R.drawable.rounded_card);
        final EditText bodyText = (EditText) convertView.findViewById(R.id.optionText);
        TextView bodyView = (TextView) convertView.findViewById(R.id.optionView);
        TextView votesView = (TextView) convertView.findViewById(R.id.votesView);
        ImageButton delete = (ImageButton) convertView.findViewById(R.id.delete);

        // Get Option object at position
        final JSONObject optionObject = getItem(position);

        if (optionObject != null) {
            // If optionObject not empty -> initialize variables
            String body = context.getString(R.string.option_body);
            String colour = "#FFFFFF";
            int fontSize = 18;
            String votes = "0";

            try {
                // Get optionObject data and store in variables
                body = optionObject.getString(OPTION_CONTENT);
                votes = optionObject.getString(OPTION_VOTES);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (editActive) {
                bodyText.setVisibility(View.VISIBLE);
                bodyText.setText(body);
                bodyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
                bodyText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (!hasFocus) {
                            changeOption(position, bodyText.getText().toString());

                            if (isLastOption(position) && !TextUtils.isEmpty(bodyText.getText().toString())) {
                                addOption();
                                optionChanged();
                            }
                        }
                    }
                });
                bodyText.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (v instanceof EditText) {
                            v.setFocusable(true);
                            v.setFocusableInTouchMode(true);
                        } else {
                            bodyText.setFocusable(false);
                            bodyText.setFocusableInTouchMode(false);
                        }
                        return false;
                    }
                });

                bodyView.setVisibility(View.INVISIBLE);
            }
            else {
                bodyText.setVisibility(View.INVISIBLE);
                bodyView.setVisibility(View.VISIBLE);
                bodyView.setText(body);
                bodyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);

                votesView.setText(votes);
                if (pollActive)
                    votesView.setVisibility(View.VISIBLE);
                else
                    votesView.setVisibility(View.INVISIBLE);
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

            // Set option background style to rounded card
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                relativeLayout.setBackground(roundedCard);
            }

            if (editActive) {
                if (isLastOption(position))
                    delete.setVisibility(View.INVISIBLE);
                else
                    delete.setVisibility(View.VISIBLE);
            }
            else
                delete.setVisibility(View.INVISIBLE);

            delete.setOnClickListener(new View.OnClickListener() {
                // If favourite button was clicked -> change that option to favourite or un-favourite
                @Override
                public void onClick(View v) {
                    deleteOption(context, position);
                }
            });
        }

        return convertView;
    }
}
