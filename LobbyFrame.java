import javax.swing.*;
import java.awt.*;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LobbyFrame extends JFrame {
    private final PapanUlarTanggaGUI gameFrame;
    private int numPlayers = 2;
    private final Color[] playerColors;
    private BufferedImage backgroundImage;

    private static final Color BUTTON_YELLOW = new Color(0xFFC700);
    private static final Color BUTTON_BORDER = new Color(0xFF8C00);
    private static final Color TEXT_WHITE = Color.WHITE;
    private static final Color CARD_BG = new Color(0x1565C0);

    public LobbyFrame(PapanUlarTanggaGUI gameFrame) {
        this.gameFrame = gameFrame;
        this.playerColors = PapanUlarTanggaGUI.PLAYER_COLORS;

        try {
            backgroundImage = ImageIO.read(new File("images/BOS_3.png"));
        } catch (IOException e) {
            System.err.println("Error: File BOS_3.png tidak ditemukan di folder project!");
        }

        setTitle("🎲 ULAR TANGGA PRIMA");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                } else {
                    g.setColor(new Color(0x1E4080));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        mainPanel.setOpaque(false);
        setContentPane(mainPanel);

        mainPanel.add(createHeaderPanel(), BorderLayout.NORTH);
        mainPanel.add(createPlayControlPanel(), BorderLayout.CENTER);

        setSize(700, 650);
        setLocationRelativeTo(null);
        setResizable(false);
        setVisible(true);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(40, 20, 10, 20));

        JLabel title = new JLabel("ULAR TANGGA PRIMA", SwingConstants.CENTER);
        title.setFont(new Font("Arial Black", Font.BOLD, 40));
        title.setForeground(TEXT_WHITE);

        title.setUI(new javax.swing.plaf.basic.BasicLabelUI() {
            @Override
            protected void paintEnabledText(JLabel l, Graphics g, String s, int x, int y) {
                g.setColor(Color.BLACK);
                g.drawString(s, x + 2, y + 2);
                g.setColor(l.getForeground());
                g.drawString(s, x, y);
            }
        });

        header.add(title);
        return header;
    }

    private JPanel createPlayControlPanel() {
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setOpaque(false);

        JButton playButton = createStyledButton("PLAY", 50);
        playButton.setMaximumSize(new Dimension(250, 100));
        playButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        playButton.addActionListener(e -> showPlayerSelectionDialog());

        // Tombol Pengaturan
        JButton settingsButton = createStyledButton("⚙️ PENGATURAN", 30);
        settingsButton.setMaximumSize(new Dimension(250, 70));
        settingsButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        settingsButton.addActionListener(e -> showSettingDialog());

        controlPanel.add(Box.createVerticalGlue());
        controlPanel.add(playButton);
        controlPanel.add(Box.createVerticalStrut(20));
        controlPanel.add(settingsButton);
        controlPanel.add(Box.createVerticalGlue());

        return controlPanel;
    }

    private JButton createStyledButton(String text, int fontSize) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2d.setColor(new Color(0, 0, 0, 100));
                g2d.fillRoundRect(5, 5, getWidth() - 10, getHeight() - 10, 30, 30);

                GradientPaint gradient = new GradientPaint(0, 0, BUTTON_YELLOW, 0, getHeight(), BUTTON_YELLOW.darker());
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, getWidth() - 10, getHeight() - 10, 30, 30);

                g2d.setColor(BUTTON_BORDER);
                g2d.setStroke(new BasicStroke(4));
                g2d.drawRoundRect(0, 0, getWidth() - 10, getHeight() - 10, 30, 30);

                g2d.setColor(new Color(0x4A2C00));
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2 - 5;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2 - 5;
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

    private void showSettingDialog() {
        JDialog dialog = new JDialog(this, "⚙️ Pengaturan Volume", true);
        dialog.setLayout(new BorderLayout());

        // Panel dengan gradient background
        JPanel contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(0x667eea),
                        0, getHeight(), new Color(0x764ba2)
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        JLabel titleLabel = new JLabel("🔊 Volume Musik");
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

        if (gameFrame.volumeControl != null) {
            float currentVol = (float) Math.pow(10f, gameFrame.volumeControl.getValue() / 20f);
            volSlider.setValue((int) (currentVol * 100));
        }

        volSlider.addChangeListener(e -> gameFrame.setVolume(volSlider.getValue() / 100f));

        contentPanel.add(volSlider);
        contentPanel.add(Box.createVerticalStrut(30));

        JButton okBtn = new JButton("Tutup");
        okBtn.setFont(new Font("Arial", Font.BOLD, 16));
        okBtn.setBackground(new Color(0xFFC700));
        okBtn.setForeground(new Color(0x4A2C00));
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

    private void showPlayerSelectionDialog() {
        JDialog dialog = new JDialog(this, "Pilih Pemain", true);
        dialog.setLayout(new BorderLayout());

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(new Color(0x1E2A38));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel titleLabel = new JLabel("Jumlah Pemain");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(20));

        JPanel buttonPanel = new JPanel(new GridLayout(2, 3, 10, 10));
        buttonPanel.setOpaque(false);

        for (int i = 2; i <= 6; i++) {
            final int playerCount = i;
            JButton btn = new JButton(i + " Pemain");
            btn.setBackground(BUTTON_YELLOW);
            btn.setFont(new Font("Arial", Font.BOLD, 14));
            btn.addActionListener(e -> {
                numPlayers = playerCount;
                dialog.dispose();
                showPlayerNameInputDialog();
            });
            buttonPanel.add(btn);
        }

        contentPanel.add(buttonPanel);
        dialog.add(contentPanel);
        dialog.setSize(400, 250);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showPlayerNameInputDialog() {
        JDialog dialog = new JDialog(this, "Input Nama Pemain", true);
        dialog.setLayout(new BorderLayout());

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(new Color(0x1E2A38));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel titleLabel = new JLabel("Masukkan Nama Pemain");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(20));

        JTextField[] nameFields = new JTextField[numPlayers];
        for (int i = 0; i < numPlayers; i++) {
            JPanel playerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            playerPanel.setOpaque(false);

            JLabel playerLabel = new JLabel("Pemain " + (i + 1) + ": ");
            playerLabel.setForeground(playerColors[i]);
            playerLabel.setFont(new Font("Arial", Font.BOLD, 16));

            nameFields[i] = new JTextField(15);
            nameFields[i].setFont(new Font("Arial", Font.PLAIN, 14));
            nameFields[i].setText("Pemain " + (i + 1));

            playerPanel.add(playerLabel);
            playerPanel.add(nameFields[i]);
            contentPanel.add(playerPanel);
            contentPanel.add(Box.createVerticalStrut(10));
        }

        JButton startButton = new JButton("Mulai Permainan");
        startButton.setBackground(BUTTON_YELLOW);
        startButton.setFont(new Font("Arial", Font.BOLD, 16));
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        startButton.addActionListener(e -> {
            List<Player> players = new ArrayList<>();
            for (int i = 0; i < numPlayers; i++) {
                String name = nameFields[i].getText().trim();
                if (name.isEmpty()) name = "Pemain " + (i + 1);
                players.add(new Player(name, playerColors[i]));
            }
            dialog.dispose();
            gameFrame.start(players);
            this.dispose();
        });

        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(startButton);

        dialog.add(contentPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
}