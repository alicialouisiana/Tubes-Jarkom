/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package Form;

import ServerClient.Message;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import net.miginfocom.swing.MigLayout;

public class Left_panel extends javax.swing.JPanel {
// Reference to the main panel or a central controller to send messages

    private Chat_panel chatPanelRef; // Reference to the Chat_panel to send messages

    public Left_panel() {
        initComponents();

        setBackground(new java.awt.Color(24, 24, 37));
        setOpaque(true);

        scroll.setBackground(new java.awt.Color(24, 24, 37));
        scroll.getViewport().setBackground(new java.awt.Color(24, 24, 37));
        group_list.setBackground(new java.awt.Color(24, 24, 37));

        scroll.setOpaque(false);
        scroll.setBorder(null);
        group_list.setOpaque(true);
        // Set the layout for the group_list JLayeredPane
        group_list.setLayout(new MigLayout(
                "fillx, insets 5",
                "[grow]"
        ));;
        initControls(); // Initialize buttons and other controls
    }

    // This method is called from Home.java to give Left_panel a reference to Chat_panel
    public void setChatPanelReference(Chat_panel chatPanelRef) {
        this.chatPanelRef = chatPanelRef;
        // After setting reference, request initial room list
        // This ensures the room list is populated as soon as the client is ready
        if (chatPanelRef != null && chatPanelRef.getClientName() != null) {
            chatPanelRef.sendMessage(new Message(chatPanelRef.getClientName(), "Requesting room list", Message.MessageType.LIST_ROOMS_REQUEST));
        }
    }

    private void initControls() {
        // Add a button for creating new rooms
        JButton createRoomButton = new JButton("Create New Room");

        createRoomButton.setBackground(new java.awt.Color(137, 180, 250));
        createRoomButton.setForeground(java.awt.Color.BLACK);

        createRoomButton.setBorderPainted(false);
        createRoomButton.setFocusPainted(false);
        createRoomButton.setContentAreaFilled(false);
        createRoomButton.setOpaque(true);

        createRoomButton.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13));
        createRoomButton.addActionListener(e -> createNewRoom());
        // Add the button to the Left_panel's layout. Adjust position as needed.
        // Using MigLayout, you can place it at the top.
        this.setLayout(new MigLayout("fillx, filly", "[]", "[pref!][fill]")); // Adjust Left_panel's own layout
        this.add(createRoomButton, "wrap, alignx center, gapy 5 5"); // Place button at top center, then wrap
        this.add(scroll, "grow, wrap"); // Add the scroll pane to fill remaining space

    }

    private void createNewRoom() {
        String roomName = JOptionPane.showInputDialog(this, "Enter new room name:", "Create Room", JOptionPane.PLAIN_MESSAGE);
        if (roomName != null && !roomName.trim().isEmpty()) {
            if (chatPanelRef != null && chatPanelRef.getClientName() != null) {
                // Send a CREATE_ROOM_REQUEST message to the server
                chatPanelRef.sendMessage(new Message(chatPanelRef.getClientName(), roomName.trim(), Message.MessageType.CREATE_ROOM_REQUEST, roomName.trim()));
            } else {
                JOptionPane.showMessageDialog(this, "Please ensure you are connected and have a name set.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Method to update the displayed list of rooms
    public void updateRoomList(List<String> rooms) {
        SwingUtilities.invokeLater(() -> {
            group_list.removeAll(); // Clear existing room bars
            for (String roomName : rooms) {
                Group_bar roomBar = new Group_bar(roomName.length() > 12 ? roomName.substring(0, 12) + "..." : roomName, chatPanelRef.getClientName(), roomName, chatPanelRef);

                // --- ADD MOUSE LISTENER TO EACH GROUP_BAR ---
                roomBar.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        // When a room bar is clicked, attempt to join that room
                        joinRoom(roomName); // Call the joinRoom method with the clicked room's name
                    }

                    // Optional: Add hover effects for better UX
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        roomBar.setBackground(new java.awt.Color(69, 71, 90)); // Highlight on hover
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        roomBar.setBackground(new java.awt.Color(24, 24, 37)); // Reset background
                    }
                });
                // --- END MOUSE LISTENER ---

                group_list.add(roomBar, "wrap, growx, gapy 1"); // Add to the layout, grow horizontally
            }
            group_list.revalidate(); // Re-calculate layout
            group_list.repaint();   // Re-draw components
        });
    }

    private void joinRoom(String roomName) {
        if (chatPanelRef != null && chatPanelRef.getClientName() != null) {
            // Send a JOIN_ROOM_REQUEST message to the server
            chatPanelRef.sendMessage(new Message(chatPanelRef.getClientName(), "Joining room", Message.MessageType.JOIN_ROOM_REQUEST, roomName));
        } else {
            JOptionPane.showMessageDialog(this, "Please ensure you are connected and have a name set.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        scroll = new javax.swing.JScrollPane();
        group_list = new javax.swing.JLayeredPane();

        scroll.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        javax.swing.GroupLayout group_listLayout = new javax.swing.GroupLayout(group_list);
        group_list.setLayout(group_listLayout);
        group_listLayout.setHorizontalGroup(
            group_listLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 350, Short.MAX_VALUE)
        );
        group_listLayout.setVerticalGroup(
            group_listLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 720, Short.MAX_VALUE)
        );

        scroll.setViewportView(group_list);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addComponent(scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 346, Short.MAX_VALUE)
                .addGap(2, 2, 2))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addComponent(scroll, javax.swing.GroupLayout.PREFERRED_SIZE, 640, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(72, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLayeredPane group_list;
    private javax.swing.JScrollPane scroll;
    // End of variables declaration//GEN-END:variables
}
