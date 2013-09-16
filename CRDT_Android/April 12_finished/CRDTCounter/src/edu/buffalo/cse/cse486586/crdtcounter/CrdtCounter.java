package edu.buffalo.cse.cse486586.crdtcounter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

public class CrdtCounter {
	final int SERVER_PORT = 10000;
	final String CMD_ASK = "ask";
	final String CMD_UPDATEP = "updatep";
	final String CMD_UPDATEN = "updaten";
	private int value;
	private HashMap<String, Integer> P;
	private HashMap<String, Integer> N;
	private String myId;
	private TextView mtext;
	private ArrayList<process> peers;

	public CrdtCounter(String Id, ArrayList<process> group, TextView textView) {
		myId = Id;
		mtext = textView;
		peers = group;

		P = new HashMap<String, Integer>();
		N = new HashMap<String, Integer>();
		value = 0;
		P.put(myId, 0);
		N.put(myId, 0);
		// startup server
		ServerSocket welcomeSocket;

		try {

			welcomeSocket = new ServerSocket(SERVER_PORT);
			Log.d(myId, "build server success");
			new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
					welcomeSocket);
		} catch (IOException e) {
			Log.e(myId, "create server socket error");
			e.printStackTrace();
		}

		mtext.setText(Integer.toString(value));
		// ask for update from peers
		AskFromPeers();

	}

	public void increment() {
		Integer i;
		i = P.get(myId);
		if (null == i) {
			P.put(myId, 0);
		} else {
			i++;
			P.put(myId, i);
		}
		update_local();
		mtext.setText(Integer.toString(value));
		updatep_peers();
	}

	public void decrement() {
		Integer i;
		i = N.get(myId);
		if (null == i) {
			N.put(myId, 0);
		} else {
			i++;
			N.put(myId, i);
		}
		update_local();
		mtext.setText(Integer.toString(value));
		updaten_peers();
	}

	private void update_local() {
		int temp_value = 0;
		for (Integer v : P.values()) {
			temp_value = temp_value+v;
		}
		for (Integer v : N.values()) {
			temp_value = temp_value-v;
		}
		value = temp_value;
		
	}

	private void updatep_peers() {
		boolean send = false;
		String cmd;
		cmd = CMD_UPDATEP + "\n";
		for (Map.Entry<String, Integer> entry : P.entrySet()) {
			String k = entry.getKey();
			Integer v = entry.getValue();
			cmd = cmd + k + "\n";
			cmd = cmd + Integer.toString(v) + "\n";
			send = true;
		}
		if (send)
			new ClientTask().execute(cmd);
	}

	private void updaten_peers() {
		boolean send = false;
		String cmd;
		cmd = CMD_UPDATEN + "\n";
		for (Map.Entry<String, Integer> entry : N.entrySet()) {
			String k = entry.getKey();
			Integer v = entry.getValue();
			cmd = cmd + k + "\n";
			cmd = cmd + Integer.toString(v) + "\n";
			send = true;
		}
		if (send)
			new ClientTask().execute(cmd);
	}
	
	private void mergep(HashMap<String,Integer> to_merge)
	{
		boolean update_value=false;
		for (Map.Entry<String, Integer> entry : to_merge.entrySet()) {
			String k = entry.getKey();
			Integer v = entry.getValue();
			Integer orginal_value;
			orginal_value =P.get(k);
			if(null ==orginal_value )
			{
				P.put(k, v);
				update_value = true;
			}else
			{
				if(orginal_value<v)
				{
					P.put(k,v);
					update_value = true;
				}
			}
			
		}
		if(update_value)
			update_local();
		return;
	}
	
	private void mergen(HashMap<String,Integer> to_merge)
	{
		boolean update_value=false;
		for (Map.Entry<String, Integer> entry : to_merge.entrySet()) {
			String k = entry.getKey();
			Integer v = entry.getValue();
			Integer orginal_value;
			orginal_value =N.get(k);
			if(null ==orginal_value )
			{
				N.put(k, v);
				update_value = true;
			}else
			{
				if(orginal_value<v)
				{
					N.put(k,v);
					update_value = true;
				}
			}
			
		}
		if(update_value)
			update_local();
		return;
	}

	public int query() {
		return value;
	}

	private void merge() {
		return;
	}

	private void AskFromPeers() {
		if (null != peers) {
			new ClientTask().execute(CMD_ASK);
		}
	}

	private class ClientTask extends AsyncTask<String, String, Void> {
		protected Void doInBackground(String... msgs) {
			for (process p : peers) {
				try {
					Socket socket = new Socket();
					socket.connect(new InetSocketAddress(p.ip, p.port), 1000);
					Log.d(myId, "client socket build success");
					PrintWriter out = new PrintWriter(new BufferedWriter(
							new OutputStreamWriter(socket.getOutputStream())),
							true);
					out.print(msgs[0]);
					out.flush();
					socket.close();
				} catch (SocketTimeoutException e) {
					Log.e(myId, "timeout");
					e.printStackTrace();
				} catch (IOException e) {
					Log.e(myId, "client socket error");
					e.printStackTrace();
				}
			}

			return null;
		}
	}

	private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
		protected Void doInBackground(ServerSocket... sockets) {
			String protocol = null;
			ServerSocket serverSocket = sockets[0];
			Socket socket;
			String key, value_str;
			

			try {
				while (true) {
					socket = serverSocket.accept();
					BufferedReader in = new BufferedReader(
							new InputStreamReader(socket.getInputStream()));
					if (null != (protocol = in.readLine())) {
						if (0 == protocol.compareTo(CMD_ASK)) {
							Log.d(myId,"receive "+CMD_ASK);
							updatep_peers();
							updaten_peers();
						} else if (0 == protocol.compareTo(CMD_UPDATEP)) {
							Log.d(myId,"receive "+CMD_UPDATEP);
							HashMap<String,Integer> p_tomerge=new HashMap<String,Integer>();
							while(null != (key = in.readLine()))
									{
									value_str=in.readLine();
									p_tomerge.put(key, Integer.parseInt(value_str));
									}
							mergep(p_tomerge);
							publishProgress(Integer.toString(value));
						} else if (0 == protocol.compareTo(CMD_UPDATEN)) {
							Log.d(myId,"receive "+CMD_UPDATEN);
							HashMap<String,Integer> n_tomerge=new HashMap<String,Integer>();
							while(null != (key = in.readLine()))
									{
									value_str=in.readLine();
									n_tomerge.put(key, Integer.parseInt(value_str));
									}
							mergen(n_tomerge);
							publishProgress(Integer.toString(value));
						}
					}
				}
			} catch (IOException e) {
				Log.e("ServerTask::doInBackground",
						"server socket accept error");
				e.printStackTrace();
			}
			return null;

		}

		protected void onProgressUpdate(String... strings) {
			mtext.setText(strings[0]);
			return;
		}
	}

}
