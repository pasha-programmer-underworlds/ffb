package com.fumbbl.ffb.client;

import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;

public class PitchViewPort implements MouseWheelListener {
	private float zoomFactor = 1f;
	private int zoomStep = 0;
	private Point zoomPoint = new Point(0, 0);
	private FieldComponent fieldComponent;

	public void setFieldComponent(FieldComponent fieldComponent) {
		this.fieldComponent = fieldComponent;
	}

	protected void paintField(BufferedImage field, Graphics pGraphics) {
		Graphics2D g2 = (Graphics2D) pGraphics;
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

		// Apply the transform helper
		g2.transform(getTransform());

		g2.drawImage(field, 0, 0, null);
	}

	public Dimension convertToZoomedDimension(Dimension d) {
		Rectangle rectangle = convertToZoomedRectangle(new Rectangle(d.width, d.height, 0, 0));
		return new Dimension(rectangle.x, rectangle.y);
	}

	public Rectangle convertToZoomedRectangle(Rectangle r) {
		if (r == null) {
			return null;
		}
		// Create the transform and apply it to the rectangle
		Shape transformedShape = getTransform().createTransformedShape(r);

		// Return the bounds of the resulting shape
		return transformedShape.getBounds();
	}

	private AffineTransform getTransform() {
		AffineTransform at = new AffineTransform();
		at.translate(zoomPoint.x, zoomPoint.y);
		at.scale(zoomFactor, zoomFactor);
		at.translate(-zoomPoint.x, -zoomPoint.y);
		return at;
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		// Check if Ctrl is held down
		if (e.isControlDown()) {
			int wheelRotation = e.getWheelRotation();
			if (wheelRotation < 0) {
				zoomStep++;
				zoomStep = Math.min(zoomStep, 14);
				if (zoomStep < 14) {
					Point point = e.getPoint();
					zoomPoint = getOriginalPoint(point.x, point.y);
				}
			} else {
				zoomStep--;
				zoomStep = Math.max(zoomStep, 0);
			}
			zoomFactor = zoomStep == 0 ? 1 : 1 + zoomStep * 0.2f;
			fieldComponent.refresh(null);
			System.out.println("Ctrl + Zoom " + (wheelRotation < 0 ? "In" : "Out") + " zoomFactor: " + zoomFactor + " point: " + zoomPoint + "zoomStep: " + zoomStep);
		}
	}


	public Point getOriginalPoint(int mouseX, int mouseY) {
		try {
			// Use the inverse of the transform helper
			AffineTransform inverseTransform = getTransform().createInverse();
			Point originalPoint = new Point();
			inverseTransform.transform(new Point(mouseX, mouseY), originalPoint);
			return originalPoint;
		} catch (NoninvertibleTransformException ex) {
			return new Point(mouseX, mouseY);
		}
	}
}
