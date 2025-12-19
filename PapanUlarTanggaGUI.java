import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.PriorityQueue;
import javax.swing.Timer;
import java.awt.geom.QuadCurve2D;

public class PapanUlarTanggaGUI extends JFrame {

    private static final int UKURAN_PAPAN = 8;
    private static final int UKURAN_KOTAK = 70;

    public static final Color[] PLAYER_COLORS = {
            new Color(0xFF6B6B), new Color(0x4ECDC4), new Color(0xFFE66D),
            new Color(0x95E1D3), new Color(0xF38181), new Color(0xAA96DA)
    };

    private static final Color BOARD_LIGHT = new Color(0xFFE5E5);
    private static final Color BOARD_DARK = new Color(0xFFB3BA);
    private static final Color BOARD_BORDER = new Color(0x8B0000);
    private static final Color UI_BG = new Color(0x006064);
    private static final Color UI_TEXT = Color.WHITE;
    private static final Color PRIME_COLOR = new Color(0xFFD700);
    private static final Color LINK_COLOR = new Color(0x4CAF50);

    private List<Player> players;
    private JTextPane historyPane;
    private Map<Integer, SquarePanel> boardSquaresMap;
    private JButton lemparDaduButton;
    private JButton exitButton;
    private DicePanel diceDisplay;
    private JLabel statusLabel;
    private JPanel boardPanel;

    // Variabel Gambar Background
    private Image mainBgImage;    // BOS_1 (Belakang Papan)
    private Image controlBgImage; // BOS_4 (Belakang Kontrol)

    private final Map<Integer, Integer> links = new HashMap<>();
    private final Map<Integer, Integer> bonusPoints = new HashMap<>();
    private final int[][] papanData = new int[UKURAN_PAPAN][UKURAN_PAPAN];

    private int currentPlayerIndex = 0;
    private Random random = new Random();

    public PapanUlarTanggaGUI() {
        setupLinksAndBonus();
        initializeBoardData();
        initializePlayers();
        setupHistoryPane();

        // Memuat Gambar dari folder images
        try {
            mainBgImage = new ImageIcon("images/BOS_1.png").getImage();
            controlBgImage = new ImageIcon("images/BOS_4.png").getImage();
        } catch (Exception e) {
            System.out.println("Gagal memuat file gambar di folder images/");
        }
    }

    private void setupLinksAndBonus() {
        links.put(2, 13); links.put(3, 17); links.put(5, 19);
        links.put(7, 23); links.put(11, 29); links.put(13, 31);
        links.put(17, 37); links.put(19, 41); links.put(23, 43);
        links.put(29, 47); links.put(31, 53); links.put(37, 59);
        links.put(41, 61);
        for (int i = 1; i <= 64; i++) bonusPoints.put(i, 10 + random.nextInt(41));
        for (int prime : links.keySet()) bonusPoints.put(prime, bonusPoints.get(prime) + 30);
        bonusPoints.put(64, 100);
    }

    private boolean isPrime(int n) {
        if (n <= 1) return false;
        if (n <= 3) return true;
        if (n % 2 == 0 || n % 3 == 0) return false;
        for (int i = 5; i * i <= n; i += 6) if (n % i == 0 || n % (i + 2) == 0) return false;
        return true;
    }

