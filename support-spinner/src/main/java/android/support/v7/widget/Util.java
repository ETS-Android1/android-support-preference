package android.support.v7.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.AttrRes;
import android.support.annotation.RestrictTo;
import android.util.TypedValue;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
final class Util {
    private static final ThreadLocal<int[]> TEMP_ARRAY = new ThreadLocal<int[]>() {
        @Override
        protected int[] initialValue() {
            return new int[]{0};
        }
    };

    private Util() {
        throw new AssertionError();
    }

    private static int[] getTempArray() {
        return TEMP_ARRAY.get();
    }

    public static float resolveDimension(Context context, @AttrRes int attr, float fallback) {
        final int[] tempArray = getTempArray();
        tempArray[0] = attr;
        TypedArray ta = context.obtainStyledAttributes(tempArray);
        try {
            return ta.getDimension(0, fallback);
        } finally {
            ta.recycle();
        }
    }

    public static int resolveDimensionPixelSize(Context context, @AttrRes int attr, int fallback) {
        float dimen = resolveDimension(context, attr, fallback);
        return (int) (dimen + 0.5f);
    }

    public static int dpToPxOffset(Context context, int dp) {
        return (int) (dpToPx(context, dp));
    }

    public static int dpToPxSize(Context context, int dp) {
        return (int) (0.5f + dpToPx(context, dp));
    }

    public static float dpToPx(Context context, int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

}
