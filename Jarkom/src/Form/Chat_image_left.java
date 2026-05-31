package Form;

import java.awt.Image;
import java.time.LocalDateTime;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Chat_image_left extends JPanel {

    public Chat_image_left(String sender, byte[] imageData, LocalDateTime timestamp) {

        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel bubble = new JPanel();
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBackground(java.awt.Color.WHITE);

        bubble.setBorder(
            javax.swing.BorderFactory.createEmptyBorder(
                5, 5, 5, 5
            )
        );

        JLabel senderLabel = new JLabel(sender);
        bubble.add(senderLabel);

        ImageIcon icon = new ImageIcon(imageData);

        Image resized = icon.getImage().getScaledInstance(
                350,
                -1,
                Image.SCALE_SMOOTH);

        JLabel imageLabel =
                new JLabel(new ImageIcon(resized));

        bubble.add(imageLabel);

        DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("HH:mm");

        String time = timestamp.format(formatter);

        JLabel timeLabel = new JLabel(time);

        timeLabel.setForeground(new java.awt.Color(110,110,110));
        
        JPanel timePanel = new JPanel(
                new java.awt.FlowLayout(
                        java.awt.FlowLayout.RIGHT,
                        0,
                        0));
                
        timePanel.setOpaque(false);
        timePanel.add(timeLabel);        
        bubble.add(timePanel);

        add(bubble);
    }

 
}