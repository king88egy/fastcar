package com.fastcar.racing;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.fastcar.common.L10n;

public class PrivacyActivity extends Activity {

    public static final String EXTRA_DOC = "doc";
    public static final String DOC_PRIVACY = "privacy";
    public static final String DOC_TERMS = "terms";

    private TextView status;
    private TextView body;
    private Button btnPrivacy;
    private Button btnTerms;
    private String lang;
    private String current;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Sfx.init(this);
        lang = Api.getLang(this);
        current = getIntent().getStringExtra(EXTRA_DOC);
        if (current == null) current = DOC_PRIVACY;

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(16, 19, 26));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(20), dp(24), dp(20), dp(16));

        TextView title = new TextView(this);
        title.setText(t("privacy") + " / " + t("terms"));
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.rgb(255, 213, 79));
        title.setGravity(Gravity.CENTER);
        col.addView(title);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        btnPrivacy = tab(1f);
        btnTerms = tab(1f);
        tabs.addView(btnPrivacy);
        tabs.addView(btnTerms);
        col.addView(tabs);

        status = new TextView(this);
        status.setText(t(L10n.I_LOADING) + "...");
        status.setTextSize(13);
        status.setTextColor(Color.rgb(169, 199, 255));
        status.setPadding(0, dp(8), 0, dp(6));
        col.addView(status);

        btnPrivacy.setText("" + t("privacy"));
        btnTerms.setText(t("terms"));
        btnPrivacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sfx.play("click");
                load(DOC_PRIVACY);
            }
        });
        btnTerms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sfx.play("click");
                load(DOC_TERMS);
            }
        });

        body = new TextView(this);
        body.setTextSize(15);
        body.setTextColor(Color.WHITE);
        body.setLineSpacing(0, 1.15f);
        body.setPadding(0, dp(6), 0, dp(10));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(body, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        col.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        Button back = new Button(this);
        back.setText(t(L10n.I_HOME));
        back.setAllCaps(false);
        back.setTextColor(Color.WHITE);
        back.setBackgroundColor(Color.rgb(40, 52, 74));
        back.setMinHeight(dp(54));
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sfx.play("click");
                finish();
            }
        });
        col.addView(back);

        root.addView(col);
        setContentView(root);
        load(current);
    }

    private Button tab(float w) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setMinHeight(dp(50));
        b.setPadding(dp(4), dp(8), dp(4), dp(8));
        b.setTextColor(Color.rgb(216, 230, 255));
        b.setBackgroundColor(Color.rgb(40, 52, 74));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, w);
        lp.setMargins(0, dp(8), dp(4), 0);
        b.setLayoutParams(lp);
        return b;
    }

    private void load(final String doc) {
        current = doc;
        String text = doc.equals(DOC_TERMS) ? Docs.terms(lang) : Docs.privacy(lang);
        body.setText(text == null ? "" : text);
        btnPrivacy.setTextColor(doc.equals(DOC_PRIVACY) ? Color.rgb(255, 213, 79) : Color.rgb(216, 230, 255));
        btnTerms.setTextColor(doc.equals(DOC_TERMS) ? Color.rgb(255, 213, 79) : Color.rgb(216, 230, 255));
        status.setText(t("privacy") + " / " + t("terms"));
    }

    private String t(int key) {
        return L10n.get(lang, key);
    }

    private String t(String key) {
        return L10n.get(lang, key);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}