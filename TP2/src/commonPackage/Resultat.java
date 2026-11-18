package commonPackage;

import java.io.Serializable;

/**
 * Activité 2-2 : objet renvoyé par le serveur au client.
 *
 * Il contient soit la valeur calculée, soit un message d'erreur
 * (ex. division par zéro).
 */
public class Resultat implements Serializable {

    private static final long serialVersionUID = 1L;

    private final boolean succes;
    private final double valeur;
    private final String message;

    private Resultat(boolean succes, double valeur, String message) {
        this.succes = succes;
        this.valeur = valeur;
        this.message = message;
    }

    public static Resultat ok(double valeur) {
        return new Resultat(true, valeur, null);
    }

    public static Resultat erreur(String message) {
        return new Resultat(false, 0, message);
    }

    public boolean isSucces() {
        return succes;
    }

    public double getValeur() {
        return valeur;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return succes ? SyntaxeOperation.formater(valeur) : "Erreur : " + message;
    }
}
