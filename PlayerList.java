package com.tongs.funpatternwifi;

import android.icu.util.Calendar;
import android.util.Log;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlayerList {
    String TAG = "[dbg]PlayerList";
    static enum TEST_RESULT {
        NA,
        PASS,
        FAILED,
        TIMEOUT,
    };
    private class PlayerInfo{
        String deviceName;
        String macAddress;
        long lastCommTime;
        TEST_RESULT result;
        String playerId; //unique to the Host in realtime, maintained by the Host
        int index;  //for listview item update only (text, color)
    }
    static List<PlayerInfo> playerList = new ArrayList<>();

    private String GeneratePlayerId(String deviceName, String macAddress) //get unique ID
    {
        int nCount = playerList.size();
        if (nCount == 0) return deviceName;
        else{
            int n= 0;
            for (PlayerInfo player: playerList) {
                Log.i(TAG, "GeneratePlayerId "+n + "/"+nCount+" to add ("+deviceName + " - " + macAddress +")");
                if (player.deviceName.equals(deviceName)){
                    if (player.macAddress.equals(macAddress)) return deviceName+nCount;
                    return player.playerId;
                }
            }
            return deviceName;
        }
    }

    public String AddPlayer(String deviceName, String macAddress, int index)
    {
        PlayerInfo player = new PlayerInfo();
        player.deviceName = deviceName;
        player.macAddress = macAddress;
        player.lastCommTime = Calendar.getInstance().getTimeInMillis()/1000;
        player.result = TEST_RESULT.NA;
        player.index = index;
        player.playerId = GeneratePlayerId(deviceName, macAddress);
        playerList.add(player);
        Log.i(TAG, "AddPlayer "+player.playerId);
        return player.playerId;
    }
    public PlayerInfo UpdatePlayerResult(String ID, TEST_RESULT result) {
        if (playerList.size() == 0) return null;
        for (int i = 0; i < playerList.size(); i++) {
            PlayerInfo player = playerList.get(i);
            if (player.playerId.equals(ID)) { // Compare playerIds using .equals() for string comparison
                player.result = result;
                player.lastCommTime = Calendar.getInstance().getTimeInMillis() / 1000;

                playerList.set(i, player); // Set the updated item back at the same index in the list
                return player;
            }
        }
        return null;
    }
    // Function to remove MAC addresses from playerList that do not exist in listOfMacAddress
    public void HouseKeeping(List<String> listOfMacAddress) {
        List<PlayerInfo> filteredPlayerList = new ArrayList<>();
        Log.i(TAG, "HouseKeeping Null?"+ (listOfMacAddress==null));
        // Create a set of valid MAC addresses from listOfMacAddress for faster lookups
        Set<String> validMacSet = new HashSet<>(listOfMacAddress);

        // Iterate through playerList and keep only players with valid MAC addresses
        for (PlayerInfo player : playerList) {
            if (validMacSet.contains(player.macAddress)) {
                filteredPlayerList.add(player);
            }
        }

        // Update playerList with the filtered list
        playerList = filteredPlayerList;
    }

    public void Reset()
    {
        playerList.clear();
    }
}
