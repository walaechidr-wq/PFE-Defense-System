package ma.soutenance.model.enums;

public enum Specialite {
    INFORMATIQUE("Informatique"),
    MATHEMATIQUE("Mathématique"),
    GESTION("Gestion"), 
    ANGLAIS("Anglais");
	
    private final String libelle;
    Specialite(String l){
    	this.libelle=l;
    	}
    public String getLibelle(){
    	return libelle;
    	}
    public static Specialite fromString(String s){
        if(s==null)return INFORMATIQUE;
        String t=s.trim().toLowerCase();
        if(t.contains("info"))return INFORMATIQUE;
        if(t.contains("math"))return MATHEMATIQUE;
        if(t.contains("gest"))return GESTION;
        if(t.contains("angl"))return ANGLAIS;
        return INFORMATIQUE;
    }
}
