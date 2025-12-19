import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.awt.geom.QuadCurve2D;
import javax.sound.sampled.*;
import java.io.File;

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
    private JTextPane historyPane;
    private Map<Integer, SquarePanel> boardSquaresMap;
    private JButton lemparDaduButton;
    private JButton exitButton;
    private DicePanel diceDisplay;
    private JLabel statusLabel;
    private JPanel boardPanel;

    private Image mainBgImage;
    private Image controlBgImage;
    private Clip backgroundMusic;
    private FloatControl volumeControl;

    private final Map<Integer, Integer> links = new HashMap<>();
    private final int[][] papanData = new int[UKURAN_PAPAN][UKURAN_PAPAN];
    private int currentPlayerIndex = 0;
    private Random random = new Random();

    public PapanUlarTanggaGUI() {
        setupLinks();
        initializeBoardData();
        setupHistoryPane();

        try {
            mainBgImage = new ImageIcon("images/BOS_1.png").getImage();
            controlBgImage = new ImageIcon("images/BOS_4.png").getImage();
        } catch (Exception e) {
            System.out.println("Gagal memuat gambar.");
        }

        // Memutar musik latar utama saat startup
        playBackgroundMusic("sound/BOS_2.wav");
    }

    // --- SISTEM AUDIO ---
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

    // Method untuk SFX (Suara Pendek Sekali Putar)
    private void playSoundEffect(String filePath) {
        // Jalankan dalam thread terpisah agar tidak membeku (freeze) dan bisa overlap
        new Thread(() -> {
            try {
                File soundPath = new File(filePath);
                if (soundPath.exists()) {
                    AudioInputStream audioInput = AudioSystem.getAudioInputStream(soundPath);
                    Clip sfx = AudioSystem.getClip();
                    sfx.open(audioInput);
                    sfx.start();

                    // Pastikan resource ditutup setelah selesai diputar agar tidak memori bocor
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

    // --- LOGIKA PERMAINAN ---
    private void handleDiceRoll(ActionEvent e) {
        if (players.isEmpty()) return;

        // SFX Klik Tombol

        lemparDaduButton.setEnabled(false);
        int diceRes = random.nextInt(6) + 1;
        diceDisplay.setDiceValue(diceRes);

        Player p = players.get(currentPlayerIndex);
        int nextPos = p.getPosition() + diceRes;

        if (nextPos <= 64) {
            // SFX Bidak Jalan
            playSoundEffect("sound/bos_3.wav");
            p.setPosition(nextPos);
        }

        updateBoardUI();
        updateHistory();
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        lemparDaduButton.setEnabled(true);
    }

    // --- ANTARMUKA ---
    private JPanel createControlPanel() {
        JPanel cp = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (controlBgImage != null) g.drawImage(controlBgImage, 0, 0, getWidth(), getHeight(), this);
            }
        };
        cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));
        cp.setPreferredSize(new Dimension(320, 0));
        cp.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        cp.setOpaque(false);

        // Tombol Setting
        JButton btnSetting = new JButton("⚙️ PENGATURAN");
        btnSetting.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnSetting.addActionListener(e -> {
            showSettingDialog();
        });
        cp.add(btnSetting);
        cp.add(Box.createVerticalStrut(20));

        statusLabel = new JLabel("Giliran: -");
        statusLabel.setForeground(Color.YELLOW);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cp.add(statusLabel);
        cp.add(Box.createVerticalStrut(20));

        diceDisplay = new DicePanel(0);
        diceDisplay.setAlignmentX(Component.CENTER_ALIGNMENT);
        diceDisplay.setOpaque(false);
        cp.add(diceDisplay);
        cp.add(Box.createVerticalStrut(20));

        lemparDaduButton = new JButton("🎲 LEMPAR DADU");
        lemparDaduButton.setMaximumSize(new Dimension(200, 45));
        lemparDaduButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        lemparDaduButton.addActionListener(this::handleDiceRoll);
        cp.add(lemparDaduButton);

        exitButton = new JButton("🚪 KEMBALI KE LOBBY");
        exitButton.setMaximumSize(new Dimension(200, 40));
        exitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitButton.addActionListener(e -> {
            this.dispose();
        });
        cp.add(Box.createVerticalStrut(15));
        cp.add(exitButton);

        return cp;
    }

    private void showSettingDialog() {
        JDialog dialog = new JDialog(this, "Pengaturan Volume", true);
        dialog.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 20));
        dialog.setSize(300, 150);
        dialog.setLocationRelativeTo(this);

        JSlider volSlider = new JSlider(0, 100);
        if (volumeControl != null) {
            float currentVol = (float) Math.pow(10f, volumeControl.getValue() / 20f);
            volSlider.setValue((int) (currentVol * 100));
        }

        volSlider.addChangeListener(e -> setVolume(volSlider.getValue() / 100f));

        JButton okBtn = new JButton("Tutup");
        okBtn.addActionListener(e -> {
            dialog.dispose();
        });

        dialog.add(new JLabel("Volume Musik:"));
        dialog.add(volSlider);
        dialog.add(okBtn);
        dialog.setVisible(true);
    }

    private void setupLinks() {
        links.put(2, 13); links.put(3, 17); links.put(5, 19);
        links.put(7, 23); links.put(11, 29); links.put(13, 31);
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

    private void setupHistoryPane() {
        historyPane = new JTextPane();
        historyPane.setEditable(false);
    }

    public void start(List<Player> initialPlayers) {
        this.players = initialPlayers;
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
        updateHistory();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createBoardPanel() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        boardPanel = new JPanel(new GridLayout(UKURAN_PAPAN, UKURAN_PAPAN));
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

    private void updateHistory() {
        if (!players.isEmpty()) {
            statusLabel.setText("Giliran: " + players.get(currentPlayerIndex).getName());
        }
    }

    private class SquarePanel extends JPanel {
        private final List<Player> playersHere = new ArrayList<>();
        public SquarePanel(int n) {
            setPreferredSize(new Dimension(UKURAN_KOTAK, UKURAN_KOTAK));
            setBackground(n % 2 == 0 ? BOARD_LIGHT : BOARD_DARK);
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
                x += 22;
            }
        }
    }

    private static class DicePanel extends JPanel {
        private int val;
        public DicePanel(int v) { this.val = v; setPreferredSize(new Dimension(80, 80)); }
        public void setDiceValue(int v) { this.val = v; repaint(); }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setFont(new Font("Arial", Font.BOLD, 14));
            g.drawString("Dadu: " + val, 15, 45);
        }
    }
    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            PapanUlarTanggaGUI game = new PapanUlarTanggaGUI();

            new LobbyFrame(game);

        });

    }

}