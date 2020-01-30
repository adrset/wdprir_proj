package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

public class FileServer extends Thread {
    private EchoClientHandler ev;

    private ServerSocket ss;
    private int port;

    public FileServer(int port) { this.port = port; }

    @Override
    public void run() {
        try {
            ss = new ServerSocket(port);
            while (true) {
                ev = new EchoClientHandler(ss.accept());
                ev.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static class EchoClientHandler extends Thread {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        final static int BPS_COUNTER_FREQ = 40000;

        public EchoClientHandler(Socket socket) {
            this.clientSocket = socket;
        }


        private void saveFile(Socket clientSock) throws IOException {

            DataInputStream dis = new DataInputStream(clientSock.getInputStream());

            byte[] buffer = new byte[4096];

            int read = 0;
            int totalRead = 0;
            byte[] sizeAr = new byte[8];
            int i=0;

            int readd = dis.read(sizeAr);
            long filesize = ByteBuffer.wrap(sizeAr).asLongBuffer().get();
            long remaining = filesize;
            long checkpoint = 0;
            sizeAr = new byte[4];
            readd = dis.read(sizeAr);
            int fileNameLength = ByteBuffer.wrap(sizeAr).asIntBuffer().get();
           // System.out.println("File name length : " + fileNameLength);
            sizeAr = new byte[fileNameLength];
            readd = dis.read(sizeAr);
            String fname = new String(sizeAr);
            FileOutputStream fos = new FileOutputStream("e:\\dane\\out\\" + fname);
          //  System.out.println("File name: " + fname);

            long startTime = System.nanoTime();
            while ((read = dis.read(buffer, 0, Math.min(buffer.length, (int) remaining))) > 0) {
                totalRead += read;
                remaining -= read;
                checkpoint += read;
                //System.out.println("read " + totalRead + " bytes.");
                fos.write(buffer, 0, read);

                if (i++ % BPS_COUNTER_FREQ == 0) {
                    long timeElapsed = System.nanoTime() - startTime;
                    startTime = System.nanoTime();
                   // System.out.println(String.format("HOST: %s %.2f kBps ", clientSock.getLocalAddress(), checkpoint / (timeElapsed * Math.pow(10, -6))));
                    checkpoint = 0;
                }
            }


            fos.close();
            dis.close();
        }

        @Override
        public void run() {
            try {
                saveFile(clientSocket);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }

    public static void main(String[] args) {
        FileServer fs = new FileServer(1988);
        fs.start();
    }
}