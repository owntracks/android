package org.owntracks.android.support;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import org.owntracks.android.R;

/**
 * Created by Christophe on 12/07/2014.
 * Based on https://github.com/christophesmet/android_maskable_layout
 */

public class MaskingFrameLayout extends FrameLayout {

    //Constants
    private static final String TAG = "MaskingFrameLayout";

    private static final int MODE_ADD = 0;
    private static final int MODE_CLEAR = 1;
    private static final int MODE_DARKEN = 2;
    private static final int MODE_DST = 3;
    private static final int MODE_DST_ATOP = 4;
    private static final int MODE_DST_IN = 5;
    private static final int MODE_DST_OUT = 6;
    private static final int MODE_DST_OVER = 7;
    private static final int MODE_LIGHTEN = 8;
    private static final int MODE_MULTIPLY = 9;
    private static final int MODE_OVERLAY = 10;
    private static final int MODE_SCREEN = 11;
    private static final int MODE_SRC = 12;
    private static final int MODE_SRC_ATOP = 13;
    private static final int MODE_SRC_IN = 14;
    private static final int MODE_SRC_OUT = 15;
    private static final int MODE_SRC_OVER = 16;
    private static final int MODE_XOR = 17;

    private Handler mHandler;

    private Drawable mDrawableMask = null;
    private Bitmap mFinalMask = null;

    //Drawing props
    private Paint mPaint = null;
    private PorterDuffXfermode mPorterDuffXferMode = null;

    public MaskingFrameLayout(Context context) {
        super(context);
    }

