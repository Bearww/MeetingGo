package com.nuk.meetinggo;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

public class PagerAdapter extends FragmentStatePagerAdapter {
    int mNumOfTabs;

    public PagerAdapter(FragmentManager fm, int NumOfTabs) {
        super(fm);
        this.mNumOfTabs = NumOfTabs;
    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                MainFragment tab1 = new MainFragment();
                //TabFragment1 tab1 = new TabFragment1();
                return tab1;
            case 1:
                MemberFragment tab2 = new MemberFragment();
                return tab2;
            case 2:
                DocumentFragment tab3 = new DocumentFragment();
                return tab3;
            case 3:
                QuestionFragment tab4 = new QuestionFragment();
                return tab4;
            case 4:
                TabFragment3 tab5 = new TabFragment3();
                return tab5;
            case 5:
                TabFragment3 tab6 = new TabFragment3();
                return tab6;
            case 6:
                TabFragment3 tab7 = new TabFragment3();
                return tab7;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}
