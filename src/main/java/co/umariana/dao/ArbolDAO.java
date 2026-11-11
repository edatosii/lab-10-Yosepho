package co.umariana.dao;

import co.umariana.conexion.ConexionBD;
import co.umariana.modelo.ArbolGenealogico;
import co.umariana.modelo.Persona;
import co.umariana.modelo.ResumenArbol;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ArbolDAO {

    private static final String SQL_MIEMBRO =
            "INSERT INTO miembros (arbol_id, posicion, nombre, nacimiento, fallecimiento) VALUES (?, ?, ?, ?, ?)";

    private void insertarMiembros(Connection con, long arbolId, ArbolGenealogico arbol) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SQL_MIEMBRO)) {
            for (Persona p : arbol.miembros()) {
                ps.setLong(1, arbolId);
                ps.setString(2, p.getPosicion());
                ps.setString(3, p.getNombre());
                ps.setInt(4, p.getNacimiento());
                if (p.getFallecimiento() == null) {
                    ps.setNull(5, java.sql.Types.INTEGER);
                } else {
                    ps.setInt(5, p.getFallecimiento());
                }
                ps.executeUpdate();
            }
        }
    }

    public long guardar(String nombre, ArbolGenealogico arbol) {
        if (arbol == null || nombre == null || nombre.isBlank() || nombre.length() > 100) {
            return -1;
        }
        try (Connection con = ConexionBD.conectar()) {
            con.setAutoCommit(false);
            try {
                long id;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO arboles (nombre) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, nombre);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) {
                            throw new SQLException("No se obtuvo el id del árbol.");
                        }
                        id = rs.getLong(1);
                    }
                }
                insertarMiembros(con, id, arbol);
                con.commit();
                return id;
            } catch (SQLException e) {
                con.rollback();
                System.out.println("Error al guardar el árbol: " + e.getMessage());
                return -1;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.out.println("Error de conexión: " + e.getMessage());
            return -1;
        }
    }

    public List<ResumenArbol> listar() {
        List<ResumenArbol> lista = new ArrayList<>();
        String sql = "SELECT a.id, a.nombre, a.creado, COUNT(m.id) AS total "
                + "FROM arboles a LEFT JOIN miembros m ON m.arbol_id = a.id "
                + "GROUP BY a.id, a.nombre, a.creado ORDER BY a.id";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new ResumenArbol(rs.getLong("id"), rs.getString("nombre"),
                        rs.getInt("total"), rs.getTimestamp("creado").toLocalDateTime()));
            }
            return lista;
        } catch (SQLException e) {
            System.out.println("Error al listar los árboles: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public ArbolGenealogico cargar(long id) {
        String sql = "SELECT posicion, nombre, nacimiento, fallecimiento FROM miembros "
                + "WHERE arbol_id = ? ORDER BY id";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                ArbolGenealogico arbol = null;
                while (rs.next()) {
                    int f = rs.getInt("fallecimiento");
                    Integer fallecimiento = rs.wasNull() ? null : f;
                    Persona p = new Persona(rs.getString("posicion"), rs.getString("nombre"),
                            rs.getInt("nacimiento"), fallecimiento);
                    if (arbol == null) {
                        arbol = new ArbolGenealogico(p);
                    } else {
                        arbol.agregar(p);
                    }
                }
                return arbol;
            }
        } catch (SQLException | RuntimeException e) {
            System.out.println("Error al cargar el árbol: " + e.getMessage());
            return null;
        }
    }

    public boolean actualizar(long id, ArbolGenealogico arbol) {
        if (arbol == null) {
            return false;
        }
        try (Connection con = ConexionBD.conectar()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement("SELECT id FROM arboles WHERE id = ?")) {
                    ps.setLong(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return false;
                        }
                    }
                }
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM miembros WHERE arbol_id = ?")) {
                    ps.setLong(1, id);
                    ps.executeUpdate();
                }
                insertarMiembros(con, id, arbol);
                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                System.out.println("Error al actualizar el árbol: " + e.getMessage());
                return false;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.out.println("Error de conexión: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminar(long id) {
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement("DELETE FROM arboles WHERE id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al eliminar el árbol: " + e.getMessage());
            return false;
        }
    }
}
