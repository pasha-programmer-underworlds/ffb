package com.fumbbl.ffb.client;

import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;

public class PitchViewPort implements MouseWheelListener, MouseMotionListener, MouseInputListener {
	private float zoomFactor = 1f;
	private int zoomStep = 0;
	private Point zoomPoint = new Point(0, 0);
	private FieldComponent fieldComponent;
	private Point lastMouseDraggPoint;
	private final Point panOffset = new Point(0, 0);
	private BufferedImage fieldImage;

	public void setFieldComponent(FieldComponent fieldComponent) {
		this.fieldComponent = fieldComponent;
	}

	protected void paintField(BufferedImage field, Graphics pGraphics) {
		this.fieldImage = field; // Cache the reference
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

	private void clampPanOffset() {
		if (fieldImage == null || fieldComponent == null) {
			return;
		}

		int viewW = fieldComponent.getWidth();
		int viewH = fieldComponent.getHeight();

		if (viewW == 0 || viewH == 0) {
			return;
		}

		int imgW = fieldImage.getWidth();
		int imgH = fieldImage.getHeight();

		// Calculate the current scaled size
		float scaledW = imgW * zoomFactor;
		float scaledH = imgH * zoomFactor;

		// 1. Calculate the 'Zoom Shift'
		// This is the offset created by the transform's focus on zoomPoint.
		float zoomShiftX = zoomPoint.x * (1 - zoomFactor);
		float zoomShiftY = zoomPoint.y * (1 - zoomFactor);

		// 2. Calculate current effective position
		// This is where the image top-left actually lands on the screen.
		float effectiveX = panOffset.x + zoomShiftX;
		float effectiveY = panOffset.y + zoomShiftY;

		// 3. Define the bounds
		// The image can't be pulled further right than 0,
		// and can't be pulled further left than (viewWidth - scaledWidth).
		float minX = Math.min(0, viewW - scaledW);
		float minY = Math.min(0, viewH - scaledH);

		// 4. Clamp the effective position
		effectiveX = Math.max(minX, Math.min(0, effectiveX));
		effectiveY = Math.max(minY, Math.min(0, effectiveY));

		// 5. Convert back to panOffset and update
		// panOffset = effectivePosition - zoomShift
		panOffset.x = (int) (effectiveX - zoomShiftX);
		panOffset.y = (int) (effectiveY - zoomShiftY);
	}

//	private AffineTransform getTransform() {
//		AffineTransform at = new AffineTransform();
//		at.translate(zoomPoint.x, zoomPoint.y);
//		at.scale(zoomFactor, zoomFactor);
//		at.translate(-zoomPoint.x, -zoomPoint.y);
//		return at;
//	}

	private AffineTransform getTransform() {
		AffineTransform at = new AffineTransform();
		// 1. Translate by the Pan Offset (Movement)
		at.translate(panOffset.x, panOffset.y);
		// 2. Translate to Zoom Point (Zoom Center)
		at.translate(zoomPoint.x, zoomPoint.y);
		// 3. Scale
		at.scale(zoomFactor, zoomFactor);
		// 4. Translate back
		at.translate(-zoomPoint.x, -zoomPoint.y);
		return at;
	}

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
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
        // Clamping here ensures that if the user zooms out,
        // the image snaps back into view instead of staying "lost" off-screen.
        clampPanOffset();
        fieldComponent.repaint();
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


	@Override
	public void mousePressed(MouseEvent e) {
		if ((e.getModifiersEx() & InputEvent.BUTTON2_DOWN_MASK) != 0) {
			lastMouseDraggPoint = e.getPoint();
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if ((e.getModifiersEx() & InputEvent.BUTTON2_DOWN_MASK) != 0 && lastMouseDraggPoint != null) {
			int dx = e.getX() - lastMouseDraggPoint.x;
			int dy = e.getY() - lastMouseDraggPoint.y;

			panOffset.x += dx;
			panOffset.y += dy;

			// --- ADD THIS LINE ---
			clampPanOffset();
			// ---------------------

			lastMouseDraggPoint = e.getPoint();
			fieldComponent.repaint();
		}
	}


	@Override
	public void mouseReleased(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON2) {
			// Reset the tracker so the next drag doesn't jump
			lastMouseDraggPoint = null;
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {

	}

	@Override
	public void mouseExited(MouseEvent e) {

	}

	@Override
	public void mouseClicked(MouseEvent e) {

	}

	@Override
	public void mouseMoved(MouseEvent e) {

	}
}
