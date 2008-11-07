   package jp.sourceforge.qrcode.example.jmf;

   import java.io.*;
   import java.net.URL;
   import java.net.MalformedURLException;
   import java.awt.*;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
   import javax.swing.JFileChooser;
import javax.swing.JLabel;
   import javax.media.*;
   import javax.media.control.*;
   import javax.media.protocol.*;
   import javax.media.format.VideoFormat;

import javax.swing.JOptionPane;


   public class mainFrame extends javax.swing.JFrame {
       
       private camDataSource dataSource;
       
       private DataSource camSource;
       private DataSource recordCamSource;
       private DataSink dataSink;
       private Processor processor;
       private Processor recordProcessor;
       private camStateHelper playhelper;
       
       private JFileChooser movieChooser;
       
       Thread decoderThread;
       
       public mainFrame(camDataSource dataSource) {
           this.dataSource = dataSource;
           this.dataSource.setParent(this);
           camSource = dataSource.cloneCamSource();
           initComponents();
           try{
               processor = Manager.createProcessor(camSource);
           }catch (IOException e) {
               JOptionPane.showMessageDialog(this, 
                  "Exception creating processor: " + e.getMessage(), "Error", JOptionPane.WARNING_MESSAGE);
               return;
           }catch (NoProcessorException e) {
               JOptionPane.showMessageDialog(this, 
                  "Exception creating processor: " + e.getMessage(), "Error", JOptionPane.WARNING_MESSAGE);
               return;
           }
           playhelper = new camStateHelper(processor);
           if(!playhelper.configure(10000)){
               JOptionPane.showMessageDialog(this, 
                  "cannot configure processor", "Error", JOptionPane.WARNING_MESSAGE);
               return;
           }
           checkIncoding(processor.getTrackControls());
           processor.setContentDescriptor(null);
           if(!playhelper.realize(10000)){
               JOptionPane.showMessageDialog(this, 
                  "cannot realize processor", "Error", JOptionPane.WARNING_MESSAGE);
               return;
           }
           
           setJPEGQuality(processor, 1.0f);
           processor.start();
           processor.getVisualComponent().setBackground(Color.gray);
           centerPanel.add(processor.getVisualComponent(), BorderLayout.CENTER);
           centerPanel.add(processor.getControlPanelComponent(), BorderLayout.SOUTH);
           QRCodeDecoderJMFExample decoder = new  QRCodeDecoderJMFExample(processor);
           Thread decoderThread = new Thread(decoder);
           decoderThread.start();
       }
       
       
       private void initComponents() {//GEN-BEGIN:initComponents
           northPanel = new javax.swing.JPanel();
           messageLabel = new javax.swing.JLabel();
           southPanel = new javax.swing.JPanel();
           mainToolBar = new javax.swing.JToolBar();
           recordButton = new javax.swing.JButton();
           fileLabel = new javax.swing.JLabel();
           centerPanel = new javax.swing.JPanel();

           setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
           setTitle("My Webcam");
           addWindowListener(new java.awt.event.WindowAdapter() {
               public void windowClosing(java.awt.event.WindowEvent evt) {
                   formWindowClosing(evt);
               }
           });

           northPanel.setLayout(new java.awt.BorderLayout());

           messageLabel.setText("Status");
           northPanel.add(messageLabel, java.awt.BorderLayout.CENTER);

           getContentPane().add(northPanel, java.awt.BorderLayout.NORTH);

           southPanel.setLayout(new java.awt.BorderLayout());

           recordButton.setText("Record");
           recordButton.addActionListener(new java.awt.event.ActionListener() {
               public void actionPerformed(java.awt.event.ActionEvent evt) {
                   recordButtonActionPerformed(evt);
               }
           });

           mainToolBar.add(recordButton);

           fileLabel.setText("File:");
           mainToolBar.add(fileLabel);

           southPanel.add(mainToolBar, java.awt.BorderLayout.CENTER);

           getContentPane().add(southPanel, java.awt.BorderLayout.SOUTH);

           centerPanel.setLayout(new java.awt.BorderLayout());

           getContentPane().add(centerPanel, java.awt.BorderLayout.CENTER);

           pack();
       }//GEN-END:initComponents
       
       private void formWindowClosing(java.awt.event.WindowEvent evt) {
          //GEN-FIRST:event_formWindowClosing
           processor.close();
       }//GEN-LAST:event_formWindowClosing
       
       private void recordButtonActionPerformed(java.awt.event.ActionEvent evt) {
          //GEN-FIRST:event_recordButtonActionPerformed
           if(recordButton.getText().equals("Record")){
               fileLabel.setText("File:");
               if (movieChooser == null) movieChooser = new JFileChooser();
               movieChooser.setDialogType(JFileChooser.SAVE_DIALOG);
               //Add a custom file filter and disable the default
               //(Accept All) file filter.
               movieChooser.addChoosableFileFilter(new MOVFilter());
               movieChooser.setAcceptAllFileFilterUsed(false);
               movieChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
               int returnVal = movieChooser.showDialog(this, "Record");
               if (returnVal == JFileChooser.APPROVE_OPTION) {
                   File file = movieChooser.getSelectedFile();
                   if(!file.getName().endsWith(".mov")
                      &&!file.getName().endsWith(".MOV")) file = new File(file.toString() + ".mov");
                   recordToFile(file);
                   fileLabel.setText("File:" + file.toString());
                   recordButton.setText("Stop");
               }
           }else{
               stopRecording();
               recordButton.setText("Record");
           }
       }//GEN-LAST:event_recordButtonActionPerformed
       
       void setJPEGQuality(Player p, float val) {
           Control cs[] = p.getControls();
           QualityControl qc = null;
           VideoFormat jpegFmt = new VideoFormat(VideoFormat.JPEG);
           
           // Loop through the controls to find the Quality control for
           // the JPEG encoder.
           for (int i = 0; i < cs.length; i++) {
               if (cs[i] instanceof QualityControl && cs[i] instanceof Owned) {
                   Object owner = ((Owned)cs[i]).getOwner();
                   // Check to see if the owner is a Codec.
                   // Then check for the output format.
                   if (owner instanceof Codec) {
                       Format fmts[] = ((Codec)owner).getSupportedOutputFormats(null);
                       for (int j = 0; j < fmts.length; j++) {
                           if (fmts[j].matches(jpegFmt)) {
                               qc = (QualityControl)cs[i];
                               qc.setQuality(val);
                               break;
                           }
                       }
                   }
                   if (qc != null) break;
               }
           }
       }
       
       public void checkIncoding(TrackControl track[]){
           for (int i = 0; i < track.length; i++) {
               Format format = track[i].getFormat();
               if (track[i].isEnabled() && format instanceof VideoFormat) {
                   Dimension size = ((VideoFormat)format).getSize();
                   float frameRate = ((VideoFormat)format).getFrameRate();
                   int w = (size.width % 8 == 0 ? size.width :(int)(size.width / 8) * 8);
                   int h = (size.height % 8 == 0 ? size.height :(int)(size.height / 8) * 8);
                   VideoFormat jpegFormat = new VideoFormat(
                      VideoFormat.JPEG_RTP, new Dimension(w, h), Format.NOT_SPECIFIED, Format.byteArray, frameRate);
                   messageLabel.setText("Status: Video transmitted as: " + jpegFormat.toString());
               }
           }
       }
       
       public void recordToFile(File file){
           URL movieUrl = null;
           MediaLocator dest = null;
           try{
               movieUrl = file.toURL();
               dest = new MediaLocator(movieUrl);
           }catch(MalformedURLException e){
               
           }
           
           recordCamSource = dataSource.cloneCamSource();
           try{
               recordProcessor = Manager.createProcessor(recordCamSource);
           }catch (IOException e) {
               JOptionPane.showMessageDialog(this, 
                  "Exception creating record processor: " + e.getMessage(), "Error", JOptionPane.WARNING_MESSAGE);
               return;
           }catch (NoProcessorException e) {
               JOptionPane.showMessageDialog(this, 
                  "Exception creating record processor: " + e.getMessage(), "Error", JOptionPane.WARNING_MESSAGE);
               return;
           }
           playhelper = new camStateHelper(recordProcessor);
           if(!playhelper.configure(10000)){
               JOptionPane.showMessageDialog(this, 
                  "cannot configure record processor", "Error", JOptionPane.WARNING_MESSAGE);
               return;
           }
           
           VideoFormat vfmt = new VideoFormat(VideoFormat.CINEPAK);
           (recordProcessor.getTrackControls())[0].setFormat(vfmt);
           (recordProcessor.getTrackControls())[0].setEnabled(true);
           recordProcessor.setContentDescriptor(new FileTypeDescriptor(FileTypeDescriptor.QUICKTIME));
           Control control = recordProcessor.getControl("javax.media.control.FrameRateControl");
           if ( control != null && control instanceof javax.media.control.FrameRateControl )
              ((javax.media.control.FrameRateControl)control).setFrameRate(15.0f);
           if(!playhelper.realize(10000)){
               JOptionPane.showMessageDialog(this, 
                  "cannot realize processor", "Error", JOptionPane.WARNING_MESSAGE);
               return;
           }
           
           try {
               if(recordProcessor.getDataOutput()==null){
                   JOptionPane.showMessageDialog(this, 
                      "No Data Output", "Error", JOptionPane.WARNING_MESSAGE);
                   return;
               }
               dataSink = Manager.createDataSink(recordProcessor.getDataOutput(), dest);
               recordProcessor.start();
               dataSink.open();
               dataSink.start();
           } catch (NoDataSinkException ex) {
               JOptionPane.showMessageDialog(this, 
                  "No DataSink " + ex.getMessage(), "Error", JOptionPane.WARNING_MESSAGE);
           } catch (IOException ex) {
               JOptionPane.showMessageDialog(this, 
                  "IOException " + ex.getMessage(), "Error", JOptionPane.WARNING_MESSAGE);
           }
       }
       
       public void stopRecording(){
           try {
               recordProcessor.close();
               dataSink.stop();
               dataSink.close();
           } catch (IOException e) {
               JOptionPane.showMessageDialog(this, 
                  "cannot stop recording " + e.getMessage(), "Error", JOptionPane.WARNING_MESSAGE);
           }
       }
       
       // Variables declaration - do not modify//GEN-BEGIN:variables
       private javax.swing.JPanel centerPanel;
       private javax.swing.JLabel fileLabel;
       private javax.swing.JToolBar mainToolBar;
       private javax.swing.JLabel messageLabel;
       private javax.swing.JPanel northPanel;
       private javax.swing.JButton recordButton;
       private javax.swing.JPanel southPanel;
       // End of variables declaration//GEN-END:variables
   }