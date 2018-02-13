/*
 * Paperclickers - Affordable solution for classroom response system.
 *
 * Copyright (C) 2015-2018 Eduardo Valle Jr <dovalle@dca.fee.unicamp.br>
 * Copyright (C) 2015-2018 Eduardo Seiti de Oliveira <eduseiti@dca.fee.unicamp.br>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */

package com.paperclickers.onboarding;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.paperclickers.R;
import com.paperclickers.log;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by eduseiti on 04/02/18.
 */

public class OnboardingActivity extends FragmentActivity {

    /**
     * The number of pages (wizard steps) to show in this demo.
     */
    private static final int NUM_PAGES = 4;

    private static final int ONBOARDING_SHOW_TIMER = 3000;

    final static String TAG = "OnboardingActivity";


    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private ViewPager mPager;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private PagerAdapter mPagerAdapter;

    private ImageView[] mOnboarding_page_indicators;
    private LinearLayout mOnboarding_indicators;

    private int mCurrentPage;

    private Timer mOnboardingPageSwitcherTimer = null;

    private boolean mTimerRunning = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.onboarding_activity);

        // Instantiate a ViewPager and a PagerAdapter.

        mOnboarding_indicators = (LinearLayout) findViewById(R.id.onboarding_page_indicators);

        mPager = (ViewPager) findViewById(R.id.onboarding_carousel);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());

        mPager.setAdapter(mPagerAdapter);

        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

                mOnboarding_page_indicators[mCurrentPage].setImageDrawable(getResources().getDrawable(R.drawable.onboarding_page_indicator_off));
                mOnboarding_page_indicators[position].setImageDrawable(getResources().getDrawable(R.drawable.onboarding_page_indicator_on));

                mCurrentPage = position;

                schedulePageSwitcherTimer();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });


        mCurrentPage = 0;

        mPager.setCurrentItem(mCurrentPage);

        mOnboarding_page_indicators = new ImageView[NUM_PAGES];

        for (int page = 0; page < NUM_PAGES; page++) {
            mOnboarding_page_indicators[page] = new ImageView(getApplicationContext());

            if (page != mCurrentPage) {
                mOnboarding_page_indicators[page].setImageDrawable(getResources().getDrawable(R.drawable.onboarding_page_indicator_off));
            } else {
                mOnboarding_page_indicators[page].setImageDrawable(getResources().getDrawable(R.drawable.onboarding_page_indicator_on));
            }

            mOnboarding_page_indicators[page].setPadding(5, 5, 5, 5);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

            params.gravity = Gravity.CENTER;

            mOnboarding_indicators.addView(mOnboarding_page_indicators[page], params);
        }
    }



    @Override
    public void onBackPressed() {
        if (mPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            // Otherwise, select the previous step.
            mPager.setCurrentItem(mPager.getCurrentItem() - 1);
        }
    }



    @Override
    protected void onPause() {
        super.onPause();

        log.d(TAG, "onPause");

        if (mOnboardingPageSwitcherTimer != null) {
            mOnboardingPageSwitcherTimer.cancel();
            mOnboardingPageSwitcherTimer = null;
        }
    }



    @Override
    protected void onResume() {
        super.onResume();

        schedulePageSwitcherTimer();
    }



    private void schedulePageSwitcherTimer() {

        log.d(TAG, "schedulePageSwitcherTimer. mTimerRunning = " + mTimerRunning);

//        if (mCurrentPage < NUM_PAGES - 1) {
//
            if (mOnboardingPageSwitcherTimer != null) {
                mOnboardingPageSwitcherTimer.cancel();
                mOnboardingPageSwitcherTimer = null;
            }

            mOnboardingPageSwitcherTimer = new Timer();

            mOnboardingPageSwitcherTimer.schedule(new TimerTask() {

                public void run() {

                    if (mPager != null) {
                        runOnUiThread(switchPage);
                    }
                }
            }, (long) ONBOARDING_SHOW_TIMER);
//        }
    }



    final Runnable switchPage = new Runnable() {
        public void run() {

            mOnboardingPageSwitcherTimer = null;

            if (mCurrentPage == NUM_PAGES - 1) {
                mPager.setCurrentItem(0);
            } else {
                mPager.setCurrentItem(mCurrentPage + 1);
            }
        }
    };




    /**
     * A simple pager adapter that represents 3 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            OnboardingPageFragment onboardingPage = new OnboardingPageFragment();

            Bundle args = new Bundle();
            args.putInt("position", position);

            onboardingPage.setArguments(args);

            return onboardingPage;
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }
}
