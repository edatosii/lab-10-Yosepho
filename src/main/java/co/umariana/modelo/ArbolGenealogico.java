package co.umariana.modelo;

import java.util.ArrayList;
import java.util.List;

public class ArbolGenealogico {

    private static class Nodo {
        Persona persona;
        Nodo padre;
        Nodo madre;
        List<Nodo> hijos = new ArrayList<>();

        Nodo(Persona persona) {
            this.persona = persona;
        }
    }

    private final Nodo raiz;
    private int cantidad;

    public ArbolGenealogico(Persona sujeto) {
        sujeto.validar();
        if (!Posicion.esSujeto(sujeto.getPosicion())) {
            throw new ArbolInvalidoException("El sujeto debe estar en la posición 1.");
        }
        raiz = new Nodo(sujeto);
        cantidad = 1;
    }

    public Persona sujeto() {
        return raiz.persona;
    }

    public int tamano() {
        return cantidad;
    }

    public Persona buscar(String posicion) {
        Nodo n = buscarNodo(posicion);
        return n == null ? null : n.persona;
    }

    public boolean contiene(String posicion) {
        return buscarNodo(posicion) != null;
    }

    // ---------- navegación ----------

    private Nodo buscarNodo(String posicion) {
        if (!Posicion.esValida(posicion)) {
            return null;
        }
        if (Posicion.esAhnentafel(posicion)) {
            int n = Integer.parseInt(posicion);
            int bits = 31 - Integer.numberOfLeadingZeros(n);
            Nodo actual = raiz;
            // los bits después del primer 1: 0 = padre, 1 = madre
            for (int i = bits - 1; i >= 0 && actual != null; i--) {
                actual = ((n >> i) & 1) == 0 ? actual.padre : actual.madre;
            }
            return actual;
        }
        String[] partes = posicion.split("\\.");
        Nodo actual = raiz;
        for (int i = 1; i < partes.length && actual != null; i++) {
            actual = hijoConOrden(actual, Integer.parseInt(partes[i]));
        }
        return actual;
    }

    private Nodo hijoConOrden(Nodo nodo, int orden) {
        for (Nodo h : nodo.hijos) {
            if (Posicion.orden(h.persona.getPosicion()) == orden) {
                return h;
            }
        }
        return null;
    }

    // ---------- agregar ----------

    public Persona agregar(Persona persona) {
        persona.validar();
        String pos = persona.getPosicion();
        if (Posicion.esSujeto(pos) || buscarNodo(pos) != null) {
            throw new ArbolInvalidoException("la posición " + pos + " está repetida.");
        }
        if (Posicion.esAscendiente(pos)) {
            String posHijo = Posicion.posicionDelHijoEnLinea(pos);
            Nodo hijo = buscarNodo(posHijo);
            if (hijo == null) {
                throw new ArbolInvalidoException("la posición " + pos + " necesita a la posición "
                        + posHijo + ", que no existe.");
            }
            if (persona.getNacimiento() >= hijo.persona.getNacimiento()) {
                throw new ArbolInvalidoException("la posición " + pos + " debe haber nacido antes que la posición "
                        + posHijo + ".");
            }
            Nodo nuevo = new Nodo(persona);
            if (Integer.parseInt(pos) % 2 == 0) {
                hijo.padre = nuevo;
            } else {
                hijo.madre = nuevo;
            }
        } else {
            String posProgenitor = Posicion.posicionDelProgenitor(pos);
            Nodo progenitor = buscarNodo(posProgenitor);
            if (progenitor == null) {
                throw new ArbolInvalidoException("la posición " + pos + " necesita a la posición "
                        + posProgenitor + ", que no existe.");
            }
            if (persona.getNacimiento() <= progenitor.persona.getNacimiento()) {
                throw new ArbolInvalidoException("la posición " + pos + " debe haber nacido después que la posición "
                        + posProgenitor + ".");
            }
            Nodo nuevo = new Nodo(persona);
            int orden = Posicion.orden(pos);
            int i = 0;
            while (i < progenitor.hijos.size()
                    && Posicion.orden(progenitor.hijos.get(i).persona.getPosicion()) < orden) {
                i++;
            }
            progenitor.hijos.add(i, nuevo);
        }
        cantidad++;
        return persona;
    }

    public Persona agregarPadre(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        return agregarAscendiente(posicion, nombre, nacimiento, fallecimiento, true);
    }

    public Persona agregarMadre(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        return agregarAscendiente(posicion, nombre, nacimiento, fallecimiento, false);
    }

