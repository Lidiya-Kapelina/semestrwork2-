import javax.swing.*; // Импортируем все компоненты Swing (JFrame, JButton, JTextField и т.д.)
import java.awt.*;    // Импортируем классы для работы с компоновкой и цветами
import java.awt.event.ActionEvent; // Импорт для обработки событий кнопки
import java.awt.event.ActionListener; // Интерфейс для обработки кликов
import java.io.IOException;
import java.net.Socket;

// Класс окна входа
public class LoginGUI extends JFrame {
    String username;
    private JTextField nameField; // Поле для ввода имени пользователя
    private JButton playButton;   // Кнопка "Играть"

    // Конструктор окна
    public LoginGUI() {
        setTitle("Морской Бой — Вход"); // Заголовок окна
        setSize(500, 150);             // Размер окна в пикселях (ширина, высота)
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Закрытие программы при закрытии окна
        setLocationRelativeTo(null);   // Размещает окно по центру экрана
        setLayout(new BorderLayout()); // Задаем менеджер компоновки BorderLayout (север, юг, центр)

        // Создаем надпись "Введите ваше имя"
        JLabel label = new JLabel("Введите ваше имя:");
        label.setHorizontalAlignment(SwingConstants.CENTER); // Выравнивание по центру
        add(label, BorderLayout.NORTH); // Добавляем надпись в верхнюю часть окна

        // Создаем текстовое поле для ввода имени с ограниченным размером
        nameField = new JTextField();
        nameField.setPreferredSize(new Dimension(300, 30)); // Ограничиваем размер поля
        nameField.setMaximumSize(new Dimension(300, 30));
        
        // Создаем панель для центрирования поля ввода
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centerPanel.add(nameField);
        add(centerPanel, BorderLayout.CENTER); // Добавляем панель в центр окна

        // Создаем кнопку "Играть"
        playButton = new JButton("Играть");
        playButton.setPreferredSize(new Dimension(160, 55)); // шире по высоте, уже по ширине

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(getBackground()); // тот же цвет, что у окна
        buttonPanel.add(playButton);

        add(buttonPanel, BorderLayout.SOUTH);
        // Добавляем кнопку в нижнюю часть окна

        // Обработка нажатия кнопки
        playButton.addActionListener(event -> {
            String username = nameField.getText().trim();

            if (!username.isEmpty()) {
                try {
                    Socket socket = new Socket("localhost", 1223);

                    Client client = new Client(socket, username);


                    dispose(); // окно входа закрывается корректно

                    SwingUtilities.invokeLater(() -> new ShipPlacementGUI(client));


                } catch (IOException e) {
                    e.printStackTrace();

                }
            }
        });


        setVisible(true); // Делаем окно видимым
    }

    // Точка входа в программу
    public static void main(String[] args) throws IOException {
        SwingUtilities.invokeLater(LoginGUI::new); // Создаем окно входа в потоке Swing

    }
}
