import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;
import java.util.Random;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

public class Atelier extends Agent {
	
	private Random random = new Random();
	
	private int lambda1 = 1000;
	private int lambda2 = 5000;
    private int nbCompetencesActives = 2;
    private int nbCompetencesTotal = 5;
	
	private int idProduit = 1;
	

	// Put agent initializations here
	protected void setup() {
        // Chargement de la configuration
        try {
            Properties prop = new Properties();
            FileInputStream input = new FileInputStream("config.properties");
            prop.load(input);
            
            this.lambda1 = Integer.parseInt(prop.getProperty("lambda1", "1000"));
            this.lambda2 = Integer.parseInt(prop.getProperty("lambda2", "5000"));
            this.nbCompetencesActives = Integer.parseInt(prop.getProperty("nbCompetencesActives", "2"));
            this.nbCompetencesTotal = Integer.parseInt(prop.getProperty("nbCompetencesTotal", "5"));
            
            input.close();
            System.out.println("Atelier config chargée: L1=" + lambda1 + " L2=" + lambda2 + " Act=" + nbCompetencesActives + " Tot=" + nbCompetencesTotal);
            
        } catch (IOException ex) {
            System.out.println("Atelier: config.properties non trouvé, utilisation des valeurs par défaut.");
        }

		System.out.println("Atelier Créé");
		long temp = getRandomTemps();
		this.addBehaviour(new TickerBehaviour(this, temp) { 
		
			@Override
			protected void onTick(){
				
				//Créer un nouveau Produit à envoyer
				Produit newProduit = new Produit("P" + idProduit);
				idProduit++;
				
				System.out.println("Atelier tick temps = " + getPeriod() + " ms viens de créer le produit " + newProduit.getId());
				
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
