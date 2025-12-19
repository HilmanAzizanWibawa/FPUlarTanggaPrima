import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.awt.geom.QuadCurve2D;
import javax.sound.sampled.*;
import java.io.File;
import javax.swing.Timer;

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
    private static final Color PRIME_COLOR = new Color(0xFFD700);
    private static final Color LINK_COLOR = new Color(0x4CAF50);

    private List<Player> players;
    private Map<Integer, SquarePanel> boardSquaresMap;
    private JButton lemparDaduButton;
    private JButton exitButton;
    private DicePanel diceDisplay;
    private JLabel statusLabel;
    private JPanel boardPanel;
    private JPanel playerStatusPanel;

    private Image mainBgImage;
    private Image controlBgImage;
    private Clip backgroundMusic;
    public FloatControl volumeControl;

    private final Map<Integer, Integer> links = new HashMap<>();
    private final int[][] papanData = new int[UKURAN_PAPAN][UKURAN_PAPAN];
    private int currentPlayerIndex = 0;
    private Random random = new Random();
    private boolean gameEnded = false;

    public PapanUlarTanggaGUI() {
        setupLinks();
        initializeBoardData();

        try {
            mainBgImage = new ImageIcon("images/BOS_1.png").getImage();
            controlBgImage = new ImageIcon("images/BOS_4.png").getImage();
        } catch (Exception e) {
            System.out.println("Gagal memuat gambar.");
        }

        playBackgroundMusic("sound/BOS_2.wav");
    }

    private void playBackgroundMusic(String filePath) {
        try {
            File musicPath = new File(filePath);
            if (musicPath.exists()) {
                AudioInputStream audioInput = AudioSystem.getAudioInputStream(musicPath);
                backgroundMusic = AudioSystem.getClip();
                backgroundMusic.open(audioInput);
                if (backgroundMusic.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    volumeControl = (FloatControl) backgroundMusic.getControl(FloatControl.Type.MASTER_GAIN);
                }
                backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
                backgroundMusic.start();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void playSoundEffect(String filePath) {
        new Thread(() -> {
            try {
                File soundPath = new File(filePath);
                if (soundPath.exists()) {
                    AudioInputStream audioInput = AudioSystem.getAudioInputStream(soundPath);
                    Clip sfx = AudioSystem.getClip();
                    sfx.open(audioInput);
                    sfx.start();

                    sfx.addLineListener(event -> {
                        if (event.getType() == LineEvent.Type.STOP) {
                            sfx.close();
                        }
                    });
                }
            } catch (Exception e) {
                System.err.println("Error SFX: " + e.getMessage());
            }
        }).start();
    }

    public void setVolume(float volume) {
        if (volumeControl != null) {
            float dB = (float) (Math.log(volume <= 0 ? 0.0001 : volume) / Math.log(10.0) * 20.0);
            volumeControl.setValue(dB);
        }
    }

    private void handleDiceRoll(ActionEvent e) {
        if (players.isEmpty() || gameEnded) return;

        lemparDaduButton.setEnabled(false);
        int diceRes = random.nextInt(6) + 1;
        diceDisplay.setDiceValue(diceRes);

        Player p = players.get(currentPlayerIndex);
        int oldPos = p.getPosition();
        int nextPos = oldPos + diceRes;

        if (nextPos <= 64) {
            playSoundEffect("sound/bos_3.wav");
            p.setPosition(nextPos);

            // Cek link (tangga/ular)
            if (links.containsKey(nextPos)) {
                int linkedPos = links.get(nextPos);
                Timer linkTimer = new Timer(500, evt -> {
                    p.setPosition(linkedPos);
                    updateBoardUI();
                    if (linkedPos > nextPos) {
                        playSoundEffect("sound/bos_4.wav"); // Tangga
                    } else {
                        playSoundEffect("sound/bos_5.wav"); // Ular
                    }
                    checkWinner(p);
                });
                linkTimer.setRepeats(false);
                linkTimer.start();
            } else {
                checkWinner(p);
            }
        }

        updateBoardUI();
        updatePlayerStatus();

        if (!gameEnded) {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            statusLabel.setText("Giliran: " + players.get(currentPlayerIndex).getName());
        }

        lemparDaduButton.setEnabled(!gameEnded);
    }

    private void checkWinner(Player p) {
        if (p.getPosition() >= 64 && !gameEnded) {
            gameEnded = true;
            playSoundEffect("sound/bos_6.wav");
            Timer winTimer = new Timer(500, evt -> showWinnerDialog(p));
            winTimer.setRepeats(false);
            winTimer.start();
        }
    }

    private void showWinnerDialog(Player winner) {
        JDialog dialog = new JDialog(this, "🎉 Pemenang!", true);
        dialog.setLayout(new BorderLayout());

        // Panel dengan animasi warna
        JPanel contentPanel = new JPanel() {
            private float hue = 0;
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;

                // Gradient background berubah warna
                Color color1 = Color.getHSBColor(hue, 0.8f, 0.9f);
                Color color2 = Color.getHSBColor((hue + 0.3f) % 1.0f, 0.8f, 0.6f);

                GradientPaint gradient = new GradientPaint(
                        0, 0, color1,
                        getWidth(), getHeight(), color2
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Gambar bintang-bintang
                g2d.setColor(new Color(255, 255, 255, 150));
                Random r = new Random(42);
                for (int i = 0; i < 50; i++) {
                    int x = r.nextInt(getWidth());
                    int y = r.nextInt(getHeight());
                    int size = 2 + r.nextInt(4);
                    g2d.fillOval(x, y, size, size);
                }
            }
        };
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(40, 50, 40, 50));

        // Animasi warna background
        Timer colorTimer = new Timer(50, new ActionListener() {
            float hue = 0;
            public void actionPerformed(ActionEvent e) {
                hue = (hue + 0.01f) % 1.0f;
                try {
                    java.lang.reflect.Field field = contentPanel.getClass().getDeclaredField("hue");
                    field.setAccessible(true);
                    field.setFloat(contentPanel, hue);
                } catch (Exception ex) {}
                contentPanel.repaint();
            }
        });
        colorTimer.start();

        JLabel trophyLabel = new JLabel("🏆", SwingConstants.CENTER);
        trophyLabel.setFont(new Font("Arial", Font.PLAIN, 80));
        trophyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(trophyLabel);
        contentPanel.add(Box.createVerticalStrut(20));

        JLabel winnerLabel = new JLabel("SELAMAT!", SwingConstants.CENTER);
        winnerLabel.setFont(new Font("Arial Black", Font.BOLD, 36));
        winnerLabel.setForeground(Color.WHITE);
        winnerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(winnerLabel);
        contentPanel.add(Box.createVerticalStrut(15));

        JLabel nameLabel = new JLabel(winner.getName(), SwingConstants.CENTER);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 28));
        nameLabel.setForeground(winner.getColor());
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(nameLabel);
        contentPanel.add(Box.createVerticalStrut(10));

        JLabel messageLabel = new JLabel("Telah memenangkan permainan!", SwingConstants.CENTER);
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        messageLabel.setForeground(Color.WHITE);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(messageLabel);
        contentPanel.add(Box.createVerticalStrut(30));

        JButton closeBtn = new JButton("Kembali ke Lobby");
        closeBtn.setFont(new Font("Arial", Font.BOLD, 16));
        closeBtn.setBackground(new Color(0xFFC700));
        closeBtn.setForeground(new Color(0x4A2C00));
        closeBtn.setFocusPainted(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setPreferredSize(new Dimension(200, 45));
        closeBtn.setMaximumSize(new Dimension(200, 45));
        closeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeBtn.addActionListener(e -> {
            colorTimer.stop();
            dialog.dispose();
            this.dispose();
            SwingUtilities.invokeLater(() -> new LobbyFrame(new PapanUlarTanggaGUI()));
        });

        contentPanel.add(closeBtn);

        dialog.add(contentPanel);
        dialog.setSize(450, 450);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JPanel createControlPanel() {
        JPanel cp = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (controlBgImage != null) g.drawImage(controlBgImage, 0, 0, getWidth(), getHeight(), this);
            }
        };
        cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));
        cp.setPreferredSize(new Dimension(250, 0));
        cp.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        cp.setOpaque(false);

        // Header "KONTROL"
        JLabel kontrolLabel = new JLabel("☐ KONTROL");
        kontrolLabel.setFont(new Font("Arial", Font.BOLD, 18));
        kontrolLabel.setForeground(Color.WHITE);
        kontrolLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cp.add(kontrolLabel);
        cp.add(Box.createVerticalStrut(15));

        // Status Giliran dengan border
        statusLabel = new JLabel("Giliran: -", SwingConstants.CENTER);
        statusLabel.setForeground(new Color(0xFFD700));
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cp.add(statusLabel);
        cp.add(Box.createVerticalStrut(15));

        // Panel Papan Dadu dengan border kuning
        JPanel dicePanel = new JPanel();
        dicePanel.setLayout(new BorderLayout());
        dicePanel.setOpaque(false);
        dicePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xFFD700), 3),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        dicePanel.setMaximumSize(new Dimension(220, 180));

        diceDisplay = new DicePanel(0);
        diceDisplay.setAlignmentX(Component.CENTER_ALIGNMENT);
        diceDisplay.setOpaque(false);
        dicePanel.add(diceDisplay, BorderLayout.CENTER);

        cp.add(dicePanel);
        cp.add(Box.createVerticalStrut(15));

        // Tombol Lempar Dadu
        lemparDaduButton = new JButton("⚅ LEMPAR DADU");
        lemparDaduButton.setFont(new Font("Arial", Font.BOLD, 14));
        lemparDaduButton.setBackground(new Color(0xFFD700));
        lemparDaduButton.setForeground(Color.BLACK);
        lemparDaduButton.setFocusPainted(false);
        lemparDaduButton.setMaximumSize(new Dimension(220, 40));
        lemparDaduButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        lemparDaduButton.addActionListener(this::handleDiceRoll);
        cp.add(lemparDaduButton);
        cp.add(Box.createVerticalStrut(15));

        // Tombol Kembali ke Lobby
        exitButton = new JButton("⟲ KEMBALI KE LOBBY");
        exitButton.setFont(new Font("Arial", Font.BOLD, 12));
        exitButton.setBackground(new Color(0xDC3545));
        exitButton.setForeground(Color.WHITE);
        exitButton.setFocusPainted(false);
        exitButton.setMaximumSize(new Dimension(220, 35));
        exitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitButton.addActionListener(e -> {
            this.dispose();
            SwingUtilities.invokeLater(() -> new LobbyFrame(new PapanUlarTanggaGUI()));
        });
        cp.add(exitButton);
        cp.add(Box.createVerticalStrut(15));

        // Panel Status Pemain dengan header
        JPanel turnSection = new JPanel();
        turnSection.setLayout(new BoxLayout(turnSection, BoxLayout.Y_AXIS));
        turnSection.setOpaque(false);
        turnSection.setMaximumSize(new Dimension(220, 300));
        turnSection.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));

        JLabel turnHeader = new JLabel("🎮 TURN:");
        turnHeader.setFont(new Font("Arial", Font.BOLD, 16));
        turnHeader.setForeground(new Color(0xFFD700));
        turnHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        turnSection.add(turnHeader);
        turnSection.add(Box.createVerticalStrut(10));

        playerStatusPanel = new JPanel();
        playerStatusPanel.setLayout(new BoxLayout(playerStatusPanel, BoxLayout.Y_AXIS));
        playerStatusPanel.setOpaque(false);
        playerStatusPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        turnSection.add(playerStatusPanel);

        cp.add(turnSection);
        cp.add(Box.createVerticalStrut(15));

        // Leaderboard section
        JPanel leaderboardSection = new JPanel();
        leaderboardSection.setLayout(new BoxLayout(leaderboardSection, BoxLayout.Y_AXIS));
        leaderboardSection.setOpaque(false);
        leaderboardSection.setMaximumSize(new Dimension(220, 200));

        JLabel leaderHeader = new JLabel("🏆 LEADERBOARD");
        leaderHeader.setFont(new Font("Arial", Font.BOLD, 16));
        leaderHeader.setForeground(new Color(0xFFD700));
        leaderHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        leaderboardSection.add(leaderHeader);
        leaderboardSection.add(Box.createVerticalStrut(10));

        playerStatusPanel = new JPanel();
        playerStatusPanel.setLayout(new BoxLayout(playerStatusPanel, BoxLayout.Y_AXIS));
        playerStatusPanel.setOpaque(false);
        playerStatusPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leaderboardSection.add(playerStatusPanel);

        cp.add(leaderboardSection);
        cp.add(Box.createVerticalGlue());

        return cp;
    }

    private void updatePlayerStatus() {
        playerStatusPanel.removeAll();

        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            JPanel playerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
            playerPanel.setOpaque(false);
            playerPanel.setMaximumSize(new Dimension(210, 28));

            // Icon emoji untuk pemain
            JLabel iconLabel = new JLabel("🎯");
            iconLabel.setFont(new Font("Arial", Font.PLAIN, 14));

            // Nama pemain dengan warna
            JLabel nameLabel = new JLabel(p.getName());
            nameLabel.setForeground(p.getColor());
            nameLabel.setFont(new Font("Arial", Font.BOLD, 13));

            // Posisi dan poin (selalu 0 untuk sekarang)
            JLabel posLabel = new JLabel("Posisi: " + p.getPosition() + " | Poin: 0");
            posLabel.setForeground(Color.WHITE);
            posLabel.setFont(new Font("Arial", Font.PLAIN, 11));

            JPanel infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setOpaque(false);

            JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            nameRow.setOpaque(false);
            nameRow.add(nameLabel);

            JPanel posRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            posRow.setOpaque(false);
            posRow.add(posLabel);

            infoPanel.add(nameRow);
            infoPanel.add(posRow);

            playerPanel.add(iconLabel);
            playerPanel.add(infoPanel);

            playerStatusPanel.add(playerPanel);
            playerStatusPanel.add(Box.createVerticalStrut(8));
        }

        // Tambahkan leaderboard ranking
        playerStatusPanel.add(Box.createVerticalStrut(10));

        // Sort players by position (descending)
        List<Player> sortedPlayers = new ArrayList<>(players);
        sortedPlayers.sort((p1, p2) -> Integer.compare(p2.getPosition(), p1.getPosition()));

        for (int i = 0; i < sortedPlayers.size(); i++) {
            Player p = sortedPlayers.get(i);
            JPanel rankPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
            rankPanel.setOpaque(false);
            rankPanel.setMaximumSize(new Dimension(210, 25));

            JLabel rankLabel = new JLabel((i + 1) + ". ");
            rankLabel.setForeground(Color.WHITE);
            rankLabel.setFont(new Font("Arial", Font.BOLD, 12));

            JLabel playerLabel = new JLabel(p.getName() + " - 0 poin");
            playerLabel.setForeground(p.getColor());
            playerLabel.setFont(new Font("Arial", Font.PLAIN, 12));

            rankPanel.add(rankLabel);
            rankPanel.add(playerLabel);

            playerStatusPanel.add(rankPanel);
        }

        playerStatusPanel.revalidate();
        playerStatusPanel.repaint();
    }

    private void setupLinks() {
        // Tangga (naik)
        links.put(2, 13);
        links.put(5, 19);
        links.put(11, 29);
        links.put(17, 36);
        links.put(22, 41);

        // Ular (turun)
        links.put(48, 26);
        links.put(55, 34);
        links.put(62, 18);
    }

    private void initializeBoardData() {
        int angka = 1;
        for (int baris = 0; baris < UKURAN_PAPAN; baris++) {
            int barisPapan = UKURAN_PAPAN - 1 - baris;
            if (baris % 2 == 0) {
                for (int k = 0; k < UKURAN_PAPAN; k++) papanData[barisPapan][k] = angka++;
            } else {
                for (int k = UKURAN_PAPAN - 1; k >= 0; k--) papanData[barisPapan][k] = angka++;
            }
        }
    }

    public void start(List<Player> initialPlayers) {
        this.players = initialPlayers;
        this.gameEnded = false;
        this.currentPlayerIndex = 0;

        setTitle("🎲 Ular Tangga Prima");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainContentPanel = new JPanel(new BorderLayout(10, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (mainBgImage != null) g.drawImage(mainBgImage, 0, 0, getWidth(), getHeight(), this);
            }
        };
        setContentPane(mainContentPanel);

        add(createBoardPanel(), BorderLayout.CENTER);
        add(createControlPanel(), BorderLayout.EAST);

        updateBoardUI();
        updatePlayerStatus();
        statusLabel.setText("Giliran: " + players.get(currentPlayerIndex).getName());

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createBoardPanel() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        boardPanel = new JPanel(new GridLayout(UKURAN_PAPAN, UKURAN_PAPAN, 2, 2));
        boardPanel.setBackground(new Color(0x8B0000));
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

    private void updateBoardUI() {
        for (SquarePanel s : boardSquaresMap.values()) s.clearPlayers();
        for (Player p : players) {
            SquarePanel s = boardSquaresMap.get(p.getPosition());
            if (s != null) s.addPlayer(p);
        }
        boardPanel.repaint();
    }

    private boolean isPrime(int n) {
        if (n < 2) return false;
        if (n == 2) return true;
        if (n % 2 == 0) return false;
        for (int i = 3; i * i <= n; i += 2) {
            if (n % i == 0) return false;
        }
        return true;
    }

    private class SquarePanel extends JPanel {
        private final int number;
        private final List<Player> playersHere = new ArrayList<>();

        public SquarePanel(int n) {
            this.number = n;
            setPreferredSize(new Dimension(UKURAN_KOTAK, UKURAN_KOTAK));

            // Warna kotak berdasarkan bilangan prima
            if (isPrime(n)) {
                setBackground(PRIME_COLOR);
            } else {
                setBackground(n % 2 == 0 ? BOARD_LIGHT : BOARD_DARK);
            }

            setBorder(BorderFactory.createLineBorder(BOARD_BORDER, 1));
            setLayout(null);
        }

        public void addPlayer(Player p) { playersHere.add(p); repaint(); }
        public void clearPlayers() { playersHere.clear(); repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Gambar nomor kotak
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.setColor(Color.BLACK);
            String numStr = String.valueOf(number);
            FontMetrics fm = g2d.getFontMetrics();
            int numWidth = fm.stringWidth(numStr);
            g2d.drawString(numStr, (getWidth() - numWidth) / 2, 20);

            // Gambar link jika ada
            if (links.containsKey(number)) {
                int target = links.get(number);
                g2d.setStroke(new BasicStroke(3));

                if (target > number) {
                    // Tangga (hijau)
                    g2d.setColor(new Color(0, 200, 0));
                    g2d.drawLine(10, getHeight() - 10, getWidth() - 10, 25);
                    g2d.drawLine(10, getHeight() - 20, getWidth() - 10, 35);
                    g2d.drawString("↑" + target, getWidth() - 25, getHeight() - 25);
                } else {
                    // Ular (merah)
                    g2d.setColor(new Color(200, 0, 0));
                    QuadCurve2D curve = new QuadCurve2D.Float(
                            getWidth() / 2, 30,
                            getWidth() / 2 + 15, getHeight() / 2,
                            getWidth() / 2, getHeight() - 10
                    );
                    g2d.draw(curve);
                    g2d.drawString("↓" + target, getWidth() - 25, getHeight() - 25);
                }
            }

            // Gambar pemain
            int playerSize = 18;
            int startX = 10;
            int startY = getHeight() - 35;

            for (int i = 0; i < playersHere.size(); i++) {
                Player p = playersHere.get(i);
                int x = startX + (i % 2) * (playerSize + 4);
                int y = startY - (i / 2) * (playerSize + 4);

                g2d.setColor(p.getColor());
                g2d.fillOval(x, y, playerSize, playerSize);
                g2d.setColor(Color.WHITE);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawOval(x, y, playerSize, playerSize);
            }
        }
    }

    private static class DicePanel extends JPanel {
        private int val;

        public DicePanel(int v) {
            this.val = v;
            setPreferredSize(new Dimension(140, 140));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        }

        public void setDiceValue(int v) {
            this.val = v;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Background putih sudah di-set di constructor

            // Gambar titik dadu
            g2d.setColor(Color.BLACK);
            int dotSize = 16;
            int centerX = getWidth() / 2 - dotSize / 2;
            int centerY = getHeight() / 2 - dotSize / 2;
            int offset = 35;

            switch(val) {
                case 1:
                    g2d.fillOval(centerX, centerY, dotSize, dotSize);
                    break;
                case 2:
                    g2d.fillOval(centerX - offset, centerY - offset, dotSize, dotSize);
                    g2d.fillOval(centerX + offset, centerY + offset, dotSize, dotSize);
                    break;
                case 3:
                    g2d.fillOval(centerX - offset, centerY - offset, dotSize, dotSize);
                    g2d.fillOval(centerX, centerY, dotSize, dotSize);
                    g2d.fillOval(centerX + offset, centerY + offset, dotSize, dotSize);
                    break;
                case 4:
                    g2d.fillOval(centerX - offset, centerY - offset, dotSize, dotSize);
                    g2d.fillOval(centerX + offset, centerY - offset, dotSize, dotSize);
                    g2d.fillOval(centerX - offset, centerY + offset, dotSize, dotSize);
                    g2d.fillOval(centerX + offset, centerY + offset, dotSize, dotSize);
                    break;
                case 5:
                    g2d.fillOval(centerX - offset, centerY - offset, dotSize, dotSize);
                    g2d.fillOval(centerX + offset, centerY - offset, dotSize, dotSize);
                    g2d.fillOval(centerX, centerY, dotSize, dotSize);
                    g2d.fillOval(centerX - offset, centerY + offset, dotSize, dotSize);
                    g2d.fillOval(centerX + offset, centerY + offset, dotSize, dotSize);
                    break;
                case 6:
                    g2d.fillOval(centerX - offset, centerY - offset - 10, dotSize, dotSize);
                    g2d.fillOval(centerX + offset, centerY - offset - 10, dotSize, dotSize);
                    g2d.fillOval(centerX - offset, centerY, dotSize, dotSize);
                    g2d.fillOval(centerX + offset, centerY, dotSize, dotSize);
                    g2d.fillOval(centerX - offset, centerY + offset + 10, dotSize, dotSize);
                    g2d.fillOval(centerX + offset, centerY + offset + 10, dotSize, dotSize);
                    break;
                default:
                    // Jika belum dilempar, tampilkan tanda tanya
                    g2d.setFont(new Font("Arial", Font.BOLD, 48));
                    String qMark = "?";
                    FontMetrics fm = g2d.getFontMetrics();
                    int x = (getWidth() - fm.stringWidth(qMark)) / 2;
                    int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                    g2d.drawString(qMark, x, y);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PapanUlarTanggaGUI game = new PapanUlarTanggaGUI();
            new LobbyFrame(game);
        });
    }
}