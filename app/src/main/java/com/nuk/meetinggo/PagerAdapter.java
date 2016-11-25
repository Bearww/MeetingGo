package com.nuk.meetinggo;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

public class PagerAdapter extends FragmentStatePagerAdapter {
    int mNumOfTabs;

    MainFragment tab1;
    MemberFragment tab2;
    DocumentFragment tab3;
    QuestionFragment tab4;
    PollFragment tab5;
    RecordFragment tab6;

    public PagerAdapter(FragmentManager fm, int NumOfTabs) {
        super(fm);
        this.mNumOfTabs = NumOfTabs;
    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                if (tab1 == null)
                    tab1 = new MainFragment();
                //TabFragment1 tab1 = new TabFragment1();
                return tab1;
            case 1:
                if (tab2 == null)
                    tab2 = new MemberFragment();
                return tab2;
            case 2:
                if (tab3 == null)
                    tab3 = new DocumentFragment();
                return tab3;
            case 3:
                if (tab4 == null)
                    tab4 = new QuestionFragment();
                return tab4;
            case 4:
                if (tab5 == null)
                    tab5 = new PollFragment();
                return tab5;
            case 5:
                if (tab6 == null)
                    tab6 = new RecordFragment();
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
