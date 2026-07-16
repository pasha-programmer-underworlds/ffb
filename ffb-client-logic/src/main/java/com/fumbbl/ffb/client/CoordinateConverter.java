package com.fumbbl.ffb.client;

import com.fumbbl.ffb.FieldCoordinate;

import java.awt.*;
import java.awt.event.MouseEvent;

public class CoordinateConverter {
	private final UiDimensionProvider uiDimensionProvider;
	private final PitchDimensionProvider pitchDimensionProvider;
	private final PitchViewPort pitchViewPort;

	public CoordinateConverter(UiDimensionProvider uiDimensionProvider,
	                           PitchDimensionProvider pitchDimensionProvider,
	                           PitchViewPort pitchViewPort) {
		this.uiDimensionProvider = uiDimensionProvider;
		this.pitchDimensionProvider = pitchDimensionProvider;
		this.pitchViewPort = pitchViewPort;
	}

	public FieldCoordinate getFieldCoordinate(MouseEvent pMouseEvent) {
		FieldCoordinate coordinate = null;
		int x = pMouseEvent.getX();
		int y = pMouseEvent.getY();
		Point originalPoint = pitchViewPort.getOriginalPoint(x, y);
		int origX = (int) originalPoint.getX();
		int origY = (int) originalPoint.getY();

		Dimension field = uiDimensionProvider.dimension(Component.FIELD);
		if ((origX > 0) && (origX < field.width) && (origY > 0) && (origY < field.height)) {
			int calculatedX = (int)
					((origX / (pitchDimensionProvider.getLayoutSettings().getScale()
							* pitchDimensionProvider.getLayoutSettings().getLayout().getPitchScale()))
							/ pitchDimensionProvider.unscaledFieldSquare());
			int calculatedY = (int)
					((origY / (pitchDimensionProvider.getLayoutSettings().getScale()
							* pitchDimensionProvider.getLayoutSettings().getLayout().getPitchScale()))
							/ pitchDimensionProvider.unscaledFieldSquare());
			coordinate = new FieldCoordinate(calculatedX, calculatedY);
			coordinate = pitchDimensionProvider.mapToGlobal(coordinate);
		}
		return coordinate;
	}

}
