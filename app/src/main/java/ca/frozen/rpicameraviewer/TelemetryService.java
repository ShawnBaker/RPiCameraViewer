package ca.frozen.rpicameraviewer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class TelemetryService extends Service {
    public static final String LOCAL_PORT_KEY = "local_port";
    public static final String TELEMETRY_BROADCAST_KEY = "telemetry_broadcast";
    public static final String TELEMETRY_MESSAGE_KEY = "message";
    private static final int DEFAULT_UDP_PORT = 5555;
    private static final int MAX_UDP_DATAGRAM_LEN = 1500;
    private static final int UDP_SERVER_RESPAWN = 2000;

    private UDPReceiver telemetryReceiver = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final int localUDPPort = intent.getIntExtra(LOCAL_PORT_KEY, DEFAULT_UDP_PORT);

        telemetryReceiver = new UDPReceiver(localUDPPort);
        telemetryReceiver.start();

        return Service.START_NOT_STICKY;    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        telemetryReceiver.kill();

        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    private class UDPReceiver extends Thread {
        private boolean keepRunning = true;
        private int UDP_SERVER_PORT;

        public UDPReceiver(int listen_port) {
            UDP_SERVER_PORT = listen_port;
        }

        public void run() {
            final byte[] buffer = new byte[MAX_UDP_DATAGRAM_LEN];

            final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            DatagramSocket socket = null;

            do {
                try {
                    Thread.sleep(UDP_SERVER_RESPAWN);
                    socket = new DatagramSocket(null);
                    socket.setReuseAddress(true);
                    socket.bind(new InetSocketAddress(UDP_SERVER_PORT));

                    while (keepRunning) {
                        socket.receive(packet);

                        final String message = new String(buffer, 0, packet.getLength());

                        final Intent messageIntent = new Intent();

                        messageIntent.setAction(TELEMETRY_BROADCAST_KEY);

                        messageIntent.putExtra(TELEMETRY_MESSAGE_KEY, message);

                        sendBroadcast(messageIntent);
                    }

                    socket.close();
                } catch (Throwable e) {
                    Log.e(getClass().getName(), e.getMessage());
                    e.printStackTrace();
                }
            } while (keepRunning);

            if (socket != null) {
                socket.close();
            }
        }

        void kill() {
            keepRunning = false;
        }
    }
}
