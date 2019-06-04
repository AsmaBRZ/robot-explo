package envStructures;
/**
 * 
 * @author Clara Rigaud 
 *  
 */
public class RoomObject{
	protected int id;
	protected Point cornerLeft;
	protected Point cornerRight;
	protected float height,width; // m
	
	
	
	public RoomObject(int id, Point cornerLeft, Point cornerRight, float height, float width) {
		this.id = id;
		this.cornerLeft = cornerLeft;
		this.cornerRight = cornerRight;
		this.height = height;
		this.width = width;
	}
	/**
	 * Gives the width of the object in meters
	 * @return float width in meters
	 */
	public void updateWidth() {
		 this.width=(cornerLeft.position().sub(cornerRight.position())).length();
	}
	public float getWidth(){
		//return (cornerLeft.position().sub(cornerRight.position())).length();
		return this.width;
	}
	public Point getCornerLeft() {
		return cornerLeft;
	}
	public void setCornerLeft(Point cornerLeft) {
		this.cornerLeft = cornerLeft;
	}
	public Point getCornerRight() {
		return cornerRight;
	}
	public void setCornerRight(Point cornerRight) {
		this.cornerRight = cornerRight;
	}
	public void setWidth(float width) {
		this.width = width;
	}
	/**
	 * Gives the height of the object in meters
	 * @return float height in meters
	 */
	public float getHeight(){
		return this.height;
	}
	public void setHeight(float h){
		this.height=h;
	}
	/**
	 * Gives the rotation of the wall according to the x vector
	 * @return (float) angle in radians between 0.0 and 2*PI
	 */
	public float getRotation(){
		return Vec2.angle(cornerLeft.position().sub(cornerRight.position()), new Vec2(1,0));
	}
	
	/**
	 * Gives the id of the object
	 * @return int id
	 */
	public int getId() {
		return this.id;
	}
	/**
	 * Sets the id of the Object
	 */
	public void setId(int newId){
		this.id = newId;
	}
	/**
	 * Gives the X position of the center of the wall
	 * @return float position X
	 */
	public float posX() {
		return (cornerLeft.position().x+cornerRight.position().x)/2;
	}
	
	/**
	 * Gives the Y position of the center of the wall
	 * @return float position Y
	 */
	public float posY() {
		return (cornerLeft.position().y+cornerRight.position().y)/2;
	}
	@Override
	public String toString() {
		return "RoomObject [id=" + id + ", cornerLeft=" + cornerLeft + ", cornerRight=" + cornerRight + ", height="
				+ height + ", width=" + width + "]";
	}
}