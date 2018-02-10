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

package com.paperclickers;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
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

/**
 * Created by eduseiti on 13/07/17.
 */

public class OverlayFragment extends Fragment {

    final static String TAG = "OverlayFragment";

    private Analytics mAnalytics = null;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mAnalytics = new Analytics(getActivity().getApplicationContext());

        // Inflate the layout for this fragment

        View fragmentView = inflater.inflate(R.layout.start_screen_overlay_fragment, container, false);


        // Add text for the start screen overlay help

        TextView line = (TextView) fragmentView.findViewById(R.id.overlay_line_1);

        line.setText(getResources().getText(R.string.start_overlay_settings_icon));

        Typeface overlayTypeface = Typeface.createFromAsset(getActivity().getAssets(), "ArchitectsDaughter.ttf");

        line.setTypeface(overlayTypeface);

        line = (TextView) fragmentView.findViewById(R.id.overlay_line_2);

        line.setText(getResources().getText(R.string.start_overlay_settings_text));

        line.setTypeface(overlayTypeface);

        line = (TextView) fragmentView.findViewById(R.id.overlay_dont_show_anymore);

        line.setText(getResources().getText(R.string.start_overlay_dont_show_anymore));

        line.setTypeface(overlayTypeface);


        RelativeLayout fragmentLayout = (RelativeLayout) fragmentView.findViewById(R.id.overlay_main_view);

        fragmentLayout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                removeFragment(getFragmentManager(), true);
            }
        });


        CheckBox dontShowOverlay = (CheckBox) fragmentView.findViewById(R.id.checkbox_dont_show_overlay);

        dontShowOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (((CheckBox) v).isChecked()) {
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();

                    editor.putBoolean("development_dont_show_help", true);
                    editor.commit();

                    removeFragment(getFragmentManager(), true);

                    mAnalytics.send_disabledOverlay();
                }
            }
        });

        return fragmentView;
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
}
