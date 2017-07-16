package com.paperclickers;

import android.app.Fragment;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by eduseiti on 13/07/17.
 */

public class OverlayFragment extends Fragment {

    final static String TAG = "overlayFragment";



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment

        View fragmentView = inflater.inflate(R.layout.start_screen_overlay_fragment, container, false);

        TextView line = (TextView) fragmentView.findViewById(R.id.overlay_line_1);

        line.setText("Access settings \u2192");

        Typeface overlayTypeface = Typeface.createFromAsset(getActivity().getAssets(), "ArchitectsDaughter.ttf");

        line.setTypeface(overlayTypeface);

        line = (TextView) fragmentView.findViewById(R.id.overlay_line_2);

        line.setText("To print students' codes,\nshare answers and more...");

        line.setTypeface(overlayTypeface);

        return fragmentView;
    }
}
