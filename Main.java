package com.college.insurance;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;

public class Main {
    private static final String DB_URL = "jdbc:sqlite:insurance.db";
    private JFrame frame;
    private JTextField searchField;
    private JTabbedPane tabs;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new Main().showLogin();
        });
    }

    private Connection connect() throws SQLException {
        Connection c = DriverManager.getConnection(DB_URL);
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return c;
    }

    private void initializeDatabase() throws SQLException {
        try (Connection c = connect(); Statement st = c.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS customers (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    phone TEXT,
                    email TEXT,
                    address TEXT,
                    created_at TEXT NOT NULL
                )
            """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS policies (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    customer_id INTEGER NOT NULL,
                    policy_type TEXT NOT NULL,
                    coverage REAL NOT NULL,
                    rate REAL NOT NULL,
                    premium REAL NOT NULL,
                    start_date TEXT NOT NULL,
                    FOREIGN KEY(customer_id) REFERENCES customers(id)
                )
            """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS claims (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    policy_id INTEGER NOT NULL,
                    claim_amount REAL NOT NULL,
                    description TEXT,
                    status TEXT NOT NULL DEFAULT 'Pending',
                    claim_date TEXT NOT NULL,
                    FOREIGN KEY(policy_id) REFERENCES policies(id)
                )
            """);
        }
    }

    private void showLogin() {
        JFrame login = new JFrame("Insurance Management System - Login");
        login.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        login.setSize(420, 300);
        login.setLocationRelativeTo(null);

        JPanel root = new JPanel(new GridBagLayout());
        root.setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));
        root.setBackground(new Color(244, 247, 252));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(7, 6, 7, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
        JLabel title = new JLabel("Insurance Management System", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 19));
        root.add(title, g);

        g.gridwidth = 1; g.gridy++;
        root.add(new JLabel("Username"), g);
        JTextField user = new JTextField("admin", 18);
        g.gridx = 1; root.add(user, g);

        g.gridx = 0; g.gridy++;
        root.add(new JLabel("Password"), g);
        JPasswordField pass = new JPasswordField("admin123", 18);
        g.gridx = 1; root.add(pass, g);

        g.gridx = 0; g.gridy++; g.gridwidth = 2;
        JButton loginButton = new JButton("Login");
        root.add(loginButton, g);
        JLabel hint = new JLabel("Demo login: admin / admin123", SwingConstants.CENTER);
        hint.setForeground(new Color(90, 100, 115));
        g.gridy++; root.add(hint, g);

        loginButton.addActionListener(e -> {
            if ("admin".equals(user.getText().trim())
                    && "admin123".equals(new String(pass.getPassword()))) {
                login.dispose();
                try {
                    initializeDatabase();
                    showMainWindow();
                } catch (SQLException ex) {
                    showError("Could not initialize database: " + ex.getMessage());
                }
            } else {
                JOptionPane.showMessageDialog(login, "Invalid username or password.",
                        "Login failed", JOptionPane.WARNING_MESSAGE);
            }
        });
        pass.addActionListener(e -> loginButton.doClick());
        login.setContentPane(root);
        login.setVisible(true);
    }

    private void showMainWindow() {
        frame = new JFrame("Insurance Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1050, 680);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createEmptyBorder(10, 12, 4, 12));
        JLabel title = new JLabel("Insurance Management System");
        title.setFont(new Font("SansSerif", Font.BOLD, 23));
        top.add(title, BorderLayout.WEST);
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        searchField = new JTextField(18);
        JButton search = new JButton("Search");
        JButton clear = new JButton("Show All");
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(search);
        searchPanel.add(clear);
        top.add(searchPanel, BorderLayout.EAST);
        frame.add(top, BorderLayout.NORTH);

        tabs = new JTabbedPane();
        tabs.addTab("Customers", customerPanel());
        tabs.addTab("Policies", policyPanel());
        tabs.addTab("Premium Calculator", premiumPanel());
        tabs.addTab("Claims", claimsPanel());
        tabs.addTab("Reports", reportPanel());
        frame.add(tabs, BorderLayout.CENTER);

        search.addActionListener(e -> refreshCurrentTable(searchField.getText().trim()));
        searchField.addActionListener(e -> refreshCurrentTable(searchField.getText().trim()));
        clear.addActionListener(e -> {
            searchField.setText("");
            refreshCurrentTable("");
        });

        frame.setVisible(true);
    }

    private JPanel customerPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JTextField name = new JTextField(15), phone = new JTextField(12),
                email = new JTextField(18), address = new JTextField(18);
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        form.add(new JLabel("Name*")); form.add(name);
        form.add(new JLabel("Phone")); form.add(phone);
        form.add(new JLabel("Email")); form.add(email);
        form.add(new JLabel("Address")); form.add(address);
        JButton add = new JButton("Add Customer");
        form.add(add);
        DefaultTableModel model = model("ID", "Name", "Phone", "Email", "Address", "Created");
        JTable table = new JTable(model);
        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        add.addActionListener(e -> {
            if (name.getText().trim().isEmpty()) {
                showError("Customer name is required."); return;
            }
            try (Connection c = connect(); PreparedStatement p = c.prepareStatement(
                    "INSERT INTO customers(name,phone,email,address,created_at) VALUES(?,?,?,?,?)")) {
                p.setString(1, name.getText().trim());
                p.setString(2, phone.getText().trim());
                p.setString(3, email.getText().trim());
                p.setString(4, address.getText().trim());
                p.setString(5, LocalDate.now().toString());
                p.executeUpdate();
                name.setText(""); phone.setText(""); email.setText(""); address.setText("");
                loadCustomers(model, "");
                JOptionPane.showMessageDialog(frame, "Customer added successfully.");
            } catch (SQLException ex) { showError(ex.getMessage()); }
        });
        loadCustomers(model, "");
        return panel;
    }

    private JPanel policyPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JTextField customerId = new JTextField(6), type = new JTextField(12),
                coverage = new JTextField(10), rate = new JTextField("2.5", 6);
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        form.add(new JLabel("Customer ID*")); form.add(customerId);
        form.add(new JLabel("Policy type*")); form.add(type);
        form.add(new JLabel("Coverage amount*")); form.add(coverage);
        form.add(new JLabel("Rate (%)*")); form.add(rate);
        JButton add = new JButton("Add Policy");
        form.add(add);
        DefaultTableModel model = model("Policy ID", "Customer ID", "Customer", "Type",
                "Coverage", "Rate %", "Premium", "Start date");
        JTable table = new JTable(model);
        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        add.addActionListener(e -> {
            try {
                int cid = Integer.parseInt(customerId.getText().trim());
                String policyType = type.getText().trim();
                double cov = Double.parseDouble(coverage.getText().trim());
                double r = Double.parseDouble(rate.getText().trim());
                if (policyType.isEmpty() || cov <= 0 || r < 0) throw new IllegalArgumentException();
                double premium = cov * r / 100.0;
                try (Connection c = connect()) {
                    try (PreparedStatement check = c.prepareStatement("SELECT id FROM customers WHERE id=?")) {
                        check.setInt(1, cid);
                        try (ResultSet rs = check.executeQuery()) {
                            if (!rs.next()) { showError("Customer ID not found. Add a customer first."); return; }
                        }
                    }
                    try (PreparedStatement p = c.prepareStatement(
                            "INSERT INTO policies(customer_id,policy_type,coverage,rate,premium,start_date) VALUES(?,?,?,?,?,?)")) {
                        p.setInt(1, cid); p.setString(2, policyType); p.setDouble(3, cov);
                        p.setDouble(4, r); p.setDouble(5, premium); p.setString(6, LocalDate.now().toString());
                        p.executeUpdate();
                    }
                }
                customerId.setText(""); type.setText(""); coverage.setText("");
                loadPolicies(model, "");
                JOptionPane.showMessageDialog(frame, String.format("Policy added. Estimated premium: %.2f", premium));
            } catch (NumberFormatException ex) { showError("Enter valid numeric values for Customer ID, coverage, and rate."); }
              catch (IllegalArgumentException ex) { showError("Policy type is required; coverage must be positive and rate cannot be negative."); }
              catch (SQLException ex) { showError(ex.getMessage()); }
        });
        loadPolicies(model, "");
        return panel;
    }

    private JPanel premiumPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10, 8, 10, 8); g.fill = GridBagConstraints.HORIZONTAL;
        JTextField coverage = new JTextField("100000", 18);
        JTextField rate = new JTextField("2.5", 18);
        JLabel result = new JLabel("Estimated premium: —");
        result.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.gridx=0; g.gridy=0; panel.add(new JLabel("Coverage amount (₹)"), g);
        g.gridx=1; panel.add(coverage, g);
        g.gridx=0; g.gridy++; panel.add(new JLabel("Rate (%)"), g);
        g.gridx=1; panel.add(rate, g);
        JButton calculate = new JButton("Calculate Premium");
        g.gridx=0; g.gridy++; g.gridwidth=2; panel.add(calculate, g);
        g.gridy++; panel.add(result, g);
        JLabel note = new JLabel("Formula: premium = coverage amount × rate / 100");
        note.setForeground(Color.GRAY);
        g.gridy++; panel.add(note, g);
        calculate.addActionListener(e -> {
            try {
                double cov = Double.parseDouble(coverage.getText().trim());
                double r = Double.parseDouble(rate.getText().trim());
                if (cov < 0 || r < 0) throw new NumberFormatException();
                result.setText(String.format("Estimated premium: ₹%,.2f", cov * r / 100.0));
            } catch (NumberFormatException ex) { showError("Enter valid non-negative numbers."); }
        });
        return panel;
    }

    private JPanel claimsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JTextField policyId = new JTextField(7), amount = new JTextField(10), description = new JTextField(24);
        JComboBox<String> status = new JComboBox<>(new String[]{"Pending", "Approved", "Rejected"});
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        form.add(new JLabel("Policy ID*")); form.add(policyId);
        form.add(new JLabel("Claim amount*")); form.add(amount);
        form.add(new JLabel("Description")); form.add(description);
        form.add(new JLabel("Status")); form.add(status);
        JButton add = new JButton("Register Claim"); form.add(add);
        DefaultTableModel model = model("Claim ID", "Policy ID", "Customer", "Policy type",
                "Amount", "Description", "Status", "Date");
        JTable table = new JTable(model);
        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        add.addActionListener(e -> {
            try {
                int pid = Integer.parseInt(policyId.getText().trim());
                double amt = Double.parseDouble(amount.getText().trim());
                if (amt <= 0) throw new IllegalArgumentException();
                try (Connection c = connect()) {
                    try (PreparedStatement check = c.prepareStatement("SELECT id FROM policies WHERE id=?")) {
                        check.setInt(1, pid);
                        try (ResultSet rs = check.executeQuery()) {
                            if (!rs.next()) { showError("Policy ID not found. Add a policy first."); return; }
                        }
                    }
                    try (PreparedStatement p = c.prepareStatement(
                            "INSERT INTO claims(policy_id,claim_amount,description,status,claim_date) VALUES(?,?,?,?,?)")) {
                        p.setInt(1, pid); p.setDouble(2, amt); p.setString(3, description.getText().trim());
                        p.setString(4, (String) status.getSelectedItem()); p.setString(5, LocalDate.now().toString());
                        p.executeUpdate();
                    }
                }
                policyId.setText(""); amount.setText(""); description.setText("");
                loadClaims(model, "");
                JOptionPane.showMessageDialog(frame, "Claim registered successfully.");
            } catch (NumberFormatException ex) { showError("Enter valid Policy ID and claim amount."); }
              catch (IllegalArgumentException ex) { showError("Claim amount must be greater than zero."); }
              catch (SQLException ex) { showError(ex.getMessage()); }
        });
        loadClaims(model, "");
        return panel;
    }

    private JPanel reportPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(12, 12, 12, 12);
        JButton refresh = new JButton("Refresh Report");
        JLabel customers = new JLabel(), policies = new JLabel(), claims = new JLabel(), pending = new JLabel();
        Font font = new Font("SansSerif", Font.BOLD, 20);
        customers.setFont(font); policies.setFont(font); claims.setFont(font); pending.setFont(font);
        g.gridx=0; g.gridy=0; panel.add(customers, g);
        g.gridy++; panel.add(policies, g);
        g.gridy++; panel.add(claims, g);
        g.gridy++; panel.add(pending, g);
        g.gridy++; panel.add(refresh, g);
        Runnable update = () -> {
            try (Connection c = connect(); Statement s = c.createStatement()) {
                customers.setText("Total customers: " + count(s, "customers"));
                policies.setText("Total policies: " + count(s, "policies"));
                claims.setText("Total claims: " + count(s, "claims"));
                try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM claims WHERE status='Pending'")) {
                    rs.next(); pending.setText("Pending claims: " + rs.getInt(1));
                }
            } catch (SQLException ex) { showError(ex.getMessage()); }
        };
        refresh.addActionListener(e -> update.run());
        update.run();
        return panel;
    }

    private int count(Statement s, String table) throws SQLException {
        try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM " + table)) { rs.next(); return rs.getInt(1); }
    }

    private DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    private void loadCustomers(DefaultTableModel m, String q) {
        m.setRowCount(0);
        String sql = "SELECT id,name,phone,email,address,created_at FROM customers " +
                "WHERE name LIKE ? OR phone LIKE ? OR email LIKE ? ORDER BY id DESC";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            String like = "%" + q + "%";
            p.setString(1, like); p.setString(2, like); p.setString(3, like);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) m.addRow(new Object[]{rs.getInt("id"), rs.getString("name"),
                        rs.getString("phone"), rs.getString("email"), rs.getString("address"), rs.getString("created_at")});
            }
        } catch (SQLException ex) { showError(ex.getMessage()); }
    }

    private void loadPolicies(DefaultTableModel m, String q) {
        m.setRowCount(0);
        String sql = """
            SELECT p.id,p.customer_id,c.name,p.policy_type,p.coverage,p.rate,p.premium,p.start_date
            FROM policies p JOIN customers c ON c.id=p.customer_id
            WHERE CAST(p.id AS TEXT) LIKE ? OR c.name LIKE ? OR p.policy_type LIKE ?
            ORDER BY p.id DESC
            """;
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            String like = "%" + q + "%";
            p.setString(1, like); p.setString(2, like); p.setString(3, like);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) m.addRow(new Object[]{rs.getInt(1), rs.getInt(2), rs.getString(3),
                        rs.getString(4), rs.getDouble(5), rs.getDouble(6), rs.getDouble(7), rs.getString(8)});
            }
        } catch (SQLException ex) { showError(ex.getMessage()); }
    }

    private void loadClaims(DefaultTableModel m, String q) {
        m.setRowCount(0);
        String sql = """
            SELECT cl.id,cl.policy_id,c.name,p.policy_type,cl.claim_amount,cl.description,cl.status,cl.claim_date
            FROM claims cl JOIN policies p ON p.id=cl.policy_id JOIN customers c ON c.id=p.customer_id
            WHERE CAST(cl.id AS TEXT) LIKE ? OR CAST(cl.policy_id AS TEXT) LIKE ? OR c.name LIKE ? OR cl.status LIKE ?
            ORDER BY cl.id DESC
            """;
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            String like = "%" + q + "%";
            for (int i=1;i<=4;i++) p.setString(i, like);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) m.addRow(new Object[]{rs.getInt(1), rs.getInt(2), rs.getString(3),
                        rs.getString(4), rs.getDouble(5), rs.getString(6), rs.getString(7), rs.getString(8)});
            }
        } catch (SQLException ex) { showError(ex.getMessage()); }
    }

    private void refreshCurrentTable(String q) {
        if (tabs == null) return;
        int i = tabs.getSelectedIndex();
        if (i == 0) {
            JTable t = findTable(tabs.getComponentAt(0));
            if (t != null) loadCustomers((DefaultTableModel)t.getModel(), q);
        } else if (i == 1) {
            JTable t = findTable(tabs.getComponentAt(1));
            if (t != null) loadPolicies((DefaultTableModel)t.getModel(), q);
        } else if (i == 3) {
            JTable t = findTable(tabs.getComponentAt(3));
            if (t != null) loadClaims((DefaultTableModel)t.getModel(), q);
        } else {
            JOptionPane.showMessageDialog(frame, "Search is available on Customers, Policies, and Claims tabs.");
        }
    }

    private JTable findTable(Component component) {
        if (component instanceof JTable) return (JTable) component;
        if (component instanceof JScrollPane sp && sp.getViewport().getView() instanceof JTable t) return t;
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JTable t = findTable(child);
                if (t != null) return t;
            }
        }
        return null;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
