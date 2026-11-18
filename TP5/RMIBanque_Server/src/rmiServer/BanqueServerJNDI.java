package rmiServer;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import rmiService.BanqueImpl;

/**
 * Étape 5 : serveur de banque avec JNDI.
 *
 * L'objet distant est enregistré via l'API JNDI (InitialContext.rebind) au
 * lieu de Naming.rebind. La configuration (fabrique de contexte et URL du
 * registre RMI) est lue automatiquement dans le fichier jndi.properties
 * présent dans le classpath.
 */
public class BanqueServerJNDI {

    public static void main(String[] args) {
        try {
            // Le service d'annuaire utilisé par JNDI reste le registre RMI : il faut le démarrer
            LocateRegistry.createRegistry(1099);
            System.out.println("Annuaire RMI demarre sur le port 1099");

            BanqueImpl banque = new BanqueImpl();

            // InitialContext lit jndi.properties (java.naming.factory.initial, java.naming.provider.url)
            Context contexte = new InitialContext();
            System.out.println("Contexte JNDI : " + contexte.getEnvironment().get(Context.INITIAL_CONTEXT_FACTORY)
                    + " -> " + contexte.getEnvironment().get(Context.PROVIDER_URL));

            contexte.rebind("BanqueService", banque);
            System.out.println("Objet distant enregistre via JNDI sous le nom : BanqueService");
            System.out.println("Serveur de banque (JNDI) pret.");
        } catch (RemoteException | NamingException e) {
            System.err.println("Erreur cote serveur : " + e);
        }
    }
}
