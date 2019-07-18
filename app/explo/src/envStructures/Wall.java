package envStructures;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Clara Rigaud
 * @author Asma BRAZI
 *
 */
public class Wall extends RoomObject{
	public boolean obstacle = false;
	public double robotRotation;
	public Vec2 robotPosition;
	private List<RoomObject> objectsOnWall; //contains the objects detected on the wall

	/**
	 * 
	 * @param id unique identity of the wall
	 * @param left coordinates of the left corner of the wall
	 * @param right coordinates of the right corner of the wall
	 * @param height the height of the wall in 0.01*centimeter
	 * @param width the width of the wall in 0.01* centimeter
	 * @param robotRotation the rotation of the robot in rRadian when the wall has been captured
	 * @param robotPosition the position's coordinates of the robot  when the wall has been captured
	 */
	public Wall(int id, Point left, Point right, float  height,float width,double robotRotation,Vec2 robotPosition){
		super(id, left, right, height, width);
		objectsOnWall=new ArrayList<RoomObject>();
		this.obstacle=false;
		this.robotRotation=robotRotation;
		this.robotPosition=robotPosition;

	}
	/**
	 * 
	 * @param id unique identity of the wall
	 * @param left coordinates of the left corner of the wall
	 * @param right coordinates of the right corner of the wall
	 * @param height the height of the wall in 0.01*centimeter
	 * @param width the width of the wall in 0.01* centimeter
	 * @param robotRotation the rotation of the robot in Radian when the wall has been captured
	 * @param robotPosition the position's coordinates of the robot  when the wall has been captured
	 * @param obstacle if the robot has detect the wall with its sensors, the wall is considered as an obstacle, may be for a future verification 
	 */
	public Wall(int id, Point left, Point right, float  height,float width,double robotRotation, Vec2 robotPosition,boolean obstacle){
		super(id, left, right, height, width);
		objectsOnWall=new ArrayList<RoomObject>();
		this.obstacle=obstacle;
		this.robotRotation=robotRotation;
		this.robotPosition=robotPosition;
	}
	/**
	 * Modify the coordinate on the Y-axis
	 * @param y the coordinate of the corner of the Y-axis
	 */
	public void setY(double y) {
		float x=(float) this.cornerRight.getPosition().getX();
		//we assume having the same y for the two corners
		this.cornerLeft.setPosition(x, (float) (y));
		this.cornerRight.setPosition(x, (float) (y));
	}
	@Override
	public String toString() {
		return "Wall "+" id=" + id + ", obstacle=" + obstacle + ", robotRotation=" + robotRotation
				+ ", robotPosition=" + robotPosition + ", cornerLeft=" + cornerLeft + ", cornerRight="
				+ cornerRight + ", height=" + height + ", width=" + width + "]";
	}
	public void updateObjectPoints(){
		// constrain objects to stay on the wall
		for(int i = 0;i< objectsOnWall.size(); i++){
			Vec2 oldCl = objectsOnWall.get(i).cornerLeft.getPosition();
			Vec2 oldCr = objectsOnWall.get(i).cornerRight.getPosition();
			Vec2 newoCl = Vec2.projectPointOnLine(oldCl, this.cornerRight.getPosition(), this.cornerLeft.getPosition());
			Vec2 newoCr = Vec2.projectPointOnLine(oldCr, this.cornerRight.getPosition(), this.cornerLeft.getPosition());
			objectsOnWall.get(i).cornerLeft.setPosition(newoCl);
			objectsOnWall.get(i).cornerRight.setPosition(newoCr);
		}
	}
	/**
	 *  Unused 4 now - stores a RoomObject attached on the wall on the list
	 * @param o Room object to add
	 */
	public void addObject(RoomObject o){
		System.out.println("room in addobject"+o.toString());
		this.objectsOnWall.add(o);
	}

}
