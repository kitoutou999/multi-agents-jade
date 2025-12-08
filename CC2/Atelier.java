import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;
import java.util.Random;

public class Atelier extends Agent {
	
	private Random random = new Random();
	
	//1000 = 1 sec
	private int lambda1 = 1000; //λ1
	private int lambda2 = 5000; //λ2

	// Put agent initializations here
	protected void setup() {
		// Print a welcome message
		System.out.println("Atelier Créer");
		long temp = getRandomTemps();
		this.addBehaviour(new TickerBehaviour(this, temp) { 
		
			@Override
			protected void onTick(){
				System.out.println("Atelier tick temps = " + getPeriod() + " ms");
				
				long temp = getRandomTemps(); //Créer une nouvelle durée aléatoire
				
				reset(temp); //maj le temps avec le nouveau temps
				
				
			}
		});
	}


	// Put agent clean-up operations here
	protected void takeDown() {
		// Printout a dismissal message
		System.out.println("Atelier fermée");

	}
	
	private long getRandomTemps(){
		return random.nextInt((lambda2 - lambda1) + 1) + lambda1;
	}

}
