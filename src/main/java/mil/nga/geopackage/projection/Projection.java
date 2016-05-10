package mil.nga.geopackage.projection;

import mil.nga.geopackage.core.srs.SpatialReferenceSystem;

import org.osgeo.proj4j.CoordinateReferenceSystem;

/**
 * Single EPSG Projection
 * 
 * @author osbornb
 */
public class Projection {

	/**
	 * EPSG code
	 */
	private final long epsg;

	/**
	 * Coordinate Reference System
	 */
	private final CoordinateReferenceSystem crs;

	/**
	 * Constructor
	 * 
	 * @param epsg
	 * @param crs
	 */
	Projection(long epsg, CoordinateReferenceSystem crs) {
		this.epsg = epsg;
		this.crs = crs;
	}

	/**
	 * Get the EPSG code
	 * 
	 * @return
	 */
	public long getEpsg() {
		return epsg;
	}

	/**
	 * Get the Coordinate Reference System
	 * 
	 * @return
	 */
	public CoordinateReferenceSystem getCrs() {
		return crs;
	}

	/**
	 * Get the transformation from this Projection to the EPSG code. Each thread
	 * of execution should have it's own transformation.
	 * 
	 * @param epsg
	 * @return
	 */
	public ProjectionTransform getTransformation(long epsg) {
		Projection projectionTo = ProjectionFactory.getProjection(epsg);
		return getTransformation(projectionTo);
	}

	/**
	 * Get the transformation from this Projection to the Spatial Reference
	 * System. Each thread of execution should have it's own transformation.
	 * 
	 * @param srs
	 *            spatial reference system
	 * @return projection transform
	 * @since 1.1.8
	 */
	public ProjectionTransform getTransformation(SpatialReferenceSystem srs) {
		Projection projectionTo = ProjectionFactory.getProjection(srs);
		return getTransformation(projectionTo);
	}

	/**
	 * Get the transformation from this Projection to the provided projection.
	 * Each thread of execution should have it's own transformation.
	 * 
	 * @param projection
	 * @return
	 */
	public ProjectionTransform getTransformation(Projection projection) {
		return new ProjectionTransform(this, projection);
	}

	/**
	 * Convert the value to meters
	 * 
	 * @param value
	 * @return
	 */
	public double toMeters(double value) {
		return value / crs.getProjection().getFromMetres();
	}

}
