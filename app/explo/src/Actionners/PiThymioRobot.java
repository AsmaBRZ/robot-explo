package Actionners;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import com.jcraft.jsch.*;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.connection.channel.forwarded.SocketForwardingConnectListener;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;


/**
 * @author Jehyanka
 * @author asma
 *
 */
public class PiThymioRobot extends Robot {
	private static String RPi_id;
	private static String RPi_password;
	private static String RPi_IP;
	@SuppressWarnings("unused")
	private static String RPi_hostname;
	@SuppressWarnings("unused")
	private static String Path_To_SSHPASS;
	private final SSHClient ssh=new SSHClient();
	private String currentPic="";

	
	public PiThymioRobot(Properties props) throws UnknownHostException {
		super(props);
		RPi_id = props.getProperty("RPi_id","pi");
		RPi_password = props.getProperty("RPi_password","raspberry");
		RPi_IP = props.getProperty("RPi_IP");
		PiThymioRobot.RPi_hostname = props.getProperty("RPi_hostname","pisma3");
		
		/*if(RPi_IP.length()<1)
			RPi_IP=getpiIP(RPi_hostname);*/
		PiThymioRobot.Path_To_SSHPASS = props.getProperty("Path_To_SSHPASS");
	}

	@Override
	//this function order to the robot to take a picture, analyse it, and collect the results
	//the information retrieved may be the height estimated of the wall or the distance to the wall calculated according to the obj detected on the wall

