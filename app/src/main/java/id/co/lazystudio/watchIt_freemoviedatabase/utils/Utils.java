package id.co.lazystudio.watchIt_freemoviedatabase.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.balysv.materialripple.MaterialRippleLayout;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.util.List;

import id.co.lazystudio.watchIt_freemoviedatabase.BuildConfig;
import id.co.lazystudio.watchIt_freemoviedatabase.R;

/**
 * Created by faqiharifian on 25/09/16.
 */
public class Utils {
    public static boolean isInternetConnected(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        return activeNetwork != null && activeNetwork.isConnected();
    }

    public static void setProcessComplete(View view){
        view.setVisibility(View.GONE);
    }

    public static void setProcessComplete(View view, List<String> processList, String process){
        processList.remove(process);
        if(processList.isEmpty())
            setProcessComplete(view);
    }

    public static void setProcessError(Context context, View view, int type){
        String text;
        TextView textView = (TextView) view;
        switch (type){
            case 400:
                text = context.getResources().getString(R.string.error_server_error);
                break;
            case 200:
                text = context.getResources().getString(R.string.error_not_found);
                textView.setBackgroundColor(Color.TRANSPARENT);
                textView.setTextColor(Utils.getColorWithAlpha(0.87f, Color.BLACK));
                break;
            default:
                text = context.getResources().getString(R.string.error_no_internet);
                break;
        }
        if(view != null){
            view.setVisibility(View.VISIBLE);
            ((TextView) view).setText(text);
        }
        Log.e("connection error", text);
    }


    public static void initializeAd(Activity context, View view){
        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(context, BuildConfig.ADMOB_APP_ID);

        // Gets the ad view defined in layout/ad_fragment.xml with ad unit ID set in
        // values/strings.xml.
        AdView mAdView = (AdView) context.findViewById(R.id.ad_view);

        // Create an ad request. Check your logcat output for the hashed device ID to
        // get test ads on a physical device. e.g.
        // "Use AdRequest.Builder.addTestDevice("ABCDEF012345") to get test ads on this device."
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addKeyword("movie")
                .addKeyword("film")
                .addKeyword("image")
                .addKeyword("picture")
                .addKeyword("theater")
                .addKeyword("video")
                .addKeyword("music")
                .addKeyword("audio")
                .addKeyword("media")
                .addKeyword("player")
                .addKeyword("game")
                .addKeyword("record")
                .build();

        // Start loading the ad in the background.
        mAdView.loadAd(adRequest);

//        int paddingBottom = AdSize.SMART_BANNER.getHeightInPixels(context);
//        view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), paddingBottom);
    }

    public static int getColorWithAlpha(float alpha, int baseColor) {
        int a = Math.min(255, Math.max(0, (int) (alpha * 255))) << 24;
        int rgb = 0x00ffffff & baseColor;
        return a + rgb;
    }

    public static void createRipple(View view){
        MaterialRippleLayout.on(view)
                .rippleOverlay(true)
                .rippleColor(Color.BLACK)
                .rippleAlpha((float)0.25)
                .rippleDelayClick(true)
                .rippleDuration(250)
                .create();
    }
}
