package com.android.keyguard;

import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

/**
 * Pure Black & White OLED Lockscreen badge mirroring DumbOS mockup ("Mode: Normal").
 * Reads Settings.System "dumbos_mode" with zero external R or Dagger dependencies.
 */
public class DumbModeBadgeView extends TextView {

    private static final String SETTINGS_KEY_MODE = "dumbos_mode";
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final ContentObserver mObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            refreshModeLabel();
        }
    };

    public DumbModeBadgeView(Context context) {
        this(context, null);
    }

    public DumbModeBadgeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DumbModeBadgeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initStyle(context);
    }

    private void initStyle(Context context) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#0B0B0B"));
        bg.setStroke(dp(context, 1), Color.parseColor("#262626"));
        bg.setCornerRadius(dp(context, 6));
        setBackground(bg);
        setPadding(dp(context, 8), dp(context, 3), dp(context, 8), dp(context, 3));
        setTextColor(Color.parseColor("#B0B0B0"));
        setMaxLines(1);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        try {
            getContext().getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(SETTINGS_KEY_MODE), false, mObserver);
        } catch (Throwable ignored) {
        }
        refreshModeLabel();
    }

    @Override
    protected void onDetachedFromWindow() {
        try {
            getContext().getContentResolver().unregisterContentObserver(mObserver);
        } catch (Throwable ignored) {
        }
        super.onDetachedFromWindow();
    }

    private void refreshModeLabel() {
        String mode = null;
        try {
            mode = Settings.System.getString(getContext().getContentResolver(), SETTINGS_KEY_MODE);
        } catch (Throwable ignored) {
        }
        if (mode == null || mode.isEmpty()) {
            mode = "Normal";
        }
        setText("Mode: " + mode);
    }

    private static int dp(Context ctx, int val) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, val, ctx.getResources().getDisplayMetrics()));
    }
}
