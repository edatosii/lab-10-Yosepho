package co.umariana.construccion;

import co.umariana.modelo.ArbolGenealogico;
import co.umariana.modelo.ArbolInvalidoException;
import co.umariana.modelo.Persona;
import co.umariana.modelo.Posicion;
import java.util.ArrayList;
import java.util.List;

public final class ConstructorArbol {

    private ConstructorArbol() {
    }

    private static class Entrada {
        final int linea;
        final Persona persona;

        Entrada(int linea, Persona persona) {
            this.linea = linea;
            this.persona = persona;
        }

        int grupo() {
            String p = persona.getPosicion();
            if (Posicion.esSujeto(p)) return 0;
            return Posicion.esAscendiente(p) ? 1 : 2;
        }
    }

    public static ArbolGenealogico construir(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ArbolInvalidoException("El texto está vacío: escriba al menos una persona.");
        }
        String[] lineas = texto.split("\\r?\\n", -1);
        List<Entrada> entradas = new ArrayList<>();

        for (int i = 0; i < lineas.length; i++) {
            String linea = lineas[i];
            if (linea.isBlank()) {
                continue;
            }
            int numero = i + 1;
            String[] campos = linea.split(";", -1);
            if (campos.length < 3 || campos.length > 4) {
                throw new ArbolInvalidoException("Línea " + numero
                        + ": debe tener los campos posicion;nombre;nacimiento;fallecimiento.");
            }
            for (int k = 0; k < campos.length; k++) {
                campos[k] = campos[k].trim();
            }
            int nacimiento;
            try {
                nacimiento = Integer.parseInt(campos[2]);
            } catch (NumberFormatException e) {
                throw new ArbolInvalidoException("Línea " + numero + ": el nacimiento debe ser un número entero.");
            }
            Integer fallecimiento = null;
            if (campos.length == 4 && !campos[3].isEmpty()) {
                try {
                    fallecimiento = Integer.valueOf(campos[3]);
                } catch (NumberFormatException e) {
                    throw new ArbolInvalidoException("Línea " + numero
                            + ": el fallecimiento debe ser un número entero o quedar vacío.");
                }
            }
            Persona persona = new Persona(campos[0], campos[1], nacimiento, fallecimiento);
            try {
                persona.validar();
            } catch (IllegalArgumentException e) {
                throw new ArbolInvalidoException("Línea " + numero + ": " + e.getMessage());
            }
            for (Entrada previa : entradas) {
                if (previa.persona.getPosicion().equals(persona.getPosicion())) {
                    throw new ArbolInvalidoException("Línea " + numero + ": la posición "
                            + persona.getPosicion() + " está repetida.");
                }
            }
            entradas.add(new Entrada(numero, persona));
        }

        if (entradas.isEmpty()) {
            throw new ArbolInvalidoException("El texto está vacío: escriba al menos una persona.");
        }

        // ordenar: sujeto, ascendientes por Ahnentafel, descendientes por generación
        entradas.sort((a, b) -> {
            if (a.grupo() != b.grupo()) {
                return Integer.compare(a.grupo(), b.grupo());
            }
            if (a.grupo() == 1) {
                return Integer.compare(Integer.parseInt(a.persona.getPosicion()),
                        Integer.parseInt(b.persona.getPosicion()));
            }
            if (a.grupo() == 2) {
                int g = Integer.compare(Posicion.generacion(a.persona.getPosicion()),
                        Posicion.generacion(b.persona.getPosicion()));
                if (g != 0) return g;
            }
            return Integer.compare(a.linea, b.linea);
        });

        if (entradas.get(0).grupo() != 0) {
            throw new ArbolInvalidoException("Falta la persona de la posición 1 (el sujeto).");
        }

        ArbolGenealogico arbol;
        try {
            arbol = new ArbolGenealogico(entradas.get(0).persona);
        } catch (ArbolInvalidoException e) {
            throw new ArbolInvalidoException("Línea " + entradas.get(0).linea + ": " + e.getMessage());
        }
        for (int i = 1; i < entradas.size(); i++) {
            Entrada e = entradas.get(i);
            try {
                arbol.agregar(e.persona);
            } catch (ArbolInvalidoException ex) {
                throw new ArbolInvalidoException("Línea " + e.linea + ": " + ex.getMessage());
            }
        }
        return arbol;
    }
}
