import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) {
        String serverAddress = "localhost";
        int port = 8080;

        try (Socket socket = new Socket(serverAddress, port);
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Connected to FTP server.");
            String command;
            while (true) {
                System.out.println("\nEnter command (MKDIR,READ,LIST,UPLOAD,DOWNLOAD,EXIT): ");
                command = reader.readLine();
                if (command.equalsIgnoreCase("EXIT")) {
                    dos.writeUTF("EXIT");
                    System.out.println("Exiting...");
                    break;
                }

                switch (command) {
                    case "MKDIR":
                        System.out.print("Enter directory name: ");
                        String dirName = reader.readLine();
                        dos.writeUTF("MKDIR");
                        dos.writeUTF(dirName);
                        System.out.println(dis.readUTF());
                        break;

                    case "UPLOAD":
                        System.out.print("Enter file path to upload: ");
                        String filePath = reader.readLine();
                        File file = new File(filePath);
                        if (file.exists()) {
                            dos.writeUTF("UPLOAD");
                            dos.writeUTF(file.getName());
                            dos.writeLong(file.length());
                            try (FileInputStream fis = new FileInputStream(file)) {
                                byte[] buffer = new byte[4096];
                                int bytesRead;
                                while ((bytesRead = fis.read(buffer)) != -1) {
                                    dos.write(buffer, 0, bytesRead);
                                }
                            }
                            System.out.println(dis.readUTF());
                        } else {
                            System.out.println("File does not exist.");
                        }
                        break;

                    case "DOWNLOAD":
                        System.out.print("Enter file name to download: ");
                        String fileNameToDownload = reader.readLine();
                        dos.writeUTF("DOWNLOAD");
                        dos.writeUTF(fileNameToDownload);

                        String response = dis.readUTF();
                        if (response.equals("FILE_FOUND")) {
                            long fileSize = dis.readLong();

                            // Save to default Downloads directory
                            String userHome = System.getProperty("user.home");
                            String downloadDir = userHome + File.separator + "Downloads"; // Default download folder
                            File downloadedFile = new File(downloadDir + File.separator + fileNameToDownload);

                            // Create the Downloads folder if it does not exist
                            new File(downloadDir).mkdirs();

                            try (FileOutputStream fos = new FileOutputStream(downloadedFile)) {
                                byte[] buffer = new byte[4096];
                                int bytesRead;
                                long remaining = fileSize;
                                while (remaining > 0 && (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                                    fos.write(buffer, 0, bytesRead);
                                    remaining -= bytesRead;
                                }
                            }

                            System.out.println("File downloaded successfully to: " + downloadedFile.getAbsolutePath());
                        } else {
                            System.out.println("File not found on server.");
                        }
                        break;

                    case "LIST":
                        dos.writeUTF("LIST");
                        String serverResponse = dis.readUTF();
                        if (serverResponse.equals("FILES_FOUND")) {
                            int fileCount = dis.readInt();
                            System.out.println("Files and directories on the server:");
                            for (int i = 0; i < fileCount; i++) {
                                System.out.println("- " + dis.readUTF());
                            }
                        } else {
                            System.out.println("No files found on the server.");
                        }
                        break;

                    case "READ":
                        System.out.print("Enter the file name to read: ");
                        String readFileName = reader.readLine();
                        dos.writeUTF("READ");
                        dos.writeUTF(readFileName);

                        String readResponse = dis.readUTF();
                        if (readResponse.equals("FILE_FOUND")) {
                            System.out.println("File content:");
                            String line;
                            while (!(line = dis.readUTF()).equals("EOF")) {
                                System.out.println(line);
                            }
                        } else {
                            System.out.println("File not found on server.");
                        }
                        break;

                    case "WRITE":
                        System.out.print("Enter the file name to write to: ");
                        String writeFileName = reader.readLine();
                        dos.writeUTF("WRITE");
                        dos.writeUTF(writeFileName);

                        String writeResponse = dis.readUTF();
                        if (writeResponse.equals("FILE_FOUND")) {
                            System.out.print("Enter content to append: ");
                            String content = reader.readLine();
                            dos.writeUTF(content);
                            System.out.println(dis.readUTF()); // Acknowledgment from server
                        } else {
                            System.out.println("File not found on server.");
                        }
                        break;


                    default:
                        System.out.println("Invalid command.");
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void readFileLocally(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                System.out.println("Error reading file: " + e.getMessage());
            }
        } else {
            System.out.println("File does not exist: " + filePath);
        }
    }

    public static void writeFileLocally(String filePath, String content) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath, true))) {
            bw.write(content);
            bw.newLine();
            System.out.println("Content written to file: " + filePath);
        } catch (IOException e) {
            System.out.println("Error writing to file: " + e.getMessage());
        }
    }
}
