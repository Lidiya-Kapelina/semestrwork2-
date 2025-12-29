import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private PlayersRoom room;
    private String username;

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            username = bufferedReader.readLine(); // имя клиента
        } catch (IOException e) {
            closeEverything();
        }
    }

    public void setRoom(PlayersRoom room) {
        this.room = room;
    }

    public BufferedWriter getWriter() {
        return bufferedWriter;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public void run() {
        String message;
        try {
            while ((message = bufferedReader.readLine()) != null) {

                if (message.startsWith("READY")) {
                    int[][] field = parseField(message);
                    room.handleReady(this, field);
                }

                else if (message.startsWith("FIRE")) {
                    String[] parts = message.split("\\|");
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    room.handleFire(this, x, y);
                }
            }
        } catch (IOException e) {
            closeEverything();
        }
    }

    private int[][] parseField(String message) {
        int[][] field = new int[10][10];
        String[] data = message.split("\\|");
        int index = 1;

        for (int y = 0; y < 10; y++)
            for (int x = 0; x < 10; x++)
                field[x][y] = Integer.parseInt(data[index++]);

        return field;
    }

    public void closeEverything() {
        try {
            if (bufferedReader != null) bufferedReader.close();
            if (bufferedWriter != null) bufferedWriter.close();
            if (socket != null) socket.close();
        } catch (IOException ignored) {}

        if (room != null) room.removePlayer(this);
    }
}
