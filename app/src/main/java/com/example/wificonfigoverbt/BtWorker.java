package com.example.wificonfigoverbt;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BtWorker extends Thread {
    private final UUID uuid = UUID.fromString("815425a5-bfac-47bf-9321-c5ff980b5e11");
    private final byte delimiter = 124; // 124d is '|' ascii representation
    private int readBufferPosition = 0;

    private Context context;

    private BluetoothSocket mmSocket;
    private BluetoothDevice device;
    private Command command;

    public BtWorker(Command cmd, BluetoothDevice bt_device, Context context){
        this.command = cmd;
        this.device = bt_device;
        this.context = context;
    }

    public void run(){
        //json
        Gson gson = new Gson();

        //bluetooth
        clearOutput();
        writeOutput("Starting config update.");

        writeOutput("Device: " + device.getName() + " - " + device.getAddress());

        try {
            mmSocket = device.createRfcommSocketToServiceRecord(uuid);
            if (!mmSocket.isConnected()) {
                mmSocket.connect();
                Thread.sleep(1000);
            }

            //now we are connected to bluetooth socket
            writeOutput("Connected. Starting request...");

            // create I/O-streams
            OutputStream mmOutputStream = mmSocket.getOutputStream();
            InputStream mmInputStream = mmSocket.getInputStream();
            // send request
            mmOutputStream.write(gson.toJson(command).getBytes());
            mmOutputStream.flush();
            // receive response
            waitForResponse(mmInputStream, -1);
            //clean up
            mmSocket.close();
            writeOutput("Finished Request.");

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

            writeOutput("Failed.");
        }

        writeOutput("Done.");
    }

    private void writeOutput(String text) {
        Log.i("INFO","WROTE: " + text);
        /*
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String currentText = messageTextView.getText().toString();
                messageTextView.setText(currentText + "\n" + text);
            }
        });
        */
    }

    private void clearOutput() {
        Log.i("INFO","OUTPUT cleared");
        /*
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageTextView.setText("");
            }
        });
        */
    }

    /*
     * TODO actually use the timeout
     */
    private void waitForResponse(InputStream mmInputStream, long timeout) throws IOException {
        int bytesAvailable;

        while (true) {
            bytesAvailable = mmInputStream.available();
            if (bytesAvailable > 0) {
                byte[] packetBytes = new byte[bytesAvailable];
                byte[] readBuffer = new byte[1024];
                mmInputStream.read(packetBytes);

                for (int i = 0; i < bytesAvailable; i++) {
                    byte b = packetBytes[i];

                    if (b == delimiter) {
                        byte[] encodedBytes = new byte[readBufferPosition];
                        System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                        final String data = new String(encodedBytes, "US-ASCII");

                        process_data(data);

                        return;
                    } else {
                        readBuffer[readBufferPosition++] = b;
                    }
                }
            }
        }
    }

    private void process_data(String data){
        switch (this.command.getCommand()){
            case "WIFI_SCAN":
                //Send networks[] json String to MainActivity
                sendNets(data);
                break;
            case "WIFI_SET":
                ((Activity)this.context).runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(context,
                                "New IP Address: " + data, Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            default:
                break;
        }
    }

    private void sendNets(String network_json_string){
        Intent intent = new Intent("nets_received_event");
        // You can also include some extra data.
        intent.putExtra("nets", network_json_string);
        LocalBroadcastManager.getInstance(this.context).sendBroadcast(intent);
    }
}
