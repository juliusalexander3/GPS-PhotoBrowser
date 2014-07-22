package custom.mapview;

public class RectDouble {
	
	private double left;
	private double right;
	private double top;
	private double bottom;
	
	public RectDouble(double left, double top, double right, double bottom){
		this.left = left;
		this.right = right;
		this.top =top;
		this.bottom = bottom;
	}

	public double getLeft() {
		return left;
	}

	public void setLeft(double left) {
		this.left = left;
	}

	public double getRight() {
		return right;
	}

	public void setRight(double right) {
		this.right = right;
	}

	public double getTop() {
		return top;
	}

	public void setTop(double top) {
		this.top = top;
	}

	public double getBottom() {
		return bottom;
	}

	public void setBottom(double bottom) {
		this.bottom = bottom;
	}
	

}