	public List<ArrayList<Float>> captureData() { 
		//list contains in the order: distance starX starY endX endY resoltution1 resoltution 2
		List<ArrayList<Float>> list=new ArrayList<ArrayList<Float>>();
		ArrayList<Float> tmp;
		String pfile_returned = null;
		try {
		    //u: case: corner, we assume that there is PEPEER in each corner, we dont need to check all the elements of the DB
			//m: multi, on a random place of a wall, we check all the elements of the DB to verify if one of them matches with the picture taken by the robot
			
		    pfile_returned = executeCommandRPi("python3 exploreOnce.py ",true);
			pfile_returned=pfile_returned.trim();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(pfile_returned==null) {
			System.out.println("Erreur capture data");
			return null;
		}
		
		//get the distance from the object if the distance is equal to -1: any object has been detected, else rotate 
		sendToPC("data/distanceCaptured",localURL+"/ressources/data/");
		 try (FileReader reader = new FileReader(localURL+"/ressources/data/distanceCaptured");
		            BufferedReader br = new BufferedReader(reader)) {
		            String line;
		            //the first line contains the resolution of the image
		           if ((line = br.readLine()) != null) {
		        	  tmp =new ArrayList<Float>();
		        	   String[] parts = line.split("/");
		               for(int i=0;i<parts.length;i++) {
		            	   tmp.add(Float.parseFloat(parts[i]));   
		               }
		               list.add(tmp);
		           }
		           //the rest of the file tells if an object has been detected by having its distance to the wall !=-1
		           //if all the distances measured by matching each element of the DB are -1 , this means that any onject 
		           //has been detected
		           //a line is as= the distance to the wall ,[startX, startY, endX, endY] of the object detected, resolution(2d)
		           while ((line = br.readLine()) != null) {
		        	   tmp=new ArrayList<Float>();
		        	   String[] parts = line.split("/");
		                //float distance=Float.parseFloat(parts[0]);     
		               	//object detected
		            	   //System.out.println("The distance measured to the wall is: "+distance);
		            	   //add coordinates of objDetected
		            	   for(int i=0;i<parts.length;i++) {
		            		   tmp.add(Float.parseFloat(parts[i]));
		            	   }
		            	   list.add(tmp);	               
		           }
		        } catch (IOException e) {
	            System.err.format("IOException: %s%n", e);
	        }

				float dimension;
				tmp=new ArrayList<Float>();
				sendToPC("data/dimWall",localURL+"/ressources/data/");
				 try (FileReader reader = new FileReader(localURL+"/ressources/data/dimWall");
				            BufferedReader br = new BufferedReader(reader)) {
				            String line;
				           while((line = br.readLine()) != null) {
				               dimension=Float.parseFloat(line);     
				               tmp.add(dimension);
				               list.add(tmp);
				           }
				        } catch (IOException e) {
			            System.err.format("IOException: %s%n", e);
			        }

				float distanceSensor;
				tmp=new ArrayList<Float>();
				sendToPC("data/disSensor",localURL+"/ressources/data/");
				 try (FileReader reader = new FileReader(localURL+"/ressources/data/disSensor");
				            BufferedReader br = new BufferedReader(reader)) {
				            String line;
				           while((line = br.readLine()) != null) {
				               distanceSensor=Float.parseFloat(line);     
				               tmp.add(distanceSensor);
				               list.add(tmp);
				           }
				        } catch (IOException e) {
			            System.err.format("IOException: %s%n", e);
			        }
				
	return list;

}
	/**
	 * Sends order to thymio to move from dist (meter)
	 * @param dist
	 * @throws InterruptedException 
	 */
	public boolean move(float dist) throws IOException, InterruptedException {

		 /* A partir de la distance en cm -> On envoie une commande (du rasp au thymio) avec pour arguments la distance devant être parcouru par le robot
		 * Si distance est négative on rajoute l'option r (=reverse) pour préciser la direction 
		 * */
		this.position = this.position.add(this.getPointer().mul(dist));
		dist*=100;
		String cmd="python3 getAsebaFileD.py move True False "+Math.abs(dist);
		
		if(dist<0)
			cmd+=" r ";
		executeCommandRPi(cmd,true);
        return true;
	}
	
	/**
	 * Sends order to thymio to move at a given speed for a given time
	 * @param speed
	 * @param limit
	 * @return true if the move is complete
	 * @throws InterruptedException 
	 */
	public boolean move(int speed, int limit) throws IOException, InterruptedException {

		 /* A partir de la distance en cm -> On envoie une commande (du rasp au thymio) avec pour arguments la vitesse et le temps durant lequel le robot devra bouger
		 * Le script prend en paramètres ces deux valeurs et modifie le fichier .aesl en conséquent
		 * Une deuxième commande est ensuite lancée à partir du pi pour charger le script aseba dans le thymio
		 * Enfin on déconnecte le thymio en tuant le processus lancé.
		 * option r means reverse 
		 * */
		//Dist en m ?
		float dist=speed*limit;
		this.position = this.position.add(this.getPointer().mul(dist));
		
		String cmd="python3 getAsebaFile.py move False False "+Math.abs(speed)+" "+limit;
		if(speed<0)
			cmd+=" r ";
		executeCommandRPi(cmd,true);
		
        return true;
	}
	/**
	 * Rotate until we get to the wanted angle
	 */
	@Override
	public boolean rotate(double angle) throws IOException, InterruptedException{
		//this.rotation = (this.rotation + angle + 2 * Math.PI) % (2 * Math.PI);
		double angleRad=(angle*Math.PI)/180;
		System.out.println("Je veux tourner de : " + (angle) / Math.PI * 180);
		// if ok 
		this.rotation = (this.rotation -angleRad) % (2 * Math.PI);
		String cmd="python3 getAsebaFileD.py rotate True True "+Math.abs(angle);
		if(angle<0)
			cmd+=" r ";
		executeCommandRPi(cmd,true);
		return true;
	}

	/**
	 * Get the obj from raspberry pi and saves it in dir
	 * @param objToReceive
	 * @param dir
	 */
	private void sendToPC(String objToReceive,String dir) {
		try{
			JSch jsch = new JSch();
			//Creating a new session to connect to the raspberry
			com.jcraft.jsch.Session session = jsch.getSession("pi", RPi_IP);
			session.setPassword("raspberry");
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.connect();

			//Creating the channel to send data, Raspberries have sftp channels
			ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
			sftpChannel.connect();

			//Get the object from the raspberry, store it in the directory dir (ressources/inputImages)
			sftpChannel.get(objToReceive, dir);

			sftpChannel.exit();
			session.disconnect();
		} catch (Exception e){
			System.out.println(e);
		}
		//System.out.println("Image envoyée : "+objToReceive+" dans "+dir);
	}
	
	/**
	 * Connection with known IP adress (of RPi)
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	@Override
	public void connect() throws IOException, InterruptedException{   

		//Connects the pi
		ssh.loadKnownHosts();

		ssh.addHostKeyVerifier(new NullHostKeyVerifier());
		ssh.registerX11Forwarder(new SocketForwardingConnectListener(new InetSocketAddress("pi", 6000)));
		ssh.connect(RPi_IP);	
		
		ssh.authPassword(RPi_id, RPi_password);
		/*String command="sshpass -p \""+RPi_password+"\"ssh -o StrictHostKeyChecking=no "+RPi_IP+" -l pi";
			if(windows)
				command=pathToPutty+" -ssh pi@" +RPi_IP+" "+port;
			executeCommand(command);*/
		System.out.println("Connection to Rasp OK");

		
		initPi();
		
	}
	
