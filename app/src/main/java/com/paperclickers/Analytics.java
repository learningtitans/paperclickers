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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.google.firebase.analytics.FirebaseAnalytics;



/**
 * Created by eduseiti on 18/06/17.
 */

public class Analytics {

    final static String ANALYTICS_ANSWERS_TEXT_STATUS_EVENT = "pce_answers_enter_text_status";
    final static String ANALYTICS_CHANGED_CLASS_SIZE_EVENT  = "pce_changed_class_size";
    final static String ANALYTICS_CLEARED_ANSWERS_LOG_EVENT = "pce_cleared_answers_log";
    final static String ANALYTICS_SHARED_ANSWERS_LOG_EVENT  = "pce_shared_answers_log";

    final static String ANALYTICS_SCAN_CYCLE_EVENT                = "pce_scan_cycle";
    final static String ANALYTICS_SCAN_CYCLE_ALL_STUDENTS_EVENT   = "pce_scan_cycle_all";
    final static String ANALYTICS_RESCAN_CYCLE_EVENT              = "pce_rescan_cycle";
    final static String ANALYTICS_RESCAN_CYCLE_ALL_STUDENTS_EVENT = "pce_rescan_cycle_all";

    final static String ANALYTICS_ENTERED_ANSWERS_EVENT = "pce_entered_answers";

    final static String ANALYTICS_DEBUG_MODE_EVENT = "pce_debug_mode";


    final static String ANALYTICS_TOTAL_TIME_PARAM      = "pcp_total_time";
    final static String ANALYTICS_STUDENTS_NUMBER_PARAM = "pcp_students_num";
    final static String ANALYTICS_BOOLEAN_PARAM         = "pcp_boolean";
    final static String ANALYTICS_ANSWERS_COUNT_PARAM   = "pcp_answers_count";


    private FirebaseAnalytics mFirebaseAnalytics;

    private boolean mAnalyticsEnabled = true;


    public Analytics(Context whichContext) {

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(whichContext);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(whichContext);

        mAnalyticsEnabled = prefs.getString("usage_analytics", "0").equals("0");

        mFirebaseAnalytics.setAnalyticsCollectionEnabled(mAnalyticsEnabled);
    }



    public void send_answersEnterTextStatus(boolean answersTextStatus) {

        if (mAnalyticsEnabled) {
            Bundle bundle = new Bundle();

            bundle.putBoolean(ANALYTICS_BOOLEAN_PARAM, answersTextStatus);

            mFirebaseAnalytics.logEvent(ANALYTICS_ANSWERS_TEXT_STATUS_EVENT, bundle);
        }
    }



    public void send_changedClassSize(int studentsNumber) {

        if (mAnalyticsEnabled) {
            Bundle bundle = new Bundle();

            bundle.putLong(ANALYTICS_STUDENTS_NUMBER_PARAM, studentsNumber);

            mFirebaseAnalytics.logEvent(ANALYTICS_CHANGED_CLASS_SIZE_EVENT, bundle);
        }
    }



    public void send_clearedAnswersLog() {

        if (mAnalyticsEnabled) {
            mFirebaseAnalytics.logEvent(ANALYTICS_CLEARED_ANSWERS_LOG_EVENT, null);
        }
    }



    public void send_debugMode(boolean debugModeStatus) {

        if (mAnalyticsEnabled) {
            Bundle bundle = new Bundle();

            bundle.putBoolean(ANALYTICS_BOOLEAN_PARAM, debugModeStatus);

            mFirebaseAnalytics.logEvent(ANALYTICS_DEBUG_MODE_EVENT, bundle);
        }
    }



    public void send_enteredAnswers(int answersCount) {

        if (mAnalyticsEnabled) {
            Bundle bundle = new Bundle();

            bundle.putLong(ANALYTICS_ANSWERS_COUNT_PARAM, answersCount);

            mFirebaseAnalytics.logEvent(ANALYTICS_ENTERED_ANSWERS_EVENT, bundle);
        }
    }



    public void send_scanCycle(long scanCycleTime, int topCodesFound, int topCodesPreviouslyFound, int studentsNumber) {

        if (mAnalyticsEnabled) {
            Bundle bundle = new Bundle();

            bundle.putLong(ANALYTICS_TOTAL_TIME_PARAM, scanCycleTime);
            bundle.putLong(ANALYTICS_ANSWERS_COUNT_PARAM, topCodesFound);
            bundle.putLong(ANALYTICS_ANSWERS_COUNT_PARAM, topCodesPreviouslyFound);
            bundle.putLong(ANALYTICS_STUDENTS_NUMBER_PARAM, studentsNumber);

            String eventToLog = ANALYTICS_SCAN_CYCLE_EVENT;

            if (topCodesPreviouslyFound > 0) {
                if (topCodesFound == studentsNumber) {
                    eventToLog = ANALYTICS_RESCAN_CYCLE_ALL_STUDENTS_EVENT;
                } else {
                    eventToLog = ANALYTICS_RESCAN_CYCLE_EVENT;
                }
            } else {
                if (topCodesFound == studentsNumber) {
                    eventToLog = ANALYTICS_SCAN_CYCLE_ALL_STUDENTS_EVENT;
                }
            }

            mFirebaseAnalytics.logEvent(eventToLog, bundle);
        }
    }



    public void send_shareAnswersLog() {

        if (mAnalyticsEnabled) {
            mFirebaseAnalytics.logEvent(ANALYTICS_SHARED_ANSWERS_LOG_EVENT, null);
        }
    }
}