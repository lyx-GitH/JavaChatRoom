package ChatGui;

import ChatUtils.TinyJson;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginFrame extends ChatFrame {


    private JPasswordField passwordField;
    private boolean isLogin = false;

    /**
     * Create the application.
     */
    public LoginFrame() {
        initialize();
    }

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        var loginFrame = new LoginFrame();
        loginFrame.show();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {


        frame = new JFrame("用户登录");
        setDefaultProperties();

        Label label = new Label("账号：");
        label.setAlignment(Label.CENTER);
        label.setBounds(116, 49, 50, 23);
        frame.getContentPane().add(label);

        Label label_1 = new Label("密码：");
        label_1.setAlignment(Label.CENTER);
        label_1.setBounds(116, 85, 50, 23);
        frame.getContentPane().add(label_1);

        Label label_2 = new Label("用户状态：");
        label_2.setBounds(433, 49, 60, 23);
        frame.getContentPane().add(label_2);

        Label label_3 = new Label("未登录");
        label_3.setForeground(new Color(255, 0, 0));
        label_3.setBounds(499, 49, 40, 23);
        frame.getContentPane().add(label_3);

        JFormattedTextField formattedTextField = new JFormattedTextField();
        formattedTextField.setBounds(172, 49, 166, 23);
        frame.getContentPane().add(formattedTextField);

        passwordField = new JPasswordField();
        passwordField.setBounds(172, 85, 166, 23);
        frame.getContentPane().add(passwordField);

        JButton button = new JButton("登录");
        button.setBackground(new Color(255, 255, 255));
        button.setBounds(126, 121, 212, 23);
        frame.getContentPane().add(button);

        JButton exitButton = new JButton("返回");
        exitButton.setBackground(new Color(255, 255, 255));
        exitButton.setBounds(126, 160, 212, 23);
        frame.getContentPane().add(exitButton);

        button.addActionListener(
                (ActionEvent e) -> {

                    String getUserName = formattedTextField.getText();
                    String getUserPwd = String.valueOf(passwordField.getPassword());
                    TinyJson confirmLogJ;
                    try {
                        confirmLogJ = guiManager.isLogInSucceed(getUserName, getUserPwd);
                    } catch (NumberFormatException exp) {
                        JOptionPane.showMessageDialog(null, "登录失败!，账号必须是数字", "消息", JOptionPane.WARNING_MESSAGE);
                        label_3.setText("未登录");
                        label_3.setForeground(Color.RED);
                        passwordField.setText("");
                        return;
                    }
                    isLogin = confirmLogJ != null && (boolean) confirmLogJ.getPrimitive("loginResult");
                    String reason = confirmLogJ == null ? "账号密码不能为空" : (String) confirmLogJ.getPrimitive("reason");
                    if (isLogin) {
                        JOptionPane.showMessageDialog(null, "登录成功!", "消息", JOptionPane.PLAIN_MESSAGE);
                        label_3.setText("已登录");
                        label_3.setForeground(Color.BLUE);
                        ChatFrame newFrame = new MainChatFrame();
                        newFrame.getFrame().setTitle(guiManager.getSocketClient().getCurrentUser().username + "的JaChat聊天室");
                        newFrame.setGuiManager(guiManager);

                        guiManager.changeFrame(newFrame);
                    } else {
                        JOptionPane.showMessageDialog(null, "登录失败!，" + reason, "消息", JOptionPane.WARNING_MESSAGE);
                        label_3.setText("未登录");
                        label_3.setForeground(Color.RED);
                        passwordField.setText("");

                    }
                }
        );

        exitButton.addActionListener(e -> {
            ChatFrame welcomeFrame = new WelcomeFrame();
            welcomeFrame.setGuiManager(guiManager);
            guiManager.changeFrame(welcomeFrame);
        });
    }
}