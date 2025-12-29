import java.io.*;
import java.net.Socket;

public class Client {

    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;

    public Client(Socket socket, String username) {
        try {
            this.socket = socket;
            bufferedWriter = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())
            );
            bufferedReader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );

            // имя отправляем ОДИН РАЗ
            bufferedWriter.write(username);
            bufferedWriter.newLine();
            bufferedWriter.flush();

        } catch (IOException e) {
            closeEverything();
        }
    }

    // ❗ БЕЗ username, БЕЗ мусора
    public void sendMessage(String message) {
        try {
            bufferedWriter.write(message);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            closeEverything();
        }
    }

    public void listenForMessage(GamePanel gamePanel) {
        new Thread(() -> {
            String msg;
            try {
                while ((msg = bufferedReader.readLine()) != null) {
                    gamePanel.handleServerMessage(msg);
                }
            } catch (IOException e) {
                closeEverything();
            }
        }).start();
    }

    private void closeEverything() {
        try {
            if (bufferedReader != null) bufferedReader.close();
            if (bufferedWriter != null) bufferedWriter.close();
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }
}
