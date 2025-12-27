import jade.core.*;
import jade.domain.*;
import jade.lang.acl.ACLMessage;
import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;
import java.util.HashMap;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class Robot extends Agent {

    protected HashMap<Integer, Double> competences = new HashMap<Integer, Double>();
    protected ArrayList<Produit> listeAttenteTraitement = new ArrayList<Produit>();
    protected Produit produitEnTraitement = null;

    private int nbCompetencesActives;
    private int nbCompetencesTotal;

    protected void setup() {
        System.out.println("Robot " + this.getLocalName() + " is ready.");

        loadConfig();

        attribuerCompetences(nbCompetencesActives, nbCompetencesTotal);

        System.out.println("Robot " + getLocalName() + " compétences : " + this.competences);

        registerDF();
        

        this.addBehaviour(new TickerBehaviour(this, 1000) { 
		
			@Override
			protected void onTick(){
				
				System.out.println("Listeing");
                ACLMessage msg = myAgent.receive();
                if(msg!=null){
                    System.out.println("Recus");
                }
				
				
			}
		});
        
        System.out.println(findRobotsWithCompetence(1));
    }

    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Robot " + this.getLocalName() + " terminating.");
    }

    private void loadConfig() {
        Properties prop = new Properties();
        try {
            FileInputStream input = new FileInputStream("config/config.properties");
            prop.load(input);
            this.nbCompetencesActives = Integer.parseInt(prop.getProperty("nbCompetencesActives", "2"));
            this.nbCompetencesTotal = Integer.parseInt(prop.getProperty("nbCompetencesTotal", "5"));
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void attribuerCompetences(int active, int total) {
        ArrayList<Integer> allCompetences = new ArrayList<>();
        for (int i = 1; i <= total; i++) {
            allCompetences.add(i);
        }
        Collections.shuffle(allCompetences);

        for (int i = 0; i < total; i++) {
            if (i<active){
                double eff = Math.round(Math.random() * 100.0) / 100.0;
                this.competences.put(allCompetences.get(i), eff);
            }else{
                this.competences.put(allCompetences.get(i), 0.0);
            }
            
        }
    }

    private void registerDF() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("robots");
        sd.setName(getLocalName());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    private Integer getNotCompleted(Produit p) {
        if (p.estFini()) return null;
        
        for (Integer comp : p.getCompetences().keySet()) {
            if (p.getCompetences().get(comp) == false) {
                return comp;
            }
        }
        return null;
    }

    private ACLMessage ask(AID AIDTarget, String question){
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(AIDTarget);
        msg.setContent(question);
        send(msg);


        while(true){
            ACLMessage msgr = this.receive();
            if (msgr!=null){
                return msgr;
            }
        }
    }




    private ArrayList<AID> findRobotsWithCompetence(Integer competence) {

            ArrayList<AID> robotsWithCompetence = new ArrayList<>();

          
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("robots");
            template.addServices(sd);
            try{    
                DFAgentDescription[] result = DFService.search(this, template);
                for (DFAgentDescription dfd : result) {
                    AID robotName = dfd.getName();

                    ACLMessage message = ask(robotName,"Comp");

                    //System.out.println("message = "+message);



                
                    robotsWithCompetence.add(robotName);
                }
                
            }catch(FIPAException ex){
                System.out.println(ex);
            }
            
            
           

            return robotsWithCompetence;
    }


    private ArrayList<String> sortRobotsBySpeed(ArrayList<String> robots){
        //TODO 
        return robots;
    }   

    


}


