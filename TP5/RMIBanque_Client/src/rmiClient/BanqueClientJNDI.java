package rmiClient;

import java.rmi.RemoteException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import rmiService.IBanque;

/**
 * Étape 5 : client du service de banque avec JNDI.
 *
 * La recherche se fait avec InitialContext.lookup au lieu de Naming.lookup.
 * L'adresse du registre est lue dans jndi.properties (rmi://localhost:1099) ;
 * pour un serveur distant, on la remplace au lancement :
 *   -Djava.naming.provider.url=rmi://192.168.1.14:1099
 *
 * Utilisation : java -cp "out;lib/banqueClient.jar" rmiClient.BanqueClientJNDI
 */
public class BanqueClientJNDI {

    public static void main(String[] args) {
        try {
            Context contexte = new InitialContext();
            System.out.println("Contexte JNDI : " + contexte.getEnvironment().get(Context.PROVIDER_URL));

            IBanque banque = (IBanque) contexte.lookup("BanqueService");
            System.out.println("Reference obtenue via JNDI : BanqueService");
            Scenario.executer(banque);
        } catch (NamingException e) {
            // getMessage() peut être null : toString() inclut la cause d'origine (ex. ConnectException)
            System.err.println("Erreur JNDI : " + e);
        } catch (RemoteException e) {
            System.err.println("Erreur cote client : " + e.getMessage());
        }
    }
}
