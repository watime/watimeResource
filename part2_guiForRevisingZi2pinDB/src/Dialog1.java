import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Dialog1 extends JDialog {
	private JPanel contentPane;
	private JButton buttonOK;
	private JButton buttonCommit;
	private JTextField charaTextField;
	private JTextField p1TextField;
	private JTextField p2TextField;
	private JTextField p3TextField;
	private JTextField p4TextField;
	private JTextField commentTextField;
	private JTextField textField7;
	private JTextField textField8;
	private JTextField textField9;
	private JTextField textField10;
	private JTextField textField11;
	private JTextField countTextField;
	private JButton buttonGet;
	private JButton insertButton;

	static Connection connection;
	static Statement statement;

	public Dialog1() {
		setContentPane(contentPane);
		setModal(true);
		getRootPane().setDefaultButton(buttonOK);

		SQLiteConfig config = new SQLiteConfig();
		SQLiteDataSource dataSource = new SQLiteDataSource(config);
		dataSource.setUrl("jdbc:sqlite:zi2pin20130806.db3");

		try {
			connection = dataSource.getConnection();
			statement = connection.createStatement();
		} catch (Exception e) {
		}

		buttonCommit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				try {
					commit();
				} catch (Exception e) {
					e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
				}
			}
		});
		buttonGet.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				getChara();
			}
		});
//		insertButton.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent actionEvent) {
//				insert();
//			}
//		});
	}

	void getChara() {
		charaTextField.setText(charaTextField.getText().trim());
		String chara = charaTextField.getText();
		p1TextField.setText("");
		p2TextField.setText("");
		p3TextField.setText("");
		p4TextField.setText("");
		textField7.setText("");
		textField8.setText("");
		textField9.setText("");
		textField10.setText("");
		textField11.setText("");
		commentTextField.setText("");
//		countTextField.setText("");
		if (chara.isEmpty()) return;
		try {
			ResultSet rs = statement.executeQuery("select * from z where zi like '" + chara + "'");
			if (!rs.next()) {
				p1TextField.setText("nothing found");
				rs.close();
				return;
			}
			String s[] = {rs.getString(1), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7)};
			if (s[1] != null) {
				p1TextField.setText(s[1]);
				textField7.setText(s[1]);
			}
			if (s[2] != null) {
				p2TextField.setText(s[2]);
				textField8.setText(s[2]);
			}
			if (s[3] != null) {
				p3TextField.setText(s[3]);
				textField9.setText(s[3]);
			}
			if (s[4] != null) {
				p4TextField.setText(s[4]);
				textField10.setText(s[4]);
			}
			if (s[5] != null) {
				commentTextField.setText(s[5]);
				textField11.setText(s[5]);
			}
			countTextField.setText(String.valueOf(rs.getInt(2)));
			if (!s[0].equals(chara)) countTextField.setText("warning: chara and zi not matched");
			else if (rs.next()) countTextField.setText("warning: duplicate");

			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	void commit() throws Exception {
		System.out.println("commit");
		if (countTextField.getText().isEmpty()) {
			countTextField.setText("CAUTION: ENTER COUNT");
			return;
		}
		if (!p1TextField.getText().equals("nothing found")) {
			statement.executeUpdate("delete from z where zi like '" + charaTextField.getText() + "';");
		}
		statement.executeUpdate("insert into z(zi) values('" + charaTextField.getText() + "');");

		if (!textField7.getText().isEmpty())
			statement.executeUpdate("update z set p1 = '" + textField7.getText() + "' where zi like '" +
					charaTextField.getText() + "';");
		if (!textField8.getText().isEmpty())
			statement.executeUpdate("update z set p2 = '" + textField8.getText() + "' where zi like '" +
					charaTextField.getText() + "';");
		if (!textField9.getText().isEmpty())
			statement.executeUpdate("update z set p3 = '" + textField9.getText() + "' where zi like '" +
					charaTextField.getText() + "';");
		if (!textField10.getText().isEmpty())
			statement.executeUpdate("update z set p4 = '" + textField10.getText() + "' where zi like '" +
					charaTextField.getText() + "';");
		if (!textField11.getText().isEmpty())
			statement.executeUpdate("update z set spare = '" + textField11.getText() + "' where zi like '" +
					charaTextField.getText() + "';");
		if (!countTextField.getText().isEmpty())
			statement.executeUpdate("update z set count = " + countTextField.getText() + " where zi like '" +
					charaTextField.getText() + "';");
	}

//	void insert() {
//		System.out.println("insert");
//	}

	public static void main(String[] args) {
		Dialog1 dialog = new Dialog1();
		dialog.pack();
		dialog.setVisible(true);
		try {
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
}
