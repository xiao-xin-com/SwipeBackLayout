package com.github.vipulasri.timelineview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;

/**
 * Created by Vipul Asri on 05-12-2015.
 */
public class TimelineView extends View {

    public static final String TAG = TimelineView.class.getSimpleName();

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({LineOrientation.HORIZONTAL, LineOrientation.VERTICAL})
    public @interface LineOrientation {
        int HORIZONTAL = 0;
        int VERTICAL = 1;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({LineType.NORMAL, LineType.START, LineType.END, LineType.ONLYONE})
    public @interface LineType {
        int NORMAL = 0;
        int START = 1;
        int END = 2;
        int ONLYONE = 3;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({LineStyle.NORMAL, LineStyle.DASHED})
    public @interface LineStyle {
        int NORMAL = 0;
        int DASHED = 1;
    }

    private Drawable mMarker;
    private int mMarkerSize;
    private boolean mMarkerInCenter;
    private Paint mStartLinePaint = new Paint();
    private Paint mEndLinePaint = new Paint();
    private boolean mDrawStartLine = false;
    private boolean mDrawEndLine = false;
    private float mStartLineStartX, mStartLineStartY, mStartLineStopX, mStartLineStopY;
    private float mEndLineStartX, mEndLineStartY, mEndLineStopX, mEndLineStopY;
    private int mStartLineColor;
    private int mEndLineColor;
    private int mLineWidth;
    private int mLineOrientation;
    private int mLineStyle;
    private int mLineStyleDashLength;
    private int mLineStyleDashGap;
    private int mLinePadding;

    private int mStartLineWidth;
    private int mEndLineWidth;
    private int mStartLineStyle;
    private int mEndLineStyle;

    private Rect mBounds;

    public TimelineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.TimelineView);
        mMarker = typedArray.getDrawable(R.styleable.TimelineView_marker);
        mMarkerSize = typedArray.getDimensionPixelSize(R.styleable.TimelineView_markerSize, Utils.dpToPx(20, getContext()));
        mMarkerInCenter = typedArray.getBoolean(R.styleable.TimelineView_markerInCenter, true);
        mStartLineColor = typedArray.getColor(R.styleable.TimelineView_startLineColor, getResources().getColor(android.R.color.darker_gray));
        mEndLineColor = typedArray.getColor(R.styleable.TimelineView_endLineColor, getResources().getColor(android.R.color.darker_gray));
        mLineWidth = typedArray.getDimensionPixelSize(R.styleable.TimelineView_lineWidth, Utils.dpToPx(2, getContext()));
        mLineOrientation = typedArray.getInt(R.styleable.TimelineView_lineOrientation, LineOrientation.VERTICAL);
        mLinePadding = typedArray.getDimensionPixelSize(R.styleable.TimelineView_linePadding, 0);
        mLineStyle = typedArray.getInt(R.styleable.TimelineView_lineStyle, LineStyle.NORMAL);
        mLineStyleDashLength = typedArray.getDimensionPixelSize(R.styleable.TimelineView_lineStyleDashLength, Utils.dpToPx(8f, getContext()));
        mLineStyleDashGap = typedArray.getDimensionPixelSize(R.styleable.TimelineView_lineStyleDashGap, Utils.dpToPx(4f, getContext()));

        mStartLineWidth = typedArray.getDimensionPixelSize(R.styleable.TimelineView_startLineWidth, mLineWidth);
        mEndLineWidth = typedArray.getDimensionPixelSize(R.styleable.TimelineView_endLineWidth, mLineWidth);

        mStartLineStyle = typedArray.getInt(R.styleable.TimelineView_startLineStyle, mLineStyle);
        mEndLineStyle = typedArray.getInt(R.styleable.TimelineView_endLineStyle, mLineStyle);
        typedArray.recycle();

        if (isInEditMode()) {
            mDrawStartLine = true;
            mDrawEndLine = true;
        }

        if (mMarker == null) {
            mMarker = getResources().getDrawable(R.drawable.marker);
        }

        initTimeline();
        initLinePaint();

        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //Width measurements of the width and height and the inside view of child controls
        int w = mMarkerSize + getPaddingLeft() + getPaddingRight();
        int h = mMarkerSize + getPaddingTop() + getPaddingBottom();

        // Width and height to determine the final view through a systematic approach to decision-making
        int widthSize = resolveSizeAndState(w, widthMeasureSpec, 0);
        int heightSize = resolveSizeAndState(h, heightMeasureSpec, 0);

        setMeasuredDimension(widthSize, heightSize);
        initTimeline();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        initTimeline();
    }

