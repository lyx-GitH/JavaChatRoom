package ChatGui;

import ChatSchema.User;
import ChatUtils.TinyJson;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class SignUpFrame extends ChatFrame {
    private JPasswordField passwordField;
    private JPasswordField doubleCheckField;

    private void init() {
        frame = new JFrame("用户注册");
        setDefaultProperties();

        Label label = new Label("    账号：");
        label.setAlignment(Label.RIGHT);
        label.setBounds(116, 49, 100, 23);
        frame.getContentPane().add(label);

        Label label_1 = new Label("    密码：");
        label_1.setAlignment(Label.RIGHT);
        label_1.setBounds(116, 85, 100, 23);
        frame.getContentPane().add(label_1);

        Label label_2 = new Label("再次输入密码：");
        label_2.setAlignment(Label.RIGHT);
        label_2.setBounds(116, 121, 100, 23);
        frame.getContentPane().add(label_2);

        JFormattedTextField formattedTextField = new JFormattedTextField();
        formattedTextField.setBounds(252, 49, 166, 23);
        frame.getContentPane().add(formattedTextField);

        passwordField = new JPasswordField();
        passwordField.setBounds(252, 85, 166, 23);
        frame.getContentPane().add(passwordField);

        doubleCheckField = new JPasswordField();
        doubleCheckField.setBounds(252, 121, 166, 23);
        frame.getContentPane().add(doubleCheckField);

        JButton signUpButton = new JButton("注册");
        signUpButton.setBackground(new Color(255, 255, 255));
        signUpButton.setBounds(126, 160, 212, 23);
        frame.getContentPane().add(signUpButton);

        signUpButton.addActionListener((ActionEvent e) -> {
            String username = formattedTextField.getText();
            String password = String.valueOf(passwordField.getPassword());
            String doubleCheck = String.valueOf(doubleCheckField.getPassword());
            var checkRes = checkValidInput(username, password, doubleCheck);
            if (!(boolean) checkRes.getPrimitive("checkedResult")) {
                JOptionPane.showMessageDialog(null, "注册失败!\n" + checkRes.getPrimitive("message"), "消息", JOptionPane.PLAIN_MESSAGE);
                return;
            }

            var isSignedJson = guiManager.isSignUpSucceed(username, password);
            boolean isSigned = (boolean) isSignedJson.getPrimitive("loginResult");

            if (!isSigned) {
                JOptionPane.showMessageDialog(null, "注册失败!，账号重复", "消息", JOptionPane.PLAIN_MESSAGE);
                formattedTextField.setText("");
                passwordField.setText("");
                doubleCheckField.setText("");
            } else {
                JOptionPane.showMessageDialog(null, "注册成功!，请登录", "消息", JOptionPane.PLAIN_MESSAGE);
                ChatFrame loginFrame = new LoginFrame();
                loginFrame.setGuiManager(guiManager);
                guiManager.changeFrame(loginFrame);
            }
        });

    }

    SignUpFrame() {
        init();
    }

    TinyJson checkValidInput(String username, String password, String doubleCheck) {
        var checkedResult = new TinyJson(new String[]{"checkedResult", "message"});
        if (username.isEmpty() || password.isEmpty() || doubleCheck.isEmpty()) {
            return checkedResult.put("checkedResult", false).put("message", "用户名和密码均不可为空");
        }

        if (username.length() >= User.MAX_USERNAME_LEN || password.length() >= User.MAX_USERNAME_LEN) {
            return checkedResult.put("checkedResult", false).put("message", "用户名或密码不可超过" + User.MAX_USERNAME_LEN + "个字符");
        }

        if (!password.equals(doubleCheck)) {
            return checkedResult.put("checkedResult", false).put("message", "输入的密码不相同");
        }
        return checkedResult.put("checkedResult", true);
    }

    public static void main(String[] args) {
        var frame = new SignUpFrame();
        frame.show();
    }
}
