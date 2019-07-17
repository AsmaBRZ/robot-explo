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
	private List<RoomObject> objectsOnWall; // liste des objets places contre le mur de gauche a� droite
	public boolean obstacle = false;
	public double robotRotation;
	/**
	 * 
	 * @param id (int) Identifier of the wall
	 * @param left (Point) left corner of the wall
	 * @param right (Point) right corner of the wall
	 */
	public Wall(int id, Point left, Point right, float  height,float width,double robotRotation){
		super(id, left, right, height, width);
		objectsOnWall=new ArrayList<RoomObject>();
		this.obstacle=false;
		this.robotRotation=robotRotation;
		
	}
	public Wall(int id, Point left, Point right, float  height,float width,double robotRotation,boolean obstacle){
		super(id, left, right, height, width);
		objectsOnWall=new ArrayList<RoomObject>();
		this.obstacle=obstacle;
		this.robotRotation=robotRotation;
		
	}

	public void cumulateWidth(float w) {
		 this.setWidth(this.getWidth()+w);
	}
	public void cumulateCornerRight(Point c) {
		this.cornerRight.setPosition(this.cornerRight.getPosition().add(c.getPosition()));
	}

	public void updateObjectPoints(){
		// constrain objects to stay on the wall
		for(int i = 0;i< objectsOnWall.size(); i++){
			Vec2 oldCl = objectsOnWall.get(i).cornerLeft.position();
			Vec2 oldCr = objectsOnWall.get(i).cornerRight.position();
			Vec2 newoCl = Vec2.projectPointOnLine(oldCl, this.cornerRight.position(), this.cornerLeft.position());
			Vec2 newoCr = Vec2.projectPointOnLine(oldCr, this.cornerRight.position(), this.cornerLeft.position());
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
	@Override
	public String toString() {
		return "Wall [obstacle=" + obstacle + ", id=" + id + ", cornerLeft=" + cornerLeft + ", cornerRight="
				+ cornerRight + ", height=" + height + ", width=" + width + "]";
	}


}