    private void initTimeline() {

        int pLeft = getPaddingLeft();
        int pRight = getPaddingRight();
        int pTop = getPaddingTop();
        int pBottom = getPaddingBottom();

        int width = getWidth();// Width of current custom view
        int height = getHeight();

        int cWidth = width - pLeft - pRight;// Circle width
        int cHeight = height - pTop - pBottom;

        int markSize = Math.min(mMarkerSize, Math.min(cWidth, cHeight));

        if (mMarkerInCenter) { //Marker in center is true

            if (mMarker != null) {
                mMarker.setBounds((width / 2) - (markSize / 2), (height / 2) - (markSize / 2), (width / 2) + (markSize / 2), (height / 2) + (markSize / 2));
                mBounds = mMarker.getBounds();
            } else {
                mBounds = null;
            }

        } else { //Marker in center is false

            if (mMarker != null) {
                mMarker.setBounds(pLeft, pTop, pLeft + markSize, pTop + markSize);
                mBounds = mMarker.getBounds();
            } else {
                mBounds = null;
            }
        }

        if (mBounds != null) {
            if (mLineOrientation == LineOrientation.HORIZONTAL) {
                if (mDrawStartLine) {
                    mStartLineStartX = pLeft;
                    mStartLineStartY = mBounds.centerY();
                    mStartLineStopX = mBounds.left - mLinePadding;
                    mStartLineStopY = mBounds.centerY();
                }

                if (mDrawEndLine) {
                    mEndLineStartX = mBounds.right + mLinePadding;
                    mEndLineStartY = mBounds.centerY();
                    mEndLineStopX = getWidth();
                    mEndLineStopY = mBounds.centerY();
                }
            } else {

                if (mDrawStartLine) {
                    mStartLineStartX = mBounds.centerX();

                    if (mLineStyle == LineStyle.DASHED) {
                        mStartLineStartY = pTop - mLineStyleDashLength;
                    } else {
                        mStartLineStartY = pTop;
                    }

                    mStartLineStopX = mBounds.centerX();
                    mStartLineStopY = mBounds.top - mLinePadding;
                }

                if (mDrawEndLine) {
                    mEndLineStartX = mBounds.centerX();
                    mEndLineStartY = mBounds.bottom + mLinePadding;
                    mEndLineStopX = mBounds.centerX();
                    mEndLineStopY = getHeight();
                }
            }
        } else {
            float centerX = width * 1f / 2;
            float centerY = height * 1f / 2;
            if (mLineOrientation == LineOrientation.HORIZONTAL) {
                if (mDrawStartLine) {
                    mStartLineStartX = pLeft;
                    mStartLineStartY = centerY;
                    mStartLineStopX = centerX - mLinePadding;
                    mStartLineStopY = centerY;
                }

                if (mDrawEndLine) {
                    mEndLineStartX = centerX + mLinePadding;
                    mEndLineStartY = centerY;
                    mEndLineStopX = getWidth();
                    mEndLineStopY = centerY;
                }
            } else {
                if (mDrawStartLine) {
                    this.mStartLineStartX = centerX;

                    if (mLineStyle == LineStyle.DASHED) {
                        mStartLineStartY = pTop - mLineStyleDashLength;
                    } else {
                        mStartLineStartY = pTop;
                    }

                    mStartLineStopX = centerX;

                    mStartLineStopY = centerY - mLinePadding;
                }

                if (mDrawEndLine) {
                    mEndLineStartX = centerX;
                    mEndLineStartY = centerY + mLinePadding;
                    mEndLineStopX = centerX;
                    mEndLineStopY = getHeight();
                }
            }
        }

        invalidate();
    }

    private void initLinePaint() {
        mStartLinePaint.setAlpha(0);
        mStartLinePaint.setAntiAlias(true);
        mStartLinePaint.setColor(mStartLineColor);
        mStartLinePaint.setStyle(Paint.Style.STROKE);
        mStartLinePaint.setStrokeWidth(mStartLineWidth);

        if (mStartLineStyle == LineStyle.DASHED) {
            mStartLinePaint.setPathEffect(new DashPathEffect(new float[]{(float) mLineStyleDashLength, (float) mLineStyleDashGap}, 0.0f));
        } else {
            mStartLinePaint.setPathEffect(new PathEffect());
        }


        mEndLinePaint.setAlpha(0);
        mEndLinePaint.setAntiAlias(true);
        mEndLinePaint.setColor(mEndLineColor);
        mEndLinePaint.setStyle(Paint.Style.STROKE);
        mEndLinePaint.setStrokeWidth(mEndLineWidth);

        if (mEndLineStyle == LineStyle.DASHED) {
            mEndLinePaint.setPathEffect(new DashPathEffect(new float[]{(float) mLineStyleDashLength, (float) mLineStyleDashGap}, 0.0f));
        } else {
            mEndLinePaint.setPathEffect(new PathEffect());
        }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mMarker != null) {
            mMarker.draw(canvas);
        }

        if (mDrawStartLine) {
            invalidate();
            canvas.drawLine(mStartLineStartX, mStartLineStartY, mStartLineStopX, mStartLineStopY, mStartLinePaint);
        }

