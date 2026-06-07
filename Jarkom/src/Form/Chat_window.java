/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package Form;

import ServerClient.Message;
import java.awt.Dimension;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.JFileChooser;
import java.io.File;

public class Chat_window extends javax.swing.JPanel {

    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 23181;

    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String clientName;
    private String currentRoom = null;
    private Timer typingTimer;
    private boolean isTyping = false;
    private Sender_panel senderPanelRef;

    public Chat_window() {
        initComponents();
        JButton emojiBtn = new JButton("😀");
        JButton imageBtn = new JButton("📷");
        emojiBtn.setFocusPainted(false);
        emojiBtn.setBorderPainted(false);

        // Supaya saat user menekan Enter, pesan langsung terkirim
        message.getInputMap().put(
                javax.swing.KeyStroke.getKeyStroke("ENTER"),
                "sendMessage"
        );

        message.getActionMap().put("sendMessage", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                send.doClick();
            }
        });

        //ini untuk menunjukkan pilihan emoji 
        emojiBtn.addActionListener(e -> {
            String emoji = (String) JOptionPane.showInputDialog(
                    this,
                    "Select Emoji",
                    "Emoji Picker",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    new String[]{"😀", "😂", "😍", "😭", "😎", "👍", "🔥", "🎉"},
                    "😀"
            ); //emoji yang bisa dipilih

            if (emoji != null) {
                message.append(emoji);
            }
        });

        //ini button untuk mengirim foto
        imageBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            int result = chooser.showOpenDialog(Chat_window.this);

            if (result == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                System.out.println(file.getAbsolutePath());
                sendImage(file);
            }
        });

        // ambil komponen lama
        msgScroll.setPreferredSize(new Dimension(500, 100));
        send.setPreferredSize(new Dimension(50, 50));
        emojiBtn.setPreferredSize(new Dimension(50, 50));

        // bikin panel baru
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.X_AXIS));
        inputPanel.add(emojiBtn);
        inputPanel.add(imageBtn);
        inputPanel.add(msgScroll);
        inputPanel.add(send);

        // ganti isi textLayer
        textLayer.removeAll();
        textLayer.setLayout(new java.awt.BorderLayout());
        textLayer.add(inputPanel, java.awt.BorderLayout.CENTER);

        textLayer.revalidate();
        textLayer.repaint();

        setBackground(new java.awt.Color(24, 24, 37));
        setOpaque(true);

        group_list.setBackground(new java.awt.Color(30, 30, 46));
        group_list.setOpaque(true);

        body.setBackground(new java.awt.Color(49, 50, 68));

        bottom.setBackground(new java.awt.Color(24, 24, 37));

        textLayer.setBackground(new java.awt.Color(24, 24, 37));
        textLayer.setOpaque(true);

        msgScroll.setBackground(new java.awt.Color(69, 71, 90));
        msgScroll.getViewport().setBackground(new java.awt.Color(69, 71, 90));

        message.setBackground(new java.awt.Color(69, 71, 90));
        message.setForeground(java.awt.Color.WHITE);
        message.setCaretColor(java.awt.Color.WHITE);

        message.setBorder(null);

        message.setFont(new java.awt.Font("Segoe UI Emoji", java.awt.Font.PLAIN, 15));

        name.setForeground(java.awt.Color.WHITE);

        send.setFont(new java.awt.Font("Segoe UI Emoji", java.awt.Font.BOLD, 14));

        send.setText("➤");

        info.setBackground(new java.awt.Color(88, 91, 112));
        info.setForeground(java.awt.Color.WHITE);

        leaveButton.setBackground(new java.awt.Color(243, 139, 168));
        leaveButton.setForeground(java.awt.Color.BLACK);

        closeButton.setBackground(new java.awt.Color(250, 179, 135));
        closeButton.setForeground(java.awt.Color.BLACK);

        closeButton.setVisible(false);
        inputFieldEnablement(false);

        // ini timer buat mematikan status typing setelah user berhenti mengetik
        typingTimer = new Timer(2000, e -> {
            sendTypingStatus(false);
            isTyping = false;
        });
        typingTimer.setRepeats(false);

        // Mengecek perubahan isi kolom pesan
        message.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                userIsTyping();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                userIsTyping();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                userIsTyping();
            }
        });

        send.setBorderPainted(false);
        send.setFocusPainted(false);

        info.setBorderPainted(false);
        info.setFocusPainted(false);

        leaveButton.setBorderPainted(false);
        leaveButton.setFocusPainted(false);

        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);

        java.awt.Font modernFont = new java.awt.Font("Segoe UI Emoji", java.awt.Font.PLAIN, 14);

        message.setFont(modernFont);
        name.setFont(new java.awt.Font("Segoe UI Emoji", java.awt.Font.BOLD, 16));
        status.setFont(modernFont);

        send.setFont(modernFont);
        info.setFont(modernFont);
        leaveButton.setFont(modernFont);
        closeButton.setFont(modernFont);

        closeButton.setBackground(new java.awt.Color(250, 179, 135));
        closeButton.setForeground(java.awt.Color.BLACK);

        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setOpaque(true);
        msgScroll.setBorder(null);

        // ini untuk icon send 
        java.awt.Image img = new javax.swing.ImageIcon(
                getClass().getResource("/Icon/Send.png")
        ).getImage();

        java.awt.Image newImg = img.getScaledInstance(40, 40, java.awt.Image.SCALE_SMOOTH);

        send.setIcon(new javax.swing.ImageIcon(newImg));

        send.setText("");

        send.setContentAreaFilled(false);
        send.setBorderPainted(false);
        send.setPreferredSize(new java.awt.Dimension(40, 40));
        send.setOpaque(false);
        send.setBorder(null);
        send.setFocusPainted(false);
        send.setBackground(new java.awt.Color(0, 0, 0, 0));
    }

    public void setSenderPanelReference(Sender_panel senderPanelRef) {
        this.senderPanelRef = senderPanelRef;
    }

    public String getClientName() {
        return clientName;
    }

    public void startClientConnection() {
        connectToServer();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {

        group_list = new javax.swing.JLayeredPane();
        bottom = new Form.Chat_bottom();
        textLayer = new javax.swing.JLayeredPane();
        msgScroll = new javax.swing.JScrollPane();
        message = new javax.swing.JTextArea();
        send = new javax.swing.JButton();
        body = new Form.Chat_body();
        name = new javax.swing.JLabel();
        status = new javax.swing.JLabel();
        info = new javax.swing.JButton();
        leaveButton = new javax.swing.JButton();
        closeButton = new javax.swing.JButton();

        message.setColumns(20);
        message.setLineWrap(true);
        message.setRows(5);
        message.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        msgScroll.setViewportView(message);

        send.setText("Send");
        send.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendActionPerformed(evt);
            }
        });

        textLayer.setLayer(msgScroll, javax.swing.JLayeredPane.DEFAULT_LAYER);
        textLayer.setLayer(send, javax.swing.JLayeredPane.DEFAULT_LAYER);

        javax.swing.GroupLayout textLayerLayout = new javax.swing.GroupLayout(textLayer);
        textLayer.setLayout(textLayerLayout);
        textLayerLayout.setHorizontalGroup(
                textLayerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(textLayerLayout.createSequentialGroup()
                                .addComponent(msgScroll)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(send)
                                .addContainerGap())
        );
        textLayerLayout.setVerticalGroup(
                textLayerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(textLayerLayout.createSequentialGroup()
                                .addGroup(textLayerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(textLayerLayout.createSequentialGroup()
                                                .addComponent(send)
                                                .addGap(0, 0, Short.MAX_VALUE))
                                        .addComponent(msgScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 194, Short.MAX_VALUE))
                                .addContainerGap())
        );

        bottom.setLayer(textLayer, javax.swing.JLayeredPane.DEFAULT_LAYER);

        javax.swing.GroupLayout bottomLayout = new javax.swing.GroupLayout(bottom);
        bottom.setLayout(bottomLayout);
        bottomLayout.setHorizontalGroup(
                bottomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(textLayer)
        );
        bottomLayout.setVerticalGroup(
                bottomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(textLayer)
        );

        name.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        name.setText("NAMA");

        status.setForeground(new java.awt.Color(51, 255, 0));
        status.setText("Online");

        info.setText("Info");
        info.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                infoActionPerformed(evt);
            }
        });

        leaveButton.setBackground(new java.awt.Color(255, 51, 51));
        leaveButton.setText("Leave");
        leaveButton.setToolTipText("");
        leaveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                leaveButtonActionPerformed(evt);
            }
        });

        closeButton.setText("Close Group");
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });

        group_list.setLayer(bottom, javax.swing.JLayeredPane.DEFAULT_LAYER);
        group_list.setLayer(body, javax.swing.JLayeredPane.DEFAULT_LAYER);
        group_list.setLayer(name, javax.swing.JLayeredPane.DEFAULT_LAYER);
        group_list.setLayer(status, javax.swing.JLayeredPane.DEFAULT_LAYER);
        group_list.setLayer(info, javax.swing.JLayeredPane.DEFAULT_LAYER);
        group_list.setLayer(leaveButton, javax.swing.JLayeredPane.DEFAULT_LAYER);
        group_list.setLayer(closeButton, javax.swing.JLayeredPane.DEFAULT_LAYER);

        javax.swing.GroupLayout group_listLayout = new javax.swing.GroupLayout(group_list);
        group_list.setLayout(group_listLayout);
        group_listLayout.setHorizontalGroup(
                group_listLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(group_listLayout.createSequentialGroup()
                                .addGroup(group_listLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                        .addComponent(body, javax.swing.GroupLayout.DEFAULT_SIZE, 700, Short.MAX_VALUE)
                                        .addComponent(bottom, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(0, 0, Short.MAX_VALUE))
                        .addGroup(group_listLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(group_listLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(status)
                                        .addComponent(name, javax.swing.GroupLayout.PREFERRED_SIZE, 178, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(closeButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(leaveButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(info)
                                .addContainerGap())
        );
        group_listLayout.setVerticalGroup(
                group_listLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(group_listLayout.createSequentialGroup()
                                .addGroup(group_listLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(group_listLayout.createSequentialGroup()
                                                .addContainerGap()
                                                .addComponent(name, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(status)
                                                .addGap(8, 8, 8))
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, group_listLayout.createSequentialGroup()
                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addGroup(group_listLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(info)
                                                        .addComponent(leaveButton)
                                                        .addComponent(closeButton))
                                                .addGap(18, 18, 18)))
                                .addComponent(body, javax.swing.GroupLayout.DEFAULT_SIZE, 458, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(bottom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(group_list)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(group_list)
        );
    }// </editor-fold>                        

    private void sendImage(File file) {
        try {
            byte[] data = Files.readAllBytes(file.toPath());
            Message msg = new Message(
                    clientName,
                    file.getName(),
                    Message.MessageType.IMAGE,
                    currentRoom
            );
            msg.setImageData(data);
            sendMessage(msg);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void userIsTyping() {
        // Kalau belum login atau belum masuk room, tidak perlu kirim typing
        if (clientName == null || currentRoom == null) {
            return;
        }

        // Kalau kolom pesan kosong, berarti user tidak sedang mengetik
        if (message.getText().trim().isEmpty()) {
            sendTypingStatus(false);
            isTyping = false;
            return;
        }

        // Kirim typing true sekali saja, supaya tidak spam setiap huruf
        if (!isTyping) {
            sendTypingStatus(true);
            isTyping = true;
        }

        // Timer diulang setiap user mengetik
        typingTimer.restart();
    }

    private void sendTypingStatus(boolean typing) {
        if (clientName == null || currentRoom == null) {
            return;
        }

        Message typingMessage = new Message(
                clientName,
                typing ? "true" : "false",
                Message.MessageType.TYPING,
                currentRoom
        );

        sendMessage(typingMessage);
    }

    private void sendActionPerformed(java.awt.event.ActionEvent evt) {
        String msg = message.getText().trim();

        if (!msg.isEmpty() && clientName != null && currentRoom != null) {
            // tampilin pesan sendiri di kanan
            body.addItemRight(msg);

            // kirim pesan ke server
            sendMessage(new Message(clientName, msg, Message.MessageType.TEXT, currentRoom));

            // kosongkan kolom pesan
            message.setText("");

            // kalau pesan terkirim, status typing dimatikan
            sendTypingStatus(false);
            isTyping = false;
            status.setText("Online");

        } else if (currentRoom == null) {
            body.addItemLeft("System: Please join a room to send messages.");
        } else if (clientName == null) {
            body.addItemLeft("System: Not logged in. Please restart and enter your name.");
        }
    }

    private void infoActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
        requestRoomInfo();
    }

    private void leaveButtonActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
        if (clientName != null && currentRoom != null) {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to leave the room '" + currentRoom + "'?",
                    "Confirm Leave",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm == JOptionPane.YES_OPTION) {
                Message leaveMessage = new Message(clientName, "Request to leave room", Message.MessageType.LEAVE_ROOM_REQUEST, currentRoom);
                sendMessage(leaveMessage);
            }
        } else {
            JOptionPane.showMessageDialog(this, "You are not in a room.", "Leave Room", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
        if (clientName != null && currentRoom != null) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to close this room?",
                    "Confirm Close Room", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                sendMessage(new Message(clientName, "Requesting to close room",
                        Message.MessageType.CLOSE_ROOM_REQUEST, currentRoom));
            }
        } else {
            body.addItemLeft("System: You are not in a room.");
        }
    }

    public void sendMessage(Message msg) {
        if (out != null) {
            try {
                out.writeObject(msg);
                out.flush();
                System.out.println("Client sent: Type=" + msg.getType() + ", Content=" + msg.getContent() + (msg.getRoomName() != null ? " to room " + msg.getRoomName() : ""));
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> body.addItemLeft("System: Error : " + e.getMessage()));
                System.err.println("Error: " + e.getMessage());
            }
        } else {
            SwingUtilities.invokeLater(() -> body.addItemLeft("System: Not connected to the server. Please check connection."));
        }
    }

    private void connectToServer() {
        new Thread(() -> {
            try {
                System.out.println("Client: Attempting to connect to server at " + SERVER_ADDRESS + ":" + SERVER_PORT);
                socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                System.out.println("Client: Socket connected successfully.");

                out = new ObjectOutputStream(socket.getOutputStream());
                System.out.println("Client: ObjectOutputStream created.");

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    System.err.println("Client: Thread sleep interrupted.");
                }

                in = new ObjectInputStream(socket.getInputStream());
                System.out.println("Client: ObjectInputStream created. Ready to receive messages.");

                SwingUtilities.invokeLater(() -> body.addItemLeft("System: Connected to server. Select or create room"));

                while (true) {
                    Object receivedObject = in.readObject();

                    if (!(receivedObject instanceof Message)) {
                        System.err.println("Client: Received non-Message object from server: " + receivedObject.getClass().getName());
                        continue;
                    }

                    Message incomingMessage = (Message) receivedObject;
                    System.out.println("Client: Received message from server - Type: " + incomingMessage.getType() + ", Content: " + incomingMessage.getContent());

                    SwingUtilities.invokeLater(() -> {
                        switch (incomingMessage.getType()) {
                            case SYSTEM:
                                if (incomingMessage.getContent().equals("SUBMITNAME")) {
                                    System.out.println("Client: Server requested name. Displaying JOptionPane.");
                                    clientName = JOptionPane.showInputDialog(
                                            Chat_window.this,
                                            "Enter your name:",
                                            "Name",
                                            JOptionPane.PLAIN_MESSAGE
                                    );

                                    System.out.println("DEBUG (Client): clientName from JOptionPane is: '" + clientName + "'");

                                    if (clientName == null || clientName.trim().isEmpty()) {
                                        JOptionPane.showMessageDialog(Chat_window.this, "Name cannot be empty. Exiting.", "Error", JOptionPane.ERROR_MESSAGE);
                                        System.exit(0);
                                    }

                                    Message nameSubmitMessage = new Message(clientName, clientName, Message.MessageType.SYSTEM);
                                    System.out.println("DEBUG (Client): Message object created for name submission: " + nameSubmitMessage.toString());
                                    sendMessage(nameSubmitMessage);

                                    name.setText(clientName);
                                    status.setText("Online"); 
                                    status.setForeground(new java.awt.Color(51, 255, 0)); // Green for online

                                    // Request initial room list after name submission
                                    sendMessage(new Message(clientName, "Requesting room list", Message.MessageType.LIST_ROOMS_REQUEST));
                                } else {
                                    body.addItemLeft("System: " + incomingMessage.getContent());
                                }
                                break;
                            case TEXT:
                                if (incomingMessage.getRoomName() != null && incomingMessage.getRoomName().equals(currentRoom)) {
                                    if (!incomingMessage.getSenderName().equals(clientName)) {
                                        body.addItemLeft(incomingMessage.getSenderName() + ": " + incomingMessage.getContent());
                                    }
                                } else if (incomingMessage.getRoomName() == null) { 
                                    body.addItemLeft(incomingMessage.getSenderName() + ": " + incomingMessage.getContent());
                                }
                                break;
                            case IMAGE:
                                byte[] img = incomingMessage.getImageData();
                                if (img != null) {
                                    if (incomingMessage.getSenderName().equals(clientName)) {
                                        body.addImageRight(img, incomingMessage.getTimestamp());
                                    } else {
                                        body.addImageLeft(incomingMessage.getSenderName(), img, incomingMessage.getTimestamp());
                                    }
                                }
                                break;

                            case TYPING:
                                if (incomingMessage.getRoomName() != null
                                        && incomingMessage.getRoomName().equals(currentRoom)
                                        && !incomingMessage.getSenderName().equals(clientName)) {

                                    if (incomingMessage.getContent().equals("true")) {
                                        status.setText(incomingMessage.getSenderName() + " is typing...");
                                    } else {
                                        status.setText("Online");
                                    }
                                }
                                break;
                            case USER_JOINED_ROOM:
                            case USER_LEFT_ROOM:
                                // Tampilkan pesan hanya jika itu untuk currentRoom
                                if (incomingMessage.getRoomName() != null && incomingMessage.getRoomName().equals(currentRoom)) {
                                    body.addItemLeft("System: " + incomingMessage.getContent());
                                }
                                break;
                            case USER_KICKED:
                            
                                if (incomingMessage.getRoomName() != null && incomingMessage.getRoomName().equals(currentRoom)
                                        && incomingMessage.getContent().contains("You have been kicked")) {
                                    // Tambahkan pop-up pemberitahuan
                                    JOptionPane.showMessageDialog(
                                            Chat_window.this,
                                            "You have been kicked from the room '" + currentRoom + "'.",
                                            "Kicked from Room",
                                            JOptionPane.WARNING_MESSAGE
                                    );
                                   
                                    closeButton.setVisible(false);
                                    sendMessage(new Message(clientName, "Requesting room list", Message.MessageType.LIST_ROOMS_REQUEST));
                                }
                                break;
                            case ROOM_CLOSED:
                                if (incomingMessage.getRoomName() != null && incomingMessage.getRoomName().equals(currentRoom)) {
                                    body.addItemLeft("System: This room has been closed by the owner.");
                                    currentRoom = "General";
                                    name.setText(clientName + " (General)");
                                    name.setToolTipText("Current Room: None");
                                    body.removeAllItems();
                                    inputFieldEnablement(false);
                                    sendMessage(new Message(clientName, "Requesting room list", Message.MessageType.LIST_ROOMS_REQUEST));
                                } else {
                                    body.addItemLeft(incomingMessage.getContent()); 
                                }
                                break;
                            case LIST_ROOMS_RESPONSE:
                                if (senderPanelRef != null && incomingMessage.getDataList() != null) {
                                    senderPanelRef.updateRoomList(incomingMessage.getDataList());
                                }
                                break;
                            case JOIN_ROOM_RESPONSE:
                                if (incomingMessage.getContent().startsWith("SUCCESS")) {
                                    closeButton.setVisible(false);
                                    String joinedRoom = incomingMessage.getRoomName();
                                    currentRoom = joinedRoom;  
                                    name.setText(clientName + " (Room: " + currentRoom + ")");
                                    name.setToolTipText("Current Room: " + currentRoom);
                                    body.removeAllItems(); // Clear previous chat history
                                    body.addItemLeft("System: Hi welcome to '" + joinedRoom + "' room.");
                                    inputFieldEnablement(true);
                                    sendMessage(new Message(clientName, "Requesting users", Message.MessageType.USERS_IN_ROOM_REQUEST, currentRoom));

                                    sendMessage(new Message(clientName, "Silent", Message.MessageType.ROOM_INFO_REQUEST, currentRoom));
                                } else {
                                    body.addItemLeft("System: Failed to join room: " + incomingMessage.getContent());
                                    currentRoom = null; 
                                    name.setText(clientName + " (No Room)");
                                    name.setToolTipText("Current Room: None");
                                    inputFieldEnablement(false);
                                }
                                break;

                            case LEAVE_ROOM_RESPONSE:
                                if (incomingMessage.getContent().startsWith("SUCCESS")) {
                                    body.addItemLeft("System: You have left room '" + incomingMessage.getRoomName() + "'");
                                    currentRoom = null; // <<-- Clear when leaving
                                    name.setText(clientName + " (No Room)");
                                    name.setToolTipText("Current Room: None");
                                    body.removeAllItems();
                                    inputFieldEnablement(false);
                                    closeButton.setVisible(false);
                                    sendMessage(new Message(clientName, "Requesting room list", Message.MessageType.LIST_ROOMS_REQUEST));
                                } else {
                                    body.addItemLeft("System: Failed to leave room: " + incomingMessage.getContent());
                                }
                                break;
                            case USERS_IN_ROOM_RESPONSE:
                                if (incomingMessage.getDataList() != null && incomingMessage.getRoomName().equals(currentRoom)) {
                                    body.addItemLeft("System: Users in '" + incomingMessage.getRoomName() + "': " + String.join(", ", incomingMessage.getDataList()));
                                }
                                break;
                            case ROOM_INFO_RESPONSE:
                                String infoRoomName = incomingMessage.getRoomName();
                                String roomOwner = incomingMessage.getTargetUser();    
                                List<String> roomMembers = incomingMessage.getDataList(); 

                                if (infoRoomName != null && roomOwner != null && roomMembers != null) {
                                    if (incomingMessage.getContent().endsWith("Group List")) {
                                        System.out.println("Masuk sini");
                                        SwingUtilities.invokeLater(() -> {
                                            JPanel panel = new JPanel();
                                            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

                                            panel.add(new JLabel("📌 Room Name: " + infoRoomName));
                                            panel.add(new JLabel("👤 Admin: " + roomOwner));
                                            panel.add(new JLabel("👥 Members:"));

                                            for (String member : roomMembers) {
                                                panel.add(new JLabel("   • " + member));
                                            }

                                            JDialog dialog = new JDialog((java.awt.Frame) SwingUtilities.getWindowAncestor(Chat_window.this), "Room Info", true);
                                            dialog.setContentPane(panel);
                                            dialog.setPreferredSize(new Dimension(400, 300)); // Lebar 400px, tinggi 300px
                                            dialog.pack();
                                            dialog.setLocationRelativeTo(Chat_window.this); // tengah relatif terhadap Chat_window
                                            dialog.setVisible(true);
                                        });

                                    } else {

                                        if (incomingMessage.getContent().startsWith("Silent")) {
                                            // Hanya set visibilitas tombol Close Group
                                            if (roomOwner.equals(clientName)) {
                                                closeButton.setVisible(true);
                                            } else {
                                                closeButton.setVisible(false);
                                            }
                                        } else {
                                            SwingUtilities.invokeLater(() -> {
                                                Room_info roomInfoWindow = new Room_info(infoRoomName, roomOwner, clientName, roomMembers, Chat_window.this);
                                                roomInfoWindow.setVisible(true);
                                            });
                                            System.out.println("Opened Room_info window for room: " + infoRoomName);
                                        }
                                    }

                                    if (roomOwner.equals(clientName)) {
                                        closeButton.setVisible(true);
                                    } else {
                                        closeButton.setVisible(false);
                                    }

                                } else {
                                    body.addItemLeft("System: Error retrieving room info: Invalid data received.");
                                    System.err.println("Client: ROOM_INFO_RESPONSE missing data. Room: " + infoRoomName + ", Admin: " + roomOwner + ", Members: " + roomMembers);
                                }
                                break;
                            case KICK_USER_RESPONSE:
                                if (incomingMessage.getContent().startsWith("SUCCESS")) {
                                    //body.addItemLeft("System: User " + incomingMessage.getTargetUser() + " has been kicked from " + incomingMessage.getRoomName());
                                } else {
                                    body.addItemLeft("System: Failed to kick user: " + incomingMessage.getContent());
                                }
                                break;
                            case CLOSE_ROOM_RESPONSE:
                                if (incomingMessage.getContent().startsWith("SUCCESS")) {
                                    body.addItemLeft("System: Room '" + incomingMessage.getRoomName() + "' has been closed.");
//                                    currentRoom = "General"; // Reset current room
//                                    name.setText(clientName + " (General)");
//                                    name.setToolTipText("Current Room: None");
//                                    body.removeAllItems(); // Clear chat history
//                                    inputFieldEnablement(false); // Disable chat input
                                    sendMessage(new Message(clientName, "Requesting room list", Message.MessageType.LIST_ROOMS_REQUEST)); // Request updated list
                                } else {
                                    body.addItemLeft("System: Failed to close room: " + incomingMessage.getContent());
                                }
                                break;
                            default:
                                body.addItemLeft("System: Unhandled message type: " + incomingMessage.getType() + " - " + incomingMessage.getContent());
                                break;
                        }
                    });
                }

            } catch (IOException e) {
                String errorMessage;
                if (e instanceof java.net.ConnectException) {
                    errorMessage = "Check your server again";
                } else if (e instanceof java.io.EOFException) {
                    errorMessage = "Check your server again.";
                } else {
                    errorMessage = "Network error: " + e.getMessage();
                }
                System.err.println("Client Connection Error: " + errorMessage);
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    body.addItemLeft("System: Server disconnected. (" + errorMessage + ")");
                    inputFieldEnablement(false);
                    status.setText("Offline");
                    status.setForeground(new java.awt.Color(204, 0, 0));
                });
            } catch (ClassNotFoundException e) {
                System.err.println("Client Deserialization Error: " + e.getMessage());
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> body.addItemLeft("System: Error receiving data from server. (Class Not Found)"));
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                    if (in != null) {
                        in.close();
                    }
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                    System.out.println("Client: Connection closed and resources released.");
                } catch (IOException e) {
                    System.err.println("Client: Error during resource cleanup: " + e.getMessage());
                }
            }
        }).start();
    }

    private void inputFieldEnablement(boolean enable) {
        message.setEnabled(enable);
        send.setEnabled(enable);
    }

    public String getCurrentRoom() {
        return currentRoom;
    }

    // This method is now primarily for internal use, as JOIN_ROOM_RESPONSE sets currentRoom
    public void setCurrentRoom(String roomName) {
        this.currentRoom = roomName;
        SwingUtilities.invokeLater(() -> {
            if (currentRoom != null) {
                name.setText(clientName + " (Room: " + currentRoom + ")");
                name.setToolTipText("Current Room: " + currentRoom);
                inputFieldEnablement(true);
            } else {
                name.setText(clientName + " (No Room)");
                name.setToolTipText("Current Room: None");
                inputFieldEnablement(false);
            }
            body.removeAllItems(); // Clear chat history when room changes
        });
    }

    private void requestRoomInfo() {
        if (clientName == null || currentRoom == null) {
            body.addItemLeft("System: Please connect and join a room to view its info.");
            JOptionPane.showMessageDialog(this, "Please connect and join a room to view its info.", "Action Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Send ROOM_INFO_REQUEST to server
        // The roomName field is crucial here
        sendMessage(new Message(clientName, "Requesting info for " + currentRoom,
                Message.MessageType.ROOM_INFO_REQUEST, currentRoom));
        System.out.println(clientName + " requesting info for room: " + currentRoom);
    }

    // Variables declaration - do not modify                     
    private Form.Chat_body body;
    private Form.Chat_bottom bottom;
    private javax.swing.JButton closeButton;
    private javax.swing.JLayeredPane group_list;
    private javax.swing.JButton info;
    private javax.swing.JButton leaveButton;
    private javax.swing.JTextArea message;
    private javax.swing.JScrollPane msgScroll;
    private javax.swing.JLabel name;
    private javax.swing.JButton send;
    private javax.swing.JLabel status;
    private javax.swing.JLayeredPane textLayer;
    // End of variables declaration                   
}
