import java.awt.Color;
import java.util.Objects;
import java.util.UUID;

public class Player implements Comparable<Player> {
    private final String name;
    private int position;
    private final Color color;
    private final String id;
    private int bonusPoints;

    public Player(String name, Color color) {
        this.name = name;
        this.position = 1;
        this.color = color;
        this.id = name + "-" + UUID.randomUUID();
        this.bonusPoints = 0;
    }

    public String getName() {
        return name;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int newPosition) {
        this.position = Math.max(1, Math.min(newPosition, 64));
    }

    public Color getColor() {
        return color;
    }

    public String getId() {
        return id;
    }

    public int getBonusPoints() {
        return bonusPoints;
    }

    public void addBonusPoints(int points) {
        this.bonusPoints += points;
    }

    @Override
    public int compareTo(Player other) {
        // Higher points first (descending order)
        return Integer.compare(other.bonusPoints, this.bonusPoints);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Player player = (Player) obj;
        return Objects.equals(id, player.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}