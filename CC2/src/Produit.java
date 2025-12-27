import java.io.*;
import java.util.*;

public class Produit implements Serializable{
	private static final long serialVersionUID = 1L;
	
	private String id;
	private HashMap<Integer,Boolean> competences;
	
	public Produit(String id){
        this.id = id;
        this.competences = new HashMap<>();
    }
    
    //Donne l'id
    public String getId(){
		return this.id;
	}
    
    //Rajoute une competence nécessaire à la liste des compétences
    public void ajoutCompetence(Integer nomCompetence){
		this.competences.put(nomCompetence,false); 
	}
	
	//Considère une compétence à faire comme Vrai donc terminé
	public void valideLaCompetence(Integer nomCompetence){
		if (this.competences.containsKey(nomCompetence)){
			this.competences.put(nomCompetence,true); 
		}
	}
	
	//Revoie Vrai si tout est à Vrai et donc si le produit est 100% terminé
	public Boolean estFini(){
		return !this.competences.containsValue(false);
	}

	public HashMap<Integer,Boolean> getCompetences(){
		return this.competences;
	}
}
