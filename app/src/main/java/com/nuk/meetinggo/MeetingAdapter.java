package com.nuk.meetinggo;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

public class MeetingAdapter extends FragmentStatePagerAdapter {
    int mNumOfTabs;

    FutureMeetingFragment tab1;
    PastMeetingFragment tab2;

    public MeetingAdapter(FragmentManager fm, int NumOfTabs) {
        super(fm);
        this.mNumOfTabs = NumOfTabs;
    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                tab1 = new FutureMeetingFragment();
                return tab1;
            case 1:
                tab2 = new PastMeetingFragment();
                return tab2;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}
