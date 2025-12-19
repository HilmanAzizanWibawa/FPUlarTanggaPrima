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
    private static final Color PRIME_COLOR = new Color(0xFFD700);
    private static final Color BUTTON_YELLOW = new Color(0xFFC700);
    private static final Color BUTTON_BORDER = new Color(0xFF8C00);

    private List<Player> players;
    private Map<Integer, SquarePanel> boardSquaresMap;
    private JButton lemparDaduButton;
    private DicePanel diceDisplay;
    private JLabel turnLabel;
    private JPanel boardPanel;
    private JPanel playerListPanel;
    private JPanel leaderboardPanel;

    private Image mainBgImage;
    private Clip backgroundMusic;
    public FloatControl volumeControl;

    // Array untuk menyimpan imej bidak 1-6
    private Image[] playerIcons = new Image[6];

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
        loadPlayerIcons(); // Memuatkan imej bidak

        try {
            mainBgImage = new ImageIcon("images/BOS_1.png").getImage();
        } catch (Exception e) {
            System.out.println("Gagal memuat gambar background.");
        }
        playBackgroundMusic("sound/BOS_2.wav");
    }

    // Memuatkan imej bidak 1.png hingga 6.png dari folder images
    private void loadPlayerIcons() {
        for (int i = 0; i < 6; i++) {
            try {
                String path = "images/" + (i + 1) + ".png";
                playerIcons[i] = new ImageIcon(path).getImage();
            } catch (Exception e) {
                System.err.println("Gagal memuat bidak: " + (i + 1) + ".png");
            }
        }
    }

    private void stopBackgroundMusic() {
        if (backgroundMusic != null) {
            backgroundMusic.stop();
            backgroundMusic.close();
            backgroundMusic = null;
        }
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
        for (int i = 1; i <= 64; i++) {
            nodeBonusPoints.put(i, 10 + random.nextInt(41));
        }
        nodeBonusPoints.put(64, 100);
        for (int key : links.keySet()) {
            nodeBonusPoints.put(links.get(key), 50 + random.nextInt(51));
        }
    }

    private void handleDiceRoll(ActionEvent e) {
        if (players.isEmpty() || gameEnded) return;
        lemparDaduButton.setEnabled(false);
        Player p = players.get(currentPlayerIndex);

        int diceValue;
        boolean isRedDice = false;

        if (random.nextInt(100) < 20) {
            isRedDice = true;
            diceValue = random.nextInt(2) + 1;
        } else {
            diceValue = random.nextInt(6) + 1;
        }

        diceDisplay.setDiceValue(diceValue, isRedDice);
        int effectiveSteps = isRedDice ? -diceValue : diceValue;
        moveAnimated(p, effectiveSteps);
    }

    private void moveAnimated(Player p, int steps) {
        Timer moveTimer = new Timer(400, null);
        moveTimer.addActionListener(new ActionListener() {
            private int remainingSteps = Math.abs(steps);
            private boolean isMovingForward = steps > 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (remainingSteps <= 0) {
                    moveTimer.stop();
                    p.addBonusPoints(nodeBonusPoints.getOrDefault(p.getPosition(), 0));
                    updateBoardUI();
                    updatePlayerStatus();
                    checkWinner(p);
                    finishTurn();
                    return;
                }

                playSoundEffect("sound/BOS_1.wav");

                if (isMovingForward) {
                    if (p.getPosition() < 64) {
                        p.setPosition(p.getPosition() + 1);
                    } else {
                        isMovingForward = false;
                        p.setPosition(p.getPosition() - 1);
                    }
                } else {
                    if (p.getPosition() > 1) {
                        p.setPosition(p.getPosition() - 1);
                    }
                }

                remainingSteps--;
                int currentPos = p.getPosition();

                if (links.containsKey(currentPos)) {
                    moveTimer.stop();
                    int targetWarp = links.get(currentPos);
                    int sisa = remainingSteps;
                    boolean direction = isMovingForward;

                    Timer warpEffect = new Timer(500, evt -> {
                        p.setPosition(targetWarp);
                        playSoundEffect("sound/BOS_4.wav");
                        updateBoardUI();
                        updatePlayerStatus();
                        continueMovement(p, sisa, direction);
                    });
                    warpEffect.setRepeats(false);
                    warpEffect.start();
                    return;
                }

                updateBoardUI();
                updatePlayerStatus();
            }
        });
        moveTimer.start();
    }

    private void continueMovement(Player p, int steps, boolean forward) {
        if (steps <= 0) {
            p.addBonusPoints(nodeBonusPoints.getOrDefault(p.getPosition(), 0));
            checkWinner(p);
            finishTurn();
            return;
        }
        int signedSteps = forward ? steps : -steps;
        moveAnimated(p, signedSteps);
    }

    private void finishTurn() {
        if (!gameEnded) {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            updateTurnIndicator();
            lemparDaduButton.setEnabled(true);
        }
    }

    private void checkWinner(Player p) {
        if (p.getPosition() == 64 && !gameEnded) {
            gameEnded = true;
            playSoundEffect("sound/BOS_3.wav");
            PriorityQueue<Player> leaderboard = new PriorityQueue<>();
            leaderboard.addAll(players);
            Timer winTimer = new Timer(500, evt -> showWinnerDialog(leaderboard, p));
            winTimer.setRepeats(false);
            winTimer.start();
        }
    }

    private JButton createStyledButton(String text, int fontSize) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gradient = new GradientPaint(0, 0, BUTTON_YELLOW, 0, getHeight(), BUTTON_YELLOW.darker());
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2d.setColor(BUTTON_BORDER);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 20, 20);
                g2d.setColor(new Color(0x4A2C00));
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2d.drawString(getText(), x, y);
            }
        };
        button.setFont(new Font("Arial Black", Font.BOLD, fontSize));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JPanel createControlPanel() {
        JPanel cp = new JPanel();
        cp.setLayout(new BorderLayout());
        cp.setPreferredSize(new Dimension(280, 0));
        cp.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 4));

        JPanel innerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gradient = new GradientPaint(0, 0, new Color(0x004D40), 0, getHeight(), new Color(0x00695C));
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
        innerPanel.setOpaque(false);
        innerPanel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));

        Dimension boxSize = new Dimension(240, 180);

        JPanel turnPanel = new JPanel();
        turnPanel.setLayout(new BoxLayout(turnPanel, BoxLayout.Y_AXIS));
        turnPanel.setOpaque(false);
        turnPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.YELLOW, 2), BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        turnPanel.setMaximumSize(new Dimension(240, 100));
        turnPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        turnLabel = new JLabel("Pemain 1");
        turnLabel.setFont(new Font("Arial", Font.BOLD, 18));
        turnLabel.setForeground(Color.WHITE);
        turnLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel turnTitle = new JLabel("🎯 TURN:");
        turnTitle.setForeground(new Color(0xFFC107));
        turnTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel posPointLabel = new JLabel("Posisi: 1 | Poin: 0");
        posPointLabel.setForeground(Color.YELLOW);
        posPointLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        posPointLabel.setName("posPointLabel");
        turnPanel.add(turnTitle);
        turnPanel.add(Box.createVerticalStrut(5));
        turnPanel.add(turnLabel);
        turnPanel.add(posPointLabel);
        innerPanel.add(turnPanel);
        innerPanel.add(Box.createVerticalStrut(15));

        JPanel playerSection = new JPanel();
        playerSection.setLayout(new BoxLayout(playerSection, BoxLayout.Y_AXIS));
        playerSection.setOpaque(false);
        playerSection.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.YELLOW, 2), BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        playerSection.setMaximumSize(boxSize);
        playerSection.setAlignmentX(Component.CENTER_ALIGNMENT);
        playerListPanel = new JPanel();
        playerListPanel.setLayout(new BoxLayout(playerListPanel, BoxLayout.Y_AXIS));
        playerListPanel.setOpaque(false);
        JLabel pTitle = new JLabel("👥 PEMAIN"); pTitle.setForeground(Color.WHITE); pTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        playerSection.add(pTitle); playerSection.add(playerListPanel);
        innerPanel.add(playerSection);
        innerPanel.add(Box.createVerticalStrut(15));

        JPanel leaderSection = new JPanel();
        leaderSection.setLayout(new BoxLayout(leaderSection, BoxLayout.Y_AXIS));
        leaderSection.setOpaque(false);
        leaderSection.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.YELLOW, 2), BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        leaderSection.setMaximumSize(boxSize);
        leaderSection.setAlignmentX(Component.CENTER_ALIGNMENT);
        leaderboardPanel = new JPanel();
        leaderboardPanel.setLayout(new BoxLayout(leaderboardPanel, BoxLayout.Y_AXIS));
        leaderboardPanel.setOpaque(false);
        JLabel lTitle = new JLabel("🏆 LEADERBOARD"); lTitle.setForeground(new Color(0xFFC107)); lTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        leaderSection.add(lTitle); leaderSection.add(leaderboardPanel);
        innerPanel.add(leaderSection);
        innerPanel.add(Box.createVerticalStrut(15));

        diceDisplay = new DicePanel(0);
        diceDisplay.setAlignmentX(Component.CENTER_ALIGNMENT);
        innerPanel.add(diceDisplay);
        innerPanel.add(Box.createVerticalStrut(15));

        JPanel buttonSection = new JPanel();
        buttonSection.setLayout(new BoxLayout(buttonSection, BoxLayout.Y_AXIS));
        buttonSection.setOpaque(false);
        buttonSection.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.YELLOW, 2), BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        buttonSection.setMaximumSize(new Dimension(240, 180));
        buttonSection.setAlignmentX(Component.CENTER_ALIGNMENT);

        lemparDaduButton = createStyledButton("⚄ LEMPAR", 14);
        lemparDaduButton.setMaximumSize(new Dimension(200, 40));
        lemparDaduButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        lemparDaduButton.addActionListener(this::handleDiceRoll);

        JButton settingsBtn = createStyledButton("⚙️ SETTING", 14);
        settingsBtn.setMaximumSize(new Dimension(200, 40));
        settingsBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        settingsBtn.addActionListener(e -> showInGameSettings());

        JButton lobbyBtn = createStyledButton("🔙 LOBBY", 14);
        lobbyBtn.setMaximumSize(new Dimension(200, 40));
        lobbyBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        lobbyBtn.addActionListener(e -> {
            stopBackgroundMusic();
            this.dispose();
            SwingUtilities.invokeLater(() -> new LobbyFrame(new PapanUlarTanggaGUI()));
        });

        buttonSection.add(lemparDaduButton); buttonSection.add(Box.createVerticalStrut(10));
        buttonSection.add(settingsBtn); buttonSection.add(Box.createVerticalStrut(10));
        buttonSection.add(lobbyBtn);
        innerPanel.add(buttonSection);

        JScrollPane scrollPane = new JScrollPane(innerPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        cp.add(scrollPane, BorderLayout.CENTER);
        return cp;
    }

    private void showWinnerDialog(PriorityQueue<Player> leaderboard, Player winner) {
        JDialog d = new JDialog(this, "🏆 Hasil Akhir 🏆", true);
        d.setLayout(new BorderLayout());
        JPanel cp = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0, 0, new Color(0x1A237E), 0, getHeight(), new Color(0x4A148C));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));
        cp.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));
        List<Player> rank = new ArrayList<>();
        while (!leaderboard.isEmpty()) rank.add(leaderboard.poll());

        JLabel title = new JLabel("HASIL AKHIR");
        title.setFont(new Font("Arial Black", Font.BOLD, 26));
        title.setForeground(Color.YELLOW); title.setAlignmentX(Component.CENTER_ALIGNMENT);
        cp.add(title); cp.add(Box.createVerticalStrut(20));

        JLabel winnerLabel = new JLabel("🎉 PEMENANG: " + winner.getName() + " 🎉");
        winnerLabel.setFont(new Font("Arial", Font.BOLD, 20));
        winnerLabel.setForeground(Color.GREEN); winnerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cp.add(winnerLabel); cp.add(Box.createVerticalStrut(20));

        for (int i = 0; i < rank.size(); i++) {
            Player p = rank.get(i);
            String prefix = (i == 0) ? "🥇 " : (i == 1) ? "🥈 " : (i == 2) ? "🥉 " : (i + 1) + ". ";
            JLabel lbl = new JLabel(prefix + p.getName() + " - " + p.getBonusPoints() + " Poin");
            lbl.setFont(new Font("Arial", Font.PLAIN, 18));
            lbl.setForeground(Color.WHITE); lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            cp.add(lbl); cp.add(Box.createVerticalStrut(10));
        }
        JButton btn = createStyledButton("KEMBALI KE LOBBY", 14);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.addActionListener(e -> { stopBackgroundMusic(); d.dispose(); this.dispose(); new LobbyFrame(new PapanUlarTanggaGUI()); });
        cp.add(Box.createVerticalStrut(20)); cp.add(btn);
        d.add(cp); d.pack(); d.setLocationRelativeTo(this); d.setVisible(true);
    }

    private void showInGameSettings() {
        JDialog dialog = new JDialog(this, "⚙️ Pengaturan Volume", true);
        dialog.setLayout(new BorderLayout());
        JPanel cp = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0, 0, new Color(0x667eea), 0, getHeight(), new Color(0x764ba2));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));
        cp.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        JLabel title = new JLabel("🔊 Volume Musik"); title.setForeground(Color.WHITE); title.setAlignmentX(Component.CENTER_ALIGNMENT);
        cp.add(title); cp.add(Box.createVerticalStrut(20));
        JSlider vol = new JSlider(0, 100); vol.setOpaque(false);
        if (volumeControl != null) {
            float current = (float) Math.pow(10f, volumeControl.getValue() / 20f);
            vol.setValue((int) (current * 100));
        }
        vol.addChangeListener(e -> setVolume(vol.getValue() / 100f));
        cp.add(vol);
        JButton ok = new JButton("Tutup"); ok.addActionListener(e -> dialog.dispose());
        cp.add(Box.createVerticalStrut(20)); cp.add(ok);
        dialog.add(cp); dialog.setSize(350, 250); dialog.setLocationRelativeTo(this); dialog.setVisible(true);
    }

    private void updateTurnIndicator() {
        Player currentPlayer = players.get(currentPlayerIndex);
        turnLabel.setText(currentPlayer.getName());
        turnLabel.setForeground(currentPlayer.getColor());
        Component[] components = ((JPanel)turnLabel.getParent()).getComponents();
        for (Component c : components) {
            if (c.getName() != null && c.getName().equals("posPointLabel")) {
                ((JLabel)c).setText("Posisi: " + currentPlayer.getPosition() + " | Poin: " + currentPlayer.getBonusPoints());
                break;
            }
        }
    }

    private void updatePlayerStatus() {
        playerListPanel.removeAll();
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            JPanel pnl = new JPanel(new FlowLayout(FlowLayout.CENTER));
            pnl.setOpaque(false);
            JLabel lbl = new JLabel(((i == currentPlayerIndex) ? "● " : "○ ") + p.getName() + " (Pos: " + p.getPosition() + ")");
            lbl.setForeground(p.getColor());
            pnl.add(lbl); playerListPanel.add(pnl);
        }
        playerListPanel.revalidate(); playerListPanel.repaint();
        updateLeaderboard(); updateTurnIndicator();
    }

    private void updateLeaderboard() {
        leaderboardPanel.removeAll();
        PriorityQueue<Player> pq = new PriorityQueue<>(players);
        List<Player> sorted = new ArrayList<>();
        while (!pq.isEmpty()) sorted.add(pq.poll());
        for (int i = 0; i < sorted.size(); i++) {
            JPanel pnl = new JPanel(new FlowLayout(FlowLayout.CENTER));
            pnl.setOpaque(false);
            JLabel lbl = new JLabel((i + 1) + ". " + sorted.get(i).getName() + " - " + sorted.get(i).getBonusPoints() + " pts");
            lbl.setForeground((i == 0) ? Color.YELLOW : Color.WHITE);
            pnl.add(lbl);
            leaderboardPanel.add(pnl);
        }
        leaderboardPanel.revalidate(); leaderboardPanel.repaint();
    }

    private void setupLinks() {
        links.put(2, 13); links.put(5, 19); links.put(11, 29);
        links.put(17, 36); links.put(22, 41); links.put(34, 52); links.put(45, 60);
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
        JPanel mainWrapper = new JPanel(new BorderLayout());
        mainWrapper.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 4));
        JPanel main = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (mainBgImage != null) g.drawImage(mainBgImage, 0, 0, getWidth(), getHeight(), this);
            }
        };
        mainWrapper.add(main);
        setContentPane(mainWrapper);
        main.add(createBoardPanel(), BorderLayout.CENTER);
        main.add(createControlPanel(), BorderLayout.EAST);
        updateBoardUI();
        updatePlayerStatus();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createBoardPanel() {
        JPanel wrapper = new JPanel(new GridBagLayout()); wrapper.setOpaque(false);
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

    private class SquarePanel extends JPanel {
        private final int number;
        private final List<Player> playersHere = new ArrayList<>();
        public SquarePanel(int n) {
            this.number = n;
            setPreferredSize(new Dimension(UKURAN_KOTAK, UKURAN_KOTAK));
            setBackground(PapanUlarTanggaGUI.this.isPrime(n) ? PRIME_COLOR : (n % 2 == 0 ? BOARD_LIGHT : BOARD_DARK));
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
            if (links.containsKey(number)) {
                g2d.setColor(new Color(0, 255, 0, 60)); g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setColor(new Color(0, 100, 0));
                g2d.drawString("X -> " + links.get(number), 5, 45);
            }
            g2d.setColor(Color.BLACK); g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString(String.valueOf(number), 5, 15);
            int b = nodeBonusPoints.getOrDefault(number, 0);
            g2d.setFont(new Font("Arial", Font.PLAIN, 10));
            g2d.setColor(new Color(0, 120, 0));
            g2d.drawString("+" + b + "pts", 5, 28);

            // Menggambar imej bidak bagi setiap pemain di petak ini
            for (int i = 0; i < playersHere.size(); i++) {
                Player p = playersHere.get(i);
                int pIndex = players.indexOf(p);
                // Mendapatkan imej bidak 1-6 (berdasarkan turutan pemain)
                if (pIndex >= 0 && pIndex < 6 && playerIcons[pIndex] != null) {
                    // Saiz bidak (contoh: 25x25)
                    g2d.drawImage(playerIcons[pIndex], 5 + (i * 18), 45, 25, 25, this);
                } else {
                    // Fallback jika imej tiada
                    g2d.setColor(p.getColor());
                    g2d.fillOval(5 + (i * 18), 50, 15, 15);
                }
            }
        }
    }

    private static class DicePanel extends JPanel {
        private int val;
        private boolean isRed;
        public DicePanel(int v) {
            this.val = v;
            setPreferredSize(new Dimension(80, 80));
            setMaximumSize(new Dimension(80, 80));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        }
        public void setDiceValue(int v, boolean red) { this.val = v; this.isRed = red; repaint(); }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (val == 0) {
                g2d.setColor(Color.LIGHT_GRAY); g2d.setFont(new Font("Arial", Font.BOLD, 40));
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString("?", (80 - fm.stringWidth("?"))/2, 55);
                return;
            }
            g2d.setColor(isRed ? Color.RED : Color.BLACK);
            int d = 14, m = 12, mid = 40 - (d / 2), L = m, R = 80 - m - d, T = m, B = 80 - m - d;
            if (val == 1 || val == 3 || val == 5) g2d.fillOval(mid, mid, d, d);
            if (val >= 2) { g2d.fillOval(R, T, d, d); g2d.fillOval(L, B, d, d); }
            if (val >= 4) { g2d.fillOval(L, T, d, d); g2d.fillOval(R, B, d, d); }
            if (val == 6) { g2d.fillOval(L, mid, d, d); g2d.fillOval(R, mid, d, d); }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PapanUlarTanggaGUI game = new PapanUlarTanggaGUI();
            new LobbyFrame(game);
        });
    }
}