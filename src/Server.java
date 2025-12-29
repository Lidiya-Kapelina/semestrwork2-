import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private ServerSocket serverSocket;
    private static List<PlayersRoom> rooms = new ArrayList<>();

    public static List<PlayersRoom> getRooms() {
        return rooms;
    }

    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public void startServer() {
        try {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected");
                ClientHandler clientHandler = new ClientHandler(socket);

                // Если список комнат пуст, создаём первую комнату
                if (rooms.isEmpty()) {
                    PlayersRoom newRoom = new PlayersRoom();
                    newRoom.addPlayer(clientHandler);
                    rooms.add(newRoom);
                } else {
                    boolean added = false;
                    for (int i = 0; i < rooms.size(); i++) {
                        if (!rooms.get(i).isFullRoom()) {
                            rooms.get(i).addPlayer(clientHandler);
                            added = true;
                            break;
                        }
                    }
                    if (!added) {
                        PlayersRoom newRoom = new PlayersRoom();
                        newRoom.addPlayer(clientHandler);
                        rooms.add(newRoom);
                    }
                }

                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeServerSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(1223);
        Server server = new Server(serverSocket);
        server.startServer();
    }
}
