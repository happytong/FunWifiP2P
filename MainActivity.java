package com.tongs.funpatternwifi;

import static com.tongs.funpatternwifi.FlyingPaths.alPaths;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
//import android.location.LocationRequest;

import android.graphics.Color;
import android.graphics.Point;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/*
this app is for Host to create patterns and distribute to Clients, and then Clients observe the patterns and guess one answer
The result is sent to Host, and Host to show in a list
 */
public class MainActivity extends AppCompatActivity {
    String TAG = "[dbg]MainActivity";
    public enum COLOR_STATUS { //text color of the status bar
        NORMAL,
        OK,
        NOK,
    };
    public enum ROLE {
        NOT_DEFINED,
        HOST,
        PLAYER,
        SELF,  //self training
    };
    static public PlayerList playerList = new PlayerList(); //for Host to update list of player with status on screen
    static List<String> listMacAddressConnected = new ArrayList<>(); //for Host to housekeeping of player list
    static boolean bPlayerReceived = false;  //player received data from host?
    static ROLE nUserRole = ROLE.NOT_DEFINED; //0-Host, 1- Player, 2-Self

    static public CustomPoint ScreenSize;
    Button btnReturn;  //for all 3 layouts
    Button btnDisconnect; //host + player
    Button btnDiscover, btnSend, btnHost, btnCreate, btnReview; //host
    Button btnStartPlaying; //player
    Button btnObserve, btnSetting; //btnCreate //self
    //CheckBox cbHost;
    ListView listViewDevice;
    TextView tvStatus;//, tvReadMsg;
    //EditText editTextSend;

    static WifiP2pHandler wifiP2pHandler = new WifiP2pHandler();
    static final String WELCOME_INFO_HOST = "Hi this is FunPatternWifi host";
    static final String WELCOME_INFO_CLIENT = "Hi this is FunPatternWifi player";
    static final int MESSAGE_CLEAR = 100; //clear the received message
    static final int MESSAGE_UPDATE_DEVICE=101; //update the list view for connection status
    static final int MESSAGE_UPDATE_RESULT=102; //update the list view for test result
    static ServerClass serverClass;  //group owner thread after the wifi p2p group formed
    static ClientClass clientClass;  //client thread after the wifi p2p group formed
    static List<RecvThread> recvThreads = new ArrayList<>();
    static List<SendThread> sendThreads = new ArrayList<>();

    //group owner info for data exchange
    InetAddress groupOwnerAddress = null;

    public enum WIFI_GROUP_STATE {
        NOT_FORMED,
        OWNER,
        CLIENT,
        TRYING_TO_CONNECT,
    };
    static WIFI_GROUP_STATE nGroupState = WIFI_GROUP_STATE.NOT_FORMED; //0-not formed; 1-group owner; 2-group clients; 10-trying to connect...

