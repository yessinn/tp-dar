package commonPackage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validation de la syntaxe d'une opération saisie par l'utilisateur, côté client,
 * AVANT tout envoi au serveur.
 *
 * Format accepté : nombre opérateur nombre
 *   - nombre    : entier ou décimal, signe optionnel (ex. 55, -8, 2.5, 2,5)
 *   - opérateur : un seul caractère parmi + - * /
 *   - les espaces autour de l'opérateur sont facultatifs.
 *
 * Exemples valides   : "55 * 25", "12+30", "5 - -8", "2,5 * 4"
 * Exemples invalides : "25 ** 55", "abc + 3", "7 /", "3 % 2"
 */
public final class SyntaxeOperation {

    private static final String NOMBRE = "([+-]?\\d+(?:[.,]\\d+)?)";
    private static final Pattern FORMAT = Pattern.compile(
            "^\\s*" + NOMBRE + "\\s*([-+*/])\\s*" + NOMBRE + "\\s*$");

    private SyntaxeOperation() {
    }

    /**
     * Analyse une saisie.
     *
     * @return un tableau {operande1, operateur, operande2} (virgule décimale
     *         remplacée par un point), ou null si la syntaxe est invalide.
     */
    public static String[] analyser(String saisie) {
        Matcher m = FORMAT.matcher(saisie);
        if (!m.matches()) {
            return null;
        }
        return new String[] {
                m.group(1).replace(',', '.'),
                m.group(2),
                m.group(3).replace(',', '.')
        };
    }

    /** Affiche 12.0 sous la forme 12, et garde les décimales sinon. */
    public static String formater(double v) {
        return (v == Math.rint(v) && !Double.isInfinite(v)) ? String.valueOf((long) v) : String.valueOf(v);
    }
}
