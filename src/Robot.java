import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Agent Robot capable d'effectuer des tâches sur des produits.
 * <p>
 * Chaque robot possède un ensemble de compétences avec un degré d'efficacité.
 * Il implémente :
 * <ul>
 * <li>Le protocole d'enchères (Contract Net) pour s'attribuer les tâches.</li>
 * <li>Le calcul du Makespan (temps espéré de traitement).</li>
 * <li>La gestion des pannes et des échecs probabilistes.</li>
 * </ul>
 * </p>
 * * @author Tom DAVID
 * * @author Titouan PASQUIER
 */
public class Robot extends BaseAgent {

    /** Compétences du robot : ID -> Efficacité [0.0 - 1.0] */
    protected HashMap<Integer, Double> competences = new HashMap<>();
    
    /** File d'attente des produits à traiter */
    protected ArrayList<Produit> fileAttente = new ArrayList<>();
    
    // État du worker
    protected Produit produitEnTraitement = null;
    protected boolean isWorking = false;
    protected long finTraitementPrevue = 0;

    // Codes couleurs console
    public String RED = "\u001B[31m";
    public String GREEN = "\u001B[32m";
    public String CYAN = "\u001B[36m";
    public String RESET = "\u001B[0m";
    public String ORANGE = "\u001B[38;5;208m";
    public String REDBACK = "\u001B[41m";

    /**
     * Initialisation du robot.
     * Attribue les compétences, s'enregistre au DF et lance les comportements.
     */
    protected void setup() {
        loadConfig(); 
        attribuerCompetences();
        registerDF("robots"); 

        System.out.println(getLocalName() + " : " + "Compétences: " + formatCompetences());

        // Boite aux lettres pour gestion des messages entrants
        addBehaviour(new MailboxBehaviour());

        // Worker simulant le travail
        addBehaviour(new TraitementBehaviour(this, 100));
    }
    
    /**
     * Attribue aléatoirement des compétences au robot lors du démarrage.
     */
    private void attribuerCompetences() {
        ArrayList<Integer> allCompetences = new ArrayList<>();
        for (int i = 1; i <= nbCompetencesTotal; i++) allCompetences.add(i);
        Collections.shuffle(allCompetences);

        for (int i = 0; i < nbCompetencesActives; i++) {
            double eff = Math.round((0.1 + Math.random() * 0.9) * 100.0) / 100.0;
            this.competences.put(allCompetences.get(i), eff);
        }
    }

    private String formatCompetences() {
        String comp = "{";
        for (Map.Entry<Integer, Double> entry : competences.entrySet()) {
            comp += entry.getKey() + ":" + entry.getValue() + " ";
        }
        comp += "}";
        return comp;
    }

    /**
     * Trouve la prochaine compétence non réalisée requise par un produit.
     * @param p Le produit.
     * @return L'ID de la compétence, ou null si fini.
     */
    private Integer getNextComp(Produit p) {
        for (Map.Entry<Integer, Boolean> entry : p.getCompetences().entrySet()) {
            if (!entry.getValue()) 
                return entry.getKey();
        }
        return null;
    }

    /**
     * Calcule le "Makespan" (temps espéré) pour traiter une compétence.
     * <p>
     * Formule : Temps file d'attente + Temps traitement * (1 + Echecs espérés).
     * </p>
     * @param competenceRequise La compétence à évaluer.
     * @return Le temps estimé en ms.
     */
    private double calculateMakespan(Integer competenceRequise) {
        if (!competences.containsKey(competenceRequise)) return Double.MAX_VALUE;

        double myEff = competences.get(competenceRequise);
        // Temps espéré pour une tâche (loi géométrique des échecs)
        double expectedTimeForOneTask = lambda3 * (1.0 + ((1.0 - myEff) / myEff));

        double timeQueue = 0;
        // Ajout du temps estimé pour tous les produits en file d'attente
        for (Produit p : fileAttente) {
            Integer comp = getNextComp(p);
            if (comp != null && competences.containsKey(comp)) {
                 double eff = competences.get(comp);
                 timeQueue += lambda3 * (1.0 + ((1.0 - eff) / eff));
            } else {
                timeQueue += lambda3; 
            }
        }
        // Ajout du temps restant sur la tâche courante
        if (isWorking) {
            timeQueue += (finTraitementPrevue - System.currentTimeMillis()); 
        }
        return timeQueue + expectedTimeForOneTask;
    }

    /**
     * Ajoute le produit à la file d'attente locale après avoir remporté une enchère.
     * Si la compétence n'est pas maitrisée (cas d'erreur), relance une enchère.
     * @param p Le produit à traiter.
     */
    private void traiterArriveeProduit(Produit p) {
        Integer comp = getNextComp(p);
        if (comp != null && competences.containsKey(comp)) {
            // Sait faire -> Ajout en file
            fileAttente.add(p);
            System.out.println(getLocalName() + ": J'accepte " + p.getId() + " (Compétence " + comp + "). Ajouté en file.");
        } else {
            // Ne sait pas faire -> Erreur de routage -> Nouvelle enchère
            System.out.println(ORANGE + getLocalName() + ": Reçu " + p.getId() + " mais je ne sais pas faire la compétence " + comp + ". Je lance une enchère." + RESET);
            addBehaviour(new EnchereBehaviour(p, false));
        }
    }

