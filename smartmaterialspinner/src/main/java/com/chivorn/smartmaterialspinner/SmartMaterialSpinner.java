package com.chivorn.smartmaterialspinner;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatSpinner;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.chivorn.smartmaterialspinner.util.SoftKeyboardUtil;
import com.chivorn.smartmaterialspinner.util.nineoldandroid.animation.ObjectAnimator;
import com.chivorn.smartmaterialspinner.util.nineoldandroid.animation.ValueAnimator;

import java.util.ArrayList;
import java.util.List;


public class SmartMaterialSpinner extends AppCompatSpinner implements ValueAnimator.AnimatorUpdateListener {
    public static final int DEFAULT_ARROW_WIDTH_DP = 10;

    private static final String TAG = SmartMaterialSpinner.class.getSimpleName();

    //Paint objects
    private Paint paint;
    private TextPaint textPaint;
    private StaticLayout staticLayout;


    private Path selectorPath;
    private Point[] selectorPoints;

    //Inner padding = "Normal" android padding
    private int innerPaddingLeft;
    private int innerPaddingRight;
    private int innerPaddingTop;
    private int innerPaddingBottom;

    //Private padding to add space for FloatingLabel and ErrorLabel
    private int extraPaddingTop;
    private int extraPaddingBottom;

    //@see dimens.xml
    private int underlineTopSpacing;
    private int underlineBottomSpacing;
    private int errorLabelSpacing;
    private int floatingLabelTopSpacing;
    private int floatingLabelBottomSpacing;
    private int floatingLabelInsideSpacing;
    private int rightLeftSpinnerPadding;
    private int minContentHeight;

    //Properties about Error Label
    private int lastPosition;
    private ObjectAnimator errorLabelAnimator;
    private int errorLabelPosX;
    private int minNbErrorLines;
    private float currentNbErrorLines;


    //Properties about Floating Label (
    private float floatingLabelPercent;
    private ObjectAnimator floatingLabelAnimator;
    private boolean isSelected;
    private boolean floatingLabelVisible;
    private int baseAlpha;


    //AttributeSet
    private int baseColor;
    private int highlightColor;
    private int errorColor;
    private int disabledColor;
    private int underlineColor;
    private CharSequence error;
    private CharSequence hint;
    private int hintColor;
    private int itemColor;
    private int itemListColor;
    private int selectedItemColor;
    private float hintTextSize;
    private CharSequence floatingLabelText;
    private int floatingLabelColor;
    private boolean multiline;
    private Typeface typeface;
    private boolean alignLabels;
    private float underlineSize;
    private int arrowColor;
    private float arrowSize;
    private boolean enableErrorLabel;
    private boolean enableFloatingLabel;
    private boolean alwaysShowFloatingLabel;
    private boolean isRtl;
    private boolean isShowEmptyDropdown;

    private HintAdapter hintAdapter;

    //Default hint views
    private Integer mDropdownView;
    private Integer mHintView;

    /*
     * **********************************************************************************
     * CONSTRUCTORS
     * **********************************************************************************
     */

    public SmartMaterialSpinner(Context context) {
        super(context);
        init(context, null);
    }

