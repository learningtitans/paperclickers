/*
 * Paperclickers - Affordable solution for classroom response system.
 *
 * Copyright (C) 2015-2017 Eduardo Valle Jr <dovalle@dca.fee.unicamp.br>
 * Copyright (C) 2015-2017 Eduardo Seiti de Oliveira <eduseiti@dca.fee.unicamp.br>
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

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;



/**
 * Created by eduseiti on 18/06/17.
 */

public class Analytics {

    final static boolean ENABLE_ANALYTICS = true;

    final static String ANALYTICS_SCAN_CYCLE_EVENT = "pce_scan_cycle";
    final static String ANALYTICS_TOTAL_TIME_PARAM = "pcp_total_time";
    final static String ANALYTICS_TOPCODES_COUNT_PARAM = "pcp_topcodes_count";


    private FirebaseAnalytics mFirebaseAnalytics;

    private boolean mAnalyticsEnabled = true;


    public Analytics(Context whichContext) {

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(whichContext);

        mAnalyticsEnabled = ENABLE_ANALYTICS;
    }



    public void send_scanCycle(long scanCycleTime, int topCodesFound) {

        if (mAnalyticsEnabled) {
            Bundle bundle = new Bundle();
            bundle.putLong(ANALYTICS_TOTAL_TIME_PARAM, scanCycleTime);
            bundle.putLong(ANALYTICS_TOPCODES_COUNT_PARAM, topCodesFound);
            mFirebaseAnalytics.logEvent(ANALYTICS_SCAN_CYCLE_EVENT, bundle);
        }
    }
}
