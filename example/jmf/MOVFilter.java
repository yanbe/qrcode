  package jp.sourceforge.qrcode.example.jmf;

   import java.io.File;
   import javax.swing.*;
   import javax.swing.filechooser.*;

   /* ImageFilter.java is a 1.4 example used by FileChooserDemo2.java. */
   public class MOVFilter extends FileFilter {
       public boolean accept(File f) {
           if (f.isDirectory()) {
               return true;
           }

           String extension = MOVFilter.getExtension(f);
           if (extension != null) {
               if (extension.equalsIgnoreCase("MOV")) {
                       return true;
               } else {
                   return false;
               }
           }

           return false;
       }

       //The description of this filter
       public String getDescription() {
           return "QuickTime Movies";
       }
       
       public static String getExtension(File f) {
           String ext = null;
           String s = f.getName();
           int i = s.lastIndexOf('.');

           if (i > 0 &&  i < s.length() - 1) {
               ext = s.substring(i+1).toLowerCase();
           }
           return ext;
       }
   }