package envStructures;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Collections;

import Actionners.Robot;
/**
 * @author Asma BRAZI
 * @author Clara Rigaud
 *
 */
public class InternalRepresentation {
	private ArrayList<Wall> walls;
	private ArrayList<Float> heights;
	private ArrayList<Integer> visitedWalls;
	private ArrayList<Point> markers; // Toutes les creations d'objets se passent ici, et on enregistre chaque marker 
	private ArrayList<RoomObject> wallObjects;
	private Robot robot;
	private int roomType = 1;
	private int currentWall;

	public InternalRepresentation(Robot robot, int RoomType){
		this.walls = new ArrayList<Wall>();
		this.markers = new ArrayList<Point>();
		this.wallObjects= new ArrayList<RoomObject>();
		this.visitedWalls= new ArrayList<Integer>();	
		this.heights=new ArrayList<Float>();
		this.robot = robot;
	}
	/**
	 * Modify the ID of the current wall
	 * @param wallId the ID of the current wall 
	 */
	public void setCurrentWall(int wallId){
		this.currentWall = wallId;
	}
	/**
	 * Retrieve the ID of the wall currently treated
	 * @return
	 */
	public int getCurrentWall(){
		return this.currentWall;
	}
	/**
	 * Retrieve the list of all detected walls
	 * @return list of the detected walls
	 */
	public ArrayList<Wall> getWalls(){
		return this.walls;
	}
	/**
	 * Retrieve the list of objects attached to a wall
	 * @return the list of objects attached to a wall
	 */
	public ArrayList<RoomObject> getWallObjects(){
		return this.wallObjects;
	}
	/**
	 * Retrieve the connected robot 
	 * @return the connected robot
	 */
	public Robot getRobot(){
		return this.robot;
	}
	public ArrayList<Float> getHeights() {
		return heights;
	}
	/**
	 * Add a new wall'slheight detected 
	 * @param f
	 */
	public void addHeight(float f) {
		this.heights.add(f);
	}
	/**
	 * Retrieve the biggest height between all heights of the detected walls
	 * @return the biggest height
	 */
	public float getMaxHeight() {
		return Collections.max(this.heights);

	}
	public void  adjustCordNearestWalls() {
		ArrayList<Wall> newWalls=new ArrayList<Wall>();
		for(int i=0;i<this.walls.size();i++) {
			for(int j=0;j<this.walls.size();j++) {
				Wall w1=this.walls.get(i);
				Wall w2=this.walls.get(j);
				if(w1.id+1==w2.id){
					//comparing two different walls						//the two walls were captured at the same robot's rotation
					double w1_y1=w1.getCornerLeft().getPosition().getY();
					double w1_y2=w1.getCornerRight().getPosition().getY();
					double w1_x1=w1.getCornerLeft().getPosition().getX();
					double w1_x2=w1.getCornerRight().getPosition().getX();

					double w2_y1=w2.getCornerLeft().getPosition().getY();
					double w2_y2=w2.getCornerRight().getPosition().getY();
					double w2_x1=w2.getCornerLeft().getPosition().getX();
					double w2_x2=w2.getCornerRight().getPosition().getX();

					Vec2 w1_translation=w1.robotPosition;
					Vec2 w2_translation=w2.robotPosition;
					double w1_rotation=w1.robotRotation;
					double w2_rotation=w2.robotRotation;

					// if the two segments intersect, fixing the first wall according to the second

					if(Math.abs(w1_rotation-w2_rotation)==Math.PI/2) {
						System.out.println("2 walls 90");
						if(Line2D.linesIntersect(w1_x1, w1_y1,
								w1_x2, w1_y2,w2_x1, w2_y1,
								w2_x2, w2_y2)){
							System.out.println("Intersection");
							System.out.println("");
							System.out.println("Wall 1 is "+w1);
							System.out.println("Wall 2 is "+w1);
							System.out.println("");
							if(w1_rotation==0 ||w1_rotation==2*Math.PI || w1_rotation==-2*Math.PI) {
								System.out.println("1");
								System.out.println("");
								if(w1_x1<w1_x2) {

									this.walls.get(i).getCornerRight().getPosition().setX((float) w2_x1);
								}
								else {
									this.walls.get(i).getCornerLeft().getPosition().setX((float) w2_x1);
								}
							}
							if(w1_rotation==Math.PI/2 ||w1_rotation==-3/4*Math.PI) {
								System.out.println("2");
								System.out.println("");
								if(w1_y1<w1_y2) {
									this.walls.get(i).getCornerRight().getPosition().setY((float) w2_y1);
								}
								else {
									this.walls.get(i).getCornerLeft().getPosition().setX((float) w2_y1);
								}
							}
							if(w1_rotation==-Math.PI ||w1_rotation==-Math.PI) {
								System.out.println("3");
								System.out.println("");
								if(w1_x1<w1_x2) {
									this.walls.get(i).getCornerLeft().getPosition().setY((float) w2_x1);
								}
								else {
									this.walls.get(i).getCornerRight().getPosition().setX((float) w2_x1);
								}
							}
							if(w1_rotation==-Math.PI/2 ||w1_rotation==3/4*Math.PI) {
								System.out.println("4");
								System.out.println("");
								if(w1_y1<w1_y2) {
									this.walls.get(i).getCornerLeft().getPosition().setY((float) w2_y1);
								}
								else {
									this.walls.get(i).getCornerRight().getPosition().setX((float) w2_y1);
								}
							}
						}

					}
				}
			}
		}
	}

