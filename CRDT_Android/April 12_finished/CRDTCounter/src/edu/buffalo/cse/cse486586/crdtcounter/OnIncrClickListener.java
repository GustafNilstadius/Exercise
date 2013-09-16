package edu.buffalo.cse.cse486586.crdtcounter;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class OnIncrClickListener implements OnClickListener{
	CrdtCounter counter;
	public OnIncrClickListener( CrdtCounter counter,TextView _tv) {
		this.counter = counter;
	}
	public void onClick(View v) {
		counter.increment();
	}
}
