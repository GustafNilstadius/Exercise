package edu.buffalo.cse.cse486586.crdtcounter;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;
public class GroupMessengerActivity extends Activity {
	

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        CrdtCounter counter= setupMcast();
        findViewById(R.id.button1).setOnClickListener(
                new OnIncrClickListener(counter,tv));    
            
        findViewById(R.id.button2).setOnClickListener(
                new OnDecClickListener(counter,tv));
    }

    private CrdtCounter setupMcast()
    {
    	TelephonyManager tel =
		        (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		
		String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
		
		String me;
		ArrayList<process> peers;
		process p0,p1,p2;
		
		p0=new process("10.0.2.2", 11108);
		p1=new process("10.0.2.2", 11112);
		p2=new process("10.0.2.2", 11116);	
				
		if(0==portStr.compareTo("5554"))
		{
			me="avd0";
			peers = new ArrayList<process>(Arrays.asList(p1,p2));
			Log.d("MainActivity","isAvd0");
		}
		else if(0==portStr.compareTo("5556"))
		{
			me="avd1";
			peers = new ArrayList<process>(Arrays.asList(p0,p2));
			Log.d("MainActivity","isAvd1");
		}
		else if(0==portStr.compareTo("5558"))
		{
			me="avd2";
			peers = new ArrayList<process>(Arrays.asList(p0,p1));
			Log.d("MainActivity","isAvd2");
		}
		else
		{	
			Log.e("MainActivity","Avd wrong");
			Log.e("MainActivity",portStr);
			me = null;
			peers = null;
		}	
		
		TextView textView =(TextView)findViewById(R.id.textView1);
		CrdtCounter counter = new CrdtCounter(me,peers,textView);
		
		return counter;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    } 
     
}
