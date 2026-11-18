package commonPackage;

import java.io.Serializable;

/**
 * Activité 3-2 (repris du TP2) : objet échangé du client vers le serveur.
 *
 * Il contient les arguments de l'opération : le premier opérande, l'opérateur
 * et le deuxième opérande. Il implémente Serializable pour pouvoir être
 * transformé en suite d'octets par ObjectOutputStream, transmis par la socket,
 * puis reconstruit par ObjectInputStream de l'autre côté.
 *
 * La classe doit être présente (même package, même serialVersionUID) chez le
 * client ET chez le serveur.
 */
public class Operation implements Serializable {

    private static final long serialVersionUID = 1L;

    private final double operande1;
    private final char operateur;
    private final double operande2;

    public Operation(double operande1, char operateur, double operande2) {
        this.operande1 = operande1;
        this.operateur = operateur;
        this.operande2 = operande2;
    }

    public double getOperande1() {
        return operande1;
    }

    public char getOperateur() {
        return operateur;
    }

    public double getOperande2() {
        return operande2;
    }

    @Override
    public String toString() {
        return SyntaxeOperation.formater(operande1) + " " + operateur + " " + SyntaxeOperation.formater(operande2);
    }
}
