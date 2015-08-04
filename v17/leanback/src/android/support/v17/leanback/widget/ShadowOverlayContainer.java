/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.widget;

import android.content.Context;
import android.support.annotation.ColorInt;
import android.support.v17.leanback.R;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

/**
 * Provides an SDK version-independent wrapper to support shadows, color overlays, and rounded
 * corners.
 * <p>
 * {@link #prepareParentForShadow(ViewGroup)} must be called on parent of container
 * before using shadow.  Depending on sdk version, optical bounds might be applied
 * to parent.
 * </p>
 * <p>
 * If shadows can appear outside the bounds of the parent view, setClipChildren(false) must
 * be called on the grandparent view.
 * </p>
 * <p>
 * {@link #initialize(boolean, boolean, boolean)} must be first called on the container.
 * Then call {@link #wrap(View)} to insert the wrapped view into the container.
 * </p>
 * <p>
 * Call {@link #setShadowFocusLevel(float)} to control the strength of the shadow (focused shadows
 * cast stronger shadows).
 * </p>
 * <p>
 * Call {@link #setOverlayColor(int)} to control overlay color.
 * </p>
 */
public class ShadowOverlayContainer extends ViewGroup {

    /**
     * No shadow.
     */
    public static final int SHADOW_NONE = ShadowOverlayHelper.SHADOW_NONE;

    /**
     * Shadows are fixed.
     */
    public static final int SHADOW_STATIC = ShadowOverlayHelper.SHADOW_STATIC;

    /**
     * Shadows depend on the size, shape, and position of the view.
     */
    public static final int SHADOW_DYNAMIC = ShadowOverlayHelper.SHADOW_DYNAMIC;

    private boolean mInitialized;
    private Object mShadowImpl;
    private View mWrappedView;
    private boolean mRoundedCorners;
    private int mShadowType = SHADOW_NONE;
    private float mUnfocusedZ;
    private float mFocusedZ;
    private static final Rect sTempRect = new Rect();
    private Paint mOverlayPaint;
    private int mOverlayColor;

    /**
     * Create ShadowOverlayContainer and auto select shadow type.
     */
    public ShadowOverlayContainer(Context context) {
        this(context, null, 0);
    }

