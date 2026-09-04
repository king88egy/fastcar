package com.fastcar.racing;

import android.app.Activity;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public final class Ads {

    public static final String APP_ID = "ca-app-pub-3940256099942544~3347511713";
    public static final String BANNER_ID = "ca-app-pub-3940256099942544/6300978111";
    public static final String REWARDED_ID = "ca-app-pub-3940256099942544/5224354917";

    private static RewardedAd rewarded;
    private static boolean loading = false;

    private Ads() {
    }

    public static void init(Context c) {
        MobileAds.initialize(c, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
    }

    public static void preload(final Context c) {
        if (loading) return;
        loading = true;
        try {
            RewardedAd.load(c, REWARDED_ID, new AdRequest.Builder().build(),
                    new RewardedAdLoadCallback() {
                        @Override
                        public void onAdLoaded(RewardedAd ad) {
                            rewarded = ad;
                            loading = false;
                        }

                        @Override
                        public void onAdFailedToLoad(LoadAdError e) {
                            loading = false;
                        }
                    });
        } catch (Throwable t) {
            loading = false;
        }
    }

    public static boolean rewardedReady() {
        return rewarded != null;
    }

    public static void showRewarded(final Activity a, final Runnable onEarned) {
        if (rewarded == null) {
            preload(a);
            if (onEarned != null) onEarned.run();
            return;
        }
        final RewardedAd ad = rewarded;
        rewarded = null;
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                preload(a);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError e) {
                preload(a);
            }
        });
        ad.show(a, new OnUserEarnedRewardListener() {
            @Override
            public void onUserEarnedReward(com.google.android.gms.ads.rewarded.RewardItem rewardItem) {
                if (onEarned != null) onEarned.run();
            }
        });
    }

    public static void addBanner(Activity a, FrameLayout root, int bottomMarginDp) {
        try {
            final AdView ad = new AdView(a);
            ad.setAdUnitId(BANNER_ID);
            ad.setAdSize(AdSize.BANNER);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL;
            int m = dp(a, bottomMarginDp);
            lp.setMargins(0, 0, 0, m);
            root.addView(ad, lp);
            ad.loadAd(new AdRequest.Builder().build());
        } catch (Throwable ignored) {
        }
    }

    private static int dp(Context c, int v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }
}