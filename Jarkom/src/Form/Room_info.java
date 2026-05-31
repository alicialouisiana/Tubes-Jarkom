package Form;

import ServerClient.Message;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.DefaultListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

public class Room_info extends JFrame {

    private String roomName;
    private String ownerName;
    private String clientName; // The name of the client who opened this window
    private List<String> members;
    private Chat_panel chatPanelRef; // Reference to Chat_panel to send kick messages

    private JLabel roomNameLabel;
    private JLabel ownerLabel;
    private DefaultListModel<String> membersListModel;
    private JList<String> membersJList;

    public Room_info(String roomName, String ownerName, String clientName, List<String> members, Chat_panel chatPanelRef) {
        this.roomName = roomName;
        this.ownerName = ownerName;
        this.clientName = clientName;
        this.members = members;
        this.chatPanelRef = chatPanelRef;

        setTitle("Room Info: " + roomName);
        setSize(400, 500);
        setMinimumSize(new Dimension(300, 400));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null); // Center on screen

        initComponents();
        populateInfo();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10)); // Add some padding

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        roomNameLabel = new JLabel("Room: " + roomName);
        roomNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        topPanel.add(roomNameLabel);

        ownerLabel = new JLabel("Owner: " + ownerName);
        ownerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        topPanel.add(ownerLabel);

        add(topPanel, BorderLayout.NORTH);

        // Members List
        membersListModel = new DefaultListModel<>();
        membersJList = new JList<>(membersListModel);
        membersJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(membersJList);

        JPanel membersPanel = new JPanel(new BorderLayout());
        membersPanel.add(new JLabel("Members:"), BorderLayout.NORTH);
        membersPanel.add(scrollPane, BorderLayout.CENTER);
        add(membersPanel, BorderLayout.CENTER);

        // Buttons Panel (for owner actions)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        // Only show kick button if the current client is the owner and there are members to kick (more than just the owner)
        if (clientName != null && clientName.equals(ownerName) && members.size() > 1) {
            JButton kickButton = new JButton("Kick Selected Member");
            kickButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    kickSelectedMember();
                }
            });
            buttonPanel.add(kickButton);
        }

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose()); // Simply close the info window
        buttonPanel.add(closeButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void populateInfo() {
        membersListModel.clear();
        if (members != null) {
            for (String member : members) {
                membersListModel.addElement(member);
            }
        }
    }

    private void kickSelectedMember() {
        String selectedMember = membersJList.getSelectedValue();
        if (selectedMember == null) {
            JOptionPane.showMessageDialog(this, "Please select a member to kick.", "No Member Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Prevent kicking self (the owner)
        if (selectedMember.equals(clientName)) {
            JOptionPane.showMessageDialog(this, "You cannot kick yourself from the room you own!", "Invalid Action", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to kick '" + selectedMember + "' from " + roomName + "?", "Confirm Kick", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (chatPanelRef != null) {
                // Send KICK_USER_REQUEST to the server
                chatPanelRef.sendMessage(new Message(clientName, "Kick request for " + selectedMember,
                        Message.MessageType.KICK_USER_REQUEST, roomName, selectedMember));
                JOptionPane.showMessageDialog(this, "Kick request sent for " + selectedMember + ". The window will now close.", "Request Sent", JOptionPane.INFORMATION_MESSAGE);
                dispose(); // Close the window after sending kick request
            } else {
                JOptionPane.showMessageDialog(this, "Internal error: Chat panel reference not set.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // If you ever need to refresh the members list without closing the window:
    public void refreshMembers(List<String> newMembers) {
        SwingUtilities.invokeLater(() -> {
            membersListModel.clear();
            for (String member : newMembers) {
                membersListModel.addElement(member);
            }
        });
    }
}