	/**
	 * Initialize the raspberry Pi by sending all the -python-aseba- scripts which will be needed after
	 */
	private void initPi() {
		String[] files= {"6.png","0.png","1.png","2.png","initWorkspace.py","exploreOnce.py", "utils.py","move.py","rotate.py","rotateD.aesl","getAsebaFile.py","getAsebaFileD.py","moveD.aesl","move.aesl"};

		String urlToScript;

		try{
			JSch jsch = new JSch();
			//Creating a new session to conncet to the raspberry
            //The login
			com.jcraft.jsch.Session session = jsch.getSession("pi", RPi_IP);
			//The password
			session.setPassword("raspberry");
			//Setting properties
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.connect();

			//Creating the channel to send datas, Raspberries have sftp channels
			ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
			sftpChannel.connect();

			//Send every s file to the Raspberry.
            //The sent file is in the /python directory
			for(String s:files) {
				urlToScript = localURL + "/python/" + s;
				sftpChannel.put(urlToScript, s);
			}

			sftpChannel.exit();
			session.disconnect();
		} catch (Exception e){
			System.err.print("initPi() ");
			e.printStackTrace();
		}
		//Create directories in the Rasp which will contain DB, pictures taken by the robot and the files written 
		//containing analyzes on picture taken
		 try {
			executeCommandRPi("python3 initWorkspace.py ",true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Raspberry Pi initialisé avec succès !");
		
	}


	/**
	 * Execute a command on the pi's virtual terminal - we suppose that we are already connected and authenticated.
	 * @param command
	 * @param getResult
	 * @return the display in pi's virtual terminal
	 * @throws IOException
	 */
	public String executeCommandRPi(String command,boolean getResult) throws IOException {
		String returnResult="";
		final Session session = ssh.startSession();
		try {
			final Command cmd = session.exec(command);
			//If we want to retrieve the result
			if(getResult) {
				returnResult=(IOUtils.readFully(cmd.getInputStream()).toString());
				System.out.println(returnResult);
				System.out.println("\n** exit status: "+ cmd.getExitStatus());
			}
		} finally {
			if(getResult)
				session.close();
		}
		return returnResult;

	}


	/**
	 * Disconnect the SSHClient
	 * @throws IOException
	 */
	public void disconnect() throws IOException {
		
		//Disconnect raspberry
		ssh.disconnect();
		ssh.close();
		
	}

	/**
	 * Customized HostKeyVerifier to avoid the interactive verification of unknown Host Key (during connection)
	 */
	public class NullHostKeyVerifier implements HostKeyVerifier {

		@Override
		public boolean verify(String arg0, int arg1, PublicKey arg2) {
			return true;
		}

	}

	
	/**
	 * Gets the IP of the pi (by a scan on the network) knowing its hostname
	 * @param hostname
	 * @return the IP String
	 * @throws UnknownHostException 
	 */
	public String getpiIP(String hostname) throws UnknownHostException {
		Runtime runtime = Runtime.getRuntime();   
		String ifName="enp0s31f6";
		String cmd="nmcli dev show "+ifName;
		String myIP="";
		String mask="";
		try {
			String[] argsMac = { "sh","-c",cmd };
			String[] argsWin = { "cmd.exe", "/C", cmd };
			final Process process1;
			if(windows) 
				process1 = runtime.exec(argsWin);
			
			else 
				process1 = runtime.exec(argsMac);

			process1.waitFor();
			BufferedReader stdInput = new BufferedReader(new 
			     InputStreamReader(process1.getInputStream()));

			// read the output from the command
			String s = null;
			boolean notFound=true;
			while ((s = stdInput.readLine()) != null && notFound) {
				if(s.contains("IP4.ADRESSE")) {
					notFound=false;
					myIP=s.split(":")[1];
					mask=myIP.split("/")[1];
					myIP=myIP.split("/")[0];
				}
			}

		} catch (IOException e) {
			System.out.println("ECHEC");
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("mon ip : "+myIP);
		System.out.println("mon masque : "+mask);
		
		cmd="nmap -sn 132.227.206.0/21";
		String ip="";
		
		try {
			String[] argsMac = { "sh","-c",cmd };
			String[] argsWin = { "cmd.exe", "/C", cmd };
			final Process process1;
			if(windows) 
				process1 = runtime.exec(argsWin);
			
			else 
				process1 = runtime.exec(argsMac);

			process1.waitFor();
			BufferedReader stdInput = new BufferedReader(new 
			     InputStreamReader(process1.getInputStream()));

			// read the output from the command
			String s = null;
			while ((s = stdInput.readLine()) != null) {
				System.out.println(s);
			    if(s.contains(hostname)) {
			    	System.out.println(s);
			    	s=s.split("\\(")[1];
			    	ip=s.substring(0, s.length()-1);
			    	System.out.println("L'IP EST : "+ip);
			    	return ip;		    	
			    }
			}

		} catch (IOException e) {
			System.out.println("ECHEC");
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return ip;
	}
	@Override
	//this function is not used anymore, we used it to calculate an angle to correct the robot's rotation
	public Float captureNewAngle() {
		Float angle=0.f;
		String pfile_returned = null;
		try {
		    //u: case: corner, we assume that there is PEPEER in each corner, we dont need to check all the elements of the DB
			//m: multi, on a random place of a wall, we check all the elements of the DB to verify if one of them matches with the picture taken by the robot
			
		    pfile_returned = executeCommandRPi("python3 imageVanishingLines.py ",true);
			pfile_returned=pfile_returned.trim();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(pfile_returned==null) {
			System.out.println("Erreur capture data");
			return null;
		}
		
		//get the distance from the object if the distance is equal to -1: any object has been detected, else rotate 
		sendToPC("data/angleCorrected",localURL+"/ressources/data/");
		 try (FileReader reader = new FileReader(localURL+"/ressources/data/angleCorrected");
		            BufferedReader br = new BufferedReader(reader)) {
		            String line;
		            //the first line contains the resolution of the image
		           if ((line = br.readLine()) != null) {
		              angle=Float.parseFloat(line);
		           }
		           
		        } catch (IOException e) {
	            System.err.format("IOException: %s%n", e);
	        } 


		 return angle;
	}
	
}
