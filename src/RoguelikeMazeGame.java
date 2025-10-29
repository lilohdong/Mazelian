import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class RoguelikeMazeGame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameFrame());
    }
}

// ====================== Frame =========================
class GameFrame extends JFrame {
    public GameFrame() {
        setTitle("Roguelike Maze");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        GamePanel panel = new GamePanel(21, 21);
        add(panel);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
}

// ====================== Panel =========================
class GamePanel extends JPanel implements KeyListener {
    private final int TILE_SIZE = 30;
    private Tile[][] map;
    private Player player;
    private java.util.List<Enemy> enemies;
    private java.util.List<Item> items;
    private int width, height;
    private Random rand = new Random();

    public GamePanel(int w, int h) {
        this.width = w;
        this.height = h;
        setPreferredSize(new Dimension(w * TILE_SIZE, h * TILE_SIZE));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        generateStage();
    }

    private void generateStage() {
        map = new MapGenerator(width, height).generateMaze();
        enemies = new ArrayList<>();
        items = new ArrayList<>();

        // 플레이어 시작점 (1,1)
        player = new Player(1, 1);

        // 몬스터 2마리
        for (int i = 0; i < 2; i++) {
            int x, y;
            do {
                x = rand.nextInt(width);
                y = rand.nextInt(height);
            } while (!map[y][x].isWalkable());
            enemies.add(new Enemy(x, y));
        }

        // 포션 1개
        placeItem(ItemType.POTION);
        // 열쇠 1개
        placeItem(ItemType.KEY);
        // 출구
        placeExit();
    }

    private void placeItem(ItemType type) {
        int x, y;
        do {
            x = rand.nextInt(width);
            y = rand.nextInt(height);
        } while (!map[y][x].isWalkable());
        items.add(new Item(x, y, type));
    }

    private void placeExit() {
        int x, y;
        do {
            x = rand.nextInt(width);
            y = rand.nextInt(height);
        } while (!map[y][x].isWalkable());
        map[y][x].setType(TileType.EXIT);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int px = player.getX();
        int py = player.getY();

        for (int y = py - 1; y <= py + 1; y++) {
            for (int x = px - 1; x <= px + 1; x++) {
                if (!inBounds(x, y)) continue;
                Tile t = map[y][x];

                switch (t.getType()) {
                    case WALL -> g.setColor(Color.DARK_GRAY);
                    case PATH -> g.setColor(Color.LIGHT_GRAY);
                    case EXIT -> g.setColor(Color.GREEN);
                }
                g.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }

        // 아이템 그리기
        for (Item item : items) {
            if (Math.abs(item.getX() - px) <= 1 && Math.abs(item.getY() - py) <= 1) {
                if (item.getType() == ItemType.POTION) g.setColor(Color.PINK);
                else if (item.getType() == ItemType.KEY) g.setColor(Color.YELLOW);
                g.fillOval(item.getX() * TILE_SIZE + 8, item.getY() * TILE_SIZE + 8, 14, 14);
            }
        }

        // 적 그리기
        for (Enemy e : enemies) {
            if (Math.abs(e.getX() - px) <= 1 && Math.abs(e.getY() - py) <= 1) {
                g.setColor(Color.RED);
                g.fillRect(e.getX() * TILE_SIZE + 5, e.getY() * TILE_SIZE + 5, 20, 20);
            }
        }

        // 플레이어 그리기
        g.setColor(Color.BLUE);
        g.fillOval(px * TILE_SIZE + 5, py * TILE_SIZE + 5, 20, 20);
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && y < height && x < width;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int dx = 0, dy = 0;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W -> dy = -1;
            case KeyEvent.VK_S -> dy = 1;
            case KeyEvent.VK_A -> dx = -1;
            case KeyEvent.VK_D -> dx = 1;
        }

        int newX = player.getX() + dx;
        int newY = player.getY() + dy;

        if (inBounds(newX, newY) && map[newY][newX].isWalkable()) {
            player.move(dx, dy);
            checkInteractions();
            repaint();
        }
    }

    private void checkInteractions() {
        // 아이템 획득
        Iterator<Item> it = items.iterator();
        while (it.hasNext()) {
            Item item = it.next();
            if (item.getX() == player.getX() && item.getY() == player.getY()) {
                if (item.getType() == ItemType.POTION) {
                    JOptionPane.showMessageDialog(this, "포션 획득! HP 회복!");
                } else if (item.getType() == ItemType.KEY) {
                    player.setHasKey(true);
                    JOptionPane.showMessageDialog(this, "열쇠를 얻었다!");
                }
                it.remove();
            }
        }

        // 출구 도달
        if (map[player.getY()][player.getX()].getType() == TileType.EXIT) {
            if (player.hasKey()) {
                JOptionPane.showMessageDialog(this, "다음 스테이지로 이동!");
                generateStage();
            } else {
                JOptionPane.showMessageDialog(this, "열쇠가 필요합니다!");
            }
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
}

// ====================== Map Generator =========================
class MapGenerator {
    private int width, height;
    private Tile[][] map;
    private Random rand = new Random();

    public MapGenerator(int width, int height) {
        this.width = width;
        this.height = height;
        map = new Tile[height][width];
    }

    public Tile[][] generateMaze() {
        // 전부 벽으로 초기화
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                map[y][x] = new Tile(x, y, TileType.WALL);

        // DFS 미로 생성
        generateDFS(1, 1);
        return map;
    }

    private void generateDFS(int x, int y) {
        map[y][x].setType(TileType.PATH);
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        Collections.shuffle(Arrays.asList(dirs));

        for (int[] d : dirs) {
            int nx = x + d[0]*2, ny = y + d[1]*2;
            if (isInBounds(nx, ny) && map[ny][nx].isWall()) {
                map[y + d[1]][x + d[0]].setType(TileType.PATH);
                generateDFS(nx, ny);
            }
        }
    }

    private boolean isInBounds(int x, int y) {
        return x > 0 && y > 0 && x < width - 1 && y < height - 1;
    }
}

// ====================== Game Elements =========================
enum TileType { WALL, PATH, EXIT }

class Tile {
    private int x, y;
    private TileType type;

    public Tile(int x, int y, TileType type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public boolean isWalkable() {
        return type != TileType.WALL;
    }

    public boolean isWall() {
        return type == TileType.WALL;
    }

    public TileType getType() { return type; }
    public void setType(TileType t) { type = t; }
}

class Player {
    private int x, y;
    private boolean hasKey = false;
    public Player(int x, int y) { this.x = x; this.y = y; }
    public void move(int dx, int dy) { x += dx; y += dy; }
    public int getX() { return x; }
    public int getY() { return y; }
    public boolean hasKey() { return hasKey; }
    public void setHasKey(boolean v) { hasKey = v; }
}

class Enemy {
    private int x, y;
    public Enemy(int x, int y) { this.x = x; this.y = y; }
    public int getX() { return x; }
    public int getY() { return y; }
}

enum ItemType { POTION, KEY }

class Item {
    private int x, y;
    private ItemType type;
    public Item(int x, int y, ItemType type) {
        this.x = x; this.y = y; this.type = type;
    }
    public int getX() { return x; }
    public int getY() { return y; }
    public ItemType getType() { return type; }
}
