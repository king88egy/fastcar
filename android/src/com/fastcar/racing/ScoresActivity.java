package com.fastcar.racing;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.fastcar.common.L10n;

public class ScoresActivity extends Activity {

    private LinearLayout list;
    private TextView status;
    private String lang;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Sfx.init(this);
        lang = Api.getLang(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(16, 19, 26));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(28), dp(22), dp(22));
        scroll.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("" + t(L10n.I_LEADERBOARD));
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.rgb(255, 213, 79));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView hint = new TextView(this);
        hint.setText(t(L10n.I_RESULT) + " " + Api.getServer(this));
        hint.setTextSize(13);
        hint.setGravity(Gravity.CENTER);
        hint.setTextColor(Color.rgb(169, 199, 255));
        root.addView(hint);

        status = new TextView(this);
        status.setText(t(L10n.I_LOADING) + "...");
        status.setTextSize(14);
        status.setGravity(Gravity.CENTER);
        status.setTextColor(Color.rgb(169, 199, 255));
        status.setPadding(0, dp(14), 0, dp(14));
        root.addView(status);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);

        Button refresh = new Button(this);
        refresh.setText("" + t(L10n.I_AGAIN));
        refresh.setAllCaps(false);
        refresh.setTextSize(16);
        refresh.setTextColor(Color.WHITE);
        refresh.setBackgroundColor(Color.rgb(76, 161, 175));
        refresh.setMinHeight(dp(54));
        refresh.setPadding(0, dp(12), 0, dp(12));
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sfx.play("click");
                load();
            }
        });
        root.addView(refresh, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(scroll);
        load();
    }

    private void load() {
        status.setText(t(L10n.I_LOADING) + "...");
        status.setTextColor(Color.rgb(169, 199, 255));
        list.removeAllViews();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String res = Api.topScores(ScoresActivity.this);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            render(res);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            status.setText(t(L10n.I_NET_ERR) + ": " + e.getMessage());
                            status.setTextColor(Color.rgb(229, 110, 81));
                        }
                    });
                }
            }
        }).start();
    }

    private void render(String res) {
        list.removeAllViews();
        if (res == null || res.trim().isEmpty()) {
            status.setText(t(L10n.I_RESULT));
            status.setTextColor(Color.rgb(169, 199, 255));
            return;
        }
        status.setText(t(L10n.I_LEADERBOARD));
        status.setTextColor(Color.rgb(123, 211, 137));
        String[] lines = res.trim().split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int p = line.lastIndexOf('|');
            String name = p >= 0 ? line.substring(0, p) : line;
            String score = p >= 0 ? line.substring(p + 1) : "";
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(10), dp(12), dp(10));

            TextView rank = new TextView(this);
            rank.setText("#" + (i + 1));
            rank.setTextSize(16);
            rank.setTypeface(Typeface.DEFAULT_BOLD);
            rank.setTextColor(Color.rgb(255, 213, 79));
            rank.setMinWidth(dp(46));
            row.addView(rank);

            TextView nm = new TextView(this);
            nm.setText(name);
            nm.setTextSize(16);
            nm.setTextColor(Color.WHITE);
            nm.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(nm);

            TextView sc = new TextView(this);
            sc.setText(score);
            sc.setTextSize(18);
            sc.setTypeface(Typeface.DEFAULT_BOLD);
            sc.setTextColor(Color.rgb(123, 211, 137));
            row.addView(sc);

            row.setContentDescription("#" + (i + 1) + " " + name + " " + t(L10n.I_POINTS) + " " + score);
            list.addView(row);
        }
    }

    private String t(int key) {
        return L10n.get(lang, key);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}