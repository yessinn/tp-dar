package rmiClient;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import rmiService.IConversion; // fourni par lib/conversionClient.jar

/**
 * Application cliente du service de conversion.
 *
 *   ii.  cherche la référence de l'objet distant dans l'annuaire RMI ;
 *   iii. invoque convertirMontant(500) via cette référence.
 *
 * Utilisation : java -cp "out;lib/conversionClient.jar" rmiClient.ConversionClient [hoteServeur] [montants...]
 */
public class ConversionClient {

    public static void main(String[] args) {
        String hote = (args.length > 0) ? args[0] : "localhost";
        String url = "rmi://" + hote + ":1099/ConversionService";

        try {
            // ii. recherche dans l'annuaire : on obtient un stub (proxy) qui implémente IConversion
            IConversion conversion = (IConversion) Naming.lookup(url);
            System.out.println("Reference obtenue depuis " + url);
            System.out.println("Classe de l'objet obtenu : " + conversion.getClass().getName());

            // iii. invocation distante : l'appel part sur le réseau, le calcul se fait sur le serveur
            double resultat = conversion.convertirMontant(500);
            System.out.println("500.0 EUR = " + resultat + " TND");

            // Montants supplémentaires éventuels passés en arguments
            for (int i = 1; i < args.length; i++) {
                double mt = Double.parseDouble(args[i]);
                System.out.println(mt + " EUR = " + conversion.convertirMontant(mt) + " TND");
            }
        } catch (NotBoundException e) {
            System.err.println("Service introuvable dans l'annuaire : " + e.getMessage());
        } catch (RemoteException | MalformedURLException e) {
            System.err.println("Erreur cote client : " + e.getMessage());
        }
    }
}
