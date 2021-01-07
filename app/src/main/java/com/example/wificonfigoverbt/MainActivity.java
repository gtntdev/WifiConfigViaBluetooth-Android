package com.example.wificonfigoverbt;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Network;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    //layout
    private Spinner devicesSpinner;
    private LinearLayout ll_network_root;
    private Button btn_refresh_bt_devices, btn_scan_wifi_nets, btn_set_new_net;

    //manage radio buttons group manually, because of future improvements
    //(showing more details in the root view)
    private LinkedList<RadioButton> wifi_rbtn_list = new LinkedList<>();
    private WifiNetwork[] wifi_nets;
    private RadioButton rbtn_sel = null;

    private DeviceAdapter adapter_devices;

    private BroadcastReceiver mMessageReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //init layout stuff
        devicesSpinner = (Spinner) findViewById(R.id.devices_spinner);
        ll_network_root = (LinearLayout) findViewById(R.id.ll_neworks);
        btn_refresh_bt_devices = (Button) findViewById(R.id.refresh_devices_button);
        btn_scan_wifi_nets = (Button) findViewById(R.id.scan_button);
        btn_set_new_net = (Button) findViewById(R.id.set_button);

        //add listeners
        btn_refresh_bt_devices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshDevices();
            }
        });

        btn_scan_wifi_nets.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Command scan = new Command("WIFI_SCAN");
                BluetoothDevice device = (BluetoothDevice) devicesSpinner.getSelectedItem();
                BtWorker btw = new BtWorker(scan, device, view.getContext());
                btw.start();
            }
        });

        btn_set_new_net.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //set Wifi
                WifiNetwork wifi_net = null;
                setWifi(wifi_net);
            }
        });

        //add intent receiver
        //init ovserver
        init_observer();
        //register observer to incoming events
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("nets_received_event"));

        //load BT devices
        refreshDevices();
    }

    private void refreshDevices() {
        adapter_devices = new DeviceAdapter(this, R.layout.spinner_devices, new ArrayList<BluetoothDevice>());
        devicesSpinner.setAdapter(adapter_devices);

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                adapter_devices.add(device);
            }
        }
    }

    private void init_observer(){
        mMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Get extra data included in the Intent
                String message = intent.getStringExtra("nets");
                //Log.d("receiver", "Got message: " + message);
                Gson gson = new Gson();
                ll_network_root.removeAllViews();
                wifi_nets = gson.fromJson(message, WifiNetwork[].class);
                wifi_rbtn_list.clear();
                rbtn_sel = null;
                for(WifiNetwork c : wifi_nets){
                    //update gui
                    LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    LinearLayout myLayout = (LinearLayout) inflater.inflate(R.layout.network_entry, null);
                    TextView tv = myLayout.findViewById(R.id.net_name);
                    RadioButton rbtn = myLayout.findViewById(R.id.net_set);
                    rbtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            RadioButton rbtn = (RadioButton) view;
                            if(rbtn.isChecked()){
                                rbtn_sel = rbtn;
                                for(RadioButton i : wifi_rbtn_list){
                                    if(!i.equals(rbtn))
                                        i.setChecked(false);
                                }
                            }
                        }
                    });
                    wifi_rbtn_list.add(rbtn);
                    tv.setText(c.toString());
                    ll_network_root.addView(myLayout);
                }
            }
        };
    }

    private void setWifi(WifiNetwork wifi_net){
        Context context = this;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Connect to Wifi");
        builder.setMessage("Enter Password:.");
        builder.setCancelable(true);
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setNegativeButton(
                "Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        builder.setPositiveButton(
                "Connect",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String password = input.getText().toString();
                        if(rbtn_sel == null){
                            //alert user and cancel
                            dialog.cancel();
                        }
                        WifiNetwork wifi_net_sel = wifi_nets[indexOf(wifi_rbtn_list, rbtn_sel)];
                        // send cmd "SET_WIFI"
                        //Log.i("WIFI to connect",wifi_net_sel.getSsid()
                        //        + ", PSK: " + password);
                        Command set = new Command("WIFI_SET", wifi_net_sel.getSsid(), password);
                        BluetoothDevice device = (BluetoothDevice) devicesSpinner.getSelectedItem();
                        BtWorker btw = new BtWorker(set, device, context);
                        btw.start();
                        dialog.cancel();
                    }
                });

        AlertDialog alert = builder.create();
        alert.show();
    }

    private static <T> int indexOf(LinkedList<T> ll, T elem){
        if(ll.size() == 0)
            return -1;
        T next = ll.getFirst();
        for(int i = 0; i < ll.size(); i++){
            if(elem.equals(ll.get(i)))
                return i;
        }
        return -1;
    }
}