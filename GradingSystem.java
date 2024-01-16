/*
CREATE DATABASE grading_system;

USE grading_system;

CREATE TABLE teachers (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    subject VARCHAR(50) NOT NULL
);

CREATE TABLE students (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    IM211 INT DEFAULT 99,
    CC214 INT DEFAULT 99,
    MS121 INT DEFAULT 99,
    PE3 INT DEFAULT 99,
    GE105 INT DEFAULT 99,
    GE106 INT DEFAULT 99,
    NET212 INT DEFAULT 99,
    ITELECTV INT DEFAULT 99,
    GENSOC INT DEFAULT 99,
    average_grade INT DEFAULT 99,
    status VARCHAR(255) DEFAULT 'Passed'
);
*/

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

public class GradingSystem {
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/grading_system";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "alexis";

    public static void main(final String[] args) {
        SwingUtilities.invokeLater(() -> new GradingSystem().createAndShowGUI());
    }

    private JDialog loginDialog;
    private JDialog registerDialog;
    private JFrame frame;
    private JTextField nameField;
    private JPasswordField passwordField;
    private JButton loginButton;

    private JButton registerButton;
    private JComboBox<String> subjectsDropdown;
    private JComboBox<String> userTypeDropdown;
    private Connection connection;

    private Statement statement;

