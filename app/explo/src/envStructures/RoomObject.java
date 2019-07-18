package envStructures;
/**
 * 
 * @author Clara Rigaud 
 * @author Asma BRAZI
 *  
 */
public class RoomObject{
	protected int id;
	protected Point cornerLeft;
	protected Point cornerRight;
	float height;
	protected float width;

	/**
	 * 
	 * @param id unique identity of the room-object
	 * @param cornerLeft coordinates of the left corner of the room-object
	 * @param cornerRight coordinates of the right corner of the room-object
	 * @param height the height of the wall in 0.01*centimeter
	 * @param width the width of the wall in 0.01* centimeter
	 */
	public RoomObject(int id, Point cornerLeft, Point cornerRight, float height, float width) {
		this.id = id;
		this.cornerLeft = cornerLeft;
		this.cornerRight = cornerRight;
		this.height = height;
		this.width = width;
	}
	/**
	 * Modify the width of the room-object
	 * @return float width in centimeter
	 */
	public void updateWidth() {
		this.width=(cornerLeft.getPosition().sub(cornerRight.getPosition())).length();
	}
	/**
	 * Retrieve the width of the room-object
	 * @return float width in centimeter
	 */
	public float getWidth(){
		//return (cornerLeft.position().sub(cornerRight.position())).length();
		return this.width;
	}
	/**
	 * Retrieve the coordinate of the left corner
	 * @return the coordinate of the left corner
	 */
	public Point getCornerLeft() {
		return cornerLeft;
	}
	/**
	 * Modify the coordinate of the left corner
	 */
	public void setCornerLeft(Point cornerLeft) {
		this.cornerLeft = cornerLeft;
	}
	/**
	 * Retrieve the coordinate of the right corner
	 * @return the coordinate of the right corner
	 */
	public Point getCornerRight() {
		return cornerRight;
	}
	/**
	 * Modify the coordinate of the right corner
	 */
	public void setCornerRight(Point cornerRight) {
		this.cornerRight = cornerRight;
	}
	/**
	 * Modify the width if the room-object
	 */
	public void setWidth(float width) {
		this.width = width;
	}
	/**
	 * Gives the height of the object in centimeter
	 * @return float height in centimeter
	 */
	public float getHeight(){
		return this.height;
	}
	/**
	 * Modify the height of the wall only if the new height is bigger than the old one
	 * @param height2 the new height
	 */
	public void updateHeight(float height2){
		if(height2>this.height) {
			this.height=height2;
		}

	}
	/**
	 * Modify the height of the wall
	 * @param height2 the new height
	 */
	public void setHeight(float height2){
		this.height=height2;		
	}
	/**
	 * Gives the rotation of the wall according to the x vector
	 * @return (float) angle in radians between 0.0 and 2*PI
	 */
	public float getRotation(){
		return Vec2.angle(cornerLeft.getPosition().sub(cornerRight.getPosition()), new Vec2(1,0));
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
		return (cornerLeft.getPosition().x+cornerRight.getPosition().x)/2;
	}

	/**
	 * Gives the Y position of the center of the wall
	 * @return float position Y
	 */
	public float posY() {
		return (cornerLeft.getPosition().y+cornerRight.getPosition().y)/2;
	}
	@Override
	public String toString() {
		return "RoomObject [id=" + id + ", cornerLeft=" + cornerLeft + ", cornerRight=" + cornerRight + ", height="
				+ height + ", width=" + width + "]";
	}
}