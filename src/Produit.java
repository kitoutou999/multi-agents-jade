import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Représente un produit circulant dans l'atelier.
 * <p>
 * Un produit est défini par une liste de compétences (tâches) à réaliser.
 * Il conserve l'historique de son état (tâches faites/à faire) et le nombre d'échecs rencontrés.
 * </p>
 * Implémente Serializable pour pouvoir être envoyé dans des messages ACL.
 *
 * @author Ton Prenom NOM
 */
public class Produit implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /** Identifiant unique du produit (ex: P1, P2). */
    private String id;
    
    /** Compteur du nombre de fois qu'un robot a échoué à traiter ce produit. */
    private int nbEchecs = 0;

    /** * Map associant l'ID d'une compétence à son état de réalisation.
     * Clé : ID compétence (Integer).
     * Valeur : true si réalisée, false sinon.
     */
    private HashMap<Integer, Boolean> competences;

    /**
     * Constructeur du produit.
     * @param id L'identifiant unique du produit.
     */
    public Produit(String id) {
        this.id = id;
        this.competences = new HashMap<>();
        this.nbEchecs = 0;
    }

    /**
     * Retourne l'identifiant du produit.
     * @return L'identifiant (String).
     */
    public String getId() {
        return this.id;
    }
    
    /**
     * Incrémente le compteur d'échecs du produit.
     * Appelée par un robot lorsqu'il rate une opération.
     */
    public void incrementerEchecs() {
		this.nbEchecs++;
	}
	
	/**
     * Retourne le nombre total d'échecs subis par ce produit.
     * @return Le nombre d'échecs.
     */
	public int getNbEchecs() {
		return this.nbEchecs;
	}

    /**
     * Ajoute une compétence requise pour ce produit.
     * @param nomCompetence L'ID de la compétence à ajouter.
     */
    public void ajoutCompetence(Integer nomCompetence) {
        this.competences.put(nomCompetence, false);
    }

    /**
     * Marque une compétence comme validée (terminée).
     * @param nomCompetence L'ID de la compétence réalisée.
     */
    public void valideLaCompetence(Integer nomCompetence) {
        if (this.competences.containsKey(nomCompetence)) {
            this.competences.put(nomCompetence, true);
        }
    }

    /**
     * Vérifie si le produit est entièrement terminé.
     * @return true si toutes les compétences sont à true, false sinon.
     */
    public Boolean estFini() {
        return !this.competences.containsValue(false);
    }

    /**
     * Récupère la map des compétences.
     * @return La HashMap des compétences.
     */
    public HashMap<Integer, Boolean> getCompetences() {
        return this.competences;
    }

    /**
     * Représentation textuelle du produit pour le débogage.
     * Affiche l'ID, l'état des compétences et le nombre d'échecs.
     */
    @Override
    public String toString() {
        String prod = "";
        prod += "Produit[" + id + "] (";
        for (Map.Entry<Integer, Boolean> entry : competences.entrySet()) {
            prod += entry.getKey() + ":" + (entry.getValue() ? "OK" : "TODO") + " ";
        }
        prod += ")";
        return prod + " [Echecs: " + nbEchecs + "]";
    }
}