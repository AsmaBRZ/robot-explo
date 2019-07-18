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
 * @author Asma BRAZI
 *
 */
public class PiThymioRobot extends Robot {
	private static String RPi_id; //id of the Raspberry-Pi
	private static String RPi_password; //the password of the Raspberry-Pi
	private static String RPi_IP; //the address IP of the Raspberry-Pi
	@SuppressWarnings("unused")
	private static String RPi_hostname; 
	@SuppressWarnings("unused") //the host-name of the Raspberry-Pi
	private static String Path_To_SSHPASS; // path to SSH pass
	private final SSHClient ssh=new SSHClient(); // to open an SSH session

	public PiThymioRobot(Properties props) throws UnknownHostException {
		super(props);
		//the following information may be updated according to the material used
		RPi_id = props.getProperty("RPi_id","pi");
		RPi_password = props.getProperty("RPi_password","raspberry");
		RPi_IP = props.getProperty("RPi_IP");
		PiThymioRobot.RPi_hostname = props.getProperty("RPi_hostname","pisma3");
		PiThymioRobot.Path_To_SSHPASS = props.getProperty("Path_To_SSHPASS");
	}
	/**
	 *Order to the robot to take a picture, to analyze it, and to collect the results
	 *the information retrieved may be the height estimated of the wall or the distance to the wall calculated according to the object detected on the wall
	 *@return a list of the camera's resolution, the height of the biggest wall, the dimensions of the target if detected dimensions of eventual walls on left, in front and on the right of the robot
	 */
	@Override 
	public List<ArrayList<Float>> captureData() { 
		//list contains in the order: distance starX starY endX endY resolution
		List<ArrayList<Float>> list=new ArrayList<ArrayList<Float>>();
		ArrayList<Float> tmp;
		String pfile_returned = null;
		try {
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
		sendToPC("data/VisualInfo",localURL+"/ressources/data/");
		try (FileReader reader = new FileReader(localURL+"/ressources/data/VisualInfo");
				BufferedReader br = new BufferedReader(reader)) {
			String line;
			//Retrieve all information captured and store the in a list
			while ((line = br.readLine()) != null) {
				System.out.println(line);
				tmp =new ArrayList<Float>();
				String[] parts = line.split("/");
				for(int i=0;i<parts.length;i++) {
					tmp.add(Float.parseFloat(parts[i]));   
				}
				list.add(tmp);
			}

		} catch (IOException e) {
			System.err.format("IOException: %s%n", e);
		}

		return list;
	}
	/**
	 * 
	 * @return the real distance traveled by the robot while a movement
	 */
	public float updatePosition() {
		float dist=0;
		//receive the real distance traveled by the robot after a movement
		sendToPC("data/distMove",localURL+"/ressources/data/");
		try (FileReader reader = new FileReader(localURL+"/ressources/data/distMove");
				BufferedReader br = new BufferedReader(reader)) {
			String line;
			if((line = br.readLine()) != null) {
				//the distance traveled is in centimeter. the distance is converted to 1/100 centimeter
				dist=Float.parseFloat(line)/100;
				// update the robot's position with the real distance traveled
				this.position = this.position.add(this.getPointer().mul(dist));
			}				            
		} catch (IOException e) {
			System.err.format("IOException: %s%n", e);
		}
		return dist;
	}
	/**
	 * 
	 * @param distanceRob distance in centimeter to be traveled by the robot 
	 * @throws IOException
	 * @throws InterruptedException
	 * @return the real distance traveled. If any obstacle has been encountered, so the robot did not traveled the given distance 
	 */
	public float move(double distanceRob) throws IOException, InterruptedException {
		String cmd="python3 move.py "+Math.abs(distanceRob);
		//if the distance is negative, the robot moves back. We specify this movement by adding the parameter 'r' 
		if(distanceRob<0)
			cmd+=" r ";
		executeCommandRPi(cmd,true);
		//update the robot's position according to the real distance traveled
		float distTravelled=updatePosition();
		return distTravelled;
	}
	/**
	 * @param angle the robot rotates to a specific a angle
	 */
	@Override
	public void rotate(double angle) throws IOException, InterruptedException{
		//this.rotation = (this.rotation + angle + 2 * Math.PI) % (2 * Math.PI);
		double angleRad=(angle*Math.PI)/180;
		// if ok 
		this.rotation = (this.rotation -angleRad) % (2 * Math.PI);
		String cmd="python3 getAsebaFileD.py rotate True True "+Math.abs(angle);
		if(angle<0)
			cmd+=" r ";
		executeCommandRPi(cmd,true);
	}

	/**
	 * Get the object from raspberry pi and saves it in the directory
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
		String[] files= {"move.py","0.png","initWorkspace.py","exploreOnce.py", "utils.py","rotate.py","rotateD.aesl","getAsebaFile.py","getAsebaFileD.py"};

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
}
