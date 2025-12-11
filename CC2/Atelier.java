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
	
	private int lambda1;
	private int lambda2;
	private int nbCompetencesMaxProduit;
	private int nbCompetencesTotal;
    
	
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
            
            this.nbCompetencesTotal = Integer.parseInt(prop.getProperty("nbCompetencesTotal", "10"));
            this.nbCompetencesMaxProduit = Integer.parseInt(prop.getProperty("nbCompetencesMaxProduit", "3"));
            
            input.close();
            registerDF();
            System.out.println("Atelier config chargée: L1=" + lambda1 + " L2=" + lambda2);
            
        } catch (IOException ex) {
            System.out.println("Atelier: config.properties non trouvé, utilisation des valeurs par défaut.");
        }

		System.out.println("Atelier Créé");
		long temps = getRandomTemps();
		this.addBehaviour(new TickerBehaviour(this, temps) { 
		
			@Override
			protected void onTick(){
				
				//Créer un nouveau Produit à envoyer
				Produit newProduit = new Produit("P" + idProduit);
				int nbCompetencesProduit = random.nextInt(nbCompetencesMaxProduit) + 1;
				System.out.println("nbCompetencesProduit " + nbCompetencesProduit);
				
				for(int i = 0;i < nbCompetencesProduit;i++){
					int competance = random.nextInt(nbCompetencesTotal) + 1;
					newProduit.ajoutCompetence(competance);
				}
				
				idProduit++;
				System.out.println("Atelier tick temps = " + getPeriod() + " ms viens de créer le produit " + newProduit.getId());
				
				//Donne le produit créer à un agent aléatoirement choisi
				
				
				
				
				
				long temps = getRandomTemps(); //Créer une nouvelle durée aléatoire
				reset(temps); //maj le temps avec le nouveau temps
				
				
			}
		});
	}


	// Put agent clean-up operations here
	protected void takeDown() {
		// Printout a dismissal message
		System.out.println("Atelier fermée");

	}
	
	//Donne un temps entre lambda2 et lambda1
	private long getRandomTemps(){
		return random.nextInt((lambda2 - lambda1) + 1) + lambda1;
	}
	
	//S'enregistre dans les pages jaunes
	private void registerDF() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("atelier");
        sd.setName(getLocalName());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

}
