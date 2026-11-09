package ma.soutenance.model.enums;

public enum Langue {
    FRANCAIS("Français"),
    ANGLAIS("Anglais");
	
    private final String libelle;
    Langue(String l){
    	this.libelle=l;
    	}
    public String getLibelle(){
    	return libelle;
    	}
    public static Langue fromString(String s){
        if(s==null)return FRANCAIS;
        return s.trim().equalsIgnoreCase("Anglais")?ANGLAIS:FRANCAIS;
    }
}
