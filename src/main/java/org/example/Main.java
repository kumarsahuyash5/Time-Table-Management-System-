package org.example;

import javax.mail.Message;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * College Time Table Management System (Java 17+)
 * Passwordless Email OTP Authentication with Admin Pre-Approval & Resend OTP Fix
 * MADE BY YASH KUMAR SAHU
 */
public class Main extends JFrame {

    // ============================== SENDER CONFIGURATION ==============================
    private static final String PARENT_EMAIL = "kumarsahuyash5@gmail.com";
    // ⚠️ Apka Google 16-character App Password yahan paste karein (e.g., "abcd efgh ijkl mnop")
    private static final String PARENT_EMAIL_PASSWORD = "phkx ufmw pxnv mext";

    // ============================== THEME ==============================
    static final Color NAVY       = new Color(25, 42, 86);
    static final Color NAVY_DARK  = new Color(14, 26, 58);
    static final Color NAVY_HOVER = new Color(45, 66, 120);
    static final Color ACTIVE     = new Color(66, 99, 190);
    static final Color BG         = new Color(240, 244, 252);
    static final Color BORDER     = new Color(214, 222, 236);
    static final Color MUTED_TEXT = new Color(100, 110, 130);
    static final Color MUTED_BTN  = new Color(125, 135, 155);
    static final Color DANGER     = new Color(200, 55, 55);
    static final Color SUCCESS    = new Color(35, 140, 90);
    static final Color ORANGE     = new Color(230, 145, 40);
    static final Color PURPLE     = new Color(130, 80, 190);

    static final String FONT = "SansSerif";
    static final Font F_TITLE  = new Font(FONT, Font.BOLD, 26);
    static final Font F_BOLD   = new Font(FONT, Font.BOLD, 14);
    static final Font F_NORMAL = new Font(FONT, Font.PLAIN, 14);
    static final Font F_SMALL  = new Font(FONT, Font.PLAIN, 12);

    static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
    static final String[] SLOTS = {"09:00 - 10:00", "10:00 - 11:00", "11:00 - 12:00", "12:00 - 01:00",
            "02:00 - 03:00", "03:00 - 04:00", "04:00 - 05:00"};
    static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    // ============================== ROLES / MODELS ==============================
    enum Role {
        ADMIN("Admin"), TEACHER("Teacher"), STUDENT("Student");
        final String label;
        Role(String label) { this.label = label; }
    }

    static class Teacher implements Serializable {
        final String username, name, department;
        Teacher(String username, String name, String department) {
            this.username = username.trim().toLowerCase();
            this.name = name;
            this.department = department;
        }
        @Override public String toString() { return name + " (" + department + ")"; }
    }

    static class Student implements Serializable {
        final String rollNo, name, className;
        Student(String rollNo, String name, String className) {
            this.rollNo = rollNo.trim().toLowerCase();
            this.name = name;
            this.className = className;
        }
    }

    record Subject(String code, String name) implements Serializable {
        @Override public String toString() { return code + " - " + name; }
    }

    record Lecture(String day, String slot, String className,
                   String subjectCode, String teacherId, String room) implements Serializable { }

    record Notice(String title, String message, String authorId, String authorName,
                  Role role, LocalDateTime time) implements Serializable { }

    record Session(Role role, String id, String name, String className, String department) { }

    // ============================== DATA STORE ==============================
    static class DataStore implements Serializable {
        private static final long serialVersionUID = 3L;
        private static final File FILE = new File("timetable_v2.dat");

        final List<Teacher> teachers = new ArrayList<>();
        final List<Student> students = new ArrayList<>();
        final List<Subject> subjects = new ArrayList<>();
        final List<Lecture> lectures = new ArrayList<>();
        final List<Notice> notices = new ArrayList<>();

