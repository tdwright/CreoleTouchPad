package co.uk.tdwright.creole;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class CreoleConnectionService extends Service
{
	private static final String INNER_TAG = "CreoleConnectionService";
    private ServerSocket serverSocket;
    private Socket clientSocket;
	public static final int SERVERPORT = 5574;

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
    	CreoleConnectionService getService() {
            return CreoleConnectionService.this;
        }
    }
	
	// Binder given to clients
	private final IBinder mBinder = new LocalBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {

		Thread tAccept = new Thread(){
			public void run() {
				
				Thread tListen = new Thread(){
					public void run() {
						try {
//							while(true) {
//								BufferedReader inStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
//								if(inStream.ready()) {
//									broadcastIntensity(inStream.readLine());
//								}
//							}
								
							while(true) {
								BufferedReader inStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
								String line = inStream.readLine();
								if (line==null) {
									Log.d(INNER_TAG, "PC disconnected");
									broadcastStatus(false);
									break;
								} else if(line.length()>0) {
//									Log.v(INNER_TAG, line);
									broadcastIntensity(line);
								}
							}
						}
						catch (Exception e) {
							Log.d(INNER_TAG, "error, can't listen");
							e.printStackTrace();
						}
					}
				};
				
				try {
					broadcastStatus(false);
					serverSocket = new ServerSocket(SERVERPORT);
					Log.d(INNER_TAG, "waiting for connection");
					while (true) {
						// listen for incoming clients
						clientSocket = serverSocket.accept();
						PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())),true);
						out.println("Hello");
						Log.d(INNER_TAG, "PC connected");
						broadcastStatus(true);
						tListen.run();
					}

				}
				// }
				catch (Exception e) {
					Log.d(INNER_TAG, "error, disconnected");
					e.printStackTrace();
				}
			}
		};
		tAccept.start();
	}

	public void sendCoords (final String coords)
	{
		if (clientSocket.isConnected()) {
//			Log.v(INNER_TAG,clientSocket.toString());
			Thread t = new Thread(){
				public void run(){
					try {
						PrintWriter out;
						out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())),true);
						out.println(coords);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			};
			t.start();
		} else {
			Log.d(INNER_TAG,"Client disconnected");
		}
	}
	
	private void broadcastStatus (Boolean connected)
	{
		Intent i = new Intent("co.uk.tdwright.creole.STATUS_BROADCAST");
		String status;
		if(connected)
		{
			status = "PC connected";
		}
		else
		{
			status = "Waiting for PC to connect...";
		}
		i.putExtra("status", status);
		i.putExtra("connected",connected);
		sendBroadcast(i);
	}
	
	private void broadcastIntensity (String intensity)
	{
		Intent i = new Intent("co.uk.tdwright.creole.INTENSITY_BROADCAST");
		i.putExtra("intensity", intensity);
		sendBroadcast(i);
	}
}