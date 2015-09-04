/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.awesome.byunghwa.app.sunshinefinal.adapter;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.support.wearable.view.CardFragment;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.util.Log;

import com.awesome.byunghwa.app.sunshinefinal.R;
import com.google.android.gms.wearable.DataMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Constructs fragments as requested by the GridViewPager. For each row a different background is
 * provided.
 * <p>
 * Always avoid loading resources from the main thread. In this sample, the background images are
 * loaded from an background task and then updated using {@link #notifyRowBackgroundChanged(int)}
 * and {@link #notifyPageBackgroundChanged(int, int)}.
 */
public class SunshineGridPagerAdapter extends FragmentGridPagerAdapter {
    private static final int TRANSITION_DURATION_MILLIS = 100;

    private final Context mContext;

    private List<Row> mRows;
    private ColorDrawable mDefaultBg;

    private static boolean isMetric;

    private DataMap map;

    public SunshineGridPagerAdapter(Context ctx, FragmentManager fm, DataMap dataMap) {
        super(fm);
        mContext = ctx;

        this.map = dataMap;

        mRows = new ArrayList<>();

        // initialize wear cards
        if (map != null) {
            int cursorRowCount = map.getInt("cursor_row_count");
            isMetric = map.getBoolean("isMetric");

            for (int i=0;i< cursorRowCount;i++) {
                int weatherConditionId = map.getInt("weatherId" + i);
                String highTemp = formatTemperature(mContext, map.getDouble("high_temp" + i));
                String lowTemp = formatTemperature(mContext, map.getDouble("low_temp" + i));

                String weatherDescription = map.getString("description" + i);
                String dateString = map.getString("dateString" + i);

                BG_IMAGES.add(i, getArtResourceForWeatherCondition(weatherConditionId));

                mRows.add(new Row(cardFragment(dateString, highTemp + " " + lowTemp + "  " + weatherDescription)));
            }
        }

        mDefaultBg = new ColorDrawable(mContext.getResources().getColor(R.color.white));
    }


    LruCache<Integer, Drawable> mRowBackgrounds = new LruCache<Integer, Drawable>(3) {
        @Override
        protected Drawable create(final Integer row) {
            int resid = BG_IMAGES.get(row);
            new DrawableLoadingTask(mContext) {
                @Override
                protected void onPostExecute(Drawable result) {
                    TransitionDrawable background = new TransitionDrawable(new Drawable[] {
                            mDefaultBg,
                            result
                    });
                    mRowBackgrounds.put(row, background);
                    notifyRowBackgroundChanged(row);
                    background.startTransition(TRANSITION_DURATION_MILLIS);
                }
            }.execute(resid);
            return mDefaultBg;
        }
    };

    public static String formatTemperature(Context context, double temperature) {
        // For presentation, assume the user doesn't care about tenths of a degree.
        if (!isMetric) {
            temperature = (temperature * 1.8) + 32;
        }
        return String.format(context.getString(R.string.format_temperature), temperature);
    }

    private Fragment cardFragment(String titleString, String descriptionString) {
        Resources res = mContext.getResources();
        CardFragment fragment =
                CardFragment.create(titleString, descriptionString);
        // Add some extra bottom margin to leave room for the page indicator
        fragment.setCardMarginBottom(
                res.getDimensionPixelSize(R.dimen.card_margin_bottom));
        return fragment;
    }

    /**
     * Helper method to provide the art resource id according to the weather condition id returned
     * by the OpenWeatherMap call.
     * @param weatherId from OpenWeatherMap API response
     * @return resource id for the corresponding icon. -1 if no relation is found.
     */
    public static int getArtResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.rain;
        } else if (weatherId == 511) {
            return R.drawable.snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.storm;
        } else if (weatherId == 800) {
            return R.drawable.clear;
        } else if (weatherId == 801) {
            return R.drawable.light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.clouds;
        }
        return -1;
    }

    static final ArrayList<Integer> BG_IMAGES = new ArrayList<>();

    /** A convenient container for a row of fragments. */
    private class Row {
        final List<Fragment> columns = new ArrayList<>();

        public Row(Fragment... fragments) {
            for (Fragment f : fragments) {
                add(f);
            }
        }

        public void add(Fragment f) {
            columns.add(f);
        }

        Fragment getColumn(int i) {
            return columns.get(i);
        }

        public int getColumnCount() {
            return columns.size();
        }
    }

    @Override
    public Fragment getFragment(int row, int col) {
        Log.i("info", "getFragment gets called!");

        isMetric = map.getBoolean("isMetric");

        int weatherConditionId = map.getInt("weatherId" + row);

        BG_IMAGES.add(row, getArtResourceForWeatherCondition(weatherConditionId));

        String highTemp = formatTemperature(mContext, map.getDouble("high_temp" + row));
        String lowTemp = formatTemperature(mContext, map.getDouble("low_temp" + row));

        String weatherDescription = map.getString("description" + row);
        String dateString = map.getString("dateString" + row);

        getBackgroundForRow(row);

        return CardFragment.create(dateString, highTemp + " " + lowTemp + "  " + weatherDescription);
    }

    @Override
    public Drawable getBackgroundForRow(final int row) {
        return mRowBackgrounds.get(row);
    }

    @Override
    public int getRowCount() {
        return mRows.size();
    }

    @Override
    public int getColumnCount(int rowNum) {
        return mRows.get(rowNum).getColumnCount();
    }

    class DrawableLoadingTask extends AsyncTask<Integer, Void, Drawable> {
        private static final String TAG = "Loader";
        private Context context;

        DrawableLoadingTask(Context context) {
            this.context = context;
        }

        @Override
        protected Drawable doInBackground(Integer... params) {
            Log.d(TAG, "Loading asset 0x" + Integer.toHexString(params[0]));
            return context.getResources().getDrawable(params[0]);
        }
    }

    public void swapMap(DataMap dataMap) {
        Log.i("info", "swap map gets called");
        this.map = dataMap;

        // clear background image resource path cache
        // THIS IS VERY IMPORTANT!
        mRowBackgrounds.evictAll();

        notifyDataSetChanged();
    }
}
