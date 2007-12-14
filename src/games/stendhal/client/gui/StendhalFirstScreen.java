/* $Id$ */
/***************************************************************************
 *                      (C) Copyright 2003 - Marauroa                      *
 ***************************************************************************
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package games.stendhal.client.gui;

import games.stendhal.client.StendhalClient;
import games.stendhal.client.stendhal;
import games.stendhal.client.sprite.SpriteStore;
import games.stendhal.client.update.ClientGameConfiguration;
import games.stendhal.client.update.HttpClient;
import games.stendhal.client.update.Version;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * Summary description for LoginGUI
 * 
 */
public class StendhalFirstScreen extends JFrame {

	private static final long serialVersionUID = -7825572598938892220L;

	private StendhalClient client;

	private Image background;

	/**
	 * Creates the first screen
	 * 
	 * @param client
	 *            StendhalClient
	 */
	public StendhalFirstScreen(StendhalClient client) {
		super();
		this.client = client;

		URL url = SpriteStore.get().getResourceURL(
				ClientGameConfiguration.get("GAME_SPLASH_BACKGROUND"));
		ImageIcon imageIcon = new ImageIcon(url);
		background = imageIcon.getImage();

		initializeComponent();

		this.setVisible(true);
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Windows Form Designer. Otherwise, retrieving design
	 * might not work properly. Tip: If you must revise this method, please
	 * backup this GUI file for JFrameBuilder to retrieve your design properly
	 * in future, before revising this method.
	 */
	private void initializeComponent() {
		this.setContentPane(new JPanel() {

			private static final long serialVersionUID = -4272347652159225492L;

			{
				setOpaque(false);
				this.setPreferredSize(new Dimension(640, 480));
			}

			@Override
			public void paint(Graphics g) {
				g.drawImage(background, 0, 0, this);
				super.paint(g);
			}
		});

		//
		// loginButton
		//
		JButton loginButton = new JButton();
		loginButton.setText("Login to "
				+ ClientGameConfiguration.get("GAME_NAME"));
		loginButton.setMnemonic(KeyEvent.VK_L);
		loginButton.setToolTipText("Press this button to login to a "
				+ ClientGameConfiguration.get("GAME_NAME") + " server");
		loginButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				login();
			}
		});
		//
		// createAccountButton
		//
		JButton createAccountButton = new JButton();
		createAccountButton.setText("Create an account");
		createAccountButton.setMnemonic(KeyEvent.VK_A);
		createAccountButton.setToolTipText("Press this button to create an account on a "
				+ ClientGameConfiguration.get("GAME_NAME") + " server.");
		createAccountButton.setEnabled(true);
		createAccountButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				createAccount();
			}
		});
		//
		// creaditButton
		//
		JButton helpButton = new JButton();
		helpButton.setText("Help");
		helpButton.setMnemonic(KeyEvent.VK_H);
		helpButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				showHelp();
			}
		});
		//
		// creaditButton
		//
		JButton creditButton = new JButton();
		creditButton.setText("Credits");
		creditButton.setMnemonic(KeyEvent.VK_C);
		creditButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				showCredits();
			}
		});

		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});

		//
		// contentPane
		//
		Container contentPane = this.getContentPane();
		contentPane.setLayout(null);

		addComponent(contentPane, loginButton, 220, 300, 200, 32);
		addComponent(contentPane, createAccountButton, 220, 340, 200, 32);
		addComponent(contentPane, helpButton, 220, 380, 200, 32);
		addComponent(contentPane, creditButton, 220, 420, 200, 32);

		getRootPane().setDefaultButton(loginButton);

		//
		// LoginGUI
		//
		setTitle(ClientGameConfiguration.get("GAME_NAME") + " "
				+ stendhal.VERSION
				+ " - a multiplayer online game using Arianne");
		this.setLocation(new Point(100, 100));
		this.setResizable(false);

		URL url = SpriteStore.get().getResourceURL(
				ClientGameConfiguration.get("GAME_ICON"));
		this.setIconImage(new ImageIcon(url).getImage());
		pack();
	}

	private void login() {
		checkVersion();
		new LoginDialog(StendhalFirstScreen.this, client);
	}

	private void showCredits() {
		new CreditsDialog(StendhalFirstScreen.this);
	}

	private void showHelp() {
		new HelpDialog().display();
	}

	private void checkVersion() {
		HttpClient httpClient = new HttpClient(
				ClientGameConfiguration.get("UPDATE_VERSION_CHECK"));
		String version = httpClient.fetchFirstLine();
		if (version != null) {
			if (Version.compare(version, stendhal.VERSION) > 0) {
				// custom title, warning icon
				JOptionPane.showMessageDialog(
						null,
						"Your client is out of date. Latest version is "
								+ version
								+ ". But you are using "
								+ stendhal.VERSION
								+ ".\nDownload from http://arianne.sourceforge.net",
						"Client out of date", JOptionPane.WARNING_MESSAGE);
			}
		}

	}

	/**
	 * opens the create account dialog after checking the server version
	 */
	public void createAccount() {
		checkVersion();
		new CreateAccountDialog(StendhalFirstScreen.this, client);
	}

	/** Add Component Without a Layout Manager (Absolute Positioning) */
	private void addComponent(Container container, Component c, int x, int y,
			int width, int height) {
		c.setBounds(x, y, width, height);
		container.add(c);
	}

}