	/**
	 * Add a new wall to the list of captured walls
	 * @param id unique identity of the wall
	 * @param left coordinates of the left corner of the wall
	 * @param right coordinates of the right corner of the wall
	 * @param height the height of the wall in 0.01*centimeter
	 * @param width the width of the wall in 0.01* centimeter
	 * @param robotRotation the rotation of the robot in Radian when the wall has been captured
	 * @param robotPosition the position's coordinates of the robot  when the wall has been captured
	 */
	public void addWall(int id,Point p1,Point p2,float height,float width,double robotRotation, Vec2 robotPosition){

		this.markers.add(p1);
		this.walls.add(new Wall(id,p1,p2,height,width,robotRotation,robotPosition));
	}
	/**
	 * Add a new wall to the list of captured walls
	 * @param id unique identity of the wall
	 * @param left coordinates of the left corner of the wall
	 * @param right coordinates of the right corner of the wall
	 * @param height the height of the wall in 0.01*centimeter
	 * @param width the width of the wall in 0.01* centimeter
	 * @param robotRotation the rotation of the robot in Radian when the wall has been captured
	 * @param robotPosition the position's coordinates of the robot  when the wall has been captured
	 * @param obstacle if the robot has detect the wall with its sensors, the wall is considered as an obstacle, may be for a future verification 
	 */
	public void addWall(int id,Point p1,Point p2,float height,float width,double robotRotation, Vec2 robotPosition,boolean obstacle){

		this.markers.add(p1);
		this.walls.add(new Wall(id,p1,p2,height,width,robotRotation,robotPosition,obstacle));
	}
	public void attachObj(int wall, int idObj,Vec2 cLeft, Vec2 cRight, float width, float height){
		//an object is defined by its two points cleft & cright
		Point cornerLeft = new Point(cLeft);
		Point cornerRight = new Point(cRight);
		this.markers.add(cornerLeft);
		this.markers.add(cornerRight);
		RoomObject objDetected=new RoomObject(idObj, cornerLeft, cornerRight, height, width);
		this.wallObjects.add(objDetected);
		System.out.println("wall to get "+this.getWall(wall).toString());
		this.getWall(wall).addObject(objDetected);

		computeNewPoints(wall, cLeft,cRight);
	}

