import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Classe abstraite servant de base à tous les agents du système (Atelier et Robots).
 * <p>
 * Elle gère :
 * <ul>
 * <li>Le chargement de la configuration depuis un fichier properties.</li>
 * <li>L'enregistrement auprès du Directory Facilitator (DF).</li>
 * <li>Le comptage global des messages envoyés (statistiques).</li>
 * </ul>
 * </p>
 *
 * @author Ton Prenom NOM
 */
public class BaseAgent extends Agent {

    protected Properties config = new Properties();
    
    /** Compteur total des messages envoyés par l'agent. */
    protected int nbMessagesEnvoyes = 0;
    
    // Paramètres de simulation chargés depuis la config
    protected int lambda1; // Temps min arrivée produit
    protected int lambda2; // Temps max arrivée produit
    protected int lambda3; // Temps moyen traitement
    protected int nbCompetencesTotal;
    protected int nbCompetencesActives;
    protected int nbCompetencesMaxProduit;
    
    /**
     * Envoie un message ACL en incrémentant le compteur de statistiques.
     * Cette méthode doit être utilisée à la place de super.send().
     * * @param msg Le message ACLMessage à envoyer.
     */
	public void sendPerso(ACLMessage msg) {
		this.nbMessagesEnvoyes++;
		super.send(msg);
	}

    /**
     * Charge les paramètres de simulation depuis "config/config.properties".
     * Utilise des valeurs par défaut en cas d'erreur de lecture.
     */
    protected void loadConfig() {
        try {
            FileInputStream input = new FileInputStream("config/config.properties");
            config.load(input);
            input.close();

            this.lambda1 = Integer.parseInt(config.getProperty("lambda1", "1000"));
            this.lambda2 = Integer.parseInt(config.getProperty("lambda2", "5000"));
            this.lambda3 = Integer.parseInt(config.getProperty("lambda3", "2000"));
            this.nbCompetencesTotal = Integer.parseInt(config.getProperty("nbCompetencesTotal", "5"));
            this.nbCompetencesActives = Integer.parseInt(config.getProperty("nbCompetencesActives", "2"));
            this.nbCompetencesMaxProduit = Integer.parseInt(config.getProperty("nbCompetencesMaxProduit", "3"));

        } catch (IOException e) {
            System.out.println("Erreur chargement config: " + e.getMessage());
            // Valeurs par défaut
            this.lambda1 = 1000; 
            this.lambda2 = 5000; 
            this.lambda3 = 2000;
            this.nbCompetencesTotal = 5; 
            this.nbCompetencesActives = 2; 
            this.nbCompetencesMaxProduit = 3;
        }
    }

    /**
     * Enregistre l'agent auprès du Directory Facilitator (pages jaunes).
     * * @param serviceType Le type de service fourni (ex: "robots", "atelier").
     */
    protected void registerDF(String serviceType) {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(serviceType);
        sd.setName(getLocalName());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
            System.out.println(getLocalName() + ": Ajout DF : '" + serviceType + "'");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    /**
     * Méthode appelée lors de l'arrêt de l'agent.
     * Affiche le nombre total de messages envoyés et se désinscrit du DF.
     */
    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {}
        System.out.println(getLocalName() + " arrêt. Messages envoyés : " + nbMessagesEnvoyes);
	}
}