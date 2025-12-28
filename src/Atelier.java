import jade.core.AID;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.Random;

public class Atelier extends BaseAgent {

    private Random random = new Random();

    private int idProduit = 1;

    private String nameColor = "\u001B[33mAtelier\u001B[0m ";

    private String GREEN_BACKGROUND = "\u001B[42m";
    private String RED_BACKGROUND = "\u001B[41m";
    private String RESET = "\u001B[0m";

    private ArrayList<Produit> produitsEnCours = new ArrayList<>();
    private ArrayList<Produit> produitsFinis = new ArrayList<>();
    private ArrayList<Produit> produitsEchec = new ArrayList<>();


    protected void setup() {
       
        loadConfig(); 
        registerDF("atelier");

        System.out.println(nameColor + getLocalName() + " prêt. (L1=" + lambda1 + ", L2=" + lambda2 + ")");

        addBehaviour(new FinishProduct());

        // Genère les produit aléatoirement
        long temps = getRandomTemps();
        addBehaviour(new TickerBehaviour(this, temps) {
            @Override
            protected void onTick() {
                Produit newProduit = new Produit("P" + idProduit++);
                int nbCompetences = random.nextInt(nbCompetencesMaxProduit) + 1;
                
                ArrayList<Integer> pool = new ArrayList<>();
                for(int i=1; i<=nbCompetencesTotal; i++) pool.add(i);
                
                for(int i=0; i<nbCompetences; i++) {
                    int index = random.nextInt(pool.size());
                    newProduit.ajoutCompetence(pool.remove(index));
                }

                System.out.println(nameColor+": NOUVEAU PRODUIT : " + newProduit);

                randomGive(newProduit);
                produitsEnCours.add(newProduit);

                long nextTime = getRandomTemps();
                reset(nextTime);
            }
        });
    }

    private void randomGive(Produit p) {
        // Trouver tous les robots
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("robots");
        template.addServices(sd);
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            
            int index = random.nextInt(result.length);
            AID Robot = result[index].getName();
            
            // Envoyer la REQUEST
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.addReceiver(Robot);
            msg.setContentObject(p);
            msg.setConversationId("attribution-produit-" + p.getId());
            send(msg);
            
            System.out.println(nameColor+": Produit " + p.getId() + " envoyé à " + Robot.getLocalName());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private long getRandomTemps() {
        return random.nextInt((lambda2 - lambda1) + 1) + lambda1;
    }

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
                        System.out.println(GREEN_BACKGROUND+nameColor+GREEN_BACKGROUND+": PRODUIT FINI : " + p+RESET);
                        produitsFinis.add(p);
                        produitsEnCours.remove(p);
                    } else if (msg.getPerformative() == ACLMessage.FAILURE) {
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
