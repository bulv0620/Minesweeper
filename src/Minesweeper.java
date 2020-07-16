import java.awt.*;
import java.awt.event.*;
import java.sql.*;

import javax.swing.*;

public class Minesweeper extends JFrame implements ActionListener, MouseListener {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    // 定义属性
    // 雷和旗子图标
    ImageIcon mineIcon = new ImageIcon("C:\\Users\\FENG\\Desktop\\code\\Minesweeper\\Mine.png");
    ImageIcon flagIcon = new ImageIcon("C:\\Users\\FENG\\Desktop\\code\\Minesweeper\\Flag.png");
    // 雷数
    int MINE = 0;
    int REMINE = 0;
    // 计时
    int countTime;
    // 定义按钮组
    JButton[][] jb;
    // 定义状态
    int[][] map;
    // 定义插旗子状态
    boolean[][] flag;
    boolean[][] flags;
    // 定义菜单栏
    JMenuBar bar = new JMenuBar();
    JMenu menu1 = new JMenu("选项");
    JMenu menu2 = new JMenu("难度:");
    JMenuItem newGame = new JMenuItem("新游戏");
    JMenuItem rank = new JMenuItem("最高分");
    JMenuItem exit = new JMenuItem("退出");
    JMenuItem easy = new JMenuItem("简单");
    JMenuItem mid = new JMenuItem("中等");
    JMenuItem hard = new JMenuItem("困难");
    JMenuItem diy = new JMenuItem("自定义");
    // 定义计时
    JLabel label1 = new JLabel("    用时:");
    JLabel time = new JLabel("0");
    JLabel label2 = new JLabel("s");
    // 剩余旗子
    JLabel label3 = new JLabel("    剩余:");
    JLabel remain = new JLabel("0");
    // 当前地图大小
    JLabel mapsize = new JLabel();

    // 数据库
    private final static String data = "jdbc:mysql://localhost:3306/minesweeper?&serverTimezone=Asia/Shanghai";
    private final static String dataName = "root";
    private final static String dataPass = "feng";
    private static Connection coon = null;
    private static PreparedStatement pst = null;
    private static ResultSet rs = null;

    // 主程序
    public Minesweeper(int NUM_x, int NUM_y, int mine) {
        super("扫雷");
        MINE = mine;
        REMINE = mine;
        remain.setText(String.valueOf(MINE));//剩余雷数
        mapsize.setText(NUM_x + "×" + NUM_y + " #" + MINE); // 地图大小、雷数
        // 按钮组
        jb = new JButton[NUM_x][NUM_y];
        // map
        map = new int[NUM_x][NUM_y];
        // flag
        flag = new boolean[NUM_x][NUM_y];
        flags = new boolean[NUM_x][NUM_y];
        // 布雷
        int count = 0;
        while (count < MINE) {
            int i = (int) (Math.random() * map.length);
            int j = (int) (Math.random() * map[0].length);
            if (map[i][j] != '*') {
                map[i][j] = '*';
                count++;

            }
        }
        // 面板组装
        JPanel panel = new JPanel(new GridLayout(NUM_x, NUM_y));
        for (int i = 0; i < NUM_x; i++) {
            for (int j = 0; j < NUM_y; j++) {
                jb[i][j] = new JButton();
                jb[i][j].setName(i + "_" + j);
                jb[i][j].setBackground(Color.white);
                panel.add(jb[i][j]);
                // 监听事件
                jb[i][j].addActionListener(this);
                // 右键监听
                jb[i][j].addMouseListener(this);
            }
        }
        // 菜单栏组装
        bar.add(menu1);
        bar.add(menu2);
        bar.add(mapsize);
        bar.add(label1);
        bar.add(time);
        bar.add(label2);
        bar.add(label3);
        bar.add(remain);
        setTimer(time);
        countTime = 0;
        // 子菜单组装
        menu1.add(newGame);
        menu1.add(rank);
        menu1.add(exit);
        menu2.add(easy);
        menu2.add(mid);
        menu2.add(hard);
        menu2.add(diy);
        // 组装整体
        getContentPane().add("Center", panel);
        getContentPane().add("North", bar);

        // 事件
        newGame.addActionListener(this);
        rank.addActionListener(this);
        exit.addActionListener(this);
        easy.addActionListener(this);
        mid.addActionListener(this);
        hard.addActionListener(this);
        diy.addActionListener(this);
        // 窗口部署
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(NUM_y * 43 + 5, NUM_x * 43 + 7);
        setLocationRelativeTo(null);
        setResizable(false);
        setIconImage(mineIcon.getImage());
        setVisible(true);
    }

