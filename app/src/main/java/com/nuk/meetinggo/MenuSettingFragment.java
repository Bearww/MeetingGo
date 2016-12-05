package com.nuk.meetinggo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import static com.nuk.meetinggo.LinkCloud.SERVER_IP;
import static com.nuk.meetinggo.LinkCloud.setIP;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 */
public class MenuSettingFragment extends Fragment {

    private View view;

    public MenuSettingFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_menu_setting, container, false);

        final EditText ipText = (EditText) view.findViewById(R.id.ipText);
        Button setButton = (Button) view.findViewById(R.id.setButton);

        ipText.setText(SERVER_IP);

        setButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newIP = ipText.getText().toString();

                if (TextUtils.isEmpty(newIP))
                    ipText.requestFocus();
                else {
                    if (setIP(newIP)) {
                        // TODO try login success or fail
                        Toast.makeText(getContext(), "IP 設定成功", Toast.LENGTH_LONG).show();
                        ipText.setText(newIP);
                    }
                    else
                        Toast.makeText(getContext(), "IP 設定失敗", Toast.LENGTH_LONG).show();
                }
            }
        });

        return view;
    }
}
