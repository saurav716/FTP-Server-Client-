import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class FTPServer {
    private static final String FIXED_DIRECTORY = "server_files"; // Fixed directory for file storage
    private static final ConcurrentHashMap<String, String> fileOwners = new ConcurrentHashMap<>(); // File ownership tracking

    public static void main(String[] args) {
        int port = 8080;

        // Create the fixed directory if it doesn't exist
        File directory = new File(FIXED_DIRECTORY);
        if (!directory.exists()) {
            directory.mkdir();
        }

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);
            var threadPool = Executors.newFixedThreadPool(10); // For handling multiple clients

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());
                threadPool.execute(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket socket) {
        try (DataInputStream dis = new DataInputStream(socket.getInputStream());
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

            while (true) {
                String command = dis.readUTF();
                if (command.equalsIgnoreCase("EXIT")) {
                    System.out.println("Client disconnected.");
                    break;
                }

                switch (command.toUpperCase()) {
                    case "UPLOAD":
                        handleUpload(dis, dos, socket.getInetAddress().toString());
                        break;
                    case "DOWNLOAD":
                        handleDownload(dis, dos);
                        break;
                    case "LIST":
                        handleList(dos);
                        break;
                    case "READ":
                        handleRead(dis, dos);
                        break;
                    case "WRITE":
                        handleWrite(dis, dos, socket.getInetAddress().toString());
                        break;
                    default:
                        dos.writeUTF("Invalid command.");
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("Error handling client: " + e.getMessage());
        }
    }

    private static void handleUpload(DataInputStream dis, DataOutputStream dos, String clientAddress) throws IOException {
        String fileName = dis.readUTF();
        long fileSize = dis.readLong();
        File file = new File(FIXED_DIRECTORY + File.separator + fileName);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while (fileSize > 0 && (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
                fos.write(buffer, 0, bytesRead);
                fileSize -= bytesRead;
            }
        }
        // Track the owner of the uploaded file (based on client's IP)
        fileOwners.put(fileName, clientAddress);
        dos.writeUTF("File uploaded successfully: " + fileName);
        System.out.println("File uploaded by " + clientAddress + ": " + fileName);
    }

    private static void handleDownload(DataInputStream dis, DataOutputStream dos) throws IOException {
        String fileName = dis.readUTF();
        File file = new File(FIXED_DIRECTORY + File.separator + fileName);

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
            System.out.println("File sent to client: " + fileName);
        } else {
            dos.writeUTF("FILE_NOT_FOUND");
            System.out.println("File not found: " + fileName);
        }
    }

    private static void handleList(DataOutputStream dos) throws IOException {
        File directory = new File(FIXED_DIRECTORY);
        File[] files = directory.listFiles();

        if (files != null && files.length > 0) {
            dos.writeUTF("FILES_FOUND");
            dos.writeInt(files.length);
            System.out.println("Listing files:");
            for (File file : files) {
                dos.writeUTF(file.getName());
                System.out.println(" - " + file.getName());
            }
        } else {
            dos.writeUTF("NO_FILES");
            System.out.println("No files found.");
        }
    }

    private static void handleRead(DataInputStream dis, DataOutputStream dos) throws IOException {
        String fileName = dis.readUTF();
        File file = new File(FIXED_DIRECTORY + File.separator + fileName);

        if (file.exists()) {
            dos.writeUTF("FILE_FOUND");
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    dos.writeUTF(line);
                }
            }
            dos.writeUTF("EOF"); // End of file marker
            System.out.println("File read by client: " + fileName);
        } else {
            dos.writeUTF("FILE_NOT_FOUND");
            System.out.println("File not found: " + fileName);
        }
    }

    private static void handleWrite(DataInputStream dis, DataOutputStream dos, String clientAddress) throws IOException {
        String fileName = dis.readUTF();
        File file = new File(FIXED_DIRECTORY + File.separator + fileName);

        // Check if the client is the owner of the file
        String owner = fileOwners.get(fileName);
        if (owner != null && owner.equals(clientAddress)) {
            dos.writeUTF("FILE_FOUND");
            String content = dis.readUTF();

            try (FileWriter writer = new FileWriter(file, true)) { // Append mode
                writer.write(content + System.lineSeparator());
            }
            dos.writeUTF("Content written to file: " + fileName);
            System.out.println("Content written to file by " + clientAddress + ": " + fileName);
        } else {
            dos.writeUTF("FILE_NOT_FOUND_OR_NO_PERMISSION");
            System.out.println("Client " + clientAddress + " tried to write to a file they don't own: " + fileName);
        }
    }
}