	/**
	 *  Depending on the type of room, updates the new points of the walls thanks to the new object position
	 */
	private void computeNewPoints(int wall, Vec2 cl, Vec2 cr){ // different strategies depending on the room type 
		switch(roomType){
		case 1: // if this is a 4 perpendicular walls room we can assume that every wall is perpendicular to his neighbor
			int aWallCl = this.markers.indexOf(this.getWall(wall).cornerLeft);
			int aWallCr = this.markers.indexOf(this.getWall(wall).cornerRight);
			Vec2 newCl = Vec2.projectPointOnLine(this.markers.get(aWallCl).getPosition(), cl, cr); // Pour l'instant c'est juste une projection orthogonale du coup ca marchera que pour le cas ou la piece est parfaitement rectengulaire 
			Vec2 newCr = Vec2.projectPointOnLine(this.markers.get(aWallCr).getPosition(), cl, cr);
			this.markers.get(aWallCl).setPosition(newCl);   // On met a jour les points du mur en fonction de ces nouveaux points -> deux nouvelles contraintes 
			this.markers.get(aWallCr).setPosition(newCr);
			// on met egalement à jour les points opposés vu qu'on sait qu'il y a des contraintes d'orthogonalité
			int oppositeWall = (wall+2)% 4;
			int oWallCl = this.markers.indexOf(this.getWall(oppositeWall).cornerLeft);
			int oWallCr = this.markers.indexOf(this.getWall(oppositeWall).cornerRight);
			Vec2 newoCl = Vec2.projectPointOnLine(this.markers.get(oWallCl).getPosition(), newCr, newCr.sub(newCl).normale().add(newCr));

			Vec2 newoCr = Vec2.projectPointOnLine(this.markers.get(oWallCr).getPosition(), newoCl, newoCl.sub(newCr).normale().add(newoCl));
			newoCr = Vec2.projectPointOnLine(newoCr, newCl, newCl.sub(newCr).normale().add(newCl));

			this.markers.get(oWallCl).setPosition(newoCl);   // On met a jour les points du mur en fonction de ces nouveaux points -> deux nouvelles contraintes 
			this.markers.get(oWallCr).setPosition(newoCr);
			walls.forEach((w)-> w.updateObjectPoints()); // mise à jour des coordonnées des objets sur les murs

			break;
		}
	}

	/**
	 * Not used (not sure if is ok) 
	 * @param heightObj
	 * @param heightImage
	 * @param idObj
	 * @return
	 */
	public double getDistance (double heightObj, double heightImage, int idObj) {
		double realHeight=2000, sensorHeight=3.6 ; //mm
		double focale=3.6; // source ?
		return (focale*realHeight*heightImage)/(heightObj*sensorHeight) ;
		//mm
	}

	/**
	 * Computes the Vec2 of the real position of the lower point of a vertical segment on a picture
	 * @param h Higher point of the segment
	 * @param b Lower point of the segment 
	 * @param realHeight (meters)
	 * @return Vec2 position of the point 
	 */
	public Vec2 getPosition(double[] h, double[] b , float realHeight){
		double height =  b[1] - h[1]; // pixels
		//Convert in millimeter
		height = height * robot.sensorHeight / robot.resolutionHeight;
		double fl = this.robot.getFocalLength();
		double d = fl * realHeight*1000 / height; // distance en mm

		System.out.println("Je suis à "+d/10+" cm de la porte");
		Vec2 Rd = robot.getPointer();
		Vec2 Rp = robot.getPosition();
		double dIm = (b[0]-robot.resolutionWidth/2)* robot.sensorWidth / robot.resolutionWidth;
		double dX = (dIm*d)/Math.sqrt(Math.pow(dIm,2)+Math.pow(fl, 2));

		Vec2 A = Rd.mul((float)(Math.sqrt(Math.pow(d,2) + Math.pow(dX, 2))));
		Vec2 B = Rd.normale().normalize().mul((float) dX);
		Vec2 P = A.add(B).add(Rp);
		P.x = P.x/1000;
		P.y =  P.y/1000;
		System.out.println("New Pos :"+ P.x + " " + P.y);
		return P;
	}
	/**
	 * Retrieve the object Wall from its ID
	 * @param wallId the ID of the wall
	 * @return the object Wall
	 */
	public Wall getWall(int wallId){
		for (int i = 0; i<this.walls.size(); i++){
			if(wallId==this.walls.get(i).id){
				return this.walls.get(i);
			}
		}
		return null;
	}

	/**
	 * Retrieve the wall in front of the robot
	 * @return the wall in front
	 */
	public Wall getFacingWall(){
		Vec2 robotPos = robot.getPosition();

		Wall w = null;
		for (int i = 0; i<this.walls.size(); i++){
			double robotRot = robot.getRotation();
			w = this.walls.get(i);
			float clAngle = Vec2.angle(w.cornerRight.getPosition().sub(robotPos), new Vec2(1,0));
			float crAngle = Vec2.angle(w.cornerLeft.getPosition().sub(robotPos), new Vec2(1,0));
			clAngle = (float) ((clAngle - crAngle+2*Math.PI)%(2*Math.PI));
			robotRot = (float) ((robotRot - crAngle+2*Math.PI)%(2*Math.PI));		
			if(robotRot<clAngle&& robotRot>0.0f){
				return w;
			}
		}
		return null;
	}

}
