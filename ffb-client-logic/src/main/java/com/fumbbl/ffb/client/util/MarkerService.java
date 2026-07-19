package com.fumbbl.ffb.client.util;

import com.fumbbl.ffb.ClientMode;
import com.fumbbl.ffb.CommonProperty;
import com.fumbbl.ffb.FieldCoordinate;
import com.fumbbl.ffb.IClientPropertyValue;
import com.fumbbl.ffb.client.DimensionProvider;
import com.fumbbl.ffb.client.FantasyFootballClient;
import com.fumbbl.ffb.client.LayoutSettings;
import com.fumbbl.ffb.client.UserInterface;
import com.fumbbl.ffb.client.ui.swing.JComboBox;
import com.fumbbl.ffb.client.ui.swing.JLabel;
import com.fumbbl.ffb.client.ui.swing.JTextField;
import com.fumbbl.ffb.marking.FieldMarker;
import com.fumbbl.ffb.marking.PlayerMarker;
import com.fumbbl.ffb.marking.TransientPlayerMarker;
import com.fumbbl.ffb.model.Game;
import com.fumbbl.ffb.model.Player;
import com.fumbbl.ffb.util.StringTool;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


public class MarkerService {

	private TransientPlayerMarker.Mode defaultMode = TransientPlayerMarker.Mode.APPEND;

	public void showMarkerPopup(final FantasyFootballClient pClient, Component source, final Player<?> pPlayer, int pX, int pY) {
		if (pPlayer != null) {
			boolean persistMarker = ClientMode.PLAYER == pClient.getMode() && !IClientPropertyValue.AUTO_MARKING.contains(pClient.getProperty(CommonProperty.SETTING_PLAYER_MARKING_TYPE));
			PlayerMarker playerMarker = persistMarker ? pClient.getGame().getFieldModel().getPlayerMarker(pPlayer.getId()) : pClient.getGame().getFieldModel().getTransientPlayerMarker(pPlayer.getId());
			String markerText = (playerMarker != null) ? playerMarker.getHomeText() : null;
			final JTextField markerField = createMarkerPopup(pClient.getUserInterface(), source, "Mark Player", StringTool.print(markerText), pX, pY, !persistMarker);
			markerField.addActionListener(pActionEvent -> {
				String text = StringTool.print(markerField.getText());
				if (persistMarker) {
					pClient.getCommunication().sendSetMarker(pPlayer.getId(), text);
				} else {
					if (StringTool.isProvided(text)) {
						TransientPlayerMarker transientMarker = new TransientPlayerMarker(pPlayer.getId(), defaultMode);
						transientMarker.setHomeText(text);
						pClient.getGame().getFieldModel().addTransient(transientMarker);
						pClient.getUserInterface().getFieldComponent().getLayerPlayers().updatePlayerMarker(transientMarker);
					} else {
						pClient.getGame().getFieldModel().removeTransient((TransientPlayerMarker) playerMarker);
						pClient.getUserInterface().getFieldComponent().getLayerPlayers().updatePlayerMarker(playerMarker);
					}
					pClient.getUserInterface().refreshUi();
				}
			});
		}
	}

	public void showMarkerPopup(final FantasyFootballClient pClient, Component source, final FieldCoordinate pCoordinate, int pX, int pY) {
		if (pCoordinate != null) {
			Game game = pClient.getGame();
			boolean persistMarker = ClientMode.PLAYER == pClient.getMode();
			FieldMarker fieldMarker = persistMarker ? game.getFieldModel().getFieldMarker(pCoordinate) : game.getFieldModel().getTransientFieldMarker(pCoordinate);
			String markerText = (fieldMarker != null) ? fieldMarker.getHomeText() : null;

			final JTextField markerField = createMarkerPopup(pClient.getUserInterface(), source, "Mark Field", StringTool.print(markerText), pX, pY, false);
			markerField.addActionListener(pActionEvent -> {
				String text = StringTool.print(markerField.getText());
				if (persistMarker) {
					pClient.getCommunication().sendSetMarker(pCoordinate, text);
				} else {
					if (StringTool.isProvided(text)) {
						pClient.getUserInterface().getFieldComponent().getLayerMarker().removeFieldMarker(fieldMarker, true);
						FieldMarker transientMarker = new FieldMarker(pCoordinate, text, null);
						pClient.getGame().getFieldModel().addTransient(transientMarker);
						pClient.getUserInterface().getFieldComponent().getLayerMarker().drawFieldMarker(transientMarker, true);
					} else {
						pClient.getGame().getFieldModel().removeTransient(fieldMarker);
						pClient.getUserInterface().getFieldComponent().getLayerMarker().removeFieldMarker(fieldMarker, true);
					}
					pClient.getUserInterface().getFieldComponent().refreshUi();
				}
			});
		}
	}

