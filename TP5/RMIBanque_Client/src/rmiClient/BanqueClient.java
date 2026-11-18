package rmiClient;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import metier.Compte;     // fourni par lib/banqueClient.jar
import rmiService.IBanque; // fourni par lib/banqueClient.jar

/**
 * Étapes 3 et 4 : client du service de banque (recherche avec Naming.lookup).
 *
 * Utilisation : java -cp "out;lib/banqueClient.jar" rmiClient.BanqueClient [hoteServeur]
 */
public class BanqueClient {

    public static void main(String[] args) {
        String hote = (args.length > 0) ? args[0] : "localhost";
        String url = "rmi://" + hote + ":1099/BanqueService";

        try {
            IBanque banque = (IBanque) Naming.lookup(url);
            System.out.println("Reference obtenue depuis " + url);
            Scenario.executer(banque);
        } catch (NotBoundException e) {
            System.err.println("Service introuvable dans l'annuaire : " + e.getMessage());
        } catch (RemoteException | MalformedURLException e) {
            System.err.println("Erreur cote client : " + e.getMessage());
        }
    }
}

/** Suite d'appels distants commune aux deux clients (RMI direct et JNDI). */
class Scenario {

    static void executer(IBanque banque) throws RemoteException {
        System.out.println("> creerCompte(new Compte(1, 1000))");
        System.out.println("  " + banque.creerCompte(new Compte(1, 1000)));
        System.out.println("> creerCompte(new Compte(2, 2500.5))");
        System.out.println("  " + banque.creerCompte(new Compte(2, 2500.5)));
        System.out.println("> creerCompte(new Compte(1, 50))");
        System.out.println("  " + banque.creerCompte(new Compte(1, 50)));
        System.out.println("> getInfoCompte(1)");
        System.out.println("  " + banque.getInfoCompte(1));
        System.out.println("> getInfoCompte(2)");
        System.out.println("  " + banque.getInfoCompte(2));
        System.out.println("> getInfoCompte(99)");
        System.out.println("  " + banque.getInfoCompte(99));
    }
}
