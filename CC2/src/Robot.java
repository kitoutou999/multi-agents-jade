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

public class Robot extends BaseAgent {

    protected HashMap<Integer, Double> competences = new HashMap<>();
    
    protected ArrayList<Produit> fileAttente = new ArrayList<>();
    
    protected Produit produitEnTraitement = null;
    protected boolean isWorking = false;
    protected long finTraitementPrevue = 0;

    public String RED = "\u001B[31m";
    public String GREEN = "\u001B[32m";
    public String CYAN = "\u001B[36m";
    public String RESET = "\u001B[0m";
    public String ORANGE = "\u001B[38;5;208m";
    public String REDBACK = "\u001B[41m";

    protected void setup() {
        loadConfig(); 
        attribuerCompetences();
        registerDF("robots"); 

        System.out.println(getLocalName() + " : " + "Compétences: " + formatCompetences());

        //Boite aux lettres
        addBehaviour(new MailboxBehaviour());

        //Worker
        addBehaviour(new TraitementBehaviour(this, 100));
    }
    

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

    private Integer getNextComp(Produit p) {
        for (Map.Entry<Integer, Boolean> entry : p.getCompetences().entrySet()) {
            if (!entry.getValue()) 
                return entry.getKey();
        }
        return null;
    }

    private double calculateMakespan(Integer competenceRequise) {
        if (!competences.containsKey(competenceRequise)) return Double.MAX_VALUE;

        double myEff = competences.get(competenceRequise);
        double expectedTimeForOneTask = lambda3 * (1.0 + ((1.0 - myEff) / myEff));

        double timeQueue = 0;
        for (Produit p : fileAttente) {
            Integer comp = getNextComp(p);
            if (comp != null && competences.containsKey(comp)) {
                 double eff = competences.get(comp);
                 timeQueue += lambda3 * (1.0 + ((1.0 - eff) / eff));
            } else {
                timeQueue += lambda3; 
            }
        }
        if (isWorking) {
            timeQueue += (finTraitementPrevue - System.currentTimeMillis()); 
        }
        return timeQueue + expectedTimeForOneTask;
    }

    private void traiterArriveeProduit(Produit p) {
        Integer comp = getNextComp(p);
        if (comp != null && competences.containsKey(comp)) {
            // Sait faire 
            fileAttente.add(p);
            System.out.println(getLocalName() + ": J'accepte " + p.getId() + " (Compétence " + comp + "). Ajouté en file.");
        } else {
            // Sait pas faire 
            System.out.println(ORANGE + getLocalName() + ": Reçu " + p.getId() + " mais je ne sais pas faire la compétence " + comp + ". Je lance une enchère." + RESET);
            addBehaviour(new EnchereBehaviour(p,false));
        }
    }

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
                    case ACLMessage.CFP: // Si a la comp alors repond makespan sinon refuse l'enchere
                        handleCfp(msg);
                        break;
                    case ACLMessage.REQUEST: // Recu du produit de l'atelier uniquement
                        handleRequest(msg);
                        break;
                    case ACLMessage.ACCEPT_PROPOSAL: // Recu produit de l'enchere (win)
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