    // 计时器监听
    private void setTimer(JLabel time) {
        final JLabel varTime = time;

        Timer timeAction = new Timer(1000, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                countTime++;
                varTime.setText("" + countTime);
            }
        });
        timeAction.start();
    }

    public static void main(String[] args) {
        new Minesweeper(8, 8, 10);
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            coon = DriverManager.getConnection(data, dataName, dataPass);
            System.out.println("数据库连接成功");
        } catch (Exception e) {
            System.out.println("数据库连接失败");
        }

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object sourse = e.getSource();
        if (sourse == easy) {
            setVisible(false);
            new Minesweeper(8, 8, 10);
        } else if (sourse == mid) {
            setVisible(false);
            new Minesweeper(13, 13, 25);
        } else if (sourse == hard) {
            setVisible(false);
            new Minesweeper(18, 18, 40);
        } else if (sourse == newGame) {
            setVisible(false);
            new Minesweeper(map.length, map[0].length, MINE);
        } else if (sourse == rank) {
            showRank();
        } else if (sourse == diy) {
            diy();
        } else if (sourse == exit) {
            System.exit(0);
        } else {
            int x, y;
            String[] strM = ((JButton) sourse).getName().split("_");
            x = Integer.parseInt(strM[0]);
            y = Integer.parseInt(strM[1]);
            checkClick(x, y);
            checkWin();
        }
    }

    // 清空排行榜
    private void cleanRank() {
        String sql = "update `rank` set time = 99999 , `name` = 'NULL'";
        System.out.println("-> " + sql);
        try {
            pst = coon.prepareStatement(sql);
            pst.execute();
            System.out.println("最高分已清空");
        } catch (SQLException e) {
            System.out.println("最高分清空失败");
        }
    }

    // 排行数据
    private void showRank() {
        // 数据库提取
        try {
            String sql = "select * from `rank`";
            System.out.println("-> " + sql);
            pst = coon.prepareStatement(sql);
            rs = pst.executeQuery();
            String[] mod = new String[3];
            String[] name = new String[3];
            Integer[] score = new Integer[3];
            for (int i = 0; i < 3; i++) {
                rs.next();
                mod[i] = rs.getString("mod");
                name[i] = rs.getString("name");
                score[i] = rs.getInt("time");
            }
            StringBuilder rankInfo = new StringBuilder("");
            for (int i = 0; i < 3; i++) {
                rankInfo.append("#" + mod[i] + "  用户:" + name[i] + "  用时:" + score[i] + "秒\n");
            }
            System.out.println("最高分读取成功");
            Object[] options = new Object[] { "确认", "清空数据" };
            int optionSelected = JOptionPane.showOptionDialog(this, rankInfo, "最高分", JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
            if (optionSelected == 1) {
                cleanRank();
                showRank();
            }
        } catch (Exception e) {
            System.out.println("发生错误");
        }
    }

    // 自定义地图
    private void diy() {
        String input = null;
        input = JOptionPane.showInputDialog(this, "输入高、宽、雷数:", "(#分隔)");
        if (input != null) {
            try {
                String[] param = input.split("#");
                Integer[] intParam = new Integer[3];
                for (int i = 0; i < 3; i++)
                    intParam[i] = Integer.parseInt(param[i]);
                if (intParam[0] * intParam[1] <= intParam[2]) {
                    JOptionPane.showMessageDialog(this, "请正确输入！", "警告", 0);
                    diy();
                } else if (intParam[0] > 20 || intParam[1] > 38) {
                    JOptionPane.showMessageDialog(this, "最大支持20×38！", "提示", 1);
                    diy();
                } else if (intParam[0] < 6 || intParam[1] < 6) {
                    JOptionPane.showMessageDialog(this, "需大于6×6！", "提示", 1);
                    diy();
                }

                else {
                    setVisible(false);
                    new Minesweeper(intParam[0], intParam[1], intParam[2]);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "请正确输入！", "警告", 0);
                diy();
            }
        }
    }

    // 点击实现
    private void checkClick(int x, int y) {
        // 判断方格是否插旗
        if (!flag[x][y]) {
            // 踩雷
            if (map[x][y] == '*') {
                jb[x][y].setIcon(mineIcon);
                showMines();// 显示所有雷
                gameOver();// 游戏结束
            } else {
                // 判断周围雷数
                int round = 0;
                for (int i = x - 1; i <= x + 1; i++) {
                    for (int j = y - 1; j <= y + 1; j++) {
                        if (i >= 0 && j >= 0 && i < map.length && j < map[0].length && map[i][j] == '*') {
                            round++;
                        }
                    }
                }
                flags[x][y] = true;
                // 显数
                jb[x][y].setBackground(Color.lightGray);
                if (round == 0) {
                    for (int i = x - 1; i <= x + 1; i++) {
                        for (int j = y - 1; j <= y + 1; j++) {
                            if (i >= 0 && j >= 0 && i < map.length && j < map[0].length && flags[i][j] == false) {
                                checkClick(i, j);
                            }
                        }
                    }
                } else if (round == 1) {
                    jb[x][y].setText(String.valueOf(round));
                    jb[x][y].setForeground(Color.BLACK);
                } else if (round == 2) {
                    jb[x][y].setText(String.valueOf(round));
                    jb[x][y].setForeground(Color.BLUE);
                } else if (round == 3) {
                    jb[x][y].setText(String.valueOf(round));
                    jb[x][y].setForeground(Color.green);
                } else if (round >= 4) {
                    jb[x][y].setText(String.valueOf(round));
                    jb[x][y].setForeground(Color.RED);
                }
            }
        }
    }

    // 显示所有雷
    private void showMines() {
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[0].length; j++) {
                if (map[i][j] == '*')
                    jb[i][j].setIcon(mineIcon);
            }
        }
    }

    // 游戏结束
    private void gameOver() {
        // 继续游戏询问
        int result = JOptionPane.showConfirmDialog(this, "是否开始新游戏？", "游戏结束", JOptionPane.YES_NO_OPTION);
        if (result == 1) {
            System.exit(0);
        } else {
            setVisible(false);
            new Minesweeper(map.length, map[0].length, MINE);
        }
    }

    // 游戏胜利
    private void checkWin() {
        int count = map.length * map[0].length;
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[0].length; j++) {
                if (flags[i][j] == true)
                    count--;
            }
        }
        if (count == MINE) {
            String s = time.getText();
            JOptionPane.showMessageDialog(this, "你的用时：" + s + "秒", "胜利", 1);
            // 数据库记录最高分
            try {
                String sql = "select * from `rank`";
                pst = coon.prepareStatement(sql);
                rs = pst.executeQuery();
                // 各个模式的最高分读取
                int[] rankTime = { 0, 0, 0 };
                for (int i = 0; i < 3; i++) {
                    rs.next();
                    rankTime[i] = rs.getInt("time");
                }
                // 模式判断、高分记录
                if (map.length == 8 && MINE == 10) {
                    if (Integer.parseInt(s) < rankTime[0]) {
                        JOptionPane.showMessageDialog(this, "新纪录！", "提示", 1);
                        String input = null;
                        input = JOptionPane.showInputDialog(this, "输入您的ID:", "");
                        sql = "UPDATE `rank` SET time = " + s + " , `name` = '" + input + "' WHERE `mod` = '1.简单'";
                        System.out.println("-> " + sql);
                        pst = coon.prepareStatement(sql);
                        pst.execute();
                        System.out.println("最高分写入成功");
                    }
                } else if (map.length == 13 && MINE == 25) {
                    if (Integer.parseInt(s) < rankTime[1]) {
                        JOptionPane.showMessageDialog(this, "新纪录！", "提示", 1);
                        String input = null;
                        input = JOptionPane.showInputDialog(this, "输入您的ID:", "");
                        sql = "UPDATE `rank` SET time = " + s + " , `name` = '" + input + "' WHERE `mod` = '2.中等'";
                        System.out.println("-> " + sql);
                        pst = coon.prepareStatement(sql);
                        pst.execute();
                        System.out.println("最高分写入成功");
                    }
                } else if (map.length == 18 && MINE == 40) {
                    if (Integer.parseInt(s) < rankTime[2]) {
                        JOptionPane.showMessageDialog(this, "新纪录！", "提示", 1);
                        String input = null;
                        input = JOptionPane.showInputDialog(this, "输入您的ID:", "");
                        sql = "UPDATE `rank` SET time = " + s + " , `name` = '" + input + "' WHERE `mod` = '3.困难'";
                        System.out.println("-> " + sql);
                        pst = coon.prepareStatement(sql);
                        pst.execute();
                        System.out.println("最高分写入成功");
                    }
                } else {
                }
            } catch (Exception e) {
                System.out.println("发生错误");
            }

            // 继续游戏询问
            int result = JOptionPane.showConfirmDialog(this, "是否开始新游戏？", "提示", JOptionPane.YES_NO_OPTION);
            if (result == 1) {
                System.exit(0);
            } else {
                setVisible(false);
                new Minesweeper(map.length, map[0].length, MINE);
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int c = e.getButton();
        Object sourse = e.getSource();
        if ((c == 1 && e.getClickCount() == 2) || c == 2) {// 双击或中键点击自动扩散
            int x, y;
            String[] strM = ((JButton) sourse).getName().split("_");
            x = Integer.parseInt(strM[0]);
            y = Integer.parseInt(strM[1]);
            if (map[x][y] != '*' && !flag[x][y] && !jb[x][y].getText().equals("")) {
                int count = 0;
                for (int i = x - 1; i <= x + 1; i++) {
                    for (int j = y - 1; j <= y + 1; j++) {
                        if (i >= 0 && j >= 0 && i < map.length && j < map[0].length && flag[i][j]) {
                            count++;
                        }
                    }
                }
                if (count >= Integer.parseInt(jb[x][y].getText())) {
                    for (int i = x - 1; i <= x + 1; i++)
                        for (int j = y - 1; j <= y + 1; j++)
                            if (i >= 0 && j >= 0 && i < map.length && j < map[0].length) {
                                checkClick(i, j);
                            }
                    checkWin();
                }
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        int c = e.getButton();
        Object sourse = e.getSource();
        if (c == 3) {
            int x, y;
            String[] strM = ((JButton) sourse).getName().split("_");
            x = Integer.parseInt(strM[0]);
            y = Integer.parseInt(strM[1]);
            if (flag[x][y] == false && flags[x][y] == false) {// 插旗子
                jb[x][y].setIcon(flagIcon);
                flag[x][y] = true;
                int t = Integer.parseInt(remain.getText());
                remain.setText(String.valueOf(--t));
            } else {
                if (flag[x][y]) {
                    int t = Integer.parseInt(remain.getText());
                    remain.setText(String.valueOf(++t));
                }
                jb[x][y].setIcon(null);
                flag[x][y] = false;
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub

    }
}
