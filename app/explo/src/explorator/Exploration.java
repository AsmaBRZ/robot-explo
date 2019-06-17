package explorator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.*;
import java.util.Scanner; 


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
		System.out.println("1: Pepper");
		System.out.println("2: Platon");
		
		String str = sc.nextLine();
		goal=Integer.parseInt(str);
		switch(goal) {
			case 0: System.out.println("Le robot autonome va essayer de trouver: QR code"); break;
			case 1: System.out.println("Le robot autonome va essayer de trouver: Pepper"); break;
			case 2: System.out.println("Le robot autonome va essayer de trouver: Platon"); break;
		}
		System.out.println("Exploration starts");
		for (int i=0;i<4;i++) {
			System.out.println("############################## WALL "+i+" ###############################################");
			this.jmeApp.map.setcurrentWall(i);
			System.out.println("Current wall JME"+this.jmeApp.map.getCurrentWall());
			
			// the 2 is for '2.png': I am searching for the image of Platon
			//0.png is the QR code
			int r=explore(i,goal);
			switch(r) {
			case -1:
				System.out.println("End");
				return ;
				
			case 0: 
				System.out.println("Next wall");
				break;
			case 1:
				System.out.println("Target found!");
				return;
			}
			
			System.out.println("#############################################################################");
		}
		while(true){

		}
		//robot.disconnect();
		//System.out.println("done");
	}

	@SuppressWarnings("unused")
	//target is the name of the object we re searching for, currently it may be in[0.png, 1.png or 2.png]
	private int explore(int  facingWall ,int target){
		//store the index of the previous wall, so as to get the coordinates of its right corner 
		int previousWall =getIndexOfPreviousWall( facingWall);
		boolean targetFound=false;
		//We set the distance with which the robot rolls to 1m. 
		int distanceRob=1;
		//only to facilitate the image recognition, we have pepper in each corner, with this variable we ask to verify only if it is pepper or not
		boolean cornerLeftDetection=true;
		float  widthObj, heightObj;
		float heightWall = 0.f;
		List<ArrayList<Float>> data;
		System.out.println("Exploration of the wall "+ facingWall+" begins");
		//the index of the object detected is according to its order 
		int indexObjDetected;
		float distanceToCorner= 0.f,partOfWidth= 0.f,distWall = 0.f;
		//Step1 the robot must be in front of the corner on the left 
		
		
		//get the height of a wall		
		//distance is captured to know if an object has been detected
		//data is defined as: the distance to the wall ,[startX, startY, endX, endY] of the object detected on the picture takers, resolution(2d)
		//the first argument is set to 1 <--> the picture of Pepper
		
		
			data=robot.captureData(1,cornerLeftDetection,"u");
			cornerLeftDetection=false;
			indexObjDetected=-1;
			//we begin from i=1 because at i=0 we get the resolution of the picture taken by the robot for recognition
			System.out.println("Data captured: "+data.toString());
			for(int i=1;i<data.size()-1;i++) {
				if(data.get(i).get(0)!=-1 ){
					indexObjDetected=i;
				}
			}
			if(indexObjDetected==-1) {
				System.out.println("Any object has been detected on the corner L");
				//as an hypothesis (for the moment) the robot should detect the corner on the left from which 
				//the robot may be sure to continue exploring the wall quietly  
				System.out.println("Exploration interrupted");
				return -1;
			}
			//the corner on the left has been detected, lets update the environment
			
			distanceToCorner=data.get(indexObjDetected).get(0)/100.f;
			System.out.println("Corner Left detected at "+distanceToCorner);
			//update the height of the current wall
			heightWall=data.get(data.size()-1).get(0)/100.f;
			this.env.getWall(facingWall).setHeight(heightWall);
			System.out.println("Height estimated "+heightWall);
			
		    //update the part of the width that the robot have seen
		    partOfWidth=(float) Math.sin(45.0)*distanceToCorner;
		    System.out.println("Part of width estimated "+partOfWidth);
		   
		    distWall=(float) Math.cos(45.0)*distanceToCorner;
		    System.out.println("The distance to the wall "+distWall);
		    
		    this.env.getWall(facingWall).cumulateWidth(partOfWidth);
		    System.out.println("The new width after update"+this.env.getWall(facingWall).getWidth());
		if(facingWall==0) {
		    //update the corner on the left
	    	this.env.getWall(facingWall).setCornerLeft(new Point(-partOfWidth,distWall));
	    	System.out.println("first facing wall, we set the left corner"+this.env.getWall(facingWall).getCornerLeft().toString());
	    }
	    else {
	    	//get the index of the previous wall, the right corner  of the previous wall is the left one of the current wall
	    	
	    	Point corner=this.env.getWall(previousWall).getCornerRight();
	    	this.env.getWall(facingWall).setCornerLeft(corner);
	    	System.out.println("wall "+facingWall+"corner left, is prvious corner right"+this.env.getWall(facingWall).getCornerLeft());
	    }
	    
	    //update the corner on the right 
	    switch( facingWall) {
	    	case 0: this.env.getWall(facingWall).setCornerRight(new Point(0,distWall)); break;
	    	case 1: this.env.getWall(facingWall).setCornerRight(new Point(this.env.getWall(facingWall).getCornerLeft().getPosition().getX(),this.env.getWall( facingWall).getCornerLeft().getPosition().getY()-partOfWidth)); break;
	    	case 2: this.env.getWall(facingWall).setCornerRight(new Point(this.env.getWall( facingWall).getCornerLeft().getPosition().getX()-partOfWidth,this.env.getWall(facingWall).getCornerLeft().getPosition().getY())); break;
	    	case 3: this.env.getWall(facingWall).setCornerRight(new Point(this.env.getWall(facingWall).getCornerLeft().getPosition().getX(),this.env.getWall( facingWall).getCornerLeft().getPosition().getY()+partOfWidth)); break;
	    }
	    System.out.println("after update corner right of facing wall"+this.env.getWall(facingWall).getCornerRight().toString());
		//rotation of 45 right, to be in front of the wall
		try {
			//45 right, -45 left
			this.robot.rotate(45);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

				
		//Step2 the robot is in front of the wall
		//reset the indexObjDetected for a new capture
				indexObjDetected=-1;
				boolean isCornerRight=false;
		///////////////////////////////////////////////////////////////////////////////////
		/*
		 * 
		 * Correction of the angle (previous strategy)
		 * 
		 */
		//////////////////////////////////////////////////////////////////////////////////
			
		/*float newAngleCorrected=this.robot.captureNewAngle();
		System.out.println("New angle corrected: "+ newAngleCorrected);
		try {
			if(Math.abs(newAngleCorrected)>=0){
			    if(newAngleCorrected>0){
			        this.robot.rotate((int)newAngleCorrected);
                }
			    else{
			    	this.robot.rotate((int)newAngleCorrected);
                }
			}

			this.robot.rotate(newAngleCorrected);
		} catch (IOException | InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
*/

		//while the robot didn't reach the corner on its right, it continues exploring the wall
		while(!isCornerRight) {
			//in front of  the current wall
			data=robot.captureData(2,cornerLeftDetection,"m");
			indexObjDetected=-1;
			//we begin from i=1 because at i=0 we get the resolution of the picture taken by the robot for recognition
			for(int i=1;i<data.size();i++) {
				if(data.get(i).get(0)!=-1 ){
					indexObjDetected=i;
				}
			}
			if(indexObjDetected==-1) {
				System.out.println("Any object has been detected on the current wall");
			}else {
				// an object on the wall has been detected, lets check if it is our target or not
				if(indexObjDetected==target+1) {
					//update the environment 
					Vec2 cLeft,cRight;
					cLeft=new Vec2((float) (data.get(indexObjDetected).get(1)/100.0),(float) (data.get(indexObjDetected).get(3)/100.0));
					cRight=new Vec2(data.get(indexObjDetected).get(2)/100.f,data.get(indexObjDetected).get(3)/100.f);
					widthObj=Float.parseFloat(this.dbObjects.get(indexObjDetected).get(("width")));
					heightObj = Float.parseFloat(this.dbObjects.get(indexObjDetected).get(("height")));
					//System.out.println("in front of a wall update "+);
					//attach the obj to the wall 
					//this.env.attachObj(facingWall,indexObjDetected, cLeft, cRight, widthObj, heightObj);
					System.out.println("L'objet cible a été trouvé sur le mur "+facingWall);
					System.out.println("Position du robot"+ this.robot.getPosition().toString());
					return 1;
				}
			}		
			//detection of the corner on the right
			try {
				//45 right, -45 left
				this.robot.rotate(45);
			} catch (IOException | InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//in front of the corner on the right, did the robot reach it?
			indexObjDetected=-1;
			//we begin from i=1 because at i=0 we get the resolution of the picture taken by the robot for recognition
			data=robot.captureData(1,cornerLeftDetection,"u" );
			for(int i=1;i<data.size();i++) {
				if(data.get(i).get(0)!=-1 ){
					indexObjDetected=i;
				}
			}
			if(indexObjDetected==-1) {
				System.out.println("The corner on the right hasnt been reached yet, the robot continues to exploring the wall");
				try {
					//45 right, -45 left
					this.robot.rotate(45);
					this.robot.move(distanceRob);
										
					float cumul=0.f;
					//update the width of the current wall & the corner on the right
					this.env.getWall(facingWall).cumulateWidth(distanceRob);
					System.out.println("the robot walked 1 m"+this.env.getWall(facingWall).getWidth());
					//update the corner on the right and the position of the robot
					switch( facingWall) {
						case 0: this.env.getWall(facingWall).setCornerRight(new Point(this.env.getWall(facingWall).getCornerRight().getPosition().getX()+distanceRob,this.env.getWall(facingWall).getCornerRight().getPosition().getY()));
								cumul=this.robot.getPosition().getX()+distanceRob;
								this.robot.getPosition().setX(cumul);
								break;
						case 1: this.env.getWall(facingWall).setCornerRight(new Point(this.env.getWall(facingWall).getCornerRight().getPosition().getX(),this.env.getWall(facingWall).getCornerRight().getPosition().getY()-distanceRob));
								cumul=this.robot.getPosition().getY()-distanceRob;
								this.robot.getPosition().setY(cumul);
						        break;
						case 2: this.env.getWall(facingWall).setCornerRight(new Point(this.env.getWall(facingWall).getCornerRight().getPosition().getX()-distanceRob,this.env.getWall(facingWall).getCornerRight().getPosition().getY()));
								cumul=this.robot.getPosition().getX()-distanceRob;
								this.robot.getPosition().setX(cumul);
								break;
						case 3: this.env.getWall(facingWall).setCornerRight(new Point(this.env.getWall(facingWall).getCornerRight().getPosition().getX(),this.env.getWall(facingWall).getCornerRight().getPosition().getY()+distanceRob));
								cumul=this.robot.getPosition().getY()+distanceRob;
								this.robot.getPosition().setY(cumul);
								break;
			        }
					System.out.println("cumulate right corner after moved for 1 m"+this.env.getWall(facingWall).getCornerRight().toString());
					System.out.println("New RobX "+ this.robot.getPosition().getX());
					System.out.println("New RobY "+ this.robot.getPosition().getY());
					//in front of the current wall
					this.robot.rotate(-90);
				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
				}
			}
			else {
				System.out.println("The corner on the right has been reached");
				//45 right, -45 left
				distanceToCorner=data.get(indexObjDetected).get(0)/100.f;
				partOfWidth=(float) Math.sin(45.0)*distanceToCorner;
				distWall=(float) Math.cos(45.0)*distanceToCorner;
				
				this.env.getWall(facingWall).cumulateWidth(partOfWidth);
				System.out.println("new width"+this.env.getWall(facingWall).getWidth());
				Point p=this.env.getWall(facingWall).getCornerRight();
				switch( facingWall) {
					case 0: this.env.getWall(facingWall).setCornerRight(new Point(p.getPosition().getX()+ partOfWidth , p.getPosition().getY()));break;
					case 1: this.env.getWall(facingWall).setCornerRight(new Point(p.getPosition().getX(),p.getPosition().getY() -partOfWidth));break;
					case 2: this.env.getWall(facingWall).setCornerRight(new Point(p.getPosition().getX()-partOfWidth , p.getPosition().getY()));break;
					case 3: this.env.getWall(facingWall).setCornerRight(new Point(p.getPosition().getX(), p.getPosition().getY() +partOfWidth));break;

				}
				System.out.println("final corner on the right"+this.env.getWall(facingWall).getCornerRight().toString());
  //update the two walls on the right & on the left of the robot
				System.out.println("Exploration of the wall"+ facingWall +" is done");
				//avg of the coordinates of the two corners
				/*Point newCornerLeft,newCornerRight,oldCornerLeft,oldCornerRight;
				oldCornerLeft=this.env.getWall(facingWall).getCornerLeft();
				oldCornerRight=this.env.getWall(facingWall).getCornerRight();
				switch( facingWall) {
				case 0: newCornerLeft=new Point(oldCornerLeft.getPosition().getX(),(oldCornerLeft.getPosition().getY()+oldCornerRight.getPosition().getY())/2.f);
						newCornerRight=new Point(oldCornerRight.getPosition().getX(),(oldCornerLeft.getPosition().getY()+oldCornerRight.getPosition().getY())/2.f);
						break;
				case 1: newCornerLeft=new Point((oldCornerLeft.getPosition().getX()+oldCornerRight.getPosition().getX())/2.f,oldCornerLeft.getPosition().getY());
						newCornerRight=new Point((oldCornerLeft.getPosition().getX()+oldCornerRight.getPosition().getX())/2.f,oldCornerRight.getPosition().getY());
						break;
				case 2: newCornerLeft=new Point(oldCornerLeft.getPosition().getX(),(oldCornerLeft.getPosition().getY()+oldCornerRight.getPosition().getY())/2.f);
						newCornerRight=new Point(oldCornerRight.getPosition().getX(),(oldCornerLeft.getPosition().getY()+oldCornerRight.getPosition().getY())/2.f);
						break;
				case 3: newCornerLeft=new Point((oldCornerLeft.getPosition().getX()+oldCornerRight.getPosition().getX())/2.f,oldCornerLeft.getPosition().getY());
						newCornerRight=new Point((oldCornerLeft.getPosition().getX()+oldCornerRight.getPosition().getX())/2.f,oldCornerRight.getPosition().getY());
						break;
				}*/
				//we return 0 because we have reached the corner on the right and we did not find the target
				//so we move to the next wall

				return 0;
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
	//calculate the index of the previous wall according to the current position i
	public int getIndexOfPreviousWall(int i) {
		int index = 0;
		switch(i) {
		case 0: index=3; break;
		case 1: index=0; break;
		case 2: index=1; break;
		case 3: index=2; break;
		
		}
		return index;
	}
}
