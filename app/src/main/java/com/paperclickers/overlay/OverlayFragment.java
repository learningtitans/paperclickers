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

import android.app.Fragment;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.paperclickers.Analytics;
import com.paperclickers.R;
import com.paperclickers.log;

/**
 * Created by eduseiti on 13/07/17.
 */

public class OverlayFragment extends Fragment {

    final static String TAG = "OverlayFragment";

    private Analytics mAnalytics = null;



    void buildAnswersScreenOverlay(View whichView) {
        Typeface overlayTypeface = Typeface.createFromAsset(getActivity().getAssets(), "ArchitectsDaughter.ttf");

        TextView line = (TextView) whichView.findViewById(R.id.overlay_return_capture_text);

        line.setTypeface(overlayTypeface);

        line = (TextView) whichView.findViewById(R.id.overlay_scroll_text);

        line.setTypeface(overlayTypeface);

        line = (TextView) whichView.findViewById(R.id.overlay_chart_text);

        line.setTypeface(overlayTypeface);
    }



    void buildCapturingScreenOverlay(View whichView) {

    }



    void buildChartScreenOverlay(View whichView) {

    }



    void buildStartScreenOverlay(View whichView) {
        Typeface overlayTypeface = Typeface.createFromAsset(getActivity().getAssets(), "ArchitectsDaughter.ttf");

        TextView line = (TextView) whichView.findViewById(R.id.overlay_print_text);

        line.setTypeface(overlayTypeface);

        line = (TextView) whichView.findViewById(R.id.overlay_start_text);

        line.setTypeface(overlayTypeface);
    }




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mAnalytics = new Analytics(getActivity().getApplicationContext());

        int whichOverlay         = getArguments().getInt("overlay", 0);
        int whichOverlayLayoutId = getArguments().getInt("layout_id", 0);
        int whichOverlayViewId   = getArguments().getInt("view_id", 0);

        log.d(TAG, String.format("whichOverlay: %d - whichOverlayViewId: %x", whichOverlay, whichOverlayViewId));

        // Inflate the layout for this fragment

        View fragmentView = inflater.inflate(whichOverlayLayoutId, container, false);


        switch (whichOverlay) {
            case OverlayManager.INITIAL_SCREEN:

                buildStartScreenOverlay(fragmentView);

                break;

            case OverlayManager.CAPTURING_SCREEN:

                buildCapturingScreenOverlay(fragmentView);

                break;

            case OverlayManager.ANSWERS_SCREEN:

                buildAnswersScreenOverlay(fragmentView);

                break;

            case OverlayManager.CHART_SCREEN:

                buildChartScreenOverlay(fragmentView);

                break;
        }


        RelativeLayout fragmentLayout = (RelativeLayout) fragmentView.findViewById(whichOverlayViewId);

        fragmentLayout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                OverlayManager.removeFragment(getFragmentManager(), true);
            }
        });


//        CheckBox dontShowOverlay = (CheckBox) fragmentView.findViewById(R.id.checkbox_dont_show_overlay);
//
//        dontShowOverlay.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                if (((CheckBox) v).isChecked()) {
//                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
//
//                    editor.putBoolean("development_dont_show_help", true);
//                    editor.commit();
//
//                    OverlayManager.removeFragment(getFragmentManager(), true);
//
//                    mAnalytics.send_disabledOverlay();
//                }
//            }
//        });

        return fragmentView;
    }
}
