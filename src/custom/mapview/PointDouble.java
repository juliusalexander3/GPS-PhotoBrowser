package custom.mapview;

public class PointDouble{
	
	private double x;
	private double y;

	/**
	 * Creates a Point with double representation with the passed coordinates
	 * @param x is double
	 * @param y is double
	 */
	public PointDouble(double x, double y)
	{
		this.x = x;
		this.y = y;
	}
	/**
	 * Creates a Point with double representation with x=0 and y=0
	 * @param x is double
	 * @param y is double
	 */
	public PointDouble()
	{
		this.x=0;
		this.y=0;
	}

	@Override
	/**
	 * Returns String Representation of double point "x,y"
	 */
	public String toString()
	{
		return "(" + Double.toString(x) + "," + Double.toString(y) + ")";
	}
	/**
	 * Return the X value of the PointDouble
	 * @return double x
	 */
	public double getX(){
		return this.x;
	}
	/**
	 * Set the Value of X
	 * @param X
	 */
	public void setX(double X){
		this.x = X;
	}
	
	/**
	 * Return the Y value of the PointDouble
	 * @return double y
	 */
	public double getY(){
		return this.y;
	}
	/**
	 * Set the value of Y
	 * @param Y
	 */
	public void setY(double Y){
		this.y = Y;
	}
	
}