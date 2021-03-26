package org.lineageos.eleven.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ClippedImageView extends ImageView {


    public ClippedImageView(@NonNull Context context) {
        this(context, null);
    }

    public ClippedImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ClippedImageView(@NonNull Context context, @Nullable AttributeSet attrs,
                            int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setClipToOutline(true);
    }
}
