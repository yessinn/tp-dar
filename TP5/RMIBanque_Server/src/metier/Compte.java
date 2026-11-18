package metier;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Compte bancaire : code, solde et date de création.
 *
 * Serializable car il est passé PAR VALEUR dans les appels RMI : le client
 * envoie une copie sérialisée du compte au serveur (creerCompte).
 */
public class Compte implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int code;
    private double solde;
    private final Date dateCreation;

    public Compte(int code, double solde) {
        this(code, solde, new Date());
    }

    public Compte(int code, double solde, Date dateCreation) {
        this.code = code;
        this.solde = solde;
        this.dateCreation = dateCreation;
    }

    public int getCode() {
        return code;
    }

    public double getSolde() {
        return solde;
    }

    public void setSolde(double solde) {
        this.solde = solde;
    }

    public Date getDateCreation() {
        return dateCreation;
    }

    @Override
    public String toString() {
        return "Compte{code=" + code + ", solde=" + solde + ", dateCreation="
                + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(dateCreation) + "}";
    }
}
