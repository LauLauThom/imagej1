package ij.plugin;
import ij.*;
import ij.process.*;
import ij.gui.*;

/** Implements the Plugins/Utilities/Run Benchmark command.
 * Suppresses subordinate status bar messages by using
 * IJ.showStatus("!"+"rest of messager") and displays
 * subordinate progress bars as dots by using
 * IJ.showProgress(-currentIndex,finalIndex).
*/
public class Benchmark implements PlugIn {
    int size = 5000;
    int ops = 62;
    int counter;

    public void run(String arg) {
    	ImagePlus cImp = WindowManager.getCurrentImage();
    	if (cImp!=null && cImp.getWidth()==512 && cImp.getHeight()==512 && cImp.getBitDepth()==24) {
			IJ.runPlugIn(cImp, "ij.plugin.filter.Benchmark", "");
    		return;
    	}
        IJ.showStatus("Creating "+size+"x"+size+" 16-bit image");
        long t0 = System.currentTimeMillis();
        ImageProcessor.setRandomSeed(12345);
        ImagePlus imp = IJ.createImage("Untitled", "16-bit noise", size, size, 1);
        ImageProcessor.setRandomSeed(Double.NaN);
        imp.copy();                
        for (int i=0; i<3; i++)
            analyzeParticles(imp);
        for (int i=0; i<3; i++) {
            IJ.run(imp, "Median...", "radius=2");
            showProgress("Median");
        }
        for (int i=0; i<12; i++) {
            IJ.run(imp, "Unsharp Mask...", "radius=1 mask=0.60");
            showProgress("Unsharp Mask");
        }
        ImageProcessor ip = imp.getProcessor();
        ip.snapshot();
        for (int i=0; i<12; i++) {
            ip.blurGaussian(40);
            showProgress("Gaussian blur");
        }
        ip.reset();
        for (int i=0; i<360; i+=20) {
            ip.reset();
            ip.rotate(i);
            showProgress("Rotate");
        }
        double scale = 1.2;
        for (int i=0; i<14; i++) {
            ip.reset();
            ip.scale(scale, scale);
            showProgress("Scale");
            scale = scale*1.2;
        }
        double time = (System.currentTimeMillis()-t0)/1000.0;
        IJ.showStatus("!"+IJ.d2s(time,1)+" seconds to perform "+counter+" operations on a "+size+"x"+size+" 16-bit image");
    }
    
    void analyzeParticles(ImagePlus imp) {
        showProgress("Particle analyzer");
        imp.paste();
        IJ.setAutoThreshold(imp, "Default");
        IJ.run(imp, "Gaussian Blur...", "sigma=10");
        IJ.setAutoThreshold(imp, "Default");
        IJ.run(imp, "Analyze Particles...", "clear overlay composite");
        Overlay overlay = imp.getOverlay();
        int n = overlay.size();
        double sumArea = 0;
        double sumMean = 0;
        for (int i=0; i<n; i++) {
            imp.setRoi(overlay.get(i));
            ImageStatistics stats = imp.getStatistics();
            sumArea += stats.area;
            sumMean += stats.mean;
        }
        imp.resetRoi();
        if (counter==1 && (n!=1886||sumArea/n!=5843.324496288441||sumMean/n!=32637.72733693335)) {
            IJ.log(n+" "+sumArea/n+" "+sumMean/n);
            error("Particle analyzer");
        }
    }
    
    void showProgress(String msg) {
        counter++;
        msg = msg.length()>1?" ("+msg+")":"";
        IJ.showStatus("!"+counter + "/"+ops+msg);
        IJ.showProgress(-counter, ops);
    }

    void error(String msg) {
        IJ.log("Benchmark: "+msg+" regression");
    }
}


