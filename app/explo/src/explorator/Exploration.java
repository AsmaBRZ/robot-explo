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
    private boolean targetFound=false;
    //We set the distance with which the robot rolls to 1m. 
	private double distanceRob=50;
	private	Point cornerLeft, cornerRight;
	private int threshClose =150;
	private int timeForMove=15000;
	private double lastDistanceTravelled=0; 
	float lenSeg,x0,y0,x1,y1;
	double robRotation;
	Vec2 robPosition;
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
	@SuppressWarnings("resource")
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
			
		    System.out.println("My walls are: "+this.env.getWalls().toString()+"\n0");
		    robRotation=this.robot.getRotation()-Math.PI/2;
		    
			robPosition=this.robot.getPosition();
			System.out.println();
			//Step 0: Capture the visual information			
			// capture visual information
			if(!obstacle) {
				data=robot.captureData();
				this.obstacle=false;
			}
			
			//System.out.println("Visual information: "+data.toString());

			//retreive information about the object to find
			ArrayList<Float> dimensionsObj=data.get(1);
			//System.out.println("dimensionsObj"+dimensionsObj);
			float distanceToWall=dimensionsObj.get(0);
			float startX=dimensionsObj.get(1);
			float startY =dimensionsObj.get(2);
			float endX=dimensionsObj.get(3);
			float endY=dimensionsObj.get(4);
			
			//retreive information about the dimensions of the walls
			float height=data.get(2).get(0)/100;
			this.env.updateWallsHeight(height);
			for(int i=0; i<this.env.getWalls().size();i++) {
				this.env.getWall(i).updateHeight(height);
			}
			ArrayList<Float> width=data.get(3);
			ArrayList<Float>  depthL=data.get(4);
			ArrayList<Float>  depthR=data.get(5);

			//retreive information about the distances captures by the autonomous robot's sensors
			ArrayList<Float>  disSensors=data.get(6);

			if(startX!=-1) {
				//the target object has been detected
				this.targetFound=true;
				System.out.println("Target detected");
				return 1;
			}
			if(disSensors.get(2)!=0|| disSensors.get(3)!=0 || disSensors.get(1)!=0){
				System.out.println("sinus                  "+Math.sin(robRotation));
				//the autonomous robot is too close from the wall in front
				height=1;
				System.out.println("prox in front: the autonomous robot is so too close from an eventual  wall in front, -> move back");
				try {
					System.out.println(" prox in front robot s position"+robPosition.toString());
					double alpha = Math.toRadians(45); 
					double b = Math.tan(alpha)*this.lastDistanceTravelled;
					double yA=this.lastDistanceTravelled;
					double yB=this.lastDistanceTravelled;
					float xA=(float) 0.5;
					float xB=(float) -0.5;
					System.out.println("OBSTACLE BEFORE "+xA+" "+ yA+" "+ xB+ " " +yB);	
					//rotation + translation according to rob's rotation + position
							
					//System.out.println("ROb rotation is"+this.robRotation);
					x0=(float) (xA*((float)Math.cos(robRotation))-yA*Math.sin(robRotation)+robPosition.x);
					y0=(float) (yA*((float)Math.cos(robRotation))+xA*Math.sin(robRotation)+robPosition.y);
					x1=(float) (xB*((float)Math.cos(robRotation))-yB*Math.sin(robRotation)+robPosition.x);
					y1=(float) (yB*((float)Math.cos(robRotation))+xB*Math.sin(robRotation)+robPosition.y);
					System.out.println("OBSTACLE AFTER "+x0+" "+ y0+" "+ x1+ " " +y1);	
					cornerLeft=new Point(x0,(y1+y0)/2);
					cornerRight=new Point(x1,(y0+y1)/2);
					lenSeg=(float) Math.sqrt(Math.pow((x0-x1),2)+Math.pow((y0-y1),2));

					this.env.addWall(this.cpWall,cornerLeft,cornerRight,10,lenSeg,true);
					this.jmeApp.map.setCurrentWall(cpWall);

					this.cpWall++;
					System.out.println("New Wall: "+cornerLeft.toString()+" "+cornerRight.toString());
					this.robot.rotate(90);
					this.robot.rotate(90);
					this.lastDistanceTravelled=0;
					this.lastDistanceTravelled=this.robot.move(distanceRob);
					//Thread.sleep(this.timeForMove);
					this.robot.rotate(-90);
					this.lastDistanceTravelled+=this.robot.move(distanceRob);
					//Thread.sleep(this.timeForMove);
					this.robot.rotate(-90);

				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				return 0;
			}
			if(disSensors.get(0)!=0 ){
				//the autonomous robot is too close from the wall in front
				System.out.println("prox left: the autonomous robot is so too close from an eventual  wall in front, -> move back");
				try {
					height=1;
					y0=pixToCmY(width.get(2));
					y1=pixToCmY(width.get(4));
					x0=pixToCmX(width.get(1),y0);
					x1=pixToCmX(width.get(3),y1);
					System.out.println(" prox right "+x0+" "+ y0+" "+ x1+ " " +y1);
					x0=(float) (x0*((float)Math.cos(robRotation))-y0*Math.sin(robRotation)+robPosition.x);
					y0=(float) (y0*((float)Math.cos(robRotation))+x0*Math.sin(robRotation)+robPosition.y);
					x1=(float) (x1*((float)Math.cos(robRotation))-y1*Math.sin(robRotation)+robPosition.x);
					y1=(float) (y1*((float)Math.cos(robRotation))+x1*Math.sin(robRotation)+robPosition.y);
					//Thread.sleep(this.timeForMove);
					x0/=100;
					y0/=100;
					x1/=100;
					y1/=100;
					//System.out.println("XY "+x0+ " "+y0+ " "+ x1+ " "+y1);
					cornerLeft=new Point(x0,(y1+y0)/2);
					cornerRight=new Point(x1,(y0+y1)/2);
					//System.out.println("prox right x0 y0 x1 y1"+x0+" "+ y0+" "+ x1+ " " +y1);
					lenSeg=(float) Math.sqrt(Math.pow((x0-x1),2)+Math.pow((y0-y1),2));

					this.env.addWall(this.cpWall,cornerLeft,cornerRight,height,lenSeg);
					this.jmeApp.map.setCurrentWall(this.cpWall);
					this.cpWall++;
					System.out.println("New Wall: "+cornerLeft.toString()+" "+cornerRight.toString());
					this.lastDistanceTravelled=0;
					this.robot.rotate(90);
					this.lastDistanceTravelled=this.robot.move(distanceRob);
				    //Thread.sleep(this.timeForMove);
					this.robot.rotate(-90);				
					
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				return 0;
			}

			if(disSensors.get(4)!=0 ){
				//the autonomous robot is too close from the wall in front
				System.out.println("prox right: the autonomous robot is so too close from an eventual  wall in front, -> move back");
				try {
					height=1;
					y0=pixToCmY(width.get(2));
					y1=pixToCmY(width.get(4));
					x0=pixToCmX(width.get(1),y0);
					x1=pixToCmX(width.get(3),y1);
					//System.out.println(" prox right "+x0+" "+ y0+" "+ x1+ " " +y1);
					x0=(float) (x0*((float)Math.cos(robRotation))-x0*Math.sin(robRotation)+robPosition.x);
					y0=(float) (y0*((float)Math.cos(robRotation))+y0*Math.sin(robRotation)+robPosition.y);
					x1=(float) (x1*((float)Math.cos(robRotation))-y1*Math.sin(robRotation)+robPosition.x);
					y1=(float) (y1*((float)Math.cos(robRotation))+x1*Math.sin(robRotation)+robPosition.y);
					
					System.out.println("prox right x0 y0 x1 y1 "+x0+" "+ y0+" "+ x1+ " " +y1);
					
					x0/=100;
					y0/=100;
					x1/=100;
					y1/=100;
					//System.out.println("XY "+x0+ " "+y0+ " "+ x1+ " "+y1);
					cornerLeft=new Point(x0,(y1+y0)/2);
					cornerRight=new Point(x1,(y0+y1)/2);
					lenSeg=(float) Math.sqrt(Math.pow((x0-x1),2)+Math.pow((y0-y1),2));

					this.env.addWall(this.cpWall,cornerLeft,cornerRight,height,lenSeg);
					this.jmeApp.map.setCurrentWall(cpWall);
					this.cpWall++;
					System.out.println("New Wall: "+cornerLeft.toString()+" "+cornerRight.toString());
					this.lastDistanceTravelled=0;
					this.robot.rotate(-90);
					this.lastDistanceTravelled=this.robot.move(distanceRob);
					//Thread.sleep(this.timeForMove);
					this.robot.rotate(90);
					this.lastDistanceTravelled+=this.robot.move(distanceRob);
					//Thread.sleep(this.timeForMove);
					this.robot.rotate(90);
					
					
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				return 0;
			}
			
			if(disSensors.get(5)!=0 || disSensors.get(6)!=0 ){
					//the autonomous robot is too close from the wall in front
					System.out.println("rear prox: the autonomous robot is so too close from a wall behind it, -> exploration of this wall");
					try {
						this.lastDistanceTravelled=0;
						this.lastDistanceTravelled=this.robot.move(distanceRob);
						//Thread.sleep(this.timeForMove);
						this.robot.rotate(90);
						this.robot.rotate(90);
					} catch (IOException | InterruptedException e) {
						e.printStackTrace();
					}
					
					return 0;
			}
				
			
			//building detected walls
			if(width.get(0)!=-1 && width.get(2)>threshClose){
					y0=width.get(2);
					y1=width.get(4);
					x0=width.get(1);
					x1=width.get(3);
					//System.out.println("before width"+x0+" "+ y0+" "+ x1+ " " +y1);

					y0=pixToCmY(width.get(2));
					y1=pixToCmY(width.get(4));
					x0=pixToCmX(width.get(1),y0);
					x1=pixToCmX(width.get(3),y1);
					
					//System.out.println("WWWWWWidth "+x0+" "+ y0+" "+ x1+ " " +y1);
					//System.out.println("ROB rotation"+robRotation);
					x0=(float) (x0*((float)Math.cos(robRotation))-y0*Math.sin(robRotation)+robPosition.x);
					y0=(float) (y0*((float)Math.cos(robRotation))+x0*Math.sin(robRotation)+robPosition.y);
					x1=(float) (x1*((float)Math.cos(robRotation))-y1*Math.sin(robRotation)+robPosition.x);
					y1=(float) (y1*((float)Math.cos(robRotation))+x1*Math.sin(robRotation)+robPosition.y);
					//System.out.println("ROTATION x0 y0 x1 y1"+x0+" "+ y0+" "+ x1+ " " +y1);
					
					x0/=100;
					y0/=100;
					x1/=100;
					y1/=100;
					//System.out.println("XY "+x0+ " "+y0+ " "+ x1+ " "+y1);
					cornerLeft=new Point(x0,(y1+y0)/2);
					cornerRight=new Point(x1,(y0+y1)/2);
					lenSeg=(float) Math.sqrt(Math.pow((x0-x1),2)+Math.pow((y0-y1),2));

					this.env.addWall(this.cpWall,cornerLeft,cornerRight,height,lenSeg);
					this.jmeApp.map.setCurrentWall(cpWall);
					this.cpWall++;
					
			    }
			if(depthR.get(0)!=-1) {
				y0=pixToCmY(depthR.get(2));
				y1=pixToCmY(depthR.get(4));
				x0=pixToCmX(depthR.get(1),y0);
				x1=pixToCmX(depthR.get(3),y1);
				System.out.println("DepthR x0 y0 x1 y1"+x0+" "+ y0+" "+ x1+ " " +y1);
				x0=(float) (x0*((float)Math.cos(robRotation))-y0*Math.sin(robRotation)+robPosition.x);
				y0=(float) (y0*((float)Math.cos(robRotation))+x0*Math.sin(robRotation)+robPosition.y);
				x1=(float) (x1*((float)Math.cos(robRotation))-y1*Math.sin(robRotation)+robPosition.x);
				y1=(float) (y1*((float)Math.cos(robRotation))+x1*Math.sin(robRotation)+robPosition.y);
				x0/=100;
				y0/=100;
				x1/=100;
				y1/=100;
				cornerLeft=new Point(x0,(y1+y0));
				cornerRight=new Point(x1,(y0+y1)/2);
				lenSeg=(float) Math.sqrt(Math.pow((x0-x1),2)+Math.pow((y0-y1),2));

				this.env.addWall(this.cpWall,cornerLeft,cornerRight,height,lenSeg);
				this.jmeApp.map.setCurrentWall(cpWall);
				this.cpWall++;
			}
			if(depthL.get(0)!=-1) {
				y0=pixToCmY(depthL.get(2));
				y1=pixToCmY(depthL.get(4));
				x0=pixToCmX(depthL.get(1),y0);
				x1=pixToCmX(depthL.get(3),y1);
				System.out.println("DepthL x0 y0 x1 y1"+x0+" "+ y0+" "+ x1+ " " +y1);
				
				x0=(float) (x0*((float)Math.cos(robRotation))-y0*Math.sin(robRotation)+robPosition.x);
				y0=(float) (y0*((float)Math.cos(robRotation))+x0*Math.sin(robRotation)+robPosition.y);
				x1=(float) (x1*((float)Math.cos(robRotation))-y1*Math.sin(robRotation)+robPosition.x);
				y1=(float) (y1*((float)Math.cos(robRotation))+x1*Math.sin(robRotation)+robPosition.y);
				x0/=100;
				y0/=100;
				x1/=100;
				y1/=100;
				cornerLeft=new Point(x0,(y1+y0));
				cornerRight=new Point(x1,(y0+y1)/2);
				lenSeg=(float) Math.sqrt(Math.pow((x0-x1),2)+Math.pow((y0-y1),2));

				System.out.println("ROTATION x0 y0 x1 y1"+x0+" "+ y0+" "+ x1+ " " +y1);
				this.env.addWall(this.cpWall,cornerLeft,cornerRight,height,lenSeg);
				this.jmeApp.map.setCurrentWall(cpWall);
				this.cpWall++;
			}
			
			//the autonomous robot detect a segment (Horizontal) only
			if(width.get(0)!=-1) { 
				if(depthL.get(0)==-1 ) {
						if(depthR.get(0)==-1) {
							System.out.println("100");
							if(width.get(2)>threshClose){
								System.out.println("Close");
								try {
									this.lastDistanceTravelled=0;
									this.robot.rotate(90);
									this.lastDistanceTravelled=this.robot.move(distanceRob);
									if((this.lastDistanceTravelled)<(this.distanceRob/10) ){
										System.out.println("Distance not totally travelled: Last"+(int)this.lastDistanceTravelled+ " robDistanece"+(int)this.distanceRob);
										return 0;
									}
									System.out.println("before sleep");
									//Thread.sleep(this.timeForMove);
									System.out.println("after sleep");
									this.robot.rotate(-90);
									return 0;
								} catch (IOException | InterruptedException e) {
									e.printStackTrace();
								}
							}
							else {
								System.out.println("Far");
								try {
									this.lastDistanceTravelled=0;
									this.lastDistanceTravelled=this.robot.move(distanceRob);
									//Thread.sleep(this.timeForMove);
           							return 0;
								} catch (IOException | InterruptedException e) {
									e.printStackTrace();
								}
							}
						}
						else {
							System.out.println("101");
							if (width.get(2)>threshClose){
								System.out.println("Close");
								try {
									this.robot.rotate(90);
									this.robot.rotate(90);
									this.lastDistanceTravelled=0;
									this.lastDistanceTravelled=this.robot.move(this.distanceRob);
									if(this.lastDistanceTravelled<this.distanceRob/10) {
										System.out.println("Distance not totally travelled");
										return 0;
									}
									this.robot.rotate(-90);}
								 catch (IOException | InterruptedException e) {
									e.printStackTrace();
								}
								return 0;
							}
							else {
								System.out.println("Far");
								try {
									this.lastDistanceTravelled=0;
									this.lastDistanceTravelled=this.robot.move(distanceRob);
								} catch (IOException | InterruptedException e) {
									e.printStackTrace();
								}
								return 0;
							}
						}
			    }
				else {
					if(depthR.get(0)==-1) {
						System.out.println("110");
						if(width.get(2)>threshClose){
							System.out.println("Close");
							try {
								this.lastDistanceTravelled=0;
								this.robot.rotate(90);
								this.lastDistanceTravelled=this.robot.move(distanceRob);
								if(this.lastDistanceTravelled<this.distanceRob/10) {
									System.out.println("Distance not totally travelled");
									return 0;
								}
								this.robot.rotate(-90);
							} catch (IOException | InterruptedException e) {
									e.printStackTrace();
								}
							return 0;
						}
						else {
							System.out.println("Far");
							try {
								this.lastDistanceTravelled=0;
								this.lastDistanceTravelled=this.robot.move(distanceRob);
							} catch (IOException | InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
					else {
						System.out.println("111");
						if (width.get(2)>threshClose){
							System.out.println("Close");
							try {
								this.robot.rotate(90);
							} catch (IOException | InterruptedException e1) {
								e1.printStackTrace();
							}
							return 0;
						}
						else {
							System.out.println("Far");
							try {
								this.lastDistanceTravelled=0;
								this.lastDistanceTravelled=this.robot.move(distanceRob);
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
						System.out.println("000");
							try {
								this.lastDistanceTravelled=0;
								this.lastDistanceTravelled=this.robot.move(distanceRob);
								//Thread.sleep(this.timeForMove);
							} catch (IOException | InterruptedException e) {
								e.printStackTrace();
							}
						return 0;

					}
					else {
						System.out.println("001");
						try {
							this.robot.rotate(90);
						} catch (IOException | InterruptedException e) {
							e.printStackTrace();
						}
					return 0;

					}
				}
				else {
					if(depthR.get(0)==-1) {
						System.out.println("010");
						try {
							this.robot.rotate(-90);
						} catch (IOException | InterruptedException e) {
							e.printStackTrace();
						}
					return 0;
					}
					else {
						System.out.println("011");
						try {
							this.robot.rotate(90);
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
	public float pixToCmY(float x) {
		return (float)((x)/((9.829874266*0.1*x-29.90298351*Math.pow(x,0.5)+  227.7674479)));
	}
	public float pixToCmX(float x1,float x2) {
		return (float) (-2.472523567*Math.pow(10,-6) *Math.pow(x1,3)-2.356197273*Math.pow(10,-5) *Math.pow(x1,2)*x2-2.456736539*Math.pow(10,-5) *Math.pow(x2,2)*x1-2.448177102*Math.pow(10,-6) *Math.pow(x2,3)+2.85262976*Math.pow(10,-3) *Math.pow(x1,2)+1.282380515*Math.pow(10,-2) *x1*x2+2.864496127*Math.pow(10,-3) *Math.pow(x2,2)-7.842542877*Math.pow(10,-1) *x1-8.549721731*Math.pow(10,-1) *x2+62.57726979);
	}
}
