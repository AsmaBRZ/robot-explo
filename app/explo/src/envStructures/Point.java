package envStructures;
/**
 * 
 * @author Clara Rigaud 
 * @author Asma
 */
public class Point{
	private Vec2 position;
	
	
	public Point(){
		this.position = new Vec2();
	}
	

	public Point(float x, float y){
		this.position = new Vec2(x,y);
	}
	public Point(Vec2 p){
		this.position = p;
	}
	/**
	 * Modify the coordinates on the X and Y axis
	 * @param x Coordinate on the X-axis
	 * @param y Coordinate on the Y-axis
	 */
	public void setPosition(float x, float y){
		this.position.x = x;
		this.position.y = y;
	}
	/**
	 * Modify the current position by a new one
	 * @param v x and y coordinate
	 */
	public void setPosition(Vec2 v){
		this.position = v;
	}
	/**
	 * Retrieve the x and y coordinate 
	 * @return the x and y coordinate 
	 */
	public Vec2 getPosition(){
		return this.position;
	}
	@Override
	public String toString() {
		return "Point [position=" + position.toString() + "]";
	}

}