    private void createAndShowGUI() {
        frame = new JFrame("Grading System");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                confirmExit();
            }
        });

        final JPanel panel = createMainScreen();
        frame.getContentPane().add(panel);
        frame.getContentPane().setPreferredSize(new Dimension(600, 400));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
            statement = connection.createStatement();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

    private JPanel createMainScreen() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);

        nameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        loginButton = new JButton("Login");
        registerButton = new JButton("Register");

        loginButton.addActionListener(e -> showLoginDialog());
        registerButton.addActionListener(e -> showRegisterDialog());

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        panel.add(loginButton, constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 2;
        panel.add(registerButton, constraints);

        return panel;
    }

    private void login() {
        final String name = nameField.getText();
        final String password = new String(passwordField.getPassword());

        if (name.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter both name and password", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            final String selectedUserType = (String) userTypeDropdown.getSelectedItem();

            // Check if the user is a teacher
            if ("Teacher".equals(selectedUserType)) {
                final PreparedStatement teacherStatement = connection.prepareStatement(
                        "SELECT * FROM teachers WHERE name = ? AND password = ?");
                teacherStatement.setString(1, name);
                teacherStatement.setString(2, password);

                final ResultSet teacherResultSet = teacherStatement.executeQuery();

                if (teacherResultSet.next()) {
                    loginDialog.dispose();
                    showTeacherDashboard();
                    return;
                }
            }
            // Check if the user is a student
            else if ("Student".equals(selectedUserType)) {
                final PreparedStatement studentStatement = connection.prepareStatement(
                        "SELECT * FROM students WHERE name = ? AND password = ?");
                studentStatement.setString(1, name);
                studentStatement.setString(2, password);

                final ResultSet studentResultSet = studentStatement.executeQuery();

                if (studentResultSet.next()) {
                    loginDialog.dispose();
                    showStudentDashboard();
                    return;
                }
            }

            JOptionPane.showMessageDialog(frame, "Incorrect name or password", "Login Failed",
                    JOptionPane.ERROR_MESSAGE);

        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

    private void register() {
        final String name = nameField.getText();
        final String password = new String(passwordField.getPassword());

        if (name.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter both name and password", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        final String userType = (String) userTypeDropdown.getSelectedItem();

        try {
            // Check if the name already exists
            if (isNameAlreadyExists(name, userType)) {
                JOptionPane.showMessageDialog(frame, "Name already exists, please choose another one", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (isInvalidName(name)) {
                JOptionPane.showMessageDialog(frame, "Please follow this format:\nAlexis C. Corporal", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            PreparedStatement preparedStatement;
            if ("Student".equals(userType)) {
                preparedStatement = connection.prepareStatement(
                        "INSERT INTO students (name, password) VALUES (?,?)");
            } else {
                preparedStatement = connection.prepareStatement(
                        "INSERT INTO teachers (name, password, subject) VALUES (?, ?, ?)");
                preparedStatement.setString(3, (String) subjectsDropdown.getSelectedItem());
            }

            preparedStatement.setString(1, name);
            preparedStatement.setString(2, password);

            final int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(frame, "Registration successful", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                // Close the dialog only if registration is successful
                registerDialog.dispose();
            } else {
                JOptionPane.showMessageDialog(frame, "Registration failed", "Error", JOptionPane.ERROR_MESSAGE);
            }

        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

    private void showLoginDialog() {
        loginDialog = new JDialog(frame, "Login", true);
        loginDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        loginDialog.setSize(400, 200);
        loginDialog.setLocationRelativeTo(frame);
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);

        final JPanel loginPanel = new JPanel(new GridLayout(3, 2));

        loginPanel.add(new JLabel("Name:"));
        nameField = new JTextField(20);
        loginPanel.add(nameField);

        loginPanel.add(new JLabel("Password:"));
        passwordField = new JPasswordField(20);
        loginPanel.add(passwordField);

        userTypeDropdown = new JComboBox<>(new String[] { "Student", "Teacher" });
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 2;
        loginPanel.add(userTypeDropdown, constraints);

        final JButton loginButton = new JButton("Login");
        loginButton.addActionListener(e -> {
            login();
        });
        loginPanel.add(loginButton);

        loginDialog.add(loginPanel);
        loginDialog.setVisible(true);
    }

    private void showRegisterDialog() {
        registerDialog = new JDialog(frame, "Register", true);
        registerDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        registerDialog.setSize(400, 200);
        registerDialog.setLocationRelativeTo(frame);
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);

        final JPanel registerPanel = new JPanel(new GridLayout(4, 2));

        registerPanel.add(new JLabel("Name:"));
        nameField = new JTextField(20);
        registerPanel.add(nameField);

        registerPanel.add(new JLabel("Password:"));
        passwordField = new JPasswordField(20);
        registerPanel.add(passwordField);

        userTypeDropdown = new JComboBox<>(new String[] { "Student", "Teacher" });
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 2;
        registerPanel.add(userTypeDropdown, constraints);

        // Add a new JComboBox for subjects
        subjectsDropdown = new JComboBox<>(
                new String[] { "IM211", "CC214", "MS121", "PE3", "GE105", "GE106", "NET212", "ITELECTV", "GENSOC" });
        subjectsDropdown.setEnabled(false); // Initially disabled
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.gridwidth = 2;

        userTypeDropdown.addActionListener(e -> {
            // Enable the subjects dropdown only if the user type is "Teacher"
            subjectsDropdown.setEnabled("Teacher".equals(userTypeDropdown.getSelectedItem()));

            // Fetch subjects already selected by other teachers
            final List<String> subjectsSelectedByOtherTeachers = getSubjectsSelectedByOtherTeachers();

            // Remove already selected subjects from the dropdown
            for (final String subject : subjectsSelectedByOtherTeachers) {
                subjectsDropdown.removeItem(subject);
            }
        });

        registerPanel.add(subjectsDropdown, constraints);

        final JButton registerButton = new JButton("Register");
        registerButton.addActionListener(e -> {
            register();
        });
        registerPanel.add(registerButton);

        registerDialog.add(registerPanel);
        registerDialog.setVisible(true);
    }

    private void searchStudentsByName(final String query, final JTable table) {
        final SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    final String selectedSubject = getTeacherSubject();
                    final String searchQuery = "SELECT id, name, " + selectedSubject
                            + " FROM students WHERE name LIKE ?";
                    final PreparedStatement searchStatement = connection.prepareStatement(searchQuery);
                    searchStatement.setString(1, "%" + query + "%");

                    final ResultSet resultSet = searchStatement.executeQuery();

                    final String[] columnNames = { "ID", "Name", selectedSubject };
                    final DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
                        @Override
                        public boolean isCellEditable(final int row, final int column) {
                            return false; // Make the table not editable
                        }
                    };

                    while (resultSet.next()) {
                        final Object[] rowData = {
                                resultSet.getInt("id"),
                                resultSet.getString("name"),
                                resultSet.getInt(selectedSubject.toLowerCase()),
                        };
                        tableModel.addRow(rowData);
                    }

                    // Update the table model on the event dispatch thread
                    SwingUtilities.invokeLater(() -> table.setModel(tableModel));

                } catch (final SQLException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };

        // Execute the SwingWorker
        worker.execute();
    }

    private JTable createTable(final DefaultTableModel tableModel) {
        final JTable table = new JTable(tableModel) {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return getPreferredSize().width < getParent().getWidth();
            }
        };

        table.getTableHeader().setReorderingAllowed(false); // Disable column rearrangement
        final int[] columnWidths = { 50, 100, 60 };
        for (int i = 0; i < columnWidths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(columnWidths[i]);
        }

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return table;
    }

    private void showTeacherDashboard() {
        frame.getContentPane().removeAll();
        frame.repaint();
        frame.setLocationRelativeTo(null);

        try {
            final String selectedSubject = getTeacherSubject();
            final ResultSet resultSet = statement
                    .executeQuery("SELECT id, name, " + selectedSubject + " FROM students");

            // Create a table to display student information
            final String[] columnNames = { "ID", "Name", selectedSubject };
            final DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(final int row, final int column) {
                    return false; // Make the table not editable
                }
            };

            while (resultSet.next()) {
                final Object[] rowData = {
                        resultSet.getInt("id"),
                        resultSet.getString("name"),
                        resultSet.getInt(selectedSubject.toLowerCase()),
                };
                tableModel.addRow(rowData);
            }

            final JTable table = createTable(tableModel);

            final JScrollPane scrollPane = new JScrollPane(table);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            final JPanel searchPanel = new JPanel();
            final JTextField searchField = new JTextField(20);

            searchPanel.add(new JLabel("Search by Name:"));
            searchPanel.add(searchField);
            searchField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(final DocumentEvent e) {
                    searchStudentsByName(searchField.getText(), table);
                }

                @Override
                public void removeUpdate(final DocumentEvent e) {
                    searchStudentsByName(searchField.getText(), table);
                }

                @Override
                public void changedUpdate(final DocumentEvent e) {
                    // Not needed for plain text fields
                }
            });

            final JPanel buttonPanel = new JPanel();
            final JButton logoutButton = new JButton("Logout");
            logoutButton.addActionListener(e -> logout());
            buttonPanel.add(logoutButton);

            final JButton editButton = new JButton("Edit Student");
            editButton.addActionListener(e -> {
                final int selectedRow = table.getSelectedRow();
                if (selectedRow != -1) {
                    editStudent((int) table.getValueAt(selectedRow, 0));
                }
            });
            buttonPanel.add(editButton);

            final JButton deleteAccountButton = new JButton("Delete Account");
            deleteAccountButton.addActionListener(e -> deleteTeacherAccount());
            buttonPanel.add(deleteAccountButton);

            frame.setLayout(new BorderLayout());
            frame.add(searchPanel, BorderLayout.NORTH);
            frame.add(scrollPane, BorderLayout.CENTER);
            frame.add(buttonPanel, BorderLayout.SOUTH);
            frame.pack();
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

    private void showStudentDashboard() {
        frame.getContentPane().removeAll();
        frame.repaint();
        frame.setLocationRelativeTo(null);

        try {
            ResultSet resultSet;
            resultSet = statement
                    .executeQuery("SELECT * FROM students WHERE name = '" + nameField.getText() + "'");

            final String[] columnNames = { "ID", "Name", "IM211", "CC214", "MS121", "PE3", "GE105", "GE106", "NET212",
                    "ITELECTV", "GENSOC", "Average Grade", "Status" };
            final DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(final int row, final int column) {
                    return false; // Make the table not editable
                }
            };

            while (resultSet.next()) {
                final Object[] rowData = {
                        resultSet.getInt("id"),
                        resultSet.getString("name"),
                        resultSet.getInt("IM211"),
                        resultSet.getInt("CC214"),
                        resultSet.getInt("MS121"),
                        resultSet.getInt("PE3"),
                        resultSet.getInt("GE105"),
                        resultSet.getInt("GE106"),
                        resultSet.getInt("NET212"),
                        resultSet.getInt("ITELECTV"),
                        resultSet.getInt("GENSOC"),
                        resultSet.getInt("average_grade"),
                        resultSet.getString("status")
                };
                tableModel.addRow(rowData);
            }
            final JTable table = new JTable(tableModel) {
                @Override
                public boolean getScrollableTracksViewportWidth() {
                    return getPreferredSize().width < getParent().getWidth();
                }
            };

            table.getTableHeader().setReorderingAllowed(false); // Disable column rearrangement
            final int[] columnWidths = { 50, 100, 60, 60, 60, 60, 60, 60, 60, 80, 60, 100, 80 };
            for (int i = 0; i < columnWidths.length; i++) {
                table.getColumnModel().getColumn(i).setPreferredWidth(columnWidths[i]);
            }

            // Wrap the table in a JScrollPane to enable horizontal scrolling
            final JScrollPane scrollPane = new JScrollPane(table);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            final JPanel buttonPanel = new JPanel();

            final JButton logoutButton = new JButton("Logout");
            logoutButton.addActionListener(e -> logout());
            buttonPanel.add(logoutButton);
            final JButton deleteAccountButton = new JButton("Delete Account");
            deleteAccountButton.addActionListener(e -> deleteStudentAccount());
            buttonPanel.add(deleteAccountButton);
            frame.setLayout(new BorderLayout());
            frame.add(scrollPane, BorderLayout.CENTER);
            frame.add(buttonPanel, BorderLayout.SOUTH);
            frame.pack();
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

    private void editStudent(final int studentId) {
        final JTextField im211Field = new JTextField();
        final JTextField cc214Field = new JTextField();
        final JTextField ms121Field = new JTextField();
        final JTextField pe3Field = new JTextField();
        final JTextField ge105Field = new JTextField();
        final JTextField ge106Field = new JTextField();
        final JTextField net212Field = new JTextField();
        final JTextField itelectvField = new JTextField();
        final JTextField gensocField = new JTextField();

        try {
            final PreparedStatement selectStatement = connection
                    .prepareStatement("SELECT * FROM students WHERE id = ?");
            selectStatement.setInt(1, studentId);
            final ResultSet resultSet = selectStatement.executeQuery();
            final String selectedSubject = getTeacherSubject();
            final JPanel panel = new JPanel(new GridLayout(0, 1));
            if (resultSet.next()) {
                final ArrayList<JTextField> gradeFields = new ArrayList<>(
                        Arrays.asList(im211Field, cc214Field, ms121Field, pe3Field,
                                ge105Field, ge106Field, net212Field, itelectvField, gensocField));

                final ArrayList<String> subjectCodes = new ArrayList<>(
                        Arrays.asList("IM211", "CC214", "MS121", "PE3", "GE105", "GE106", "NET212", "ITELECTV",
                                "GENSOC"));

                final int totalSubjects = gradeFields.size();

                for (int i = 0; i < gradeFields.size(); i++) {
                    final boolean isSelectedSubject = selectedSubject != null
                            && selectedSubject.equalsIgnoreCase(getSubjectCode(i));

                    if (isSelectedSubject) {
                        gradeFields.get(i).setText(String.valueOf(resultSet.getInt(subjectCodes.get(i))));
                        gradeFields.get(i).setEnabled(true);
                        gradeFields.get(i).setVisible(true);

                        panel.add(new JLabel(getSubjectCode(i) + ":"));
                        panel.add(gradeFields.get(i));
                    } else {
                        gradeFields.get(i).setText(String.valueOf(resultSet.getInt(subjectCodes.get(i))));
                        gradeFields.get(i).setVisible(false);
                    }
                }

                final int result = JOptionPane.showConfirmDialog(frame, panel, "Edit Student",
                        JOptionPane.OK_CANCEL_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    // Obtain individual grades from text fields for update
                    final int im211 = Integer.parseInt(im211Field.getText());
                    final int cc214 = Integer.parseInt(cc214Field.getText());
                    final int ms121 = Integer.parseInt(ms121Field.getText());
                    final int pe3 = Integer.parseInt(pe3Field.getText());
                    final int ge105 = Integer.parseInt(ge105Field.getText());
                    final int ge106 = Integer.parseInt(ge106Field.getText());
                    final int net212 = Integer.parseInt(net212Field.getText());
                    final int itelectv = Integer.parseInt(itelectvField.getText());
                    final int gensoc = Integer.parseInt(gensocField.getText());
                    final int averageGrade = (im211 + cc214 + ms121 + pe3 + ge105 + ge106 + net212 + itelectv + gensoc)
                            / totalSubjects;
                    final String status = (averageGrade < 75) ? "Failed" : "Passed";

                    final PreparedStatement preparedStatement = connection.prepareStatement(
                            "UPDATE students SET IM211 = ?, CC214 = ?, MS121 = ?, PE3 = ?, GE105 = ?, GE106 = ?, NET212 = ?, ITELECTV = ?, GENSOC = ?, average_grade = ?, status = ? WHERE id = ?");
                    preparedStatement.setInt(1, im211);
                    preparedStatement.setInt(2, cc214);
                    preparedStatement.setInt(3, ms121);
                    preparedStatement.setInt(4, pe3);
                    preparedStatement.setInt(5, ge105);
                    preparedStatement.setInt(6, ge106);
                    preparedStatement.setInt(7, net212);
                    preparedStatement.setInt(8, itelectv);
                    preparedStatement.setInt(9, gensoc);
                    preparedStatement.setInt(10, averageGrade);
                    preparedStatement.setString(11, status);
                    preparedStatement.setInt(12, studentId);

                    if (!isValidGradeRange(im211) || !isValidGradeRange(cc214)
                            || !isValidGradeRange(ms121) ||
                            !isValidGradeRange(pe3) || !isValidGradeRange(ge105) || !isValidGradeRange(ge106) ||
                            !isValidGradeRange(net212) || !isValidGradeRange(itelectv) || !isValidGradeRange(gensoc)) {
                        JOptionPane.showMessageDialog(frame, "Please enter valid grades for all fields", "Error",
                                JOptionPane.ERROR_MESSAGE);
                        editStudent(studentId);
                        return;
                    }

                    final int rowsAffected = preparedStatement.executeUpdate();
                    if (rowsAffected > 0) {
                        JOptionPane.showMessageDialog(frame, "Student updated successfully", "Success",
                                JOptionPane.INFORMATION_MESSAGE);
                        showTeacherDashboard();
                    } else {
                        JOptionPane.showMessageDialog(frame, "Failed to update student", "Error",
                                JOptionPane.ERROR_MESSAGE);
                        editStudent(studentId);
                    }
                }
            }

        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

    private void confirmExit() {
        final int option = JOptionPane.showConfirmDialog(null, "Are you sure you want to exit?", "Exit",
                JOptionPane.YES_NO_OPTION);
        if (option == JOptionPane.YES_OPTION) {
            // If the user clicks "Yes," exit the application
            System.exit(0);
        }
    }

    private void logout() {
        // Clear name and password fields
        nameField.setText("");
        passwordField.setText("");

        // Reset user type dropdown to default
        userTypeDropdown.setSelectedIndex(0);

        // Show the login panel
        showLoginPanel();
    }

    private void showLoginPanel() {
        frame.getContentPane().removeAll();
        frame.add(createMainScreen());
        frame.repaint();
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private boolean isValidGradeRange(final double grade) {
        return grade >= 50 && grade <= 100;
    }

    private boolean isInvalidName(final String name) {
        // Use regex to check if the name contains only alphabets
        return !((name.matches("^[a-zA-Z.]+$")) || (name.contains(" ")));
    }

    private String getTeacherSubject() throws SQLException {
        final String name = nameField.getText();
        final PreparedStatement teacherSubjectStatement = connection.prepareStatement(
                "SELECT subject FROM teachers WHERE name = ?");
        teacherSubjectStatement.setString(1, name);
        final ResultSet subjectResultSet = teacherSubjectStatement.executeQuery();

        return subjectResultSet.next() ? subjectResultSet.getString("subject") : null;
    }

    private void deleteStudentAccount() {
        final int option = JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete your account?",
                "Delete Account",
                JOptionPane.YES_NO_OPTION);

        if (option == JOptionPane.YES_OPTION) {
            try {
                // Delete the student account
                final String name = nameField.getText();
                final PreparedStatement deleteStatement = connection
                        .prepareStatement("DELETE FROM students WHERE name = ?");
                deleteStatement.setString(1, name);

                final int rowsAffected = deleteStatement.executeUpdate();

                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(frame, "Account deleted successfully", "Success",
                            JOptionPane.INFORMATION_MESSAGE);

                    // After deleting the account, log out
                    logout();
                } else {
                    JOptionPane.showMessageDialog(frame, "Failed to delete account", "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (final SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    private String getSubjectCode(final int index) {
        final String[] subjectCodes = { "IM211", "CC214", "MS121", "PE3", "GE105", "GE106", "NET212", "ITELECTV",
                "GENSOC" };
        return subjectCodes[index];
    }

    // Check if the name already exists
    private boolean isNameAlreadyExists(final String name, final String userType) throws SQLException {
        final String tableName = "Student".equals(userType) ? "students" : "teachers";
        final String query = "SELECT COUNT(*) FROM " + tableName + " WHERE name = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, name.toLowerCase());
            final ResultSet resultSet = statement.executeQuery();

            resultSet.next();
            final int count = resultSet.getInt(1);

            return count > 0;
        }
    }

    private void deleteTeacherAccount() {
        final int option = JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete your account?",
                "Delete Account",
                JOptionPane.YES_NO_OPTION);

        if (option == JOptionPane.YES_OPTION) {
            try {
                // Delete the teacher account
                final String name = nameField.getText();
                final PreparedStatement deleteStatement = connection
                        .prepareStatement("DELETE FROM teachers WHERE name = ?");
                deleteStatement.setString(1, name);

                final int rowsAffected = deleteStatement.executeUpdate();

                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(frame, "Account deleted successfully", "Success",
                            JOptionPane.INFORMATION_MESSAGE);

                    // After deleting the account, log out
                    logout();
                } else {
                    JOptionPane.showMessageDialog(frame, "Failed to delete account", "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (final SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    private List<String> getSubjectsSelectedByOtherTeachers() {
        final List<String> subjects = new ArrayList<>();
        try {
            final String query = "SELECT subject FROM teachers WHERE name <> ?";
            final PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, nameField.getText()); // Exclude the current teacher from the query
            final ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                subjects.add(resultSet.getString("subject"));
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        return subjects;
    }
}
