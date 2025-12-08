import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;

public class playerAgent extends Agent {

	// Put agent initializations here
	protected void setup() {
		// Print a welcome message
		enregistrePageJaune();
		System.out.println("Hello! Agent " + this.getAID().getName() + "is ready.");
		this.addBehaviour(new myBehaviour(this, 10000)); //1000 = 1 sec
	}
	
	protected void enregistrePageJaune() {
		//Creer une description de l'agent pour les pages jaunes
		DFAgentDescription dfd = new DFAgentDescription(); 
		dfd.setName(this.getAID());
		
		//Creer un service pour la description
		ServiceDescription sd = new ServiceDescription();
		
		//Faire en sorte qu'il demande son équipe a un temp t aléatoire de sorte que 
		int num_team = 1;
		sd.setType("Team" + num_team);
		dfd.addServices(sd);
		
		//Tente d'ajouter la description de l'agent aux pages jaunes
		try {
			DFService.register(this, dfd);
 		}
		catch (FIPAException fe) {
 			fe.printStackTrace();
 		}

	}


	// Put agent clean-up operations here
	protected void takeDown() {
		// Printout a dismissal message
		System.out.println("Agent " + this.getAID().getName() + "terminating.");

	}
	
	private class myBehaviour extends TickerBehaviour {

		public myBehaviour(Agent a, long period) {
			super(a, period);
		}

		protected void onTick() {
			System.out.println("MOI JOUE");
		}

	}

}
