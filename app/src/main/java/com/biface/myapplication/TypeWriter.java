package com.biface.myapplication;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;
import android.os.Handler;

import androidx.annotation.Nullable;

public class TypeWriter extends androidx.appcompat.widget.AppCompatTextView {
    private CharSequence myText;
    private int myIndex;
    private int myDelay = 100;

    public TypeWriter(Context context){
        super(context);
    }

    public TypeWriter(Context context, @Nullable AttributeSet attrs){
        super(context, attrs);
    }

    private Handler myHandler = new Handler();

    private Runnable characterAdder = new Runnable() {
        @Override
        public void run() {
            setText(myText.subSequence(0, myIndex++));
            if(myIndex <= myText.length()){
                myHandler.postDelayed(characterAdder, myDelay);
            }
        }
    };

    public void animateText(CharSequence myText){
        this.myText = myText;
        myIndex = 0;

        setText("");
        myHandler.removeCallbacks(characterAdder);
        myHandler.postDelayed(characterAdder, myDelay);
    }

    private void setCharacterDelay(int m){
        myDelay = m;
    }

}
