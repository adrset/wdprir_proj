package client;

import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;
import thread.ThreadPoolService;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;


public class FileClient implements Runnable {

    private Socket s;
    private int port;
    private String host;
    private String file;
    private CountDownLatch latch;
    public FileClient(String host, int port, String file, CountDownLatch latch) {
        this.host = host;
        this.port = port;
        this.file = file;
        this.latch = latch;
    }

    public void run() {
        try {
            s = new Socket(host, port);
            sendFile(file);
        } catch (Exception e) {
            e.printStackTrace();
        }

        latch.countDown();
    }


    public void sendFile(String file) throws IOException {
        String shortFName = file.substring(file.lastIndexOf("\\") + 1, file.length());
        File myFile = new File(file);
        DataOutputStream dos = new DataOutputStream(s.getOutputStream());
        FileInputStream fis = new FileInputStream(myFile);
        System.out.println(myFile.length());
        int neededBytes = 8 + 4;
        ByteBuffer header = ByteBuffer.allocate(neededBytes);
        header.putLong(myFile.length());
        header.putInt(shortFName.length());

        byte[] size = header.array();

        dos.write(size);
        dos.write(shortFName.getBytes());
        dos.flush();
        byte[] buffer = new byte[1024];

        while (fis.read(buffer) > 0) {
            dos.write(buffer);
        }

        fis.close();
        dos.close();
    }

    public static void main(String[] args) throws Exception {
        int size = 100;
        int retries = 10;
        ThreadPoolService service = new ThreadPoolService(72);
        double av[] = new double[retries];
        for (int jj = 0; jj < retries; jj++) {
            service.init(size);
            FileClient fc[] = new FileClient[size];
            long start = System.nanoTime();
            for (int ii = 0; ii < size; ii++) {
                fc[ii] = new FileClient("localhost", 1988, "e:\\dane\\in\\" + (ii + 1) + ".exe", service.getLatch());
            }

            for (int ii = 0; ii < size; ii++) {
                service.submit(fc[ii]);
            }

            service.await();

            av[jj] = Math.pow(10, -9) * (System.nanoTime() - start);
        }

        double average = 0;

        for (int jj = 0; jj < retries; jj++) {
            average += av[jj];
        }
        average = average / retries;
        System.out.println("Elapsed: " + (average));
    }

}