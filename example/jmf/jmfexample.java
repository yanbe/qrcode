package jp.sourceforge.qrcode.example.jmf;

// Original JMF(Java Media Framework) video capture example comes from 
// http://java.about.com/library/weekly/uc_jmfmovie1.htm
// By Mr. Gal Ratner 

// Note that you need JMF runtime to run this application
// http://java.sun.com/products/java-media/jmf/2.1.1/download.html

public class jmfexample {
    
   
    public jmfexample() {
        
        camDataSource dataSource = new camDataSource(null);
        dataSource.setMainSource();
        dataSource.makeDataSourceCloneable();
        dataSource.startProcessing();
        mainFrame frame = new mainFrame(dataSource);
        //frame.setSize(640, 600);
        frame.setSize(400, 400);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        
    }
    
    
    public static void main(String[] args) {
        
        jmfexample jmf = new jmfexample();
        
    }
    
}

