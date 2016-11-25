package com.nuk.meetinggo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
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

import static com.nuk.meetinggo.DataUtils.RECORD_BODY;
import static com.nuk.meetinggo.DataUtils.RECORD_FAVOURED;
import static com.nuk.meetinggo.DataUtils.RECORD_REFERENCE;
import static com.nuk.meetinggo.DataUtils.RECORD_TITLE;
import static com.nuk.meetinggo.RecordFragment.checkedArray;
import static com.nuk.meetinggo.RecordFragment.deleteActive;
import static com.nuk.meetinggo.RecordFragment.searchActive;
import static com.nuk.meetinggo.RecordFragment.setFavourite;
import static com.nuk.meetinggo.RecordFragment.updateRecord;

/**
 * Adapter class for custom records ListView
 */
public class RecordAdapter extends BaseAdapter implements ListAdapter {
    private Context context;
    private JSONArray adapterData;
    private LayoutInflater inflater;

    /**
     * Adapter constructor -> Sets class variables
     * @param context application context
     * @param adapterData JSONArray of records
     */
    public RecordAdapter(Context context, JSONArray adapterData) {
        this.context = context;
        this.adapterData = adapterData;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    // Return number of records
    @Override
    public int getCount() {
        if (this.adapterData != null)
            return this.adapterData.length();

        else
            return 0;
    }

    // Return record at position
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
        // Inflate custom record view if null
        if (convertView == null)
            convertView = this.inflater.inflate(R.layout.list_view_record, parent, false);

        // Initialize layout items
        RelativeLayout relativeLayout = (RelativeLayout) convertView.findViewById(R.id.relativeLayout);
        LayerDrawable roundedCard = (LayerDrawable) context.getResources().getDrawable(R.drawable.rounded_card);
        TextView titleView = (TextView) convertView.findViewById(R.id.titleView);
        TextView bodyView = (TextView) convertView.findViewById(R.id.bodyView);
        ImageButton favourite = (ImageButton) convertView.findViewById(R.id.favourite);
        ImageButton editButton = (ImageButton) convertView.findViewById(R.id.editButton);

        // Get Record object at position
        JSONObject recordObject = getItem(position);

        if (recordObject != null) {

            // If recordObject not empty -> initialize variables
            String title = context.getString(R.string.record_title);
            String body = context.getString(R.string.record_body);
            String reference = context.getString(R.string.record_reference);
            String colour = "#FFFFFF";
            int fontSize = 18;
            Boolean favoured = false;

            try {
                // Get recordObject data and store in variables
                title = recordObject.getString(RECORD_TITLE);
                body = recordObject.getString(RECORD_BODY);
                reference = recordObject.getString(RECORD_REFERENCE);
                favoured = recordObject.getBoolean(RECORD_FAVOURED);

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

            // If current record is selected for deletion -> highlight
            if (checkedArray.contains(position)) {
                ((GradientDrawable) roundedCard.findDrawableByLayerId(R.id.card))
                        .setColor(context.getResources().getColor(R.color.theme_primary));
            }

            // If current record is not selected -> set background colour to normal
            else {
                ((GradientDrawable) roundedCard.findDrawableByLayerId(R.id.card))
                        .setColor(Color.parseColor(colour));
            }

            // Set record background style to rounded card
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                relativeLayout.setBackground(roundedCard);
            }

            final Boolean finalFavoured = favoured;
            favourite.setOnClickListener(new View.OnClickListener() {
                // If favourite button was clicked -> change that record to favourite or un-favourite
                @Override
                public void onClick(View v) {
                    setFavourite(context, !finalFavoured, position);
                }
            });

            editButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LayoutInflater inflater = LayoutInflater.from(context);
                    final View recordView = inflater.inflate(R.layout.dialog_add_record, null);

                    // Edit record and add record are the same view
                    new AlertDialog.Builder(context)
                            .setTitle("編輯記錄")
                            .setView(recordView)
                            .setPositiveButton(R.string.yes_button, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    // If record field not empty -> continue
                                    EditText bodyText = (EditText) recordView.findViewById(R.id.recordBody);

                                    String record = bodyText.getText().toString();
                                    if(TextUtils.isEmpty(record))
                                        bodyText.setError(context.getString(R.string.error_field_required));
                                    else {
                                        // TODO update existed record
                                        updateRecord(context, record, position);
                                    }
                                }
                            })
                            .setNegativeButton(R.string.no_button, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .show();
                }
            });
        }

        return convertView;
    }
}
