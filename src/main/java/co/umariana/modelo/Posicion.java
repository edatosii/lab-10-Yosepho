package co.umariana.modelo;

/**
 * Las posiciones de un árbol genealógico. Cada miembro tiene una posición (texto) que dice quién es
 * respecto al SUJETO (la persona de la que trata el árbol). Se usan dos sistemas:
 *
 * AHNENTAFEL (para los ASCENDIENTES: padres, abuelos...). Es un número entero:
 *
 *     1                  el sujeto
 *     2 = padre del 1    3 = madre del 1
 *     4 = padre del 2    5 = madre del 2    6 = padre del 3    7 = madre del 3
 *
 *   Regla: el padre de la persona n es 2n y la madre es 2n + 1. Por eso los números pares son
 *   padres y los impares (mayores que 1) son madres. Y la persona n es hijo/a de n / 2 (división entera).
 *
 * D'ABOVILLE (para los DESCENDIENTES: hijos, nietos...). Es el "1" del sujeto seguido de un punto y
 * el número de orden en cada generación:
 *
 *     1.1  primer hijo del sujeto      1.2  segundo hijo del sujeto
 *     1.1.1  primer hijo del 1.1       1.2.3  tercer hijo del 1.2
 *
 *   Regla: el progenitor de 1.2.3 es 1.2 (se quita el último número).
 *
 * La posición "1" (el sujeto) cuenta como Ahnentafel.
 *
 * LÍMITES (para que las posiciones quepan en la base de datos y en un int):
 *   - Ahnentafel: de 1 a 2047 (hasta 10 generaciones de ascendientes).
 *   - D'Aboville: cada número de orden va de 1 a 9999 y la posición completa tiene máximo 50 caracteres.
 *
 * INSTRUCCIONES PARA EL ESTUDIANTE (Etapa 1)
 * ------------------------------------------
 * Cada método marcado con "TODO" lanza UnsupportedOperationException: reemplácelo por su implementación.
 * Los métodos reciben texto: use String.matches(...) (expresiones regulares) o String.split(...).
 * NO modifique las pruebas (src/test).
 */
public final class Posicion {

    /** La posición del sujeto. */
    public static final String SUJETO = "1";
    /** Mayor número Ahnentafel permitido (10 generaciones de ascendientes). */
    public static final int MAX_AHNENTAFEL = 2047;
    /** Mayor número de orden permitido en cada nivel d'Aboville. */
    public static final int MAX_ORDEN = 9999;
    /** Longitud máxima de una posición d'Aboville (es lo que cabe en la base de datos). */
    public static final int MAX_LONGITUD = 50;

    private Posicion() {
    }

    /**
     * @return true solo si la posición es exactamente "1"
     */
    public static boolean esSujeto(String posicion) {
    return SUJETO.equals(posicion);
}

public static boolean esAhnentafel(String posicion) {
    if (posicion == null || !posicion.matches("[1-9][0-9]{0,3}")) {
        return false;
    }
    return Integer.parseInt(posicion) <= MAX_AHNENTAFEL;
}

public static boolean esDAboville(String posicion) {
    if (posicion == null || posicion.length() > MAX_LONGITUD) {
        return false;
    }
    // cada grupo ".k" es un entero de 1 a 9999 sin ceros a la izquierda
    return posicion.matches("1(\\.[1-9][0-9]{0,3})+");
}

public static boolean esValida(String posicion) {
    return esAhnentafel(posicion) || esDAboville(posicion);
}

public static boolean esAscendiente(String posicion) {
    return esAhnentafel(posicion) && !esSujeto(posicion);
}

public static boolean esDescendiente(String posicion) {
    return esDAboville(posicion);
}

public static String posicionDelPadre(String ahnentafel) {
    if (!esAhnentafel(ahnentafel)) {
        throw new IllegalArgumentException("La posición no es Ahnentafel válida: " + ahnentafel);
    }
    return String.valueOf(2 * Integer.parseInt(ahnentafel));
}

public static String posicionDeLaMadre(String ahnentafel) {
    if (!esAhnentafel(ahnentafel)) {
        throw new IllegalArgumentException("La posición no es Ahnentafel válida: " + ahnentafel);
    }
    return String.valueOf(2 * Integer.parseInt(ahnentafel) + 1);
}

public static String posicionDelHijoEnLinea(String ahnentafel) {
    if (!esAscendiente(ahnentafel)) {
        throw new IllegalArgumentException("La posición no es un ascendiente: " + ahnentafel);
    }
    return String.valueOf(Integer.parseInt(ahnentafel) / 2);
}

public static String posicionDelProgenitor(String daboville) {
    if (!esDAboville(daboville)) {
        throw new IllegalArgumentException("La posición no es un descendiente: " + daboville);
    }
    return daboville.substring(0, daboville.lastIndexOf('.'));
}

public static int orden(String daboville) {
    if (!esDAboville(daboville)) {
        throw new IllegalArgumentException("La posición no es un descendiente: " + daboville);
    }
    return Integer.parseInt(daboville.substring(daboville.lastIndexOf('.') + 1));
}

public static int generacion(String posicion) {
    if (esAhnentafel(posicion)) {
        // parte entera de log2(n)
        return 31 - Integer.numberOfLeadingZeros(Integer.parseInt(posicion));
    }
    if (esDAboville(posicion)) {
        int puntos = 0;
        for (int i = 0; i < posicion.length(); i++) {
            if (posicion.charAt(i) == '.') {
                puntos++;
            }
        }
        return puntos;
    }
    throw new IllegalArgumentException("Posición no válida: " + posicion);
}
}
