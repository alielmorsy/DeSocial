package aie.network.testing;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleTracker {
    private static List<String> hosts = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        ServerSocket socket = new ServerSocket(9090);
        while (!Thread.interrupted()) {
            Socket client = socket.accept();
            new Thread(() -> {
                try {
                    handleBasicHandeShake(client);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private static void handleBasicHandeShake(Socket client) throws IOException {
        System.out.println("Accepting Client: " + client.getInetAddress().getHostAddress());
        byte[] bytes = new byte[5];
        var is = client.getInputStream();
        var os = client.getOutputStream();
        if (is.read(bytes) != 5) {
            return;
        }
        if (!(bytes[0] == 1 && bytes[1] == 9 && bytes[2] == (byte) 200)) {
            return;
        }
        System.out.println("Success");
        switch (bytes[3]) {
            case 1:
                //Asking for data
                sendHosts( os);
                System.out.println("Hosts Sent");
                break;
            default:
                System.out.println("Unknown");
                client.close();
        }
        byte[] result = new byte[27];
        is.read(result);
        if (result[0] == 1) {
            StringBuilder hostID = new StringBuilder();
            hostID.append(result[1]).append(".");
            hostID.append(result[2]).append(".");
            hostID.append(result[3]).append(".");
            hostID.append(result[4]);
            System.out.println("Notifying host: " + hostID);
            byte r = (byte) (notifyHost(hostID.toString(), client.getInetAddress().getAddress()) ? 1 : 0);
            System.out.println("Notification result: " + r);
            os.write(r);
            client.close();
        } else {
            System.out.println("Registering Host");
            if (result[1] == 1) {
                hosts.add(client.getInetAddress().getHostAddress());
            }
            client.close();

        }

    }

    private static void sendHosts(OutputStream os) throws IOException {
        StringBuilder builder = new StringBuilder();
        for (var host : hosts) {
            builder.append(host).append(":");
        }
        os.write((byte) hosts.size());
        var bytes = builder.toString().getBytes(StandardCharsets.UTF_8);
        os.write(bytes);
    }

    //This additional step is to be sure none can connect to the server until we say yes.
    private static boolean notifyHost(String host, byte[] clientIP) throws IOException {
        Socket socket = new Socket(host, 9851);
        InputStream is = socket.getInputStream();
        var os = socket.getOutputStream();

        var response = is.read();
        if (response != 1) {
            socket.close();
            return false;
        }
        os.write(109); //Means Register Client and wait for him.
        var state = is.read();
        if (state != 1) {
            socket.close();
            return false;
        }
        os.write(clientIP);
        var result = is.read();
        if (result == 11) { //Full Host Accept only 3 devices
            socket.close();
            return false;
        } else if (result == 1) {
            socket.close();
            return true;
        }
        //Failed to add device.
        socket.close();
        return false;
    }
}
