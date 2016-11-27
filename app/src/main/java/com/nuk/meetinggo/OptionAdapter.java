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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.nuk.meetinggo.DataUtils.OPTION_CONTENT;
import static com.nuk.meetinggo.EditPollFragment.deleteOption;

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
        EditText bodyView = (EditText) convertView.findViewById(R.id.optionText);
        ImageButton delete = (ImageButton) convertView.findViewById(R.id.delete);

        // Get Ask object at position
        JSONObject optionObject = getItem(position);

        if (optionObject != null) {
            // If optionObject not empty -> initialize variables
            String body = context.getString(R.string.option_body);
            String colour = "#FFFFFF";
            int fontSize = 18;

            try {
                // Get optionObject data and store in variables
                body = optionObject.getString(OPTION_CONTENT);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            bodyView.setVisibility(View.VISIBLE);
            bodyView.setText(body);
            bodyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);

            ((GradientDrawable) roundedCard.findDrawableByLayerId(R.id.card))
                    .setColor(Color.parseColor(colour));

            // Set option background style to rounded card
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                relativeLayout.setBackground(roundedCard);
            }

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