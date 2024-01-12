import java.awt.Color;
import java.awt.Graphics;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.*;


class ViewTheme {
    public Color foreground;
    public Color background;
    public int cellSize;
    public static final int MAX = 3;
    
    public ViewTheme(Color fg, Color bg, boolean outline) {
        foreground = fg;
        background = bg;
     
        if (outline)
            cellSize = 8;
        else
            cellSize = 10;
    }
}

class Ruleset implements Serializable {
    private int[] countToInstantiate;
    private int[] countToLive;
    private String name;
    public static final int MAX = 5;
    
    Ruleset(String n, int[] instantiate, int[] live) {
        name = n;
        countToInstantiate = instantiate;
        countToLive = live;
    }
    
    // Вычеслить состояние клетки
    public int calculate(int state, int neighbors) {
        if (state == 0) {
            for (int i = 0; i < countToInstantiate.length; i++) {
                if (neighbors == countToInstantiate[i]) {
                    return 1;
                }
            }
        }
        else {
            for (int i = 0; i < countToLive.length; i++) {
                if (neighbors == countToLive[i]) {
                    return 1;
                }
            }
        }
        return 0;
    }
    
    public String GetName() { return name; }
}

class User implements Serializable {
    private String username;
    private String password;
    
    private Ruleset[] rulesets;
    
    public User(String n, String p) {
        username = n;
        password = p;
        
        rulesets = new Ruleset[Ruleset.MAX];
    }
    
    public void AddRuleset(Ruleset newRuleset) {
        for (int i = 0; i < Ruleset.MAX; i++) {
            if (rulesets[i] == null) {
                rulesets[i] = newRuleset;
                break;
            }
        }
    }
    
    public String GetUsername() { return username; }
    public Ruleset[] GetRulesets() { return rulesets; }
    public boolean CheckPassword(String value) { return password.equals(value); }
}


class GameOfLife extends JPanel {
    private int[][] grid;
    private int cols;
    private int rows;
    private int resolution = 10;
    private int width;
    private int height;
    private Random rnd;
    
    private Ruleset myRuleset;
    private ViewTheme myViewTheme;
    
    private boolean isPlaying;

    public GameOfLife(int w, int h) {
        myRuleset = new Ruleset("Conway", new int[]{3}, new int[]{2, 3});
        myViewTheme = new ViewTheme(new Color(0, 255, 0), new Color(0, 0, 0), true);
        
        width = w;
        height = h;

        cols = width / resolution;
        rows = height / resolution;

        rnd = new Random();

        grid = new int[cols][rows];
        
        isPlaying = true;
    }
    
    // Обновление сетки
    public void updateGrid() {
        // Сетка не обновляется при Паузе и отсутствия правил
        if (!isPlaying && myRuleset != null) return;

        int[][] next = new int[cols][rows];

        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                next[i][j] = myRuleset.calculate(grid[i][j], countNeighbors(i, j));
            }
        }

        grid = next.clone();
    }

    private int countNeighbors(int x, int y) {
        int sum = 0;
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                sum += grid[(x + i + cols) % cols][(y + j + rows) % rows];
            }
        }
        sum -= grid[x][y];
        return sum;
    }
    
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(myViewTheme.background);

        g.fillRect(0, 0, width, height);

        g.setColor(myViewTheme.foreground);

        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                if (grid[i][j] == 1) {
                    g.fillRect(i * resolution, j * resolution, myViewTheme.cellSize, myViewTheme.cellSize);
                }
            }
        }
    }
    
    // Заполнить сетку случайно
    private void randomGrid(double random) {
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j ++) {
                if (rnd.nextDouble() < random)
                    grid[i][j] = 1;
                else
                    grid[i][j] = 0;
            }
        }
    }
    
    // Заполнить сетку одним значением
    private void setGrid(int value) {
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j ++) {
                grid[i][j] = value;
            }
        }
    }
    
    public void SetTheme(ViewTheme theme) {  myViewTheme = theme; }
    public void SetRule(Ruleset ruleset) { myRuleset = ruleset; }
    
    public void Play() { isPlaying = true; }
    public void Pause() { isPlaying = false; }
    
    public void GenerateGrid(double random) {
        if (random == 0) setGrid(0);
        else if (random == 1) setGrid(1);
        else randomGrid(random);
    }
    
}