    private JTextField createMarkerPopup(UserInterface ui, Component source, String pTitle, String pMarkerText, int pX, int pY, boolean includeMode) {
        DimensionProvider dimensionProvider = ui.getUiDimensionProvider();

        // 1. Create a transparent panel that covers the whole screen to act as the glass pane
        JPanel glassPane = new JPanel(null); // Null layout for absolute positioning
        glassPane.setOpaque(false); // Transparent background

        // 2. Define the cleanup logic
        Runnable cleanup = () -> {
            ui.setGlassPane(new JPanel()); // Reset to default
            ui.getGlassPane().setVisible(false);
        };

        // 3. Create your actual popup panel
        JPanel popupPanel = new JPanel();
        popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
        popupPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        popupPanel.setBackground(Color.WHITE);

        // 4. Handle clicks on the glass pane
        glassPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Check if the click is outside the popupPanel
                Point p = SwingUtilities.convertPoint(glassPane, e.getPoint(), popupPanel);
                if (!popupPanel.contains(p)) {
                    cleanup.run();
                }
            }
        });

        // 5. Build your UI components (as before)
        JPanel spacerPanel = new JPanel();
        spacerPanel.setLayout(new BoxLayout(spacerPanel, BoxLayout.Y_AXIS));
        spacerPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);

        if (StringTool.isProvided(pTitle)) {
            JLabel comp = new JLabel(dimensionProvider, pTitle);
            comp.setAlignmentX(JPanel.LEFT_ALIGNMENT);
            JPanel panel = new JPanel();
            panel.add(comp);
            spacerPanel.add(panel);
        }

        JTextField markerField = new JTextField(dimensionProvider, 7);
        if (StringTool.isProvided(pMarkerText)) {
            markerField.setText(pMarkerText);
        }

        // Action on Enter
        markerField.addActionListener(pActionEvent -> cleanup.run());

        spacerPanel.add(markerField);

        if (includeMode) {
            JComboBox<TransientPlayerMarker.Mode> modeComboBox = new JComboBox<>(dimensionProvider, TransientPlayerMarker.Mode.values());
            modeComboBox.setRenderer(new MarkerCellRenderer(dimensionProvider));
            modeComboBox.setSelectedItem(defaultMode);
            modeComboBox.addActionListener(pActionEvent -> {
                defaultMode = modeComboBox.getSelectedItem();
                markerField.requestFocus();
            });
            spacerPanel.add(modeComboBox);
        }

        popupPanel.add(spacerPanel);
        popupPanel.setSize(popupPanel.getPreferredSize());

        // 6. Positioning
        Dimension offset = offset(ui, source, dimensionProvider);
        popupPanel.setLocation(pX + offset.width + ui.getX(), pY + offset.height + ui.getY());

        // 7. Add popup to the glass pane, NOT the LayeredPane
        glassPane.add(popupPanel);

        // 8. Set and show
        ui.setGlassPane(glassPane);
        glassPane.setVisible(true);

        // 1. Ensure the field gets focus
        markerField.requestFocusInWindow();
        markerField.selectAll();

        // 2. Add ESC key binding to close the popup easily
        // This allows the user to press ESC to dismiss instead of clicking away
        popupPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "closePopup");
        popupPanel.getActionMap().put("closePopup", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cleanup.run();
            }
        });

        return markerField;
    }

	private Dimension offset(UserInterface ui, Component source, DimensionProvider dimensionProvider) {
		Dimension dimension = new Dimension(0, 0);

		if (source == ui.getFieldComponent()) {
			dimension.width = (int) dimensionProvider.dimension(com.fumbbl.ffb.client.Component.SIDEBAR).getWidth();
		} else if (source == ui.getSideBarAway()) {
			dimension.width = (int) (dimensionProvider.dimension(com.fumbbl.ffb.client.Component.SIDEBAR).getWidth() + dimensionProvider.dimension(com.fumbbl.ffb.client.Component.FIELD).getWidth());
		}
		return dimension;
	}

	private static class MarkerCellRenderer extends JPanel implements ListCellRenderer<TransientPlayerMarker.Mode> {

		private final JLabel label;

		public MarkerCellRenderer(DimensionProvider dimensionProvider) {
			label = new JLabel(dimensionProvider);
			add(label);
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends TransientPlayerMarker.Mode> list, TransientPlayerMarker.Mode value, int index, boolean isSelected, boolean cellHasFocus) {
			label.setText(value.getDisplayText());
			return this;
		}
	}
}
