import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;

public class Robot extends Agent {

    protected HashMap competences = new HashMap<String, Double>();

	// Put agent initializations here
	protected void setup() {
        // Print a welcome message
		System.out.println("Robot " + this.getAID().getName() + " is ready.");
		this.addBehaviour(new myBehaviour(this, 10000)); //1000 = 1 sec

    


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
			System.out.println("TICK");
		}

	}

}


