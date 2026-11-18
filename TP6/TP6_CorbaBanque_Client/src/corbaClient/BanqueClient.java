package corbaClient;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import corbaBanque.Compte;
import corbaBanque.IBanqueRemote;
import corbaBanque.IBanqueRemoteHelper;
import corbaBanque.OperationRefusee;

/**
 * Client CORBA du service bancaire : teste chaque méthode distante via le stub.
 */
public class BanqueClient {

    private static IBanqueRemote stub;

    public static void main(String[] args) {
        try {
            // a. propriétés de l'annuaire JNDI (lues dans jndi.properties)
            Context contexte = new InitialContext();
            System.out.println("Annuaire JNDI : " + contexte.getEnvironment().get(Context.PROVIDER_URL));

            // b. référence de l'objet distant
            Object ref = contexte.lookup("BanqueService");

            // c. conversion de la référence générique vers IBanqueRemote : création du stub
            stub = IBanqueRemoteHelper.narrow((org.omg.CORBA.Object) ref);
            System.out.println("Stub obtenu : " + stub.getClass().getName());

            // d. appels distants
            titre("creerCompte");
            essayer("creerCompte(1, 1000)", () -> stub.creerCompte(new Compte(1, 1000f)));
            essayer("creerCompte(2, 500)", () -> stub.creerCompte(new Compte(2, 500f)));
            essayer("creerCompte(3, 2500)", () -> stub.creerCompte(new Compte(3, 2500f)));
            essayer("creerCompte(1, 50)  [code deja utilise]", () -> stub.creerCompte(new Compte(1, 50f)));

            titre("verser");
            essayer("verser(250, 1)", () -> stub.verser(250f, 1));
            essayer("verser(100, 99) [compte inexistant]", () -> stub.verser(100f, 99));

            titre("retirer");
            essayer("retirer(200, 2)", () -> stub.retirer(200f, 2));
            essayer("retirer(1000, 2) [solde insuffisant]", () -> stub.retirer(1000f, 2));

            titre("getCompte");
            essayer("getCompte(1)", () -> afficher(stub.getCompte(1)));
            essayer("getCompte(99) [compte inexistant]", () -> afficher(stub.getCompte(99)));

            titre("getComptes");
            Compte[] tous = stub.getComptes();
            System.out.println("getComptes() -> " + tous.length + " compte(s) :");
            for (Compte c : tous) {
                afficher(c);
            }

            titre("conversion");
            System.out.println("conversion(500 EUR) -> " + stub.conversion(500f) + " DT");
            System.out.println("conversion(" + tous[0].solde + " EUR, solde du compte 1) -> "
                    + stub.conversion(tous[0].solde) + " DT");
        } catch (NamingException e) {
            System.err.println("Erreur JNDI : " + e);
        } catch (org.omg.CORBA.SystemException e) {
            System.err.println("Erreur CORBA : " + e);
        }
    }

    /** Appel distant qui peut lever l'exception IDL OperationRefusee. */
    interface AppelDistant {
        void executer() throws OperationRefusee;
    }

    private static void essayer(String libelle, AppelDistant appel) {
        try {
            appel.executer();
            System.out.println(libelle + " -> OK");
        } catch (OperationRefusee e) {
            System.out.println(libelle + " -> refuse : " + e.message);
        }
    }

    private static void afficher(Compte c) {
        System.out.println("   Compte{code=" + c.code + ", solde=" + c.solde + "}");
    }

    private static void titre(String t) {
        System.out.println("--- " + t + " ---");
    }
}
