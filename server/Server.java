package file_transfer.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private ServerSocket serverSocket;


    public Server() {
        try {
            init();
            System.out.println("Server is running.");
        } catch (IOException e) {
            System.out.println("PORT may have been used.");
            return;
        }
        listen();
    }


    private void init() throws IOException {
        int PORT = 11111;
        serverSocket = new ServerSocket(PORT);
    }

    private void listen() {
        try {
            while (true) {
                new Handler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            System.out.println("Connection lost.");
        }
    }

    protected class Handler implements Runnable {
        Thread thread;
        Socket socket;
        private int port;

        Handler(Socket socket) {
            this.socket = socket;
            this.port = socket.getPort();
            this.thread = new Thread(this);
        }

        void start() {
            if (this.thread == null) {
                this.thread = new Thread(this);
            }
            System.out.println("Connected: [" + this.socket.getPort() + "]");
            this.thread.start();
        }

        @Override
        public void run() {
            boolean flag = true;
            String request;
            while (flag) {
                try {
                    request = read();
                    if (request == null || !request.contains(" ")) {
//                        flag = false;
                        return;
                    }
                    String[] args = request.split(" ");
                    String requestBody = request.replace(args[0]+" ","");
                    switch (args[0]) {
                        case "MESSAGE":
                            System.out.println("Request by [" + port + "]: " + args[1]);
                            send("Sent From Server: " + requestBody);
                            break;
                        case "DOWNLOAD":
                            if (findFile(requestBody)) {
                                send("FOUND ");
                                sendFile(requestBody);
                            } else {
                                send("NOTFOUND ");
                            }
                            break;
                        case "EXIT":
                            socket.close();
                            break;
                        default:
                            System.out.println("Undefined request.");
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected: [" + port + "]");
                    flag = false;
                }
            }
        }

        private String read() throws IOException {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            return in.readLine();
        }

        private void send(String message) throws IOException {
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
            out.println(message);
            out.flush();
        }

        private boolean findFile(String fileName) {
            File file = new File(fileName);
            try {
                new FileInputStream(file);
            } catch (FileNotFoundException e) {
                System.out.println("File [" + fileName + "] " +
                        "request by [" + port + "] not found.");
                return false;
            }
            return true;
        }

        synchronized private void sendFile(String fileName) {
            File myFile;
            OutputStream OS;
            DataOutputStream DOS;
            byte[] buf;

            // handle file read
            System.out.println("Start sending file...");
            myFile = new File(fileName);
            byte[] mybytearray = new byte[(int) myFile.length()];
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(myFile);
            } catch (FileNotFoundException e) {
                return;
            }
            try {
                OS = socket.getOutputStream();
                // Sending file name and file size to the server
                DOS = new DataOutputStream(OS);
            } catch (IOException e) {
                return;
            }
            try {
                int bufferSize = 8192;
                buf = new byte[bufferSize];
                DOS.writeUTF(myFile.getName());
                DOS.writeLong(mybytearray.length);

                while (true) {
                    int read = 0;
                    if (fis != null) {
                        read = fis.read(buf);
                    }
                    if (read == -1) {
                        break;
                    }
                    DOS.write(buf, 0, read);
                }
                DOS.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("File = (" + fileName + ") sent.");
        }


    }

    public static void main(String[] args) {
        new Server();
    }
}