    public MaskingFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        construct(context, attrs);
    }

    public MaskingFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        construct(context, attrs);
    }

    private void construct(Context context, AttributeSet attrs) {
        mHandler = new Handler();
        setDrawingCacheEnabled(true);
        if (Build.VERSION.SDK_INT >= 11) {
            setLayerType(LAYER_TYPE_SOFTWARE, null); //Only works for software layers
        }
        mPaint = createPaint();
        Resources.Theme theme = context.getTheme();
        if (theme != null) {
            TypedArray a = theme.obtainStyledAttributes(
                    attrs,
                    R.styleable.MaskableLayout,
                    0, 0);
            try {
                //Load the mask if specified in xml
                initMask(loadMask(a));
                //Load the mode if specified in xml
                mPorterDuffXferMode = getModeFromInteger(
                        a.getInteger(R.styleable.MaskableLayout_porterduffxfermode, 0));

                initMask(mDrawableMask);
            } finally {
                if (a != null) {
                    a.recycle();
                }
            }
        } else {
            log("Couldn't load theme, mask in xml won't be loaded.");
        }
        registerMeasure();
    }

    private Paint createPaint() {
        Paint output = new Paint(Paint.ANTI_ALIAS_FLAG);
        output.setXfermode(mPorterDuffXferMode);
        return output;
    }

    //Mask functions
    private Drawable loadMask(TypedArray a) {
        return a.getDrawable(R.styleable.MaskableLayout_mask);
    }

    private void initMask(Drawable input) {
        if (input != null) {
            mDrawableMask = input;
            if (mDrawableMask instanceof AnimationDrawable) {
                mDrawableMask.setCallback(this);
            }
        } else {
            log("Are you sure you don't want to provide a mask ?");
        }
    }

    public Drawable getDrawableMask() {
        return mDrawableMask;
    }

    private Bitmap makeBitmapMask(Drawable drawable) {
        if (drawable != null) {
            if (getMeasuredWidth() > 0 && getMeasuredHeight() > 0) {
                Bitmap mask = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(),
                        Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(mask);
                drawable.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
                drawable.draw(canvas);
                return mask;
            } else {
                log("Can't create a mask with height 0 or width 0. Or the layout has no children and is wrap content");
                return null;
            }
        } else {
            log("No bitmap mask loaded, view will NOT be masked !");
        }
        return null;
    }

    public void setMask(int drawableRes) {
        Resources res = getResources();
        if (res != null) {
            setMask(res.getDrawable(drawableRes));
        } else {
            log("Unable to load resources, mask will not be loaded as drawable");
        }
    }

    public void setMask(Drawable input) {
        initMask(input);
        swapBitmapMask(makeBitmapMask(mDrawableMask));
        invalidate();
    }

    //Once the size has changed we need to remake the mask.
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setSize(w, h);
    }

    private void setSize(int width, int height) {
        if (width > 0 && height > 0) {
            if (mDrawableMask != null) {
                //Remake the 9patch
                swapBitmapMask(makeBitmapMask(mDrawableMask));
            }
        } else {
            log("Width and height must be higher than 0");
        }
    }

    //Drawing
    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mFinalMask != null && mPaint != null) {
            mPaint.setXfermode(mPorterDuffXferMode);
            canvas.drawBitmap(mFinalMask, 0.0f, 0.0f, mPaint);
            mPaint.setXfermode(null);
        } else {
            log("Mask or paint is null ...");
        }
    }

    //Once inflated we have no height or width for the mask. Wait for the layout.
    private void registerMeasure() {
        final ViewTreeObserver treeObserver = MaskingFrameLayout.this.getViewTreeObserver();
        if (treeObserver != null && treeObserver.isAlive()) {
            treeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    ViewTreeObserver aliveObserver = treeObserver;
                    if (!aliveObserver.isAlive()) {
                        aliveObserver = MaskingFrameLayout.this.getViewTreeObserver();
                    }
                    if (aliveObserver != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            aliveObserver.removeOnGlobalLayoutListener(this);
                        } else {
                            aliveObserver.removeGlobalOnLayoutListener(this);
                        }
                    } else {
                        log("GlobalLayoutListener not removed as ViewTreeObserver is not valid");
                    }
                    swapBitmapMask(makeBitmapMask(mDrawableMask));
                }
            });
        }
    }

    //Logging
    private void log(String message) {
        Log.d(TAG, message);
    }

    //Animation
    @Override
    public void invalidateDrawable(Drawable dr) {
        if (dr != null) {
            initMask(dr);
            swapBitmapMask(makeBitmapMask(dr));
            invalidate();
        }
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        if (who != null && what != null) {
            mHandler.postAtTime(what, when);
        }
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        if (who != null && what != null) {
            mHandler.removeCallbacks(what);
        }
    }

    private void swapBitmapMask(Bitmap newMask) {
        if (newMask != null) {
            if (mFinalMask != null && !mFinalMask.isRecycled()) {
                mFinalMask.recycle();
            }
            mFinalMask = newMask;
        }
    }

    //Utils
    private PorterDuffXfermode getModeFromInteger(int index) {
        PorterDuff.Mode mode = null;
        switch (index) {
            case MODE_ADD:
                if (Build.VERSION.SDK_INT >= 11) {
                    mode = PorterDuff.Mode.ADD;
                } else {
                    log("MODE_ADD is not supported on api lvl " + Build.VERSION.SDK_INT);
                }
            case MODE_CLEAR:
                mode = PorterDuff.Mode.CLEAR;
                break;
            case MODE_DARKEN:
                mode = PorterDuff.Mode.DARKEN;
                break;
            case MODE_DST:
                mode = PorterDuff.Mode.DST;
                break;
            case MODE_DST_ATOP:
                mode = PorterDuff.Mode.DST_ATOP;
                break;
            case MODE_DST_IN:
                mode = PorterDuff.Mode.DST_IN;
                break;
            case MODE_DST_OUT:
                mode = PorterDuff.Mode.DST_OUT;
                break;
            case MODE_DST_OVER:
                mode = PorterDuff.Mode.DST_OVER;
                break;
            case MODE_LIGHTEN:
                mode = PorterDuff.Mode.LIGHTEN;
                break;
            case MODE_MULTIPLY:
                mode = PorterDuff.Mode.MULTIPLY;
                break;
            case MODE_OVERLAY:
                if (Build.VERSION.SDK_INT >= 11) {
                    mode = PorterDuff.Mode.OVERLAY;
                } else {
                    log("MODE_OVERLAY is not supported on api lvl " + Build.VERSION.SDK_INT);
                }
            case MODE_SCREEN:
                mode = PorterDuff.Mode.SCREEN;
                break;
            case MODE_SRC:
                mode = PorterDuff.Mode.SRC;
                break;
            case MODE_SRC_ATOP:
                mode = PorterDuff.Mode.SRC_ATOP;
                break;
            case MODE_SRC_IN:
                mode = PorterDuff.Mode.SRC_IN;
                break;
            case MODE_SRC_OUT:
                mode = PorterDuff.Mode.SRC_OUT;
                break;
            case MODE_SRC_OVER:
                mode = PorterDuff.Mode.SRC_OVER;
                break;
            case MODE_XOR:
                mode = PorterDuff.Mode.XOR;
                break;
            default:
                mode = PorterDuff.Mode.DST_IN;
        }
        log("Mode is " + mode.toString());
        return new PorterDuffXfermode(mode);
    }
}
