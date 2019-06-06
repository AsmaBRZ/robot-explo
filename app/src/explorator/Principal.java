package explorator;
/**
 * 
 * @author Asma BRAZI
 **
 */
import java.io.IOException;

import explorator.Exploration;

public class Principal {
	public static void main (String [] args) throws IOException, InterruptedException{
		//init with the creation of an instance of the class Exploration which will init JME app
		Exploration app = new Exploration();
		app.start();
	}
	
}
