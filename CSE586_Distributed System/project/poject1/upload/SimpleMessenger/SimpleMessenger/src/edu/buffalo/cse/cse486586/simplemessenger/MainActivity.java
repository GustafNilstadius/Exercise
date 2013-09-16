package edu.buffalo.cse.cse486586.simplemessenger;

//

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {
	public static boolean isAvd0 = false;
	public final static int listenport = 10000;
	public static int connect_port;
	
	public TextView mTextView; 
	public String aline;
	ServerSocket welcomeSocket;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		TelephonyManager tel =
		        (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		
		String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
		if(0==portStr.compareTo("5554"))
		{
			isAvd0 = true;
			connect_port = 11112;
			Log.d("MainActivity","isAvd0");
		}
		else if(0==portStr.compareTo("5556"))
		{
			isAvd0 = false;
			connect_port = 11108;
			Log.d("MainActivity","isAvd1");
		}
		else
		{	
			Log.e("MainActivity","Avd wrong");
			Log.e("MainActivity",portStr);
		}
		
		mTextView = (TextView) findViewById(R.id.output_message);
		 
		try {
				welcomeSocket = new ServerSocket(listenport);
				Log.d("onCreate","build server success");
				new ServerTask().executeOnExecutor(
						AsyncTask.THREAD_POOL_EXECUTOR,welcomeSocket);
		} catch (IOException e)
 		{
 			Log.e("MainActivity","create server socket error");
 			e.printStackTrace();
 		}
		
	}
	
	private class ServerTask extends AsyncTask<ServerSocket,String,Void>{
		
		protected Void doInBackground(ServerSocket... sockets){
			String msg=null;
			ServerSocket serverSocket = sockets[0];
			Socket socket;
			try 
			{
				while(true)
				{
					socket = serverSocket.accept();
					Log.d("ServerTask::doInBackground","new client");
					BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					while(null !=(msg = in.readLine()))
						{
						Log.d("ServerTask::doInBackground","receive line from client");
						Log.d("ServerTask::doInBackground",msg);
						publishProgress(msg);
						}
					socket.close();
				}
			} catch(IOException e)
			{
				Log.e("ServerTask::doInBackground","server socket accept error");
    			e.printStackTrace();	
			}
			
			return null;		
		}
		
		protected void onProgressUpdate(String... strings)
		{
			//TextView textView =(TextView)FindViewById(R.id.textView1);
			Log.d("ServerTask::onProgressUpdate","receive line from client");
			Log.d("ServerTask::onProgressUpdate",strings[0]);
			mTextView.append(strings[0]+"\n");
			return;
		}
	}
	
	private class ClientTask extends AsyncTask<String,String,Void>{
		
		protected Void doInBackground(String... msgs){
			try {
			Socket socket=new Socket("10.0.2.2", connect_port);
			Log.d("ClientTask::doInBackground","client socket build success");
			PrintWriter out= new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
			out.print(msgs[0]);
			out.flush();
			Log.d("ClientTask::doInBackground","send to server");
			socket.close();
			}catch (IOException e)
			{
				Log.e("ClientTask::doInBackground","client socket error");
				e.printStackTrace();
			}
			
			return null;		
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	/** Called when the user clicks the Send button */
	public void sendMessage(View view) {
		EditText editText = (EditText) findViewById(R.id.edit_message);
		String message = editText.getText().toString();
		Log.d("sendMessage",message);
		editText.setText("");
		
		new ClientTask().executeOnExecutor(
				AsyncTask.THREAD_POOL_EXECUTOR,message);
		
		return;
	}

}
