	package Actionners;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import envStructures.Vec2;

/**
 * 
 * @author Clara Rigaud 
 * @author Jehyankaa Jeyarajaratnam
 * @author Asma BRAZI
 */
public class Robot {
	protected Vec2 position;
	protected double rotation;
	protected boolean windows;
	protected boolean mac;
	protected String localURL = "";
	private double cameraFocale;
	public double sensorWidth, sensorHeight; // mm
	public double resolutionWidth, resolutionHeight; // pixels
	protected Vec2 initPosition;
	public Robot(Properties props) {
		this.position = new Vec2(0.0f, 0.0f);
		this.initPosition=new Vec2(0.0f, 0.0f);
		this.rotation = Math.PI / 2;
		this.windows = System.getProperty("os.name").contains("Windows");
		this.mac = System.getProperty("os.name").contains("Mac");
		this.localURL = System.getProperty("user.dir");
		cameraFocale = Double.parseDouble(props.getProperty("focalLength"));
		sensorWidth = Double.parseDouble(props.getProperty("sensorWidth"));
		sensorHeight = Double.parseDouble(props.getProperty("sensorHeight"));
		resolutionWidth = Double.parseDouble(props.getProperty("imageWidth"));
		resolutionHeight = Double.parseDouble(props.getProperty("imageHeight"));
	}
	
	/**
	 * Retrieve the initial position of the robot
	 * @return the initial position of the robot
	 */
	public Vec2 getInitPosition() {
		return initPosition;
	}
	/**
	 * 
	 * @param initPosition modify the initial position of the robot
	 */
	public void setInitPosition(Vec2 initPosition) {
		this.initPosition = initPosition;
	}
	/**
	 * The robot rotates to reach the specified angle
	 * @param angle of rotation
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void rotate(double angle) throws IOException, InterruptedException {
		//update the rotation of the robot according to the angle specified
		this.rotation = -(this.rotation + angle + 2 * Math.PI) % (2 * Math.PI);
	}
	/**
	 * Gives order to the robot to move forward in its current direction from dist meters
	 *The autonomous robot moves forward or backward with a specific distance
	 * @param dist the distance to be traveled 
	 * @return boolean success
	 * @throws IOException
	 * @throws InterruptedException
	 */

	public float move(float dist) throws IOException, InterruptedException {
		this.position = this.position.add(this.getPointer().mul(dist));
		System.out.println("Je veux bouger de " + dist + " m");
		return 0;
	}

/**
 * Retrieve the focal length of the camera
 * @return the focal length of the camera
 */
	public double getFocalLength() {
		return this.cameraFocale;
	}


	/**
	 * Retrieve the rotation of the robot which is between 0.0 and 2.0*PI
	 * @return the current rotation of the robot
	 */
	public double getRotation() {
		return this.rotation;
	}
/**
 * Retrieve the coordinate on the X-axis of the robot
 * @return coordinate on the X-axis
 */
	public float posX() {
		return this.position.x;
	}
	/**
	 * Retrieve the coordinate on the Y-axis of the robot
	 * @return coordinate on the Y-axis
	 */
	public float posY() {
		return this.position.y;
	}

	/**
	 * @return the directional unit vector of the robot calculated with his current rotation
	 */
	public Vec2 getPointer() {
		return new Vec2((float) Math.cos(this.rotation), (float) Math.sin(this.rotation));
	}

	/**
	 * Execute a command on the local PC
	 *
	 * @param cmd
	 * @return boolean that informs if the execution succeed
	 */
	public boolean executeCommand(String cmd) {

		Runtime runtime = Runtime.getRuntime();
		try {
			// WIN 
			//String[] argsLinux = { "sh",cmd };
			String[] argsMac = {"sh", "-c", cmd};
			String[] argsWin = {"cmd.exe", "/C", cmd};
			final Process process1;
			if (windows) {

				process1 = runtime.exec(argsWin);
			} else
				process1 = runtime.exec(argsMac);


			process1.waitFor();
			if (process1.exitValue() == 0) {
				System.out.println("SUCCES EXECUTION : " + cmd);
				return true;
			}

		} catch (IOException e) {
			System.out.println("ECHEC");
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void connect() throws IOException, InterruptedException {
		System.out.println("Robot connected");

	}


	public void disconnect() throws IOException {
		System.out.println("Robot disconnected");

	}
	/**
	 * Retrieve the position of the robot
	 * @return the position of the robot
	 */
	public Vec2 getPosition() {
		return position;
	}
	/**
	 * 
	 * @param position modify the  position of the robot
	 */
	public void setPosition(Vec2 position) {
		this.position = position;
	}
	//Retrieve the visual information captured
	public List<ArrayList<Float>>  captureData() {
		// TODO Auto-generated method stub
		return null;
	}
}