package de.tobifleig.lxc.plaf.android.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.tobifleig.lxc.R;

public class HelpElement extends LinearLayout {

    public HelpElement(Context context, AttributeSet attrs) {
        super(context, attrs);
        // load layout
        LayoutInflater.from(context).inflate(R.layout.help_element, this);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.HelpElement,
                0, 0);

        try {
            ((TextView) findViewById(R.id.help_question)).setText(a.getString(R.styleable.HelpElement_questionText));
            ((TextView) findViewById(R.id.help_answer)).setText(a.getString(R.styleable.HelpElement_answerText));
        } finally {
            a.recycle();
        }
    }

}
