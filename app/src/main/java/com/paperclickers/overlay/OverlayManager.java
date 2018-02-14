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

    static final int SHOW_OVERLAY_TIMER = 3000;

    public static final int INITIAL_SCREEN   = 0;
    public static final int CAPTURING_SCREEN = 1;
    public static final int ANSWERS_SCREEN   = 2;
    public static final int CHART_SCREEN     = 3;


    private Analytics mAnalytics = null;

    private Context mContext = null;
    private FragmentManager mFragmentManager = null;
    private Timer mOpenOverlayTimer = null;
    private SharedPreferences mSharedPreferences = null;



    public void checkAndTurnOnOverlayTimer(final int whichOverlay) {
        boolean dontShowOverlay = mSharedPreferences.getBoolean("development_dont_show_help", false);

        if (mOpenOverlayTimer == null) {
            if (!dontShowOverlay) {
                mOpenOverlayTimer = new Timer();

                mOpenOverlayTimer.schedule(new TimerTask() {

                    public void run() {
                        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();

                        OverlayFragment fragment = new OverlayFragment();

                        fragmentTransaction.setCustomAnimations(R.animator.fragment_slide_up_enter, R.animator.fragment_slide_up_exit,
                                R.animator.fragment_slide_up_enter, R.animator.fragment_slide_up_exit);

                        int containerViewId = -1;
                        int overlayLayoutId = -1;
                        int overlayViewId   = -1;

                        switch (whichOverlay) {
                            case INITIAL_SCREEN:

                                containerViewId = R.id.overlay_container_start_print;
                                overlayLayoutId = R.layout.overlay_start_screen_print;
                                overlayViewId   = R.id.overlay_start_screen_print;

                                containerViewId = R.id.overlay_container_start_share;
                                overlayLayoutId = R.layout.overlay_start_screen_share;
                                overlayViewId   = R.id.overlay_start_screen_share;

                                break;

                            case CAPTURING_SCREEN:

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

                        args.putInt("overlay", whichOverlay);
                        args.putInt("layout_id", overlayLayoutId);
                        args.putInt("view_id", overlayViewId);

                        fragment.setArguments(args);

                        fragmentTransaction.add(containerViewId, fragment, OverlayFragment.TAG);
                        fragmentTransaction.addToBackStack(OverlayFragment.TAG);

                        fragmentTransaction.commit();

                        mOpenOverlayTimer = null;
                    }
                }, (long) SHOW_OVERLAY_TIMER);
            }
        }
    }



    public OverlayManager(Context whichContext, FragmentManager whichFragmentManager) {

        mContext         = whichContext;
        mFragmentManager = whichFragmentManager;

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
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
