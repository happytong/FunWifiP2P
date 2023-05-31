package com.tongs.funpatternwifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.util.Log;
import android.widget.Toast;

/**
 * A BroadcastReceiver that notifies of important wifi p2p events.
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
    String TAG="[dbg]WiFiDirectBroadcastReceiver";
    private WifiP2pManager manager;
    private Channel channel;
    private MainActivity activity;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel,
                                       MainActivity activity) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.i(TAG, "onReceive "+ action + ", mananger="+ (manager != null));
        if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            Log.d(TAG, "P2P peers changed");

            if (manager != null) {
                manager.requestPeers(channel, activity.peerListListener);
            }

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            //openAI: This broadcast is only triggered when the Wi-Fi Direct connection state changes, such as when a device connects or disconnects from the group, or when the group owner changes.
            //In general, it can take a few seconds to establish the Wi-Fi Direct connection, and the WIFI_P2P_CONNECTION_CHANGED_ACTION broadcast intent is usually triggered once the connection is established.
            if (manager == null) {
                //Toast.makeText(activity,"null WIFI_P2P_CONNECTION_CHANGED_ACTION",Toast.LENGTH_SHORT).show();
                Log.d(TAG, "null WIFI_P2P_CONNECTION_CHANGED_ACTION");
                return;
            }

            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            Log.i(TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION: "+ networkInfo.isConnected());
            if (networkInfo.isConnected()) {
                //Toast.makeText(activity,"WIFI_P2P_CONNECTION: connected", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION: connected");
                manager.requestConnectionInfo(channel, activity.connectionInfoListener);

            } else {
                // It's a disconnect
                //Toast.makeText(activity,"WIFI_P2P_CONNECTION: disconnected", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION: disconnected, " + activity.nGroupState);
                activity.ResetData();
            }

            int newWifiP2pInfo = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO, -1);
            Log.d(TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION p2p info - " + newWifiP2pInfo); //always -1
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            //WifiP2pDevice.EXTRA_DEVICE: configuration, including its name, address, and status
            //EXTRA_WIFI_P2P_INFO: network connection status, actual communication between devices in the same group
            //EXTRA_NETWORK_INFO: network connection status


            WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            Log.d(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION " + device.deviceName +": " + device.status); //own device
            //Toast.makeText(activity, "THIS_DEVICE: "+device.deviceName, Toast.LENGTH_SHORT).show();
            MainActivity.wifiP2pHandler.myDeviceName = device.deviceName;

            // Wi-Fi Direct is enabled again after removeGroup?
            int newWifiP2pState = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (newWifiP2pState == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Proceed with the next createGroup operation
                Log.d(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION ok - " + newWifiP2pState +": " + device.status);
            } else { //WIFI_P2P_STATE_DISABLED 0
                //always -1 received
                Log.d(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION error - " + newWifiP2pState +": " + device.status);
            }

            int newWifiP2pInfo = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO, -1);
            Log.d(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION p2p info - " + newWifiP2pInfo); //always -1?
        }
    }
}