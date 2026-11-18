package rmiServer;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

import rmiService.BanqueImpl;

/**
 * Étapes 2 à 4 : serveur de banque avec le registre RMI (LocateRegistry + Naming).
 */
public class BanqueServer {

    public static void main(String[] args) {
        try {
            LocateRegistry.createRegistry(1099);
            System.out.println("Annuaire RMI demarre sur le port 1099");

            BanqueImpl banque = new BanqueImpl();
            Naming.rebind("rmi://localhost:1099/BanqueService", banque);
            System.out.println("Objet distant publie sous : rmi://localhost:1099/BanqueService");
            System.out.println("Serveur de banque pret.");
        } catch (RemoteException | MalformedURLException e) {
            System.err.println("Erreur cote serveur : " + e.getMessage());
        }
    }
}
