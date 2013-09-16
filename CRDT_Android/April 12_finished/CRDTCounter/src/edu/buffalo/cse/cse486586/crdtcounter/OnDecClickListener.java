package edu.buffalo.cse.cse486586.crdtcounter;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class OnDecClickListener implements OnClickListener{
	CrdtCounter counter;
	public OnDecClickListener(CrdtCounter counter,TextView _tv) {
		this.counter = counter;
	}
	public void onClick(View v) {
		counter.decrement();
	}
}
