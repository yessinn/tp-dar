package rmiService;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface distante du service de conversion de devises.
 *
 * - Elle étend Remote : ses méthodes peuvent être invoquées depuis une autre JVM.
 * - Chaque méthode déclare RemoteException : un appel distant peut échouer pour
 *   des raisons réseau (serveur arrêté, connexion perdue...).
 *
 * C'est la seule classe partagée avec le client (conversionClient.jar).
 */
public interface IConversion extends Remote {

    /** Convertit un montant en euros vers le dinar tunisien. */
    double convertirMontant(double mt) throws RemoteException;
}
