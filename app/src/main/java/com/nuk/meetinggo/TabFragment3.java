package com.nuk.meetinggo;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;

public class TabFragment3 extends Fragment {

    //擷取畫面按鈕
    private Button screenShot;
    //截圖的畫面
    private WebView screenImage;

    private boolean isConnect = false;

    public Handler messageHandler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_fragment_3, container, false);

        //取得Button與ImageView元件
        screenShot = (Button) view.findViewById(R.id.ScreenShot);
        screenImage = (WebView) view.findViewById(R.id.ScreenImage);

        String pdfUrl = "http://www.csie.nuk.edu.tw/~rex/test2.pdf";
        screenImage.getSettings().setJavaScriptEnabled(true);

        screenImage.loadUrl("https://drive.google.com/viewerng/viewer?embedded=true&url=" + pdfUrl);
        //screenImage.loadUrl("http://www.csie.nuk.edu.tw/~rex/RaspberryPi.pdf");

        //點擊按鈕觸發
        screenShot.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(!isConnect) {
                    Thread provider = new Thread(new ImageProvider(Constants.SERVER_RECVIMAGE, screenImage, getActivity()));
                    provider.start();
                    //TODO change connection checking
                    //isConnect = true;
                }
            }
        });
        return view;
    }
}
