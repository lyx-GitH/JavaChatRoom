package ChatGui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class WelcomeFrame extends ChatFrame {
    private void init() {
        this.frame = new JFrame("Java 聊天室");
        setDefaultProperties();

        Label label = new Label("请登录或注册：");
        label.setAlignment(Label.CENTER);
        label.setBounds(116, 49, 100, 23);
        frame.getContentPane().add(label);

        JButton loginButton = new JButton("登录账号");
        loginButton.setBackground(new Color(255, 255, 255));
        loginButton.setBounds(126, 121, 212, 23);
        frame.getContentPane().add(loginButton);

        JButton signupButton = new JButton("注册账号");
        signupButton.setBackground(new Color(255, 255, 255));
        signupButton.setBounds(126, 160, 212, 23);
        frame.getContentPane().add(signupButton);

        loginButton.addActionListener((ActionEvent e) -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setGuiManager(this.guiManager);
            guiManager.changeFrame(loginFrame);
        });

        signupButton.addActionListener((ActionEvent e) -> {
            SignUpFrame signUpFrame = new SignUpFrame();
            signUpFrame.setGuiManager(guiManager);
            guiManager.changeFrame(signUpFrame);
        });
    }

    WelcomeFrame() {
        init();
    }

    public static void main(String[] args) {
        var frame = new WelcomeFrame();
        frame.init();
        frame.show();
    }
}