    /**
     * Create ShadowOverlayContainer and auto select shadow type.
     */
    public ShadowOverlayContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Create ShadowOverlayContainer and auto select shadow type.
     */
    public ShadowOverlayContainer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        useStaticShadow();
        useDynamicShadow();
    }

    /**
     * Create ShadowOverlayContainer with specific shadowType.
     */
    public ShadowOverlayContainer(Context context,
            int shadowType, boolean hasColorDimOverlay, boolean roundedCorners) {
        super(context);
        mUnfocusedZ = getResources().getDimension(R.dimen.lb_material_shadow_normal_z);
        mFocusedZ = getResources().getDimension(R.dimen.lb_material_shadow_focused_z);
        initialize(shadowType, hasColorDimOverlay, roundedCorners);
    }

    /**
     * Return true if the platform sdk supports shadow.
     */
    public static boolean supportsShadow() {
        return StaticShadowHelper.getInstance().supportsShadow();
    }

    /**
     * Returns true if the platform sdk supports dynamic shadows.
     */
    public static boolean supportsDynamicShadow() {
        return ShadowHelper.getInstance().supportsDynamicShadow();
    }

    /**
     * {@link #prepareParentForShadow(ViewGroup)} must be called on parent of container
     * before using shadow.  Depending on sdk version, optical bounds might be applied
     * to parent.
     */
    public static void prepareParentForShadow(ViewGroup parent) {
        StaticShadowHelper.getInstance().prepareParent(parent);
    }

    /**
     * Sets the shadow type to {@link #SHADOW_DYNAMIC} if supported.
     */
    public void useDynamicShadow() {
        useDynamicShadow(getResources().getDimension(R.dimen.lb_material_shadow_normal_z),
                getResources().getDimension(R.dimen.lb_material_shadow_focused_z));
    }

    /**
     * Sets the shadow type to {@link #SHADOW_DYNAMIC} if supported and sets the elevation/Z
     * values to the given parameteres.
     */
    public void useDynamicShadow(float unfocusedZ, float focusedZ) {
        if (mInitialized) {
            throw new IllegalStateException("Already initialized");
        }
        if (supportsDynamicShadow()) {
            mShadowType = SHADOW_DYNAMIC;
            mUnfocusedZ = unfocusedZ;
            mFocusedZ = focusedZ;
        }
    }

    /**
     * Sets the shadow type to {@link #SHADOW_STATIC} if supported.
     */
    public void useStaticShadow() {
        if (mInitialized) {
            throw new IllegalStateException("Already initialized");
        }
        if (supportsShadow()) {
            mShadowType = SHADOW_STATIC;
        }
    }

    /**
     * Returns the shadow type, one of {@link #SHADOW_NONE}, {@link #SHADOW_STATIC}, or
     * {@link #SHADOW_DYNAMIC}.
     */
    public int getShadowType() {
        return mShadowType;
    }

    /**
     * Initialize shadows, color overlay.
     * @deprecated use {@link #initialize(boolean, boolean, boolean)} instead.
     */
    @Deprecated
    public void initialize(boolean hasShadow, boolean hasColorDimOverlay) {
        initialize(hasShadow, hasColorDimOverlay, true);
    }

    /**
     * Initialize shadows, color overlay, and rounded corners.  All are optional.
     * Shadow type are auto-selected based on {@link #useStaticShadow()} and
     * {@link #useDynamicShadow()} call.
     * @deprecated use {@link #initialize(int, boolean, boolean)} instead.
     */
    @Deprecated
    public void initialize(boolean hasShadow, boolean hasColorDimOverlay, boolean roundedCorners) {
        int shadowType;
        if (!hasShadow) {
            shadowType = SHADOW_NONE;
        } else {
            shadowType = mShadowType;
        }
        initialize(shadowType, hasColorDimOverlay, roundedCorners);
    }

    /**
     * Initialize shadows, color overlay, and rounded corners.  All are optional.
     */
    public void initialize(int shadowType, boolean hasColorDimOverlay, boolean roundedCorners) {
        if (mInitialized) {
            throw new IllegalStateException();
        }
        mInitialized = true;
        mShadowType = shadowType;
        switch (mShadowType) {
            case SHADOW_DYNAMIC:
                mShadowImpl = ShadowHelper.getInstance().addDynamicShadow(
                        this, mUnfocusedZ, mFocusedZ, roundedCorners);
                break;
            case SHADOW_STATIC:
                mShadowImpl = StaticShadowHelper.getInstance().addStaticShadow(this);
                break;
        }
        mRoundedCorners = roundedCorners;
        if (hasColorDimOverlay) {
            setWillNotDraw(false);
            mOverlayColor = Color.TRANSPARENT;
            mOverlayPaint = new Paint();
            mOverlayPaint.setColor(mOverlayColor);
            mOverlayPaint.setStyle(Paint.Style.FILL);
        } else {
            setWillNotDraw(true);
            mOverlayPaint = null;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (mOverlayPaint != null && mOverlayColor != Color.TRANSPARENT) {
            canvas.drawRect(mWrappedView.getLeft(), mWrappedView.getTop(),
                    mWrappedView.getRight(), mWrappedView.getBottom(),
                    mOverlayPaint);
        }
    }

    /**
     * Set shadow focus level (0 to 1). 0 for unfocused, 1f for fully focused.
     */
    public void setShadowFocusLevel(float level) {
        if (mShadowImpl != null) {
            ShadowOverlayHelper.setShadowFocusLevel(mShadowImpl, mShadowType, level);
        }
    }

    /**
     * Set color (with alpha) of the overlay.
     */
    public void setOverlayColor(@ColorInt int overlayColor) {
        if (mOverlayPaint != null) {
            if (overlayColor != mOverlayColor) {
                mOverlayColor = overlayColor;
                mOverlayPaint.setColor(overlayColor);
                invalidate();
            }
        }
    }

    /**
     * Inserts view into the wrapper.
     */
    public void wrap(View view) {
        if (!mInitialized || mWrappedView != null) {
            throw new IllegalStateException();
        }
        addView(view);
        if (mRoundedCorners && mShadowType == SHADOW_STATIC) {
            RoundedRectHelper.getInstance().setClipToRoundedOutline(view, true);
        }
        mWrappedView = view;
    }

    /**
     * Returns the wrapper view.
     */
    public View getWrappedView() {
        return mWrappedView;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mWrappedView == null) {
            throw new IllegalStateException();
        }
        // padding and child margin are not supported.
        // first measure the wrapped view, then measure the shadow view and/or overlay view.
        int childWidthMeasureSpec, childHeightMeasureSpec;
        LayoutParams lp = mWrappedView.getLayoutParams();
        if (lp.width == LayoutParams.MATCH_PARENT) {
            childWidthMeasureSpec = MeasureSpec.makeMeasureSpec
                    (MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY);
        } else {
            childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, 0, lp.width);
        }
        if (lp.height == LayoutParams.MATCH_PARENT) {
            childHeightMeasureSpec = MeasureSpec.makeMeasureSpec
                    (MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.EXACTLY);
        } else {
            childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, 0, lp.height);
        }
        mWrappedView.measure(childWidthMeasureSpec, childHeightMeasureSpec);

        int measuredWidth = mWrappedView.getMeasuredWidth();
        int measuredHeight = mWrappedView.getMeasuredHeight();

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child == mWrappedView) {
                continue;
            }
            lp = child.getLayoutParams();
            if (lp.width == LayoutParams.MATCH_PARENT) {
                childWidthMeasureSpec = MeasureSpec.makeMeasureSpec
                        (measuredWidth, MeasureSpec.EXACTLY);
            } else {
                childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, 0, lp.width);
            }

            if (lp.height == LayoutParams.MATCH_PARENT) {
                childHeightMeasureSpec = MeasureSpec.makeMeasureSpec
                        (measuredHeight, MeasureSpec.EXACTLY);
            } else {
                childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, 0, lp.height);
            }
            child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
        }
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final int width = child.getMeasuredWidth();
                final int height = child.getMeasuredHeight();
                child.layout(0, 0, width, height);
            }
        }
        if (mWrappedView != null) {
            sTempRect.left = (int) mWrappedView.getPivotX();
            sTempRect.top = (int) mWrappedView.getPivotY();
            offsetDescendantRectToMyCoords(mWrappedView, sTempRect);
            setPivotX(sTempRect.left);
            setPivotY(sTempRect.top);
        }
    }

}
