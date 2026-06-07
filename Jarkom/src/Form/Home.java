/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package Form;

import net.miginfocom.swing.MigLayout;

public class Home extends javax.swing.JLayeredPane {

    private Sender_panel senderPanel;
    private Chat_window chatWindow;

    public Home() {
        initComponents();
        init();
    }

    public void init() {
        setLayout(new MigLayout("fillx, filly", "3[fill, 300!]5[fill, 700!]3", "0[fill]0"));
        
        setBackground(new java.awt.Color(30,30,46));
        setOpaque(true);
        
        senderPanel = new Sender_panel();
        chatWindow = new Chat_window();
        
        // Establish references between panels
        senderPanel.setChatWindowReference(chatWindow);
        chatWindow.setSenderPanelReference(senderPanel);

        this.add(senderPanel);
        this.add(chatWindow);
        
        // Start the client connection after both panels are initialized and linked
        chatWindow.startClientConnection();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1000, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 720, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
