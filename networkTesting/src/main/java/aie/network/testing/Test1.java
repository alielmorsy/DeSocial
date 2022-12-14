package aie.network.testing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Test1 {
    private static List<String> clients = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        Socket socket = new Socket("127.0.0.1", 9090);
        var os = socket.getOutputStream();
        var is = socket.getInputStream();

        os.write(new byte[]{1, 9, (byte) 200, 1, 1});
        var count = is.read();
        if (count == 0) {
            var data = new byte[27];
            data[0] = 0;
            data[1] = 1;
            os.write(data);
            socket.close();
            launchHost();
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int read;
            byte[] bytes = new byte[1024];
            while ((read = is.read(bytes)) > 0) {

                baos.write(bytes, 0, read);
                if (is.available() == 0) break;
            }
            String[] host = baos.toString(StandardCharsets.UTF_8).split(":")[0].split("\\.");
            System.out.println(Arrays.toString(host));
            var data = new byte[27];
            data[0] = 1;
            int index = 1;
            for (var n : host) {
                data[index++] = (byte) Integer.parseInt(n);
            }
            os.write(data);
            byte b = (byte) is.read();
            if (b == 1) {
                System.out.println("We are known by the host");
            } else {
                System.out.println("Failed to connect to the host");
            }
            socket.close();

            callHost(String.join(".", host));
        }
    }

    private static void callHost(String ip) throws IOException {
        Socket socket = new Socket(ip, 9851);
        var is = socket.getInputStream();
        var os = socket.getOutputStream();
        int state = is.read();
        if (state != 1) {
            System.out.println("Host not ready");
            socket.close();
            return;
        }
        System.out.println("Sending HI to the server.");
        os.write(100); //Means HI
        int result = is.read();
        if (result == 100) {
            System.out.println("Server Saying Hi");
        } else if (result == -1) {
            System.out.println("You are not allowed to be with that host");
            socket.close();
        }
    }

    private static void launchHost() throws IOException {
        ServerSocket socket = new ServerSocket(9851);
        while (!Thread.interrupted()) {
            var client = socket.accept();
            new Thread(() -> {
                try {
                    handleClient(client);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private static void handleClient(Socket client) throws IOException {
        var is = client.getInputStream();
        var os = client.getOutputStream();
        os.write(1); //Means Host Is Up No unknown failure, or its under maintains.
        int what = is.read(); //Registering Client
        if (what == 109) {
            if (clients.size() < 3) {
                os.write(1);
            } else {
                os.write(-1);
                client.close();
                return;
            }
            var address = new byte[4];
            is.read(address);
            StringBuilder builder = new StringBuilder();
            builder.append(address[0]).append(".");
            builder.append(address[1]).append(".");
            builder.append(address[2]).append(".");
            builder.append(address[3]);
            var ipString = builder.toString();
            System.out.println(ipString);

            if (clients.contains(ipString)) {
                os.write(11); //Means Ip Already Registered
            }
            os.write(1);
            clients.add(ipString);
            client.close();

        } else if (what == 100) { //Saying Hi
            if (clients.contains(client.getInetAddress().getHostAddress())) {
                os.write(100);
                client.close();
            }else{
                os.write(-1);
                client.close();
            }
        }
    }
}
