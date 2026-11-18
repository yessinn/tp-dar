package rmiService;

import java.rmi.Remote;
import java.rmi.RemoteException;

import metier.Compte;

/**
 * Interface distante du service de banque (partagée avec le client via banqueClient.jar).
 */
public interface IBanque extends Remote {

    /** Crée le compte s'il n'existe pas déjà un compte avec le même code. */
    String creerCompte(Compte c) throws RemoteException;

    /** Renvoie les informations du compte, ou un message si le code est inconnu. */
    String getInfoCompte(int code) throws RemoteException;
}
