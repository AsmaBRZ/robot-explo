package explorator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import Actionners.PiThymioRobot;
import Representation.Scene;
import envStructures.InternalRepresentation;
import envStructures.Point;
import envStructures.Vec2;
/**
 * 
 * @author Asma BRAZI
 * @author Clara Rigaud 
 * @author Jehyankaa Jeyarajaratnam
 **
 */


public class Exploration {
	private boolean init=true; //indicate if the robot is at the initial state. It is useful to verify whether the robot finished the exploration
	private int explorationMode; //currently there are only one strategy of exploration
	private static boolean windows; //indicate whether the OS is windows of not
	public String localURL = "";
	private static boolean deviceConnected;
	private PiThymioRobot robot;
	private InternalRepresentation env ;
	private Scene jmeApp;
	private ArrayList<HashMap<String, String>> dbObjects=new ArrayList<HashMap<String, String>>();
	private int cpWall=0; //counter of the detected walls
	private boolean targetFound=false; //indicate whether the target has been found or not
	//We set the distance with which the robot rolls 70 centimeter
	private double distanceRob=70; //this distance may be changed. It is the current distance which the distance travel at each movement
	private int threshClose =150; //threshold indicating if the horizontal segment detected is too close or too far rom the robot
	private double lastDistanceTraveled=0; //the last distance traveled by the robot
	List<ArrayList<Float>> data;
	private boolean obstacle=false;

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
			//Read the database of objects, in our case we have ponly one object: QR code
			readDBObjects();
			System.out.println("DB created");
			//Initialization of the JME application, where we will display the environment's construction
			env =  new InternalRepresentation(robot,explorationMode);
			jmeApp = new Scene(env, robot);
			this.jmeApp.start();

		} catch (IOException | URISyntaxException ex) {
			System.err.print("Exploration() ");
			ex.printStackTrace();
		}
	}

	/**
	 * Start the exploration
	 */
	public int start(){
		//We ask the user the name of the object that robot have to search
		Scanner sc = new Scanner(System.in);
		int goal=-1;

		System.out.println("Please enter the number corresponding to the target from the following list:");
		System.out.println("0: QR code");
		//System.out.println("1: Pepper");
		//System.out.println("2: Platon");
		String str = sc.nextLine();

		goal=Integer.parseInt(str); //goal is the name of the object we are searching for, currently it may be in[0.png, 1.png or 2.png]
		switch(goal) {
		case 0: System.out.println("Target: QR code"); break;
		//case 1: System.out.println("Target: Pepper"); break;
		//case 2: System.out.println("Target: Platon"); break;
		default: System.out.println("Wrong number"); 
		return 0;
		}

		//the robot explores until finding the target or finishing exploring the environment without finding the target
		System.out.println("Exploration begins");
		while(!this.targetFound){
			int state;
			state=explore(goal);
			if(state==2) {
				System.out.println("The robot has finished exploring its environment without finding the target !");
				break;
			}
		}
		System.out.println("Exploration ends");
		try {
			robot.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 1;
	}

	@SuppressWarnings("unused")
	/**
	 * 
	 * @param target The target to find
	 * @return 1 if the target has been found and 0 to continue exploring 
	 */
	private int explore(int target){
		if(this.init) {
			//After 1 iteration, the robot has initialized the exploration
			this.init=false;
		}
		else {
			//If the current position of the robot is equal to its initial one and that the robot is not at the initialization
			//Than the environment has been totally explored without finding the target
			if(this.robot.getPosition().equals(this.robot.getInitPosition())) {
				return 2;
			}
		}
		float lenSeg,x0,y0,x1,y1,x0_tmp,y0_tmp,x1_tmp,y1_tmp;
		Point cornerLeft, cornerRight; //the left and right corners of the current detected wall
		double robRotation; //the current rotation of the robot
		Vec2 robPosition; //the current position of the robot
		robRotation=this.robot.getRotation()-Math.PI/2;
		robPosition=this.robot.getPosition();

		//Step 0: Capture the visual information			
		data=robot.captureData();

		//Step 1: Retrieve information about the object to find
		//The first element of 'data' is the resolution, for the moment we do not use it
		//The second element of 'data' contains the dimensions of the target. -1 everywhere if the robot did not detect anything
		ArrayList<Float> dimensionsObj=data.get(1);
		float distanceToWall=dimensionsObj.get(0);
		float startX=dimensionsObj.get(1);
		float startY =dimensionsObj.get(2);
		float endX=dimensionsObj.get(3);
		float endY=dimensionsObj.get(4);

		//The third element of 'data' contains information about the walls dimensions
		float height=data.get(2).get(0)/100;
		if(height==-1) {
			height=1;
		}
		this.env.addHeight(height);
		ArrayList<Float> width=data.get(3); //The fourth element of 'data' contains the dimensions of the wall in front of the robot
		ArrayList<Float>  depthL=data.get(4); //The fifth element of 'data' contains the dimensions of the wall on the left of the robot
		ArrayList<Float>  depthR=data.get(5); //The sixth element of 'data' contains the dimensions of the wall on the right of the robot

		//The seventh element of 'data' contains the distances captured by the autonomous robot's sensors
		ArrayList<Float>  disSensors=data.get(6);

		if(startX!=-1) {
			//the target object has been detected
			this.targetFound=true;
			System.out.println("Target detected");
			return 1;
		}

		//Step 2: building the detected wall

		//If the robot's front sensors are activated, than an obstacle (eventual wall) has been detected 
		if(disSensors.get(2)!=0|| disSensors.get(3)!=0 || disSensors.get(1)!=0){
			height=1;
			System.out.println("Obstalce in front of the autonomous robot");
			try {
				double yA=this.lastDistanceTraveled;
				double yB=this.lastDistanceTraveled;
				float xA=(float) 0.1;
				float xB=(float) -0.1;

				//transformation : translation and rotation of the robot to from the robot's system to the main system
				x0=(float) (xA*((float)Math.cos(robRotation))-yA*Math.sin(robRotation)+robPosition.x);
				y0=(float) (yA*((float)Math.cos(robRotation))+xA*Math.sin(robRotation)+robPosition.y);
				x1=(float) (xB*((float)Math.cos(robRotation))-yB*Math.sin(robRotation)+robPosition.x);
				y1=(float) (yB*((float)Math.cos(robRotation))+xB*Math.sin(robRotation)+robPosition.y);


				//adjust the y coordinate to the mean of the y coordinate of the first corner and the second one
				cornerLeft=new Point(x0,(y1+y0)/2);
				cornerRight=new Point(x1,(y0+y1)/2);
				lenSeg=(float) Math.sqrt(Math.pow((x0-x1),2)+Math.pow((y0-y1),2));

				this.env.addWall(this.cpWall,cornerLeft,cornerRight,1,lenSeg,robRotation,robPosition,true);
				this.env.addHeight(height);
				this.jmeApp.map.setCurrentWall(cpWall);

				this.cpWall++;
				System.out.println("New Wall: "+cornerLeft.toString()+" "+cornerRight.toString());
				//this.env.adjustCordNearestWalls();
				this.robot.rotate(90);
				this.robot.rotate(90);

				this.lastDistanceTraveled=0;
				this.lastDistanceTraveled=this.robot.move(this.distanceRob);
				double tmpDistancetraveled=this.lastDistanceTraveled;
				this.robot.rotate(-90);
				this.robot.move(distanceRob);
				this.robot.rotate(-90);
				this.lastDistanceTraveled=tmpDistancetraveled;
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return 0;
		}


		if(disSensors.get(0)!=0 ){
			System.out.println("Obstacle on the left of the autonomous robot");
			height=1;
			double yA=-0.1;
			double yB=0.1;
			float xA=(float) -0.1;
			float xB=(float) -0.1;

			//transformation : translation and rotation of the robot to from the robot's system to the main system
			x0=(float) (xA*((float)Math.cos(robRotation))-yA*Math.sin(robRotation)+robPosition.x);
			y0=(float) (yA*((float)Math.cos(robRotation))+xA*Math.sin(robRotation)+robPosition.y);
			x1=(float) (xB*((float)Math.cos(robRotation))-yB*Math.sin(robRotation)+robPosition.x);
			y1=(float) (yB*((float)Math.cos(robRotation))+xB*Math.sin(robRotation)+robPosition.y);

			cornerLeft=new Point(x0,y0);
			cornerRight=new Point(x1,y1);
			lenSeg=(float) Math.sqrt(Math.pow((x0-x1),2)+Math.pow((y0-y1),2));

			this.env.addWall(this.cpWall,cornerLeft,cornerRight,height,lenSeg,robRotation,robPosition,true);
			this.env.addHeight(height);
			this.jmeApp.map.setCurrentWall(this.cpWall);
			this.cpWall++;
			System.out.println("New Wall: "+cornerLeft.toString()+" "+cornerRight.toString());
			this.env.adjustCordNearestWalls();
			return 0;
		}

		if(disSensors.get(4)!=0 ){
			//the autonomous robot is too close from the wall in front
			height=1;
			System.out.println("Obstalce on the right of the autonomous robot");
			double yA=-0.1;
			double yB=0.1;
			float xA=(float) 0.1;
			float xB=(float) 0.1;

			//transformation : translation and rotation of the robot to from the robot's system to the main system
			x0=(float) (xA*((float)Math.cos(robRotation))-yA*Math.sin(robRotation)+robPosition.x);
			y0=(float) (yA*((float)Math.cos(robRotation))+xA*Math.sin(robRotation)+robPosition.y);
			x1=(float) (xB*((float)Math.cos(robRotation))-yB*Math.sin(robRotation)+robPosition.x);
			y1=(float) (yB*((float)Math.cos(robRotation))+xB*Math.sin(robRotation)+robPosition.y);

			cornerLeft=new Point(x0,y0);
			cornerRight=new Point(x1,y1);
			lenSeg=(float) Math.sqrt(Math.pow((x0-x1),2)+Math.pow((y0-y1),2));

			this.env.addWall(this.cpWall,cornerLeft,cornerRight,height,lenSeg,robRotation,robPosition,true);

			this.env.addHeight(height);
			this.jmeApp.map.setCurrentWall(cpWall);
			this.cpWall++;
			System.out.println("New Wall: "+cornerLeft.toString()+" "+cornerRight.toString());
			this.env.adjustCordNearestWalls();
			return 0;
		}

		if(disSensors.get(5)!=0 || disSensors.get(6)!=0 ){
			System.out.println("Obstalce behind the autonomous robot");
			try {
				//move to face the wall behind the robot
				this.lastDistanceTraveled=0;
				this.lastDistanceTraveled=this.robot.move(distanceRob);
				this.robot.rotate(90);
				this.robot.rotate(90);
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}

			return 0;
		}


		//an horizontal wall has been detected
		if(width.get(0)!=-1 && width.get(2)>threshClose){
			y0=width.get(2);
			y1=width.get(4);
			x0=width.get(1);
			x1=width.get(3);

			//Convert the coordinates in pixel to centimeter
			y0_tmp=pixToCmY(width.get(2));
			y1_tmp=pixToCmY(width.get(4));
			x0_tmp=pixToCmX(width.get(1),y0);
			x1_tmp=pixToCmX(width.get(3),y1);

			//if any error has been making by the regression, we correct the wall as follows
			double landmark=pixToCmX(70,(y0+y1)/2);
			//70*4=400 and 600x400 is the resolution of the current camera. Generally an error may occur when the wall spread on 400px
			//In this case, the regression may not be precious. So, we decided to put a small wall there. Because the robot moves with 70cm and there are 
			// a huge change to correct the previous wall by recovering it
			x0_tmp-=landmark*4;
			x1_tmp-=landmark*4;
			if(x0_tmp>100.0) {
				x0_tmp=40;
			}
			if(x0_tmp<-100.0) {
				x0_tmp=-40;
			}
			if(x1_tmp<-100.0) {
				x1_tmp=-40;
			}
			if(x1_tmp>100.0) {
				x1_tmp=40;
			}

			//transformation : translation and rotation of the robot to from the robot's system to the main system
			x0=(float) (x0_tmp*((float)Math.cos(robRotation))-y0_tmp*Math.sin(robRotation)+robPosition.x*100);
			y0=(float) (y0_tmp*((float)Math.cos(robRotation))+x0_tmp*Math.sin(robRotation)+robPosition.y*100);
			x1=(float) (x1_tmp*((float)Math.cos(robRotation))-y1_tmp*Math.sin(robRotation)+robPosition.x*100);
			y1=(float) (y1_tmp*((float)Math.cos(robRotation))+x1_tmp*Math.sin(robRotation)+robPosition.y*100);

			//conversion to 1/100 centimeter
			x0/=100;
			y0/=100;
			x1/=100;
			y1/=100;

			//adjust the y coordinate to the mean of the y coordinate of the first corner and the second one
			cornerLeft=new Point(x0,(y1+y0)/2);
			cornerRight=new Point(x1,(y0+y1)/2);
			lenSeg=(float) Math.sqrt(Math.pow((x0-x1),2)+Math.pow((y0-y1),2));

			this.env.addWall(this.cpWall,cornerLeft,cornerRight,height,lenSeg,robRotation,robPosition);
			this.jmeApp.map.setCurrentWall(cpWall);
			this.cpWall++;
			this.env.adjustCordNearestWalls();
		}


		if(width.get(0)!=-1) { 
			if(depthL.get(0)==-1 ) {
				if(depthR.get(0)==-1) {
					//System.out.println("100");
					//the autonomous robot detects a segment (Horizontal) only
					if(width.get(2)>threshClose){
						//the detected segment  is close
						//System.out.println("Close");
						try {
							this.lastDistanceTraveled=0;
							this.robot.rotate(90);
							this.lastDistanceTraveled=this.robot.move(distanceRob);
							if((this.lastDistanceTraveled)<(this.distanceRob/100) ){
								System.out.println("Distance not totally traveled: Last"+(int)this.lastDistanceTraveled+ " robDistanece"+(int)this.distanceRob);
								return 0;
							}
							this.robot.rotate(-90);
							return 0;
						} catch (IOException | InterruptedException e) {
							e.printStackTrace();
						}
					}
					else {
						//System.out.println("Far");
						//the detected segment is too far, the wall is far. The robot may miss some details. So it moves forward
						try {
							this.lastDistanceTraveled=0;
							this.lastDistanceTraveled=this.robot.move(distanceRob);
							return 0;
						} catch (IOException | InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				else {
					//System.out.println("101");
					if (width.get(2)>threshClose){
						//System.out.println("Close");
						try {
							this.lastDistanceTraveled=0;
							this.robot.rotate(90);
							this.lastDistanceTraveled=this.robot.move(distanceRob);
							if((this.lastDistanceTraveled)<(this.distanceRob/100) ){
								System.out.println("Distance not totally traveled: Last"+(int)this.lastDistanceTraveled+ " robDistanece"+(int)this.distanceRob);
								return 0;
							}
							this.robot.rotate(-90);
							return 0;
						}
						catch (IOException | InterruptedException e) {
							e.printStackTrace();
						}
					}
					else {
						//System.out.println("Far");
						//the detected segment is too far, the wall is far. The robot may miss some details. So it moves forward
						try {
							this.lastDistanceTraveled=0;
							this.lastDistanceTraveled=this.robot.move(distanceRob);
						} catch (IOException | InterruptedException e) {
							e.printStackTrace();
						}
						return 0;
					}
				}
			}
			else {
				if(depthR.get(0)==-1) {
					//System.out.println("110");
					if(width.get(2)>threshClose){
						//System.out.println("Close");
						try {
							this.lastDistanceTraveled=0;
							this.robot.rotate(90);
							this.lastDistanceTraveled=this.robot.move(distanceRob);
							if(this.lastDistanceTraveled<this.distanceRob/100) {
								System.out.println("Distance not totally traveled");
								return 0;
							}
							this.robot.rotate(-90);
						} catch (IOException | InterruptedException e) {
							e.printStackTrace();
						}
						return 0;
					}
					else {
						//System.out.println("Far");
						//the detected segment is too far, the wall is far. The robot may miss some details. So it moves forward
						try {
							this.lastDistanceTraveled=0;
							this.lastDistanceTraveled=this.robot.move(distanceRob);
						} catch (IOException | InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				else {
					//System.out.println("111");
					if (width.get(2)>threshClose){
						//System.out.println("Close");
						try {
							this.lastDistanceTraveled=0;
							this.robot.rotate(90);
							this.lastDistanceTraveled=this.robot.move(distanceRob);
							if((this.lastDistanceTraveled)<(this.distanceRob/100) ){
								System.out.println("Distance not totally traveled: Last"+(int)this.lastDistanceTraveled+ " robDistanece"+(int)this.distanceRob);
								return 0;
							}
							this.robot.rotate(-90);
							return 0;
						} catch (IOException | InterruptedException e1) {
							e1.printStackTrace();
						}
						return 0;
					}
					else {
						//System.out.println("Far");
						//the detected segment is too far, the wall is far. The robot may miss some details. So it moves forward
						try {
							this.lastDistanceTraveled=0;
							this.lastDistanceTraveled=this.robot.move(distanceRob);
						} catch (IOException | InterruptedException e) {
							e.printStackTrace();
						}
						return 0;
					}
				}


			}
		}
		else {
			if(depthL.get(0)==-1 ) {
				if(depthR.get(0)==-1) {
					//System.out.println("000");
					try {
						this.lastDistanceTraveled=0;
						this.lastDistanceTraveled=this.robot.move(distanceRob);
					} catch (IOException | InterruptedException e) {
						e.printStackTrace();
					}
					return 0;

				}
				else {
					//System.out.println("001");
					try {
						this.lastDistanceTraveled=0;
						this.lastDistanceTraveled=this.robot.move(distanceRob);
						if((this.lastDistanceTraveled)<(this.distanceRob/100) ){
							System.out.println("Distance not totally traveled: Last"+(int)this.lastDistanceTraveled+ " robDistanece"+(int)this.distanceRob);
							return 0;
						}
						return 0;
					} catch (IOException | InterruptedException e) {
						e.printStackTrace();
					}
					return 0;

				}
			}
			else {
				if(depthR.get(0)==-1) {
					//System.out.println("010");
					try {
						this.lastDistanceTraveled=0;
						this.lastDistanceTraveled=this.robot.move(distanceRob);
						if((this.lastDistanceTraveled)<(this.distanceRob/100) ){
							System.out.println("Distance not totally traveled: Last"+(int)this.lastDistanceTraveled+ " robDistanece"+(int)this.distanceRob);
							return 0;
						}
						return 0;
					} catch (IOException | InterruptedException e) {
						e.printStackTrace();
					}
					return 0;
				}
				else {
					//System.out.println("011");
					try {
						this.lastDistanceTraveled=0;
						this.lastDistanceTraveled=this.robot.move(distanceRob);
						if((this.lastDistanceTraveled)<(this.distanceRob/100) ){
							System.out.println("Distance not totally traveled: Last"+(int)this.lastDistanceTraveled+ " robDistanece"+(int)this.distanceRob);
							return 0;
						}
						return 0;
					} catch (IOException | InterruptedException e) {
						e.printStackTrace();
					}
					return 0;

				}
			}
		}
		return 0;
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
	//Nonlinear Regression from the coordinate on the Y-axis to the real distance to the object
	public float pixToCmY(float x) {
		return (float)((x)/((9.829874266*0.1*x-29.90298351*Math.pow(x,0.5)+  227.7674479)));
	}
	//Multiple Polynomial Regression from the real distance to the object + the coordinate on Y-axis to find the real width of the object
	public float pixToCmX(float x1,float x2) {
		return (float) (

				-2.053128854*Math.pow(10,-13)*Math.pow(x1,6)
				-1.752368973*Math.pow(10,-12)*Math.pow(x1,5)*x2
				-6.375390897*Math.pow(10,-12)*Math.pow(x1,4)*Math.pow(x2,2)
				-6.792800155*Math.pow(10,-12)*Math.pow(x1,3)*Math.pow(x2,3)
				-7.52539452*Math.pow(10,-12)*Math.pow(x1,2)*Math.pow(x2,4)
				-2.958182279*Math.pow(10,-12)*x1*Math.pow(x2,5)
				-3.328668717*Math.pow(10,-13)*Math.pow(x2,6)
				+4.178651395*Math.pow(10,-10)*Math.pow(x1,5)
				+3.031167416*Math.pow(10,-9)*Math.pow(x1,4)*x2
				+7.619578689*Math.pow(10,-9)*Math.pow(x1,3)*Math.pow(x2,2)
				+7.502650439*Math.pow(10,-9)*Math.pow(x1,2)*Math.pow(x2,3)
				+3.940304607*Math.pow(10,-9)*x1*Math.pow(x2,4)
				+6.117046335*Math.pow(10,-10)*Math.pow(x2,5)
				-3.264627853*Math.pow(10,-7)*Math.pow(x1,4)
				-1.86428138*Math.pow(10,-6)*Math.pow(x1,3)*x2
				-3.064907802*Math.pow(10,-6)*Math.pow(x1,2)*Math.pow(x2,2)
				-2.010726826*Math.pow(10,-6)*x1*Math.pow(x2,3)
				-4.345091807*Math.pow(10,-7)*Math.pow(x2,4)
				+1.232293973*Math.pow(10,-4)*Math.pow(x1,3)
				+4.970106407*Math.pow(10,-4)*Math.pow(x1,2)*x2
				+5.050777988*Math.pow(10,-4)*x1*Math.pow(x2,2)
				+1.532324622*Math.pow(10,-4)*Math.pow(x2,3)
				-2.331915875*Math.pow(10,-2)*Math.pow(x1,2)
				-5.69684691*Math.pow(10,-2)*x1*x2
				-2.831304254*Math.pow(10,-2)*Math.pow(x2,2)
				+2.121914659*x1
				+2.492107766*x2
				-65.3504777
				);
	}

}
