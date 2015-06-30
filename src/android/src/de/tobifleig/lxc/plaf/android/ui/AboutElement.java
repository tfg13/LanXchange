package de.tobifleig.lxc.plaf.android.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.tobifleig.lxc.R;

public class AboutElement extends LinearLayout {

    public AboutElement(Context context, AttributeSet attrs) {
        super(context, attrs);
        // load layout
        LayoutInflater.from(context).inflate(R.layout.about_element, this);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.AboutElement,
                0, 0);

        try {
            ((TextView) findViewById(R.id.about_header)).setText(a.getString(R.styleable.AboutElement_headerText));
            ((TextView) findViewById(R.id.about_text)).setText(a.getString(R.styleable.AboutElement_contentText));
        } finally {
            a.recycle();
        }
    }

    /**
     * Set text dynamically, required to fill in some values at runtime.
     * @param text the new text
     */
    public void setText(String text) {
        ((TextView) findViewById(R.id.about_text)).setText(text);
    }

}
