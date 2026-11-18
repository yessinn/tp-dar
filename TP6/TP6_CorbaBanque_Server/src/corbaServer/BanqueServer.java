package corbaServer;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.omg.CORBA.ORB;
import org.omg.CORBA.UserException;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import service.BanqueImpl;

/**
 * Serveur CORBA du service bancaire.
 *
 * Prérequis : le service de nommage doit être lancé (tnameserv, port 900).
 */
public class BanqueServer {

    public static final String NOM_SERVICE = "BanqueService";

    public static void main(String[] args) {
        try {
            // a. initialisation de l'ORB
            ORB orb = ORB.init(args, null);
            System.out.println("ORB initialise.");

            // b. adaptateur d'objets : référence sur le RootPOA puis activation du POAManager
            POA rootPOA = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            rootPOA.the_POAManager().activate();
            System.out.println("RootPOA recupere et POAManager active.");

            // c. annuaire JNDI (configuré par jndi.properties : CNCtxFactory, iiop://localhost:900)
            Context contexte = new InitialContext();
            System.out.println("Annuaire JNDI : " + contexte.getEnvironment().get(Context.PROVIDER_URL));

            // d. création du servant
            BanqueImpl servant = new BanqueImpl();

            // e. enregistrement : le POA active le servant et fournit sa référence d'objet CORBA,
            //    publiée ensuite dans le service de nommage sous un nom
            org.omg.CORBA.Object reference = rootPOA.servant_to_reference(servant);
            contexte.rebind(NOM_SERVICE, reference);
            System.out.println("Reference publiee sous le nom : " + NOM_SERVICE);
            System.out.println("IOR : " + orb.object_to_string(reference).substring(0, 60) + "...");

            // f. attente des requêtes des clients (bloquant)
            System.out.println("Serveur CORBA pret, en attente de requetes...");
            orb.run();
        } catch (UserException | NamingException e) {
            System.err.println("Erreur cote serveur : " + e);
        }
    }
}
