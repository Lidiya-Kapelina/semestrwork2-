import java.io.IOException;

public class PlayersRoom {

    private ClientHandler p1, p2;
    private int[][] f1, f2;  // поля кораблей (1 = корабль, 0 = пусто)
    private int[][] hits1, hits2;  // поля попаданий (0 = нет попадания, 1 = попадание, 2 = промах)
    private boolean p1Ready = false, p2Ready = false;
    private boolean p1Turn = true;

    public void addPlayer(ClientHandler p) {
        if (p1 == null) {
            p1 = p;
            p.setRoom(this);
            send(p1, "WAIT_PLAYER");
        } else {
            p2 = p;
            p.setRoom(this);

            // 🔥 ОБА ИГРОКА ВИДЯТ ДРУГ ДРУГА
            send(p1, "START_GAME|" + p2.getUsername());
            send(p2, "START_GAME|" + p1.getUsername());
        }
    }

    private void send(ClientHandler p, String msg) {
        try {
            p.getWriter().write(msg + "\n");
            p.getWriter().flush();
        } catch (IOException ignored) {}
    }



    public boolean isFullRoom() {
        return p1 != null && p2 != null;
    }

    public void removePlayer(ClientHandler p) {
        if (p == p1) p1 = null;
        if (p == p2) p2 = null;
    }

    public void handleReady(ClientHandler p, int[][] field) throws IOException {

        if (p == p1) {
            f1 = field;
            hits1 = new int[10][10];  // инициализируем поле попаданий
            p1Ready = true;
        } else {
            f2 = field;
            hits2 = new int[10][10];  // инициализируем поле попаданий
            p2Ready = true;
        }

        if (p1Ready && p2Ready) {
            send(p1, "YOUR_TURN");
            send(p2, "WAIT");
            p1Turn = true;
        }
    }



    public void handleFire(ClientHandler shooter, int x, int y) throws IOException {
        // Проверка очереди хода
        if ((shooter == p1 && !p1Turn) || (shooter == p2 && p1Turn)) return;

        ClientHandler target = shooter == p1 ? p2 : p1;
        int[][] targetField = shooter == p1 ? f2 : f1;
        int[][] targetHits = shooter == p1 ? hits2 : hits1;

        // Проверка на повторный выстрел
        if (targetHits[x][y] != 0) return;  // уже был выстрел в эту клетку

        boolean isHit = (targetField[x][y] == 1);
        
        if (isHit) {
            targetHits[x][y] = 1;  // попадание
            send(shooter, "HIT|" + x + "|" + y);  // стреляющему
            send(target, "MY_HIT|" + x + "|" + y);  // обстреливаемому
            
            // Проверяем, полностью ли уничтожен корабль
            if (isShipDestroyed(targetField, targetHits, x, y)) {
                // Отмечаем все соседние клетки как промахи
                markAdjacentCellsAsMiss(targetField, targetHits, x, y, shooter, target);
            }
        } else {
            targetHits[x][y] = 2;  // промах
            send(shooter, "MISS|" + x + "|" + y);  // стреляющему
            send(target, "MY_MISS|" + x + "|" + y);  // обстреливаемому
        }

        // Проверка на окончание игры (все корабли подбиты)
        if (isGameOver(targetField, targetHits)) {
            send(p1, "GAME_OVER|" + shooter.getUsername());
            send(p2, "GAME_OVER|" + shooter.getUsername());
            return;
        }

        // Переключение хода только при промахе
        if (!isHit) {
            p1Turn = !p1Turn;
            updateTurns();
        } else {
            // При попадании ход остается у того же игрока
            if (p1Turn) {
                send(p1, "YOUR_TURN");
                send(p2, "WAIT");
            } else {
                send(p2, "YOUR_TURN");
                send(p1, "WAIT");
            }
        }
    }

    private void updateTurns() throws IOException {
        if (p1Turn) {
            p1.getWriter().write("YOUR_TURN\n");
            p2.getWriter().write("WAIT\n");
        } else {
            p2.getWriter().write("YOUR_TURN\n");
            p1.getWriter().write("WAIT\n");
        }
        p1.getWriter().flush();
        p2.getWriter().flush();
    }

