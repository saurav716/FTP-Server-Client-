import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class FTPServer {
    public static void main(String[] args) {
        int port = 8080;
        ExecutorService threadPool = Executors.newFixedThreadPool(10); // Fixed thread pool for concurrent clients

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("FTP Server is running on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());
                threadPool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }
}

class ClientHandler implements Runnable {
    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (DataInputStream dis = new DataInputStream(socket.getInputStream());
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

            while (true) {
                String command = dis.readUTF();
                if (command.equalsIgnoreCase("EXIT")) {
                    System.out.println("Client disconnected.");
                    break;
                }

                switch (command) {
                    case "MKDIR":
                        String dirName = dis.readUTF();
                        File dir = new File(dirName);
                        if (dir.mkdir()) {
                            dos.writeUTF("Directory created successfully.");
                        } else {
                            dos.writeUTF("Failed to create directory.");
                        }
                        break;

                    case "UPLOAD":
                        String fileName = dis.readUTF();
                        long fileSize = dis.readLong();
                        try (FileOutputStream fos = new FileOutputStream(fileName)) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while (fileSize > 0 && (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
                                fos.write(buffer, 0, bytesRead);
                                fileSize -= bytesRead;
                            }
                        }
                        dos.writeUTF("File uploaded successfully.");
                        break;

                    case "DOWNLOAD":
                        fileName = dis.readUTF();
                        File file = new File(fileName);
                        if (file.exists()) {
                            dos.writeUTF("FILE_FOUND");
                            dos.writeLong(file.length());
                            try (FileInputStream fis = new FileInputStream(file)) {
                                byte[] buffer = new byte[4096];
                                int bytesRead;
                                while ((bytesRead = fis.read(buffer)) != -1) {
                                    dos.write(buffer, 0, bytesRead);
                                }
                            }
                        } else {
                            dos.writeUTF("FILE_NOT_FOUND");
                        }
                        break;

                    case "LIST":
                        File currentDir = new File(".");
                        File[] files = currentDir.listFiles();
                        if (files != null && files.length > 0) {
                            dos.writeUTF("FILES_FOUND");
                            dos.writeInt(files.length);
                            for (File f : files) {
                                dos.writeUTF(f.getName() + (f.isDirectory() ? " [DIR]" : ""));
                            }
                        } else {
                            dos.writeUTF("NO_FILES");
                        }
                        break;

                    default:
                        dos.writeUTF("Invalid command.");
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
