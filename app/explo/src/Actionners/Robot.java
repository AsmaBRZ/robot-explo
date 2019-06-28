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

	public Robot(Properties props) {
		this.position = new Vec2(0.0f, 0.0f);
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

	public boolean rotate(int speed, int limit) throws IOException, InterruptedException {
		return true;
	}

	public boolean rotate(double angle) throws IOException, InterruptedException {
		System.out.println("Je veux tourner de : " + (angle) / Math.PI * 180);
		// if ok 
		this.rotation = -(this.rotation + angle + 2 * Math.PI) % (2 * Math.PI);
		return true;
	}
	/**
	 * Gives order to robot to move forward in his current orientation from dist meters
	 *
	 * @param dist the distance to be moved
	 * @return boolean success
	 * @throws IOException
	 * @throws InterruptedException
	 */

	public boolean move(float dist) throws IOException, InterruptedException {
		this.position = this.position.add(this.getPointer().mul(dist));
		System.out.println("Je veux bouger de " + dist + " m");
		return true;
	}


	public double getFocalLength() {
		return this.cameraFocale;
	}

	public Vec2 position() {
		return this.position;
	}

	/**
	 * @return actual rotation of robot between 0.0 and 2.0*PI
	 */
	public double rotation() {
		return this.rotation;
	}

	public float posX() {
		return this.position.x;
	}

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
	 * Execute a command on the local pc
	 *
	 * @param cmd
	 * @return boolean that informs if the execution was a success
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

	public String takePic() throws IOException, InterruptedException {
		System.out.println("Taking a pic");
		return "1546120907.jpg";

	}

	public Vec2 getPosition() {
		return position;
	}

	public void setPosition(Vec2 position) {
		this.position = position;
	}

	public float analysePic() {
		return 0;
	}

	public void calculateAngle(){

    }

	public List<ArrayList<Float>>  captureData() {
		// TODO Auto-generated method stub
		return null;
	}
	public Float captureNewAngle() {
		// TODO Auto-generated method stub
		return 0.f;
	}

}