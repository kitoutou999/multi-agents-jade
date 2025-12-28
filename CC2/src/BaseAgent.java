import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class BaseAgent extends Agent {

    protected Properties config = new Properties();
    
    protected int lambda1;
    protected int lambda2;
    protected int lambda3;
    protected int nbCompetencesTotal;
    protected int nbCompetencesActives;
    protected int nbCompetencesMaxProduit;

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
            System.out.println(e.getMessage());
            this.lambda1 = 1000; 
            this.lambda2 = 5000; 
            this.lambda3 = 2000;
            this.nbCompetencesTotal = 5; 
            this.nbCompetencesActives = 2; 
            this.nbCompetencesMaxProduit = 3;
        }
    }

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

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {}
        System.out.println(getLocalName() + " arrêt.");
    }
}
