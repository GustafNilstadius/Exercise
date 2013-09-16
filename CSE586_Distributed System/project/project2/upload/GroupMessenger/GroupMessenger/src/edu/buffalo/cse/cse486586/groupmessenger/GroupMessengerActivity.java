package edu.buffalo.cse.cse486586.groupmessenger;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
public class GroupMessengerActivity extends Activity {
	 private SequencerTotal protocol;
	 public static OnTest2ClickListener Test2Listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TextView tv = (TextView) findViewById(R.id.textView1);
        EditText et = (EditText) findViewById(R.id.editText1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
       
        SequencerTotal protocol = setupMcast();
        
        findViewById(R.id.button2).setOnClickListener(
                new OnTest1ClickListener(tv,protocol));
        
        Test2Listener= new OnTest2ClickListener(tv,protocol);
        findViewById(R.id.button3).setOnClickListener(
        		Test2Listener);
        
        findViewById(R.id.button4).setOnClickListener(
                new OnSendClickListener(et,tv,protocol));
        
        setupMcast();
    }

    private SequencerTotal setupMcast()
    {
    	TelephonyManager tel =
		        (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		
		String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
		
		String me;
		
		process p0=new process("10.0.2.2", 11108);
		process p1=new process("10.0.2.2", 11112);
		process p2=new process("10.0.2.2", 11116);
		
		ArrayList<process> group = new ArrayList<process>(Arrays.asList(p0,p1,p2));
		//ArrayList<process> group = new ArrayList<process>(Arrays.asList(p0));
		process own;
		process sequencer = p0;
		boolean isleader = false;
		
		if(0==portStr.compareTo("5554"))
		{
			me="avd0";
			own = p0;
			isleader = true;
			Log.d("MainActivity","isAvd0");
		}
		else if(0==portStr.compareTo("5556"))
		{
			me="avd1";
			own = p1;
			Log.d("MainActivity","isAvd1");
		}
		else if(0==portStr.compareTo("5558"))
		{
			me="avd2";
			own = p2;
			Log.d("MainActivity","isAvd2");
		}
		else
		{	
			Log.e("MainActivity","Avd wrong");
			Log.e("MainActivity",portStr);
			own= null;
			me = null;
		}	
		
		TextView textView =(TextView)findViewById(R.id.textView1);
		protocol = 
				new SequencerTotal(own, me,group,sequencer, isleader,textView);
		
		return protocol;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
    
   
    
    
}
