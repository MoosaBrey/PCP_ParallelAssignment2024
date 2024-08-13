package parallelAbelianSandpile;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ForkJoinPool;
import java.lang.Boolean;
import java.util.concurrent.RecursiveTask;

//This class is for the grid for the Abelian Sandpile cellular automaton
public class ParallelGrid{
	private int rows, columns;
	private int [][] parallelGrid;//grid
	private int [][] updateGrid;//grid for next time step

	public ParallelGrid(int w, int h){
		rows = w+2;//for the "sink" border
		columns = h+2;//for the "sink" border
		parallelGrid = new int[this.rows][this.columns];
		updateGrid = new int[this.rows][this.columns];
		/* grid  initialization */
		for(int i=0; i<this.rows;i++){
			for(int j=0; j<this.columns;j++){
				parallelGrid[i][j] = 0;
				updateGrid[i][j] = 0;
			}
		}
	}

	public ParallelGrid(int[][] newParallelGrid){
		this(newParallelGrid.length,newParallelGrid[0].length); //call constructor above
		//don't copy over sink border
		for(int i=1; i<rows-1; i++){
			for(int j=1; j<columns-1; j++){
				this.parallelGrid[i][j] = newParallelGrid[i-1][j-1];
			}
		}
	}

	public ParallelGrid(ParallelGrid copyParallelGrid){
		this(copyParallelGrid.rows, copyParallelGrid.columns);//call constructor above
		/* grid  initialization */
		for(int i=0; i<rows; i++){
			for(int j=0; j<columns; j++){
				this.parallelGrid[i][j]=copyParallelGrid.get(i,j);
			}
		}
	}

	public int getRows(){
		return rows-2;//less the sink
	}

	public int getColumns(){
		return columns-2;//less the sink
	}

	int get(int i, int j) {
		return this.parallelGrid[i][j];
	}

	void setAll(int value) {
		//borders are always 0
		for( int i = 1; i<rows-1; i++ ) {
			for( int j = 1; j<columns-1; j++ ) 			
				parallelGrid[i][j]=value;
			}
	}
	

	//for the next timestep - copy updateGrid into grid
	public void nextTimeStep() {
		for(int i=1; i<rows-1; i++ ) {
			for( int j=1; j<columns-1; j++ ) {
				this.parallelGrid[i][j]=updateGrid[i][j];
			}
		}
	}
	// Inner class that handles the creation of new threads for update method.
	public class ParallelGridUpdate extends RecursiveTask<Boolean>{
		private int start;//starting row
		private int end;//end row
		private int [][] parallelGrid;
		private final int CUTOFF = (rows*columns)/Runtime.getRuntime().availableProcessors();//sequential cutoff variable for computer

		public ParallelGridUpdate(int[][] parallelGrid, int start, int end){
			this.start = start;
			this.end = end;
			this.parallelGrid = parallelGrid;
		}

		//compute method to assign a grid to threads
		@Override
		protected Boolean compute(){
			if((end-start)*columns <= CUTOFF){
				boolean change = false;
				//do not update border
				for(int i=start; i<end; i++){
					for(int j=1; j<columns-1; j++){
						updateGrid[i][j] = (parallelGrid[i][j] % 4) + 
										(parallelGrid[i-1][j] / 4) +
										parallelGrid[i+1][j] / 4 +
										parallelGrid[i][j-1] / 4 + 
										parallelGrid[i][j+1] / 4;
						if (parallelGrid[i][j]!=updateGrid[i][j]) {  
							change=true; 
						}
					}
				}//end nested for loop
				return change;
			}
			else{
				int mid = (start + end)/2;//middle row
				//threads split graph to top and bottom sections of graph
				ParallelGridUpdate top = new ParallelGridUpdate(parallelGrid, start, mid);//assigning of top section of graph to a thread
				ParallelGridUpdate bottom = new ParallelGridUpdate(parallelGrid, mid, end);//assignm=ing of bottom section of graph to a thread

				top.fork();
				boolean resultA = bottom.compute();
				boolean resultB = top.join();
				return resultA||resultB;//return of combined results after completion
			}
		}


	}

	//key method to calculate the next update grid
	boolean update(){
		ParallelGridUpdate update = new ParallelGridUpdate(parallelGrid, 1, rows-1);
		boolean change = ForkJoinPool.commonPool().invoke(update);
		if(change){
			nextTimeStep();
		}
		return change;
	}

	//display the grid in text format
	void printParallelGrid( ) {
		int i,j;
		//not border is not printed
		System.out.printf("Grid:\n");
		System.out.printf("+");
		for( j=1; j<columns-1; j++ ) System.out.printf("  --");
		System.out.printf("+\n");
		for( i=1; i<rows-1; i++ ) {
			System.out.printf("|");
			for( j=1; j<columns-1; j++ ) {
				if ( parallelGrid[i][j] > 0) 
					System.out.printf("%4d", parallelGrid[i][j] );
				else
					System.out.printf("    ");
			}
			System.out.printf("|\n");
		}
		System.out.printf("+");
		for( j=1; j<columns-1; j++ ) System.out.printf("  --");
		System.out.printf("+\n\n");
	}
	
	//write grid out as an image
	void gridToImage(String fileName) throws IOException {
        BufferedImage dstImage =
                new BufferedImage(rows, columns, BufferedImage.TYPE_INT_ARGB);
        //integer values from 0 to 255.
        int a=0;
        int g=0;//green
        int b=0;//blue
        int r=0;//red

		for( int i=0; i<rows; i++ ) {
			for( int j=0; j<columns; j++ ) {
			     g=0;//green
			     b=0;//blue
			     r=0;//red

				switch (parallelGrid[i][j]) {
					case 0:
		                break;
		            case 1:
		            	g=255;
		                break;
		            case 2:
		                b=255;
		                break;
		            case 3:
		                r = 255;
		                break;
		            default:
		                break;
				
				}
		                // Set destination pixel to mean
		                // Re-assemble destination pixel.
		              int dpixel = (0xff000000)
		                		| (a << 24)
		                        | (r << 16)
		                        | (g<< 8)
		                        | b; 
		              dstImage.setRGB(i, j, dpixel); //write it out

			
			}}
		
        File dstFile = new File(fileName);
        ImageIO.write(dstImage, "png", dstFile);
	}


}