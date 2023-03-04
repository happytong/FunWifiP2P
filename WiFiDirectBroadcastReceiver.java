package com.example.funpatternwifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
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

        if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            // request available peers from the wifi p2p manager. This is an
            // asynchronous call and the calling activity is notified with a
            // callback on PeerListListener.onPeersAvailable()
            if (manager != null) {
                manager.requestPeers(channel, activity.peerListListener);
                        //(PeerListListener) activity.getFragmentManager().findFragmentById(R.id.frag_list));
            }
            Toast.makeText(activity,"P2P peers changed",Toast.LENGTH_SHORT).show();
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            //openAI: This broadcast is only triggered when the Wi-Fi Direct connection state changes, such as when a device connects or disconnects from the group, or when the group owner changes.
            //In general, it can take a few seconds to establish the Wi-Fi Direct connection, and the WIFI_P2P_CONNECTION_CHANGED_ACTION broadcast intent is usually triggered once the connection is established.
            if (manager == null) {
                Toast.makeText(activity,"null WIFI_P2P_CONNECTION_CHANGED_ACTION",Toast.LENGTH_SHORT).show();
                return;
            }

            /*
            WifiP2pInfo wifiP2pInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
            if (wifiP2pInfo != null) {
                if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
                    // This device is the group owner
                    Log.i(TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION: groupowner "+ wifiP2pInfo.groupOwnerAddress );
                } else if (wifiP2pInfo.groupFormed) {
                    // A device has connected to the group
                    Log.i(TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION: device connected");
                } else {
                    // The group has been removed
                    Log.i(TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION: group removed");
                }
            }*/

            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

           // Toast.makeText(activity,"WIFI_P2P_CONNECTION: " + (networkInfo.isConnected()?"yes":"no"), Toast.LENGTH_SHORT).show();
            Log.i(TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION: "+ networkInfo.isConnected());
            if (networkInfo.isConnected()) {
                Toast.makeText(activity,"WIFI_P2P_CONNECTION: connected", Toast.LENGTH_SHORT).show();
                // we are connected with the other device, request connection
                // info to find group owner IP

                //DeviceDetailFragment fragment = (DeviceDetailFragment) activity
                //        .getFragmentManager().findFragmentById(R.id.frag_detail);
                manager.requestConnectionInfo(channel, activity.connectionInfoListener);

            } else {
                // It's a disconnect
                Toast.makeText(activity,"WIFI_P2P_CONNECTION: disconnected", Toast.LENGTH_SHORT).show();

                activity.ResetData();
            }


        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
/*            DeviceListFragment fragment = (DeviceListFragment) activity.getFragmentManager()
                    .findFragmentById(R.id.frag_list);
            fragment.updateThisDevice((WifiP2pDevice) intent.getParcelableExtra(
                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
*/

            WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            Log.d(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION " + device.deviceName +": " + device.status); //own device
            Toast.makeText(activity, "THIS_DEVICE: "+device.deviceName, Toast.LENGTH_SHORT).show();
            activity.myDeviceName = device.deviceName;
        }
    }
}