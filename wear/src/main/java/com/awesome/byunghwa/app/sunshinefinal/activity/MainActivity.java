package com.awesome.byunghwa.app.sunshinefinal.activity;

import android.app.Activity;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.GridViewPager;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;

import com.awesome.byunghwa.app.sunshinefinal.R;
import com.awesome.byunghwa.app.sunshinefinal.adapter.SunshineGridPagerAdapter;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private static DataMap map;

    private static SunshineGridPagerAdapter pagerAdapter;

    static Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                Log.i("info", "handler gets called!");
                pagerAdapter.swapMap(map);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Resources res = getResources();
        final GridViewPager pager = (GridViewPager) findViewById(R.id.pager);
        pager.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                // Adjust page margins:
                //   A little extra horizontal spacing between pages looks a bit
                //   less crowded on a round display.
                final boolean round = insets.isRound();
                int rowMargin = res.getDimensionPixelOffset(R.dimen.page_row_margin);
                int colMargin = res.getDimensionPixelOffset(round ?
                        R.dimen.page_column_margin_round : R.dimen.page_column_margin);
                pager.setPageMargins(rowMargin, colMargin);

                // GridViewPager relies on insets to properly handle
                // layout for round displays. They must be explicitly
                // applied since this listener has taken them over.
                pager.onApplyWindowInsets(insets);
                return insets;
            }
        });
        pagerAdapter = new SunshineGridPagerAdapter(this, getFragmentManager(), map);
        pager.setAdapter(pagerAdapter);
        DotsPageIndicator dotsPageIndicator = (DotsPageIndicator) findViewById(R.id.page_indicator);
        dotsPageIndicator.setPager(pager);
    }

    // Normally a service class should be a standalone java class. If it's declared as an inner class,
    // then it should be both public and static, or the android system has no way of instantiating this
    public static class SunshineWeatherWearableListenerService extends WearableListenerService {

        @Override
        public void onDataChanged(DataEventBuffer dataEvents) {
            super.onDataChanged(dataEvents);

            Log.i(TAG, "MainActivity SunshineWeatherWearableListenerService gets called");

            final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
            for(DataEvent event : events) {
                final Uri uri = event.getDataItem().getUri();
                final String path = uri!=null ? uri.getPath() : null;
                if("/WEATHERINFO".equals(path)) {
                    map = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    // post to UI thread to update UI
                    handler.sendEmptyMessage(0);
                }
            }
        }

    }
}