        static DataStore load() {
            if (FILE.exists()) {
                try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(FILE))) {
                    return (DataStore) in.readObject();
                } catch (Exception ex) {
                    System.err.println("Could not load saved data: " + ex.getMessage());
                }
            }
            DataStore s = new DataStore();
            s.seedDemo();
            s.save();
            return s;
        }

        void save() {
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILE))) {
                out.writeObject(this);
            } catch (IOException ex) {
                System.err.println("Could not save data: " + ex.getMessage());
            }
        }

        private void seedDemo() {
            subjects.add(new Subject("CS101", "Data Structures"));
            subjects.add(new Subject("CS102", "Database Systems"));
            subjects.add(new Subject("CS103", "Operating Systems"));
            subjects.add(new Subject("MA101", "Discrete Mathematics"));

            teachers.add(new Teacher("teacher1", "Prof. Anita Sharma", "Computer Science"));
            teachers.add(new Teacher("teacher2", "Prof. Rahul Verma", "Mathematics"));
            teachers.add(new Teacher("yash", "Yash Kumar Sahu", "CSE"));

            students.add(new Student("student1", "Aarav Singh", "CSE-C"));
            students.add(new Student("student2", "Diya Patel", "CSE-C"));
            students.add(new Student("2025pietcsyash185", "Yash Sahu", "CSE-C"));

            lectures.add(new Lecture("Monday", SLOTS[0], "CSE-C", "CS101", "teacher1", "R101"));
            lectures.add(new Lecture("Monday", SLOTS[1], "CSE-C", "MA101", "teacher2", "R101"));
            lectures.add(new Lecture("Tuesday", SLOTS[0], "CSE-C", "CS102", "teacher1", "R102"));
            lectures.add(new Lecture("Wednesday", SLOTS[2], "CSE-C", "CS103", "teacher1", "R103"));
            lectures.add(new Lecture("Thursday", SLOTS[1], "CSE-C", "MA101", "teacher2", "R101"));
            lectures.add(new Lecture("Friday", SLOTS[0], "CSE-C", "CS101", "teacher1", "R101"));

            notices.add(new Notice("Welcome to the new timetable system",
                    "Check your timetable regularly. System created by Yash Kumar Sahu.",
                    "admin", "Administrator", Role.ADMIN, LocalDateTime.now()));
        }

        Teacher teacher(String id) {
            String clean = id.trim().toLowerCase();
            return teachers.stream().filter(t -> t.username.equalsIgnoreCase(clean)).findFirst().orElse(null);
        }
        String teacherName(String id) { Teacher t = teacher(id); return t == null ? "(removed)" : t.name; }
        String subjectName(String code) {
            return subjects.stream().filter(s -> s.code().equalsIgnoreCase(code)).map(Subject::name)
                    .findFirst().orElse("(removed)");
        }

        String findConflict(Lecture n) {
            for (Lecture l : lectures) {
                if (!l.day().equals(n.day()) || !l.slot().equals(n.slot())) continue;
                if (l.teacherId().equalsIgnoreCase(n.teacherId()))
                    return teacherName(n.teacherId()) + " already has a lecture at this time.";
                if (l.room().equalsIgnoreCase(n.room()))
                    return "Room " + n.room() + " is already booked at this time.";
                if (l.className().equalsIgnoreCase(n.className()))
                    return "Class " + n.className() + " already has a lecture at this time.";
            }
            return null;
        }
    }

    // ============================== UTILITIES ==============================
    static int dayIndex(String d) {
        for (int i = 0; i < DAYS.length; i++) if (DAYS[i].equals(d)) return i;
        return DAYS.length;
    }

    static int slotIndex(String s) {
        for (int i = 0; i < SLOTS.length; i++) if (SLOTS[i].equals(s)) return i;
        return SLOTS.length;
    }

    static int compareLectures(Lecture a, Lecture b) {
        int c = Integer.compare(dayIndex(a.day()), dayIndex(b.day()));
        return c != 0 ? c : Integer.compare(slotIndex(a.slot()), slotIndex(b.slot()));
    }

    static int todayIndex() {
        DayOfWeek d = LocalDate.now().getDayOfWeek();
        return d.getValue() - 1;
    }

    static Predicate<Lecture> scope(Session s) {
        return switch (s.role()) {
            case ADMIN -> l -> true;
            case TEACHER -> l -> l.teacherId().equalsIgnoreCase(s.id());
            case STUDENT -> l -> l.className().equalsIgnoreCase(s.className());
        };
    }

    static Color roleColor(Role r) {
        return switch (r) { case ADMIN -> DANGER; case TEACHER -> SUCCESS; case STUDENT -> ACTIVE; };
    }

    static void aa(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    // ============================== CUSTOM COMPONENTS ==============================
    static class RoundButton extends JButton {
        private float hoverAlpha = 0f;
        private final Timer hoverTimer = new Timer(15, null);

        RoundButton(String text, Color bg) {
            super(text);
            setBackground(bg);
            setForeground(Color.WHITE);
            setFont(F_BOLD);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

            hoverTimer.addActionListener(e -> {
                boolean ro = getModel().isRollover();
                if (ro && hoverAlpha < 1f) {
                    hoverAlpha = Math.min(1f, hoverAlpha + 0.1f);
                    repaint();
                } else if (!ro && hoverAlpha > 0f) {
                    hoverAlpha = Math.max(0f, hoverAlpha - 0.1f);
                    repaint();
                } else {
                    hoverTimer.stop();
                }
            });

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hoverTimer.start(); }
                @Override public void mouseExited(MouseEvent e) { hoverTimer.start(); }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            Color c = getBackground();
            if (!isEnabled()) {
                c = new Color(190, 195, 205);
            } else if (getModel().isPressed()) {
                c = c.darker();
            } else {
                c = LoginFrame.mix(c, c.brighter(), hoverAlpha);
            }
            g2.setColor(c);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class NavButton extends JButton {
        private boolean active;
        private float hover = 0f;
        private final Timer anim = new Timer(15, null);

        NavButton(String text) {
            super(text);
            setForeground(Color.WHITE);
            setFont(new Font(FONT, Font.PLAIN, 15));
            setHorizontalAlignment(SwingConstants.LEFT);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setBorder(BorderFactory.createEmptyBorder(0, 28, 0, 10));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            Dimension d = new Dimension(210, 44);
            setPreferredSize(d);
            setMaximumSize(d);
            setAlignmentX(Component.CENTER_ALIGNMENT);

            anim.addActionListener(e -> {
                float target = getModel().isRollover() ? 1f : 0f;
                hover += (target - hover) * 0.2f;
                if (Math.abs(target - hover) < 0.01f) { hover = target; anim.stop(); }
                repaint();
            });

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { anim.start(); }
                @Override public void mouseExited(MouseEvent e) { anim.start(); }
            });
        }

        void setActive(boolean a) { active = a; repaint(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            if (active) {
                g2.setColor(ACTIVE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(8, 12, 4, getHeight() - 24, 4, 4);
            } else if (hover > 0.01f) {
                g2.setColor(new Color(45, 66, 120, (int) (hover * 255)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class RoundPanel extends JPanel {
        RoundPanel(LayoutManager lm) {
            super(lm);
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g2.setColor(BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class StatCard extends JPanel {
        final JLabel value = new JLabel("0");
        private final Color accent;
        StatCard(String title, Color accent) {
            this.accent = accent;
            setOpaque(false);
            setLayout(new GridLayout(2, 1, 0, 2));
            setBorder(BorderFactory.createEmptyBorder(18, 30, 18, 15));
            value.setFont(new Font(FONT, Font.BOLD, 34));
            value.setForeground(NAVY);
            add(value);
            add(label(title, new Font(FONT, Font.BOLD, 15), MUTED_TEXT));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g2.setColor(BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g2.setColor(accent);
            g2.fillRoundRect(0, 14, 8, getHeight() - 28, 8, 8);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class Badge extends JLabel {
        private final Color bg;
        Badge(String text, Color bg) {
            super(text, SwingConstants.CENTER);
            this.bg = bg;
            setForeground(Color.WHITE);
            setFont(new Font(FONT, Font.BOLD, 12));
            setBorder(BorderFactory.createEmptyBorder(5, 14, 5, 14));
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class Avatar extends JComponent {
        private final String text;
        private final Color bg, fg;
        Avatar(String text, Color bg, Color fg, int size) {
            this.text = text; this.bg = bg; this.fg = fg;
            Dimension d = new Dimension(size, size);
            setPreferredSize(d); setMinimumSize(d); setMaximumSize(d);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            g2.setColor(bg);
            g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
            g2.setColor(fg);
            g2.setFont(new Font(FONT, Font.BOLD, (int) (getWidth() * 0.40)));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2,
                    (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    static class ScrollPanel extends JPanel implements Scrollable {
        ScrollPanel(LayoutManager lm) { super(lm); }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 80; }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    static class GridRenderer implements TableCellRenderer {
        private final JTextArea area = new JTextArea();
        GridRenderer() {
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        }
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                                 boolean focus, int row, int col) {
            String text = v == null ? "" : v.toString();
            area.setText(text);
            area.setForeground(NAVY);
            if (col == 0) {
                area.setBackground(new Color(225, 232, 250));
                area.setFont(F_BOLD);
            } else {
                area.setBackground(text.isEmpty() ? Color.WHITE : new Color(222, 236, 255));
                area.setFont(F_SMALL);
            }
            return area;
        }
    }

    static class ModernScrollBarUI extends BasicScrollBarUI {
        @Override protected void configureScrollBarColors() {
            this.thumbColor = new Color(180, 195, 225);
            this.trackColor = new Color(240, 244, 252);
        }
        @Override protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
        @Override protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
        private JButton createZeroButton() {
            JButton btn = new JButton();
            btn.setPreferredSize(new Dimension(0, 0));
            return btn;
        }
        @Override protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            g2.setColor(thumbColor);
            g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2, thumbBounds.width - 4, thumbBounds.height - 4, 8, 8);
            g2.dispose();
        }
    }

    // ============================== UI HELPERS ==============================
    static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    static void styleField(JTextField f) {
        f.setFont(F_NORMAL);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(190, 200, 220)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
    }

    static JTextField field() {
        JTextField f = new JTextField();
        styleField(f);
        return f;
    }

    static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    static JTable table(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setRowHeight(38);
        t.setFont(F_NORMAL);
        t.setShowVerticalLines(false);
        t.setGridColor(new Color(230, 235, 245));
        t.setSelectionBackground(new Color(200, 215, 250));
        t.setSelectionForeground(Color.BLACK);
        t.setFillsViewportHeight(true);

        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel,
                                                                     boolean foc, int r, int c) {
                super.getTableCellRendererComponent(tb, v, sel, false, r, c);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                if (!sel) setBackground(r % 2 == 0 ? Color.WHITE : new Color(247, 250, 255));
                return this;
            }
        });

        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(100, 40));
        h.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel,
                                                                     boolean foc, int r, int c) {
                super.getTableCellRendererComponent(tb, v, false, false, r, c);
                setBackground(NAVY);
                setForeground(Color.WHITE);
                setFont(F_BOLD);
                setHorizontalAlignment(SwingConstants.LEFT);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return this;
            }
        });
        return t;
    }

    static JScrollPane scroll(JComponent c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createLineBorder(BORDER));
        sp.getViewport().setBackground(Color.WHITE);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        return sp;
    }

    static RoundPanel formCard() {
        RoundPanel p = new RoundPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        return p;
    }

    static void addField(JPanel form, int col, int row, String text, JComponent field) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = row;
        g.insets = new Insets(8, col == 0 ? 5 : 20, 8, 10);
        g.anchor = GridBagConstraints.WEST;
        g.gridx = col * 2;
        form.add(label(text, F_BOLD, NAVY), g);
        g.gridx = col * 2 + 1;
        g.weightx = 1;
        g.insets = new Insets(8, 0, 8, 10);
        g.fill = GridBagConstraints.HORIZONTAL;
        if (field instanceof JTextField || field instanceof JComboBox || field instanceof JLabel) {
            field.setPreferredSize(new Dimension(220, 38));
            field.setFont(F_NORMAL);
        }
        form.add(field, g);
    }

    static void addButtonRow(JPanel form, int row, int columns, JComponent button) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = row;
        g.gridwidth = columns * 2;
        g.anchor = GridBagConstraints.EAST;
        g.insets = new Insets(14, 5, 4, 5);
        form.add(button, g);
    }

    static void info(Component p, String msg) {
        JOptionPane.showMessageDialog(p, msg, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    static void error(Component p, String msg) {
        JOptionPane.showMessageDialog(p, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    static boolean confirm(Component p, String msg) {
        return JOptionPane.showConfirmDialog(p, msg, "Confirm", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
    }

    // ============================== PAGE BASE ==============================
    abstract static class Page extends JPanel {
        protected final DataStore store;
        protected final JButton refreshBtn = new RoundButton("🔄 Refresh Data", ACTIVE);

        Page(DataStore store, String title, String subtitle) {
            this.store = store;
            setLayout(new BorderLayout(15, 15));
            setBackground(BG);
            setBorder(BorderFactory.createEmptyBorder(24, 30, 24, 30));

            JPanel top = new JPanel(new BorderLayout());
            top.setOpaque(false);

            JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 4));
            textPanel.setOpaque(false);
            textPanel.add(label(title, F_TITLE, NAVY));
            textPanel.add(label(subtitle, new Font(FONT, Font.PLAIN, 15), MUTED_TEXT));

            refreshBtn.addActionListener(e -> {
                refresh();
                info(this, "Data refreshed successfully.");
            });

            top.add(textPanel, BorderLayout.WEST);
            top.add(refreshBtn, BorderLayout.EAST);
            add(top, BorderLayout.NORTH);
        }

        abstract void refresh();
    }

    // ============================== DASHBOARD ==============================
    static class DashboardPage extends Page {
        private final Session session;
        private final StatCard[] cards = new StatCard[4];
        private final DefaultTableModel model = model("Time", "Class", "Subject", "Teacher", "Room");
        private final JLabel scheduleTitle = label("", new Font(FONT, Font.BOLD, 18), NAVY);

        DashboardPage(DataStore s, Session session) {
            super(s, "Welcome, " + session.name() + "!", switch (session.role()) {
                case ADMIN -> "You have full access to manage the college timetable.";
                case TEACHER -> "Here is an overview of your teaching schedule.";
                case STUDENT -> "Here is an overview of your class timetable.";
            });
            this.session = session;
            String[] titles = switch (session.role()) {
                case ADMIN -> new String[]{"Teachers", "Students", "Subjects", "Total Lectures"};
                case TEACHER -> new String[]{"My Lectures", "Today's Lectures", "Classes Taught", "Notices"};
                case STUDENT -> new String[]{"My Class", "Today's Lectures", "Weekly Lectures", "Notices"};
            };
            Color[] accents = {ACTIVE, SUCCESS, ORANGE, PURPLE};

            JPanel row = new JPanel(new GridLayout(1, 4, 18, 0));
            row.setOpaque(false);
            for (int i = 0; i < 4; i++) {
                cards[i] = new StatCard(titles[i], accents[i]);
                row.add(cards[i]);
            }

            JTable table = table(model);
            JPanel schedule = new JPanel(new BorderLayout(0, 10));
            schedule.setOpaque(false);
            schedule.add(scheduleTitle, BorderLayout.NORTH);
            schedule.add(scroll(table), BorderLayout.CENTER);

            JPanel center = new JPanel(new BorderLayout(0, 22));
            center.setOpaque(false);
            center.add(row, BorderLayout.NORTH);
            center.add(schedule, BorderLayout.CENTER);
            add(center, BorderLayout.CENTER);
            refresh();
        }

        @Override void refresh() {
            List<Lecture> mine = store.lectures.stream().filter(scope(session)).collect(Collectors.toList());
            int today = todayIndex();
            String todayName = today < DAYS.length ? DAYS[today] : "Sunday";
            List<Lecture> todays = mine.stream().filter(l -> l.day().equals(todayName))
                    .sorted(Main::compareLectures).collect(Collectors.toList());

            String[] v = switch (session.role()) {
                case ADMIN -> new String[]{"" + store.teachers.size(), "" + store.students.size(),
                        "" + store.subjects.size(), "" + store.lectures.size()};
                case TEACHER -> new String[]{"" + mine.size(), "" + todays.size(),
                        "" + mine.stream().map(l -> l.className().toLowerCase()).distinct().count(),
                        "" + store.notices.size()};
                case STUDENT -> new String[]{session.className(), "" + todays.size(),
                        "" + mine.size(), "" + store.notices.size()};
            };
            for (int i = 0; i < 4; i++) cards[i].value.setText(v[i]);

            scheduleTitle.setText("Today's Schedule - " + todayName);
            model.setRowCount(0);
            if (todays.isEmpty()) {
                model.addRow(new Object[]{"-", "No lectures scheduled today", "", "", ""});
            } else {
                for (Lecture l : todays)
                    model.addRow(new Object[]{l.slot(), l.className(), store.subjectName(l.subjectCode()),
                            store.teacherName(l.teacherId()), l.room()});
            }
        }
    }

    // ============================== ADD TIMETABLE ==============================
    static class AddTimetablePage extends Page {
        private final JComboBox<String> day = new JComboBox<>(DAYS), slot = new JComboBox<>(SLOTS);
        private final JComboBox<String> classBox = new JComboBox<>();
        private final JComboBox<Subject> subject = new JComboBox<>();
        private final JComboBox<Teacher> teacher = new JComboBox<>();
        private final JTextField room = field();

        AddTimetablePage(DataStore s) {
            super(s, "Add Timetable", "Schedule a lecture. Teacher, room and class clashes are detected automatically.");
            classBox.setEditable(true);

            RoundPanel form = formCard();
            addField(form, 0, 0, "Day", day);
            addField(form, 1, 0, "Time Slot", slot);
            addField(form, 0, 1, "Class / Section", classBox);
            addField(form, 1, 1, "Classroom", room);
            addField(form, 0, 2, "Subject", subject);
            addField(form, 1, 2, "Teacher", teacher);

            JButton addBtn = new RoundButton("Add Lecture", SUCCESS);
            addBtn.addActionListener(e -> addLecture());
            addButtonRow(form, 3, 2, addBtn);

            JPanel wrap = new JPanel(new BorderLayout());
            wrap.setOpaque(false);
            wrap.add(form, BorderLayout.NORTH);
            add(wrap, BorderLayout.CENTER);
        }

        private void addLecture() {
            Object c = classBox.getEditor().getItem();
            String cls = c == null ? "" : c.toString().trim();
            String rm = room.getText().trim();
            Subject sub = (Subject) subject.getSelectedItem();
            Teacher tea = (Teacher) teacher.getSelectedItem();

            if (sub == null || tea == null) { error(this, "Add at least one subject and one teacher first."); return; }
            if (cls.isEmpty() || rm.isEmpty()) { error(this, "Please enter the class and classroom."); return; }

            Lecture l = new Lecture((String) day.getSelectedItem(), (String) slot.getSelectedItem(),
                    cls, sub.code(), tea.username, rm);
            String clash = store.findConflict(l);
            if (clash != null) { error(this, "Clash detected!\n" + clash); return; }

            store.lectures.add(l);
            saveLectureToDB((String) day.getSelectedItem(), (String) slot.getSelectedItem(), cls, sub.name(), tea.name, rm);

            store.save();
            room.setText("");
            info(this, "Lecture added successfully.");
        }

        @Override void refresh() {
            subject.setModel(new DefaultComboBoxModel<>(store.subjects.toArray(new Subject[0])));
            teacher.setModel(new DefaultComboBoxModel<>(store.teachers.toArray(new Teacher[0])));
            TreeSet<String> classes = new TreeSet<>();
            store.students.forEach(st -> classes.add(st.className));
            store.lectures.forEach(l -> classes.add(l.className()));
            classBox.setModel(new DefaultComboBoxModel<>(classes.toArray(new String[0])));
            classBox.setSelectedItem("");
        }
    }

    // ============================== TIMETABLE ==============================
    static class TimetablePage extends Page {
        private final Predicate<Lecture> scope;
        private final JComboBox<String> dayFilter = new JComboBox<>();
        private final JTextField searchField = field();
        private final DefaultTableModel listModel = model("Day", "Time", "Class", "Subject", "Teacher", "Room");
        private final JTable listTable = table(listModel);
        private final DefaultTableModel gridModel = model(gridColumns());
        private final JTable gridTable = table(gridModel);
        private final CardLayout views = new CardLayout();
        private final JPanel viewPanel = new JPanel(views);
        private final JButton listBtn = new RoundButton("List View", ACTIVE);
        private final JButton gridBtn = new RoundButton("Weekly Grid", MUTED_BTN);
        private JButton deleteBtn;
        private List<Lecture> shown = new ArrayList<>();

        private static String[] gridColumns() {
            String[] cols = new String[DAYS.length + 1];
            cols[0] = "Time";
            System.arraycopy(DAYS, 0, cols, 1, DAYS.length);
            return cols;
        }

        TimetablePage(DataStore s, String title, String subtitle, Predicate<Lecture> scope, boolean canDelete) {
            super(s, title, subtitle);
            this.scope = scope;

            dayFilter.addItem("All Days");
            for (String d : DAYS) dayFilter.addItem(d);
            dayFilter.setFont(F_NORMAL);
            dayFilter.addActionListener(e -> refresh());

            searchField.setPreferredSize(new Dimension(160, 34));
            searchField.setToolTipText("Filter by Subject, Class or Teacher");
            searchField.addKeyListener(new KeyAdapter() {
                @Override public void keyReleased(KeyEvent e) { refresh(); }
            });

            gridTable.setRowHeight(100);
            gridTable.setDefaultRenderer(Object.class, new GridRenderer());
            gridTable.setRowSelectionAllowed(false);
            gridTable.setShowVerticalLines(true);
            gridTable.getColumnModel().getColumn(0).setMaxWidth(120);
            gridTable.getColumnModel().getColumn(0).setPreferredWidth(110);

            viewPanel.add(scroll(listTable), "list");
            viewPanel.add(scroll(gridTable), "grid");

            listBtn.addActionListener(e -> showView("list"));
            gridBtn.addActionListener(e -> showView("grid"));

            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            left.setOpaque(false);
            left.add(label("Day:", F_BOLD, NAVY));
            left.add(dayFilter);
            left.add(label("Search:", F_BOLD, NAVY));
            left.add(searchField);
            left.add(listBtn);
            left.add(gridBtn);

            JPanel bar = new JPanel(new BorderLayout());
            bar.setOpaque(false);
            bar.add(left, BorderLayout.WEST);

            JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            rightBtns.setOpaque(false);

            JButton exportBtn = new RoundButton("Export", PURPLE);
            exportBtn.addActionListener(e -> exportTimetable());
            rightBtns.add(exportBtn);

            if (canDelete) {
                deleteBtn = new RoundButton("Delete Selected", DANGER);
                deleteBtn.addActionListener(e -> {
                    int row = listTable.getSelectedRow();
                    if (row == -1) {
                        error(this, "Please select a lecture to delete.");
                        return;
                    }

                    String day = (String) listModel.getValueAt(row, 0);
                    String slot = (String) listModel.getValueAt(row, 1);
                    String subject = (String) listModel.getValueAt(row, 3);
                    String teacher = (String) listModel.getValueAt(row, 4);

                    if (confirm(this, "Are you sure you want to delete this lecture from Database?")) {
                        boolean deleted = deleteLectureFromDB(day, slot, subject, teacher);
                        if (deleted) {
                            info(this, "Lecture deleted successfully from Database!");
                            refresh();
                        } else {
                            error(this, "Failed to delete lecture from Database.");
                        }
                    }
                });
                rightBtns.add(deleteBtn);
            }
            bar.add(rightBtns, BorderLayout.EAST);

            JPanel center = new JPanel(new BorderLayout(0, 12));
            center.setOpaque(false);
            center.add(bar, BorderLayout.NORTH);
            center.add(viewPanel, BorderLayout.CENTER);
            add(center, BorderLayout.CENTER);
            refresh();
        }

        private void exportTimetable() {
            StringBuilder sb = new StringBuilder();
            sb.append("COLLEGE TIMETABLE EXPORT\nGenerated by Yash Kumar Sahu's System\n");
            sb.append("====================================================\n\n");
            for (Lecture l : shown) {
                sb.append(String.format("%-10s | %-15s | Class: %-8s | %-20s | Teacher: %-20s | Room: %s\n",
                        l.day(), l.slot(), l.className(), store.subjectName(l.subjectCode()),
                        store.teacherName(l.teacherId()), l.room()));
            }
            JTextArea area = new JTextArea(sb.toString(), 15, 50);
            area.setFont(new Font("Monospaced", Font.PLAIN, 12));
            area.setEditable(false);
            JOptionPane.showMessageDialog(this, new JScrollPane(area), "Exported Timetable", JOptionPane.INFORMATION_MESSAGE);
        }

        private void showView(String v) {
            boolean list = v.equals("list");
            views.show(viewPanel, v);
            dayFilter.setEnabled(list);
            searchField.setEnabled(list);
            if (deleteBtn != null) deleteBtn.setEnabled(list);
            listBtn.setBackground(list ? ACTIVE : MUTED_BTN);
            gridBtn.setBackground(list ? MUTED_BTN : ACTIVE);
        }

        @Override void refresh() {
            loadLecturesFromDB(listModel);
            List<Lecture> scoped = store.lectures.stream().filter(scope).collect(Collectors.toList());

            String f = (String) dayFilter.getSelectedItem();
            String q = searchField.getText().trim().toLowerCase();

            shown = scoped.stream()
                    .filter(l -> f == null || f.equals("All Days") || l.day().equals(f))
                    .filter(l -> q.isEmpty() || store.subjectName(l.subjectCode()).toLowerCase().contains(q)
                            || store.teacherName(l.teacherId()).toLowerCase().contains(q)
                            || l.className().toLowerCase().contains(q)
                            || l.room().toLowerCase().contains(q))
                    .sorted(Main::compareLectures)
                    .collect(Collectors.toList());

            gridModel.setRowCount(0);
            for (String slot : SLOTS) {
                Object[] row = new Object[DAYS.length + 1];
                row[0] = slot;
                for (int d = 0; d < DAYS.length; d++) {
                    StringBuilder sb = new StringBuilder();
                    for (Lecture l : scoped) {
                        if (!l.day().equals(DAYS[d]) || !l.slot().equals(slot)) continue;
                        if (sb.length() > 0) sb.append("\n\n");
                        sb.append(store.subjectName(l.subjectCode())).append('\n')
                                .append(store.teacherName(l.teacherId())).append('\n')
                                .append("Room ").append(l.room()).append(" | ").append(l.className());
                    }
                    row[d + 1] = sb.toString();
                }
                gridModel.addRow(row);
            }
        }
    }

    // ============================== TEACHERS ==============================
    static class TeachersPage extends Page {
        private final JTextField nameField = field(), deptField = field(), userField = field();
        private final DefaultTableModel model = model("Name", "Department", "Username");
        private final JTable table = table(model);

        TeachersPage(DataStore s) {
            super(s, "Manage Teachers", "Create teacher accounts and grant login access permissions.");

            RoundPanel form = formCard();
            addField(form, 0, 0, "Teacher Name", nameField);
            addField(form, 1, 0, "Department", deptField);
            addField(form, 2, 0, "Username / Teacher ID", userField);

            JButton addBtn = new RoundButton("Add Teacher", SUCCESS);
            addBtn.addActionListener(e -> addTeacher());
            addButtonRow(form, 1, 3, addBtn);

            JButton deleteBtn = new RoundButton("Delete Selected", DANGER);
            deleteBtn.addActionListener(e -> deleteTeacher());
            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            bottom.setOpaque(false);
            bottom.add(deleteBtn);

            JPanel center = new JPanel(new BorderLayout(0, 14));
            center.setOpaque(false);
            center.add(form, BorderLayout.NORTH);
            center.add(scroll(table), BorderLayout.CENTER);
            center.add(bottom, BorderLayout.SOUTH);
            add(center, BorderLayout.CENTER);
            refresh();
        }

        private void addTeacher() {
            String n = nameField.getText().trim();
            String d = deptField.getText().trim();
            String u = userField.getText().trim().toLowerCase();
            if (n.isEmpty() || d.isEmpty() || u.isEmpty()) { error(this, "Please fill in all fields."); return; }
            if (u.contains(" ") || u.equals("admin")) { error(this, "Invalid username."); return; }
            if (store.teacher(u) != null) { error(this, "This username already exists."); return; }

            store.teachers.add(new Teacher(u, n, d));
            saveUserToDatabase(u, "TEACHER", u + "@faculty.poornima.org");

            store.save();
            nameField.setText(""); deptField.setText(""); userField.setText("");
            info(this, "Teacher pre-approved and added successfully!");
            refresh();
        }

        private void deleteTeacher() {
            int row = table.getSelectedRow();
            if (row < 0) { error(this, "Select a teacher first."); return; }
            Teacher t = store.teachers.get(row);
            if (store.lectures.stream().anyMatch(l -> l.teacherId().equalsIgnoreCase(t.username))) {
                error(this, "This teacher has scheduled lectures. Delete those lectures first.");
                return;
            }
            if (confirm(this, "Delete " + t.name + "?")) {
                store.teachers.remove(row);
                deleteUserFromDatabase(t.username);
                store.save();
                refresh();
            }
        }

        @Override void refresh() {
            model.setRowCount(0);
            for (Teacher t : store.teachers) model.addRow(new Object[]{t.name, t.department, t.username});
        }
    }

    // ============================== STUDENTS ==============================
    static class StudentsPage extends Page {
        private final boolean canEdit;
        private final JTextField rollField = field(), nameField = field(), classField = field();
        private final JTextField search = field();
        private final DefaultTableModel model = model("Roll No / Username", "Name", "Class");
        private final JTable table = table(model);
        private List<Student> shown = new ArrayList<>();

        StudentsPage(DataStore s, boolean canEdit) {
            super(s, canEdit ? "Manage Students" : "Students",
                    canEdit ? "Create student accounts and grant login access permissions."
                            : "View the list of students (read only).");
            this.canEdit = canEdit;

            JPanel center = new JPanel(new BorderLayout(0, 14));
            center.setOpaque(false);

            if (canEdit) {
                RoundPanel form = formCard();
                addField(form, 0, 0, "Roll No / Username", rollField);
                addField(form, 1, 0, "Student Name", nameField);
                addField(form, 2, 0, "Class / Section", classField);
                JButton addBtn = new RoundButton("Add Student", SUCCESS);
                addBtn.addActionListener(e -> addStudent());
                addButtonRow(form, 1, 3, addBtn);
                center.add(form, BorderLayout.NORTH);
            }

            JPanel searchBar = new JPanel(new BorderLayout(10, 0));
            searchBar.setOpaque(false);
            searchBar.add(label("Search:", F_BOLD, NAVY), BorderLayout.WEST);
            searchBar.add(search, BorderLayout.CENTER);
            search.addKeyListener(new KeyAdapter() {
                @Override public void keyReleased(KeyEvent e) { refresh(); }
            });

            JPanel middle = new JPanel(new BorderLayout(0, 10));
            middle.setOpaque(false);
            middle.add(searchBar, BorderLayout.NORTH);
            middle.add(scroll(table), BorderLayout.CENTER);
            center.add(middle, BorderLayout.CENTER);

            if (canEdit) {
                JButton deleteBtn = new RoundButton("Delete Selected", DANGER);
                deleteBtn.addActionListener(e -> deleteStudent());
                JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
                bottom.setOpaque(false);
                bottom.add(deleteBtn);
                center.add(bottom, BorderLayout.SOUTH);
            }
            add(center, BorderLayout.CENTER);
            refresh();
        }

        private void addStudent() {
            String r = rollField.getText().trim().toLowerCase();
            String n = nameField.getText().trim();
            String c = classField.getText().trim();
            if (r.isEmpty() || n.isEmpty() || c.isEmpty()) { error(this, "Please fill in all fields."); return; }
            if (r.contains(" ")) { error(this, "Roll number cannot contain spaces."); return; }
            if (store.students.stream().anyMatch(x -> x.rollNo.equalsIgnoreCase(r))) {
                error(this, "A student with this roll number already exists.");
                return;
            }
            store.students.add(new Student(r, n, c));
            saveUserToDatabase(r, "STUDENT", r + "@poornima.org");

            store.save();
            rollField.setText(""); nameField.setText(""); classField.setText("");
            info(this, "Student pre-approved and added successfully!");
            refresh();
        }

        private void deleteStudent() {
            int row = table.getSelectedRow();
            if (row < 0) { error(this, "Select a student first."); return; }
            Student st = shown.get(row);
            if (confirm(this, "Delete " + st.name + "?")) {
                store.students.remove(st);
                deleteUserFromDatabase(st.rollNo);
                store.save();
                refresh();
            }
        }

        @Override void refresh() {
            String q = search.getText().trim().toLowerCase();
            shown = store.students.stream()
                    .filter(s -> q.isEmpty() || s.name.toLowerCase().contains(q)
                            || s.rollNo.toLowerCase().contains(q) || s.className.toLowerCase().contains(q))
                    .collect(Collectors.toList());
            model.setRowCount(0);
            for (Student s : shown) model.addRow(new Object[]{s.rollNo, s.name, s.className});
        }
    }

    static class SubjectsPage extends Page {
        private final JTextField codeField = field(), nameField = field();
        private final DefaultTableModel model = model("Code", "Subject Name");
        private final JTable table = table(model);

        SubjectsPage(DataStore s) {
            super(s, "Manage Subjects", "Add and remove subjects offered by the college.");

            RoundPanel form = formCard();
            addField(form, 0, 0, "Subject Code", codeField);
            addField(form, 1, 0, "Subject Name", nameField);
            JButton addBtn = new RoundButton("Add Subject", SUCCESS);
            addBtn.addActionListener(e -> addSubject());
            addButtonRow(form, 1, 2, addBtn);

            JButton deleteBtn = new RoundButton("Delete Selected", DANGER);
            deleteBtn.addActionListener(e -> deleteSubject());
            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            bottom.setOpaque(false);
            bottom.add(deleteBtn);

            JPanel center = new JPanel(new BorderLayout(0, 14));
            center.setOpaque(false);
            center.add(form, BorderLayout.NORTH);
            center.add(scroll(table), BorderLayout.CENTER);
            center.add(bottom, BorderLayout.SOUTH);
            add(center, BorderLayout.CENTER);
            refresh();
        }

        private void addSubject() {
            String c = codeField.getText().trim().toUpperCase(), n = nameField.getText().trim();
            if (c.isEmpty() || n.isEmpty()) { error(this, "Please fill in all fields."); return; }
            if (store.subjects.stream().anyMatch(x -> x.code().equalsIgnoreCase(c))) {
                error(this, "A subject with this code already exists.");
                return;
            }
            store.subjects.add(new Subject(c, n));
            store.save();
            codeField.setText(""); nameField.setText("");
            refresh();
        }

        private void deleteSubject() {
            int row = table.getSelectedRow();
            if (row < 0) { error(this, "Select a subject first."); return; }
            Subject sub = store.subjects.get(row);
            if (store.lectures.stream().anyMatch(l -> l.subjectCode().equalsIgnoreCase(sub.code()))) {
                error(this, "This subject has scheduled lectures. Delete those lectures first.");
                return;
            }
            if (confirm(this, "Delete " + sub.name() + "?")) {
                store.subjects.remove(row);
                store.save();
                refresh();
            }
        }

        @Override void refresh() {
            model.setRowCount(0);
            for (Subject s : store.subjects) model.addRow(new Object[]{s.code(), s.name()});
        }
    }

    static class NoticesPage extends Page {
        private final Session session;
        private final JTextField titleField = field();
        private final JTextArea messageArea = new JTextArea(3, 30);
        private final JPanel list = new JPanel();

        NoticesPage(DataStore s, Session session) {
            super(s, "Notice Board", session.role() == Role.STUDENT
                    ? "Latest announcements from your college."
                    : "Post announcements and manage your notices.");
            this.session = session;

            JPanel center = new JPanel(new BorderLayout(0, 14));
            center.setOpaque(false);

            if (session.role() != Role.STUDENT) {
                messageArea.setFont(F_NORMAL);
                messageArea.setLineWrap(true);
                messageArea.setWrapStyleWord(true);
                messageArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
                JScrollPane msgScroll = new JScrollPane(messageArea);
                msgScroll.setBorder(BorderFactory.createLineBorder(new Color(190, 200, 220)));
                msgScroll.setPreferredSize(new Dimension(240, 80));

                RoundPanel form = formCard();
                addField(form, 0, 0, "Title", titleField);
                addField(form, 0, 1, "Message", msgScroll);
                JButton postBtn = new RoundButton("Post Notice", SUCCESS);
                postBtn.addActionListener(e -> post());
                addButtonRow(form, 2, 1, postBtn);
                center.add(form, BorderLayout.NORTH);
            }

            list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
            list.setOpaque(false);
            ScrollPanel holder = new ScrollPanel(new BorderLayout());
            holder.setOpaque(false);
            holder.add(list, BorderLayout.NORTH);
            JScrollPane sp = new JScrollPane(holder);
            sp.setBorder(null);
            sp.setOpaque(false);
            sp.getViewport().setOpaque(false);
            sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            sp.getVerticalScrollBar().setUnitIncrement(16);
            sp.getVerticalScrollBar().setUI(new ModernScrollBarUI());
            center.add(sp, BorderLayout.CENTER);

            add(center, BorderLayout.CENTER);
            refresh();
        }

        private void post() {
            String t = titleField.getText().trim(), m = messageArea.getText().trim();
            if (t.isEmpty() || m.isEmpty()) { error(this, "Please enter a title and a message."); return; }
            store.notices.add(new Notice(t, m, session.id(), session.name(), session.role(), LocalDateTime.now()));
            store.save();
            titleField.setText("");
            messageArea.setText("");
            refresh();
        }

        @Override void refresh() {
            list.removeAll();
            List<Notice> sorted = new ArrayList<>(store.notices);
            sorted.sort((a, b) -> b.time().compareTo(a.time()));

            if (sorted.isEmpty()) {
                list.add(label("No notices yet.", F_NORMAL, MUTED_TEXT));
            }
            for (Notice n : sorted) {
                boolean canDelete = session.role() == Role.ADMIN
                        || (n.authorId().equalsIgnoreCase(session.id()) && n.role() == session.role());

                RoundPanel card = new RoundPanel(new BorderLayout(0, 8));
                card.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

                JPanel head = new JPanel(new BorderLayout());
                head.setOpaque(false);
                head.add(label(n.title(), new Font(FONT, Font.BOLD, 16), NAVY), BorderLayout.WEST);
                head.add(label(n.time().format(DATE_FMT), F_SMALL, MUTED_TEXT), BorderLayout.EAST);

                JPanel north = new JPanel(new GridLayout(2, 1, 0, 2));
                north.setOpaque(false);
                north.add(head);
                north.add(label("Posted by " + n.authorName() + " (" + n.role().label + ")",
                        F_SMALL, roleColor(n.role())));

                JTextArea body = new JTextArea(n.message());
                body.setEditable(false);
                body.setLineWrap(true);
                body.setWrapStyleWord(true);
                body.setOpaque(false);
                body.setFont(F_NORMAL);

                card.add(north, BorderLayout.NORTH);
                card.add(body, BorderLayout.CENTER);

                if (canDelete) {
                    JButton del = new RoundButton("Delete", DANGER);
                    del.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
                    del.addActionListener(e -> {
                        if (confirm(this, "Delete this notice?")) {
                            store.notices.remove(n);
                            store.save();
                            refresh();
                        }
                    });
                    JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
                    foot.setOpaque(false);
                    foot.add(del);
                    card.add(foot, BorderLayout.SOUTH);
                }
                list.add(card);
                list.add(Box.createVerticalStrut(12));
            }
            list.revalidate();
            list.repaint();
        }
    }

    static class ProfilePage extends Page {
        private final Session session;

        ProfilePage(DataStore s, Session session) {
            super(s, "My Profile", "View your details and active session status.");
            this.session = session;

            RoundPanel details = formCard();
            addField(details, 0, 0, "Role", new JLabel(session.role().label));
            addField(details, 0, 1, "Name / Username", new JLabel(session.name()));
            addField(details, 0, 2, session.role() == Role.STUDENT ? "Roll No" : "User ID", new JLabel(session.id()));
            if (session.role() == Role.STUDENT) addField(details, 0, 3, "Class", new JLabel(session.className()));
            if (session.role() == Role.TEACHER) addField(details, 0, 3, "Department", new JLabel(session.department()));

            RoundPanel authStatus = formCard();
            addField(authStatus, 0, 0, "Authentication Mode", new JLabel("Passwordless (Gmail OTP Only)"));
            addField(authStatus, 0, 1, "Admin Access Control", new JLabel("Pre-Approved Verification Active"));
            addField(authStatus, 0, 2, "Backend Engine", new JLabel("MySQL Database Persistent"));

            JPanel grid = new JPanel(new GridLayout(1, 2, 20, 0));
            grid.setOpaque(false);
            grid.add(wrapTitled("Account Details", details));
            grid.add(wrapTitled("Authentication Status", authStatus));

            JPanel wrap = new JPanel(new BorderLayout());
            wrap.setOpaque(false);
            wrap.add(grid, BorderLayout.NORTH);
            add(wrap, BorderLayout.CENTER);
        }

        private JPanel wrapTitled(String title, JComponent body) {
            JPanel p = new JPanel(new BorderLayout(0, 8));
            p.setOpaque(false);
            p.add(label(title, new Font(FONT, Font.BOLD, 17), NAVY), BorderLayout.NORTH);
            p.add(body, BorderLayout.CENTER);
            return p;
        }

        @Override void refresh() { }
    }

    // ============================== LOGIN WINDOW (WITH RESEND & RESET OTP) ==============================
    static class LoginFrame extends JFrame {

        private static final Preferences PREFS = Preferences.userNodeForPackage(LoginFrame.class);

        private final DataStore store;
        private Role role = Role.ADMIN;
        private int statusKind;

        private static String generatedOTP = null;
        private static String targetUserEmail = null;
        private static String targetUserName = null;

        private final FormPanel form = new FormPanel();
        private final RoleSelector selector = new RoleSelector(this::selectRole);
        private final InputField identifierField = new InputField("Enter Email / Mobile", InputField.USER, false);
        private final InputField otpField = new InputField("Enter 6-Digit OTP", InputField.LOCK, false);
        private final GlowButton sendOtpBtn = new GlowButton("Send OTP via Mail");
        private final GlowButton verifyLoginBtn = new GlowButton("Verify & Sign In");
        private final JLabel status = label(" ", new Font(FONT, Font.PLAIN, 13), DANGER);

        LoginFrame(DataStore store) {
            this.store = store;
            setTitle("Login - College Time Table Management System | MADE BY YASH KUMAR SAHU");
            setSize(1040, 680);
            setResizable(false);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLocationRelativeTo(null);
            setLayout(new GridLayout(1, 2));

            JPanel formPanel = buildFormPanel();
            add(buildBrandPanel());
            add(formPanel);

            try { role = Role.valueOf(PREFS.get("role", "ADMIN")); } catch (IllegalArgumentException ignored) { }
            selector.init(role);
            selectRole(role);
        }

        public static boolean sendOTPViaEmail(String toEmail, String otpCode) {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");

            javax.mail.Session mailSession = javax.mail.Session.getInstance(props, new javax.mail.Authenticator() {
                @Override
                protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                    return new javax.mail.PasswordAuthentication(PARENT_EMAIL, PARENT_EMAIL_PASSWORD);
                }
            });

            try {
                Message message = new MimeMessage(mailSession);
                message.setFrom(new InternetAddress(PARENT_EMAIL, "Time Table Management System"));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
                message.setSubject("Your Login Verification Code - TTMS");
                message.setText("Hello,\n\nYour fresh 6-digit OTP verification code for login is: "
                        + otpCode + "\n\nThis code is valid for single use.\n\nRegards,\nCollege Time Table System");

                Transport.send(message);
                return true;
            } catch (Exception e) {
                System.err.println("Email Sending Failed: " + e.getMessage());
                return false;
            }
        }

        public static String processUserAndGenerateOTP(DataStore store, String identifier, String roleLabel) {
            String inputEmail = identifier.trim().toLowerCase();
            if (inputEmail.isEmpty()) return null;

            String extractedId = inputEmail.contains("@") ? inputEmail.split("@")[0].trim().toLowerCase() : inputEmail.trim().toLowerCase();

            if (roleLabel.equalsIgnoreCase("ADMIN")) {
                if (!inputEmail.equals("kumarsahuyash5@gmail.com") && !inputEmail.equals("admin@college.com")) {
                    return "ERR_INVALID_ADMIN_EMAIL";
                }
            } else if (roleLabel.equalsIgnoreCase("TEACHER")) {
                if (!inputEmail.contains("teacher") && !inputEmail.endsWith("@faculty.poornima.org") && !inputEmail.endsWith("@college.com")) {
                    return "ERR_INVALID_TEACHER_EMAIL";
                }

                boolean existsInStore = store.teachers.stream().anyMatch(t -> t.username.trim().equalsIgnoreCase(extractedId));
                boolean existsInDb = checkUserExistsInDB(extractedId);

                if (!existsInStore && !existsInDb) {
                    return "ERR_NOT_REGISTERED_BY_ADMIN";
                }

            } else if (roleLabel.equalsIgnoreCase("STUDENT")) {
                if (!inputEmail.endsWith("@poornima.org") && !inputEmail.endsWith("@student.poornima.org")) {
                    return "ERR_INVALID_STUDENT_EMAIL";
                }

                boolean existsInStore = store.students.stream().anyMatch(s -> s.rollNo.trim().equalsIgnoreCase(extractedId));
                boolean existsInDb = checkUserExistsInDB(extractedId);

                if (!existsInStore && !existsInDb) {
                    return "ERR_NOT_REGISTERED_BY_ADMIN";
                }
            }

            targetUserEmail = inputEmail;
            targetUserName = extractedId;

            int otpInt = 100000 + new Random().nextInt(900000);
            generatedOTP = String.valueOf(otpInt);

            return generatedOTP;
        }

        public static boolean verifyOTP(String inputOTP) {
            return generatedOTP != null && generatedOTP.equals(inputOTP.trim());
        }

        @Override public void setVisible(boolean visible) {
            super.setVisible(visible);
            if (visible) {
                form.playIntro();
                SwingUtilities.invokeLater(identifierField::focusInput);
            }
        }

        private static JComponent left(JComponent c) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            return c;
        }

        private JPanel buildBrandPanel() {
            BrandPanel p = new BrandPanel();
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            p.setBorder(BorderFactory.createEmptyBorder(60, 55, 40, 40));

            Avatar logo = new Avatar("TT", Color.WHITE, NAVY, 64);
            logo.setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(logo);
            p.add(Box.createVerticalStrut(20));
            p.add(left(label("COLLEGE", new Font(FONT, Font.BOLD, 13), new Color(190, 205, 245))));
            p.add(Box.createVerticalStrut(6));
            p.add(left(label("Time Table", new Font(FONT, Font.BOLD, 36), Color.WHITE)));
            p.add(left(label("Management System", new Font(FONT, Font.BOLD, 36), Color.WHITE)));
            p.add(Box.createVerticalStrut(10));
            p.add(left(label("MADE BY YASH KUMAR SAHU", new Font(FONT, Font.BOLD, 14), new Color(255, 215, 0))));
            p.add(Box.createVerticalStrut(14));
            p.add(left(label("Plan smarter. Teach better. Learn on time.",
                    new Font(FONT, Font.PLAIN, 15), new Color(215, 225, 250))));
            p.add(Box.createVerticalStrut(28));
            p.add(new FeatureItem("Admin", "Full control of schedules and users", () -> selector.select(Role.ADMIN)));
            p.add(Box.createVerticalStrut(10));
            p.add(new FeatureItem("Teacher", "Personal schedule and notices", () -> selector.select(Role.TEACHER)));
            p.add(Box.createVerticalStrut(10));
            p.add(new FeatureItem("Student", "Class timetable and announcements", () -> selector.select(Role.STUDENT)));
            p.add(Box.createVerticalGlue());
            p.add(left(label("\u00A9 " + LocalDate.now().getYear() + " Yash Kumar Sahu. All rights reserved.",
                    F_SMALL, new Color(180, 195, 235))));
            return p;
        }

        private JPanel buildFormPanel() {
            form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
            form.setBorder(BorderFactory.createEmptyBorder(48, 60, 28, 60));

            form.add(left(label("Welcome back", new Font(FONT, Font.BOLD, 32), NAVY)));
            form.add(Box.createVerticalStrut(6));
            form.add(left(label("Enter Email to receive OTP verification", new Font(FONT, Font.PLAIN, 15), MUTED_TEXT)));
            form.add(Box.createVerticalStrut(26));

            form.add(left(label("Sign in as", F_BOLD, NAVY)));
            form.add(Box.createVerticalStrut(8));
            form.add(left(selector));
            form.add(Box.createVerticalStrut(18));

            form.add(left(identifierField));
            form.add(Box.createVerticalStrut(10));

            otpField.setVisible(false);
            form.add(left(otpField));
            form.add(Box.createVerticalStrut(8));

            // Resend & Change Email Action Bar
            JPanel actionLinkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
            actionLinkPanel.setOpaque(false);
            actionLinkPanel.setVisible(false);

            JButton resendBtn = new JButton("Resend OTP");
            resendBtn.setFont(F_SMALL);
            resendBtn.setForeground(ACTIVE);
            resendBtn.setBorderPainted(false);
            resendBtn.setContentAreaFilled(false);
            resendBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JButton changeEmailBtn = new JButton("Change Email");
            changeEmailBtn.setFont(F_SMALL);
            changeEmailBtn.setForeground(DANGER);
            changeEmailBtn.setBorderPainted(false);
            changeEmailBtn.setContentAreaFilled(false);
            changeEmailBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            actionLinkPanel.add(resendBtn);
            actionLinkPanel.add(changeEmailBtn);
            form.add(left(actionLinkPanel));
            form.add(Box.createVerticalStrut(10));

            status.setPreferredSize(new Dimension(300, 20));
            status.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
            form.add(left(status));
            form.add(Box.createVerticalStrut(8));

            form.add(left(sendOtpBtn));
            verifyLoginBtn.setVisible(false);
            form.add(left(verifyLoginBtn));

            form.add(Box.createVerticalGlue());

            sendOtpBtn.addActionListener(e -> {
                String idInput = identifierField.value().trim();
                if (idInput.isEmpty()) {
                    setStatus("Please enter a valid Email ID.", DANGER, 1);
                    return;
                }

                String code = processUserAndGenerateOTP(store, idInput, role.label);

                if ("ERR_NOT_REGISTERED_BY_ADMIN".equals(code)) {
                    setStatus("Access Denied! Account not added by Admin.", DANGER, 1);
                    error(this, "Access Denied!\nYour account is not pre-approved by Admin.");
                    return;
                } else if ("ERR_INVALID_ADMIN_EMAIL".equals(code)) {
                    setStatus("Access Denied! Not an Admin email.", DANGER, 1);
                    return;
                } else if ("ERR_INVALID_TEACHER_EMAIL".equals(code)) {
                    setStatus("Invalid Teacher Email domain!", DANGER, 1);
                    return;
                } else if ("ERR_INVALID_STUDENT_EMAIL".equals(code)) {
                    setStatus("Invalid Student Email domain!", DANGER, 1);
                    return;
                }

                if (code != null) {
                    setStatus("Sending OTP to " + idInput + "...", ACTIVE, 0);
                    new Thread(() -> {
                        boolean mailSent = sendOTPViaEmail(idInput, code);
                        SwingUtilities.invokeLater(() -> {
                            if (mailSent) {
                                setStatus("Fresh OTP sent successfully to your email!", SUCCESS, 0);
                                info(this, "A fresh OTP has been sent to your registered email:\n" + idInput);
                            } else {
                                setStatus("Failed to send OTP via email.", DANGER, 1);
                            }
                            sendOtpBtn.setVisible(false);
                            otpField.setVisible(true);
                            verifyLoginBtn.setVisible(true);
                            actionLinkPanel.setVisible(true);
                            form.revalidate();
                            form.repaint();
                            otpField.focusInput();
                        });
                    }).start();
                }
            });

            resendBtn.addActionListener(e -> {
                String idInput = identifierField.value().trim();
                String code = processUserAndGenerateOTP(store, idInput, role.label);
                if (code != null) {
                    setStatus("Resending fresh OTP...", ACTIVE, 0);
                    new Thread(() -> {
                        boolean mailSent = sendOTPViaEmail(idInput, code);
                        SwingUtilities.invokeLater(() -> {
                            if (mailSent) {
                                setStatus("New OTP resent successfully!", SUCCESS, 0);
                                info(this, "A fresh OTP has been resent to your email!");
                            } else {
                                setStatus("Resend failed. Check connection.", DANGER, 1);
                            }
                        });
                    }).start();
                }
            });

            changeEmailBtn.addActionListener(e -> {
                otpField.setVisible(false);
                verifyLoginBtn.setVisible(false);
                actionLinkPanel.setVisible(false);
                sendOtpBtn.setVisible(true);
                setStatus(" ", DANGER, 0);
                form.revalidate();
                form.repaint();
                identifierField.focusInput();
            });

            verifyLoginBtn.addActionListener(e -> {
                String userOtp = otpField.value().trim();
                if (verifyOTP(userOtp)) {
                    info(this, "OTP Verified Successfully!");

                    Teacher t = store.teacher(targetUserName);
                    String displayName = (t != null) ? t.name : targetUserName;
                    String dept = (t != null) ? t.department : "Computer Science";

                    Student st = store.students.stream().filter(s -> s.rollNo.equalsIgnoreCase(targetUserName)).findFirst().orElse(null);
                    String clsName = (st != null) ? st.className : "CSE-C";
                    if (st != null) displayName = st.name;

                    Session s = new Session(role, targetUserName, displayName, clsName, dept);
                    dispose();
                    new Main(store, s).setVisible(true);
                } else {
                    setStatus("Invalid OTP! Click 'Resend OTP' if needed.", DANGER, 1);
                    otpField.setError(true);
                }
            });

            getRootPane().setDefaultButton(sendOtpBtn);
            return form;
        }

        private void selectRole(Role r) {
            role = r;
            identifierField.setError(false);
            otpField.setError(false);
            if (statusKind == 1) setStatus(" ", DANGER, 0);
        }

        private void setStatus(String text, Color c, int kind) {
            status.setText(text);
            status.setForeground(c);
            statusKind = kind;
        }

        static Color mix(Color a, Color b, float t) {
            t = Math.max(0f, Math.min(1f, t));
            return new Color(
                    (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                    (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                    (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                    (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
        }

        static float ease(float t) {
            return 1f - (float) Math.pow(1f - t, 3);
        }

        static class FormPanel extends JPanel {
            private float intro = 0f, shakeX = 0f;

            FormPanel() { setOpaque(false); }

            void playIntro() {
                long t0 = System.currentTimeMillis();
                Timer t = new Timer(12, null);
                t.addActionListener(e -> {
                    float p = (System.currentTimeMillis() - t0) / 550f;
                    p = Math.max(0f, Math.min(1f, p));
                    intro = ease(p);
                    if (p >= 1f) t.stop();
                    repaint();
                });
                t.start();
            }

            @Override public void paint(Graphics g) {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, getWidth(), getHeight());
                Graphics2D g2 = (Graphics2D) g.create();
                g2.translate(shakeX, (1f - intro) * 26f);
                g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, intro))));
                super.paint(g2);
                g2.dispose();
            }
        }

        static class BrandPanel extends JPanel {
            private static final int LOGO_CX = 55 + 32, LOGO_CY = 60 + 32;
            private static class Bubble { double x, y, r, vx, vy, depth; int alpha; }

            private final List<Bubble> bubbles = new ArrayList<>();
            private final Random rnd = new Random(7);
            private final Timer timer = new Timer(16, e -> step());
            private final long start = System.nanoTime();
            private float px, py;

            BrandPanel() {
                for (int i = 0; i < 11; i++) {
                    Bubble b = new Bubble();
                    b.x = rnd.nextDouble() * 520;
                    b.y = rnd.nextDouble() * 680;
                    b.r = 30 + rnd.nextDouble() * 95;
                    b.vx = (rnd.nextDouble() - 0.5) * 0.35;
                    b.vy = -(0.12 + rnd.nextDouble() * 0.38);
                    b.depth = b.r / 125.0;
                    b.alpha = 12 + rnd.nextInt(20);
                    bubbles.add(b);
                }
            }

            @Override public void addNotify() { super.addNotify(); timer.start(); }
            @Override public void removeNotify() { timer.stop(); super.removeNotify(); }

            private void step() {
                int w = getWidth(), h = getHeight();
                if (w <= 0 || h <= 0) return;
                for (Bubble b : bubbles) {
                    b.x += b.vx;
                    b.y += b.vy;
                    if (b.y + b.r < 0) { b.y = h + b.r; b.x = rnd.nextDouble() * w; }
                    if (b.x < -b.r) b.x = w + b.r;
                    if (b.x > w + b.r) b.x = -b.r;
                }
                float tx = 0, ty = 0;
                try {
                    PointerInfo pi = MouseInfo.getPointerInfo();
                    if (pi != null && isShowing()) {
                        Point pt = pi.getLocation();
                        SwingUtilities.convertPointFromScreen(pt, this);
                        tx = Math.max(-1f, Math.min(1f, (pt.x - w / 2f) / (w / 2f)));
                        ty = Math.max(-1f, Math.min(1f, (pt.y - h / 2f) / (h / 2f)));
                    }
                } catch (Exception ignored) { }
                px += (tx - px) * 0.05f;
                py += (ty - py) * 0.05f;
                repaint();
            }

            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                aa(g2);
                int w = getWidth(), h = getHeight();
                g2.setPaint(new GradientPaint(0, 0, NAVY_DARK, w, h, ACTIVE));
                g2.fillRect(0, 0, w, h);

                for (Bubble b : bubbles) {
                    double cx = b.x - px * 38 * b.depth;
                    double cy = b.y - py * 38 * b.depth;
                    Ellipse2D e = new Ellipse2D.Double(cx - b.r, cy - b.r, b.r * 2, b.r * 2);
                    g2.setColor(new Color(255, 255, 255, b.alpha));
                    g2.fill(e);
                    g2.setColor(new Color(255, 255, 255, Math.min(255, b.alpha + 12)));
                    g2.setStroke(new BasicStroke(1f));
                    g2.draw(e);
                }

                double t = (System.nanoTime() - start) / 1e9;
                float phase = (float) ((t % 2.6) / 2.6);
                float radius = 32 + phase * 30;
                g2.setStroke(new BasicStroke(2f));
                g2.setColor(new Color(255, 255, 255, (int) ((1f - phase) * 110)));
                g2.draw(new Ellipse2D.Float(LOGO_CX - radius, LOGO_CY - radius, radius * 2, radius * 2));
                g2.dispose();
            }
        }

        static class FeatureItem extends JComponent {
            private final String title, desc;
            private final Runnable onClick;
            private boolean over;
            private float hover;
            private final Timer timer = new Timer(14, e -> tick());

            FeatureItem(String title, String desc, Runnable onClick) {
                this.title = title; this.desc = desc; this.onClick = onClick;
                setPreferredSize(new Dimension(380, 62));
                setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
                setAlignmentX(Component.LEFT_ALIGNMENT);
                setFocusable(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { over = true; timer.start(); }
                    @Override public void mouseExited(MouseEvent e) { over = false; timer.start(); }
                    @Override public void mousePressed(MouseEvent e) { FeatureItem.this.onClick.run(); }
                });
            }

            private void tick() {
                float target = over ? 1f : 0f;
                hover += (target - hover) * 0.2f;
                if (Math.abs(target - hover) < 0.01f) { hover = target; timer.stop(); }
                repaint();
            }

            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                aa(g2);
                int w = getWidth(), h = getHeight();
                int slide = (int) (hover * 8);
                g2.setColor(new Color(255, 255, 255, (int) (16 + hover * 38)));
                g2.fillRoundRect(0, 0, w - 1, h - 1, 18, 18);
                g2.setColor(new Color(255, 255, 255, (int) (22 + hover * 50)));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 18, 18);

                g2.setColor(mix(new Color(170, 190, 245), Color.WHITE, hover));
                g2.fillOval(slide + 18, h / 2 - 5, 10, 10);

                g2.setColor(Color.WHITE);
                g2.setFont(new Font(FONT, Font.BOLD, 15));
                g2.drawString(title, slide + 42, h / 2 - 3);
                g2.setColor(new Color(215, 225, 250));
                g2.setFont(new Font(FONT, Font.PLAIN, 12));
                g2.drawString(desc, slide + 42, h / 2 + 15);

                if (hover > 0.02f) {
                    g2.setColor(new Color(255, 255, 255, (int) (hover * 230)));
                    g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int ax = w - 34 + (int) (hover * 6), ay = h / 2;
                    g2.drawLine(ax - 8, ay, ax, ay);
                    g2.drawLine(ax - 4, ay - 4, ax, ay);
                    g2.drawLine(ax - 4, ay + 4, ax, ay);
                }
                g2.dispose();
            }
        }

        static class RoleSelector extends JComponent {
            private final Role[] roles = Role.values();
            private final Consumer<Role> onChange;
            private int selected, hover = -1;
            private float pillX;
            private final Timer timer = new Timer(12, e -> tick());

            RoleSelector(Consumer<Role> onChange) {
                this.onChange = onChange;
                setPreferredSize(new Dimension(300, 46));
                setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setFocusable(true);
                MouseAdapter m = new MouseAdapter() {
                    @Override public void mousePressed(MouseEvent e) { requestFocusInWindow(); select(indexAt(e.getX())); }
                    @Override public void mouseMoved(MouseEvent e) { setHover(indexAt(e.getX())); }
                    @Override public void mouseExited(MouseEvent e) { setHover(-1); }
                };
                addMouseListener(m);
                addMouseMotionListener(m);
                addKeyListener(new KeyAdapter() {
                    @Override public void keyPressed(KeyEvent e) {
                        if (e.getKeyCode() == KeyEvent.VK_LEFT) select(selected - 1);
                        else if (e.getKeyCode() == KeyEvent.VK_RIGHT) select(selected + 1);
                    }
                });
            }

            void init(Role r) { selected = r.ordinal(); pillX = selected; repaint(); }
            void select(Role r) { select(r.ordinal()); }

            void select(int idx) {
                idx = Math.max(0, Math.min(roles.length - 1, idx));
                if (idx == selected) return;
                selected = idx;
                timer.start();
                onChange.accept(roles[idx]);
            }

            private int indexAt(int x) {
                float seg = (getWidth() - 8) / (float) roles.length;
                return Math.max(0, Math.min(roles.length - 1, (int) ((x - 4) / seg)));
            }

            private void setHover(int i) { if (hover != i) { hover = i; repaint(); } }

            private void tick() {
                pillX += (selected - pillX) * 0.22f;
                if (Math.abs(selected - pillX) < 0.002f) { pillX = selected; timer.stop(); }
                repaint();
            }

            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                aa(g2);
                int w = getWidth(), h = getHeight(), pad = 4;
                float seg = (w - 2 * pad) / (float) roles.length;

                g2.setColor(new Color(232, 237, 248));
                g2.fillRoundRect(0, 0, w, h, 16, 16);

                if (hover >= 0 && hover != selected) {
                    g2.setColor(new Color(255, 255, 255, 170));
                    g2.fill(new RoundRectangle2D.Float(pad + hover * seg, pad, seg, h - 2 * pad, 12, 12));
                }

                float px = pad + pillX * seg;
                g2.setColor(new Color(25, 42, 86, 45));
                g2.fill(new RoundRectangle2D.Float(px + 1, pad + 3, seg, h - 2 * pad, 12, 12));
                g2.setPaint(new GradientPaint(px, 0, NAVY, px + seg, 0, ACTIVE));
                g2.fill(new RoundRectangle2D.Float(px, pad, seg, h - 2 * pad, 12, 12));

                g2.setFont(F_BOLD);
                FontMetrics fm = g2.getFontMetrics();
                for (int i = 0; i < roles.length; i++) {
                    float near = Math.max(0f, 1f - Math.abs(pillX - i));
                    g2.setColor(mix(NAVY, Color.WHITE, near));
                    String s = roles[i].label;
                    int tx = (int) (pad + i * seg + (seg - fm.stringWidth(s)) / 2);
                    int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(s, tx, ty);
                }
                g2.dispose();
            }
        }

        static class InputField extends JPanel {
            static final int USER = 0, LOCK = 1;

            final JTextComponent input;
            private String label;
            private final int icon;
            private float focus, lift, err;
            private boolean focused, hasError;
            private Runnable editHook = () -> { };
            private final Timer timer = new Timer(14, e -> tick());

            InputField(String label, int icon, boolean password) {
                this.label = label;
                this.icon = icon;
                input = new JTextField();

                setOpaque(false);
                setLayout(new BorderLayout());
                setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
                setPreferredSize(new Dimension(300, 60));
                setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

                input.setOpaque(false);
                input.setFont(new Font(FONT, Font.PLAIN, 15));
                input.setForeground(NAVY);
                input.setCaretColor(NAVY);
                input.setSelectionColor(new Color(190, 205, 245));
                input.setBorder(BorderFactory.createEmptyBorder(20, 40, 4, 6));
                add(input, BorderLayout.CENTER);

                input.addFocusListener(new FocusAdapter() {
                    @Override public void focusGained(FocusEvent e) { focused = true; kick(); }
                    @Override public void focusLost(FocusEvent e) { focused = false; kick(); }
                });
                input.getDocument().addDocumentListener(new DocumentListener() {
                    @Override public void insertUpdate(DocumentEvent e) { edited(); }
                    @Override public void removeUpdate(DocumentEvent e) { edited(); }
                    @Override public void changedUpdate(DocumentEvent e) { edited(); }
                });
            }

            void setError(boolean b) { hasError = b; kick(); }
            void focusInput() { input.requestFocusInWindow(); }

            String value() {
                return input.getText();
            }

            private void edited() {
                hasError = false;
                editHook.run();
                kick();
            }

            private void kick() { if (!timer.isRunning()) timer.start(); }

            private void tick() {
                float tf = focused ? 1f : 0f;
                float tl = (focused || input.getDocument().getLength() > 0) ? 1f : 0f;
                float te = hasError ? 1f : 0f;
                focus += (tf - focus) * 0.22f;
                lift += (tl - lift) * 0.26f;
                err += (te - err) * 0.26f;
                if (Math.abs(tf - focus) < 0.01f && Math.abs(tl - lift) < 0.01f && Math.abs(te - err) < 0.01f) {
                    focus = tf; lift = tl; err = te;
                    timer.stop();
                }
                repaint();
            }

            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                aa(g2);
                int x = 3, y = 3, w = getWidth() - 7, h = getHeight() - 7;

                Color accent = mix(mix(new Color(200, 208, 225), ACTIVE, focus), DANGER, err);
                float glow = Math.max(focus * 0.9f, err);
                for (int i = 3; i >= 1; i--) {
                    g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (30 * glow / i)));
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawRoundRect(x - i, y - i, w + 2 * i, h + 2 * i, 14 + i * 2, 14 + i * 2);
                }

                g2.setColor(mix(new Color(247, 249, 254), Color.WHITE, focus));
                g2.fillRoundRect(x, y, w, h, 14, 14);
                g2.setColor(accent);
                g2.setStroke(new BasicStroke(1f + focus * 0.8f));
                g2.drawRoundRect(x, y, w, h, 14, 14);

                Color tint = mix(mix(MUTED_TEXT, ACTIVE, focus), DANGER, err);
                drawIcon(g2, x + 14, y + h / 2 - 10, tint);

                float size = 15f - 4f * lift;
                g2.setFont(new Font(FONT, lift > 0.5f ? Font.BOLD : Font.PLAIN, 15).deriveFont(size));
                g2.setColor(tint);
                float idleY = y + 37, upY = y + 15;
                float by = idleY * (1f - lift) + upY * lift;

                g2.drawString(label, x + 40, by);
                g2.dispose();
            }

            private void drawIcon(Graphics2D g2, int x, int y, Color c) {
                g2.setColor(c);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                if (icon == USER) {
                    g2.drawOval(x + 5, y, 8, 8);
                    g2.draw(new Arc2D.Float(x, y + 12, 18, 14, 0, 180, Arc2D.OPEN));
                } else {
                    g2.drawRoundRect(x + 1, y + 8, 16, 12, 4, 4);
                    g2.draw(new Arc2D.Float(x + 4, y, 10, 14, 0, 180, Arc2D.OPEN));
                    g2.fillOval(x + 7, y + 12, 4, 4);
                }
            }
        }

        static class GlowButton extends JButton {
            private boolean over, busy;
            private float hover, ripple = -1f, spin;
            private int rx, ry;
            private final Timer anim = new Timer(14, e -> tick());

            GlowButton(String text) {
                super(text);
                setFont(new Font(FONT, Font.BOLD, 16));
                setForeground(Color.WHITE);
                setContentAreaFilled(false);
                setFocusPainted(false);
                setBorderPainted(false);
                setOpaque(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(300, 56));
                setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { over = true; anim.start(); }
                    @Override public void mouseExited(MouseEvent e) { over = false; anim.start(); }
                    @Override public void mousePressed(MouseEvent e) { rx = e.getX(); ry = e.getY(); ripple = 0f; anim.start(); }
                });
            }

            private void tick() {
                float target = over ? 1f : 0f;
                hover += (target - hover) * 0.22f;
                if (Math.abs(target - hover) < 0.01f) hover = target;
                if (ripple >= 0f) { ripple += 0.05f; if (ripple >= 1f) ripple = -1f; }
                if (busy) spin = (spin + 9) % 360;
                if (hover == target && ripple < 0f && !busy) anim.stop();
                repaint();
            }

            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                aa(g2);
                int w = getWidth(), bh = getHeight() - 6;
                int press = getModel().isPressed() ? 1 : 0;

                if (hover > 0f) {
                    g2.setColor(new Color(25, 42, 86, (int) (60 * hover)));
                    g2.fillRoundRect(8, 9, w - 16, bh, 16, 16);
                }

                g2.translate(0, Math.round(-2 * hover) + press);

                RoundRectangle2D body = new RoundRectangle2D.Float(0, 0, w, bh, 16, 16);
                g2.setPaint(new GradientPaint(0, 0, mix(NAVY, NAVY_HOVER, hover),
                        w, 0, mix(new Color(50, 80, 170), ACTIVE, hover)));
                g2.fill(body);

                if (ripple >= 0f) {
                    Shape old = g2.getClip();
                    g2.clip(body);
                    float r = ripple * w;
                    g2.setColor(new Color(255, 255, 255, (int) ((1f - ripple) * 90)));
                    g2.fill(new Ellipse2D.Float(rx - r, ry - r, r * 2, r * 2));
                    g2.setClip(old);
                }

                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                String txt = getText();
                int tw = fm.stringWidth(txt);
                int ty = (bh - fm.getHeight()) / 2 + fm.getAscent();
                g2.setColor(Color.WHITE);

                int tx = (w - tw) / 2 - (int) (hover * 10);
                g2.drawString(txt, tx, ty);
                if (hover > 0.02f) {
                    int ax = tx + tw + 12, ay = bh / 2, len = (int) (hover * 16);
                    g2.setColor(new Color(255, 255, 255, (int) (hover * 255)));
                    g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(ax, ay, ax + len, ay);
                    g2.drawLine(ax + len - 6, ay - 6, ax + len, ay);
                    g2.drawLine(ax + len - 6, ay + 6, ax + len, ay);
                }
                g2.dispose();
            }
        }
    }

    private final DataStore store;
    private final Session session;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private final Map<String, Page> pages = new LinkedHashMap<>();
    private final Map<String, NavButton> navButtons = new LinkedHashMap<>();

    Main(DataStore store, Session session) {
        this.store = store;
        this.session = session;

        setTitle("College Time Table Management System - " + session.role().label + " | MADE BY YASH KUMAR SAHU");
        setSize(1200, 760);
        setMinimumSize(new Dimension(1050, 680));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { exitApp(); }
        });

        buildPages();
        contentPanel.setBackground(BG);
        pages.forEach((name, page) -> contentPanel.add(page, name));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);
        navigate("Dashboard");
    }

    private void buildPages() {
        pages.put("Dashboard", new DashboardPage(store, session));
        switch (session.role()) {
            case ADMIN -> {
                pages.put("Add Timetable", new AddTimetablePage(store));
                pages.put("Timetable", new TimetablePage(store, "Complete Timetable",
                        "View, filter and delete scheduled lectures.", l -> true, true));
                pages.put("Teachers", new TeachersPage(store));
                pages.put("Students", new StudentsPage(store, true));
                pages.put("Subjects", new SubjectsPage(store));
                pages.put("Notices", new NoticesPage(store, session));
            }
            case TEACHER -> {
                pages.put("My Schedule", new TimetablePage(store, "My Schedule",
                        "All lectures assigned to you.", scope(session), false));
                pages.put("Full Timetable", new TimetablePage(store, "Full Timetable",
                        "Complete college timetable (read only).", l -> true, false));
                pages.put("Students", new StudentsPage(store, false));
                pages.put("Notices", new NoticesPage(store, session));
            }
            case STUDENT -> {
                pages.put("My Timetable", new TimetablePage(store, "My Timetable",
                        "Weekly lectures for class " + session.className() + ".", scope(session), false));
                pages.put("Notices", new NoticesPage(store, session));
            }
        }
        pages.put("My Profile", new ProfilePage(store, session));
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                BorderFactory.createEmptyBorder(14, 25, 14, 25)));

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        titlePanel.setOpaque(false);
        titlePanel.add(label("TIME TABLE MANAGEMENT SYSTEM", new Font(FONT, Font.BOLD, 20), NAVY));
        titlePanel.add(label("|  MADE BY YASH KUMAR SAHU", new Font(FONT, Font.BOLD, 13), ACTIVE));

        header.add(titlePanel, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        right.add(label(session.name(), new Font(FONT, Font.BOLD, 14), Color.DARK_GRAY));
        right.add(new Badge(session.role().label.toUpperCase(), roleColor(session.role())));
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(NAVY);
        sidebar.setPreferredSize(new Dimension(235, 600));
        sidebar.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

        sidebar.add(Box.createVerticalStrut(28));
        String initial = session.name().replaceAll("^(Prof\\.|Dr\\.|Mr\\.|Ms\\.)\\s*", "");
        initial = initial.isEmpty() ? "?" : initial.substring(0, 1).toUpperCase();
        Avatar avatar = new Avatar(initial, ACTIVE, Color.WHITE, 66);
        avatar.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(avatar);
        sidebar.add(Box.createVerticalStrut(10));

        JLabel name = label(session.name(), new Font(FONT, Font.BOLD, 15), Color.WHITE);
        name.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(name);
        JLabel role = label(session.role().label + " Panel", F_SMALL, new Color(180, 195, 235));
        role.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(role);
        sidebar.add(Box.createVerticalStrut(26));

        for (String page : pages.keySet()) {
            NavButton b = new NavButton(page);
            b.addActionListener(e -> navigate(page));
            navButtons.put(page, b);
            sidebar.add(b);
            sidebar.add(Box.createVerticalStrut(6));
        }

        sidebar.add(Box.createVerticalGlue());

        JButton logout = new RoundButton("Logout", DANGER);
        logout.setMaximumSize(new Dimension(210, 42));
        logout.setAlignmentX(Component.CENTER_ALIGNMENT);
        logout.addActionListener(e -> logout());
        sidebar.add(logout);
        sidebar.add(Box.createVerticalStrut(24));
        return sidebar;
    }

    private void navigate(String name) {
        pages.get(name).refresh();
        cardLayout.show(contentPanel, name);
        navButtons.forEach((n, b) -> b.setActive(n.equals(name)));
    }

    private void logout() {
        if (confirm(this, "Do you want to log out?")) {
            store.save();
            dispose();
            new LoginFrame(store).setVisible(true);
        }
    }

    private void exitApp() {
        if (confirm(this, "Exit the application?")) {
            store.save();
            dispose();
            System.exit(0);
        }
    }

    // ============================== MYSQL DB HELPERS ==============================
    public static boolean saveUserToDatabase(String username, String role, String email) {
        String cleanUser = username.trim().toLowerCase();
        String sql = "INSERT INTO users (username, password, role, status, email, mobile) VALUES (?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE role = VALUES(role), email = VALUES(email)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cleanUser);
            pstmt.setString(2, "otp_user");
            pstmt.setString(3, role.toUpperCase());
            pstmt.setString(4, "Active");
            pstmt.setString(5, email);
            pstmt.setString(6, "9876543210");
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean checkUserExistsInDB(String username) {
        String cleanUser = username.trim().toLowerCase();
        String sql = "SELECT * FROM users WHERE LOWER(username) = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cleanUser);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteUserFromDatabase(String username) {
        String cleanUser = username.trim().toLowerCase();
        String sql = "DELETE FROM users WHERE LOWER(username) = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cleanUser);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean saveLectureToDB(String day, String slot, String className, String subjectName, String teacherName, String room) {
        String sql = "INSERT INTO schedule (subject, teacher, day, time_slot) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, subjectName);
            pstmt.setString(2, teacherName);
            pstmt.setString(3, day);
            pstmt.setString(4, slot);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void loadLecturesFromDB(DefaultTableModel model) {
        model.setRowCount(0);
        String sql = "SELECT * FROM schedule";
        try (Connection conn = DatabaseConnection.getConnection();
             java.sql.Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("day"),
                        rs.getString("time_slot"),
                        "CSE-C",
                        rs.getString("subject"),
                        rs.getString("teacher"),
                        "R101"
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean deleteLectureFromDB(String day, String slot, String subjectName, String teacherName) {
        String sql = "DELETE FROM schedule WHERE day = ? AND time_slot = ? AND subject = ? AND teacher = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, day);
            pstmt.setString(2, slot);
            pstmt.setString(3, subjectName);
            pstmt.setString(4, teacherName);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> {
            Main.DataStore store = DataStore.load();
            new Main.LoginFrame(store).setVisible(true);
        });
    }
}