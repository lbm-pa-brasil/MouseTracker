package lbmpabrasil.dd.app;

import javax.swing.SwingUtilities;

import org.opencv.core.Core;

import lbmpabrasil.dd.camera.CameraSource;

public class App {    
	public void wakeUp() {
		CameraSource cs = new CameraSource() {{ start(); }};
		
		long waitTime = 0L;
		
		for(;;) {
			if(cs.hasError())
				cs = new CameraSource();
				
			if(!cs.isBooting()) {
				System.out.println("(" + cs.getDirection().x + ", " + cs.getDirection().y + ")");
				waitTime = cs.getTime() + 2L;				
			}
			
			try { Thread.sleep(waitTime); } catch (InterruptedException e) { e.printStackTrace(); }
		}
	}
	
	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
	    SwingUtilities.invokeLater(new Runnable() {
	        public void run() {
	        	new App() {{ wakeUp(); }};
	        }
	    });
	}
}