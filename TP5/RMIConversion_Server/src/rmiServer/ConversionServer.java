package rmiServer;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

import rmiService.ConversionImpl;

/**
 * Application serveur du service de conversion.
 *
 *   i.   active l'annuaire RMI (rmiregistry) sur le port 1099 ;
 *   ii.  crée une instance de ConversionImpl ;
 *   iii. publie sa référence dans l'annuaire sous le nom "ConversionService".
 */
public class ConversionServer {

    public static final int PORT_REGISTRY = 1099;
    public static final String NOM_SERVICE = "ConversionService";

    public static void main(String[] args) {
        try {
            // i. démarrage de l'annuaire RMI dans la JVM du serveur
            LocateRegistry.createRegistry(PORT_REGISTRY);
            System.out.println("Annuaire RMI demarre sur le port " + PORT_REGISTRY);

            // ii. création (et exportation) de l'objet distant
            ConversionImpl conversion = new ConversionImpl();

            // iii. publication de la référence dans l'annuaire
            String url = "rmi://localhost:" + PORT_REGISTRY + "/" + NOM_SERVICE;
            Naming.rebind(url, conversion);
            System.out.println("Objet distant publie sous : " + url);
            System.out.println("Serveur de conversion pret (1 EUR = " + ConversionImpl.TAUX_EUR_TND + " TND).");
            // Le main se termine, mais la JVM reste active : l'objet exporté attend les appels.
        } catch (RemoteException | MalformedURLException e) {
            System.err.println("Erreur cote serveur : " + e.getMessage());
        }
    }
}
