package explorator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.*;

import Actionners.*;
import Representation.Scene;
import envStructures.*;
/**
 * 
 * @author Clara Rigaud 
 * @author Jehyankaa Jeyarajaratnam
 * @author Asma BRAZI
 **
 */
public class Exploration {
	private int explorationMode;
	private static boolean windows;
	public String localURL = "";
	private static boolean deviceConnected;
	private PiThymioRobot robot;
	private InternalRepresentation env ;
	private Scene jmeApp;
	private ArrayList<HashMap<String, String>> dbObjects=new ArrayList<HashMap<String, String>>();
    private int cpWall=0;
    private boolean newWall=true;
    private boolean targetFound=false;
    //We set the distance with which the robot rolls to 1m. 
	private int distanceRob=1;
	private Wall w;
	private	Point cornerLeft, cornerRight;
	private	float widthWall
	private threshClose =10;
	private int threshTargetRec=1500;
	float lenSeg,x0,y0,x1,y1;
	double robRotation:
	Vec2 robPosition;
	public Exploration() throws InterruptedException{
		
		File configFile = new File("config.properties");
		try {

		    FileReader reader = new FileReader(configFile);
		    Properties props = new Properties();
		    props.load(reader);

		    //Check if the current OS is Windows or not
		    windows = System.getProperty("os.name").contains("Windows");
		    //Check if we want to connect to a raspberry, or if we want to test on a simulation
		    deviceConnected = props.getProperty("deviceConnected").contains("true");
		    explorationMode = Integer.parseInt(props.getProperty("roomType"));
		    System.out.println("Started exploration: \nWindows Os = " + windows + "\nDevice available = " + deviceConnected + "\nExplorationMode = "+ explorationMode);
			if(deviceConnected){
				robot = new PiThymioRobot(props);
			}else{
				robot = new PiThymioRobot(props);
			}
			reader.close();
			this.robot.connect();	
			URI uri;
    		uri = Exploration.class.getProtectionDomain().getCodeSource().getLocation().toURI();
    		List<String> localURLArray = Arrays.asList(uri.toString().split("/")).subList(1, uri.toString().split("/").length-1);
    		this.localURL = URLDecoder.decode(String.join("/", localURLArray), "UTF-8");
    		if(!windows){
    			this.localURL="/"+this.localURL;
    		}
			//read the database of objects, in our case we have three images: platon, pepper and the QR code
			readDBObjects();
			System.out.println("DB created");
			//Init of the JME application, where we will display the environment's construction
			env =  new InternalRepresentation(robot,explorationMode);
			jmeApp = new Scene(env, robot);
			this.jmeApp.start();
			robRotation=this.robot.getRotation();
			robPosition=this.robot.getPosition();
		} catch (IOException | URISyntaxException ex) {
            System.err.print("Exploration() ");
            ex.printStackTrace();
        }
	}