    private void handleCfp(ACLMessage msg) {
        try {
            Produit p = (Produit) msg.getContentObject();
            Integer comp = getNextComp(p);

            if (comp != null && competences.containsKey(comp)) {
                double makespan = calculateMakespan(comp);
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.PROPOSE);
                reply.setContent(String.valueOf(makespan));
                send(reply);
            } else {
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.REFUSE);
                send(reply);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleRequest(ACLMessage msg) {
        try {
            Produit p = (Produit) msg.getContentObject();
            System.out.println(getLocalName() + ": Reçu demande directe de l'Atelier pour " + p.getId());
            traiterArriveeProduit(p);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleAcceptProposal(ACLMessage msg) {
        try {
            Produit p = (Produit) msg.getContentObject();
            traiterArriveeProduit(p); 
        } catch (Exception e) { e.printStackTrace(); }
    }

    //Boucle de traitement des produits
    private class TraitementBehaviour extends TickerBehaviour {
        public TraitementBehaviour(Agent a, long period) { super(a, period); }

        @Override
        protected void onTick() {
            if (!isWorking && !fileAttente.isEmpty()) {
                produitEnTraitement = fileAttente.remove(0);
                isWorking = true;
                finTraitementPrevue = System.currentTimeMillis() + lambda3;
                System.out.println(getLocalName() + ": Travaille sur " + produitEnTraitement.getId());
            }

            if (isWorking) {
                if (System.currentTimeMillis() >= finTraitementPrevue) { 
                    Integer comp = getNextComp(produitEnTraitement);
                    double eff = competences.getOrDefault(comp, 0.0); 
                    
                    if (Math.random() < eff) {
                        produitEnTraitement.valideLaCompetence(comp);
                        System.out.println(GREEN+getLocalName() + ": SUCCÈS sur " + produitEnTraitement.getId()+RESET);
                        
                        if (produitEnTraitement.estFini()) {
                            sendAtelier(produitEnTraitement,true);
                        } else {
                            System.out.println(getLocalName() + ": " + produitEnTraitement.getId() + " pas fini. Je cherche suite.");
                            if (competences.containsKey(getNextComp(produitEnTraitement))) {
                                myAgent.addBehaviour(new EnchereBehaviour(produitEnTraitement,true));
                            }else {
                                myAgent.addBehaviour(new EnchereBehaviour(produitEnTraitement,false));
                            }
                            
                        }
                        
                        isWorking = false;
                        produitEnTraitement = null;
                    } else {
                        System.out.println(RED+getLocalName() + ": ÉCHEC sur " + produitEnTraitement.getId() + ". Retry."+RESET);
                        finTraitementPrevue = System.currentTimeMillis() + lambda3;
                    }
                }
            }
        }
    }

    private void sendAtelier(Produit p,boolean success) {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("atelier");
        template.addServices(sd);
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            ACLMessage msg;
            
            if (success){
                msg = new ACLMessage(ACLMessage.INFORM);
            }else{
                msg = new ACLMessage(ACLMessage.FAILURE);
            }

            msg.addReceiver(result[0].getName());
            msg.setContentObject(p);
            send(msg);
            System.out.println(getLocalName() + ": Renvoi " + p.getId() + " fini à l'Atelier.");
            
        } catch (Exception e) { e.printStackTrace(); }
    }
    // Comportement d'enchère
    private class EnchereBehaviour extends Behaviour {
        private Produit produit;
        private int step = 0;
        private MessageTemplate mt;
        private ArrayList<AID> robotsParticipants = new ArrayList<>();
        private int repliesCnt = 0;
        private AID bestRobot = null;
        private double bestTime = Double.MAX_VALUE;
        private long timeout = 5000;
        private long startTime;
        private Boolean saisFaire;

        public EnchereBehaviour(Produit p, Boolean saisFaire) { 
            this.produit = p; 
            this.saisFaire = saisFaire;
        }

        @Override
        public void action() {
            switch (step) {
                case 0:
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("robots");
                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                        
                        for (DFAgentDescription agent : result) {
                            AID aid = agent.getName();

                            // Si j'ai la comp, j'invite tout le monde.
                            // Sinon j'invite tout le monde sauf moi.
                            if (saisFaire || !aid.equals(myAgent.getAID())) {
                                cfp.addReceiver(aid);
                                robotsParticipants.add(aid);
                            }
                        }
                        
                        if(robotsParticipants.isEmpty()) {
                             System.out.println(CYAN+myAgent.getLocalName() + ": Pas de collègues trouvés !"+RESET);
                             sendAtelier(produit, false);
                             step = 3; // Fin
                             return;
                        }

                        cfp.setContentObject(produit);
                        cfp.setConversationId("enchere-" + produit.getId() + "-" + System.currentTimeMillis());
                        cfp.setReplyWith("cfp" + System.currentTimeMillis());
                        myAgent.send(cfp);
                        
                        mt = MessageTemplate.and(
                                MessageTemplate.MatchConversationId(cfp.getConversationId()),
                                MessageTemplate.MatchInReplyTo(cfp.getReplyWith())
                        );
                        step = 1;
                        startTime = System.currentTimeMillis();
                        System.out.println(CYAN+myAgent.getLocalName() + ": Enchère lancée pour " + produit.getId()+RESET);
                    } catch (Exception e) { e.printStackTrace(); }
                    break;

                case 1:
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        repliesCnt++;
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            try {
                                double proposalTime = Double.parseDouble(reply.getContent());
                                if (proposalTime < bestTime) {
                                    bestTime = proposalTime;
                                    bestRobot = reply.getSender();
                                }
                            } catch (Exception e) {}
                        }
                        if (repliesCnt >= robotsParticipants.size()) step = 2;
                    } else {
                        if(System.currentTimeMillis() - startTime > timeout) step = 2;
                        else block();
                    }
                    break;

                case 2:
                    if (bestRobot != null) {
                        ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                        order.addReceiver(bestRobot);
                        try {
                            order.setContentObject(produit);
                            order.setConversationId("enchere-" + produit.getId() + "-" + System.currentTimeMillis());
                            myAgent.send(order);
                            System.out.println(CYAN+myAgent.getLocalName() + ": " + produit.getId() + " attribué à " + bestRobot.getLocalName()+RESET);
                        } catch (IOException e) { e.printStackTrace(); }
                    } else {
                        System.out.println(REDBACK+myAgent.getLocalName() + ": Échec enchère pour " + produit.getId() + "."+RESET);
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