    private boolean isGameOver(int[][] field, int[][] hits) {
        // Игра окончена, если все клетки с кораблями (field[x][y] == 1) 
        // имеют попадания (hits[x][y] == 1)
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                if (field[x][y] == 1 && hits[x][y] != 1) {
                    return false;  // есть неподбитый корабль
                }
            }
        }
        return true;  // все корабли подбиты
    }

    /**
     * Проверяет, полностью ли уничтожен корабль, содержащий клетку (x, y)
     */
    private boolean isShipDestroyed(int[][] field, int[][] hits, int startX, int startY) {
        boolean[][] visited = new boolean[10][10];
        return isShipFullyHit(field, hits, visited, startX, startY);
    }

    /**
     * Рекурсивно проверяет, все ли клетки корабля имеют попадания
     */
    private boolean isShipFullyHit(int[][] field, int[][] hits, boolean[][] visited, int x, int y) {
        // Выход за границы
        if (x < 0 || x >= 10 || y < 0 || y >= 10) return true;
        
        // Уже посещали эту клетку
        if (visited[x][y]) return true;
        
        // Это не корабль - пропускаем
        if (field[x][y] != 1) return true;
        
        // Отмечаем как посещенную
        visited[x][y] = true;
        
        // Если у этой клетки корабля нет попадания, корабль не уничтожен
        if (hits[x][y] != 1) return false;
        
        // Проверяем соседние клетки (только горизонтально и вертикально для поиска всего корабля)
        boolean result = true;
        result = isShipFullyHit(field, hits, visited, x + 1, y) && result;
        result = isShipFullyHit(field, hits, visited, x - 1, y) && result;
        result = isShipFullyHit(field, hits, visited, x, y + 1) && result;
        result = isShipFullyHit(field, hits, visited, x, y - 1) && result;
        
        return result;
    }

    /**
     * Находит все клетки корабля и отмечает соседние клетки как промахи
     */
    private void markAdjacentCellsAsMiss(int[][] field, int[][] hits, int startX, int startY, 
                                         ClientHandler shooter, ClientHandler target) throws IOException {
        boolean[][] shipCells = new boolean[10][10];
        boolean[][] visited = new boolean[10][10];
        
        // Находим все клетки корабля
        markShipCells(field, shipCells, visited, startX, startY);
        
        // Для каждой клетки корабля отмечаем соседние клетки как промахи
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                if (shipCells[x][y]) {
                    // Проверяем все 8 соседних клеток (включая диагонали)
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            int nx = x + dx;
                            int ny = y + dy;
                            
                            // Проверяем границы
                            if (nx < 0 || nx >= 10 || ny < 0 || ny >= 10) continue;
                            
                            // Пропускаем саму клетку корабля
                            if (dx == 0 && dy == 0) continue;
                            
                            // Если это клетка корабля, пропускаем
                            if (field[nx][ny] == 1) continue;
                            
                            // Если уже была отметка, пропускаем
                            if (hits[nx][ny] != 0) continue;
                            
                            // Отмечаем как промах
                            hits[nx][ny] = 2;
                            send(shooter, "MISS|" + nx + "|" + ny);
                            send(target, "MY_MISS|" + nx + "|" + ny);
                        }
                    }
                }
            }
        }
    }

    /**
     * Рекурсивно помечает все клетки корабля
     */
    private void markShipCells(int[][] field, boolean[][] shipCells, boolean[][] visited, int x, int y) {
        // Выход за границы
        if (x < 0 || x >= 10 || y < 0 || y >= 10) return;
        
        // Уже посещали эту клетку
        if (visited[x][y]) return;
        
        // Это не корабль
        if (field[x][y] != 1) return;
        
        // Отмечаем как посещенную и как клетку корабля
        visited[x][y] = true;
        shipCells[x][y] = true;
        
        // Проверяем соседние клетки (только горизонтально и вертикально)
        markShipCells(field, shipCells, visited, x + 1, y);
        markShipCells(field, shipCells, visited, x - 1, y);
        markShipCells(field, shipCells, visited, x, y + 1);
        markShipCells(field, shipCells, visited, x, y - 1);
    }
}
