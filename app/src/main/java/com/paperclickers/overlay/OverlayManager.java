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

package com.paperclickers.overlay;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.paperclickers.Analytics;
import com.paperclickers.R;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by eduseiti on 14/02/18.
 */

public class OverlayManager {

    static final int SHOW_OVERLAY_TIMER = 1500;
    static final int HIDE_OVERLAY_TIMER = 7000;

    public static final String OVERLAY_ALREADY_SHOWN_PREFERENCES[] = {
            "overlay_start_screen_print_already_shown",
            "overlay_start_screen_shared_already_shown",
            "overlay_capture_screen_already_shown",
            "overlay_answers_screen_already_shown",
            "overlay_chart_screen_already_shown"
    };

    public static final int NO_OVERLAY           = -1;
    public static final int INITIAL_SCREEN       = 0;
    public static final int INITIAL_SCREEN_SHARE = 1;
    public static final int CAPTURE_SCREEN       = 2;
    public static final int ANSWERS_SCREEN       = 3;
    public static final int CHART_SCREEN         = 4;


    private Context mContext = null;
    private FragmentManager mFragmentManager = null;

    private Timer mOpenOverlayTimer  = null;

    private SharedPreferences mSharedPreferences = null;

    private int mWhichOverlay;



    public void checkAndTurnOnOverlayTimer(final int whichOverlay) {

        if (mOpenOverlayTimer == null) {

            mWhichOverlay = whichOverlay;

            boolean canShownOverlay = !mSharedPreferences.getBoolean(OVERLAY_ALREADY_SHOWN_PREFERENCES[whichOverlay], false);

            if ((!canShownOverlay) && (whichOverlay == INITIAL_SCREEN)) {

                /*
                   If the print message of the initial screen has already been shown, check if need to show
                   the second message - share: it can only be shown if the user has already gone until the
                   answers screen
                 */

                canShownOverlay = !mSharedPreferences.getBoolean(OVERLAY_ALREADY_SHOWN_PREFERENCES[INITIAL_SCREEN_SHARE], false) &&
                        mSharedPreferences.getBoolean(OVERLAY_ALREADY_SHOWN_PREFERENCES[ANSWERS_SCREEN], false);

                mWhichOverlay = INITIAL_SCREEN_SHARE;
            }

            if (canShownOverlay) {
                mOpenOverlayTimer = new Timer();

                mOpenOverlayTimer.schedule(new TimerTask() {

                    public void run() {
                        createOverlayFragment();

                        mOpenOverlayTimer = null;
                    }
                }, (long) SHOW_OVERLAY_TIMER);
            } else {
                mWhichOverlay = NO_OVERLAY;
            }

        }
    }



    void createOverlayFragment() {
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();

        OverlayFragment fragment = new OverlayFragment();

        fragmentTransaction.setCustomAnimations(R.animator.fragment_slide_up_enter, R.animator.fragment_slide_up_exit,
                R.animator.fragment_slide_up_enter, R.animator.fragment_slide_up_exit);

        int containerViewId = -1;
        int overlayLayoutId = -1;
        int overlayViewId   = -1;

        switch (mWhichOverlay) {
            case INITIAL_SCREEN:
                containerViewId = R.id.overlay_container_start_print;
                overlayLayoutId = R.layout.overlay_start_screen_print;
                overlayViewId   = R.id.overlay_start_screen_print;

                break;

            case INITIAL_SCREEN_SHARE:
                containerViewId = R.id.overlay_container_start_share;
                overlayLayoutId = R.layout.overlay_start_screen_share;
                overlayViewId   = R.id.overlay_start_screen_share;

                break;

            case CAPTURE_SCREEN:
                containerViewId = R.id.overlay_container_capture;
                overlayLayoutId = R.layout.overlay_capture_screen;
                overlayViewId   = R.id.overlay_capture_screen;

                break;

            case ANSWERS_SCREEN:
                containerViewId = R.id.overlay_container_answers;
                overlayLayoutId = R.layout.overlay_answers_screen;
                overlayViewId   = R.id.overlay_answers_screen;

                break;

            case CHART_SCREEN:
                containerViewId = R.id.overlay_container_chart;
                overlayLayoutId = R.layout.overlay_chart_screen;
                overlayViewId   = R.id.overlay_chart_screen;

                break;
        }

        Bundle args = new Bundle();

        args.putInt("overlay", mWhichOverlay);
        args.putInt("layout_id", overlayLayoutId);
        args.putInt("view_id", overlayViewId);

        fragment.setArguments(args);

        fragmentTransaction.add(containerViewId, fragment, OverlayFragment.TAG);
        fragmentTransaction.addToBackStack(OverlayFragment.TAG);

        fragmentTransaction.commit();
    }



    public OverlayManager(Context whichContext, FragmentManager whichFragmentManager) {

        mContext         = whichContext;
        mFragmentManager = whichFragmentManager;

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        mWhichOverlay = NO_OVERLAY;
    }



    public void markOverlayAsShown() {

        if (mWhichOverlay != NO_OVERLAY) {
            SharedPreferences.Editor editor = mSharedPreferences.edit();

            editor.putBoolean(OverlayManager.OVERLAY_ALREADY_SHOWN_PREFERENCES[mWhichOverlay], true);
            editor.commit();
        }
    }



    static void removeFragment(FragmentManager fragmentManager, boolean animate) {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        if (animate) {
            fragmentTransaction.setCustomAnimations(R.animator.fragment_slide_up_enter, R.animator.fragment_slide_up_exit,
                    R.animator.fragment_slide_up_enter, R.animator.fragment_slide_up_exit);
        }

        OverlayFragment fragment = (OverlayFragment) fragmentManager.findFragmentByTag(OverlayFragment.TAG);

        if (fragment != null) {
            fragmentTransaction.remove(fragment);

            fragmentTransaction.commit();

            fragmentManager.popBackStack();
        }
    }



    public void removeOverlay() {
        if (mOpenOverlayTimer != null) {
            mOpenOverlayTimer.cancel();
            mOpenOverlayTimer = null;
        } else {
            removeFragment(mFragmentManager, false);
        }
    }
}
