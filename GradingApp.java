import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GradingApp extends JFrame {
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton searchButton;
    private JTextField searchField;
    private JTable table;
    private DefaultTableModel tableModel;

    private Connection connection;
    private String jdbcURL = "jdbc:mysql://localhost:3306/grade";
    private String username = "root";
    private String password = "alexis";

    public GradingApp() {
        try {
            connection = DriverManager.getConnection(jdbcURL, username, password);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        initComponents();
        setupUI();
        loadTableData();
        // Add a window listener to handle the window closing event
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmExit();
            }
        });
    }

    private void initComponents() {
        addButton = new JButton("Add");
        editButton = new JButton("Edit");
        deleteButton = new JButton("Delete");
        searchButton = new JButton("Search");
        searchField = new JTextField();

        searchField.setPreferredSize(new Dimension(200, 25));
        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Make all cells not editable
                return false;
            }
        };
        table = new JTable(tableModel);
    }

    private void setupUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Search components
        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        // Add search panel to the top
        add(searchPanel, BorderLayout.NORTH);

        JScrollPane tableScrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);

        add(tableScrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Set column reordering to false
        table.getTableHeader().setReorderingAllowed(false);
        // Allow multiple row selection
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // Add ActionListener to searchField to perform search when Enter is pressed
        searchField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String searchText = searchField.getText();
                searchStudents(searchText);
            }
        });

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAddStudentDialog("", 99, 99);
            }
        });

        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow != -1) {
                    showEditStudentDialog(selectedRow);
                } else {
                    JOptionPane.showMessageDialog(null, "Please select a student to edit.");
                }
            }
        });
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] selectedRows = table.getSelectedRows();
                if (selectedRows.length > 0) {
                    int option = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete?",
                            "Confirm Deletion", JOptionPane.YES_NO_OPTION);
                    if (option == JOptionPane.YES_OPTION) {
                        for (int i = selectedRows.length - 1; i >= 0; i--) {
                            String studentId = (String) tableModel.getValueAt(selectedRows[i], 0);
                            deleteStudent(studentId);
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Please select at least one row to delete.");
                }
            }
        });
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String searchText = searchField.getText();
                searchStudents(searchText);
            }
        });

        pack();
        setLocationRelativeTo(null);
    }

    private void showAddStudentDialog(String previousName, double previousIm211Grade, double previousCc214Grade) {
        JPanel panel = new JPanel(new GridLayout(3, 2));
        JTextField nameField = new JTextField(previousName);
        JTextField im211Field = new JTextField(Double.toString(previousIm211Grade));
        JTextField cc214Field = new JTextField(Double.toString(previousCc214Grade));

        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("IM211 Grade:"));
        panel.add(im211Field);
        panel.add(new JLabel("CC214 Grade:"));
        panel.add(cc214Field);

        int result = JOptionPane.showConfirmDialog(
                null, panel, "Add Student", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText();
            try {
                if (!isNameExists(name)) {
                    if (isValidName(name) && isValidGrade(im211Field.getText())
                            && isValidGrade(cc214Field.getText())) {
                        addStudent(name, Double.parseDouble(im211Field.getText()),
                                Double.parseDouble(cc214Field.getText()));
                    } else {
                        JOptionPane.showMessageDialog(null,
                                "Invalid input. Please check name, ensure grades are numeric and less than 101.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                        showAddStudentDialog(name, Double.parseDouble(im211Field.getText()),
                                Double.parseDouble(cc214Field.getText()));
                    }
                } else {
                    JOptionPane.showMessageDialog(null,
                            "Student with the same name already exists. Please choose a different name.", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    showAddStudentDialog(name, Double.parseDouble(im211Field.getText()),
                            Double.parseDouble(cc214Field.getText()));
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error checking for existing name. Please try again.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return; // Exit the loop in case of an exception
            }
        } else {
            // User pressed Cancel, exit the loop
            return;
        }
    }

    private void addStudent(String name, double im211Grade, double cc214Grade) {
        try {
            String sql = "INSERT INTO students (name, im211, cc214, average_grade, status) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);

            statement.setString(1, name);
            statement.setDouble(2, im211Grade);
            statement.setDouble(3, cc214Grade);

            // Calculate average grade
            double averageGrade = (im211Grade + cc214Grade) / 2;
            statement.setDouble(4, averageGrade);

            // Determine status (assuming passing grade is 60)
            String status = (averageGrade >= 60) ? "Passed" : "Failed";
            statement.setString(5, status);

            statement.executeUpdate();

            // Refresh the table after adding a new student
            loadTableData();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showEditStudentDialog(int selectedRow) {
        JPanel panel = new JPanel(new GridLayout(4, 2));
        JTextField nameField = new JTextField((String) tableModel.getValueAt(selectedRow, 1));
        JTextField im211Field = new JTextField(tableModel.getValueAt(selectedRow, 2).toString());
        JTextField cc214Field = new JTextField(tableModel.getValueAt(selectedRow, 3).toString());

        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("IM211 Grade:"));
        panel.add(im211Field);
        panel.add(new JLabel("CC214 Grade:"));
        panel.add(cc214Field);

        int result = JOptionPane.showConfirmDialog(
                null, panel, "Edit Student", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String newName = nameField.getText();
            if (!newName.equals((String) tableModel.getValueAt(selectedRow, 1))) {
                // Check if the new name is different from the current one
                try {
                    if (!isNameExists(newName)) {
                        if (isValidName(newName) && isValidGrade(im211Field.getText())
                                && isValidGrade(cc214Field.getText())) {
                            editStudent(
                                    (String) tableModel.getValueAt(selectedRow, 0),
                                    newName,
                                    Double.parseDouble(im211Field.getText()),
                                    Double.parseDouble(cc214Field.getText()));
                        } else {
                            JOptionPane.showMessageDialog(null,
                                    "Invalid input. Please check name and ensure grades are numeric.", "Error",
                                    JOptionPane.ERROR_MESSAGE);
                            showEditStudentDialog(selectedRow);
                        }
                    } else {
                        JOptionPane.showMessageDialog(null,
                                "Student with the same name already exists. Please choose a different name.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                        showEditStudentDialog(selectedRow);
                    }
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(null,
                            "Invalid input. Please enter numeric values for grades.", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    showEditStudentDialog(selectedRow);
                } catch (SQLException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Error checking for existing name. Please try again.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return; // Exit the loop in case of an exception
                }
            } else {
                if (isValidName(newName) && isValidGrade(im211Field.getText())
                        && isValidGrade(cc214Field.getText())) {
                    editStudent(
                            (String) tableModel.getValueAt(selectedRow, 0),
                            newName,
                            Double.parseDouble(im211Field.getText()),
                            Double.parseDouble(cc214Field.getText()));
                } else {
                    JOptionPane.showMessageDialog(null,
                            "Invalid input. Please check name and ensure grades are numeric.", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    showEditStudentDialog(selectedRow);
                }
            }
        } else {
            // User pressed Cancel, exit the loop
            return;
        }
    }

    private void editStudent(String studentId, String name, double im211Grade, double cc214Grade) {
        try {
            String sql = "UPDATE students SET name=?, im211=?, cc214=?, average_grade=?, status=? WHERE id=?";
            PreparedStatement statement = connection.prepareStatement(sql);

            statement.setString(1, name);
            statement.setDouble(2, im211Grade);
            statement.setDouble(3, cc214Grade);

            // Calculate average grade
            double averageGrade = (im211Grade + cc214Grade) / 2;
            statement.setDouble(4, averageGrade);

            // Determine status (assuming passing grade is 60)
            String status = (averageGrade >= 60) ? "Passed" : "Failed";
            statement.setString(5, status);

            statement.setString(6, studentId);

            statement.executeUpdate();

            // Refresh the table after editing a student
            loadTableData();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteStudent(String studentId) {
        try {
            String sql = "DELETE FROM students WHERE id=?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, studentId);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Refresh the table after deleting a student
        loadTableData();
    }

    private void loadTableData() {
        try {
            String sql = "SELECT id, name, im211, cc214, average_grade, status FROM students";
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();

            // Clear existing rows from the tableModel
            tableModel.setRowCount(0);

            tableModel.setColumnIdentifiers(
                    new Object[] { "ID", "Name", "IM211", "CC214", "Average Grade", "Status" });

            while (resultSet.next()) {
                String id = resultSet.getString("id");
                String name = resultSet.getString("name");
                double im211Grade = resultSet.getDouble("im211");
                double cc214Grade = resultSet.getDouble("cc214");
                double averageGrade = resultSet.getDouble("average_grade");
                String status = resultSet.getString("status");

                tableModel.addRow(new Object[] { id, name, im211Grade, cc214Grade, averageGrade, status });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void searchStudents(String searchText) {
        try {
            String sql = "SELECT id, name, average_grade, status FROM students WHERE name LIKE ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, "%" + searchText + "%"); // Use '%' to match any part of the name
            ResultSet resultSet = statement.executeQuery();

            // Clear existing rows from the tableModel
            tableModel.setRowCount(0);

            tableModel.setColumnIdentifiers(new Object[] { "ID", "Name", "Average Grade", "Status" });

            while (resultSet.next()) {
                String id = resultSet.getString("id");
                String name = resultSet.getString("name");
                double averageGrade = resultSet.getDouble("average_grade");
                String status = resultSet.getString("status");

                tableModel.addRow(new Object[] { id, name, averageGrade, status });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean isNameExists(String name) throws SQLException {
        String sql = "SELECT COUNT(*) FROM students WHERE name=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private boolean isValidName(String name) {
        // Use regex to check if the name contains only alphabets
        return ((name.matches("^[a-zA-Z]+$")) || (name.contains(" ")));
    }

    private boolean isValidGrade(String grade) {
        try {
            double numericGrade = Double.parseDouble(grade);
            // Check if the grade is greater than or equal to 0 and less than or equal to
            // 100
            return numericGrade >= 0 && numericGrade <= 100;
        } catch (NumberFormatException e) {
            // If parsing as double fails, it's not a valid grade
            return false;
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new GradingApp().setVisible(true);
        });
    }
}
