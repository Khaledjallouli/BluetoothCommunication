package com.example.bluetoothcommunication_al_k_team;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;

import android.util.Log;




import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;
import java.lang.String;
public class bluetoothConnectionService {

    private static final String TAG = "bluetoothConn"; //debugging tag
    private static final String appName = "Blue_APP"; // name of the app


    private final BluetoothAdapter mBluetoothAdapter;

    Context mContext;
    private AcceptThread mInsecureAcceptThread;

    private ConnectThread mConnectThread;
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;
    ProgressDialog mProgressDialog;
    private ConnectedThread mConnectedThread;
    public static final UUID MY_UUID_INSECURE = UUID.fromString("123e4567-e89b-12d3-a456-556642440000");
             //UUID.randomUUID(); //random generated number
    public bluetoothConnectionService(Context context) { //constructor
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        start();// call it when object connection created
        // initialiser acceptThread
    }

    // AcceptThread :first thread which will wait  for connection
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;


            try { // to set up the socket

                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
                        appName, MY_UUID_INSECURE);
                Log.d(TAG, "AcceptThread: Setting up server using :" + MY_UUID_INSECURE);


            } catch (IOException e) {
                Log.d(TAG, "run : AcceptThread Running.");
            }
            mmServerSocket = tmp;

        }

        public void run() {

            BluetoothSocket socket = null;
            Log.d(TAG, "run: socket start...");
            // Listen to the server socket if we're not connected

                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept(200); //wait connection go or no
                    // if success tell us otherwise go out
                } catch (IOException e) {
                    Log.e(TAG, "AcceptThread : IOException"+ e.getMessage());

                }

                // If a connection was accepted
                if (socket != null) {
                    connected(socket,mmDevice);
                }
                Log.i(TAG, "END mAcceptThread");

        }
        public void cancel() { // close serversocket
            Log.d(TAG, "cancel : Canceling cceptThread. " );
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel : close of AcceptThread ServerSocket failed");
            }
        }


    }



    private class ConnectThread extends Thread {
        private  BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
          Log.d(TAG,"ConnectThread: started");
            mmDevice = device;
            deviceUUID = uuid;

            }



        public void run() {
            BluetoothSocket tmp = null;

            Log.i(TAG, "Run mConnectThread ");


            // Make a connection to the BluetoothSocket
            try {
                Log.d(TAG,"ConnectThread: Trying to create insecureComm Socket using UUID"+ MY_UUID_INSECURE);
                tmp = mmDevice.createInsecureRfcommSocketToServiceRecord (deviceUUID);

                // This is a blocking call and will only return on a
                // successful connection or an exception

            } catch (IOException e) {
                Log.e(TAG, "ConnectThread: Could not create Insecure Comm socket " + e.getMessage());

            }
            mmSocket = tmp;
            mBluetoothAdapter.cancelDiscovery();

            try{
                mmSocket.connect();
                Log.d(TAG,"run: connectThread connected");
            }

            catch (IOException e) { // close socket
                try {
                    mmSocket.close();
                    Log.d(TAG,"run: closed socket");

                }
                catch (IOException e1){
                    Log.e(TAG,"mConnected Thread:run : unable to close connection.. ");

                }
                Log.d(TAG, "run: connectThread could not connect to uui"+ MY_UUID_INSECURE);

            }
            connected(mmSocket,mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
                Log.d(TAG,"cancel:closing client Socket");
            } catch (IOException e) {
                Log.e(TAG, "cancel : close() of connect failed " );
            }
        }
    }


    public synchronized  void start(){
        //method to start connection service : initiate acceptThread
        Log.d(TAG,"start");
        if (mConnectThread!=null){ // if exist we cancel it and create new one
            mConnectThread.cancel();
            mConnectThread=null;
        }
        if (mInsecureAcceptThread==null){
            mInsecureAcceptThread= new AcceptThread();
            mInsecureAcceptThread.start();
        }
    }
    public void startClient(BluetoothDevice device, UUID uuid){
        Log.d(TAG,"StartCLient: started");

        mProgressDialog= ProgressDialog.show(mContext,"Connecting Bluetooth","Please wait..",true);
        mConnectThread = new ConnectThread(device,uuid);
        mConnectThread.start();
    }




    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread: Starting " );
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                mProgressDialog.dismiss();
            }catch (NullPointerException e){
                Log.e(TAG, "null exception");
                e.printStackTrace();

            }

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;

        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024]; //buffer store from the stream
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) { //listen to connection forever
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer); // convert into  string
                    String incomingMessage = new String(buffer,0,bytes);

                    Log.d(TAG,"InputStream:"+ incomingMessage);
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    //end connection
                    break;
                }
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
        /**
         * Write to the connected OutStream.
         *
         * @param bytes The bytes to write
         */
        public void write(byte[] bytes) { // create string from buffer
          String text = new String(bytes,Charset.defaultCharset());
            Log.d(TAG,"<rite: Writing to outputstream"+text);
            try {  
                mmOutStream.write(bytes);

            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

    }

    private void connected(BluetoothSocket mmSocket, BluetoothDevice mmDevice) {
        Log.d(TAG,"Connected : Starting");
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }

    public void write(byte[] out) {
        ConnectedThread r;
        Log.d(TAG,"Write: write called");
//perfom the write
        mConnectedThread.write(out);
    }

}