package envStructures;

import java.util.ArrayList;

import Actionners.Robot;
/**
 * 
 * @author Clara Rigaud
 *
 */
public class InternalRepresentation {
	private ArrayList<Wall> walls;
	private ArrayList<Integer> visitedWalls;
	private ArrayList<Point> markers; // Toutes les creations d'objets se passent ici, et on enregistre chaque marker 
	private ArrayList<RoomObject> wallObjects;
	private float  wallHeight = 2.5f; //m
	private Robot robot;
	private int roomType = 1;
	private int currentWall;

	public InternalRepresentation(Robot robot, int RoomType){
		this.walls = new ArrayList<Wall>();
		this.markers = new ArrayList<Point>();
		this.wallObjects= new ArrayList<RoomObject>();
		this.visitedWalls= new ArrayList<Integer>();		
		this.robot = robot;
	}
	public void updateWallsHeight(float h) {
		for(int i=0;i<this.walls.size();i++) {
			if(this.walls.get(i).getHeight()<h) {
				this.walls.get(i).setHeight(h);
			}
		}
	}
	public int getVisitedNb() {
		return this.visitedWalls.size();
	}
	
	public void addVisitedWall(int i) {
		if(!this.visitedWalls.contains(i))
			this.visitedWalls.add(i);
	}
	private void addWall(int id,Point p1,Point p2,float height,float width){

		this.markers.add(p1);
		this.walls.add(new Wall(id,p1,p2,height,width));
	}
	
	//we assume having 4 points all having the same coordinates initialy (0.0), this points are forming 4 fours; each wall's height/width is equal to 0 
	private void create4WallsRoom(){
		Point m1 = new Point(0,0);
		Point m2 = new Point(0,0);
		Point m3 = new Point(0,0);
		Point m4 = new Point(0,0);
		/*Point m1 = new Point(-2,2);
		Point m2 = new Point(2,2);
		Point m3 = new Point(2,-2);
		Point m4 = new Point(-2,-2);*/
		
		this.markers.add(m1);
		this.markers.add(m2);
		this.markers.add(m3);
		this.markers.add(m4);
		
		this.walls.add(new Wall(0,m1,m2,0,0));
		this.walls.add(new Wall(1,m2,m3,0,0));
		this.walls.add(new Wall(2,m3,m4,0,0));
		this.walls.add(new Wall(3,m4,m1,0,0));
		
		//this.walls.add(new Wall(0,m1,m2,1,4));
		//this.walls.add(new Wall(1,m2,m3,1,4));
		//this.walls.add(new Wall(2,m3,m4,1,4));
		//this.walls.add(new Wall(3,m4,m1,1,4));
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
			Vec2 newCl = Vec2.projectPointOnLine(this.markers.get(aWallCl).position(), cl, cr); // Pour l'instant c'est juste une projection orthogonale du coup ca marchera que pour le cas ou la piece est parfaitement rectengulaire 
			Vec2 newCr = Vec2.projectPointOnLine(this.markers.get(aWallCr).position(), cl, cr);
			this.markers.get(aWallCl).setPosition(newCl);   // On met a jour les points du mur en fonction de ces nouveaux points -> deux nouvelles contraintes 
			this.markers.get(aWallCr).setPosition(newCr);
			 // on met egalement à jour les points opposés vu qu'on sait qu'il y a des contraintes d'orthogonalité
			int oppositeWall = (wall+2)% 4;
			int oWallCl = this.markers.indexOf(this.getWall(oppositeWall).cornerLeft);
			int oWallCr = this.markers.indexOf(this.getWall(oppositeWall).cornerRight);
			Vec2 newoCl = Vec2.projectPointOnLine(this.markers.get(oWallCl).position(), newCr, newCr.sub(newCl).normale().add(newCr));
			
			Vec2 newoCr = Vec2.projectPointOnLine(this.markers.get(oWallCr).position(), newoCl, newoCl.sub(newCr).normale().add(newoCl));
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
		// rapportons en mm :
		height = height * robot.sensorHeight / robot.resolutionHeight;
		double fl = this.robot.getFocalLength();
		double d = fl * realHeight*1000 / height; // distance en mm

		System.out.println("Je suis à "+d/10+" cm de la porte");
		Vec2 Rd = robot.getPointer();
		Vec2 Rp = robot.position();
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
	
	public Wall getWall(int wallId){
		for (int i = 0; i<this.walls.size(); i++){
			if(wallId==this.walls.get(i).id){
				return this.walls.get(i);
			}
		}
		return null;
	}
	public Wall getFacingWall(){
		Vec2 robotPos = robot.position();
		
		Wall w = null;
		for (int i = 0; i<this.walls.size(); i++){
			double robotRot = robot.rotation();
			w = this.walls.get(i);
			float clAngle = Vec2.angle(w.cornerRight.position().sub(robotPos), new Vec2(1,0));
			float crAngle = Vec2.angle(w.cornerLeft.position().sub(robotPos), new Vec2(1,0));
			clAngle = (float) ((clAngle - crAngle+2*Math.PI)%(2*Math.PI));
			robotRot = (float) ((robotRot - crAngle+2*Math.PI)%(2*Math.PI));		
			if(robotRot<clAngle&& robotRot>0.0f){
				return w;
			}
		}
		return null;
	}
	
	public void setcurrentWall(int wallId){
		this.currentWall = wallId;
	}
	
	public int getCurrentWall(){
		return this.currentWall;
	}
	
	public ArrayList<Wall> getWalls(){
		return this.walls;
	}
	
	public ArrayList<RoomObject> getWallObjects(){
		return this.wallObjects;
	}
	
	public Robot robot(){
		return this.robot;
	}
}
