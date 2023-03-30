package ChatGui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public abstract class ChatFrame {

    JFrame frame;

    GuiManager guiManager;

    void show() {
        this.frame.setVisible(true);
    }

    void close() {
        this.frame.setVisible(false);
        this.frame.dispose();
    }

    void setGuiManager(GuiManager guiManager) {
        this.guiManager = guiManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public JFrame getFrame() {
        return frame;
    }

    void setDefaultProperties() {
        frame.setBounds(100, 100, 900, 600);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (guiManager == null)
                    System.exit(0);
                int flag = JOptionPane.showConfirmDialog(frame, "确认退出吗？", "提示", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                if (JOptionPane.YES_OPTION == flag) {
                    guiManager.getSocketClient().shutdown();
                    close();
                } else {
                    return;
                }
            }
        });
        //获取本机屏幕横向分辨率
        int w = Toolkit.getDefaultToolkit().getScreenSize().width;
        //获取本机屏幕纵向分辨率
        int h = Toolkit.getDefaultToolkit().getScreenSize().height;
        frame.setLocation((w - frame.WIDTH) / 2, (h - frame.HEIGHT) / 2);
        frame.getContentPane().setLayout(null);
    }

}
