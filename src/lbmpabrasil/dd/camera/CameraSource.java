package lbmpabrasil.dd.camera;

import java.util.stream.DoubleStream;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

public class CameraSource extends Thread {
	private boolean error = false;
	private VideoCapture vc;	
	private boolean boot = true;	
	private Mat frame = new Mat();
	private Mat previousFrame = new Mat();	
	private Point direction = new Point(0, 0);	
	private final int resX = 192;
	private final int resY = 192;
	private final int resCenterFrameX = 72;
	private final int resCenterFrameY = 72;	
	private long time = 0L;
	
	public synchronized long getTime() {
		return this.time;
	}
		
	public synchronized Point getDirection() {
		return direction;
	}
	
	public boolean isBooting() {
		return this.boot;
	}
		
	public final boolean hasError() {
		return this.error;
	}
	
	public void run() {		
		this.vc = new VideoCapture(0);
		
		int errorCounter = 0;
		for(;;) {
			if(this.vc.isOpened()) break;
			
			if(errorCounter++ >= 3) {
				this.error = true;
				
				break;
			}
			
			try { Thread.sleep(250L); } catch(InterruptedException e) { e.printStackTrace(); }
		}
		
		if(!this.error) {			
			for(;;) {
				long tempTime = System.currentTimeMillis();
				
				vc.read(this.frame);				
				Imgproc.resize(this.frame, this.frame, new Size(resX, resY));
				
				if(boot) {
					this.frame.copyTo(this.previousFrame);
					
					boot = !boot;
				}					
				
				/*
				 *  OBTENHO UM FRAME CENTRALIZADO DA IMAGEM CAPTURADA.
				 *  USAREI ESSE FRAME PARA VARRER A IMAGEM E OBTER AS MATRIZES DE DIFERENÇA. 
				 *  A IDEIA AQUI É PROCURAR A MENOR DIFERENÇA ABSOLUTA. 
				 */
				Rect rec = new Rect(resX / 2, resY / 2, resCenterFrameX, resCenterFrameY);
				Mat centerFrame = new Mat(frame, rec);
				
				double[][] frameM = new double[resY][resX];
				double[][] previousFrameM = new double[resY][resX];				
				for(int i = 0; i < resY; i++) 
					for(int j = 0; j < resX; j++) {
						frameM[i][j] = DoubleStream.of(frame.get(i, j)).average().getAsDouble();
						previousFrameM[i][j] = DoubleStream.of(previousFrame.get(i, j)).average().getAsDouble();
					}
				
				double[][] centerFrameM = new double[resY][resX];
				for(int i = 0; i < resCenterFrameY; i++) 
					for(int j = 0; j < resCenterFrameX; j++) 
						centerFrameM[i][j] = DoubleStream.of(centerFrame.get(i, j)).average().getAsDouble();
				
				Point minPosition = new Point();
				double minPower = Double.MAX_VALUE;
				
				for(int i1 = 0; i1 < resY - resCenterFrameY; i1++) {
					for(int j1 = 0; j1 < resX - resCenterFrameX; j1++) {						
						double power = 0.0d;						
						for(int i2 = 0; i2 < resCenterFrameY; i2++)
							for(int j2 = 0; j2 < resCenterFrameX; j2++)
								power += Math.abs(previousFrameM[i1 + i2][j1 + j2] - centerFrameM[i2][j2]);

						if(power < minPower) {
							minPower = power;
							
							minPosition.x = j1;
							minPosition.y = i1;
						}
					}
				}				
				
				this.frame.copyTo(this.previousFrame);
				
				synchronized(this) {
					this.direction.x = minPosition.x - resX / 2;
					this.direction.y = minPosition.y - resY / 2;
					
					this.time = System.currentTimeMillis() - tempTime;					
				}
				
				try { Thread.sleep(1L); } catch(InterruptedException e) { e.printStackTrace(); }
			}
		}
	}
}