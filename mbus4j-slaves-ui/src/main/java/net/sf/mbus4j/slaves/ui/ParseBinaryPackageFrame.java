package net.sf.mbus4j.slaves.ui;

/*
 * #%L
 * mbus4j-slaves-ui
 * %%
 * Copyright (C) 2009 - 2014 MBus4J
 * %%
 * mbus4j - Drivers for the M-Bus protocol - http://mbus4j.sourceforge.net/
 * Copyright (C) 2009-2014, mbus4j.sf.net, and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * #L%
 */
import java.awt.event.ActionListener;
import net.sf.mbus4j.dataframes.Frame;
import net.sf.mbus4j.dataframes.UserDataResponse;
import net.sf.mbus4j.decoder.Decoder;
import net.sf.mbus4j.decoder.DecoderListener;
import net.sf.mbus4j.slaves.Slave;
import org.jdesktop.application.Action;

/**
 *
 * @author aploese
 */
public class ParseBinaryPackageFrame extends javax.swing.JFrame {

    /**
     * Creates new form ParseBinaryPackageFrame
     */
    public ParseBinaryPackageFrame() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton1 = new javax.swing.JButton();
        binaryPackageLabel = new javax.swing.JLabel();
        binaryPackageTextField = new javax.swing.JTextField();
        jSeparator1 = new javax.swing.JSeparator();
        slavePane1 = new net.sf.mbus4j.slaves.ui.SlavePane();
        parseButton = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JSeparator();
        addButton = new javax.swing.JButton();

        setMinimumSize(new java.awt.Dimension(837, 337));

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance().getContext().getActionMap(ParseBinaryPackageFrame.class, this);
        jButton1.setAction(actionMap.get("closeFrame")); // NOI18N
        jButton1.setName("jButton1"); // NOI18N

        binaryPackageLabel.setText("Package:");
        binaryPackageLabel.setName("binaryPackageLabel"); // NOI18N

        binaryPackageTextField.setText("684D4D680800725020770877040B042D0000000C78502077080407DA3800000C15203308000B2D2602000B3B9312000A5A39060A5E87040B61221500046D23092D130227FB0009FD0E1009FD0F200F00001916");
        binaryPackageTextField.setName("binaryPackageTextField"); // NOI18N

        jSeparator1.setName("jSeparator1"); // NOI18N

        slavePane1.setName("slavePane1"); // NOI18N

        parseButton.setAction(actionMap.get("parsePackage")); // NOI18N
        parseButton.setName("parseButton"); // NOI18N

        jSeparator2.setName("jSeparator2"); // NOI18N

        addButton.setText("Add");
        addButton.setName("addButton"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(slavePane1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jSeparator2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 813, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(binaryPackageLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(binaryPackageTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 699, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(parseButton))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(addButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton1))
                    .addComponent(jSeparator1, javax.swing.GroupLayout.DEFAULT_SIZE, 813, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(binaryPackageLabel)
                    .addComponent(binaryPackageTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(parseButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(slavePane1, javax.swing.GroupLayout.DEFAULT_SIZE, 460, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(addButton))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ParseBinaryPackageFrame().setVisible(true);
            }
        });
    }

    @Action
    public void parsePackage() {
        Decoder decoder = new Decoder(new DecoderListener() {

            @Override
            public void success(Frame frame) {
                if (frame instanceof UserDataResponse) {
                    Slave s = Slave.fromResponse((UserDataResponse) frame);
                    slavePane1.setSlave(s);
                } else {
                    slavePane1.setSlave(null);
                }
            }

        });
        byte[] data = Decoder.ascii2Bytes(binaryPackageTextField.getText());
        for (byte b : data) {
            decoder.addByte(b);
        }
    }

    @Action
    public void closeFrame() {
        setVisible(false);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JLabel binaryPackageLabel;
    private javax.swing.JTextField binaryPackageTextField;
    private javax.swing.JButton jButton1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JButton parseButton;
    private net.sf.mbus4j.slaves.ui.SlavePane slavePane1;
    // End of variables declaration//GEN-END:variables

    public void addAddSlaveListener(ActionListener l) {
        addButton.addActionListener(l);
    }

    public Slave createSlave() {
        return slavePane1.createSlave();
    }

    public void setSlave(Slave s) {
        slavePane1.setSlave(s);
    }

    void commitChanges() {
        slavePane1.commitChanges(slavePane1.getSlave());
    }

}
