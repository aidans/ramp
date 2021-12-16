package discretisation;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;

public class SqGridCell implements Discretisation{

	private int DIAMETER;
	
	public SqGridCell(int diameter) {
		DIAMETER=diameter;
	}
	
	@Override
	public Point getXY(int col, int row) {
		return new Point(col*DIAMETER,row*DIAMETER);
	}

	@Override
	public Point getXYCentre(int col, int row) {
		return new Point(col*DIAMETER+DIAMETER/2,row*DIAMETER+DIAMETER/2);
	}

	@Override
	public Point[] getVertices(int col, int row) {
		Point[] returnValue=new Point[4];
		returnValue[0]=new Point(col*DIAMETER,row*DIAMETER);
		returnValue[1]=new Point(col*DIAMETER+DIAMETER,row*DIAMETER);
		returnValue[2]=new Point(col*DIAMETER+DIAMETER,row*DIAMETER+DIAMETER);
		returnValue[3]=new Point(col*DIAMETER,row*DIAMETER+DIAMETER);
		return returnValue;
	}

	@Override
	public Point getColRow(int x, int y) {
		return new Point(((int)x/DIAMETER),((int)y/DIAMETER));
	}

	@Override
	public void changeDiameter(int diameter) {
		this.DIAMETER=diameter;
	}

	@Override
	public Shape getShape(int col, int row) {
		return new Rectangle(col*DIAMETER,row*DIAMETER,DIAMETER,DIAMETER);
	}
}
