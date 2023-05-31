package com.tongs.funpatternwifi;

import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;

import java.util.ArrayList;
import java.util.List;

public class WifiP2pHandler {
    String TAG="[dbg]WifiP2pHandler";
    WifiP2pManager manager;
    WifiP2pManager.Channel channel;
    IntentFilter filter;
    WifiManager wifiManager;
    WiFiDirectBroadcastReceiver receiver;
    static List<WifiP2pDevice> listPeers = new ArrayList<WifiP2pDevice>();
    static String[] deviceNames;
    static WifiP2pDevice[] devices;
    static String hostDeviceName;
    String myDeviceName;

}