        if (mDrawEndLine) {
            invalidate();
            canvas.drawLine(mEndLineStartX, mEndLineStartY, mEndLineStopX, mEndLineStopY, mEndLinePaint);
        }
    }

    /**
     * Sets marker.
     *
     * @param marker will set marker drawable to timeline
     */
    public void setMarker(Drawable marker) {
        mMarker = marker;
        initTimeline();
    }

    public Drawable getMarker() {
        return mMarker;
    }

    /**
     * Sets marker.
     *
     * @param marker will set marker drawable to timeline
     * @param color  with a color
     */
    public void setMarker(Drawable marker, int color) {
        mMarker = marker;
        mMarker.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        initTimeline();
    }

    /**
     * Sets marker color.
     *
     * @param color the color
     */
    public void setMarkerColor(int color) {
        mMarker.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        initTimeline();
    }

    /**
     * Sets start line.
     *
     * @param color    the color of the start line
     * @param viewType the view type
     */
    public void setStartLineColor(int color, int viewType) {
        mStartLineColor = color;
        initLine(viewType);
    }

    public int getStartLineColor() {
        return mStartLineColor;
    }

    /**
     * Sets end line.
     *
     * @param color    the color of the end line
     * @param viewType the view type
     */
    public void setEndLineColor(int color, int viewType) {
        mEndLineColor = color;
        initLine(viewType);
    }

    public int getEndLineColor() {
        return mEndLineColor;
    }

    /**
     * Sets marker size.
     *
     * @param markerSize the marker size
     */
    public void setMarkerSize(int markerSize) {
        mMarkerSize = markerSize;
        initTimeline();
    }

    public int getMarkerSize() {
        return mMarkerSize;
    }

    public boolean isMarkerInCenter() {
        return mMarkerInCenter;
    }

    public void setMarkerInCenter(boolean markerInCenter) {
        this.mMarkerInCenter = markerInCenter;
        initTimeline();
    }

    /**
     * Sets line width.
     *
     * @param mStartLineWidth the start line width
     * @param mEndLineWidth   the end line width
     */
    public void setLineWidth(int mStartLineWidth, int mEndLineWidth) {
        this.mStartLineWidth = mStartLineWidth;
        this.mEndLineWidth = mEndLineWidth;
        initTimeline();
    }

    public void setLineWidth(int mLineWidth) {
        setLineWidth(mLineWidth, mLineWidth);
    }

    public int getLineWidth() {
        return mLineWidth;
    }

    public int getStartLineWidth() {
        return mStartLineWidth;
    }

    public int getEndLineWidth() {
        return mEndLineWidth;
    }

    /**
     * Sets line padding
     *
     * @param padding the line padding
     */
    public void setLinePadding(int padding) {
        mLinePadding = padding;
        initTimeline();
    }

    public int getLineOrientation() {
        return mLineOrientation;
    }

    public void setLineOrientation(int lineOrientation) {
        this.mLineOrientation = lineOrientation;
    }

    public int getStartLineStyle() {
        return mStartLineStyle;
    }

    public int getEndLineStyle() {
        return mEndLineStyle;
    }

    public int getLineStyle() {
        return mLineStyle;
    }

    public void setLineStyle(int mLineStyle) {
        setLineStyle(mLineStyle, mLineStyle);
    }

    public void setLineStyle(int mStartLineStyle, int mEndLineStyle) {
        this.mStartLineStyle = mStartLineStyle;
        this.mEndLineStyle = mEndLineStyle;
        initLinePaint();
    }

    public int getLineStyleDashLength() {
        return mLineStyleDashLength;
    }

    public void setLineStyleDashLength(int lineStyleDashLength) {
        this.mLineStyleDashLength = lineStyleDashLength;
        initLinePaint();
    }

    public int getLineStyleDashGap() {
        return mLineStyleDashGap;
    }

    public void setLineStyleDashGap(int lineStyleDashGap) {
        this.mLineStyleDashGap = lineStyleDashGap;
        initLinePaint();
    }

    public int getLinePadding() {
        return mLinePadding;
    }

    private void showStartLine(boolean show) {
        mDrawStartLine = show;
        initTimeline();
    }

    private void showEndLine(boolean show) {
        mDrawEndLine = show;
        initTimeline();
    }

    /**
     * Init line.
     *
     * @param viewType the view type
     */
    public void initLine(int viewType) {
        if (viewType == LineType.START) {
            showStartLine(false);
            showEndLine(true);
        } else if (viewType == LineType.END) {
            showStartLine(true);
            showEndLine(false);
        } else if (viewType == LineType.ONLYONE) {
            showStartLine(false);
            showEndLine(false);
        } else {
            showStartLine(true);
            showEndLine(true);
        }

        initTimeline();
    }

    /**
     * Gets timeline view type.
     *
     * @param position  the position of current item view
     * @param totalSize the total size of the items
     * @return the timeline view type
     */
    public static int getTimeLineViewType(int position, int totalSize) {
        if (totalSize == 1) {
            return LineType.ONLYONE;
        } else if (position == 0) {
            return LineType.START;
        } else if (position == totalSize - 1) {
            return LineType.END;
        } else {
            return LineType.NORMAL;
        }
    }
}
