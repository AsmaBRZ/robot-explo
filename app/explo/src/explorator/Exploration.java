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
    private int currentWall=0;
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
		//System.out.println("1: Pepper");
		//System.out.println("2: Platon");
		
		String str = sc.nextLine();
		goal=Integer.parseInt(str);
		switch(goal) {
			case 0: System.out.println("Le robot autonome va essayer de trouver: QR code"); break;
			//case 1: System.out.println("Le robot autonome va essayer de trouver: Pepper"); break;
			//case 2: System.out.println("Le robot autonome va essayer de trouver: Platon"); break;
		}
		System.out.println("Exploration begins");
		explore(goal);
		System.out.println("Exploration ends");
		
		while(true){

		}
		//robot.disconnect();
		//System.out.println("done");
	}

	@SuppressWarnings("unused")
	//target is the name of the object we re searching for, currently it may be in[0.png, 1.png or 2.png]
	private int explore(int target){
		int cpWall=0;
		boolean targetFound=false;
		//We set the distance with which the robot rolls to 1m. 
		int distanceRob=1;
		while(!targetFound) {
			this.jmeApp.map.setcurrentWall(cpWall);
			List<ArrayList<Float>> data;
			// capture visual information
			data=robot.captureData();
			
			//retreive information about the object to find
			ArrayList<Float> dimensionsObj=data.get(1);
			System.out.println("dimensionsObj"+dimensionsObj);
			float distanceToWall=dimensionsObj.get(0);
			float startX=dimensionsObj.get(1);
			float startY =dimensionsObj.get(2);
			float endX=dimensionsObj.get(3);
			float endY=dimensionsObj.get(4);
			
			if(startX!=-1) {
				//the target object has been detected
				targetFound=true;
				//return 1;
			}
			
			//retreive information about the dimensions of the walls
			ArrayList<Float> dimensionsWalls=data.get(2);
			System.out.println("dimensionsWalls"+dimensionsWalls);
			float height=dimensionsWalls.get(0);
			float width=dimensionsWalls.get(1);
			float depthL=dimensionsWalls.get(2);
			float depthR=dimensionsWalls.get(3);
			
			//update the walls with the new height captured if necessary
			this.env.updateWallsHeight(height);
			
			if(depthL==-1 && depthR==-1) {
				//update the width of the wall in front
				System.out.println("In front of a wall");
				Point cornerLeft, cornerRight;
				float widthWall=(float) Math.tan(45.0)*distanceToWall;
				if(this.env.getWalls().isEmpty()) {
					cornerLeft=new Point(-widthWall,distanceToWall);
					cornerRight=new Point(widthWall,distanceToWall);
				}
				else {
					int indexPreviousWall=getIndexOfPreviousWall(cpWall);
					cornerLeft=this.env.getWalls().get(indexPreviousWall).getCornerRight();
					cornerRight=new Point(this.robot.getPosition().mul(distanceToWall));
				}
				
				Wall w=new Wall(cpWall, cornerLeft, cornerRight,height,widthWall*2);
				System.out.println("Wall"+cornerLeft.toString()+" "+cornerRight.toString());
				cpWall++;
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
		if (this.env.getWalls().isEmpty()) {
			return -1;
		}
		int lastIndex=this.env.getWalls().size()-1;
		if(i==0) {
			return lastIndex;
		}
		return i--;
	}
}