	/**
	 * Starts the exploration
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public void start() throws IOException, InterruptedException {
		//We ask the user the name of the object that robot have to search
		Scanner sc = new Scanner(System.in);
		int goal=-1;
		System.out.println("Veuillez saisir le chiffre correspondant à l'objet à trouver parmi la liste suivante:");
		System.out.println("0: QR code");
		//System.out.println("1: Pepper");
		//System.out.println("2: Platon");
		
		String str = sc.nextLine();
		goal=Integer.parseInt(str);
		switch(goal) {
			case 0: System.out.println("Le robot autonome va essayer de trouver: QR code"); break;
			//case 1: System.out.println("Le robot autonome va essayer de trouver: Pepper"); break;
			//case 2: System.out.println("Le robot autonome va essayer de trouver: Platon"); break;
			default: System.out.println("Exit"); break;
		}
		System.out.println("Exploration begins");
		while(!this.targetFound){
			explore(goal);
		}
		System.out.println("Exploration ends");
		//robot.disconnect();
		//System.out.println("done");
	}

	@SuppressWarnings("unused")
	//target is the name of the object we re searching for, currently it may be in[0.png, 1.png or 2.png]
	private int explore(int target){
			robRotation=this.robot.getRotation();
			robPosition=this.robot.getPosition();
			//Step 0: Capture the visual information

			this.jmeApp.map.setcurrentWall(cpWall);
			List<ArrayList<Float>> data;
			// capture visual information
			data=robot.captureData();
			System.out.println("Visual information: "+data.toString());

			//retreive information about the object to find
			ArrayList<Float> dimensionsObj=data.get(1);
			System.out.println("dimensionsObj"+dimensionsObj);
			float distanceToWall=dimensionsObj.get(0);
			float startX=dimensionsObj.get(1);
			float startY =dimensionsObj.get(2);
			float endX=dimensionsObj.get(3);
			float endY=dimensionsObj.get(4);
			
			//retreive information about the dimensions of the walls
			float height=data.get(2).get(0);
			ArrayList<Float> width=data.get(3);
			ArrayList<Float>  depthL=data.get(4);
			ArrayList<Float>  depthR=data.get(5);

			//retreive information about the distances captures by the autonomous robot's sensors
			ArrayList<Float>  disSensors=data.get(6);

			if(startX!=-1) {
				//the target object has been detected
				this.targetFound=true;
			}

			//Step 1: construct the environment with the visual information
			//update the walls with the new height captured if necessary
			this.env.updateWallsHeight(height);
			
			//the autonomous robot did not detect any useful visual information
			if(width.get(0)==-1 && depthL.get(0)==-1 && depthR.get(0)==-1){ //000
				System.out.println("Any segment has been detected");
				//It refers to its sensors to know if it is too close from a wall

				if(disSensors.get(0)!=-1){
					//the autonomous robot is too close from the wall in front
					System.out.println("the autonomous robot is so too close from a wall in front, -> move back");
					this.robot.move(-distanceRob);
					return 0;
				}
				if(disSensors.get(1)!=-1 || disSensors.get(2)!=-1 ){
					//the autonomous robot is too close from the wall in front
					System.out.println("the autonomous robot is so too close from a wall behind it, -> exploration of this wall");
					this.robot.move(distanceRob);
					this.robot.rotate(180);
					return 0;
				}
				if(disSensors.get(0)==-1 && disSensors.get(1)==-1 && disSensors.get(2)==-1 ){
					//As any information was detected, the robot moves forward
					System.out.println("the autonomous robot sees nothing: move forward");
					this.robot.move(distanceRob);
					return 0;
				}
			}
			//the autonomous robot detect a segment (Horizontal) only
			if(width.get(0)!=-1 && depthL.get(0)==-1 && depthR.get(0)==-1) { //100
				//add a new wall to the environment
				System.out.println("100: A wall in front has been detected.");
				if(width.get(2)<threshTargetRec){
					lenSeg=width.get(0)
					x0=width.get(1)*(float)*Math.cos(robRotation)-width.get(2)*Math.sin(robRotation)+robPosition.x;
					y0=width.get(2)*(float)*Math.cos(robRotation)+width.get(1)*Math.sin(robRotation)+robPosition.y;
					x1=width.get(3)*(float)*Math.cos(robRotation)-width.get(4)*Math.sin(robRotation)+robPosition.x;
					y1=width.get(4)*(float)*Math.cos(robRotation)+width.get(3)*Math.sin(robRotation)+robPosition.y;
					cornerLeft=new Point(x0,y0);
					cornerRight=new Point(x1,y1);
					this.env.addWall(this.cpWall,cornerLeft,cornerRight,height,lenSeg);
					this.cpWall++;
					System.out.println("Wall"+cornerLeft.toString()+" "+cornerRight.toString());
					this.robot.rotate(90);
					this.robot.move(distanceRob);
					this.robot.rotate(-90);
			    }
			    else{
			    	//if the wall in front is soo far, the robot moves forward
			    	System.out.println("The target may be missed");
			    	this.robot.move(distanceRob);
			    }
				return 0;
			}

			if(width.get(0)==-1 && (depthL.get(0)!=-1 || depthR.get(0)!=-1)){ //001,010,011
				//the autonomous robot is so far from the wall
				System.out.println("001-010-011:The autonomous robot is so far from an eventual wall");
				this.robot.move(distanceRob);
				return 0;
			}

			if(width.get(0)!=-1  && depthL.get(0)==-1 &&  depthR.get(0)!=-1) { //101
				//add a new wall to the environment
				//wall on the right
					lenSeg=depthR.get(0)
					x0=depthR.get(1)*(float)*Math.cos(robRotation)-depthR.get(2)*Math.sin(robRotation)+robPosition.x;
					y0=depthR.get(2)*(float)*Math.cos(robRotation)+depthR.get(1)*Math.sin(robRotation)+robPosition.y;
					x1=depthR.get(3)*(float)*Math.cos(robRotation)-depthR.get(4)*Math.sin(robRotation)+robPosition.x;
					y1=depthR.get(4)*(float)*Math.cos(robRotation)+depthR.get(3)*Math.sin(robRotation)+robPosition.y;
					cornerLeft=new Point(x0,y0);
					cornerRight=new Point(x1,y1);
					this.env.addWall(this.cpWall,cornerLeft,cornerRight,height,lenSeg);
				this.cpWall++;
				if (tooClose(width,dephtR)){
					System.out.println("101: Some walls have been detected - Too close walls");
					//wall in front
					if(width.get(2)<threshTargetRec){
						lenSeg=width.get(0)
						x0=width.get(1)*(float)*Math.cos(robRotation)-width.get(2)*Math.sin(robRotation)+robPosition.x;
						y0=width.get(2)*(float)*Math.cos(robRotation)+width.get(1)*Math.sin(robRotation)+robPosition.y;
						x1=width.get(3)*(float)*Math.cos(robRotation)-width.get(4)*Math.sin(robRotation)+robPosition.x;
						y1=width.get(4)*(float)*Math.cos(robRotation)+width.get(3)*Math.sin(robRotation)+robPosition.y;
						cornerLeft=new Point(x0,y0);
						cornerRight=new Point(x1,y1);
						this.env.addWall(this.cpWall,cornerLeft,cornerRight,height,lenSeg);
						
						this.cpWall++;
						this.robot.rotate(180);
						this.robot.move(400-max(depthR.get(2),depthR.get(4))+this.distanceRob);
						this.rotate(-90);
					}
					else{
						//if the wall in front is soo far, the robot moves forward
						System.out.println("The target may be missed");
			    		this.robot.move(distanceRob);
					}
					return 0;
				}
				else{
					System.out.println("101: Some walls have been detected - Not too close walls");
					this.robot.move(distanceRob);
					return 0;
				}
				
			}

			if(width.get(0)!=-1  && depthL.get(0)!=-1 &&  depthR.get(0)==-1) { //110
				//add a new wall to the environment

				//wall on the left
				lenSeg=depthL.get(0)
				x0=depthL.get(1)*(float)*Math.cos(robRotation)-depthL.get(2)*Math.sin(robRotation)+robPosition.x;
				y0=depthL.get(2)*(float)*Math.cos(robRotation)+depthL.get(1)*Math.sin(robRotation)+robPosition.y;
				x1=depthL.get(3)*(float)*Math.cos(robRotation)-depthL.get(4)*Math.sin(robRotation)+robPosition.x;
				y1=depthL.get(4)*(float)*Math.cos(robRotation)+depthL.get(3)*Math.sin(robRotation)+robPosition.y;
				cornerLeft=new Point(x0,y0);
				cornerRight=new Point(x1,y1);
				this.env.addWall(this.cpWall,cornerLeft,cornerRight,height,lenSeg);
				this.cpWall++;

				if (tooClose(width,dephtL)){
					System.out.println("110: Some walls have been detected - Too close walls");
					if(width.get(2)<threshTargetRec){
						//wall in front
						    
						lenSeg=width.get(0)
						x0=width.get(1)*(float)*Math.cos(robRotation)-width.get(2)*Math.sin(robRotation)+robPosition.x;
						y0=width.get(2)*(float)*Math.cos(robRotation)+width.get(1)*Math.sin(robRotation)+robPosition.y;
						x1=width.get(3)*(float)*Math.cos(robRotation)-width.get(4)*Math.sin(robRotation)+robPosition.x;
						y1=width.get(4)*(float)*Math.cos(robRotation)+width.get(3)*Math.sin(robRotation)+robPosition.y;
						cornerLeft=new Point(x0,y0);
						cornerRight=new Point(x1,y1);
						this.env.addWall(this.cpWall,cornerLeft,cornerRight,height,lenSeg);
						this.cpWall++;
						this.robot.rotate(90);
						this.robot.move(400-max(width.get(1),width.get(3))+this.distanceRob);
						this.rotate(-90);
					else{
						//if the wall in front is soo far, the robot moves forward
						System.out.println("The target may be missed");
						this.robot.move(distanceRob);
					}
					return 0;
				}
				else{
					System.out.println("110: Some walls have been detected - Not too close walls");
					this.robot.move(distanceRob);
				}
				return;
			}

			if(width.get(0)!=-1  && depthL.get(0)!=-1 &&  depthR.get(0)!=-1) { //111
				//add new walls to the environment

				//wall on the left
				lenSeg=depthL.get(0)
				x0=depthL.get(1)*(float)*Math.cos(robRotation)-depthL.get(2)*Math.sin(robRotation)+robPosition.x;
				y0=depthL.get(2)*(float)*Math.cos(robRotation)+depthL.get(1)*Math.sin(robRotation)+robPosition.y;
				x1=depthL.get(3)*(float)*Math.cos(robRotation)-depthL.get(4)*Math.sin(robRotation)+robPosition.x;
				y1=depthL.get(4)*(float)*Math.cos(robRotation)+depthL.get(3)*Math.sin(robRotation)+robPosition.y;
				cornerLeft=new Point(x0,y0);
				cornerRight=new Point(x1,y1);
				this.env.addWall(this.cpWall,cornerLeft,cornerRight,height,lenSeg);
				this.cpWall++;

				lenSeg=depthR.get(0)
				x0=depthR.get(1)*(float)*Math.cos(robRotation)-depthR.get(2)*Math.sin(robRotation)+robPosition.x;
				y0=depthR.get(2)*(float)*Math.cos(robRotation)+depthR.get(1)*Math.sin(robRotation)+robPosition.y;
				x1=depthR.get(3)*(float)*Math.cos(robRotation)-depthR.get(4)*Math.sin(robRotation)+robPosition.x;
				y1=depthR.get(4)*(float)*Math.cos(robRotation)+depthR.get(3)*Math.sin(robRotation)+robPosition.y;
				cornerLeft=new Point(x0,y0);
				cornerRight=new Point(x1,y1);
				this.env.addWall(this.cpWall,cornerLeft,cornerRight,height,lenSeg);
				this.cpWall++;


				if (tooClose(width,dephtL) ||  tooClose(width,dephtR)){
					System.out.println("111: Some walls have been detected - Too close walls");
					if(width.get(2)<threshTargetRec){
						//wall in front
					    
						lenSeg=width.get(0)
						x0=width.get(1)*(float)*Math.cos(robRotation)-width.get(2)*Math.sin(robRotation)+robPosition.x;
						y0=width.get(2)*(float)*Math.cos(robRotation)+width.get(1)*Math.sin(robRotation)+robPosition.y;
						x1=width.get(3)*(float)*Math.cos(robRotation)-width.get(4)*Math.sin(robRotation)+robPosition.x;
						y1=width.get(4)*(float)*Math.cos(robRotation)+width.get(3)*Math.sin(robRotation)+robPosition.y;
						cornerLeft=new Point(x0,y0);
						cornerRight=new Point(x1,y1);
						this.env.addWall(this.cpWall,cornerLeft,cornerRight,height,lenSeg);
						this.cpWall++;
						this.robot.rotate(90);
						this.robot.move(400-max(width.get(1),width.get(3))+this.distanceRob);
						this.rotate(-90);
					}
					else{
						//if the wall in front is soo far, the robot moves forward
						System.out.println("The target may be missed");
						this.robot.move(distanceRob);
					}
					return 0;
				}
				else{
					System.out.println("111: Some walls have been detected - Not too close walls");
					this.robot.move(distanceRob);
				}
				return 0;
			}
		return 0;
	}
		
	//test if too lines have closest extremities
	public boolean tooClose(ArrayList<Float> l1, ArrayList<Float> l2){
		if(abs(l1.get(1)-l2.get(1))<this.threshClose && abs(l1.get(2)-l2.get(2))<this.threshClose){
			return true;
		}
		if(abs(l1.get(1)-l2.get(3))<this.threshClose && abs(l1.get(2)-l2.get(4))<this.threshClose){
			return true;
		}
		if(abs(l1.get(3)-l2.get(4))<this.threshClose && abs(l1.get(4)-l2.get(2))<this.threshClose){
			return true;
		}
		if(abs(l1.get(3)-l2.get(3))<this.threshClose && abs(l1.get(4)-l2.get(4))<this.threshClose){
			return true;
		}
		return false;
	}
	//"dataBase" is a file containing the name of the image and its real dimensions, this function allows to store the DB
	public void readDBObjects() {
	
		try (FileReader reader = new FileReader(this.localURL+"/ressources/objects/dataBase");
	           BufferedReader br = new BufferedReader(reader)) {
	           String line;
	           while ((line = br.readLine()) != null) {
	        	   String[] parts = line.split("/");
	        	   HashMap<String, String> qrProperties=new HashMap<String,String>();
				   qrProperties.put("id", ""+parts[0]);
				   qrProperties.put("img",parts[1]);
				   qrProperties.put("width", parts[2]);
				   qrProperties.put("height",parts[3]);
				   this.dbObjects.add(qrProperties);               
	           }
	        } catch (IOException e) {
            System.err.format("IOException: %s%n", e);
        }
		
	}
}
