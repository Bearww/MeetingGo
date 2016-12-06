package com.nuk.meetinggo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.nuk.meetinggo.DataUtils.MEMBER_ID;
import static com.nuk.meetinggo.DataUtils.MEMBER_NAME;
import static com.nuk.meetinggo.DataUtils.MEMBER_ONLINE;
import static com.nuk.meetinggo.MeetingActivity.controlViewVisibility;
import static com.nuk.meetinggo.MeetingInfo.getControllable;
import static com.nuk.meetinggo.MeetingInfo.isController;
import static com.nuk.meetinggo.MeetingInfo.isPresenter;
import static com.nuk.meetinggo.MemberFragment.checkedArray;
import static com.nuk.meetinggo.MemberFragment.controlActive;
import static com.nuk.meetinggo.MemberFragment.deleteActive;

/**
 * Adapter class for custom members ListView
 */
public class MemberAdapter extends BaseAdapter implements ListAdapter {
    private Context context;
    private JSONArray adapterData;
    private LayoutInflater inflater;

    /**
     * Adapter constructor -> Sets class variables
     * @param context application context
     * @param adapterData JSONArray of members
     */
    public MemberAdapter(Context context, JSONArray adapterData) {
        this.context = context;
        this.adapterData = adapterData;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    // Return number of members
    @Override
    public int getCount() {
        if (this.adapterData != null)
            return this.adapterData.length();

        else
            return 0;
    }

    // Return member at position
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
        // Inflate custom member view if null
        if (convertView == null)
            convertView = this.inflater.inflate(R.layout.list_view_member, parent, false);

        // Initialize layout items
        RelativeLayout relativeLayout = (RelativeLayout) convertView.findViewById(R.id.relativeLayout);
        LinearLayout controlLayout = (LinearLayout) convertView.findViewById(R.id.controlLayout);
        LinearLayout memberLayout = (LinearLayout) convertView.findViewById(R.id.memberLayout);
        LayerDrawable roundedCard = (LayerDrawable) context.getResources().getDrawable(R.drawable.rounded_card);
        TextView idView = (TextView) convertView.findViewById(R.id.idView);
        CheckedTextView nameView = (CheckedTextView) convertView.findViewById(R.id.nameView);
        ImageView controllerMark = (ImageView) convertView.findViewById(R.id.controllerMark);
        //ImageView chairmanMark = (ImageView) convertView.findViewById(R.id.chairmanMark);
        ImageView presenterMark = (ImageView) convertView.findViewById(R.id.presenterMark);
        ImageButton controllerButton = (ImageButton) convertView.findViewById(R.id.controllerButton);
        //ImageButton chairmanButton = (ImageButton) convertView.findViewById(R.id.chairmanButton);
        ImageButton presenterButton = (ImageButton) convertView.findViewById(R.id.presenterButton);

        // Get Member object at position
        JSONObject memberObject = getItem(position);

        if (memberObject != null) {

            // If memberObject not empty -> initialize variables
            String id = context.getString(R.string.member_id);
            String name = context.getString(R.string.member_name);
            String colour = String.valueOf(context.getResources().getColor(R.color.white));
            int online = 0;
            int fontSize = 18;

            try {
                // Get memberObject data and store in variables
                id = memberObject.getString(MEMBER_ID);
                name = memberObject.getString(MEMBER_NAME);

                if (memberObject.has(MEMBER_ONLINE))
                    online = memberObject.getInt(MEMBER_ONLINE);

                if (online > 0)
                    colour = String.valueOf(ContextCompat.getColor(context, R.color.green));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // If delete mode is active -> hide layout; Show otherwise
            if (deleteActive) {
                controlLayout.setVisibility(View.INVISIBLE);
                memberLayout.setVisibility(View.INVISIBLE);
            }
            else {
                // If control mode is active -> hide member layout; Show otherwise
                if (controlActive) {
                    controlLayout.setVisibility(View.VISIBLE);
                    memberLayout.setVisibility(View.INVISIBLE);

                    if (isController(MemberInfo.memberID)) controllerButton.setVisibility(View.VISIBLE);
                    else controllerButton.setVisibility(View.GONE);

                    //if (isChairman(MemberInfo.memberID)) chairmanButton.setVisibility(View.VISIBLE);
                    //else chairmanButton.setVisibility(View.GONE);

                    if (isPresenter(MemberInfo.memberID)) presenterButton.setVisibility(View.VISIBLE);
                    else presenterButton.setVisibility(View.GONE);
                }
                else {
                    memberLayout.setVisibility(View.VISIBLE);
                    controlLayout.setVisibility(View.INVISIBLE);

                    if (isController(name)) controllerMark.setVisibility(View.VISIBLE);
                    else controllerMark.setVisibility(View.GONE);

                    //if (isChairman(id)) chairmanMark.setVisibility(View.VISIBLE);
                    //else chairmanMark.setVisibility(View.GONE);

                    if (isPresenter(name)) presenterMark.setVisibility(View.VISIBLE);
                    else presenterMark.setVisibility(View.GONE);
                }
            }

            idView.setText(id);

            // Set member name, text to normal and set text size to 'fontSize' as sp
            nameView.setVisibility(View.VISIBLE);
            nameView.setText(name);
            nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);

            // If current member is selected for deletion -> highlight
            if (checkedArray.contains(position)) {
                ((GradientDrawable) roundedCard.findDrawableByLayerId(R.id.card))
                        .setColor(ContextCompat.getColor(context, R.color.theme_primary));
            }

            // If current member is not selected -> set background colour to normal
            else {
                if (colour.contains("#"))
                    ((GradientDrawable) roundedCard.findDrawableByLayerId(R.id.card))
                            .setColor(Color.parseColor(colour));
                else
                    ((GradientDrawable) roundedCard.findDrawableByLayerId(R.id.card))
                            .setColor(Integer.parseInt(colour));
            }

            // Set member background style to rounded card
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                relativeLayout.setBackground(roundedCard);
            }

            final String finalId = id;
            final String finalName = name;
            controllerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!isController(finalId)) {
                        new AlertDialog.Builder(context)
                                .setTitle("設" + finalName +"為主控？")
                                .setPositiveButton(R.string.yes_button, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // Set controller to member
                                        MeetingInfo.controller = finalId;

                                        if (!getControllable(MemberInfo.memberName))
                                            controlViewVisibility(false);
                                        else
                                            controlViewVisibility(true);
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
                }
            });
/*
            chairmanButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!isChairman(finalId)) {
                        new AlertDialog.Builder(context)
                                .setTitle("設" + finalName +"為主席？")
                                .setPositiveButton(R.string.yes_button, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // Set controller to member
                                        MeetingInfo.chairman = finalId;
                                        // TODO update ui
                                        if (!getControlable(MemberInfo.memberID))
                                            controlViewVisibility(false);
                                        else
                                            controlViewVisibility(true);
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
                }
            });
*/
            presenterButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!isPresenter(finalId)) {
                        new AlertDialog.Builder(context)
                                .setTitle("設" + finalName +"為簡報者？")
                                .setPositiveButton(R.string.yes_button, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // Set controller to member
                                        MeetingInfo.presenter = finalId;
                                        if (!getControllable(MemberInfo.memberName))
                                            controlViewVisibility(false);
                                        else
                                            controlViewVisibility(true);
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
                }
            });
        }

        return convertView;
    }
}
