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
    IM211 INT DEFAULT 50,
    CC214 INT DEFAULT 50,
    MS121 INT DEFAULT 50,
    PE3 INT DEFAULT 50,
    GE105 INT DEFAULT 50,
    GE106 INT DEFAULT 50,
    NET212 INT DEFAULT 50,
    ITELECTV INT DEFAULT 50,
    GENSOC INT DEFAULT 50,
    average_grade INT DEFAULT 50,
    status VARCHAR(255) DEFAULT 'Failed'
);
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.ImageIcon;

import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import keeptoo.KGradientPanel;

public class GradingSystem {

    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/grading_system";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "alexis";

    public static void main(final String[] args) {
        SwingUtilities.invokeLater(() -> new GradingSystem().createAndShowGUI());
    }

    // Instance variables
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

    // Create the GUI
    private void createAndShowGUI() {
        frame = new JFrame("AcuGrade");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent event) {
                confirmExit();
            }
        });

        final JPanel panel = createMainScreen();
        frame.getContentPane().add(panel);
        frame.getContentPane().setPreferredSize(new Dimension(800, 400));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Start the database
        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
            statement = connection.createStatement();
        } catch (final SQLException event) {
            event.printStackTrace();
        }
    }

    public JPanel createMainScreen() {
        // Use KGradientPanel instead of JPanel
        final KGradientPanel panel = new KGradientPanel();

        // Set the gradient colors
        panel.setkStartColor(new Color(62, 23, 25));  // Dark violet color
        panel.setkEndColor(new Color(187, 20, 53));
        panel.setkGradientFocus(0);

        panel.setLayout(new GridBagLayout());
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);

        nameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        loginButton = new JButton("Login");
        registerButton = new JButton("Register");

        // Set pastel white color for buttons
        Color pastelWhite = new Color(240, 240, 240);  // Adjust as needed
        loginButton.setBackground(pastelWhite);
        registerButton.setBackground(pastelWhite);

        loginButton.addActionListener(event -> showLoginDialog());
        registerButton.addActionListener(event -> showRegisterDialog());

        // Logo image label
        JLabel logoImageLabel = new JLabel();
        logoImageLabel.setIcon(new ImageIcon(getClass().getResource("logo.png")));

        // Create a panel to hold both labels horizontally
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        logoPanel.setOpaque(false); // Make the panel transparent
        logoPanel.add(logoImageLabel);

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        panel.add(logoPanel, constraints);

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

    private void loginAction() {
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
            } // Check if the user is a student
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

            JOptionPane.showMessageDialog(frame, "Incorrect name, password or role", "Login Failed",
                    JOptionPane.ERROR_MESSAGE);

        } catch (final SQLException event) {
            event.printStackTrace();
        }
    }

    private void registerAction() {
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

            if (!isValidPassword(password)) {

                JOptionPane.showMessageDialog(frame, "Password should contain uppercase, lowercase, and digit.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if(!isValidLength(password)) {
                JOptionPane.showMessageDialog(frame, "Password length should be > 6.", "Error",
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

        } catch (final SQLException event) {
            event.printStackTrace();
        }
    }

    public static boolean isValidPassword(String password) {
        // Check if it contains at least one uppercase, lowercase, and digit
        String regex = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).+$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(password);

        return matcher.matches();
    }

    private void showLoginDialog() {
        loginDialog = new JDialog(frame, "Login", true);
        loginDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        loginDialog.setSize(400, 200);
        loginDialog.setLocationRelativeTo(frame);

        final KGradientPanel loginPanel = new KGradientPanel(); // Replace with the actual class from your library
        loginPanel.setLayout(new GridBagLayout());
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);

        // Name label and text field
        constraints.gridx = 0;
        constraints.gridy = 0;
        loginPanel.add(new JLabel("Name:"), constraints);

        constraints.gridx = 1;
        constraints.gridy = 0;
        nameField = new JTextField(20);
        loginPanel.add(nameField, constraints);

        // Password label and password field
        constraints.gridx = 0;
        constraints.gridy = 1;
        loginPanel.add(new JLabel("Password:"), constraints);

        constraints.gridx = 1;
        constraints.gridy = 1;
        passwordField = new JPasswordField(20);
        loginPanel.add(passwordField, constraints);

        constraints.gridx = 4;
        constraints.gridy = 1;
        JCheckBox showPasswordCheckBox = new JCheckBox("Show"); // Unicode for eye icon
        showPasswordCheckBox.setFont(new Font("Arial", Font.PLAIN, 8)); // Adjust font size if needed
        showPasswordCheckBox.setForeground(Color.white);
        showPasswordCheckBox.setOpaque(false);
        showPasswordCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    passwordField.setEchoChar((char) 0); // Show password
                } else {
                    passwordField.setEchoChar('*'); // Hide password
                }
            }
        });
        loginPanel.add(showPasswordCheckBox, constraints);

        // User type dropdown
        userTypeDropdown = new JComboBox<>(new String[]{"Student", "Teacher"});
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        loginPanel.add(userTypeDropdown, constraints);

        // Login button
        final JButton loginButton = new JButton("Login");
        loginButton.addActionListener(event -> loginAction());
        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.gridwidth = 2;
        loginPanel.add(loginButton, constraints);

        // Set the background color of the panel with alpha for transparency
        loginPanel.setkEndColor(new Color(128, 0, 0)); // Dark maroon color
        loginPanel.setkStartColor(Color.WHITE); // White color
        loginPanel.setkGradientFocus(300); // Adjust alpha (last parameter) as needed

        loginDialog.add(loginPanel);
        loginDialog.setVisible(true);
    }

    private void showRegisterDialog() {
        registerDialog = new JDialog(frame, "Register", true);
        registerDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        registerDialog.setSize(600, 400);
        registerDialog.setLocationRelativeTo(frame);

        // Use KGradientPanel as the content pane for the dialog
        KGradientPanel buttonPanel = new KGradientPanel();
        buttonPanel.setLayout(new BorderLayout());
        buttonPanel.setkEndColor(new Color(128, 0, 0)); // Dark maroon color
        buttonPanel.setkStartColor(Color.WHITE); // White color
        buttonPanel.setkGradientFocus(300);                  // Linear gradient from top to bottom
        registerDialog.setContentPane(buttonPanel);

        final JPanel registerPanel = new JPanel(new GridBagLayout());
        registerPanel.setOpaque(false); // Make the panel transparent

        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.fill = GridBagConstraints.HORIZONTAL;

        // Name
        registerPanel.add(new JLabel("Name:"), constraints);
        constraints.gridx = 1;
        constraints.gridy = 0;
        nameField = new JTextField(20);
        registerPanel.add(nameField, constraints);

        // Password
        constraints.gridx = 0;
        constraints.gridy = 1;
        registerPanel.add(new JLabel("Password:"), constraints);
        constraints.gridx = 1;
        constraints.gridy = 1;
        passwordField = new JPasswordField(20);
        registerPanel.add(passwordField, constraints);

        constraints.gridx = 2;
        constraints.gridy = 1;
        JCheckBox showPasswordCheckBox = new JCheckBox("Show"); // Unicode for eye icon
        showPasswordCheckBox.setFont(new Font("Arial", Font.PLAIN, 8));
        showPasswordCheckBox.setForeground(Color.white);
        showPasswordCheckBox.setOpaque(false);
        showPasswordCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    passwordField.setEchoChar((char) 0); // Show password
                } else {
                    passwordField.setEchoChar('*'); // Hide password
                }
            }
        });
        registerPanel.add(showPasswordCheckBox, constraints);

        // User Type Dropdown
        constraints.gridx = 0;
        constraints.gridy = 2;
        registerPanel.add(new JLabel("User Type:"), constraints);
        constraints.gridx = 1;
        constraints.gridy = 2;
        userTypeDropdown = new JComboBox<>(new String[]{"Student", "Teacher"});
        registerPanel.add(userTypeDropdown, constraints);

        // Subjects Dropdown
        constraints.gridx = 0;
        constraints.gridy = 3;
        registerPanel.add(new JLabel("Subjects:"), constraints);
        constraints.gridx = 1;
        constraints.gridy = 3;
        subjectsDropdown = new JComboBox<>(
                new String[]{"IM211", "CC214", "MS121", "PE3", "GE105", "GE106", "NET212", "ITELECTV", "GENSOC"});
        subjectsDropdown.setEnabled(false); // Initially disabled
        userTypeDropdown.addActionListener(event -> {
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
        registerButton.addActionListener(event -> {
            registerAction();
        });
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 2;
        registerPanel.add(registerButton, constraints);

        buttonPanel.add(registerPanel, BorderLayout.CENTER);
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

                    final String[] columnNames = {"ID", "Name", selectedSubject};
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
                            resultSet.getInt(selectedSubject.toLowerCase()),};
                        tableModel.addRow(rowData);
                    }

                    // Update the table model on the event dispatch thread
                    SwingUtilities.invokeLater(() -> table.setModel(tableModel));

                } catch (final SQLException event) {
                    event.printStackTrace();
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
        final int[] columnWidths = {50, 100, 60};
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
            final String[] columnNames = {"ID", "Name", selectedSubject};
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
                    resultSet.getInt(selectedSubject.toLowerCase()),};
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
                public void insertUpdate(final DocumentEvent event) {
                    searchStudentsByName(searchField.getText(), table);
                }

                @Override
                public void removeUpdate(final DocumentEvent event) {
                    searchStudentsByName(searchField.getText(), table);
                }

                @Override
                public void changedUpdate(final DocumentEvent event) {
                    // Not Needed
                }
            });

            final JPanel buttonPanel = new JPanel();
            final JButton logoutButton = new JButton("Logout");
            logoutButton.addActionListener(event -> logoutAction());
            buttonPanel.add(logoutButton);

            final JButton editButton = new JButton("Edit Student");
            editButton.addActionListener(event -> {
                final int selectedRow = table.getSelectedRow();
                if (selectedRow != -1) {
                    editStudent((int) table.getValueAt(selectedRow, 0));
                }
            });
            buttonPanel.add(editButton);

            final JButton deleteAccountButton = new JButton("Delete Account");
            deleteAccountButton.addActionListener(event -> deleteTeacherAccount());
            buttonPanel.add(deleteAccountButton);

            frame.setLayout(new BorderLayout());
            frame.add(searchPanel, BorderLayout.NORTH);
            frame.add(scrollPane, BorderLayout.CENTER);
            frame.add(buttonPanel, BorderLayout.SOUTH);
            frame.pack();
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        } catch (final SQLException event) {
            event.printStackTrace();
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

            final String[] columnNames = {"ID", "Name", "IM211", "CC214", "MS121", "PE3", "GE105", "GE106", "NET212",
                "ITELECTV", "GENSOC", "Average Grade", "Status"};
            final DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(final int row, final int column) {
                    return false; // Make the table not editable
                }
            };

            // Get the informations
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
            final int[] columnWidths = {50, 100, 60, 60, 60, 60, 60, 60, 60, 80, 60, 100, 80};
            for (int i = 0; i < columnWidths.length; i++) {
                table.getColumnModel().getColumn(i).setPreferredWidth(columnWidths[i]);
            }

            // Wrap the table in a JScrollPane to enable horizontal scrolling
            final JScrollPane scrollPane = new JScrollPane(table);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            final JPanel buttonPanel = new JPanel();

            final JButton logoutButton = new JButton("Logout");
            logoutButton.addActionListener(event -> logoutAction());
            buttonPanel.add(logoutButton);
            final JButton deleteAccountButton = new JButton("Delete Account");
            deleteAccountButton.addActionListener(event -> deleteStudentAccount());
            buttonPanel.add(deleteAccountButton);
            frame.setLayout(new BorderLayout());
            frame.add(scrollPane, BorderLayout.CENTER);
            frame.add(buttonPanel, BorderLayout.SOUTH);
            frame.pack();
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

        } catch (final SQLException event) {
            event.printStackTrace();
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
                    final int im211 = parseGrade(im211Field);
                    final int cc214 = parseGrade(cc214Field);
                    final int ms121 = parseGrade(ms121Field);
                    final int pe3 = parseGrade(pe3Field);
                    final int ge105 = parseGrade(ge105Field);
                    final int ge106 = parseGrade(ge106Field);
                    final int net212 = parseGrade(net212Field);
                    final int itelectv = parseGrade(itelectvField);
                    final int gensoc = parseGrade(gensocField);
                    final int averageGrade = (im211 + cc214 + ms121 + pe3 + ge105 + ge106 + net212 + itelectv + gensoc)
                            / totalSubjects;
                    final String status = (averageGrade < 75) ? "Failed" : "Passed";

                    if (!isValidGradeRange(im211) || !isValidGradeRange(cc214)
                            || !isValidGradeRange(ms121)
                            || !isValidGradeRange(pe3) || !isValidGradeRange(ge105) || !isValidGradeRange(ge106)
                            || !isValidGradeRange(net212) || !isValidGradeRange(itelectv) || !isValidGradeRange(gensoc)) {
                        JOptionPane.showMessageDialog(frame, "Please enter a valid grade", "Error",
                                JOptionPane.ERROR_MESSAGE);
                        editStudent(studentId);
                        return;
                    }

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

        } catch (final SQLException event) {
            event.printStackTrace();
        }
    }

    private int parseGrade(JTextField textField) throws NumberFormatException {
        String text = textField.getText().trim();
        int grade;
        if (text.isEmpty()) {
            throw new NumberFormatException("Empty input");
        }

        try {
            grade = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return -1; // return an invalid grade to ask again
        }
        return grade;
    }

    private void confirmExit() {
        final int option = JOptionPane.showConfirmDialog(null, "Are you sure you want to exit?", "Exit",
                JOptionPane.YES_NO_OPTION);
        if (option == JOptionPane.YES_OPTION) {
            // If the user clicks "Yes," exit the application
            System.exit(0);
        }
    }

    private void logoutAction() {
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
    
    // Check if it's between 50 to 100
    private boolean isValidGradeRange(final double grade) {
        return grade >= 50 && grade <= 100;
    }
    private boolean isValidLength(final String password) {
        return password.length() > 6;
    }

    private boolean isInvalidName(final String name) {
        // Use regex to check if the name contains only alphabets
        return !((name.matches("^[a-zA-Z.]+$")) || (name.contains(" ")));
    }

    private String getTeacherSubject() throws SQLException {
        final String studentName = nameField.getText();
        final PreparedStatement teacherSubjectStatement = connection.prepareStatement(
                "SELECT subject FROM teachers WHERE name = ?");
        teacherSubjectStatement.setString(1, studentName);
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
                final String studentName = nameField.getText();
                final PreparedStatement deleteStatement = connection
                        .prepareStatement("DELETE FROM students WHERE name = ?");
                deleteStatement.setString(1, studentName);

                final int rowsAffected = deleteStatement.executeUpdate();

                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(frame, "Account deleted successfully", "Success",
                            JOptionPane.INFORMATION_MESSAGE);

                    // After deleting the account, log out
                    logoutAction();
                } else {
                    JOptionPane.showMessageDialog(frame, "Failed to delete account", "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (final SQLException event) {
                event.printStackTrace();
            }
        }
    }

    private String getSubjectCode(final int index) {
        final String[] subjectCodes = {"IM211", "CC214", "MS121", "PE3", "GE105", "GE106", "NET212", "ITELECTV",
            "GENSOC"};
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
                final String teacherName = nameField.getText();
                final PreparedStatement deleteStatement = connection
                        .prepareStatement("DELETE FROM teachers WHERE name = ?");
                deleteStatement.setString(1, teacherName);

                final int rowsAffected = deleteStatement.executeUpdate();

                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(frame, "Account deleted successfully", "Success",
                            JOptionPane.INFORMATION_MESSAGE);

                    // After deleting the account, log out
                    logoutAction();
                } else {
                    JOptionPane.showMessageDialog(frame, "Failed to delete account", "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (final SQLException event) {
                event.printStackTrace();
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
        } catch (final SQLException event) {
            event.printStackTrace();
        }
        return subjects;
    }
}
