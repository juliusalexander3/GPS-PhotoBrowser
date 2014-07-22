package custom.mapview;

import android.graphics.Bitmap;

public class Tile
{
	private int x;		//X origin of Tile
	private int y;		//Y origin of Tile
	private int zoom;	//Zoom level of this Tile
	private Bitmap img;	//Bitmap that corresponds to this Tile

	public Tile(int x, int y, int zoom, Bitmap img)
	{
		this.x = x;
		this.y = y;
		this.zoom = zoom;
		this.img = img;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}
	
	public int getZoom() {
		return zoom;
	}

	public void setZoom(int Zoom) {
		this.zoom = Zoom;
	}

	public Bitmap getImg() {
		return img;
	}

	public void setImg(Bitmap img) {
		this.img = img;
	}
	

}