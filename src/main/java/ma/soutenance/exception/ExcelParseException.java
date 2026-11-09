package ma.soutenance.exception;
public class ExcelParseException extends SoutenanceException {
    private final String sheetName;
    private final int rowIndex;
    public ExcelParseException(String sheet,int row,String detail,Throwable cause){
        super("Erreur lecture Excel — Feuille '"+sheet+"' ligne "+row+": "+detail,cause);
        this.sheetName=sheet;this.rowIndex=row;
    }
    public String getSheetName(){return sheetName;}
    public int getRowIndex(){return rowIndex;}
}
