package net.coalcube.bansystem.core.util;

import net.coalcube.bansystem.core.BanSystem;

import java.io.*;
import java.net.Socket;

public class ServerSocket {

    public ServerSocket(int port) throws IOException {
        java.net.ServerSocket serverSocket = new java.net.ServerSocket(port);

        System.out.println("Starting listening on Port "+port);

        while (true) {
            Socket socket = serverSocket.accept();
            OutputStream os = socket.getOutputStream();
            PrintWriter pw = new PrintWriter(os, true);

            BanSystem.getInstance().sendConsoleMessage("Connected");


            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            System.out.println("0");
            while ((line = br.readLine()) != null) {
                System.out.println("Client response: " + line);
                pw.println(line);
                pw.flush();
                if(line.equals("quit")) {
                    System.out.println("quit");
                    break;
                }

            }
            System.out.println("1");
            pw.close();
            br.close();
            os.close();
            socket.close();
            System.out.println("Closed Connection");
        }
    }


}