    public void start(List<Player> initialPlayers) {
        this.players = initialPlayers;
        setTitle("🎲 Ular Tangga Prima - Linked Nodes");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Panel Utama dengan Background BOS_1
        JPanel mainContentPanel = new JPanel(new BorderLayout(10, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (mainBgImage != null && mainBgImage.getWidth(null) != -1) {
                    g.drawImage(mainBgImage, 0, 0, getWidth(), getHeight(), this);
                } else {
                    g.setColor(new Color(0x00838F));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        setContentPane(mainContentPanel);

        add(createBoardPanel(), BorderLayout.CENTER);
        add(createControlPanel(), BorderLayout.EAST);

        updateHistory();
        updateBoardUI();
        if (!players.isEmpty()) lemparDaduButton.setEnabled(true);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createBoardPanel() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false); // Transparan agar BOS_1 terlihat
        wrapper.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        boardPanel = new JPanel(new GridLayout(UKURAN_PAPAN, UKURAN_PAPAN, 0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                for (Map.Entry<Integer, Integer> entry : links.entrySet()) drawLink(g2d, entry.getKey(), entry.getValue());
            }

            private void drawLink(Graphics2D g2d, int from, int to) {
                Point fromPoint = getSquareCenter(from);
                Point toPoint = getSquareCenter(to);
                if (fromPoint == null || toPoint == null) return;
                g2d.setColor(LINK_COLOR);
                g2d.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int x1 = fromPoint.x, y1 = fromPoint.y, x2 = toPoint.x, y2 = toPoint.y;
                int ctrlX = (x1 + x2) / 2 + (y2 - y1) / 4;
                int ctrlY = (y1 + y2) / 2 - (x2 - x1) / 4;
                QuadCurve2D curve = new QuadCurve2D.Float(x1, y1, ctrlX, ctrlY, x2, y2);
                g2d.draw(curve);
                drawArrowHead(g2d, ctrlX, ctrlY, x2, y2);
            }

            private void drawArrowHead(Graphics2D g2d, int x1, int y1, int x2, int y2) {
                double angle = Math.atan2(y2 - y1, x2 - x1);
                int arrowSize = 15;
                int[] xPoints = {(int)x2, (int)(x2 - arrowSize * Math.cos(angle - Math.PI / 6)), (int)(x2 - arrowSize * Math.cos(angle + Math.PI / 6))};
                int[] yPoints = {(int)y2, (int)(y2 - arrowSize * Math.sin(angle - Math.PI / 6)), (int)(y2 - arrowSize * Math.sin(angle + Math.PI / 6))};
                g2d.fillPolygon(xPoints, yPoints, 3);
            }

            private Point getSquareCenter(int sq) {
                SquarePanel p = boardSquaresMap.get(sq);
                if (p == null) return null;
                Rectangle r = p.getBounds();
                return new Point(r.x + r.width / 2, r.y + r.height / 2);
            }
        };

        boardPanel.setBackground(Color.WHITE);
        boardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BOARD_BORDER, 8),
                BorderFactory.createLineBorder(new Color(0xFFD700), 3)
        ));

