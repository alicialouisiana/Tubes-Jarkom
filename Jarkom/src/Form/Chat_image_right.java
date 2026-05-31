package Form;

import java.awt.Color;
import java.awt.Image;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class Chat_image_right extends JPanel {

    public Chat_image_right(byte[] imageData, LocalDateTime timestamp) {

        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel bubble = new JPanel();

        bubble.setBackground(new Color(179,233,255)); // sama seperti chat kanan
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));

        bubble.setBorder(
                BorderFactory.createEmptyBorder(
                        5,5,5,5));

        ImageIcon icon =
                new ImageIcon(imageData);

        Image resized =
                icon.getImage().getScaledInstance(
                        350,
                        -1,
                        Image.SCALE_SMOOTH);

        JLabel imageLabel =
                new JLabel(
                        new ImageIcon(resized));

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