// ================== _ M Y _ M A I N _ ==================

public class MyMain extends JFrame implements ActionListener {
    private static GameOfLife myGOL;
    
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    
    private static int currentUserID = -1;
    private static User[] allUsers;
    private static ViewTheme[] viewThemes;
    
    // Элементы интерфейса ТопБара
    JMenuBar menuBar;
    JMenu loginMenu;
    JMenu gameMenu;
    JMenu resetMenu;
    JMenu rulesetMenu;
    JMenu themeMenu;
    JMenuItem loginoutItem;
    JMenuItem createAccItem;
    JMenuItem playItem;
    JMenuItem pauseItem;
    JMenuItem random0Item;
    JMenuItem random10Item;
    JMenuItem random50Item;
    JMenuItem random90Item;
    JMenuItem random100Item;
    JMenuItem[] rulesetItems;
    JMenuItem newRulesetItem;
    JMenuItem[] themeItems;
    
    MyMain() {
        initComponents();
        
        new Timer(100, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                myGOL.updateGrid();
                myGOL.repaint();
            }
        }).start();
    }
    
    private void initComponents() {
        // INIT
        viewThemes = new ViewTheme[3];
        viewThemes[0] = new ViewTheme(Color.GREEN, Color.BLACK, true);
        viewThemes[1] = new ViewTheme(Color.BLACK, Color.WHITE, true);
        viewThemes[2] = new ViewTheme(Color.RED, Color.WHITE, false);
        
        myGOL = new GameOfLife(800, 600);
        myGOL.SetTheme(viewThemes[2]);
        
        allUsers = new User[10];
        
        // Загружаем данные с файла
        try {
            doLoadData();
        } catch (IOException ex) {
            Logger.getLogger(MyMain.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(MyMain.class.getName()).log(Level.SEVERE, null, ex);
        }

        
        // SWING
        this.getContentPane().add(myGOL);
        
        menuBar = new JMenuBar();
        
        loginMenu = new JMenu("Login");
        gameMenu = new JMenu("Game");
        resetMenu = new JMenu("Reset");
        rulesetMenu = new JMenu("Ruleset");
        themeMenu = new JMenu("Theme");

        loginoutItem = new JMenuItem("Loginout");
        createAccItem = new JMenuItem("Create Account");
        
        playItem = new JMenuItem("Play");
        pauseItem = new JMenuItem("Pause");
        
        random0Item = new JMenuItem("Random 0%");
        random10Item = new JMenuItem("Random 10%");
        random50Item = new JMenuItem("Random 50%");
        random90Item = new JMenuItem("Random 90%");
        random100Item = new JMenuItem("Random 100%");
        
        rulesetItems = new JMenuItem[Ruleset.MAX];
        for (int i = 0; i < Ruleset.MAX; i++) rulesetItems[i] = new JMenuItem();
        newRulesetItem = new JMenuItem("New Ruleset");
        
        themeItems = new JMenuItem[ViewTheme.MAX];
        themeItems[0] = new JMenuItem("Hacker");
        themeItems[1] = new JMenuItem("Classic");
        themeItems[2] = new JMenuItem("Red");
        
        // Функционал
        loginoutItem.addActionListener(this);
        createAccItem.addActionListener(this);
        playItem.addActionListener(this);
        pauseItem.addActionListener(this);
        random0Item.addActionListener(this);
        random10Item.addActionListener(this);
        random50Item.addActionListener(this);
        random90Item.addActionListener(this);
        random100Item.addActionListener(this);
        for (int i = 0; i < Ruleset.MAX; i++) rulesetItems[i].addActionListener(this);
        newRulesetItem.addActionListener(this);
        for (int i = 0; i < ViewTheme.MAX; i++) themeItems[i].addActionListener(this);
        
        // Иерархия
        menuBar.add(loginMenu);
        menuBar.add(gameMenu);
        menuBar.add(resetMenu);
        menuBar.add(rulesetMenu);
        menuBar.add(themeMenu);
        
        loginMenu.add(loginoutItem);
        loginMenu.add(createAccItem);
        
        gameMenu.add(playItem);
        gameMenu.add(pauseItem);
        
        resetMenu.add(random0Item);
        resetMenu.add(random10Item);
        resetMenu.add(random50Item);
        resetMenu.add(random90Item);
        resetMenu.add(random100Item);
        
        for (int i = 0; i < Ruleset.MAX; i++) rulesetMenu.add(rulesetItems[i]);
        rulesetMenu.addSeparator();
        rulesetMenu.add(newRulesetItem);
        
        for (int i = 0; i < ViewTheme.MAX; i++) themeMenu.add(themeItems[i]);
        

        this.setJMenuBar(menuBar);
        this.pack();
        
        // SETTINGS
        this.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle("Game of Life");
        this.setResizable(false);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
        
        // Добавляем сохранение данных перед выходом
        this.addWindowListener(new WindowAdapter(){
                public void windowClosing(WindowEvent e){
                    try {
                        doSaveData();
                    } catch (IOException ex) {
                        Logger.getLogger(MyMain.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    System.exit(0);
                }
            });
    }
    
    // Функционал
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == loginoutItem) {
            if (currentUserID == -1)
                doLogin();
            else
                doLogout();
        }
        if (e.getSource() == createAccItem) {
            doCreateAccount();
        }
        
        if (e.getSource() == playItem) { myGOL.Play(); }
        if (e.getSource() == pauseItem) { myGOL.Pause(); }
        
        if (e.getSource() == random0Item) { myGOL.GenerateGrid(0.0); }
        if (e.getSource() == random10Item) { myGOL.GenerateGrid(0.1); }
        if (e.getSource() == random50Item) { myGOL.GenerateGrid(0.5); }
        if (e.getSource() == random90Item) { myGOL.GenerateGrid(0.9); }
        if (e.getSource() == random100Item) { myGOL.GenerateGrid(1.0); }
        
        if (currentUserID != -1) {
            for (int i = 0; i < Ruleset.MAX; i++)
                if (allUsers[currentUserID].GetRulesets()[i] != null && e.getSource() == rulesetItems[i]) {
                    myGOL.SetRule(allUsers[currentUserID].GetRulesets()[i]);
                }
            if (e.getSource() == newRulesetItem) { doNewRuleset(); }
        }
        
        for (int i = 0; i < ViewTheme.MAX; i++)
            if (e.getSource() == themeItems[i]) { myGOL.SetTheme(viewThemes[i]); }
    }
    
    // Создать новое правило
    private void doNewRuleset() {
        String countInstantiate = JOptionPane.showInputDialog(null, "Cells Instantiate on Count:", "3");
        String countLive = JOptionPane.showInputDialog(null, "Cells Live on Count:", "23");
        
        // Ошибка
        if (countInstantiate.length() < 0 || countInstantiate.length() > 10 || countLive.length() < 0 || countLive.length() > 10) {
            JOptionPane.showMessageDialog(null, "Too much infromation", "Message", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Записать первый ряд правил
        int[] arCountInstnantiate = new int[countInstantiate.length()];
        for (int i = 0; i < countInstantiate.length(); i++) {
            arCountInstnantiate[i] = Integer.parseInt(String.valueOf(countInstantiate.charAt(i)));
        }
        
        // Записать второй ряд правил
        int[] arCountLive = new int[countLive.length()];
        for (int i = 0; i < countLive.length(); i++) {
            arCountLive[i] = Integer.parseInt(String.valueOf(countLive.charAt(i)));
        }
        
        String name = JOptionPane.showInputDialog(null, "Name:");
        
        // Сохранение
        allUsers[currentUserID].AddRuleset(new Ruleset(name, arCountInstnantiate, arCountLive));
        JOptionPane.showMessageDialog(null, "Successfully added Ruleset", "Message", JOptionPane.INFORMATION_MESSAGE);
        doUpdateRulesets();
    }
    
    // Действия при Логине
    private void doLogin() {
        String username = JOptionPane.showInputDialog(null, "Username:", "Username");
        String password = JOptionPane.showInputDialog(null, "Password:");
        
        for (int i = 0; i < allUsers.length; i++) {
            if (allUsers[i] != null && allUsers[i].GetUsername().equals(username)) {
                if (allUsers[i].CheckPassword(password)) {
                    JOptionPane.showMessageDialog(null, "Successfully Login", "Message", JOptionPane.INFORMATION_MESSAGE);
                    currentUserID = i;
                    loginMenu.setText(allUsers[i].GetUsername());
                    loginoutItem.setText("Logout");
                    doUpdateRulesets();
                    return; // Выходим
                }
                break;
            }
        }
        // Если не вышли из цикла - результат Ошибка
        JOptionPane.showMessageDialog(null, "Login Failed", "Message", JOptionPane.INFORMATION_MESSAGE);
    }
    
    // Действия при Логауте
    private void doLogout() {
        int answer = JOptionPane.showConfirmDialog(null, "You are sure you want to Logout?", "Message", JOptionPane.YES_NO_OPTION);
        if (answer == 0) { // Ответ "Да"
            JOptionPane.showMessageDialog(null, "Successfully Logout", "Message", JOptionPane.INFORMATION_MESSAGE);
            loginMenu.setText("Login");
            loginoutItem.setText("Login");
            currentUserID = -1;
            doUpdateRulesets();
        }
        
    }
    
    // Действия при создании аккаунта
    private void doCreateAccount() {
        String username = JOptionPane.showInputDialog(null, "Username:", "Username");
        String password = JOptionPane.showInputDialog(null, "Password:");

        if (password.equals(JOptionPane.showInputDialog(null, "Confirm password:"))) {
            for (int i = 0; i < allUsers.length; i++) {
                if (allUsers[i] == null) {
                    allUsers[i] = new User(username, password);
                    currentUserID = i;
                    loginMenu.setText(allUsers[i].GetUsername());
                    loginoutItem.setText("Logout");
                    JOptionPane.showMessageDialog(null, "Successfully created Account", "Message", JOptionPane.INFORMATION_MESSAGE);
                    doUpdateRulesets();
                    return; // Выходим без ошибок
                }
            }
            JOptionPane.showMessageDialog(null, "Users full", "Message", JOptionPane.ERROR_MESSAGE);  
        }
        else {
            JOptionPane.showMessageDialog(null, "Incorrect password", "Message", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Обновление списка правил
    private void doUpdateRulesets() {
        if (currentUserID == -1) {
            for (int i = 0; i < Ruleset.MAX; i++) {
                rulesetItems[i].setText("");
            }
        }
        else {
            Ruleset[] rule = allUsers[currentUserID].GetRulesets();
            for (int i = 0; i < Ruleset.MAX; i++) {
                if (rule[i] != null)
                    rulesetItems[i].setText(rule[i].GetName());
                else
                    rulesetItems[i].setText("");
            }
        }
    }
    
    // Сохранение данных
    private void doLoadData() throws FileNotFoundException, IOException, ClassNotFoundException {
        ObjectInputStream in = new ObjectInputStream(new FileInputStream("users.txt"));
        allUsers = (User[]) in.readObject();
        for (int i = 0; i < allUsers.length; i++) {
            if (allUsers[i] != null)
                System.out.println(allUsers[i].GetUsername());
        }
    }
    
    // Загрузка данных
    private void doSaveData() throws FileNotFoundException, IOException {
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("users.txt"));
        out.writeObject(allUsers);
    }
    
    public static void main(String[] args) {
        MyMain myMain = new MyMain();
    }
}

