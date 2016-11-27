package com.nuk.meetinggo;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;

public class SettingFragment extends Fragment {

    //擷取畫面按鈕
    private Button screenShot;
    //截圖的畫面
    private WebView screenImage;

    private boolean isConnect = false;

    public Handler messageHandler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        return view;
    }
}
