package serialAbelianSandpile;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ParallelAutomatonSimulation extends Thread{
    static final boolean DEBUG=false;//for debugging output, off
	
	static long startTime = 0;
	static long endTime = 0;

    private static void tick(){ //start timing
		startTime = System.currentTimeMillis();
	}
	private static void tock(){ //end timing
		endTime=System.currentTimeMillis(); 
	}

    
}
