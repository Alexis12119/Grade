/*
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
    im211 INT DEFAULT 99,
    cc214 INT DEFAULT 99,
    ms121 INT DEFAULT 99,
    pe3 INT DEFAULT 99,
    ge105 INT DEFAULT 99,
    ge106 INT DEFAULT 99,
    net212 INT DEFAULT 99,
    itelectv INT DEFAULT 99,
    gensoc INT DEFAULT 99,
    average_grade INT DEFAULT 99,
    status VARCHAR(255) DEFAULT 'Passed'
);
*/

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.event.DocumentEvent;

public class GradingSystem {
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/grading_system";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "alexis";

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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GradingSystem().createAndShowGUI());
    }

    private void createAndShowGUI() {
        frame = new JFrame("Grading System");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmExit();
            }
        });

        JPanel panel = createMainScreen();
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private JPanel createMainScreen() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
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
        String name = nameField.getText();
        String password = new String(passwordField.getPassword());

        if (name.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter both name and password", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            String selectedUserType = (String) userTypeDropdown.getSelectedItem();

            // Check if the user is a teacher
            if ("Teacher".equals(selectedUserType)) {
                PreparedStatement teacherStatement = connection.prepareStatement(
                        "SELECT * FROM teachers WHERE name = ? AND password = ?");
                teacherStatement.setString(1, name);
                teacherStatement.setString(2, password);

                ResultSet teacherResultSet = teacherStatement.executeQuery();

                if (teacherResultSet.next()) {
                    loginDialog.dispose();
                    showTeacherDashboard();
                    return;
                }
            }
            // Check if the user is a student
            else if ("Student".equals(selectedUserType)) {
                PreparedStatement studentStatement = connection.prepareStatement(
                        "SELECT * FROM students WHERE name = ? AND password = ?");
                studentStatement.setString(1, name);
                studentStatement.setString(2, password);

                ResultSet studentResultSet = studentStatement.executeQuery();

                if (studentResultSet.next()) {
                    loginDialog.dispose();
                    showStudentDashboard();
                    return;
                }
            }

            JOptionPane.showMessageDialog(frame, "Incorrect name or password", "Login Failed",
                    JOptionPane.ERROR_MESSAGE);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void register() {
        String name = nameField.getText();
        String password = new String(passwordField.getPassword());

        if (name.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter both name and password", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String userType = (String) userTypeDropdown.getSelectedItem();

        try {
            // Check if the name already exists
            if (isNameAlreadyExists(name, userType)) {
                JOptionPane.showMessageDialog(frame, "Name already exists, please choose another one", "Error",
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

            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(frame, "Registration successful", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                // Close the dialog only if registration is successful
                registerDialog.dispose();
            } else {
                JOptionPane.showMessageDialog(frame, "Registration failed", "Error", JOptionPane.ERROR_MESSAGE);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showLoginDialog() {
        loginDialog = new JDialog(frame, "Login", true);
        loginDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        loginDialog.setSize(400, 200);
        loginDialog.setLocationRelativeTo(frame);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);

        JPanel loginPanel = new JPanel(new GridLayout(3, 2));

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

        JButton loginButton = new JButton("Login");
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
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);

        JPanel registerPanel = new JPanel(new GridLayout(4, 2));

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
            List<String> subjectsSelectedByOtherTeachers = getSubjectsSelectedByOtherTeachers();

            // Remove already selected subjects from the dropdown
            for (String subject : subjectsSelectedByOtherTeachers) {
                subjectsDropdown.removeItem(subject);
            }
        });

        registerPanel.add(subjectsDropdown, constraints);

        JButton registerButton = new JButton("Register");
        registerButton.addActionListener(e -> {
            register();
        });
        registerPanel.add(registerButton);

        registerDialog.add(registerPanel);
        registerDialog.setVisible(true);
    }

    private void searchStudentsByName(String query, JTable table) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    String selectedSubject = getTeacherSubject();
                    String searchQuery = "SELECT id, name, " + selectedSubject + " FROM students WHERE name LIKE ?";
                    PreparedStatement searchStatement = connection.prepareStatement(searchQuery);
                    searchStatement.setString(1, "%" + query + "%");

                    ResultSet resultSet = searchStatement.executeQuery();

                    String[] columnNames = { "ID", "Name", selectedSubject };
                    DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
                        @Override
                        public boolean isCellEditable(int row, int column) {
                            return false; // Make the table not editable
                        }
                    };

                    while (resultSet.next()) {
                        Object[] rowData = {
                                resultSet.getInt("id"),
                                resultSet.getString("name"),
                                resultSet.getInt(selectedSubject.toLowerCase()),
                        };
                        tableModel.addRow(rowData);
                    }

                    // Update the table model on the event dispatch thread
                    SwingUtilities.invokeLater(() -> table.setModel(tableModel));

                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };

        // Execute the SwingWorker
        worker.execute();
    }

    private JTable createTable(DefaultTableModel tableModel) {
        JTable table = new JTable(tableModel) {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return getPreferredSize().width < getParent().getWidth();
            }
        };

        table.getTableHeader().setReorderingAllowed(false); // Disable column rearrangement
        int[] columnWidths = { 50, 100, 60 };
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
            String selectedSubject = getTeacherSubject();
            ResultSet resultSet = statement.executeQuery("SELECT id, name, " + selectedSubject + " FROM students");

            // Create a table to display student information
            String[] columnNames = { "ID", "Name", selectedSubject };
            DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false; // Make the table not editable
                }
            };

            while (resultSet.next()) {
                Object[] rowData = {
                        resultSet.getInt("id"),
                        resultSet.getString("name"),
                        resultSet.getInt(selectedSubject.toLowerCase()),
                };
                tableModel.addRow(rowData);
            }

            JTable table = createTable(tableModel);

            JScrollPane scrollPane = new JScrollPane(table);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            JPanel searchPanel = new JPanel();
            JTextField searchField = new JTextField(20);

            searchPanel.add(new JLabel("Search by Name:"));
            searchPanel.add(searchField);
            searchField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    searchStudentsByName(searchField.getText(), table);
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    searchStudentsByName(searchField.getText(), table);
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    // Not needed for plain text fields
                }
            });

            JPanel buttonPanel = new JPanel();
            JButton logoutButton = new JButton("Logout");
            logoutButton.addActionListener(e -> logout());
            buttonPanel.add(logoutButton);

            JButton editButton = new JButton("Edit Student");
            editButton.addActionListener(e -> {
                int selectedRow = table.getSelectedRow();
                if (selectedRow != -1) {
                    editStudent((int) table.getValueAt(selectedRow, 0));
                }
            });
            buttonPanel.add(editButton);

            JButton deleteAccountButton = new JButton("Delete Account");
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
        } catch (SQLException e) {
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

            String[] columnNames = { "ID", "Name", "IM211", "CC214", "MS121", "PE3", "GE105", "GE106", "NET212",
                    "ITELECTV", "GENSOC", "Average Grade", "Status" };
            DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false; // Make the table not editable
                }
            };

            while (resultSet.next()) {
                Object[] rowData = {
                        resultSet.getInt("id"),
                        resultSet.getString("name"),
                        resultSet.getInt("im211"),
                        resultSet.getInt("cc214"),
                        resultSet.getInt("ms121"),
                        resultSet.getInt("pe3"),
                        resultSet.getInt("ge105"),
                        resultSet.getInt("ge106"),
                        resultSet.getInt("net212"),
                        resultSet.getInt("itelectv"),
                        resultSet.getInt("gensoc"),
                        resultSet.getInt("average_grade"),
                        resultSet.getString("status")
                };
                tableModel.addRow(rowData);
            }
            JTable table = new JTable(tableModel) {
                @Override
                public boolean getScrollableTracksViewportWidth() {
                    return getPreferredSize().width < getParent().getWidth();
                }
            };

            table.getTableHeader().setReorderingAllowed(false); // Disable column rearrangement
            int[] columnWidths = { 50, 100, 60, 60, 60, 60, 60, 60, 60, 80, 60, 100, 80 };
            for (int i = 0; i < columnWidths.length; i++) {
                table.getColumnModel().getColumn(i).setPreferredWidth(columnWidths[i]);
            }

            // Wrap the table in a JScrollPane to enable horizontal scrolling
            JScrollPane scrollPane = new JScrollPane(table);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            JPanel buttonPanel = new JPanel();

            JButton logoutButton = new JButton("Logout");
            logoutButton.addActionListener(e -> logout());
            buttonPanel.add(logoutButton);
            JButton deleteAccountButton = new JButton("Delete Account");
            deleteAccountButton.addActionListener(e -> deleteStudentAccount());
            buttonPanel.add(deleteAccountButton);
            frame.setLayout(new BorderLayout());
            frame.add(scrollPane, BorderLayout.CENTER);
            frame.add(buttonPanel, BorderLayout.SOUTH);
            frame.pack();
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void editStudent(int studentId) {
        JTextField im211Field = new JTextField();
        JTextField cc214Field = new JTextField();
        JTextField ms121Field = new JTextField();
        JTextField pe3Field = new JTextField();
        JTextField ge105Field = new JTextField();
        JTextField ge106Field = new JTextField();
        JTextField net212Field = new JTextField();
        JTextField itelectvField = new JTextField();
        JTextField gensocField = new JTextField();
        JTextField averageGradeField = new JTextField();
        JTextField statusField = new JTextField();

        try {
            PreparedStatement selectStatement = connection.prepareStatement("SELECT * FROM students WHERE id = ?");
            selectStatement.setInt(1, studentId);
            ResultSet resultSet = selectStatement.executeQuery();
            if (resultSet.next()) {
                im211Field.setText(String.valueOf(resultSet.getInt("im211")));
                cc214Field.setText(String.valueOf(resultSet.getInt("cc214")));
                ms121Field.setText(String.valueOf(resultSet.getInt("ms121")));
                pe3Field.setText(String.valueOf(resultSet.getInt("pe3")));
                ge105Field.setText(String.valueOf(resultSet.getInt("ge105")));
                ge106Field.setText(String.valueOf(resultSet.getInt("ge106")));
                net212Field.setText(String.valueOf(resultSet.getInt("net212")));
                itelectvField.setText(String.valueOf(resultSet.getInt("itelectv")));
                gensocField.setText(String.valueOf(resultSet.getInt("gensoc")));
                averageGradeField.setText(String.valueOf(resultSet.getInt("average_grade")));
                statusField.setText(resultSet.getString("status"));
                String selectedSubject = getTeacherSubject();

                ArrayList<JTextField> subjectFields = new ArrayList<>(
                        Arrays.asList(im211Field, cc214Field, ms121Field, pe3Field,
                                ge105Field, ge106Field, net212Field, itelectvField, gensocField));
                // Enable or disable the subject text fields based on the selected subject
                for (int i = 0; i < subjectFields.size(); i++) {
                    subjectFields.get(i)
                            .setEnabled(selectedSubject != null && selectedSubject.equalsIgnoreCase(getSubjectCode(i)));
                }
                JPanel panel = new JPanel(new GridLayout(0, 1));
                panel.add(new JLabel("IM211:"));
                panel.add(im211Field);
                panel.add(new JLabel("CC214:"));
                panel.add(cc214Field);
                panel.add(new JLabel("MS121:"));
                panel.add(ms121Field);
                panel.add(new JLabel("PE3:"));
                panel.add(pe3Field);
                panel.add(new JLabel("GE105:"));
                panel.add(ge105Field);
                panel.add(new JLabel("GE106:"));
                panel.add(ge106Field);
                panel.add(new JLabel("NET212:"));
                panel.add(net212Field);
                panel.add(new JLabel("ITELECTV:"));
                panel.add(itelectvField);
                panel.add(new JLabel("GENSOC:"));
                panel.add(gensocField);

                int result = JOptionPane.showConfirmDialog(frame, panel, "Edit Student", JOptionPane.OK_CANCEL_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    int im211 = Integer.parseInt(im211Field.getText());
                    int cc214 = Integer.parseInt(cc214Field.getText());
                    int ms121 = Integer.parseInt(ms121Field.getText());
                    int pe3 = Integer.parseInt(pe3Field.getText());
                    int ge105 = Integer.parseInt(ge105Field.getText());
                    int ge106 = Integer.parseInt(ge106Field.getText());
                    int net212 = Integer.parseInt(net212Field.getText());
                    int itelectv = Integer.parseInt(itelectvField.getText());
                    int gensoc = Integer.parseInt(gensocField.getText());
                    int averageGrade = (im211 + cc214 + ms121 + pe3 + ge105 + ge106 + net212 + itelectv + gensoc) / 9;
                    String status = "Passed";
                    if (averageGrade < 75) {
                        status = "Failed";
                    }

                    PreparedStatement preparedStatement = connection.prepareStatement(
                            "UPDATE students SET im211 = ?, cc214 = ?, ms121 = ?, pe3 = ?, ge105 = ?, ge106 = ?, net212 = ?, itelectv = ?, gensoc = ?, average_grade = ?, status = ? WHERE id = ?");
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

                    int rowsAffected = preparedStatement.executeUpdate();
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

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void confirmExit() {
        int option = JOptionPane.showConfirmDialog(null, "Are you sure you want to exit?", "Exit",
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

    private boolean isValidGradeRange(double grade) {
        return grade >= 50 && grade <= 100;
    }

    private String getTeacherSubject() throws SQLException {
        String name = nameField.getText();
        PreparedStatement teacherSubjectStatement = connection.prepareStatement(
                "SELECT subject FROM teachers WHERE name = ?");
        teacherSubjectStatement.setString(1, name);
        ResultSet subjectResultSet = teacherSubjectStatement.executeQuery();

        return subjectResultSet.next() ? subjectResultSet.getString("subject") : null;
    }

    private void deleteStudentAccount() {
        int option = JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete your account?",
                "Delete Account",
                JOptionPane.YES_NO_OPTION);

        if (option == JOptionPane.YES_OPTION) {
            try {
                // Delete the student account
                String name = nameField.getText();
                PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM students WHERE name = ?");
                deleteStatement.setString(1, name);

                int rowsAffected = deleteStatement.executeUpdate();

                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(frame, "Account deleted successfully", "Success",
                            JOptionPane.INFORMATION_MESSAGE);

                    // After deleting the account, log out
                    logout();
                } else {
                    JOptionPane.showMessageDialog(frame, "Failed to delete account", "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    private String getSubjectCode(int index) {
        String[] subjectCodes = { "im211", "cc214", "ms121", "pe3", "ge105", "ge106", "net212", "itelectv", "gensoc" };
        return subjectCodes[index];
    }

    // Check if the name already exists
    private boolean isNameAlreadyExists(String name, String userType) throws SQLException {
        String tableName = "Student".equals(userType) ? "students" : "teachers";
        String query = "SELECT COUNT(*) FROM " + tableName + " WHERE name = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, name.toLowerCase());
            ResultSet resultSet = statement.executeQuery();

            resultSet.next();
            int count = resultSet.getInt(1);

            return count > 0;
        }
    }

    private void deleteTeacherAccount() {
        int option = JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete your account?",
                "Delete Account",
                JOptionPane.YES_NO_OPTION);

        if (option == JOptionPane.YES_OPTION) {
            try {
                // Delete the teacher account
                String name = nameField.getText();
                PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM teachers WHERE name = ?");
                deleteStatement.setString(1, name);

                int rowsAffected = deleteStatement.executeUpdate();

                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(frame, "Account deleted successfully", "Success",
                            JOptionPane.INFORMATION_MESSAGE);

                    // After deleting the account, log out
                    logout();
                } else {
                    JOptionPane.showMessageDialog(frame, "Failed to delete account", "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    private List<String> getSubjectsSelectedByOtherTeachers() {
        List<String> subjects = new ArrayList<>();
        try {
            String query = "SELECT subject FROM teachers WHERE name <> ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, nameField.getText()); // Exclude the current teacher from the query
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                subjects.add(resultSet.getString("subject"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return subjects;
    }
}
