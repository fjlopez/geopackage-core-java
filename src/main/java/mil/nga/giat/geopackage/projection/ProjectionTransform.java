package mil.nga.giat.geopackage.projection;

import mil.nga.giat.geopackage.BoundingBox;
import mil.nga.giat.wkb.geom.Point;

import org.osgeo.proj4j.CoordinateTransform;
import org.osgeo.proj4j.CoordinateTransformFactory;
import org.osgeo.proj4j.ProjCoordinate;

/**
 * Projection transform wrapper
 * 
 * @author osbornb
 */
public class ProjectionTransform {

	/**
	 * Coordinate transform factory
	 */
	private static CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();

	/**
	 * From Projection
	 */
	private final Projection fromProjection;

	/**
	 * To Projection
	 */
	private final Projection toProjection;

	/**
	 * Coordinate transform
	 */
	private final CoordinateTransform transform;

	/**
	 * Constructor
	 * 
	 * @param fromProjection
	 * @param toProjection
	 */
	ProjectionTransform(Projection fromProjection, Projection toProjection) {
		this.fromProjection = fromProjection;
		this.toProjection = toProjection;
		this.transform = ctFactory.createTransform(fromProjection.getCrs(),
				toProjection.getCrs());
	}

	/**
	 * Transform the projected coordinate
	 * 
	 * @param from
	 * @return
	 */
	public ProjCoordinate transform(ProjCoordinate from) {
		ProjCoordinate to = new ProjCoordinate();
		transform.transform(from, to);
		return to;
	}

	/**
	 * Transform the projected point
	 * 
	 * @param from
	 * @return
	 */
	public Point transform(Point from) {

		ProjCoordinate fromCoord;
		if (from.hasZ()) {
			fromCoord = new ProjCoordinate(from.getX(), from.getY(),
					from.getZ() != null ? from.getZ() : Double.NaN);
		} else {
			fromCoord = new ProjCoordinate(from.getX(), from.getY());
		}

		ProjCoordinate toCoord = transform(fromCoord);

		Point to = new Point(from.hasZ(), from.hasM(), toCoord.x, toCoord.y);
		if (from.hasZ()) {
			to.setZ(toCoord.z);
		}
		if (from.hasM()) {
			to.setM(from.getM());
		}

		return to;
	}

	/**
	 * Transform the bounding box
	 * 
	 * @param boundingBox
	 * @return
	 */
	public BoundingBox transform(BoundingBox boundingBox) {

		ProjCoordinate lowerLeft = new ProjCoordinate(
				boundingBox.getMinLongitude(), boundingBox.getMinLatitude());
		ProjCoordinate upperRight = new ProjCoordinate(
				boundingBox.getMaxLongitude(), boundingBox.getMaxLatitude());

		ProjCoordinate projectedLowerLeft = transform(lowerLeft);
		ProjCoordinate projectedUpperRight = transform(upperRight);

		BoundingBox projectedBoundingBox = new BoundingBox(
				projectedLowerLeft.x, projectedUpperRight.x,
				projectedLowerLeft.y, projectedUpperRight.y);

		return projectedBoundingBox;
	}

	/**
	 * Transform latitude
	 * 
	 * @param y
	 * @return
	 */
	public double transformLatitude(double y) {
		ProjCoordinate fromCoord = new ProjCoordinate(0, y);
		ProjCoordinate toCoord = transform(fromCoord);
		return toCoord.y;
	}

	/**
	 * Transform longitude
	 * 
	 * @param x
	 * @return
	 */
	public double transformLongitude(double x) {
		ProjCoordinate fromCoord = new ProjCoordinate(x, 0);
		ProjCoordinate toCoord = transform(fromCoord);
		return toCoord.x;
	}

	/**
	 * Get the from projection in the transform
	 * 
	 * @return
	 */
	public Projection getFromProjection() {
		return fromProjection;
	}

	/**
	 * Get the to projection in the transform
	 * 
	 * @return
	 */
	public Projection getToProjection() {
		return toProjection;
	}

	/**
	 * Get the transform
	 * 
	 * @return
	 */
	public CoordinateTransform getTransform() {
		return transform;
	}

}