    private Persona agregarAscendiente(String posicion, String nombre, int nacimiento,
                                       Integer fallecimiento, boolean esPadre) {
        if (buscarNodo(posicion) == null) {
            throw new ArbolInvalidoException("la posición " + posicion + " no existe.");
        }
        if (!Posicion.esAhnentafel(posicion)) {
            throw new ArbolInvalidoException("solo se pueden agregar padres al sujeto y a sus ascendientes.");
        }
        String nueva = esPadre ? Posicion.posicionDelPadre(posicion) : Posicion.posicionDeLaMadre(posicion);
        if (!Posicion.esAhnentafel(nueva)) {
            throw new ArbolInvalidoException("no se pueden agregar más generaciones de ascendientes.");
        }
        if (contiene(nueva)) {
            throw new ArbolInvalidoException("la posición " + nueva + " ya existe.");
        }
        return agregar(new Persona(nueva, nombre, nacimiento, fallecimiento));
    }

    public Persona agregarHijo(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        Nodo progenitor = buscarNodo(posicion);
        if (progenitor == null) {
            throw new ArbolInvalidoException("la posición " + posicion + " no existe.");
        }
        if (!Posicion.esSujeto(posicion) && !Posicion.esDescendiente(posicion)) {
            throw new ArbolInvalidoException("solo se pueden agregar hijos al sujeto y a sus descendientes.");
        }
        int siguiente = 1;
        if (!progenitor.hijos.isEmpty()) {
            Nodo ultimo = progenitor.hijos.get(progenitor.hijos.size() - 1);
            siguiente = Posicion.orden(ultimo.persona.getPosicion()) + 1;
        }
        String nueva = posicion + "." + siguiente;
        if (siguiente > Posicion.MAX_ORDEN || nueva.length() > Posicion.MAX_LONGITUD) {
            throw new ArbolInvalidoException("no se pueden agregar más hijos a la posición " + posicion + ".");
        }
        return agregar(new Persona(nueva, nombre, nacimiento, fallecimiento));
    }

    // ---------- eliminar ----------

    public int eliminar(String posicion) {
        if (Posicion.esSujeto(posicion)) {
            throw new ArbolInvalidoException("el sujeto (posición 1) no se puede eliminar.");
        }
        Nodo nodo = buscarNodo(posicion);
        if (nodo == null) {
            throw new ArbolInvalidoException("la posición " + posicion + " no existe.");
        }
        int eliminados = contar(nodo);
        if (Posicion.esAscendiente(posicion)) {
            Nodo hijo = buscarNodo(Posicion.posicionDelHijoEnLinea(posicion));
            if (Integer.parseInt(posicion) % 2 == 0) {
                hijo.padre = null;
            } else {
                hijo.madre = null;
            }
        } else {
            Nodo progenitor = buscarNodo(Posicion.posicionDelProgenitor(posicion));
            progenitor.hijos.remove(nodo);
        }
        cantidad -= eliminados;
        return eliminados;
    }

    private int contar(Nodo nodo) {
        if (nodo == null) {
            return 0;
        }
        int total = 1 + contar(nodo.padre) + contar(nodo.madre);
        for (Nodo h : nodo.hijos) {
            total += contar(h);
        }
        return total;
    }

    // ---------- recorridos ----------

    public List<Persona> miembros() {
        List<Persona> lista = new ArrayList<>();
        lista.add(raiz.persona);
        lista.addAll(ascendientes());
        lista.addAll(descendientes());
        return lista;
    }

    public List<Persona> ascendientes() {
        // recorrido por niveles: da el orden Ahnentafel ascendente (2, 3, 4, 5...)
        List<Nodo> cola = new ArrayList<>();
        if (raiz.padre != null) cola.add(raiz.padre);
        if (raiz.madre != null) cola.add(raiz.madre);
        List<Persona> resultado = new ArrayList<>();
        for (int i = 0; i < cola.size(); i++) {
            Nodo n = cola.get(i);
            resultado.add(n.persona);
            if (n.padre != null) cola.add(n.padre);
            if (n.madre != null) cola.add(n.madre);
        }
        return resultado;
    }

    public List<Persona> descendientes() {
        List<Persona> resultado = new ArrayList<>();
        for (Nodo h : raiz.hijos) {
            preorden(h, resultado);
        }
        return resultado;
    }

    private void preorden(Nodo nodo, List<Persona> resultado) {
        resultado.add(nodo.persona);
        for (Nodo h : nodo.hijos) {
            preorden(h, resultado);
        }
    }
}
