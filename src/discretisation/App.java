package discretisation;


import java.awt.Color;
import java.awt.Point;
import java.awt.geom.PathIterator;

import processing.core.PApplet;



public class App extends PApplet {

    private static Discretisation discretisation;
    SqGridCell sqGridCell;
    HexGridCell hexGridCell;
    
    int cellDiameter=50;
    
    private static int BOARD_WIDTH = 5;
    private static int BOARD_HEIGHT = 4;

	public static void main(String[] args) {
		PApplet.main(new String[]{"discretisation.App"});
	}
	
	public void setup() {
		size(600,600);
	    BOARD_WIDTH = width/cellDiameter;
	    BOARD_HEIGHT = height/cellDiameter;
		sqGridCell = new SqGridCell(cellDiameter);
		hexGridCell = new HexGridCell(cellDiameter);
		discretisation=sqGridCell;
	}
	
	public void draw() {
		Point mouseoverPt=discretisation.getColRow(mouseX,mouseY);
		
		textSize(12);
		textAlign(CENTER,CENTER);
		background(255);
		stroke(0);
		for (int col = 0; col < BOARD_WIDTH; col++) {
			for (int row = 0; row < BOARD_HEIGHT; row++) {
        		fill(200);
        		Point pt=discretisation.getXYCentre(col, row);
        		text(col+","+row,pt.x,pt.y);
        		if (col==mouseoverPt.x && row==mouseoverPt.y)
        			fill(200,0,0);
        		else
        			noFill();
        		beginShape();
        		float[] coord=new float[6];
        		PathIterator pi=discretisation.getShape(col, row).getPathIterator(null);
        		while (!pi.isDone()) {
        			pi.currentSegment(coord);
        			vertex(coord[0],coord[1]);
        			pi.next();
        		}
        		endShape(CLOSE);
        	}
        }
	}
	
	public void keyPressed() {
		if (key=='s')
			discretisation = sqGridCell;
		if (key=='h')
			discretisation = hexGridCell;
		
		if (key==CODED && keyCode==UP) {
			cellDiameter-=1;
			if (cellDiameter<5)
				cellDiameter=5;
			sqGridCell.changeDiameter(cellDiameter);
			hexGridCell.changeDiameter(cellDiameter);
		}
		if (key==CODED && keyCode==DOWN) {
			cellDiameter+=1;
			sqGridCell.changeDiameter(cellDiameter);
			hexGridCell.changeDiameter(cellDiameter);
		}

	}

}
