import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
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

    private List<Player> players;
    private Map<Integer, SquarePanel> boardSquaresMap;
    private JButton lemparDaduButton;
    private JButton exitButton;
    private DicePanel diceDisplay;
    private JLabel turnLabel;
    private JPanel boardPanel;
    private JPanel playerListPanel;
    private JPanel leaderboardPanel;

    private Image mainBgImage;
    private Clip backgroundMusic;
    public FloatControl volumeControl;

    private final Map<Integer, Integer> links = new HashMap<>();
    private final Map<Integer, Integer> nodeBonusPoints = new HashMap<>();
    private final int[][] papanData = new int[UKURAN_PAPAN][UKURAN_PAPAN];
    private int currentPlayerIndex = 0;
    private Random random = new Random();
    private boolean gameEnded = false;

    public PapanUlarTanggaGUI() {
        setupLinks();
        setupNodeBonusPoints();
        initializeBoardData();

        try {
            mainBgImage = new ImageIcon("images/BOS_1.png").getImage();
        } catch (Exception e) {
            System.out.println("Gagal memuat gambar background.");
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
                        if (event.getType() == LineEvent.Type.STOP) sfx.close();
                    });
                }
            } catch (Exception e) { System.err.println("Error SFX: " + e.getMessage()); }
        }).start();
    }

    public void setVolume(float volume) {
        if (volumeControl != null) {
            float dB = (float) (Math.log(volume <= 0 ? 0.0001 : volume) / Math.log(10.0) * 20.0);
            volumeControl.setValue(dB);
        }
    }

    private void setupNodeBonusPoints() {
        // Setiap node memiliki bonus point acak antara 10-50
        for (int i = 1; i <= 64; i++) {
            nodeBonusPoints.put(i, 10 + random.nextInt(41));
        }
        // Nodes khusus mendapat bonus lebih besar
        nodeBonusPoints.put(64, 100); // Finishing node
        for (int key : links.keySet()) {
            nodeBonusPoints.put(links.get(key), 50 + random.nextInt(51)); // Target tangga
        }
    }

    private void handleDiceRoll(ActionEvent e) {
        if (players.isEmpty() || gameEnded) return;

        lemparDaduButton.setEnabled(false);
        int diceRes = random.nextInt(9) - 2;
        if (diceRes == 0) diceRes = 1;

        diceDisplay.setDiceValue(diceRes);
        Player p = players.get(currentPlayerIndex);

        int targetPos = Math.max(1, Math.min(p.getPosition() + diceRes, 64));
        moveAnimated(p, targetPos);
    }

    private void moveAnimated(Player p, int targetPos) {
        Timer moveTimer = new Timer(300, null);
        moveTimer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (p.getPosition() < targetPos) {
                    p.setPosition(p.getPosition() + 1);
                } else if (p.getPosition() > targetPos) {
                    p.setPosition(p.getPosition() - 1);
                } else {
                    moveTimer.stop();
                    // Collect bonus point from end node
                    int bonusFromNode = nodeBonusPoints.getOrDefault(targetPos, 0);
                    p.addBonusPoints(bonusFromNode);
                    processTileEnd(p);
                    return;
                }
                updateBoardUI();
                updatePlayerStatus();
            }
        });
        moveTimer.start();
    }

    private void processTileEnd(Player p) {
        int currentPos = p.getPosition();
        if (links.containsKey(currentPos)) {
            int linkedPos = links.get(currentPos);
            Timer ladderTimer = new Timer(500, evt -> {
                p.setPosition(linkedPos);
                // Collect bonus from ladder target
                int bonusFromTarget = nodeBonusPoints.getOrDefault(linkedPos, 0);
                p.addBonusPoints(bonusFromTarget);
                playSoundEffect("sound/bos_4.wav");
                updateBoardUI();
                updatePlayerStatus();
                checkWinner(p);
                finishTurn();
            });
            ladderTimer.setRepeats(false);
            ladderTimer.start();
        } else {
            updatePlayerStatus();
            checkWinner(p);
            finishTurn();
        }
    }

    private void finishTurn() {
        if (!gameEnded) {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            updateTurnIndicator();
            lemparDaduButton.setEnabled(true);
        }
    }

    private void checkWinner(Player p) {
        if (p.getPosition() >= 64 && !gameEnded) {
            gameEnded = true;
            playSoundEffect("sound/bos_6.wav");

            PriorityQueue<Player> leaderboard = new PriorityQueue<>();
            leaderboard.addAll(players);

            Timer winTimer = new Timer(500, evt -> showWinnerDialog(leaderboard));
            winTimer.setRepeats(false);
            winTimer.start();
        }
    }

    private JPanel createControlPanel() {
        JPanel cp = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(0x004D40),
                        0, getHeight(), new Color(0x00695C)
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));
        cp.setPreferredSize(new Dimension(280, 0));
        cp.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Turn Indicator Section
        JPanel turnPanel = new JPanel();
        turnPanel.setLayout(new BoxLayout(turnPanel, BoxLayout.Y_AXIS));
        turnPanel.setOpaque(false);
        turnPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xFFC107), 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        turnPanel.setMaximumSize(new Dimension(250, 100));

        JLabel turnTitle = new JLabel("🎯 TURN:");
        turnTitle.setFont(new Font("Arial", Font.BOLD, 14));
        turnTitle.setForeground(new Color(0xFFC107));
        turnTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        turnLabel = new JLabel("Pemain 1");
        turnLabel.setFont(new Font("Arial", Font.BOLD, 18));
        turnLabel.setForeground(Color.WHITE);
        turnLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel posPointLabel = new JLabel("Posisi: 1 | Poin: 0");
        posPointLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        posPointLabel.setForeground(Color.YELLOW);
        posPointLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        posPointLabel.setName("posPointLabel");

        turnPanel.add(turnTitle);
        turnPanel.add(Box.createVerticalStrut(5));
        turnPanel.add(turnLabel);
        turnPanel.add(posPointLabel);

        cp.add(turnPanel);
        cp.add(Box.createVerticalStrut(15));

        // Player List Section
        JPanel playerSection = new JPanel();
        playerSection.setLayout(new BoxLayout(playerSection, BoxLayout.Y_AXIS));
        playerSection.setOpaque(false);
        playerSection.setMaximumSize(new Dimension(250, 200));

        JLabel playerTitle = new JLabel("👥 PEMAIN");
        playerTitle.setFont(new Font("Arial", Font.BOLD, 14));
        playerTitle.setForeground(Color.WHITE);
        playerTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        playerListPanel = new JPanel();
        playerListPanel.setLayout(new BoxLayout(playerListPanel, BoxLayout.Y_AXIS));
        playerListPanel.setOpaque(false);

        playerSection.add(playerTitle);
        playerSection.add(Box.createVerticalStrut(5));
        playerSection.add(playerListPanel);

        cp.add(playerSection);
        cp.add(Box.createVerticalStrut(15));

        // Leaderboard Section
        JPanel leaderboardSection = new JPanel();
        leaderboardSection.setLayout(new BoxLayout(leaderboardSection, BoxLayout.Y_AXIS));
        leaderboardSection.setOpaque(false);
        leaderboardSection.setMaximumSize(new Dimension(250, 200));

        JLabel leaderTitle = new JLabel("🏆 LEADERBOARD");
        leaderTitle.setFont(new Font("Arial", Font.BOLD, 14));
        leaderTitle.setForeground(new Color(0xFFC107));
        leaderTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        leaderboardPanel = new JPanel();
        leaderboardPanel.setLayout(new BoxLayout(leaderboardPanel, BoxLayout.Y_AXIS));
        leaderboardPanel.setOpaque(false);

        leaderboardSection.add(leaderTitle);
        leaderboardSection.add(Box.createVerticalStrut(5));
        leaderboardSection.add(leaderboardPanel);

        cp.add(leaderboardSection);
        cp.add(Box.createVerticalStrut(15));

        // Dice Panel
        JPanel diceWrapper = new JPanel();
        diceWrapper.setLayout(new BoxLayout(diceWrapper, BoxLayout.Y_AXIS));
        diceWrapper.setOpaque(false);
        diceWrapper.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 2));
        diceWrapper.setMaximumSize(new Dimension(240, 200));

        diceDisplay = new DicePanel(0);
        diceDisplay.setAlignmentX(Component.CENTER_ALIGNMENT);
        diceWrapper.add(Box.createVerticalStrut(10));
        diceWrapper.add(diceDisplay);

        lemparDaduButton = new JButton("⚄ LEMPAR DADU");
        lemparDaduButton.setBackground(new Color(0xFFC107));
        lemparDaduButton.setFont(new Font("Arial", Font.BOLD, 14));
        lemparDaduButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        lemparDaduButton.addActionListener(this::handleDiceRoll);
        diceWrapper.add(Box.createVerticalStrut(10));
        diceWrapper.add(lemparDaduButton);
        diceWrapper.add(Box.createVerticalStrut(10));

        cp.add(diceWrapper);
        cp.add(Box.createVerticalStrut(15));

        // Settings Button
        JButton settingsBtn = new JButton("⚙️ PENGATURAN");
        settingsBtn.setBackground(new Color(0x4CAF50));
        settingsBtn.setForeground(Color.WHITE);
        settingsBtn.setMaximumSize(new Dimension(240, 35));
        settingsBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        settingsBtn.addActionListener(e -> showInGameSettings());
        cp.add(settingsBtn);
        cp.add(Box.createVerticalStrut(10));

        // Exit Button
        exitButton = new JButton("🔙 KEMBALI KE LOBBY");
        exitButton.setBackground(new Color(0xD32F2F));
        exitButton.setForeground(Color.WHITE);
        exitButton.setMaximumSize(new Dimension(240, 35));
        exitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitButton.addActionListener(e -> {
            this.dispose();
            SwingUtilities.invokeLater(() -> new LobbyFrame(new PapanUlarTanggaGUI()));
        });
        cp.add(exitButton);

        return cp;
    }

    private void showInGameSettings() {
        JDialog dialog = new JDialog(this, "⚙️ Pengaturan Permainan", true);
        dialog.setLayout(new BorderLayout());

        JPanel contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(0x1A237E),
                        0, getHeight(), new Color(0x311B92)
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        JLabel titleLabel = new JLabel("🔊 Kontrol Volume");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 26));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(30));

        JSlider volSlider = new JSlider(0, 100);
        volSlider.setOpaque(false);
        volSlider.setForeground(Color.WHITE);
        volSlider.setMajorTickSpacing(25);
        volSlider.setMinorTickSpacing(5);
        volSlider.setPaintTicks(true);
        volSlider.setPaintLabels(true);

        if (volumeControl != null) {
            float currentVol = (float) Math.pow(10f, volumeControl.getValue() / 20f);
            volSlider.setValue((int) (currentVol * 100));
        }

        volSlider.addChangeListener(e -> setVolume(volSlider.getValue() / 100f));

        contentPanel.add(volSlider);
        contentPanel.add(Box.createVerticalStrut(30));

        JButton okBtn = new JButton("Kembali");
        okBtn.setFont(new Font("Arial", Font.BOLD, 16));
        okBtn.setBackground(new Color(0x4CAF50));
        okBtn.setForeground(Color.WHITE);
        okBtn.setFocusPainted(false);
        okBtn.setBorderPainted(false);
        okBtn.setPreferredSize(new Dimension(120, 40));
        okBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        okBtn.addActionListener(e -> dialog.dispose());

        contentPanel.add(okBtn);

        dialog.add(contentPanel);
        dialog.setSize(400, 250);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void updateTurnIndicator() {
        Player currentPlayer = players.get(currentPlayerIndex);
        turnLabel.setText(currentPlayer.getName());
        turnLabel.setForeground(currentPlayer.getColor());

        // Update position and points
        Component[] components = ((JPanel)turnLabel.getParent()).getComponents();
        for (Component c : components) {
            if (c.getName() != null && c.getName().equals("posPointLabel")) {
                ((JLabel)c).setText("Posisi: " + currentPlayer.getPosition() + " | Poin: " + currentPlayer.getBonusPoints());
                break;
            }
        }
    }

    private void updatePlayerStatus() {
        // Update player list
        playerListPanel.removeAll();
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
            pnl.setOpaque(false);

            String bullet = (i == currentPlayerIndex) ? "● " : "○ ";
            JLabel lbl = new JLabel(bullet + p.getName());
            lbl.setForeground(p.getColor());
            lbl.setFont(new Font("Arial", Font.PLAIN, 12));

            JLabel posLabel = new JLabel("Posisi: " + p.getPosition() + " | Poin: " + p.getBonusPoints());
            posLabel.setForeground(Color.LIGHT_GRAY);
            posLabel.setFont(new Font("Arial", Font.PLAIN, 10));

            JPanel vPanel = new JPanel();
            vPanel.setLayout(new BoxLayout(vPanel, BoxLayout.Y_AXIS));
            vPanel.setOpaque(false);
            vPanel.add(lbl);
            vPanel.add(posLabel);

            pnl.add(vPanel);
            playerListPanel.add(pnl);
        }
        playerListPanel.revalidate();
        playerListPanel.repaint();

        // Update leaderboard
        updateLeaderboard();

        // Update turn indicator
        updateTurnIndicator();
    }

    private void updateLeaderboard() {
        leaderboardPanel.removeAll();

        PriorityQueue<Player> pq = new PriorityQueue<>(players);
        List<Player> sortedPlayers = new ArrayList<>();
        while (!pq.isEmpty()) {
            sortedPlayers.add(pq.poll());
        }

        for (int i = 0; i < sortedPlayers.size(); i++) {
            Player p = sortedPlayers.get(i);
            JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
            pnl.setOpaque(false);

            String medal = (i == 0) ? "🥇 " : (i == 1) ? "🥈 " : (i == 2) ? "🥉 " : (i + 1) + ". ";
            JLabel lbl = new JLabel(medal + p.getName() + " - " + p.getBonusPoints() + " poin");
            lbl.setForeground((i == 0) ? Color.YELLOW : Color.WHITE);
            lbl.setFont(new Font("Arial", Font.PLAIN, 12));

            pnl.add(lbl);
            leaderboardPanel.add(pnl);
        }

        leaderboardPanel.revalidate();
        leaderboardPanel.repaint();
    }

    private void setupLinks() {
        links.put(2, 13);
        links.put(5, 19);
        links.put(11, 29);
        links.put(17, 36);
        links.put(22, 41);
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
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createBoardPanel() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        boardPanel = new JPanel(new GridLayout(UKURAN_PAPAN, UKURAN_PAPAN, 2, 2));
        boardPanel.setBackground(Color.BLACK);
        boardPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 5));
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
        for (int i = 3; i * i <= n; i += 2) if (n % i == 0) return false;
        return true;
    }

    private void showWinnerDialog(PriorityQueue<Player> leaderboard) {
        JDialog dialog = new JDialog(this, "🎉 Permainan Selesai!", true);
        dialog.setLayout(new BorderLayout());

        JPanel contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(0x1A237E),
                        0, getHeight(), new Color(0x4A148C)
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        JLabel titleLabel = new JLabel("🏆 HASIL AKHIR 🏆");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(Color.YELLOW);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(20));

        List<Player> finalRanking = new ArrayList<>();
        while (!leaderboard.isEmpty()) {
            finalRanking.add(leaderboard.poll());
        }

        for (int i = 0; i < finalRanking.size(); i++) {
            Player p = finalRanking.get(i);
            String medal = (i == 0) ? "🥇" : (i == 1) ? "🥈" : (i == 2) ? "🥉" : String.valueOf(i + 1);

            JLabel rankLabel = new JLabel(medal + " " + p.getName() + " - " + p.getBonusPoints() + " poin");
            rankLabel.setFont(new Font("Arial", Font.BOLD, 18));
            rankLabel.setForeground((i == 0) ? Color.YELLOW : Color.WHITE);
            rankLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            contentPanel.add(rankLabel);
            contentPanel.add(Box.createVerticalStrut(10));
        }

        contentPanel.add(Box.createVerticalStrut(20));

        JButton closeBtn = new JButton("Kembali ke Lobby");
        closeBtn.setFont(new Font("Arial", Font.BOLD, 16));
        closeBtn.setBackground(new Color(0xFFC107));
        closeBtn.setForeground(new Color(0x4A2C00));
        closeBtn.setFocusPainted(false);
        closeBtn.setPreferredSize(new Dimension(200, 45));
        closeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeBtn.addActionListener(e -> {
            dialog.dispose();
            this.dispose();
            new LobbyFrame(new PapanUlarTanggaGUI());
        });

        contentPanel.add(closeBtn);

        dialog.add(contentPanel);
        dialog.setSize(400, 450);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private class SquarePanel extends JPanel {
        private final int number;
        private final List<Player> playersHere = new ArrayList<>();

        public SquarePanel(int n) {
            this.number = n;
            setPreferredSize(new Dimension(UKURAN_KOTAK, UKURAN_KOTAK));
            setBackground(isPrime(n) ? PRIME_COLOR : (n % 2 == 0 ? BOARD_LIGHT : BOARD_DARK));
            setBorder(BorderFactory.createLineBorder(BOARD_BORDER, 1));
            setLayout(null);
        }

        public void addPlayer(Player p) {
            playersHere.add(p);
            repaint();
        }

        public void clearPlayers() {
            playersHere.clear();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            // Draw number
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString(String.valueOf(number), 5, 15);

            // Draw bonus points
            int bonus = nodeBonusPoints.getOrDefault(number, 0);
            g2d.setFont(new Font("Arial", Font.PLAIN, 9));
            g2d.setColor(new Color(0, 100, 0));
            g2d.drawString("+" + bonus, 5, 28);

            // Draw ladder indicator
            if (links.containsKey(number)) {
                g2d.setColor(Color.GREEN);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawLine(0, getHeight(), getWidth(), 0);
            }

            // Draw players
            int i = 0;
            for (Player p : playersHere) {
                g2d.setColor(p.getColor());
                g2d.fillOval(5 + (i * 20), 35, 15, 15);
                i++;
            }
        }
    }

    private static class DicePanel extends JPanel {
        private int val;
        public DicePanel(int v) {
            this.val = v;
            setPreferredSize(new Dimension(100, 100));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        }
        public void setDiceValue(int v) { this.val = v; repaint(); }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(val < 0 ? Color.RED : Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 40));
            String txt = (val == 0) ? "?" : String.valueOf(Math.abs(val));
            g2d.drawString(txt, 40, 60);
            if (val < 0) { g2d.setFont(new Font("Arial", Font.PLAIN, 12)); g2d.drawString("Mundur", 10, 90); }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PapanUlarTanggaGUI game = new PapanUlarTanggaGUI();
            new LobbyFrame(game);
        });
    }
}