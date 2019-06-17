package Representation;

import Actionners.Robot;
import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.shape.Box;
import envStructures.*;

import java.io.IOException;

/**
 * 
 * @author Clara Rigaud
 * @author Asma BRAZI
 * @author Alexandre Heintzmann
 * commented: Alexandre Heintzmann
 */
public class Scene extends SimpleApplication {
	public InternalRepresentation map;
	private float wallDepth = 0.1f;
	private Material diffuseWhite;
	private Material diffuseRed;
	private Robot robot;
	public Scene(InternalRepresentation env, Robot robot){
		this.map = env;
		this.robot = robot;
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

		//Adding and positionning light
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
		Arrow robDir = new Arrow(new Vector3f(this.map.robot().getPointer().x, this.map.robot().getPointer().y ,0));		
		Geometry robot = new Geometry("robot", robDir);
		robot.setName("robot");
		robot.setMaterial(blue);
		robot.setLocalTranslation(this.map.robot().posX(), this.map.robot().posY(), 0);
		//Adding the walls on the scene
		this.map.getWalls().forEach((w)->this.addWallOnScene(w));
		//adding the robot to the scene
		rootNode.attachChild(guizmoX);
		rootNode.attachChild(guizmoY);
		rootNode.attachChild(robot);

		//Init key binding
		initKeys();
	}
	//update automatically the scene by updatin its elements
	@Override
	 public void simpleUpdate(float tpf) {
	    	this.map.getWalls().forEach((w)->this.updateWall(w));
	    	Geometry robot = (Geometry) rootNode.getChild("robot");
	    	Arrow robDirection = new Arrow(new Vector3f(this.map.robot().getPointer().x, this.map.robot().getPointer().y ,0));	
	    	robot.setMesh(robDirection);
	    	robot.setLocalTranslation(this.map.robot().posX(), this.map.robot().posY(), 0);
	    	//this.map.getWallObjects().forEach((d)-> this.updateWall(d));
	    }
	 

	private void initKeys() {
		// You can map one or several inputs to one named action
		inputManager.addMapping("Photo",  new KeyTrigger(KeyInput.KEY_P));
		inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_I));
		inputManager.addMapping("Left",   new KeyTrigger(KeyInput.KEY_J));
		inputManager.addMapping("Down",  new KeyTrigger(KeyInput.KEY_K));
		inputManager.addMapping("Right",  new KeyTrigger(KeyInput.KEY_L));
		inputManager.addMapping("Recognition", new KeyTrigger(KeyInput.KEY_U));
		inputManager.addMapping("Angle", new KeyTrigger(KeyInput.KEY_Y));
		inputManager.addMapping("CompleteTour", new KeyTrigger(KeyInput.KEY_T));
		// Add the names to the action listener.
		inputManager.addListener(actionListener, "Photo", "Up", "Down","Left","Right","Recognition", "Angle","CompleteTour");

	}

	private final ActionListener actionListener = new ActionListener() {
		@Override
		public void onAction(String name, boolean keyPressed, float tpf) {
			if (name.equals("Photo") && keyPressed) {
			    try {
                    robot.takePic();
                } catch(IOException|InterruptedException e){
			        System.err.print("onAction ");
			        e.printStackTrace();
                }
			}
			if(name.equals("Up") && keyPressed){
				try{
					robot.move(0.2f);
				} catch(IOException|InterruptedException e){
					System.err.print("ActionListener() ");
					e.printStackTrace();
				}

			}
			if(name.equals("Down") && keyPressed){
				try{
					robot.move(-0.2f);
				} catch(IOException|InterruptedException e){
					System.err.print("ActionListener() ");
					e.printStackTrace();
				}
			}
			if(name.equals("Left") && keyPressed){
				try{
					robot.rotate(-45);
				} catch(IOException|InterruptedException e){
					System.err.print("ActionListener() ");
					e.printStackTrace();
				}
			}
			if(name.equals("Right") && keyPressed){
				try{
					robot.rotate(45);
				} catch(IOException|InterruptedException e){
					System.err.print("ActionListener() ");
					e.printStackTrace();
				}
			}
			if(name.equals("Recognition") && keyPressed){
				robot.analysePic();
			}
			if(name.equals("Angle") && keyPressed){
				robot.calculateAngle();
			}
			if(name.equals("CompleteTour") && keyPressed){
				//System.out.println("Complete Tour key detected");

			}
		}
	};

/*

    @Override
    public void simpleUpdate(float tpf) {
    	this.map.getWalls().forEach((w)->this.updateWall(w));
    	this.map.getWallObjects().forEach((d)-> this.updateWallObj(d));
    	
    	Geometry robot = (Geometry) rootNode.getChild("robot");
    	Arrow robDirection = new Arrow(new Vector3f(this.map.robot().getPointer().x, this.map.robot().getPointer().y ,0));	
    	robot.setMesh(robDirection);
    	robot.setLocalTranslation(this.map.robot().posX(), this.map.robot().posY(), 0);
    }
*/
    //covert a wall from its class that we implemented to a JME object
	public void addWallOnScene(Wall w){
		float height,width;
		height = w.getHeight();
		width = w.getWidth();
		Box mur = new Box(width, this.wallDepth+0.1f, height);
		
		Geometry geom = new Geometry();
		geom.setMesh(mur);
        geom.setName("wall"+Integer.toString(w.getId()));
		float theta = w.getRotation();
		Quaternion rot = new Quaternion();
		rot.fromAngles(0, 0, theta);
		geom.setLocalRotation(rot);
		geom.setLocalTranslation(2*w.posX(), 2*w.posY(),height/2);

		if(w.getId() == this.map.getCurrentWall()){
			geom.setMaterial(this.diffuseRed);
		}else{
			geom.setMaterial(this.diffuseWhite);
		}

        rootNode.attachChild(geom);
	}

	//update the visual information on JME of a certain wall w
	public void updateWall(Wall w){
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

	/*private void updateWallObj(RoomObject o){
		float height = 0,width;
		Geometry geom;
		Box obj;
		try{
			height = o.getHeight();
			width = o.getWidth();
			rootNode.getChild(1).setLocalRotation(rot);
			geom = (Geometry) rootNode.getChild("obj"+Integer.toString(o.getId()));
			obj =new Box(width, this.wallDepth+0.1f, height);
			
		}catch (NullPointerException e) {
			geom = new Geometry();
			obj = new Box();
	        geom.setName("obj"+Integer.toString(o.getId()));
	        geom.setMaterial(this.diffuseWhite);
	        rootNode.attachChild(geom);
		}
		geom.setMesh(obj);

		geom.setMaterial(this.diffuseWhite);
		
		float theta = o.getRotation();
		Quaternion rot = new Quaternion();
		rot.fromAngles(0, 0, theta);
		geom.setLocalRotation(rot);
		geom.setLocalTranslation(2*o.posX(), 2*o.posY(), height/2);
	}*/
}