    public SmartMaterialSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);

    }

    public SmartMaterialSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }


    /*
     * **********************************************************************************
     * INITIALISATION METHODS
     * **********************************************************************************
     */

    private void init(Context context, AttributeSet attrs) {
        initAttributes(context, attrs);
        initPaintObjects();
        initDimensions();
        initPadding();
        initFloatingLabelAnimator();
        initOnItemSelectedListener();
        setMinimumHeight(getPaddingTop() + getPaddingBottom() + minContentHeight);
        //Erase the drawable selector not to be affected by new size (extra paddings)
        setBackgroundResource(R.drawable.smart_material_spinner_background);
    }

    private void initAttributes(Context context, AttributeSet attrs) {

        TypedArray defaultArray = context.obtainStyledAttributes(new int[]{R.attr.colorControlNormal, R.attr.colorAccent});
        int defaultBaseColor = defaultArray.getColor(0, 0);
        int defaultHighlightColor = defaultArray.getColor(1, 0);
        int defaultErrorColor = context.getResources().getColor(R.color.smsp_error_color);
        defaultArray.recycle();

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.SmartMaterialSpinner);
        baseColor = array.getColor(R.styleable.SmartMaterialSpinner_smsp_baseColor, defaultBaseColor);
        highlightColor = array.getColor(R.styleable.SmartMaterialSpinner_smsp_highlightColor, defaultHighlightColor);
        errorColor = array.getColor(R.styleable.SmartMaterialSpinner_smsp_errorColor, defaultErrorColor);
        disabledColor = context.getResources().getColor(R.color.smsp_disabled_color);
        underlineColor = array.getColor(R.styleable.SmartMaterialSpinner_smsp_underlineColor, defaultBaseColor);
        error = array.getString(R.styleable.SmartMaterialSpinner_smsp_error);
        hint = array.getString(R.styleable.SmartMaterialSpinner_smsp_hint);
        floatingLabelText = array.getString(R.styleable.SmartMaterialSpinner_smsp_floatingLabelText);
        hintColor = array.getColor(R.styleable.SmartMaterialSpinner_smsp_hintColor, baseColor);
        itemColor = array.getColor(R.styleable.SmartMaterialSpinner_smsp_itemColor, Color.BLACK);
        itemListColor = array.getColor(R.styleable.SmartMaterialSpinner_smsp_itemListColor, Color.BLACK);
        selectedItemColor = array.getColor(R.styleable.SmartMaterialSpinner_smsp_selectedItemColor, itemColor);
        hintTextSize = array.getDimension(R.styleable.SmartMaterialSpinner_smsp_hintTextSize, -1);
        floatingLabelColor = array.getColor(R.styleable.SmartMaterialSpinner_smsp_floatingLabelColor, baseColor);
        multiline = array.getBoolean(R.styleable.SmartMaterialSpinner_smsp_multiline, true);
        minNbErrorLines = array.getInt(R.styleable.SmartMaterialSpinner_smsp_nbErrorLines, 1);
        alignLabels = array.getBoolean(R.styleable.SmartMaterialSpinner_smsp_alignLabels, true);
        underlineSize = array.getDimension(R.styleable.SmartMaterialSpinner_smsp_underlineSize, 0.6f);
        arrowColor = array.getColor(R.styleable.SmartMaterialSpinner_smsp_arrowColor, baseColor);
        arrowSize = array.getDimension(R.styleable.SmartMaterialSpinner_smsp_arrowSize, dpToPx(DEFAULT_ARROW_WIDTH_DP));
        enableErrorLabel = array.getBoolean(R.styleable.SmartMaterialSpinner_smsp_enableErrorLabel, true);
        enableFloatingLabel = array.getBoolean(R.styleable.SmartMaterialSpinner_smsp_enableFloatingLabel, true);
        alwaysShowFloatingLabel = array.getBoolean(R.styleable.SmartMaterialSpinner_smsp_alwaysShowFloatingLabel, false);
        isRtl = array.getBoolean(R.styleable.SmartMaterialSpinner_smsp_isRtl, false);
        mHintView = array.getResourceId(R.styleable.SmartMaterialSpinner_smsp_hintView, R.layout.smart_material_spinner_hint_item_layout);
        mDropdownView = array.getResourceId(R.styleable.SmartMaterialSpinner_smsp_dropdownView, R.layout.smart_material_spinner_dropdown_item);
        isShowEmptyDropdown = array.getBoolean(R.styleable.SmartMaterialSpinner_smsp_showEmptyDropdown, true);

        String typefacePath = array.getString(R.styleable.SmartMaterialSpinner_smsp_typeface);
        if (typefacePath != null) {
            typeface = Typeface.createFromAsset(getContext().getAssets(), typefacePath);
        }

        array.recycle();

        floatingLabelPercent = 0f;
        errorLabelPosX = 0;
        isSelected = false;
        floatingLabelVisible = false;
        lastPosition = -1;
        currentNbErrorLines = minNbErrorLines;

        // Set default item to spinner to enable dropdown item
        setItems(new ArrayList<String>());
        if (isShowEmptyDropdown) {
            configDropdownSpinnerAfterHasItems(true);
        } else {
            configDropdownSpinnerAfterHasItems(false);
        }
    }

    /*
     * Config dropdown width and height.
     */
    private void configDropdownSpinnerAfterHasItems(final boolean isShowEmptyDropdown) {
        this.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    SmartMaterialSpinner.this.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                if (isShowEmptyDropdown) {
                    SmartMaterialSpinner.this.setDropDownWidth(SmartMaterialSpinner.this.getWidth());
                    SmartMaterialSpinner.this.setDropDownVerticalOffset(SmartMaterialSpinner.this.getHeight());
                } else {
                    SmartMaterialSpinner.this.setDropDownWidth(0);
                    SmartMaterialSpinner.this.setDropDownVerticalOffset(0);
                }
            }
        });
    }

    @Override
    public void setSelection(final int position) {
        this.post(new Runnable() {
            @Override
            public void run() {
                SmartMaterialSpinner.super.setSelection(position);
            }
        });
    }

    private void initPaintObjects() {

        int labelTextSize = getResources().getDimensionPixelSize(R.dimen.label_text_size);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(labelTextSize);
        if (typeface != null) {
            textPaint.setTypeface(typeface);
        }
        textPaint.setColor(baseColor);
        baseAlpha = textPaint.getAlpha();

        selectorPath = new Path();
        selectorPath.setFillType(Path.FillType.EVEN_ODD);

        selectorPoints = new Point[3];
        for (int i = 0; i < 3; i++) {
            selectorPoints[i] = new Point();
        }
    }

    @Override
    public int getSelectedItemPosition() {
        return super.getSelectedItemPosition();
    }

    private void initPadding() {

        innerPaddingTop = getPaddingTop();
        innerPaddingLeft = getPaddingLeft();
        innerPaddingRight = getPaddingRight();
        innerPaddingBottom = getPaddingBottom();

        extraPaddingTop = enableFloatingLabel ? floatingLabelTopSpacing + floatingLabelInsideSpacing + floatingLabelBottomSpacing : floatingLabelBottomSpacing;
        updateBottomPadding();

    }

    private void updateBottomPadding() {
        Paint.FontMetrics textMetrics = textPaint.getFontMetrics();
        extraPaddingBottom = underlineTopSpacing + underlineBottomSpacing;
        if (enableErrorLabel) {
            extraPaddingBottom += (int) ((textMetrics.descent - textMetrics.ascent) * currentNbErrorLines);
        }
        updatePadding();
    }

    private void initDimensions() {
        underlineTopSpacing = getResources().getDimensionPixelSize(R.dimen.smsp_underline_top_spacing);
        underlineBottomSpacing = getResources().getDimensionPixelSize(R.dimen.smsp_underline_bottom_spacing);
        floatingLabelTopSpacing = getResources().getDimensionPixelSize(R.dimen.smsp_floating_label_top_spacing);
        floatingLabelBottomSpacing = getResources().getDimensionPixelSize(R.dimen.smsp_floating_label_bottom_spacing);
        rightLeftSpinnerPadding = alignLabels ? getResources().getDimensionPixelSize(R.dimen.smsp_right_left_spinner_padding) : 0;
        floatingLabelInsideSpacing = getResources().getDimensionPixelSize(R.dimen.smsp_floating_label_inside_spacing);
        errorLabelSpacing = (int) getResources().getDimension(R.dimen.smsp_error_label_spacing);
        minContentHeight = (int) getResources().getDimension(R.dimen.smsp_min_content_height);
    }

    private void initOnItemSelectedListener() {
        setOnItemSelectedListener(null);
    }

    /*
     * **********************************************************************************
     * ANIMATION METHODS
     * **********************************************************************************
     */

    private void initFloatingLabelAnimator() {
        if (floatingLabelAnimator == null) {
            floatingLabelAnimator = ObjectAnimator.ofFloat(this, "floatingLabelPercent", 0f, 1f);
            floatingLabelAnimator.addUpdateListener(this);
        }
    }

    public void showFloatingLabel() {
        if (floatingLabelAnimator != null) {
            floatingLabelVisible = true;
            if (floatingLabelAnimator.isRunning()) {
                floatingLabelAnimator.reverse();
            } else {
                floatingLabelAnimator.start();
            }
        }
    }

    public void hideFloatingLabel() {
        if (floatingLabelAnimator != null) {
            floatingLabelVisible = false;
            floatingLabelAnimator.reverse();
        }
    }

    private void startErrorScrollingAnimator() {

        int textWidth = Math.round(textPaint.measureText(error.toString()));
        if (errorLabelAnimator == null) {
            errorLabelAnimator = ObjectAnimator.ofInt(this, "errorLabelPosX", 0, textWidth + getWidth() / 2);
            errorLabelAnimator.setStartDelay(1000);
            errorLabelAnimator.setInterpolator(new LinearInterpolator());
            errorLabelAnimator.setDuration(150 * error.length());
            errorLabelAnimator.addUpdateListener(this);
            errorLabelAnimator.setRepeatCount(ValueAnimator.INFINITE);
        } else {
            errorLabelAnimator.setIntValues(0, textWidth + getWidth() / 2);
        }
        errorLabelAnimator.start();
    }


    private void startErrorMultilineAnimator(float destLines) {
        if (errorLabelAnimator == null) {
            errorLabelAnimator = ObjectAnimator.ofFloat(this, "currentNbErrorLines", destLines);

        } else {
            errorLabelAnimator.setFloatValues(destLines);
        }
        errorLabelAnimator.start();
    }


    /*
     * **********************************************************************************
     * UTILITY METHODS
     * **********************************************************************************
     */

    private int dpToPx(float dp) {
        final DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics);
        return Math.round(px);
    }

    private float pxToDp(float px) {
        final DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        return px * displayMetrics.density;
    }

    private void updatePadding() {
        int left = innerPaddingLeft;
        int top = innerPaddingTop + extraPaddingTop;
        int right = innerPaddingRight;
        int bottom = innerPaddingBottom + extraPaddingBottom;
        super.setPadding(left, top, right, bottom);
        setMinimumHeight(top + bottom + minContentHeight);
    }

    private boolean needScrollingAnimation() {
        if (error != null) {
            float screenWidth = getWidth() - rightLeftSpinnerPadding;
            float errorTextWidth = textPaint.measureText(error.toString(), 0, error.length());
            return errorTextWidth > screenWidth ? true : false;
        }
        return false;
    }

    private int prepareBottomPadding() {

        int targetNbLines = minNbErrorLines;
        if (error != null) {
            int mWidth = getWidth() - getPaddingRight() - getPaddingLeft();
            if (mWidth < 0) {
                mWidth = 0;
            }
            staticLayout = new StaticLayout(error, textPaint, mWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true);
            int nbErrorLines = staticLayout.getLineCount();
            targetNbLines = Math.max(minNbErrorLines, nbErrorLines);
        }
        return targetNbLines;
    }

    private boolean isSpinnerEmpty() {
        return (hintAdapter.getCount() == 0 && hint == null) || (hintAdapter.getCount() == 1 && hint != null);
    }

    /*
     * **********************************************************************************
     * DRAWING METHODS
     * **********************************************************************************
     */


    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);
        int startX = 0;
        int endX = getWidth();
        int lineHeight;

        int startYLine = getHeight() - getPaddingBottom() + underlineTopSpacing;
        int startYFloatingLabel = (int) (getPaddingTop() - floatingLabelPercent * floatingLabelBottomSpacing);


        if (error != null && enableErrorLabel) {
            lineHeight = dpToPx(underlineSize);
            int startYErrorLabel = startYLine + errorLabelSpacing + lineHeight;
            paint.setColor(errorColor);
            textPaint.setColor(errorColor);
            //Error Label Drawing
            if (multiline) {
                canvas.save();
                canvas.translate(startX + rightLeftSpinnerPadding, startYErrorLabel - errorLabelSpacing);
                if (staticLayout == null) {
                    prepareBottomPadding();
                }
                staticLayout.draw(canvas);
                canvas.restore();

            } else {
                //scrolling
                canvas.drawText(error.toString(), startX + rightLeftSpinnerPadding - errorLabelPosX, startYErrorLabel, textPaint);
                if (errorLabelPosX > 0) {
                    canvas.save();
                    canvas.translate(textPaint.measureText(error.toString()) + getWidth() / 2, 0);
                    canvas.drawText(error.toString(), startX + rightLeftSpinnerPadding - errorLabelPosX, startYErrorLabel, textPaint);
                    canvas.restore();
                }
            }

        } else {
            lineHeight = dpToPx(underlineSize);
            if (isSelected || hasFocus()) {
                paint.setColor(highlightColor);
            } else {
                paint.setColor(isEnabled() ? underlineColor : disabledColor);
            }
        }

        // Underline Drawing
        canvas.drawRect(startX, startYLine, endX, startYLine + lineHeight, paint);

        //Floating Label Drawing
        if ((hint != null || floatingLabelText != null) && enableFloatingLabel) {
            if (isSelected || hasFocus()) {
                textPaint.setColor(highlightColor);
            } else {
                textPaint.setColor(isEnabled() ? floatingLabelColor : disabledColor);
            }
            if (floatingLabelAnimator.isRunning() || !floatingLabelVisible) {
                textPaint.setAlpha((int) ((0.8 * floatingLabelPercent + 0.2) * baseAlpha * floatingLabelPercent));
            }
            String textToDraw = floatingLabelText != null ? floatingLabelText.toString() : hint.toString();
            if (isRtl) {
                canvas.drawText(textToDraw, getWidth() - rightLeftSpinnerPadding - textPaint.measureText(textToDraw), startYFloatingLabel, textPaint);
            } else {
                canvas.drawText(textToDraw, startX + rightLeftSpinnerPadding, startYFloatingLabel, textPaint);
            }
        }

        drawSelector(canvas, getWidth() - rightLeftSpinnerPadding, getPaddingTop() + dpToPx(8));


    }

    private void drawSelector(Canvas canvas, int posX, int posY) {
        if (isSelected || hasFocus()) {
            paint.setColor(highlightColor);
        } else {
            paint.setColor(isEnabled() ? arrowColor : disabledColor);
        }

        Point point1 = selectorPoints[0];
        Point point2 = selectorPoints[1];
        Point point3 = selectorPoints[2];

        point1.set(posX, posY);
        point2.set((int) (posX - (arrowSize)), posY);
        point3.set((int) (posX - (arrowSize / 2)), (int) (posY + (arrowSize / 2)));

        selectorPath.reset();
        selectorPath.moveTo(point1.x, point1.y);
        selectorPath.lineTo(point2.x, point2.y);
        selectorPath.lineTo(point3.x, point3.y);
        selectorPath.close();
        canvas.drawPath(selectorPath, paint);
    }

    /*
     * **********************************************************************************
     * LISTENER METHODS
     * **********************************************************************************
     */

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isEnabled()) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isSelected = true;
                    if (getContext() instanceof Activity)
                        SoftKeyboardUtil.hideSoftKeyboard((Activity) getContext());
                    if (!isShowEmptyDropdown) {
                        return true;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isSelected = false;
                    break;
            }
            invalidate();
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    public void setOnItemSelectedListener(final OnItemSelectedListener listener) {
        final OnItemSelectedListener onItemSelectedListener = new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (hint != null || floatingLabelText != null) {
                    if (!floatingLabelVisible && position != 0) {
                        showFloatingLabel();
                    } else if (floatingLabelVisible && (position == 0 && !alwaysShowFloatingLabel)) {
                        hideFloatingLabel();
                    }
                }

                if (position != lastPosition && error != null) {
                    setError(null);
                }
                boolean isStartup = lastPosition == -1;
                lastPosition = position;

                if (listener != null) {
                    position = hint != null ? position - 1 : position;
                    if (position >= 0) {
                        if (view instanceof TextView) {
                            TextView selectedItem = (TextView) parent.getChildAt(0);
                            selectedItem.setTextColor(itemColor);
                        }
                        listener.onItemSelected(parent, view, position, id);
                    } else if (position == -1 && !isStartup) {
                        listener.onNothingSelected(parent);
                    }
                } else {
                    if (position > 0 && !isStartup) {
                        if (view instanceof TextView) {
                            TextView selectedItem = (TextView) parent.getChildAt(0);
                            selectedItem.setTextColor(itemColor);
                        }
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (listener != null) {
                    listener.onNothingSelected(parent);
                }
            }
        };

        super.setOnItemSelectedListener(onItemSelectedListener);
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        invalidate();
    }


    /*
     * **********************************************************************************
     * GETTERS AND SETTERS
     * **********************************************************************************
     */

    public int getBaseColor() {
        return baseColor;
    }

    public void setBaseColor(int baseColor) {
        this.baseColor = baseColor;
        textPaint.setColor(baseColor);
        baseAlpha = textPaint.getAlpha();
        invalidate();
    }

    public int getHighlightColor() {
        return highlightColor;
    }

    public void setHighlightColor(int highlightColor) {
        this.highlightColor = highlightColor;
        invalidate();
    }

    public int getHintColor() {
        return hintColor;
    }

    public void setHintColor(int hintColor) {
        this.hintColor = hintColor;
        invalidate();
    }

    public float getHintTextSize() {
        return hintTextSize;
    }

    public void setHintTextSize(float hintTextSize) {
        this.hintTextSize = hintTextSize;
        invalidate();
    }

    public int getErrorColor() {
        return errorColor;
    }

    public void setErrorColor(int errorColor) {
        this.errorColor = errorColor;
        invalidate();
    }

    public int getDisabledColor() {
        return disabledColor;
    }

    public void setDisabledColor(int disabledColor) {
        this.disabledColor = disabledColor;
        invalidate();
    }

    public void setHint(CharSequence hint) {
        this.hint = hint;
        invalidate();
    }

    public void setHint(int resid) {
        CharSequence hint = getResources().getString(resid);
        setHint(hint);
    }

    public CharSequence getHint() {
        return hint;
    }

    public void setHintView(Integer resId) {
        this.mHintView = resId;
    }

    public void setDripDownHintView(Integer resId) {
        this.mDropdownView = resId;
    }

    public void setFloatingLabelText(CharSequence floatingLabelText) {
        this.floatingLabelText = floatingLabelText;
        invalidate();
    }

    public void setFloatingLabelText(int resid) {
        String floatingLabelText = getResources().getString(resid);
        setFloatingLabelText(floatingLabelText);
    }

    public CharSequence getFloatingLabelText() {
        return this.floatingLabelText;
    }

    public int getFloatingLabelColor() {
        return floatingLabelColor;
    }

    public void setFloatingLabelColor(int floatingLabelColor) {
        this.floatingLabelColor = floatingLabelColor;
        invalidate();
    }

    public boolean isMultiline() {
        return multiline;
    }

    public void setMultiline(boolean multiline) {
        this.multiline = multiline;
        invalidate();
    }

    public Typeface getTypeface() {
        return typeface;
    }

    public void setTypeface(Typeface typeface) {
        this.typeface = typeface;
        if (typeface != null) {
            textPaint.setTypeface(typeface);
        }
        invalidate();
    }

    public boolean isAlignLabels() {
        return alignLabels;
    }

    public void setAlignLabels(boolean alignLabels) {
        this.alignLabels = alignLabels;
        rightLeftSpinnerPadding = alignLabels ? getResources().getDimensionPixelSize(R.dimen.smsp_right_left_spinner_padding) : 0;
        invalidate();
    }

    public float getUnderlineSize() {
        return underlineSize;
    }

    public void setUnderlineSize(float underlineSize) {
        this.underlineSize = underlineSize;
        invalidate();
    }

    public int getArrowColor() {
        return arrowColor;
    }

    public void setArrowColor(int arrowColor) {
        this.arrowColor = arrowColor;
        invalidate();
    }

    public float getArrowSize() {
        return arrowSize;
    }

    public void setArrowSize(float arrowSize) {
        this.arrowSize = arrowSize;
        invalidate();
    }

    public boolean isEnableErrorLabel() {
        return enableErrorLabel;
    }

    public void setEnableErrorLabel(boolean enableErrorLabel) {
        this.enableErrorLabel = enableErrorLabel;
        updateBottomPadding();
        invalidate();
    }

    public boolean isEnableFloatingLabel() {
        return enableFloatingLabel;
    }

    public void setEnableFloatingLabel(boolean enableFloatingLabel) {
        this.enableFloatingLabel = enableFloatingLabel;
        extraPaddingTop = enableFloatingLabel ? floatingLabelTopSpacing + floatingLabelInsideSpacing + floatingLabelBottomSpacing : floatingLabelBottomSpacing;
        updateBottomPadding();
        invalidate();
    }

    public void setError(CharSequence error) {
        this.error = error;
        if (errorLabelAnimator != null) {
            errorLabelAnimator.end();
        }

        if (multiline) {
            startErrorMultilineAnimator(prepareBottomPadding());
        } else if (needScrollingAnimation()) {
            startErrorScrollingAnimator();
        }
        requestLayout();
    }

    public void setError(int resid) {
        CharSequence error = getResources().getString(resid);
        setError(error);
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (!enabled) {
            isSelected = false;
            invalidate();
        }
        super.setEnabled(enabled);
    }

    public CharSequence getError() {
        return this.error;
    }

    public void setRtl() {
        isRtl = true;
        invalidate();
    }

    public boolean isRtl() {
        return isRtl;
    }

    public void setUnderlineColor(int color) {
        underlineColor = color;
        invalidate();
    }

    public void setItemColor(int color) {
        this.itemColor = color;
    }

    public void setItemListColor(int color) {
        this.itemListColor = color;
        if (selectedItemColor == Color.BLACK && color != Color.BLACK) {
            selectedItemColor = color;
        }
    }

    public void setSelectedItemColor(int color) {
        this.selectedItemColor = color;
    }

    /**
     * @deprecated {use @link #setPaddingSafe(int, int, int, int)} to keep internal computation OK
     */
    @Deprecated
    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(left, top, right, bottom);
    }


    public void setPaddingSafe(int left, int top, int right, int bottom) {
        innerPaddingRight = right;
        innerPaddingLeft = left;
        innerPaddingTop = top;
        innerPaddingBottom = bottom;

        updatePadding();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void setAdapter(SpinnerAdapter adapter) {
        hintAdapter = new HintAdapter(adapter, getContext());
        super.setAdapter(hintAdapter);
    }

    public <T> void setItems(@NonNull List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), R.layout.smart_material_spinner_hint_item_layout, items);
        adapter.setDropDownViewResource(R.layout.smart_material_spinner_dropdown_item);
        setAdapter(adapter);
    }

    @Override
    public SpinnerAdapter getAdapter() {
        return hintAdapter != null ? hintAdapter.getWrappedAdapter() : null;
    }

    private float getFloatingLabelPercent() {
        return floatingLabelPercent;
    }

    private void setFloatingLabelPercent(float floatingLabelPercent) {
        this.floatingLabelPercent = floatingLabelPercent;
    }

    private int getErrorLabelPosX() {
        return errorLabelPosX;
    }

    private void setErrorLabelPosX(int errorLabelPosX) {
        this.errorLabelPosX = errorLabelPosX;
    }

    private float getCurrentNbErrorLines() {
        return currentNbErrorLines;
    }

    private void setCurrentNbErrorLines(float currentNbErrorLines) {
        this.currentNbErrorLines = currentNbErrorLines;
        updateBottomPadding();
    }

    @Override
    public Object getItemAtPosition(int position) {
        if (hint != null) {
            position++;
        }
        return (hintAdapter == null || position < 0) ? null : hintAdapter.getItem(position);
    }

    @Override
    public long getItemIdAtPosition(int position) {
        if (hint != null) {
            position++;
        }
        return (hintAdapter == null || position < 0) ? INVALID_ROW_ID : hintAdapter.getItemId(position);
    }

    public void setShowEmptyDropdown(boolean status) {
        isShowEmptyDropdown = status;
        configDropdownSpinnerAfterHasItems(status);
    }


    /*
     * **********************************************************************************
     * INNER CLASS
     * **********************************************************************************
     */

    private class HintAdapter extends BaseAdapter {

        private static final int HINT_TYPE = -1;

        private SpinnerAdapter mSpinnerAdapter;
        private Context mContext;

        public HintAdapter(SpinnerAdapter spinnerAdapter, Context context) {
            mSpinnerAdapter = spinnerAdapter;
            mContext = context;
        }


        @Override
        public int getViewTypeCount() {
            //Workaround waiting for a Google correction (https://code.google.com/p/android/issues/detail?id=79011)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return 1;
            }
            int viewTypeCount = mSpinnerAdapter.getViewTypeCount();
            return viewTypeCount;
        }

        @Override
        public int getItemViewType(int position) {
            position = hint != null ? position - 1 : position;
            return (position == -1) ? HINT_TYPE : mSpinnerAdapter.getItemViewType(position);
        }

        @Override
        public int getCount() {
            int count = mSpinnerAdapter.getCount();
            return hint != null ? count + 1 : count;
        }

        @Override
        public Object getItem(int position) {
            position = hint != null ? position - 1 : position;
            return (position == -1) ? hint : mSpinnerAdapter.getItem(position);
        }

        @Override
        public long getItemId(int position) {
            position = hint != null ? position - 1 : position;
            return (position == -1) ? 0 : mSpinnerAdapter.getItemId(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return buildView(position, convertView, parent, false);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return buildView(position, convertView, parent, true);
        }

        private View buildView(int position, View convertView, ViewGroup parent, boolean isDropDownView) {
            if (getItemViewType(position) == HINT_TYPE) {
                return getHintView(convertView, parent, isDropDownView);
            }
            //workaround to have multiple types in spinner
            if (convertView != null) {
                convertView = (convertView.getTag() != null && convertView.getTag() instanceof Integer && (Integer) convertView.getTag() != HINT_TYPE) ? convertView : null;
            }
            position = hint != null ? position - 1 : position;
            View dropdownItemView = (isDropDownView ? mSpinnerAdapter.getDropDownView(position, convertView, parent) : mSpinnerAdapter.getView(position, convertView, parent));
            if (dropdownItemView instanceof TextView) {
                TextView itemTextView = (TextView) dropdownItemView;
                itemTextView.setTextColor(itemListColor);
                if (position >= 0 && position == getSelectedItemPosition() - 1) {
                    itemTextView.setTextColor(selectedItemColor);
                }
            }
            return dropdownItemView;
        }

        private View getHintView(final View convertView, final ViewGroup parent, final boolean isDropDownView) {

            final LayoutInflater inflater = LayoutInflater.from(mContext);
            final int resid = isDropDownView ? mDropdownView : mHintView;
            final TextView textView = (TextView) inflater.inflate(resid, parent, false);
            textView.setText(hint);
            textView.setTextColor(SmartMaterialSpinner.this.isEnabled() ? hintColor : disabledColor);
            textView.setTag(HINT_TYPE);
            if (hintTextSize != -1)
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, hintTextSize);
            return textView;
        }

        private SpinnerAdapter getWrappedAdapter() {
            return mSpinnerAdapter;
        }
    }
}