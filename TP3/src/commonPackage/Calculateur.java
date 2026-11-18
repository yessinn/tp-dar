package commonPackage;

/**
 * Service de calcul partagé par les deux activités du TP3.
 */
public final class Calculateur {

    private Calculateur() {
    }

    public static Resultat calculer(double x, char operateur, double y) {
        switch (operateur) {
            case '+': return Resultat.ok(x + y);
            case '-': return Resultat.ok(x - y);
            case '*': return Resultat.ok(x * y);
            case '/':
                if (y == 0) {
                    return Resultat.erreur("division par zero impossible");
                }
                return Resultat.ok(x / y);
            default:
                return Resultat.erreur("operateur inconnu '" + operateur + "'");
        }
    }
}