        boardSquaresMap = new HashMap<>();
        for (int baris = 0; baris < UKURAN_PAPAN; baris++) {
            for (int kolom = 0; kolom < UKURAN_PAPAN; kolom++) {
                int sq = papanData[baris][kolom];
                SquarePanel s = new SquarePanel(sq);
                boardSquaresMap.put(sq, s);
                boardPanel.add(s);
            }
        }
        wrapper.add(boardPanel);
        return wrapper;
    }

    private JPanel createControlPanel() {
        // Panel Kontrol dengan Background BOS_2
        JPanel cp = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (controlBgImage != null && controlBgImage.getWidth(null) != -1) {
                    g.drawImage(controlBgImage, 0, 0, getWidth(), getHeight(), this);
                } else {
                    g.setColor(UI_BG);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };

        // MENGATUR LAYOUT
        cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));
        cp.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        cp.setPreferredSize(new Dimension(320, 0));
        cp.setOpaque(false); // Penting agar background tergambar

        // 1. JUDUL
        JLabel title = new JLabel("<html><div style='text-align: center;'>KONTROL</div></html>");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        cp.add(title);
        cp.add(Box.createVerticalStrut(20));

        // 2. STATUS GILIRAN
        statusLabel = new JLabel("Giliran: Pemain 1");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setForeground(Color.YELLOW);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cp.add(statusLabel);
        cp.add(Box.createVerticalStrut(20));

        // 3. DISPLAY DADU
        diceDisplay = new DicePanel(0);
        diceDisplay.setMaximumSize(new Dimension(150, 150)); // Kunci agar muncul di BoxLayout
        diceDisplay.setPreferredSize(new Dimension(150, 150));
        diceDisplay.setAlignmentX(Component.CENTER_ALIGNMENT);
        cp.add(diceDisplay);
        cp.add(Box.createVerticalStrut(15));

        // 4. TOMBOL LEMPAR DADU
        lemparDaduButton = new JButton("LEMPAR DADU");
        lemparDaduButton.setBackground(new Color(0xFFD700));
        lemparDaduButton.setMaximumSize(new Dimension(200, 40));
        lemparDaduButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        lemparDaduButton.addActionListener(this::handleDiceRoll);
        cp.add(lemparDaduButton);
        cp.add(Box.createVerticalStrut(10));

        // 5. TOMBOL LOBBY
        exitButton = new JButton("KEMBALI KE LOBBY");
        exitButton.setBackground(new Color(0xD32F2F));
        exitButton.setForeground(Color.WHITE);
        exitButton.setMaximumSize(new Dimension(200, 40));
        exitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitButton.addActionListener(e -> returnToLobby());
        cp.add(exitButton);
        cp.add(Box.createVerticalStrut(20));

        // 6. HISTORY PANEL (Dibuat Transparan)
        JScrollPane sp = new JScrollPane(historyPane);
        sp.setPreferredSize(new Dimension(280, 300));
        sp.setMaximumSize(new Dimension(280, 300));
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.YELLOW), "History & Status",
                0, 0, null, Color.WHITE));
        cp.add(sp);

        return cp;
    }

    private void initializeBoardData() {
        int angka = 1;
        for (int baris = 0; baris < UKURAN_PAPAN; baris++) {
            int barisPapan = UKURAN_PAPAN - 1 - baris;
            if (baris % 2 == 0) for (int k = 0; k < UKURAN_PAPAN; k++) papanData[barisPapan][k] = angka++;
            else for (int k = UKURAN_PAPAN - 1; k >= 0; k--) papanData[barisPapan][k] = angka++;
        }
    }

    private void initializePlayers() { players = new ArrayList<>(); currentPlayerIndex = 0; }
    private void setupHistoryPane() { historyPane = new JTextPane(); historyPane.setContentType("text/html"); historyPane.setEditable(false); historyPane.setBackground(new Color(0x004D40)); }

    private void handleDiceRoll(ActionEvent e) {
        if (players.isEmpty()) return;
        lemparDaduButton.setEnabled(false);
        simulateDiceRoll(result -> {
            Player p = players.get(currentPlayerIndex);
            int dice = Integer.parseInt(result);
            int next = p.getPosition() + dice;
            if (next > 64) { moveToNextPlayer(); lemparDaduButton.setEnabled(true); return; }
            p.setPosition(next);
            updateBoardUI(); updateHistory();
            lemparDaduButton.setEnabled(true);
            moveToNextPlayer();
        });
    }

    private void simulateDiceRoll(java.util.function.Consumer<String> callback) {
        int res = random.nextInt(6) + 1;
        diceDisplay.setDiceValue(res);
        callback.accept(String.valueOf(res));
    }

    private void moveToNextPlayer() { currentPlayerIndex = (currentPlayerIndex + 1) % players.size(); updateHistory(); }
    private void updateBoardUI() {
        for (SquarePanel s : boardSquaresMap.values()) s.clearPlayers();
        for (Player p : players) { SquarePanel s = boardSquaresMap.get(p.getPosition()); if (s != null) s.addPlayer(p); }
        boardPanel.repaint();
    }

    private void updateHistory() {
        if (players.isEmpty()) return;
        Player c = players.get(currentPlayerIndex);
        statusLabel.setText("Giliran: " + c.getName());
    }

    private void returnToLobby() { this.dispose(); }

    private class SquarePanel extends JPanel {
        private final int number;
        private final List<Player> playersHere = new ArrayList<>();
        public SquarePanel(int n) {
            this.number = n;
            setPreferredSize(new Dimension(UKURAN_KOTAK, UKURAN_KOTAK));
            setBackground(isPrime(n) ? PRIME_COLOR : (n % 2 == 0 ? BOARD_LIGHT : BOARD_DARK));
            setBorder(BorderFactory.createLineBorder(Color.GRAY));
            add(new JLabel(String.valueOf(n)));
        }
        public void addPlayer(Player p) { playersHere.add(p); repaint(); }
        public void clearPlayers() { playersHere.clear(); repaint(); }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int x = 5;
            for (Player p : playersHere) {
                g.setColor(p.getColor());
                g.fillOval(x, getHeight() - 25, 20, 20);
                x += 25;
            }
        }
    }

    private static class DicePanel extends JPanel {
        private int val;
        public DicePanel(int v) { this.val = v; setPreferredSize(new Dimension(80, 80)); setBackground(Color.WHITE); }
        public void setDiceValue(int v) { this.val = v; repaint(); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawString("Dadu: " + val, 20, 40);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PapanUlarTanggaGUI game = new PapanUlarTanggaGUI();
            // Catatan: Pastikan class LobbyFrame sudah ada di project Anda
            new LobbyFrame(game);
        });
    }
}