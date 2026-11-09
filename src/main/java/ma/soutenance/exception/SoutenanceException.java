package ma.soutenance.exception;
public class SoutenanceException extends RuntimeException {
    public SoutenanceException(String m){super(m);}
    public SoutenanceException(String m,Throwable c){super(m,c);}
}
