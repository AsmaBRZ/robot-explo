package Representation;

import Actionners.Robot;
import com.jme3.app.SimpleApplication;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.shape.Box;
import envStructures.*;
import java.util.ArrayList;

/**
 * 
 * @author Clara Rigaud
 * @author Asma BRAZI
 * @author Alexandre Heintzmann
 * commented: Alexandre Heintzmann
 */
public class Scene extends SimpleApplication {
	public InternalRepresentation map;
	private float wallDepth = 0.2f;
	private Material diffuseWhite;
	private Material diffuseRed;
	private ArrayList<Integer> walls=new ArrayList<Integer>();
	public Scene(InternalRepresentation env, Robot robot){
		this.map = env;
	}

	@Override
	public void simpleInitApp(){
		//Red and white color for the walls
		this.diffuseWhite = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
		this.diffuseRed = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
		diffuseRed.setBoolean("UseMaterialColors",true); 
		diffuseRed.setColor("Diffuse", new ColorRGBA(1,0,0,1));

		//Colors for the axes
		Material red = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		red.setColor("Color", new ColorRGBA(1,0,0,1));

		Material blue = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		blue.setColor("Color", new ColorRGBA(0,0,1,1));

		//Adding and positioning light
		PointLight  sun = new PointLight ();
		sun.setPosition(new Vector3f(0,0,10));
		rootNode.addLight(sun);

		//Position of the camera (our view)
		cam.setLocation(new Vector3f(0.0f,0.0f,20.0f));
		cam.setRotation(new Quaternion().fromAngles((float) (Math.PI),0.0f,(float) (Math.PI)));

		//X and Y arrows
		Arrow aX = new Arrow(new Vector3f(0.5f, 0 ,0));
		Geometry guizmoX = new Geometry("guizmoX", aX);
		guizmoX.setMaterial(red);


		Arrow aY = new Arrow(new Vector3f(0, 0.5f ,0));
		Geometry guizmoY = new Geometry("guizmoY", aY);
		guizmoY.setMaterial(red);


		//Direction of the robot
		Arrow robDir = new Arrow(new Vector3f(this.map.getRobot().getPointer().x, this.map.getRobot().getPointer().y ,0));		
		Geometry robot = new Geometry("robot", robDir);
		robot.setName("robot");
		robot.setMaterial(blue);
		robot.setLocalTranslation(this.map.getRobot().posX(), this.map.getRobot().posY(), 0);
		//Adding the walls on the scene
		for(int i=0;i<this.map.getWalls().size();i++) {
			int idWall=this.map.getWalls().get(i).getId();
			this.addWallOnScene(idWall);
		}
		//adding the robot to the scene
		rootNode.attachChild(guizmoX);
		rootNode.attachChild(guizmoY);
		rootNode.attachChild(robot);

	}
	//update automatically the scene by updating its elements
	@Override
	public void simpleUpdate(float tpf) {
		for(int i=0;i<this.map.getWalls().size();i++) {
			int idWall=this.map.getWalls().get(i).getId();
			if(rootNode.getChild(idWall)!=null) {
				if(!this.walls.contains(idWall)) {
					addWallOnScene(idWall);
				}
			}
		}
		Geometry robot = (Geometry) rootNode.getChild("robot");
		Arrow robDirection = new Arrow(new Vector3f(this.map.getRobot().getPointer().x, this.map.getRobot().getPointer().y ,0));	
		robot.setMesh(robDirection);
		robot.setLocalTranslation(this.map.getRobot().posX(), this.map.getRobot().posY(), 0);   	
	}
	//add a Wall object to the Scene
	public void addWallOnScene(int idWall){
		this.walls.add(idWall);
		Wall w=this.map.getWalls().get(idWall);
		//System.out.println("Wall to add to the scene"+w.getId());
		float height,width;
		float maxHeight=this.map.getMaxHeight();
		this.map.getWalls().get(idWall).updateHeight(maxHeight);

		height = w.getHeight();
		width = w.getWidth();
		Box mur = new Box(width, this.wallDepth, height);

		Geometry geom = new Geometry();
		geom.setMesh(mur);
		geom.setName("wall"+Integer.toString(w.getId()));
		float theta = w.getRotation();
		Quaternion rot = new Quaternion();
		rot.fromAngles(0, 0, theta);
		geom.setLocalRotation(rot);
		geom.setLocalTranslation(2*w.posX(), 2*w.posY(),height/2);
		geom.setMaterial(this.diffuseRed);

		rootNode.attachChild(geom);
	}

	//update the visual information on JME of a certain wall w
	public void updateWall(int idWall){
		Wall w=this.map.getWalls().get(idWall);
		float height = w.getHeight();
		float width = w.getWidth();

		float theta = w.getRotation();
		Quaternion rot = new Quaternion();
		rot.fromAngles(0, 0, theta);

		rootNode.getChild("wall"+Integer.toString(w.getId())).setLocalTranslation(2*w.posX(),2*w.posY(),height/2);
		rootNode.getChild("wall"+Integer.toString(w.getId())).setLocalRotation(rot);
		((Geometry)rootNode.getChild("wall"+Integer.toString(w.getId()))).setMesh(new Box(width, this.wallDepth, height));	

		if(w.getId() == this.map.getCurrentWall()){
			rootNode.getChild("wall"+w.getId()).setMaterial(this.diffuseRed);
		}else{
			rootNode.getChild("wall"+w.getId()).setMaterial(this.diffuseWhite);
		}
	}

}
