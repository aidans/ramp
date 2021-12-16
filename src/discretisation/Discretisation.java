package discretisation;
import java.awt.Point;
import java.awt.Shape;

public interface Discretisation {

	/**
	 * Gets the top left screen coordinate of the cell from its col,row
	 * @param col column
	 * @param row row
	 * @return screen coordinate of the top left corner
	 */
	Point getXY(int col, int row);

	/**
	 * Gets the centre screen coordinate of the cell its col,row
	 * @param col column
	 * @param row row
	 * @return coordinate of the centre 
	 */
	Point getXYCentre(int col, int row);

	/**
	 * Gets the screen coordinates of the shape vertices
	 * 
	 * @param col column
	 * @param row row
	 * @return the screencoordinates of the shape
	 */
	Point[] getVertices(int col, int row);

	/**
	 * Gets the screen shape
	 * 
	 * @param col column
	 * @param row row
	 * @return the screencoordinates of the shape
	 */
	Shape getShape(int col, int row);

	
	/**
	 * Gets the col,row from the screen coordinate
	 * @param x screen x-coordinate
	 * @param y screen y-coordinate
	 * @return col,row
	 */
	Point getColRow(int x, int y);
	
	void changeDiameter(int diameter);

}