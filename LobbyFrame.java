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

    // Warna Tema untuk Komponen
    private static final Color BUTTON_YELLOW = new Color(0xFFC700);
    private static final Color BUTTON_BORDER = new Color(0xFF8C00);
    private static final Color TEXT_WHITE = Color.WHITE;
    private static final Color CARD_BG = new Color(0x1565C0);

    public LobbyFrame(PapanUlarTanggaGUI gameFrame) {
        this.gameFrame = gameFrame;
        this.playerColors = PapanUlarTanggaGUI.PLAYER_COLORS;

        // --- PROSES MEMUAT GAMBAR BOS_2.png ---
        try {
            backgroundImage = ImageIO.read(new File("images/BOS_3.png"));
        } catch (IOException e) {
            System.err.println("Error: File BOS_3.png tidak ditemukan di folder project!");
        }

        setTitle("🎲 ULAR TANGGA PRIMA");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // --- PANEL UTAMA DENGAN BACKGROUND CUSTOM ---
        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    // Gambar memenuhi seluruh area frame
                    g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                } else {
                    // Warna cadangan jika gambar gagal dimuat
                    g.setColor(new Color(0x1E4080));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        mainPanel.setOpaque(false);
        setContentPane(mainPanel);

        // Menambahkan Komponen UI di atas background
        mainPanel.add(createHeaderPanel(), BorderLayout.NORTH);
        mainPanel.add(createPlayControlPanel(), BorderLayout.CENTER);

        setSize(700, 650); // Ukuran permintaan Anda
        setLocationRelativeTo(null);
        setResizable(false); // Disarankan agar background tidak distorsi
        setVisible(true);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel();
        header.setOpaque(false); // Transparan agar background terlihat
        header.setBorder(BorderFactory.createEmptyBorder(40, 20, 10, 20));

        JLabel title = new JLabel("ULAR TANGGA PRIMA", SwingConstants.CENTER);
        title.setFont(new Font("Arial Black", Font.BOLD, 40));
        title.setForeground(TEXT_WHITE);

        // Memberi bayangan tipis agar teks terbaca di background apapun
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
        controlPanel.setOpaque(false); // Transparan

        // Tombol PLAY
        JButton playButton = createStyledButton("PLAY", 50);
        playButton.setMaximumSize(new Dimension(250, 100));
        playButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        playButton.addActionListener(e -> showPlayerSelectionDialog());

        controlPanel.add(Box.createVerticalGlue());
        controlPanel.add(playButton);
        controlPanel.add(Box.createVerticalStrut(20));

        JLabel infoLabel = new JLabel("", SwingConstants.CENTER);
        infoLabel.setFont(new Font("Arial", Font.BOLD, 18));
        infoLabel.setForeground(TEXT_WHITE);
        infoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        controlPanel.add(infoLabel);

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

    private void showPlayerSelectionDialog() {
        JDialog dialog = new JDialog(this, "Pilih Pemain", true);
        dialog.setLayout(new BorderLayout());

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(new Color(0x1E2A38)); // Warna gelap kontras
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
            btn.addActionListener(e -> {
                numPlayers = playerCount;
                dialog.dispose();
                startGameWithPlayers();
            });
            buttonPanel.add(btn);
        }

        contentPanel.add(buttonPanel);
        dialog.add(contentPanel);
        dialog.setSize(400, 250);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void startGameWithPlayers() {
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            players.add(new Player("Pemain " + (i + 1), playerColors[i]));
        }
        gameFrame.start(players);
        this.dispose();
    }
}