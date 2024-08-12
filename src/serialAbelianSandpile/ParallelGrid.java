package serialAbelianSandpile;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ForkJoinPool;
import java.lang.Boolean;
import java.util.concurrent.RecursiveTask;

public class ParallelGrid{
	private int rows, columns;
	private int [][] parallelGrid;
	private int [][] updateGrid;

	public ParallelGrid(int w, int h){
		rows = w+2;
		columns = h+2;
		parallelGrid = new int[this.rows][this.columns];
		updateGrid = new int[this.rows][this.columns];

		for(int i=0; i<this.rows;i++){
			for(int j=0; j<this.columns;j++){
				parallelGrid[i][j] = 0;
			}
		}
	}

	public ParallelGrid(int[][] newParallelGrid){
		this(newParallelGrid.length,newParallelGrid[0].length);
		for(int i=1; i<rows; i++){
			for(int j=1; j<columns; j++){
				this.parallelGrid[i][j] = newParallelGrid[i-1][j-1];
			}
		}
	}

	public ParallelGrid(ParallelGrid copyParallelGrid){
		this(copyParallelGrid.rows, copyParallelGrid.columns);
		for(int i=0; i<rows; i++){
			for(int j=0; j<columns; j++){
				this.parallelGrid[i][j]=copyParallelGrid.get(i,j);
			}
		}
	}

	public int getRows(){
		return rows-2;
	}

	public int getColumns(){
		return columns-2;
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

	Boolean update(){
		ParallelGridUpdateTask task = new ParallelGridUpdateTask(parallelGrid, 1, rows-1);
		boolean change = ForkJoinPool.commonPool().invoke(task);
		if(change){
			nextTimeStep();
		}
		return change;
	}

	public class ParallelGridUpdateTask extends RecursiveTask<Boolean>{
		private int startRow;
		private int endRow;
		private int [][] parallelGrid;
		private final int CUTOFF = (rows*columns)/Runtime.getRuntime().availableProcessors();

		public ParallelGridUpdateTask(int [][] parallelGrid, int startRow, int endRow){
			this.startRow = startRow;
			this.endRow = endRow;
			this.parallelGrid = parallelGrid;
		}

		@Override
		protected Boolean compute(){
			if((endRow-startRow) <= CUTOFF){
				boolean change = false;
				for(int i=startRow; i<endRow; i++){
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
				}
				return change;
			}
			else{
				int midRow = (startRow + endRow)/2;

				ParallelGridUpdateTask top = new ParallelGridUpdateTask(parallelGrid, startRow, midRow);
				ParallelGridUpdateTask bottom = new ParallelGridUpdateTask(parallelGrid, midRow, endRow);

				top.fork();
				boolean resultA = bottom.compute();
				boolean resultB = top.join();
				return resultA||resultB;
			}
		}


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