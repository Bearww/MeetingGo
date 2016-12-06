package com.nuk.meetinggo;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
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

import static com.nuk.meetinggo.DataUtils.DOCUMENT_DOWNLOADED;
import static com.nuk.meetinggo.DataUtils.DOCUMENT_FAVOURED;
import static com.nuk.meetinggo.DataUtils.DOCUMENT_TITLE;
import static com.nuk.meetinggo.DataUtils.DOCUMENT_VIEW;
import static com.nuk.meetinggo.DocumentFragment.checkedArray;
import static com.nuk.meetinggo.DocumentFragment.deleteActive;
import static com.nuk.meetinggo.DocumentFragment.searchActive;
import static com.nuk.meetinggo.DocumentFragment.setFavourite;

/**
 * Adapter class for custom documents ListView
 */
public class DocumentAdapter extends BaseAdapter implements ListAdapter {
    private Context context;
    private JSONArray adapterData;
    private LayoutInflater inflater;

    /**
     * Adapter constructor -> Sets class variables
     * @param context application context
     * @param adapterData JSONArray of documents
     */
    public DocumentAdapter(Context context, JSONArray adapterData) {
        this.context = context;
        this.adapterData = adapterData;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    // Return number of documents
    @Override
    public int getCount() {
        if (this.adapterData != null)
            return this.adapterData.length();

        else
            return 0;
    }

    // Return document at position
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
        // Inflate custom document view if null
        if (convertView == null)
            convertView = this.inflater.inflate(R.layout.list_view_document, parent, false);

        // Initialize layout items
        RelativeLayout relativeLayout = (RelativeLayout) convertView.findViewById(R.id.relativeLayout);
        LayerDrawable roundedCard = (LayerDrawable) context.getResources().getDrawable(R.drawable.rounded_card);
        TextView titleView = (TextView) convertView.findViewById(R.id.titleView);
        TextView bodyView = (TextView) convertView.findViewById(R.id.bodyView);
        ImageButton favourite = (ImageButton) convertView.findViewById(R.id.favourite);
        ImageButton download = (ImageButton) convertView.findViewById(R.id.download);

        // Get Document object at position
        JSONObject documentObject = getItem(position);

        if (documentObject != null) {
            // Check presenter or controller
            Boolean controlled = false;

            // If documentObject not empty -> initialize variables
            String title = context.getString(R.string.document_title);
            String reference = context.getString(R.string.document_reference);
            String colour = String.valueOf(context.getResources().getColor(R.color.white));
            int fontSize = 18;
            Boolean hideBody = false;
            Boolean favoured = false;
            Boolean downloaded = false;

            try {
                // Get documentObject data and store in variables
                title = documentObject.getString(DOCUMENT_TITLE);
                favoured = documentObject.getBoolean(DOCUMENT_FAVOURED);
                downloaded = documentObject.getBoolean(DOCUMENT_DOWNLOADED) ||
                    documentObject.getString(DOCUMENT_VIEW).contains("drive");

            } catch (JSONException e) {
                e.printStackTrace();
            }

            // Set favourite image resource
            if (favoured)
                favourite.setImageResource(R.mipmap.ic_fav);

            else
                favourite.setImageResource(R.mipmap.ic_unfav);

            // Set download image resource
            if (downloaded)
                download.setImageResource(R.drawable.ic_cloud_done_black_24dp);

            else
                download.setImageResource(R.drawable.ic_cloud_download_black_24dp);

            // If search or delete modes are active -> hide favourite button; Show otherwise
            if (searchActive || deleteActive) {
                favourite.setVisibility(View.INVISIBLE);
            }
            else {
                favourite.setVisibility(View.VISIBLE);
            }

            titleView.setText(title);

            //bodyView.setVisibility(View.VISIBLE);
            //bodyView.setText(reference);
            //bodyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);

            // If current document is selected for deletion -> highlight
            if (checkedArray.contains(position)) {
                ((GradientDrawable) roundedCard.findDrawableByLayerId(R.id.card))
                        .setColor(context.getResources().getColor(R.color.theme_primary));
            }

            // If current document is not selected -> set background colour to normal
            else {
                if (colour.contains("#"))
                    ((GradientDrawable) roundedCard.findDrawableByLayerId(R.id.card))
                            .setColor(Color.parseColor(colour));
                else
                    ((GradientDrawable) roundedCard.findDrawableByLayerId(R.id.card))
                            .setColor(Integer.parseInt(colour));
            }

            // Set document background style to rounded card
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                relativeLayout.setBackground(roundedCard);
            }

            final Boolean finalFavoured = favoured;
            favourite.setOnClickListener(new View.OnClickListener() {
                // If favourite button was clicked -> change that document to favourite or un-favourite
                @Override
                public void onClick(View v) {
                    setFavourite(context, !finalFavoured, position);
                }
            });
        }

        return convertView;
    }
}
