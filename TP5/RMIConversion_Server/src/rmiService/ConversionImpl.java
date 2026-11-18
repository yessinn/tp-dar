package rmiService;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Implémentation du service de conversion.
 *
 * En héritant de UnicastRemoteObject, l'objet est « exporté » dès sa création :
 * il écoute sur un port TCP anonyme et peut recevoir des appels distants.
 * Cette classe reste sur le serveur : le client ne la connaît pas.
 */
public class ConversionImpl extends UnicastRemoteObject implements IConversion {

    private static final long serialVersionUID = 1L;

    /** Taux fixe utilisé pour l'exercice : 1 EUR = 3.40 TND. */
    public static final double TAUX_EUR_TND = 3.40;

    public ConversionImpl() throws RemoteException {
        super();
    }

    @Override
    public double convertirMontant(double mt) throws RemoteException {
        double resultat = mt * TAUX_EUR_TND;
        // Ce message s'affiche dans la console du SERVEUR : le calcul est exécuté ici
        System.out.println("Appel distant convertirMontant(" + mt + ") -> " + resultat
                + " TND   [thread " + Thread.currentThread().getName() + "]");
        return resultat;
    }
}
