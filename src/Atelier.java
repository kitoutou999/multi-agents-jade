import jade.core.AID;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.Random;

/**
 * Agent Atelier responsable de la génération des produits et du suivi de la production.
 * <p>
 * Il génère des produits selon une loi uniforme (lambda1, lambda2) et les attribue
 * aléatoirement à un robot pour initier le processus.
 * Il collecte également les statistiques sur les produits finis (moyenne d'échecs).
 * </p>
 * * @author Ton Prenom NOM
 */
public class Atelier extends BaseAgent {

    private Random random = new Random();
    
    // Statistiques globales
    private double sommeEchecs = 0;
	private int nombreProduitsFinis = 0;

    private int idProduit = 1;

    // Codes couleurs pour la console
    private String nameColor = "\u001B[33mAtelier\u001B[0m ";
    private String GREEN_BACKGROUND = "\u001B[42m";
    private String RED_BACKGROUND = "\u001B[41m";
    private String RESET = "\u001B[0m";

    private ArrayList<Produit> produitsEnCours = new ArrayList<>();
    private ArrayList<Produit> produitsFinis = new ArrayList<>();
    private ArrayList<Produit> produitsEchec = new ArrayList<>();


    /**
     * Initialisation de l'agent Atelier.
     * Lance le comportement de génération de produits et d'écoute des produits finis.
     */
    protected void setup() {
       
        loadConfig(); 
        registerDF("atelier");

        System.out.println(nameColor + getLocalName() + " prêt. (L1=" + lambda1 + ", L2=" + lambda2 + ")");

        // Comportement d'écoute des retours
        addBehaviour(new FinishProduct());

        // Comportement de génération aléatoire
        long temps = getRandomTemps();
        addBehaviour(new TickerBehaviour(this, temps) {
            @Override
            protected void onTick() {
                // Création d'un nouveau produit
                Produit newProduit = new Produit("P" + idProduit++);
                int nbCompetences = random.nextInt(nbCompetencesMaxProduit) + 1;
                
                ArrayList<Integer> pool = new ArrayList<>();
                for(int i=1; i<=nbCompetencesTotal; i++) pool.add(i);
                
                // Attribution aléatoire des compétences requises
                for(int i=0; i<nbCompetences; i++) {
                    int index = random.nextInt(pool.size());
                    newProduit.ajoutCompetence(pool.remove(index));
                }

                System.out.println(nameColor+": NOUVEAU PRODUIT : " + newProduit);

                randomGive(newProduit);
                produitsEnCours.add(newProduit);

                // Reprogrammation du prochain tick (loi uniforme)
                long nextTime = getRandomTemps();
                reset(nextTime);
            }
        });
    }

    /**
     * Transfère un produit nouvellement créé à un robot choisi au hasard.
     * Ce robot sera chargé de lancer la première enchère.
     * * @param p Le produit à injecter dans le système.
     */
    private void randomGive(Produit p) {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("robots");
        template.addServices(sd);
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            
            if (result.length > 0) {
                int index = random.nextInt(result.length);
                AID Robot = result[index].getName();
                
                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.addReceiver(Robot);
                msg.setContentObject(p);
                msg.setConversationId("attribution-produit-" + p.getId());
                sendPerso(msg);
                
                System.out.println(nameColor+": Produit " + p.getId() + " envoyé à " + Robot.getLocalName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Calcule un temps d'attente aléatoire entre lambda1 et lambda2.
     * @return Le temps en millisecondes.
     */
    private long getRandomTemps() {
        return random.nextInt((lambda2 - lambda1) + 1) + lambda1;
    }

    /**
     * Comportement cyclique attendant les produits finis (INFORM) ou échoués (FAILURE).
     * Met à jour les statistiques globales.
     */
    private class FinishProduct extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.or(
                MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchPerformative(ACLMessage.FAILURE)
            );

            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                try {
                    if (msg.getPerformative() == ACLMessage.INFORM) {
						Produit p = (Produit) msg.getContentObject();
						
						// Mise à jour des statistiques
						nombreProduitsFinis++;
						sommeEchecs += p.getNbEchecs();
						double moyenne = sommeEchecs / nombreProduitsFinis;

						System.out.println(GREEN_BACKGROUND + nameColor + GREEN_BACKGROUND + ": FINI : " + p.getId() + " (Moyenne échecs global: " + String.format("%.2f", moyenne) + ")" + RESET);
						produitsFinis.add(p);
						produitsEnCours.remove(p);
					}
					else if (msg.getPerformative() == ACLMessage.FAILURE) {
                        Produit p = (Produit) msg.getContentObject();
                        System.out.println(RED_BACKGROUND+nameColor+RED_BACKGROUND+": ECHEC DE FABRICATION : " + p+RESET);
                        produitsEchec.add(p);
                        produitsEnCours.remove(p);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                block();
            }
        }
    }
}