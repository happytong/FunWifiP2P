package com.tongs.funpatternwifi;

import android.os.AsyncTask;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SendThread extends Thread {
    private final Socket socket;
    private boolean running;
    private int dataId;
    private Object data;

    public SendThread(Socket socket) {
        this.socket = socket;
        this.running = false;
        this.dataId = 0;
        this.data = null;
    }

    public synchronized void sendData(int dataId, Object data) {
        this.dataId = dataId;
        this.data = data;
        notify();
    }

    @Override
    public void run() {
        running = true;
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            while (running) {
                synchronized (this) {
                    while (data == null) {
                        wait();
                    }
                    objectOutputStream.writeInt(dataId);
                    objectOutputStream.writeObject(data);
                    objectOutputStream.flush();
                    data = null;
                }
            }
            objectOutputStream.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public synchronized void stopThread() {
        running = false;
        notify();
    }
}