    String myIp;
    static final int WIFI_DATA_STRING=1;     //payload is a string
    static final int WIFI_DATA_BYTE_ARRAY=2; //payload is byte[]
    static final int WIFI_DATA_OBJECT_POINTS=3;     //payload is Points
        Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            byte[] readBuf;
            String str;
            switch (message.what) {
                case WIFI_DATA_STRING: //host name
                    readBuf = (byte[]) message.obj;
                    str = new String(readBuf, 0, message.arg1);
                    // DEV_NAME>info
                    if (str.contains("Well done"))
                    {
                        UpdateListViewAppend(getDevName(str), Color.BLUE, getResources().getString(R.string.text_result_well_done));
                    }
                    else if (str.contains("Wrong"))
                    {
                        UpdateListViewAppend(getDevName(str), Color.RED,  getResources().getString(R.string.text_result_wrong) );
                    }
                    if (str.contains("Timeout"))
                    {
                        UpdateListViewAppend(getDevName(str), Color.MAGENTA,  getResources().getString(R.string.text_result_timeout));
                    }
                    break;
                case WIFI_DATA_BYTE_ARRAY:
                    readBuf = (byte[]) message.obj;
                    str = new String(readBuf, 0, message.arg1);
                    break;
                case WIFI_DATA_OBJECT_POINTS:
                    bPlayerReceived = true;
                    HighlightButton(btnStartPlaying);
                    break;
                case MESSAGE_UPDATE_DEVICE:
                    if (WifiP2pHandler.devices!=null && WifiP2pHandler.devices.length >0) UpdateListViewYesNo("Connected", "(Host)", Color.BLUE, Color.MAGENTA, Color.LTGRAY);
                    break;
            }
            return true;
        }
    });

    public String getDevName(String str) {  // DEV_NAME>info
        String[] parts = str.split(">");
        if (parts.length > 1) {
            return parts[0].trim();
        } else {
            return null;
        }
    }

    private void UpdateListViewYesNo(String str1, String str2, int color1, int color2, int color3)
    {
        Log.i(TAG, "UpdateListViewYesNo: "+str1 + "/"+ str2);
        CustomAdapter adapter = (CustomAdapter) listViewDevice.getAdapter();
        if (adapter == null) return;
        adapter.updateItemYesNo(str1, str2, color1, color2, color3);
    }
    private void UpdateListViewAppend(String str, int color, String info)
    {
        Log.i(TAG, "UpdateListViewAppend: "+str);
        CustomAdapter adapter = (CustomAdapter) listViewDevice.getAdapter();
        if (adapter == null) return;
        adapter.updateItemAppend(str, color, info);
    }
        private void setListViewColor(int color, String info)
        {
            View item = listViewDevice.getChildAt(0);
            // Get the TextView within the View and set its text color
            TextView textView = item.findViewById(android.R.id.text1);
            textView.setTextColor(color);
            String info1= textView.getText().toString();
            textView.setText(info1+ info);
        }

    private void loadOnePersonScreen()
    {
        btnCreate = (Button) findViewById(R.id.btnCreate);
        btnObserve = (Button) findViewById(R.id.btnObserve);
        btnSetting = (Button) findViewById(R.id.btnSettting);

        UpdateButtonsTraining();

        btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OperationCtrl.nState = OperationCtrl.STATE.CREATE_PATTERN;
                Intent intent = new Intent(MainActivity.this, CreatePattern.class);
                startActivity(intent);
            }
        });

        btnObserve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OperationCtrl.nState = OperationCtrl.STATE.OBSERVE_PATTERN;
                Intent intent = new Intent(MainActivity.this, CreatePattern.class);
                startActivity(intent);
            }
        });
        btnSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ConfigActivity.class);
                startActivity(intent);
            }
        });
    }

    private void loadMainScreen() {
        OperationCtrl.eResult = OperationCtrl.ANSWER_RESULT.NOT_YET;
        int layoutId;
        switch (nUserRole) {
            case HOST:
                layoutId = R.layout.activity_main_host;
                break;
            case PLAYER:
                layoutId = R.layout.activity_main_player;
                break;
            case SELF:
            default:
                layoutId = R.layout.activity_main_self;
                break;
        }
        setContentView(layoutId);
        btnReturn = (Button) findViewById(R.id.btnReturn);
        btnReturn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick (View v){
                if (nUserRole != ROLE.SELF) ResetData();
                OperationCtrl.nState = OperationCtrl.STATE.START_UP;
                LoadStartLayout();
            }
        });

        // Initialize UI and buttons for the loaded layout
        switch (nUserRole) {
            case HOST:
                loadHostScreen();

                if (wifiP2pHandler.receiver == null) {
                    wifiP2pHandler.receiver = new WiFiDirectBroadcastReceiver(wifiP2pHandler.manager, wifiP2pHandler.channel, this);
                    registerReceiver(wifiP2pHandler.receiver, wifiP2pHandler.filter);
                }
                break;
            case PLAYER:
                loadPlayerScreen();
                if (wifiP2pHandler.receiver == null) {

                    wifiP2pHandler.receiver = new WiFiDirectBroadcastReceiver(wifiP2pHandler.manager, wifiP2pHandler.channel, this);
                    registerReceiver(wifiP2pHandler.receiver, wifiP2pHandler.filter);
                }
                break;
            case SELF:
            default:
                loadOnePersonScreen();
                if (wifiP2pHandler.receiver != null) unregisterReceiver(wifiP2pHandler.receiver);
                break;
        }
    }

    private void loadHostScreen()
    {
        btnDisconnect = (Button)findViewById(R.id.buttonDisconnect);
        btnHost = (Button)findViewById(R.id.buttonHost);
        btnCreate = (Button)findViewById(R.id.buttonCreate);
        btnReview = (Button)findViewById(R.id.buttonReview);
        btnSend = (Button)findViewById(R.id.buttonSend);
        listViewDevice = (ListView) findViewById(R.id.listViewDevice);
        tvStatus = (TextView) findViewById(R.id.tvStatus);
        //tvStatus.setText(getResources().getString(R.string.text_status_host));

        ShowNoDevice();
        InitWifi(); //wifi settings
        CheckPermission();
        UpdateButtonsHost();

        Disconnect(true);
        btnDisconnect.setOnClickListener(new View.OnClickListener() {
                 @Override
                 public void onClick(View view) {
                     Disconnect(true);
                     ResetData();
                 }
             }
        );

        btnCreate.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick (View v){

            OperationCtrl.nState = OperationCtrl.STATE.CREATE_PATTERN;
            Intent intent = new Intent(MainActivity.this, CreatePattern.class);
            startActivity(intent);
            }
        });
        btnReview =(Button)  findViewById(R.id.buttonReview);
        btnReview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v){
            OperationCtrl.nState = OperationCtrl.STATE.REVIEW_ALL_PATTERNS;
            Intent intent = new Intent(MainActivity.this, CreatePattern.class);
            startActivity(intent);

            }
        });
        btnSend.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View view) {
                   if (recvThreads.size() > 0){//recvThread != null && recvThread.isAlive()) {
                       //Log.i(TAG, "SendStringTask: " + msg);
                       for (RecvThread thread : recvThreads) {
                           Log.i(TAG, nGroupState + " sending data_id " + (nGroupState==WIFI_GROUP_STATE.OWNER ? 3:1));
                           if (nGroupState == WIFI_GROUP_STATE.OWNER)
                           {
                               for (SendThread send:sendThreads) {
                                   if (alPaths.size() > 0) send.sendData( 3, alPaths);
                                   else send.sendData( WIFI_DATA_STRING, WELCOME_INFO_HOST); //new SendTask(thread.socket, 3, alPaths).execute();
                               }
                           }
                           else {
                               for (SendThread send:sendThreads) send.sendData( WIFI_DATA_STRING, WELCOME_INFO_CLIENT+": "+ wifiP2pHandler.myDeviceName );
                           }
                       }
                   }else {
                       Log.i(TAG, nGroupState + " no recvThreads");
                       Toast.makeText(getApplicationContext(), "No connection at state "+nGroupState, Toast.LENGTH_SHORT).show();
                   }
               }
           }
        );

        btnHost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "btnHost: state " + nGroupState);
                if (nGroupState == WIFI_GROUP_STATE.NOT_FORMED) {
                    wifiP2pHandler.manager.createGroup(wifiP2pHandler.channel, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            Log.i(TAG, "btnHost: createGroup ok" + ", state to 1 from " + nGroupState);
                            // The group was created successfully, and this device is the group owner
                            DisplayStatus(wifiP2pHandler.myDeviceName == null ? "This is Host" : wifiP2pHandler.myDeviceName + ": Host", COLOR_STATUS.NORMAL);

                            nGroupState = WIFI_GROUP_STATE.OWNER;
                            //cbHost.setChecked(true);
                            serverClass = new ServerClass();
                            serverClass.start();
                        }

                        @Override
                        public void onFailure(int reason) {
                            Log.i(TAG, "btnHost: createGroup failed (" + reason + "), state " + nGroupState);
                            if (reason == WifiP2pManager.BUSY) {
                                DisplayStatus(reason+ getResources().getString(R.string.info_network_busy), COLOR_STATUS.NOK);
                            }
                            else DisplayStatus(reason+ getResources().getString(R.string.info_network_error), COLOR_STATUS.NOK);
                            // The group creation failed
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.info_wifi_connection)+reason,Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Disconnect(true);
                }
            }
        });
    }

    private void ShowNoDevice()
    {
        String[] deviceNames = new String[1];
        if (nUserRole == ROLE.HOST){
            if (nGroupState != WIFI_GROUP_STATE.NOT_FORMED) deviceNames[0] = getResources().getString(R.string.text_no_device);
            else deviceNames[0] = getResources().getString(R.string.text_no_device_host);
        }
        else deviceNames[0] = getResources().getString(R.string.text_no_device_player);

        Log.i(TAG, "ShowNoDevice: "+nUserRole + ", group " + nGroupState + ": " + deviceNames[0]);
        CustomAdapter adapter = new CustomAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, deviceNames );
        listViewDevice.setAdapter(adapter);
    }
    private void loadPlayerScreen()
    {
        btnDisconnect = (Button)findViewById(R.id.buttonDisconnect);
        btnDiscover = (Button)findViewById(R.id.buttonDiscover);
        btnStartPlaying = (Button)findViewById(R.id.buttonPlay);
        listViewDevice = (ListView) findViewById(R.id.listViewDevice);
        tvStatus = (TextView) findViewById(R.id.tvStatus);
        //tvStatus.setText(getResources().getString(R.string.text_status_player));
        ShowNoDevice();

        InitWifi(); //wifi settings
        CheckPermission();
        UpdateButtonsPlayer();

        Disconnect(true);

        btnDisconnect.setOnClickListener(new View.OnClickListener() {
                 @Override
                 public void onClick(View view) {
                    Disconnect(true);
                    ResetData();
                 }
             }
        );

        btnStartPlaying.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick (View v){
                Log.i(TAG, "btnStartPlaying "+OperationCtrl.nState);
                OperationCtrl.nState = OperationCtrl.STATE.OBSERVE_PATTERN;
                Intent intent = new Intent(MainActivity.this, CreatePattern.class);
                startActivity(intent);
            }
        });

        btnDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wifiP2pHandler.manager.discoverPeers(wifiP2pHandler.channel, new WifiP2pManager.ActionListener() { //ensure that the list of peers is up to date
                    @Override
                    public void onSuccess() {
                        DisplayStatus(getResources().getString(R.string.info_discovery_started), COLOR_STATUS.NORMAL);
                    }

                    @Override
                    public void onFailure(int i) {
                        DisplayStatus(getResources().getString(R.string.text_status_discovery_failed) +i, COLOR_STATUS.NOK);
                    }
                });
            }
        });

        listViewDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Toast.makeText(getApplicationContext(), "list device click: nGroupState:" + nGroupState, Toast.LENGTH_SHORT).show();
                switch (nGroupState)
                {
                    case TRYING_TO_CONNECT:
                        Disconnect(true);
                        return;
                    case NOT_FORMED:
                        if (wifiP2pHandler.devices == null)   return;
                        break;
                    default:
                        Toast.makeText(getApplicationContext(), (nGroupState == WIFI_GROUP_STATE.OWNER?"Host here":"Client here"), Toast.LENGTH_SHORT).show();
                        return;
                }

                if (wifiP2pHandler.devices.length ==0) return;
                final WifiP2pDevice device = wifiP2pHandler.devices[i];
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                Log.i(TAG, "listViewDevice click: state to 10 from "+nGroupState + ", host="+ device.deviceName);
                wifiP2pHandler.hostDeviceName = device.deviceName;

                nGroupState = WIFI_GROUP_STATE.TRYING_TO_CONNECT;
                wifiP2pHandler.manager.connect(wifiP2pHandler.channel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        DisplayStatus("Connecting...", COLOR_STATUS.NORMAL);
                        Toast.makeText(getApplicationContext(), "Connected to " + device.deviceName, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int i) {
                        Toast.makeText(getApplicationContext(), "Disconnected " + device.deviceName, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private boolean showDialog(String title, String msg) {
        final boolean[] result = {false}; // Use an array to store the result

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(msg)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Perform action on "OK" button click
                        result[0] = true; // Set result to true
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Perform action on "Cancel" button click
                        result[0] = false; // Set result to false
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();

        // Add an OnDismissListener to capture the result when the dialog is dismissed
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                // Use the result in the if statement
                if (result[0]) {
                    // Update UI for "OK" button click
                    Log.i(TAG, "Disconnect removeGroup");
                    // Disconnect from the current group
                    wifiP2pHandler.manager.removeGroup(wifiP2pHandler.channel, disconnectListener);

                } else {
                    // Update UI for "Cancel" button click
                }
            }
        });

        // Return default value (false) in case the dialog is dismissed without any button click
        return result[0];
    }


    private void Disconnect(boolean bForce) {
        Log.i(TAG, "Disconnect @" + nGroupState + ", " + nUserRole + ", force="+bForce);
        if (bForce)
        {
            wifiP2pHandler.manager.removeGroup(wifiP2pHandler.channel, disconnectListener);
            wifiP2pHandler.manager.requestGroupInfo(wifiP2pHandler.channel, groupInfoListener);
            return;
        }
        switch (nGroupState){
            case OWNER:
            case CLIENT:
                showDialog(getResources().getString(R.string.dialog_disconnect), getResources().getString(R.string.dialog_disconnect_content));
                break;
            case TRYING_TO_CONNECT:
                wifiP2pHandler.manager.removeGroup(wifiP2pHandler.channel, disconnectListener);
                break;
            default://not formed
                wifiP2pHandler.manager.removeGroup(wifiP2pHandler.channel, disconnectListener);
                break;
        }
        wifiP2pHandler.manager.requestGroupInfo(wifiP2pHandler.channel, groupInfoListener);
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LoadStartLayout();
    }
    private void LoadStartLayout(){
        nUserRole = ROLE.NOT_DEFINED;
        setContentView(R.layout.activity_main_start);
        // Set click listeners for role selection buttons
        Button hostButton = findViewById(R.id.hostButton);
        Button playerButton = findViewById(R.id.playerButton);
        Button onePersonButton = findViewById(R.id.onePersonButton);

        hostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nUserRole = ROLE.HOST;
                loadMainScreen();
            }
        });

        playerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nUserRole = ROLE.PLAYER;
                loadMainScreen();
            }
        });

        onePersonButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nUserRole = ROLE.SELF;
                loadMainScreen();
            }
        });

    }

    private void CheckPermission() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_WIFI_STATE},
                    1);
        }
        //to on/off wifi?
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "CHANGE_WIFI_STATE was not granted");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CHANGE_WIFI_STATE},
                    2);
        }

        //huawei Mate 20 pro (EMUI 12 = android 10) should remove ACCESS_COARSE_LOCATION？
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    3);
        }
        // not required for Build.VERSION_CODES.TIRAMISU or later?
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU ) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, " <TIRAMISU: ACCESS_FINE_LOCATION not granted");
                // Permission is not granted
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        4);
            }
        }
        //else //trying for huawei p30 pro
        {
            if (checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "TIRAMISU+: NEARBY_WIFI_DEVICES granted");
            } else {
                Log.i(TAG, "TIRAMISU+: NEARBY_WIFI_DEVICES not granted");
                // Permission is not granted, request the location permission again
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.NEARBY_WIFI_DEVICES},
                        5);
            }
        }
        //test for huawei p30 pro (VOG-L29, Android 10, API 29)
        //issue 1: peer change detected but wifiP2pDeviceList size is 0
        //issue 2: discover return error 0
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    6);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.INTERNET},
                    7);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_NETWORK_STATE},
                    8);
        }
    }
    private void InitWifi()
    {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)) {
            Log.e(TAG, "Wi-Fi Direct is not supported by this device.");
            Toast.makeText(getApplicationContext(),"Wi-Fi Direct is not supported", Toast.LENGTH_SHORT).show();
            finish();
        }
        Log.i(TAG, "MainActivity onCreate");

        wifiP2pHandler.manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        wifiP2pHandler.channel = wifiP2pHandler.manager.initialize(this, getMainLooper(), null);

        wifiP2pHandler.filter = new IntentFilter();
        //filter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        wifiP2pHandler.filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        wifiP2pHandler.filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        wifiP2pHandler.filter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            wifiP2pHandler.manager.requestDeviceInfo(wifiP2pHandler.channel, new WifiP2pManager.DeviceInfoListener() {
                @Override
                public void onDeviceInfoAvailable(WifiP2pDevice device) {
                    if (device != null) {
                        wifiP2pHandler.myDeviceName = device.deviceName;
                        Log.i(TAG, "requestDeviceInfo: my device - "+wifiP2pHandler.myDeviceName);
                    }
                }
            });
        }
    }

    void DisplayStatus(String info, COLOR_STATUS color)
    {
        if (tvStatus == null) {
            Log.i(TAG, "DisplayStatus not Main");
            Toast.makeText(getApplicationContext(), "- "+info, Toast.LENGTH_SHORT).show();
            return;
        }
        tvStatus.setText(info);
        switch(color) {
            case OK:
                tvStatus.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.purple_500));
                break;
            case NOK:
                tvStatus.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.option1));
                break;
            default:
                tvStatus.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.teal_700));
                break;
        }
    }

    WifiP2pManager.GroupInfoListener groupInfoListener = new WifiP2pManager.GroupInfoListener() {
        @Override
        public void onGroupInfoAvailable(WifiP2pGroup group) {
            //List<WifiP2pDevice> clients = (List<WifiP2pDevice>) group.getClientList();
            if (group != null) {
                Collection<WifiP2pDevice> clients = group.getClientList();
                int numClients = clients.size();
                Log.d(TAG, "onGroupInfoAvailable: Number of clients connected: " + numClients);
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
        wifiP2pHandler.listPeers.clear();

        CustomAdapter adapter = (CustomAdapter) listViewDevice.getAdapter();
        if (adapter != null) {
            if (wifiP2pHandler.deviceNames != null) Arrays.fill(wifiP2pHandler.deviceNames, null);
            adapter.notifyDataSetChanged();
            ShowNoDevice();
        }

        if (nGroupState == WIFI_GROUP_STATE.OWNER)
        {
            Log.i(TAG, "1 ResetData recvThread.interrupt");
            for (RecvThread thread: recvThreads) thread.interrupt();
            for (SendThread thread: sendThreads) thread.stopThread();
            serverClass.close();
            EnableButton(btnHost);
        }
        else if (nGroupState == WIFI_GROUP_STATE.CLIENT)
        {
            Log.i(TAG, "2 ResetData recvThread.interrupt");
            for (RecvThread thread: recvThreads) thread.interrupt();
            for (SendThread thread: sendThreads) thread.stopThread();
        }
        nGroupState = WIFI_GROUP_STATE.NOT_FORMED;
        DisplayStatus(getResources().getString(R.string.info_device_disconnected), COLOR_STATUS.NORMAL);
        wifiP2pHandler.hostDeviceName = "";
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
            boolean bHostAvailable = false;
            Log.i(TAG, "onPeersAvailable "+wifiP2pDeviceList.getDeviceList().size());
            if (!wifiP2pDeviceList.getDeviceList().equals(wifiP2pHandler.listPeers)) {
                listMacAddressConnected.clear();
                wifiP2pHandler.listPeers.clear();
                wifiP2pHandler.listPeers.addAll(wifiP2pDeviceList.getDeviceList());

                wifiP2pHandler.deviceNames = new String[wifiP2pDeviceList.getDeviceList().size()];
                wifiP2pHandler.devices = new WifiP2pDevice[wifiP2pDeviceList.getDeviceList().size()];

                int index=0;
                int nConnect = -1;
                for (WifiP2pDevice device : wifiP2pDeviceList.getDeviceList())
                {
                    if (device.isGroupOwner()) {
                        wifiP2pHandler.deviceNames[index] = device.deviceName + " (Host): " + getConnectionStatus(device.status);
                        //wifiP2pHandler.hostDeviceName = device.deviceName;
                        bHostAvailable = true;
                        Log.i(TAG, "peerListListener: " + (index+1) + " (host) " + device.deviceName + ": " + getConnectionStatus(device.status));
                    }
                    else {
                        wifiP2pHandler.deviceNames[index] = device.deviceName + ": " + getConnectionStatus(device.status);
                    }
                    wifiP2pHandler.devices[index] = device;
                    index++;
                    if (WifiP2pDevice.CONNECTED == device.status)  {
                        bConnected = true;
                        nConnect = index-1;
                        playerList.AddPlayer(device.deviceName, device.deviceAddress, index-1);
                        listMacAddressConnected.add(device.deviceAddress);
                    }

                    Log.i(TAG, "peerListListener: " + index + " " + device.deviceName + ": " + getConnectionStatus(device.status));
                }

                CustomAdapter adapter = new CustomAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, wifiP2pHandler.deviceNames );
                listViewDevice.setAdapter(adapter);

                if (bConnected) {
                    wifiP2pHandler.manager.requestGroupInfo(wifiP2pHandler.channel, groupInfoListener);
                    View item = listViewDevice.getChildAt(nConnect);
                    Log.i(TAG, "listViewDevice connected "+nConnect);
                    if (item != null) {
                        // never reach here, havenot created yet? try to get the TextView within the View and set its text color
                        TextView textView = item.findViewById(android.R.id.text1);
                        textView.setTextColor(Color.RED);
                    }
                }

                if ( listMacAddressConnected != null && listMacAddressConnected.size()>0)
                {
                    playerList.HouseKeeping(listMacAddressConnected);
                }
                handler.obtainMessage(MESSAGE_UPDATE_DEVICE).sendToTarget();

             }

            if (wifiP2pHandler.listPeers.size() == 0)
            {
                Toast.makeText(getApplicationContext(), "No device found", Toast.LENGTH_SHORT).show();
                ShowNoDevice();
            }
            else {
                if ( bHostAvailable && nGroupState == WIFI_GROUP_STATE.NOT_FORMED)
                    DisplayStatus(getResources().getString(R.string.text_click_to_connect), COLOR_STATUS.OK);
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
                DisplayStatus(wifiP2pHandler.myDeviceName==null ? "This is Host": wifiP2pHandler.myDeviceName + ": Host", COLOR_STATUS.NORMAL);
                nGroupState = WIFI_GROUP_STATE.OWNER;
                if (nUserRole == ROLE.HOST) {
                    HighlightButton(btnHost);
                    if (listMacAddressConnected.size() == 0 )
                        ShowNoDevice();
                }
                //Toast.makeText(getApplicationContext(), "Group Owner: " + groupOwnerAddress, Toast.LENGTH_SHORT).show();

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
                DisplayStatus((wifiP2pHandler.myDeviceName==null ? "This is Client": wifiP2pHandler.myDeviceName)+" -> "+ wifiP2pHandler.hostDeviceName, COLOR_STATUS.NORMAL);
                nGroupState = WIFI_GROUP_STATE.CLIENT;
                clientClass = new ClientClass(groupOwnerAddress);
                clientClass.start();

                wifiP2pHandler.manager.requestGroupInfo(wifiP2pHandler.channel, groupInfoListener);
            }
            else
            {
                Log.i(TAG, "connectionInfoListener: no group, state to 0 from " + nGroupState + " GO="+groupOwnerAddress);
                nGroupState = WIFI_GROUP_STATE.NOT_FORMED;
                DisplayStatus("No group formed", COLOR_STATUS.NOK);
            }
        }
    };

    /** register the BroadcastReceiver with the intent values to be matched */
    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume: nUserRole=" + nUserRole+ ", state=" +nGroupState + ", Operation="+OperationCtrl.nState);
        Display display = getWindowManager().getDefaultDisplay();
        Point point = new Point(0, 0);
        display.getSize(point);
        ScreenSize = new CustomPoint(point);

        switch (nUserRole)
        {
            case SELF:
                if (!OperationCtrl.IsPatternAvailable())  OperationCtrl.nState= OperationCtrl.STATE.START_UP;
                UpdateButtonsTraining();
            break;
            case PLAYER:
                if (nGroupState == WIFI_GROUP_STATE.NOT_FORMED){
                    DisplayStatus(getResources().getString(R.string.text_status_observe), COLOR_STATUS.NORMAL);
                } else {
                    switch (OperationCtrl.eResult) {
                        case TIMEOUT:
                            DisplayStatus("You didn't answer in time", COLOR_STATUS.NOK);
                            sendThreads.get(0).sendData(WIFI_DATA_STRING, wifiP2pHandler.myDeviceName + "> " + "Timeout");
                            break;
                        case PASSED:
                            DisplayStatus("Well done!", COLOR_STATUS.OK);
                            sendThreads.get(0).sendData(WIFI_DATA_STRING, wifiP2pHandler.myDeviceName + "> " + "Well done");
                            break;
                        case FAILED:
                            DisplayStatus("Your answer is wrong", COLOR_STATUS.NOK);
                            sendThreads.get(0).sendData(WIFI_DATA_STRING, wifiP2pHandler.myDeviceName + "> " + "Wrong");
                            break;
                        default:
                            DisplayStatus(getResources().getString(R.string.text_status_observe), COLOR_STATUS.NORMAL);
                            break;
                    }
                }
                UpdateButtonsPlayer();
            break;
            case HOST:
                DisplayStatus(getResources().getString(R.string.text_status_host), COLOR_STATUS.NORMAL);
                UpdateButtonsHost();
                break;
            default:
                break;
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: nUserRole=" + nUserRole + ", wifiP2pHandler.receiver=" + wifiP2pHandler.receiver);
        if (nUserRole == ROLE.HOST || nUserRole == ROLE.PLAYER) {
            Log.i(TAG, "wifiP2pHandler.receiver unregistered");
            unregisterReceiver(wifiP2pHandler.receiver);
        }
    }

    private void DisableButton(Button btn)
    {
        btn.setEnabled(false);
        btn.setBackgroundColor(Color.GRAY);
        btn.setTextColor(Color.WHITE);
    }
    private void HighlightButton(Button btn)
    {
        btn.setEnabled(true);
        btn.setBackgroundColor(Color.RED);
        btn.setTextColor(Color.WHITE);
    }
    private void EnableButton(Button btn)
    {
        btn.setEnabled(true);
        btn.setBackgroundColor(Color.GREEN);
        btn.setTextColor(Color.BLACK);
    }
    private void UpdateButtonsTraining()
    {
        btnCreate.setTextColor(Color.BLACK);
        btnSetting.setTextColor(Color.BLACK);
        btnCreate.setBackgroundColor(Color.GREEN);
        btnSetting.setBackgroundColor(Color.GREEN);

        switch (OperationCtrl.nState)
        {
            case START_UP:
                DisableButton(btnObserve);
                break;
            case CREATE_PATTERN:
                HighlightButton(btnObserve);
                break;
            case OBSERVE_PATTERN:
                HighlightButton(btnObserve);
                break;
            default:
                break;
        }
    }
    private void UpdateButtonsHost()
    {
        EnableButton(btnHost);
        EnableButton(btnDisconnect);
        EnableButton(btnCreate);

        if (nGroupState == WIFI_GROUP_STATE.OWNER)  HighlightButton(btnHost);
        if (alPaths != null && alPaths.size() >= 4)
        {
            EnableButton(btnReview);
            HighlightButton(btnSend);
        }
        else
        {
            DisableButton(btnReview);
            DisableButton(btnSend);
        }
    }

    private void UpdateButtonsPlayer()
    {
        EnableButton(btnDiscover);
        EnableButton(btnDisconnect);

        if (bPlayerReceived)
        {
            HighlightButton(btnStartPlaying);
        }
        else
        {
            DisableButton(btnStartPlaying);
        }
    }
    public class ServerClass extends Thread {
        Socket socket;
        ServerSocket serverSocket;

        public void close() {
            Log.i(TAG, "ServerClass close socket: "+ (serverSocket != null ? serverSocket.toString():"NULL") );
            sendThreads.clear();
            recvThreads.clear();

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

                    //each player got a pair of send/recv
                    recvThreads.add(new RecvThread(socket));
                    recvThreads.get(recvThreads.size()-1).start();
                    sendThreads.add(new SendThread(socket));
                    sendThreads.get(sendThreads.size()-1).start();


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
            Log.i(TAG, "ClientClass closeSocket");
            sendThreads.clear();
            recvThreads.clear();

            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void run() {
            try {
                Log.i(TAG, "ClientClass recvThread.start " + recvThreads.size() + " (0?) "+hostAddress + ", s="+ socket.toString());

                socket.connect(new InetSocketAddress(hostAddress, 16002), 1000);
                Log.i(TAG, "ClientClass socket connected " +socket.toString() + ": sendThreads size="+sendThreads.size() + ", recv "+recvThreads.size());

                //player has one pair of send/recv
                sendThreads.add ( new SendThread(socket) );
                sendThreads.get(0).start();
                recvThreads.add ( new RecvThread(socket) );
                recvThreads.get(0).start();

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
        }
    }
    private class RecvThread extends Thread {
        Socket socket;
        InputStream inputStream;
        ObjectInputStream objectInputStream;
        String ipAddressInput;
        //String myIp;

        public RecvThread(Socket sk)
        {
            socket = sk;

            ipAddressInput = socket.getInetAddress().getHostAddress();
            myIp = socket.getLocalAddress().toString();
        }

        @Override
        public void run() {

            Log.i(TAG, "recvThread run with socket from " + ipAddressInput);
            try {
                inputStream = socket.getInputStream();
                Log.i(TAG, "recvThread setup inputStream done");
                objectInputStream = new ObjectInputStream(inputStream);
                Log.i(TAG, "recvThread setup objectInputStream done");

                while (socket != null && !socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                    int data_id = 0;
                    Object payload = null;
                    try {
                        Log.i(TAG, "recvThread reading from " + inputStream.toString());
                        // Read the data_id and payload from the input stream
                        data_id = objectInputStream.readInt();
                        Log.i(TAG, "recvThread got int from " + inputStream.toString());
                        payload = objectInputStream.readObject();
                        Log.i(TAG, "recvThread got object from " + inputStream.toString());

                        ProcessReceivedData(data_id, payload);
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e)  {
                Log.i(TAG, "RecvThread IOException: "+e.toString());
                e.printStackTrace();
            } finally {
                try {
                    if (objectInputStream != null) {
                        Log.i(TAG, "RecvThread objectInputStream.close");
                        objectInputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "recvThread quit to " + ipAddressInput);
            }
        }
    }

    private void ProcessReceivedData(int data_id, Object payload)
    {
        Log.i(TAG, "ProcessReceivedData data_id "+ data_id);
        byte[] readBuf={0}; // received message bytes
        int numBytes = 0;// number of bytes in the received message
        if (payload instanceof String) {
            String payloadStringReceived = (String) payload;
            Log.i(TAG, "ProcessReceivedData string: "+ payloadStringReceived);
            readBuf = payloadStringReceived.getBytes(StandardCharsets.UTF_8);
            numBytes = payloadStringReceived.length();
        } else if (payload instanceof byte[]) {
            byte[] payloadBytesReceived = (byte[]) payload;
            Log.i(TAG, "ProcessReceivedData byte[]: "+ payloadBytesReceived.toString());
        } else if (payload instanceof ArrayList) {
            ArrayList<ArrayList<CustomPoint>> payloadPointsReceived = (ArrayList<ArrayList<CustomPoint>>) payload;
            alPaths = (ArrayList<ArrayList<CustomPoint>>) payload;
            Log.i(TAG, "ProcessReceivedData list size: "+ alPaths.size());//payloadPointsReceived.size());
            for (int i=0; i<alPaths.size(); i++)
            {
                FlyingPaths.alPosition.add(0);
            }
            FlyingPaths.NormalizePaths();//adjust to different screen size
        }


        Message readMsg = handler.obtainMessage(data_id, numBytes, -1, readBuf);
        handler.sendMessage(readMsg);
    }
}