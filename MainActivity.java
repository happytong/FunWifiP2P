package com.example.funpatternwifi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
//import android.location.LocationRequest;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;

import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    String TAG="[dbg]MainActivity";
    Button btnDisconnect, btnDiscover, btnSend;
    CheckBox cbHost;
    ListView listViewDevice;
    TextView tvConnection, tvReadMsg;
    EditText editTextSend;
    WifiP2pManager manager;
    WifiP2pManager.Channel channel;
    IntentFilter filter;
    WifiManager wifiManager;
    WiFiDirectBroadcastReceiver receiver;
    List<WifiP2pDevice> listPeers = new ArrayList<WifiP2pDevice>();
    String[] deviceNames;
    WifiP2pDevice[] devices;

    static final int MESSAGE_READ_STRING_HOST = 1;  //message with string type
    static final int MESSAGE_READ_STRING_INFO = 2;
    static final int MESSAGE_READ_DATA = 10;    //including with binary data (hex)
    static final String WELCOME_INFO = "Hello FunPatternWifi";
    static final int MESSAGE_CLEAR = 100; //clear the received message
    ServerClass serverClass;  //group owner thread after the wifi p2p group formed
    ClientClass clientClass;  //client thread after the wifi p2p group formed
    //RecvThread[] recvThreads; //receiving thread to receive data from the peers connected
    List<RecvThread> recvThreads = new ArrayList<>();
    int clientCount=0;  //how many clients in the group

    //group owner info for data exchange
    InetAddress groupOwnerAddress = null;
    String hostDeviceName;
    int nGroupState = 0; //0-not formed; 1-group owner; 2-group clients; 10-trying to connect...
    String myDeviceName;
    String myIp;


    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            byte[] readBuf;
            String str;
            switch (message.what)
            {
                case MESSAGE_READ_STRING_HOST: //host name
                    readBuf = (byte[])message.obj;
                    str = new String(readBuf, 0, message.arg1);
                    hostDeviceName = GetSubstringUntil(str, ':');
                    DisplayStatus((myDeviceName==null ? "This is Client": myDeviceName)+" -> "+ hostDeviceName, ContextCompat.getColor(getApplicationContext(), R.color.Color_Client));
                    break;
                case MESSAGE_READ_STRING_INFO:
                    readBuf = (byte[])message.obj;
                    str = new String(readBuf, 0, message.arg1);
                    String ori = tvReadMsg.getText().toString();
                    tvReadMsg.setText(str + "\n" + ori);
                    break;
                case MESSAGE_CLEAR:
                    tvReadMsg.setText("");
                    break;
            }

            return true;
        }
    });
    public static String GetSubstringUntil(String str, char splitChar) {
        int splitIndex = str.indexOf(splitChar);
        if (splitIndex != -1) {
            return str.substring(0, splitIndex);
        }
        return str;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)) {
            Log.e(TAG, "Wi-Fi Direct is not supported by this device.");
            Toast.makeText(getApplicationContext(),"Wi-Fi Direct is not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        Init();

        CheckPermission();

        StartListener();

        btnDisconnect.performClick();
    }


    private void enableLocation() { //not necessary, trying to solve Samsung A13 discoverPeers failed ???

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            // Location Services API version is 11.0.0 or above
            Log.i(TAG, "Location Services API version is 11.0.0 or above: " + android.os.Build.VERSION.SDK_INT);
        } else {
            // Location Services API version is below 11.0.0
            Log.i(TAG, "Location Services API version is below 11.0.0: " + android.os.Build.VERSION.SDK_INT);
        }

        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {
            // Google Play Services is available
            Log.i(TAG, "Google Play Services is available");
        } else {
            // Google Play Services is not available
            Log.i(TAG, "Google Play Services is NOT available");
        }

        //GPS location service is enabled?
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(30 * 1000);
        locationRequest.setFastestInterval(5 * 1000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        builder.setAlwaysShow(true);

        Task<LocationSettingsResponse> result =
                LocationServices.getSettingsClient(this).checkLocationSettings(builder.build());

        result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(Task<LocationSettingsResponse> task) {
                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    // All location settings are satisfied. The client can initialize location
                    // requests here.
                    Log.i(TAG, "LocationSettingsResponse ok" );
                } catch (ApiException exception) {
                    switch (exception.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            Log.i(TAG, "got LocationSettingsResponse RESOLUTION_REQUIRED exception" );
                            // Location settings are not satisfied. But could be fixed by showing the
                            // user a dialog.

                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            Log.i(TAG, "got LocationSettingsResponse SETTINGS_CHANGE_UNAVAILABLE" );
                            // Location settings are not satisfied. However, we have no way to fix the
                            // settings so we won't show the dialog.
                            break;
                    }
                }
            }
        });
    }
    private void CheckPermission() {

        //to on/off wifi?
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "CHANGE_WIFI_STATE was not granted");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CHANGE_WIFI_STATE},
                    1);
        }

        //huawei Mate 20 pro (EMUI 12 = android 10) should remove ACCESS_COARSE_LOCATION
        /*if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    1);
        }*/
        // not required for Build.VERSION_CODES.TIRAMISU or later?
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU ) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, " <TIRAMISU: ACCESS_FINE_LOCATION not granted");
                // Permission is not granted
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        2);
            }
        }
        else
        {
            if (checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "TIRAMISU+: NEARBY_WIFI_DEVICES granted");
                // Permission is granted, proceed with using the NEARBY_WIFI_DEVICES permission
                // ...
            } else {
                Log.i(TAG, "TIRAMISU+: NEARBY_WIFI_DEVICES not granted");
                // Permission is not granted, request the location permission again
                /*ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        1);*/
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.NEARBY_WIFI_DEVICES},
                        3);
            }
        }

        //enableLocation();  //just to try for Samsung A13
    }

    private void Init() {
        btnDisconnect = (Button)findViewById(R.id.disconnect);
        btnDiscover = (Button)findViewById(R.id.discover);
        btnSend = (Button)findViewById(R.id.sendButton);
        listViewDevice = (ListView) findViewById(R.id.peerListView);
        tvConnection = (TextView) findViewById(R.id.connectionStatus);
        tvReadMsg = (TextView) findViewById(R.id.readMsg);
        editTextSend = (EditText) findViewById(R.id.writeMsg);
        cbHost = (CheckBox) findViewById(R.id.checkBoxHost);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        //wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        //receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);

        filter = new IntentFilter();
        //filter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            manager.requestDeviceInfo(channel, new WifiP2pManager.DeviceInfoListener() {
                @Override
                public void onDeviceInfoAvailable(WifiP2pDevice device) {
                    if (device != null) {
                        myDeviceName = device.deviceName;
                        Log.i(TAG, "requestDeviceInfo: "+myDeviceName);
                        Toast.makeText(getApplicationContext(), "my device: " + myDeviceName, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
    void DisplayStatus(String info, int color)
    {
        tvConnection.setText(info);
        tvConnection.setTextColor(color);
    }
    private void StartListener() {
        btnDisconnect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Disconnect from the current group
                    manager.removeGroup(channel, disconnectListener);
                    //btnDiscover.setEnabled(true);

                    if (nGroupState != 0) {
                        manager.requestGroupInfo(channel,groupInfoListener);
                    }
                }
            }
        );
        btnDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                manager.discoverPeers(channel, new WifiP2pManager.ActionListener() { //ensure that the list of peers is up to date
                    @Override
                    public void onSuccess() {
                        DisplayStatus("Discovery started", ContextCompat.getColor(getApplicationContext(), R.color.Color_Info));
                        //btnDiscover.setEnabled(false);
                    }

                    @Override
                    public void onFailure(int i) {
                        DisplayStatus("Discovery starting failed: " +i, ContextCompat.getColor(getApplicationContext(), R.color.Color_Info));
                    }
                });
            }
        });

        listViewDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                if (nGroupState == 10)
                {
                    btnDisconnect.performClick();
                    return;
                }
                else if (nGroupState != 0)
                {
                    Toast.makeText(getApplicationContext(), (nGroupState == 1?"Host here":"Client here"), Toast.LENGTH_SHORT).show();
                    return;
                }

                final WifiP2pDevice device = devices[i];
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                //config.groupOwnerIntent = cbHost.isChecked()?15:0;
                Log.i(TAG, "listViewDevice click: state to 10 from "+nGroupState);

                nGroupState = 10;
                manager.connect(channel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        DisplayStatus("Connecting...", ContextCompat.getColor(getApplicationContext(), R.color.Color_Info));
                        Toast.makeText(getApplicationContext(), "Connected to " + device.deviceName, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int i) {
                        Toast.makeText(getApplicationContext(), "Disconnected " + device.deviceName, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String msg = editTextSend.getText().toString();
                    if (recvThreads.size() > 0){//recvThread != null && recvThread.isAlive()) {
                        Log.i(TAG, "SendStringTask: " + msg);
                        for (RecvThread thread : recvThreads) {
                            new SendStringTask(thread.socket).execute(msg);
                            Toast.makeText(getApplicationContext(), "Send to "+thread.ipAddressInput, Toast.LENGTH_SHORT).show();
                        }
                    }else {
                        Log.i(TAG, nGroupState + " no SendStringTask: " + msg);
                        Toast.makeText(getApplicationContext(), "No connection at state "+nGroupState, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );

        cbHost.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.i(TAG, "cbHost setOnCheckedChangeListener: "+isChecked +", state "+nGroupState);
                if (isChecked) {
                    if (nGroupState == 0)
                    {
                        manager.createGroup(channel, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                Log.i(TAG, "cbHost setOnCheckedChangeListener: createGroup ok" + ", state to 1 from "+nGroupState);
                                // The group was created successfully, and this device is the group owner
                                DisplayStatus(myDeviceName==null ? "This is Host": myDeviceName + ": Host", ContextCompat.getColor(getApplicationContext(), R.color.Color_Host));
                                nGroupState = 1;
                                //cbHost.setChecked(true);
                                serverClass = new ServerClass();
                                serverClass.start();
                            }

                            @Override
                            public void onFailure(int reason) {
                                Log.i(TAG, "cbHost setOnCheckedChangeListener: createGroup failed" + ", state "+nGroupState);
                                // The group creation failed
                                cbHost.setChecked(false);
                            }
                        });
                    }
                    else if (nGroupState != 1){
                        Log.i(TAG, "cbHost setOnCheckedChangeListener: set unchecked, state "+nGroupState);
                        cbHost.setChecked(false);
                        Toast.makeText(getApplicationContext(), "Disconnect first, state: "+nGroupState, Toast.LENGTH_SHORT).show();
                    }

                } else {
                    // Checkbox is unchecked, do something else
                    Log.i(TAG, "cbHost setOnCheckedChangeListener: unchecked, state "+nGroupState);
                    if (nGroupState == 1){
                        btnDisconnect.performClick();
                    }
                }
            }
        });
    }
    WifiP2pManager.GroupInfoListener groupInfoListener = new WifiP2pManager.GroupInfoListener() {
        @Override
        public void onGroupInfoAvailable(WifiP2pGroup group) {
            //List<WifiP2pDevice> clients = (List<WifiP2pDevice>) group.getClientList();
            if (group != null) {
                Collection<WifiP2pDevice> clients = group.getClientList();
                int numClients = clients.size();
                Log.d(TAG, "onGroupInfoAvailable: Number of clients connected: " + numClients);
                Toast.makeText(getApplicationContext(), "Group size " + numClients, Toast.LENGTH_SHORT).show();
                //only host can get client info
            }
            else {
                Log.d(TAG, "onGroupInfoAvailable: no group but size "+recvThreads.size());
                Toast.makeText(getApplicationContext(), "no group", Toast.LENGTH_SHORT).show();
                ResetData();
            }
        }
    };
    WifiP2pManager.ActionListener disconnectListener = new WifiP2pManager.ActionListener() {
        @Override
        public void onSuccess() {
            // The group was successfully removed
            Log.i(TAG, "disconnectListener");
            ResetData();
        }

        @Override
        public void onFailure(int reason) {
            // The group removal failed
        }
    };
    public void ResetData()
    {
        Log.i(TAG, "ResetData at state to 0 from " + nGroupState);
        listPeers.clear();

        ArrayAdapter<String> adapter = (ArrayAdapter<String>) listViewDevice.getAdapter();
        if (adapter != null) {
            Arrays.fill(deviceNames, null);
            adapter.notifyDataSetChanged();
        }

        if (nGroupState == 1)
        {
            Log.i(TAG, "1 ResetData recvThread.interrupt");
            //if (clientCount > 0) {
                for (RecvThread thread: recvThreads) thread.interrupt();
                serverClass.close();
            //}
        }
        else if (nGroupState == 2)
        {
            Log.i(TAG, "2 ResetData recvThread.interrupt");
            for (RecvThread thread: recvThreads) thread.interrupt();
        }
        nGroupState = 0;
        DisplayStatus("Device disconnected", ContextCompat.getColor(getApplicationContext(), R.color.Color_Info));
        //btnDiscover.setEnabled(true);
        cbHost.setChecked(false);
        hostDeviceName = "";
        handler.obtainMessage(MESSAGE_CLEAR).sendToTarget();
    }
    public String getConnectionStatus(int status) {
        switch (status) {
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown " + status;
        }
    }

    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
            boolean bConnected = false;
            if (!wifiP2pDeviceList.getDeviceList().equals(listPeers)) {
                listPeers.clear();
                listPeers.addAll(wifiP2pDeviceList.getDeviceList());

                deviceNames = new String[wifiP2pDeviceList.getDeviceList().size()];
                devices = new WifiP2pDevice[wifiP2pDeviceList.getDeviceList().size()];

                int index=0;
                for (WifiP2pDevice device : wifiP2pDeviceList.getDeviceList())
                {
                    if (device.isGroupOwner())
                        deviceNames[index] = device.deviceName + " (Host): "+ getConnectionStatus(device.status);
                    else
                        deviceNames[index] = device.deviceName + " "+device.deviceAddress + " :" + getConnectionStatus(device.status);
                    devices[index] = device;
                    index++;
                    if (WifiP2pDevice.CONNECTED == device.status)  bConnected = true;

                    Log.i(TAG, "peerListListener: " + index + " " + device.deviceName + " :" + getConnectionStatus(device.status));
                }

                if (bConnected) manager.requestGroupInfo(channel, groupInfoListener);

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, deviceNames );
                listViewDevice.setAdapter(adapter);
            }

            if (listPeers.size() == 0)
            {
                Toast.makeText(getApplicationContext(), "No device found", Toast.LENGTH_SHORT).show();
            }
        }
    };

    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            //only group owner IP is available
            //only 1 group owner in a group, can have multiple clients
            groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;

            if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner)
            {
                Log.i(TAG, "connectionInfoListener - Host: state to 1 from " + nGroupState);
                //show host
                DisplayStatus(myDeviceName==null ? "This is Host": myDeviceName + ": Host", ContextCompat.getColor(getApplicationContext(), R.color.Color_Host));
                nGroupState = 1;
                cbHost.setChecked(true);
                Toast.makeText(getApplicationContext(), "Group Owner: " + groupOwnerAddress, Toast.LENGTH_SHORT).show();

                //check if the server thread is running
                if (serverClass != null && serverClass.isAlive()) {
                    // ServerClass thread is still running
                    Log.i(TAG, "connectionInfoListener - Host: serverClass is running, recvThreads " + recvThreads.size());
                } else {
                    //android 13 (API 33): after createGroup, first disconnected then connected
                    //android 10 (API 29): after creategroup, get connected directly so no need this part
                    // ServerClass thread has already quit
                    Log.i(TAG, "connectionInfoListener - Host: serverClass is not running, start again");
                    serverClass = new ServerClass();
                    serverClass.start();
                }

            }
            else if (wifiP2pInfo.groupFormed)
            {
                Log.i(TAG, "connectionInfoListener - Client: state to 2 from " + nGroupState + ", clientClass.start");
                Toast.makeText(getApplicationContext(), "To Group Owner: " + groupOwnerAddress, Toast.LENGTH_SHORT).show();
                DisplayStatus((myDeviceName==null ? "This is Client": myDeviceName)+" -> "+ hostDeviceName, ContextCompat.getColor(getApplicationContext(), R.color.Color_Client));
                nGroupState = 2;
                cbHost.setChecked(false);

                clientClass = new ClientClass(groupOwnerAddress);
                clientClass.start();

                manager.requestGroupInfo(channel, groupInfoListener);
            }
            else
            {
                Log.i(TAG, "connectionInfoListener: no group, state to 0 from " + nGroupState + " GO="+groupOwnerAddress);
                nGroupState = 0;
                DisplayStatus("No group formed", ContextCompat.getColor(getApplicationContext(), R.color.Color_Info));
            }
        }
    };

    /** register the BroadcastReceiver with the intent values to be matched */
    @Override
    public void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    public class ServerClass extends Thread {
        Socket socket;
        ServerSocket serverSocket;

        public void close() {
            Log.i(TAG, "ServerClass close: socket "+ (serverSocket != null ? serverSocket.toString():"NULL") );
            interrupt();
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            //super.run();
            try {
                serverSocket = new ServerSocket(16002);
                Log.i(TAG, "ServerClass run: socket "+serverSocket.toString() );
                //clientCount = 0;
                while (!Thread.interrupted()  ) {
                    Log.i(TAG, "ServerClass waiting at accept: socket "+serverSocket.toString() );
                    socket = serverSocket.accept();
                    Log.i(TAG, "ServerClass recvThread.start " + recvThreads.size() + ", socket "+serverSocket.toString() );
                    //Toast.makeText(getApplicationContext(), "Accept "+socket.getInetAddress().toString(), Toast.LENGTH_SHORT).show();

                    new SendStringTask(socket).execute(WELCOME_INFO);

                    recvThreads.add(new RecvThread(socket));
                    //recvThreads[clientCount] = new RecvThread(socket);
                    recvThreads.get(recvThreads.size()-1).start();
                }
            }
            catch (IOException e)
            {
                Log.i(TAG, "ServerClass IOException " + e.toString() + ": socket "+ (serverSocket!=null?serverSocket.toString():"NULL") );
                e.printStackTrace();
            }
            close();
            Log.i(TAG, "serverClass quit " + myIp + " recvThreads.clear");
            recvThreads.clear();
            //clientCount = 0;
        }
    }

    public class ClientClass extends Thread {
        Socket socket;
        String hostAddress;
        public ClientClass(InetAddress inetAddress)  {
            hostAddress = inetAddress.getHostAddress();
            socket = new Socket();
        }
        public void closeSocket() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void run() {
            //super.run();
            try {
                Log.i(TAG, "ClientClass recvThread.start " + recvThreads.size() + " (0?) "+hostAddress + ", s="+ socket.toString());

                socket.connect(new InetSocketAddress(hostAddress, 16002), 1000);
                Log.i(TAG, "ClientClass socket connected " +socket.toString());

                recvThreads.add ( new RecvThread(socket) );
                recvThreads.get(0).start();
                //clientCount = 1;

                recvThreads.get(0).join();
            }
            catch (IOException e)
            {
                e.printStackTrace();
                Log.i(TAG, "ClientClass IOException " +e.toString());
            } catch (InterruptedException e) {
                Log.i(TAG, "ClientClass InterruptedException " +e.toString());
                throw new RuntimeException(e);
            }

            closeSocket();
            Log.i(TAG, "clientClass quit "+myIp + ", recvThreads.clear");
            recvThreads.clear();
            //clientCount = 0;
        }
    }
    private class RecvThread extends Thread {
        Socket socket;
        InputStream inputStream;
        //OutputStream outputStream;
        String ipAddressInput;
        //String myIp;

        public RecvThread(Socket sk)
        {
            socket = sk;
            try {
                inputStream= socket.getInputStream();
                ipAddressInput = socket.getInetAddress().getHostAddress();
                myIp = socket.getLocalAddress().toString();
            }
            catch (IOException e)
            {
                Log.i(TAG, "RecvThread IOException: "+e.toString());
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            byte[] buffer=new byte[1024];
            int bytes = 0;

            while (socket != null && !socket.isClosed() && !Thread.currentThread().isInterrupted())
            {
                try {
                    bytes = inputStream.read(buffer);
                    if (bytes > 0){
                        String string = new String(buffer, StandardCharsets.UTF_8);
                        if (string.contains(WELCOME_INFO)) handler.obtainMessage(MESSAGE_READ_STRING_HOST, bytes, -1, buffer).sendToTarget();
                        else handler.obtainMessage(MESSAGE_READ_STRING_INFO, bytes, -1, buffer).sendToTarget();
                        Log.i(TAG, myIp + " RecvThread obtainMessage: "+ new String(buffer, 0, bytes, StandardCharsets.UTF_8) + " - from " + ipAddressInput);
                    }

                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            Log.i(TAG, "recvThread quit to " + ipAddressInput);

        }
    }

    private class SendStringTask extends AsyncTask<String, Void, Void> {
        private Socket socket;
        String ipAddressOutput;

        public SendStringTask(Socket sk) {
            ipAddressOutput = sk.getInetAddress().getHostAddress();
            socket = sk;
        }

        @Override
        protected Void doInBackground(String... params) {
            String msg = params[0];
            try {
                if (socket == null)
                {
                    Log.i(TAG, "SendStringTask do nothing to " + ipAddressOutput);
                    return null;
                }
                Log.i(TAG, "SendStringTask write with socket "+ socket);
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write((myDeviceName+": "+msg).getBytes());
            } catch (IOException e) {
                Log.i(TAG, "SendStringTask IOException: "+ e.toString());
                e.printStackTrace();
            }
            return null;
        }
    }
    private class SendHexTask extends AsyncTask<Void, Void, Void> {
        private Socket socket;
        private byte[] data;
        private int length;

        public SendHexTask(Socket sk, byte[] data, int length) {
            this.socket = sk;
            this.data = data;
            this.length = length;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (socket == null) {
                    Log.i(TAG, "SendHexTask do nothing");
                    return null;
                }
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(data, 0, length);
            } catch (IOException e) {
                Log.i(TAG, "SendHexTask IOException: "+ e.toString());
                e.printStackTrace();
            }
            return null;
        }
    }
}