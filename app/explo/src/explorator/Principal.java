package explorator;
public class Principal {

	public static void main(String[] args) {
		//initialization by creating  of an instance of the class Exploration which will init JME app
				Exploration app;
				try {
					app = new Exploration();
					app.start();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();

				}	
	}

}
