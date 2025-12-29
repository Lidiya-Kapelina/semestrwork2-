import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class ShipPlacementGUI extends JFrame {

    private static final int CELL = 40;
    private static final int GRID = 10;

    private ShipPlacementPanel panel;
    private Client client;

    public ShipPlacementGUI(Client client) {
        this.client = client;

        setTitle("Расстановка кораблей");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        panel = new ShipPlacementPanel(client);
        add(panel, BorderLayout.CENTER);

        JButton readyButton = new JButton("Готов");
        add(readyButton, BorderLayout.SOUTH);

        readyButton.addActionListener(e -> {
            if (!panel.allPlaced()) {
                JOptionPane.showMessageDialog(this, "Расставьте все корабли", "Ошибка", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int[][] field = panel.buildField();
            client.sendMessage(buildReadyMessage(field));

            GamePanel gamePanel = new GamePanel(client, field);
            client.listenForMessage(gamePanel);

            JFrame gameFrame = new JFrame("Морской бой");
            gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            gameFrame.add(gamePanel);
            gameFrame.pack();
            gameFrame.setLocationRelativeTo(null);
            gameFrame.setVisible(true);

            dispose();
        });

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private String buildReadyMessage(int[][] field) {
        StringBuilder sb = new StringBuilder("READY");
        for (int y = 0; y < GRID; y++)
            for (int x = 0; x < GRID; x++)
                sb.append("|").append(field[x][y]);
        return sb.toString();
    }

    // ---------------------- Панель для расстановки ----------------------
    private class ShipPlacementPanel extends JPanel {
        private static final int GRID_OFFSET_X = 60;  // Отступ сетки слева
        private static final int GRID_OFFSET_Y = 60;  // Отступ сетки сверху
        
        private List<Ship> ships = new ArrayList<>();
        private Ship draggedShip;
        private int offsetX, offsetY;

        public ShipPlacementPanel(Client client) {
            setPreferredSize(new Dimension(800, 550));
            setBackground(Color.WHITE);

            // Добавляем корабли (с учетом новых отступов)
            int shipAreaX = GRID_OFFSET_X + GRID * CELL + 80;
            ships.add(new Ship(shipAreaX, 60, 4));
            ships.add(new Ship(shipAreaX, 140, 3));
            ships.add(new Ship(shipAreaX, 220, 3));
            ships.add(new Ship(shipAreaX, 300, 2));
            ships.add(new Ship(shipAreaX, 360, 2));
            ships.add(new Ship(shipAreaX, 420, 1));
            ships.add(new Ship(shipAreaX + 50, 420, 1));

            MouseAdapter mouseHandler = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    for (Ship ship : ships) {
                        Rectangle r = new Rectangle(ship.x, ship.y, ship.getWidth(CELL), ship.getHeight(CELL));
                        if (r.contains(e.getPoint())) {
                            if (SwingUtilities.isRightMouseButton(e)) {
                                ship.rotate();
                                PlacementResult result = checkPlacement(ship);
                                if (result != PlacementResult.OK) ship.rotate();
                                snapToGrid(ship);
                                repaint();
                                return;
                            }
                            draggedShip = ship;
                            offsetX = e.getX() - ship.x;
                            offsetY = e.getY() - ship.y;
                            return;
                        }
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (draggedShip != null) {
                        draggedShip.x = e.getX() - offsetX;
                        draggedShip.y = e.getY() - offsetY;
                        repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (draggedShip != null) {
                        snapToGrid(draggedShip);
                        PlacementResult result = checkPlacement(draggedShip);
                        draggedShip.placed = (result == PlacementResult.OK);
                        if (result != PlacementResult.OK) showPlacementError(result);
                        draggedShip = null;
                        repaint();
                    }
                }
            };

            addMouseListener(mouseHandler);
            addMouseMotionListener(mouseHandler);
        }

        private void snapToGrid(Ship ship) {
            // Учитываем отступы при привязке к сетке
            int gridX = (ship.x - GRID_OFFSET_X) / CELL;
            int gridY = (ship.y - GRID_OFFSET_Y) / CELL;
            ship.x = GRID_OFFSET_X + gridX * CELL;
            ship.y = GRID_OFFSET_Y + gridY * CELL;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            drawLabels(g);
            drawGrid(g);
            drawShips(g);
        }

        private void drawLabels(Graphics g) {
            g.setFont(new Font("Arial", Font.BOLD, 14));
            g.setColor(Color.BLACK);
            FontMetrics fm = g.getFontMetrics();
            
            // Буквы по горизонтали (A-J)
            String[] letters = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};
            for (int i = 0; i < GRID; i++) {
                int letterWidth = fm.stringWidth(letters[i]);
                int x = GRID_OFFSET_X + i * CELL + (CELL - letterWidth) / 2;
                g.drawString(letters[i], x, GRID_OFFSET_Y - 8);
            }
            
            // Цифры по вертикали (1-10)
            for (int i = 0; i < GRID; i++) {
                String number = String.valueOf(i + 1);
                int numberWidth = fm.stringWidth(number);
                int y = GRID_OFFSET_Y + i * CELL + CELL / 2 + fm.getAscent() / 2 - 2;
                g.drawString(number, GRID_OFFSET_X - numberWidth - 8, y);
            }
        }

        private void drawGrid(Graphics g) {
            g.setColor(Color.LIGHT_GRAY);
            int gridStartX = GRID_OFFSET_X;
            int gridStartY = GRID_OFFSET_Y;
            
            for (int i = 0; i <= GRID; i++) {
                g.drawLine(gridStartX + i * CELL, gridStartY, gridStartX + i * CELL, gridStartY + GRID * CELL);
                g.drawLine(gridStartX, gridStartY + i * CELL, gridStartX + GRID * CELL, gridStartY + i * CELL);
            }
        }

        private void drawShips(Graphics g) {
            for (Ship ship : ships) {
                g.setColor(ship.placed ? Color.BLUE : new Color(0, 0, 255, 128));
                g.fillRect(ship.x, ship.y, ship.getWidth(CELL), ship.getHeight(CELL));
                g.setColor(Color.BLACK);
                g.drawRect(ship.x, ship.y, ship.getWidth(CELL), ship.getHeight(CELL));
            }
        }

        public boolean allPlaced() {
            for (Ship ship : ships) if (!ship.placed) return false;
            return true;
        }

        public int[][] buildField() {
            int[][] field = new int[GRID][GRID];
            for (Ship ship : ships) {
                int x = (ship.x - GRID_OFFSET_X) / CELL;
                int y = (ship.y - GRID_OFFSET_Y) / CELL;
                for (int i = 0; i < ship.length; i++)
                    if (ship.horizontal) field[x + i][y] = 1;
                    else field[x][y + i] = 1;
            }
            return field;
        }

        private PlacementResult checkPlacement(Ship ship) {
            int gridX = (ship.x - GRID_OFFSET_X) / CELL;
            int gridY = (ship.y - GRID_OFFSET_Y) / CELL;
            if (gridX < 0 || gridY < 0) return PlacementResult.OUT_OF_BOUNDS;
            if (ship.horizontal && gridX + ship.length > GRID) return PlacementResult.OUT_OF_BOUNDS;
            if (!ship.horizontal && gridY + ship.length > GRID) return PlacementResult.OUT_OF_BOUNDS;

            for (int i = 0; i < ship.length; i++) {
                int x = ship.horizontal ? gridX + i : gridX;
                int y = ship.horizontal ? gridY : gridY + i;

                for (int dx = -1; dx <= 1; dx++)
                    for (int dy = -1; dy <= 1; dy++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        if (nx < 0 || ny < 0 || nx >= GRID || ny >= GRID) continue;
                        for (Ship other : ships) {
                            if (other == ship || !other.placed) continue;
                            if (isCellOccupied(other, nx, ny)) {
                                if (dx == 0 && dy == 0) return PlacementResult.INTERSECTION;
                                else return PlacementResult.TOO_CLOSE;
                            }
                        }
                    }
            }
            return PlacementResult.OK;
        }

        private boolean isCellOccupied(Ship ship, int x, int y) {
            int sx = (ship.x - GRID_OFFSET_X) / CELL;
            int sy = (ship.y - GRID_OFFSET_Y) / CELL;
            for (int i = 0; i < ship.length; i++) {
                int cx = ship.horizontal ? sx + i : sx;
                int cy = ship.horizontal ? sy : sy + i;
                if (cx == x && cy == y) return true;
            }
            return false;
        }

        private void showPlacementError(PlacementResult result) {
            String message;
            switch (result) {
                case OUT_OF_BOUNDS -> message = "Корабль выходит за пределы поля";
                case INTERSECTION -> message = "Корабли не могут пересекаться";
                case TOO_CLOSE -> message = "Корабли не могут соприкасаться";
                default -> message = "";
            }
            if (!message.isEmpty())
                JOptionPane.showMessageDialog(this, message, "Ошибка размещения", JOptionPane.ERROR_MESSAGE);
        }
    }
}
