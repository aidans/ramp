package discretisation;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;

/** http://gdreflections.com/2011/02/hexagonal-grid-math.html by Ruslan Shestopalyuk


/**
 * Uniform hexagonal grid cell's metrics utility class.
 */
public class HexGridCell implements Discretisation {
	private static final int[] NEIGHBORS_DI = { 0, 1, 1, 0, -1, -1 };
	private static final int[][] NEIGHBORS_DJ = { 
			{ -1, -1, 0, 1, 0, -1 }, { -1, 0, 1, 1, 1, 0 } };

	private int[] CORNERS_DX; // array of horizontal offsets of the cell's corners
	private int[] CORNERS_DY; // array of vertical offsets of the cell's corners
	private int SIDE;



	/**
	 * Cell radius (distance from center to one of the corners)
	 */
	public int RADIUS;
	/**
	 * Cell height
	 */
	public int HEIGHT;
	/**
	 * Cell width
	 */
	public int WIDTH;

	public static final int NUM_NEIGHBORS = 6;

	/**
	 * @param radius Cell radius (distance from the center to one of the corners)
	 */
	public HexGridCell(int diameter) {
		changeDiameter(diameter);
	}


	/**
	 * @return X coordinate of the cell's top left corner.
	 */
	@Override
	public Point getXY(int col, int row) {
		return new Point(col * SIDE,HEIGHT * (2 * row + (col % 2)) / 2);
	}

	/**
	 * @return X coordinate of the cell's top left corner.
	 */
	@Override
	public Point getXYCentre(int col, int row) {
		Point xy=getXY(col, row);
		return new Point(xy.x+RADIUS,xy.y + HEIGHT / 2);
	}


	/**
	 * Computes X and Y coordinates for all of the cell's 6 corners, clockwise,
	 * starting from the top left.
	 * 
	 * @param cornersX Array to fill in with X coordinates of the cell's corners
	 * @param cornersX Array to fill in with Y coordinates of the cell's corners
	 */
	@Override
	public Point[] getVertices(int col, int row) {
		Point[] returnValue=new Point[6];
		Point xy=getXY(col,row);
		for (int k = 0; k < NUM_NEIGHBORS; k++)
			returnValue[k]=new Point(xy.x + CORNERS_DX[k],xy.y + CORNERS_DY[k]);
		return returnValue;
	}


	/**
	 * Sets the cell as corresponding to some point inside it (can be used for
	 * e.g. mouse picking).
	 */
	@Override
	public Point getColRow(int x, int y) {
		int ci = (int)Math.floor((float)x/(float)SIDE);
		int cx = x - SIDE*ci;

		int ty = y - (ci % 2) * HEIGHT / 2;
		int cj = (int)Math.floor((float)ty/(float)HEIGHT);
		int cy = ty - HEIGHT*cj;

		if (cx > Math.abs(RADIUS / 2 - RADIUS * cy / HEIGHT)) {
			return new Point(ci, cj);
		} else {
			return new Point(ci - 1, cj + (ci % 2) - ((cy < HEIGHT / 2) ? 1 : 0));
		}
	}

	@Override
	public void changeDiameter(int diameter) {
		RADIUS = (int)(diameter*1.3f/2);
		WIDTH = RADIUS*2;
		HEIGHT = (int) (((float) RADIUS) * Math.sqrt(3));
		SIDE = RADIUS * 3 / 2;

		int cdx[] = { RADIUS / 2, SIDE, WIDTH, SIDE, RADIUS / 2, 0 };
		CORNERS_DX = cdx;
		int cdy[] = { 0, 0, HEIGHT / 2, HEIGHT, HEIGHT, HEIGHT / 2 };
		CORNERS_DY = cdy;

	}


	@Override
	public Shape getShape(int col, int row) {
		Path2D path=new Path2D.Float();
		Point xy=getXY(col,row);
		for (int k = 0; k < NUM_NEIGHBORS; k++) {
			if (k==0)
				path.moveTo(xy.x + CORNERS_DX[k],xy.y + CORNERS_DY[k]);
			else
				path.lineTo(xy.x + CORNERS_DX[k],xy.y + CORNERS_DY[k]);

		}
		path.closePath();
		return path;
	}
}