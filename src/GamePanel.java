import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class GamePanel extends JPanel {
    private String opponentName = null;


    private GameState state = GameState.READY_WAIT;

    private static final int CELL = 40;
    private static final int GRID = 10;
    private static final int TOP_OFFSET = 120;  // Отступ сверху для заголовка (увеличен, чтобы надписи не накладывались)
    private static final int SIDE_OFFSET = 50;  // Отступ по бокам для симметрии (увеличен для цифр)
    private static final int GRID_SPACING = 40;  // Расстояние между двумя сетками

    private int[][] myField;
    private int[][] opponentField = new int[GRID][GRID];  // 0 = нет попадания, 1 = попадание, 2 = промах
    private int[][] myHits = new int[GRID][GRID];  // попадания по моему полю (0 = нет, 1 = попадание, 2 = промах)

    private final Client client;

    public GamePanel(Client client, int[][] myField) {
        this.client = client;
        this.myField = myField;

        int width = SIDE_OFFSET + GRID * CELL + GRID_SPACING + GRID * CELL + SIDE_OFFSET;
        int height = TOP_OFFSET + GRID * CELL + 40;
        setPreferredSize(new Dimension(width, height));
        setBackground(Color.WHITE);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {

                if (state != GameState.YOUR_TURN) return;

                int offsetX = SIDE_OFFSET + GRID * CELL + GRID_SPACING;
                int x = (e.getX() - offsetX) / CELL;
                int y = (e.getY() - TOP_OFFSET) / CELL;

                if (x < 0 || x >= GRID || y < 0 || y >= GRID) return;
                if (opponentField[x][y] != 0) return;

                client.sendMessage("FIRE|" + x + "|" + y);
                state = GameState.OPPONENT_TURN;
                repaint();
            }
        });
    }

    /* ---------- ОБРАБОТКА СООБЩЕНИЙ СЕРВЕРА ---------- */

    public void handleServerMessage(String msg) {
        // Все обновления UI должны выполняться в потоке Swing
        SwingUtilities.invokeLater(() -> {
            if (msg.startsWith("START_GAME")) {
                opponentName = msg.split("\\|")[1];
                state = GameState.READY_WAIT;  // ожидаем готовности обоих игроков
            }

            else if (msg.equals("YOUR_TURN")) {
                state = GameState.YOUR_TURN;
            }

            else if (msg.equals("WAIT")) {
                state = GameState.OPPONENT_TURN;
            }

            else if (msg.startsWith("HIT")) {
                // Попадание по полю противника
                String[] p = msg.split("\\|");
                int x = Integer.parseInt(p[1]);
                int y = Integer.parseInt(p[2]);
                opponentField[x][y] = 1;
            }

            else if (msg.startsWith("MISS")) {
                // Промах по полю противника
                String[] p = msg.split("\\|");
                int x = Integer.parseInt(p[1]);
                int y = Integer.parseInt(p[2]);
                opponentField[x][y] = 2;
            }

            else if (msg.startsWith("MY_HIT")) {
                // Попадание по моему полю
                String[] p = msg.split("\\|");
                int x = Integer.parseInt(p[1]);
                int y = Integer.parseInt(p[2]);
                myHits[x][y] = 1;
            }

            else if (msg.startsWith("MY_MISS")) {
                // Промах по моему полю
                String[] p = msg.split("\\|");
                int x = Integer.parseInt(p[1]);
                int y = Integer.parseInt(p[2]);
                myHits[x][y] = 2;
            }

            else if (msg.startsWith("GAME_OVER")) {
                state = GameState.GAME_OVER;
                String winner = msg.split("\\|")[1];

                JOptionPane.showMessageDialog(this,
                        "Победитель: " + winner,
                        "Игра окончена",
                        JOptionPane.INFORMATION_MESSAGE);
                
                // Закрываем приложение для клиента после окончания игры
                Window window = SwingUtilities.getWindowAncestor(this);
                if (window != null) {
                    window.dispose();
                }
                System.exit(0);
            }

            repaint();
        });
    }

    /* ---------- ОТРИСОВКА ---------- */

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        drawTitle(g);
        drawFieldLabels(g);
        drawGridLabels(g, SIDE_OFFSET);
        drawGridLabels(g, SIDE_OFFSET + GRID * CELL + GRID_SPACING);
        drawGrid(g, SIDE_OFFSET);
        drawGrid(g, SIDE_OFFSET + GRID * CELL + GRID_SPACING);
        drawMyField(g);
        drawOpponentField(g);
        drawStatus(g);
    }

    private void drawGridLabels(Graphics g, int offsetX) {
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.setColor(Color.BLACK);
        FontMetrics fm = g.getFontMetrics();

        // Буквы по горизонтали (A–J)
        String[] letters = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};
        for (int i = 0; i < GRID; i++) {
            int letterWidth = fm.stringWidth(letters[i]);
            int x = offsetX + i * CELL + (CELL - letterWidth) / 2;
            g.drawString(letters[i], x, TOP_OFFSET - 8);
        }

        // Цифры по вертикали (1–10) — для ОБЕИХ карт
        for (int i = 0; i < GRID; i++) {
            String number = String.valueOf(i + 1);
            int numberWidth = fm.stringWidth(number);
            int y = TOP_OFFSET + i * CELL + CELL / 2 + fm.getAscent() / 2 - 2;

            g.drawString(number, offsetX - numberWidth - 8, y);
        }
    }


    private void drawFieldLabels(Graphics g) {
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.setColor(Color.BLACK);
        FontMetrics fm = g.getFontMetrics();
        
        // Надпись "Ваша карта" над левым полем
        String myLabel = "Ваша карта";
        int labelWidth = fm.stringWidth(myLabel);
        int myFieldCenterX = SIDE_OFFSET + (GRID * CELL) / 2;
        g.drawString(myLabel, myFieldCenterX - labelWidth / 2, 540);
        
        // Надпись "Карта противника" или "Карта противника [имя]" над правым полем
        String opponentLabel = opponentName != null ? "Карта противника " + opponentName : "Карта противника";
        int opponentLabelWidth = fm.stringWidth(opponentLabel);
        int opponentFieldCenterX = SIDE_OFFSET + GRID * CELL + GRID_SPACING + (GRID * CELL) / 2;
        g.drawString(opponentLabel, opponentFieldCenterX - opponentLabelWidth / 2, 540);
    }

    private void drawTitle(Graphics g) {
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.setColor(Color.BLACK);
        String title = "Морской бой";
        FontMetrics fm = g.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        int centerX = getWidth() / 2;
        g.drawString(title, centerX - titleWidth / 2, 30);
    }

    private void drawGrid(Graphics g, int offsetX) {
        g.setColor(Color.GRAY);
        for (int i = 0; i <= GRID; i++) {
            g.drawLine(offsetX + i * CELL, TOP_OFFSET, offsetX + i * CELL, TOP_OFFSET + GRID * CELL);
            g.drawLine(offsetX, TOP_OFFSET + i * CELL, offsetX + GRID * CELL, TOP_OFFSET + i * CELL);
        }
    }

    private void drawMyField(Graphics g) {
        int offsetX = SIDE_OFFSET;
        
        // Рисуем корабли
        for (int x = 0; x < GRID; x++)
            for (int y = 0; y < GRID; y++)
                if (myField[x][y] == 1) {
                    g.setColor(Color.BLUE);
                    g.fillRect(offsetX + x * CELL, TOP_OFFSET + y * CELL, CELL, CELL);
                }
        
        // Рисуем попадания по моему полю
        for (int x = 0; x < GRID; x++)
            for (int y = 0; y < GRID; y++) {
                int cellX = offsetX + x * CELL;
                int cellY = TOP_OFFSET + y * CELL;
                
                if (myHits[x][y] == 1) {
                    // Попадание - красный крестик
                    drawCross(g, cellX, cellY, Color.RED);
                } else if (myHits[x][y] == 2) {
                    // Промах - серый крестик
                    drawCross(g, cellX, cellY, Color.GRAY);
                }
            }
    }

    private void drawOpponentField(Graphics g) {
        int offsetX = SIDE_OFFSET + GRID * CELL + GRID_SPACING;

        for (int x = 0; x < GRID; x++)
            for (int y = 0; y < GRID; y++) {
                int cellX = offsetX + x * CELL;
                int cellY = TOP_OFFSET + y * CELL;
                
                if (opponentField[x][y] == 1) {
                    // Попадание - красный крестик
                    drawCross(g, cellX, cellY, Color.RED);
                } else if (opponentField[x][y] == 2) {
                    // Промах - серый крестик
                    drawCross(g, cellX, cellY, Color.GRAY);
                }
            }
    }

    /**
     * Рисует крестик в центре клетки
     */
    private void drawCross(Graphics g, int cellX, int cellY, Color color) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(2));
        
        int padding = 14;  // Увеличенный отступ для меньшего крестика
        int x1 = cellX + padding;
        int y1 = cellY + padding;
        int x2 = cellX + CELL - padding;
        int y2 = cellY + CELL - padding;
        
        // Рисуем две диагональные линии
        g2d.drawLine(x1, y1, x2, y2);
        g2d.drawLine(x1, y2, x2, y1);
    }

    private void drawStatus(Graphics g) {
        g.setFont(new Font("Arial", Font.BOLD, 16));
        
        // Статус хода под заголовком (опущен ниже, чтобы не накладывался)
        String text = switch (state) {
            case YOUR_TURN -> "Ваш ход";
            case OPPONENT_TURN -> "Ход противника";
            case GAME_OVER -> "Игра окончена";
            default -> "Ожидание...";
        };
        g.setColor(Color.BLACK);
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int centerX = getWidth() / 2;
        g.drawString(text, centerX - textWidth / 2, 85); // Опущено ниже

    }
}
