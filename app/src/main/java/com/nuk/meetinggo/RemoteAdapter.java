package com.nuk.meetinggo;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

public class RemoteAdapter extends FragmentStatePagerAdapter {
    int mNumOfTabs;

    RemoteControlFragment tab1;
    DocumentFragment tab2;
    QuestionFragment tab3;
    PollFragment tab4;
    RecordFragment tab5;

    public RemoteAdapter(FragmentManager fm, int NumOfTabs) {
        super(fm);
        this.mNumOfTabs = NumOfTabs;
    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                if (tab1 == null)
                    tab1 = new RemoteControlFragment();
                return tab1;
            case 1:
                if (tab2 == null)
                    tab2 = new DocumentFragment();
                return tab2;
            case 2:
                if (tab3 == null)
                    tab3 = new QuestionFragment();
                return tab3;
            case 3:
                if (tab4 == null)
                    tab4 = new PollFragment();
                return tab4;
            case 4:
                if (tab5 == null)
                    tab5 = new RecordFragment();
                return tab5;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}