    /**
     * Behaviour gérant la réception des messages ACL.
     * Agit comme un routeur vers les méthodes de gestion spécifiques (CFP, REQUEST, etc).
     */
    private class MailboxBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.or(
                MessageTemplate.MatchPerformative(ACLMessage.CFP),
                MessageTemplate.or(
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL)
                )
            );
            
            ACLMessage msg = myAgent.receive(mt);
            
            if (msg != null) {
                switch (msg.getPerformative()) {
                    case ACLMessage.CFP: // Appel d'offre reçu
                        handleCfp(msg);
                        break;
                    case ACLMessage.REQUEST: // Demande initiale de l'atelier
                        handleRequest(msg);
                        break;
                    case ACLMessage.ACCEPT_PROPOSAL: // Enchère gagnée
                        handleAcceptProposal(msg);
                        break;
                    default:
                        break;
                }
            } else {
                block();
            }
        }
    }

    /**
     * Traite un Appel d'Offre (CFP).
     * Calcule le makespan et répond par PROPOSE ou REFUSE.
     */
    private void handleCfp(ACLMessage msg) {
        try {
            Produit p = (Produit) msg.getContentObject();
            Integer comp = getNextComp(p);

            if (comp != null && competences.containsKey(comp)) {
                double makespan = calculateMakespan(comp);
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.PROPOSE);
                reply.setContent(String.valueOf(makespan));
                sendPerso(reply);
            } else {
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.REFUSE);
                sendPerso(reply);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Traite une requête directe de l'atelier.
     * Lance systématiquement une enchère pour trouver le meilleur exécutant (soi-même ou un autre).
     */
    private void handleRequest(ACLMessage msg) {
        try {
            Produit p = (Produit) msg.getContentObject();
            System.out.println(getLocalName() + ": Reçu demande de l'Atelier pour " + p.getId() + ". Lancement enchère.");

            Integer comp = getNextComp(p);
            boolean jeSaisFaire = (comp != null && competences.containsKey(comp));

            addBehaviour(new EnchereBehaviour(p, jeSaisFaire));

        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Traite la notification de victoire d'enchère.
     */
    private void handleAcceptProposal(ACLMessage msg) {
        try {
            Produit p = (Produit) msg.getContentObject();
            traiterArriveeProduit(p); 
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Behaviour simulant le travail du robot (Ticker).
     * Gère le temps de traitement lambda3 et les probabilités d'échec.
     */
    private class TraitementBehaviour extends TickerBehaviour {
        public TraitementBehaviour(Agent a, long period) { super(a, period); }

        @Override
        protected void onTick() {
            // Si libre et file non vide, on commence un travail
            if (!isWorking && !fileAttente.isEmpty()) {
                produitEnTraitement = fileAttente.remove(0);
                isWorking = true;
                finTraitementPrevue = System.currentTimeMillis() + lambda3;
                System.out.println(getLocalName() + ": Travaille sur " + produitEnTraitement.getId());
            }

            // Vérification de fin de tâche
            if (isWorking) {
                if (System.currentTimeMillis() >= finTraitementPrevue) { 
                    Integer comp = getNextComp(produitEnTraitement);
                    double eff = competences.getOrDefault(comp, 0.0); 
                    
                    // Simulation Succès / Echec
                    if (Math.random() < eff) {
                        produitEnTraitement.valideLaCompetence(comp);
                        System.out.println(GREEN+getLocalName() + ": SUCCÈS sur " + produitEnTraitement.getId()+RESET);
                        
                        if (produitEnTraitement.estFini()) {
                            sendAtelier(produitEnTraitement, true);
                        } else {
                            System.out.println(getLocalName() + ": " + produitEnTraitement.getId() + " pas fini. Je cherche suite.");
                            // Si j'ai la compétence suivante, je peux participer à l'enchère
                            if (competences.containsKey(getNextComp(produitEnTraitement))) {
                                myAgent.addBehaviour(new EnchereBehaviour(produitEnTraitement, true));
                            } else {
                                myAgent.addBehaviour(new EnchereBehaviour(produitEnTraitement, false));
                            }
                        }
                        isWorking = false;
                        produitEnTraitement = null;
                    } else {
                        // ÉCHEC
                        System.out.println(RED+getLocalName() + ": ÉCHEC sur " + produitEnTraitement.getId() + ". Retry."+RESET);
                        produitEnTraitement.incrementerEchecs(); // Incrémentation du compteur d'échecs
                        finTraitementPrevue = System.currentTimeMillis() + lambda3;
                    }
                }
            }
        }
    }

    /**
     * Renvoie le produit fini (ou échoué définitivement) à l'atelier.
     */
    private void sendAtelier(Produit p, boolean success) {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("atelier");
        template.addServices(sd);
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            ACLMessage msg;
            
            if (success){
                msg = new ACLMessage(ACLMessage.INFORM);
            } else {
                msg = new ACLMessage(ACLMessage.FAILURE);
            }

            msg.addReceiver(result[0].getName());
            msg.setContentObject(p);
            sendPerso(msg);
            System.out.println(getLocalName() + ": Renvoi " + p.getId() + " fini à l'Atelier.");
            
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Behaviour gérant le processus d'enchère (Contract Net Protocol simplifié).
     * Étapes :
     * 1. Envoi CFP à tous les robots.
     * 2. Réception des PROPOSE (temps makespan).
     * 3. Sélection du min(makespan) et envoi ACCEPT_PROPOSAL.
     */
    private class EnchereBehaviour extends Behaviour {
		private Produit produit;
		private int step = 0;
		private MessageTemplate mt;
		private int repliesCnt = 0;
		private int expectedReplies = 0;

		private AID bestRobot = null;
		private double bestTime = Double.MAX_VALUE;
		
		private long startTime;
		private boolean jePeuxLeFaireMoiMeme;
		private double monPropreTemps = Double.MAX_VALUE;

		public EnchereBehaviour(Produit p, boolean saisFaire) { 
			this.produit = p; 
			this.jePeuxLeFaireMoiMeme = saisFaire;
		}

		@Override
		public void action() {
			switch (step) {
				case 0: // Préparation et envoi CFP
					// 1. Calculer mon propre temps si je sais faire
					if (jePeuxLeFaireMoiMeme) {
						Integer comp = getNextComp(produit);
						if (comp != null) {
							monPropreTemps = calculateMakespan(comp);
							// Par défaut je suis le meilleur candidat
							bestTime = monPropreTemps;
							bestRobot = myAgent.getAID(); 
							System.out.println(getLocalName() + ": Je candidate à ma propre enchère (Best temp =" + String.format("%.2f", bestTime) + ")");
						}
					}

					// 2. Trouver les autres robots
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					sd.setType("robots");
					template.addServices(sd);
					
					try {
						DFAgentDescription[] result = DFService.search(myAgent, template);
						ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
						
						for (DFAgentDescription agent : result) {
							AID aid = agent.getName();
							// Ne pas envoyer le message à soi-même (déjà calculé localement)
							if (!aid.equals(myAgent.getAID())) {
								cfp.addReceiver(aid);
								expectedReplies++;
							}
						}
						
						if (expectedReplies > 0) {
							cfp.setContentObject(produit);
							cfp.setConversationId("enchere-" + produit.getId() + "-" + System.currentTimeMillis());
							cfp.setReplyWith("cfp" + System.currentTimeMillis());
							sendPerso(cfp); // Utilisation de sendPerso
							
							mt = MessageTemplate.and(
									MessageTemplate.MatchConversationId(cfp.getConversationId()),
									MessageTemplate.MatchInReplyTo(cfp.getReplyWith())
							);
							startTime = System.currentTimeMillis();
							step = 1;
						} else {
							// Personne d'autre n'existe
							step = 2; 
						}
					} catch (Exception e) { e.printStackTrace(); }
					break;

				case 1: // Réception des offres
					ACLMessage reply = myAgent.receive(mt);
					if (reply != null) {
						if (reply.getPerformative() == ACLMessage.PROPOSE) {
							try {
								double proposalTime = Double.parseDouble(reply.getContent());
								// Si le robot distant est meilleur que le meilleur temps actuel
								if (proposalTime < bestTime) {
									bestTime = proposalTime;
									bestRobot = reply.getSender();
								}
							} catch (Exception e) {}
						}
						repliesCnt++;
						if (repliesCnt >= expectedReplies) step = 2;
					} else {
						if (System.currentTimeMillis() - startTime > 5000) step = 2; // Timeout
						else block();
					}
					break;

				case 2: // Décision
					// Cas 1: Je suis le meilleur (ou je suis le seul capable)
					if (bestRobot != null && bestRobot.equals(myAgent.getAID())) {
						System.out.println(GREEN + getLocalName() + ": Je remporte l'enchère pour " + produit.getId() + " (Best temp: " + String.format("%.2f", bestTime) + ")" + RESET);
						fileAttente.add(produit);
					} 
					// Cas 2: Un autre robot est meilleur
					else if (bestRobot != null) {
						ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
						order.addReceiver(bestRobot);
						try {
							order.setContentObject(produit);
							order.setConversationId("enchere-attribution");
							sendPerso(order); // Utilisation de sendPerso
							System.out.println(CYAN + getLocalName() + ": Délégation de " + produit.getId() + " à " + bestRobot.getLocalName() + " (Best temp: " + String.format("%.2f", bestTime) + ")" + RESET);
						} catch (IOException e) { e.printStackTrace(); }
					} 
					// Cas 3: Personne ne sait faire (y compris moi)
					else {
						System.out.println(REDBACK + getLocalName() + ": Personne ne peut traiter " + produit.getId() + RESET);
						sendAtelier(produit, false);
					}
					step = 3;
					break;
			}
		}

		@Override
		public boolean done() { return step == 3; }
	}
}
