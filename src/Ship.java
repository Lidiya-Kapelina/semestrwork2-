public class Ship {
    int x, y;          // пиксели
    int length;
    boolean horizontal = true;
    boolean placed = false;

    public Ship(int x, int y, int length) {
        this.x = x;
        this.y = y;
        this.length = length;
    }

    public int getWidth(int cell) {
        return horizontal ? length * cell : cell;
    }

    public int getHeight(int cell) {
        return horizontal ? cell : length * cell;
    }

    public void rotate() {
        horizontal = !horizontal;
    }
}
