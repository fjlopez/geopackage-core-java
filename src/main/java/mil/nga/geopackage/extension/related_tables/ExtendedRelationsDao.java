package mil.nga.geopackage.extension.related_tables;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mil.nga.geopackage.schema.TableColumnKey;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.PreparedDelete;
import com.j256.ormlite.stmt.PreparedUpdate;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.j256.ormlite.support.ConnectionSource;

/**
 * Geometry Columns Data Access Object
 * 
 * @author osbornb
 */
public class ExtendedRelationsDao extends
		BaseDaoImpl<ExtendedRelations, TableColumnKey> {

	/**
	 * Constructor, required by ORMLite
	 * 
	 * @param connectionSource
	 * @param dataClass
	 * @throws SQLException
	 */
	public ExtendedRelationsDao(ConnectionSource connectionSource,
			Class<ExtendedRelations> dataClass) throws SQLException {
		super(connectionSource, dataClass);
	}

	/**
	 * Get all the base table names
	 * 
	 * @return base table names
	 * @throws SQLException
	 */
	public List<String> getBaseTables() throws SQLException {

		List<String> baseTables = new ArrayList<String>();

		List<ExtendedRelations> extendedRelations = queryForAll();
		for (ExtendedRelations extendedRelation : extendedRelations) {
			baseTables.add(extendedRelation.getBaseTableName());
		}

		return baseTables;
	}
//
//	/**
//	 * {@inheritDoc}
//	 */
//	@Override
//	public GeometryColumns queryForId(TableColumnKey key) throws SQLException {
//		GeometryColumns geometryColumns = null;
//		if (key != null) {
//			Map<String, Object> fieldValues = new HashMap<String, Object>();
//			fieldValues.put(GeometryColumns.COLUMN_TABLE_NAME,
//					key.getTableName());
//			fieldValues.put(GeometryColumns.COLUMN_COLUMN_NAME,
//					key.getColumnName());
//			List<GeometryColumns> results = queryForFieldValues(fieldValues);
//			if (!results.isEmpty()) {
//				if (results.size() > 1) {
//					throw new SQLException("More than one "
//							+ GeometryColumns.class.getSimpleName()
//							+ " returned for key. Table Name: "
//							+ key.getTableName() + ", Column Name: "
//							+ key.getColumnName());
//				}
//				geometryColumns = results.get(0);
//			}
//		}
//		return geometryColumns;
//	}
//
//	/**
//	 * {@inheritDoc}
//	 */
//	@Override
//	public TableColumnKey extractId(GeometryColumns data) throws SQLException {
//		return data.getId();
//	}
//
//	/**
//	 * {@inheritDoc}
//	 */
//	@Override
//	public boolean idExists(TableColumnKey id) throws SQLException {
//		return queryForId(id) != null;
//	}
//
//	/**
//	 * {@inheritDoc}
//	 */
//	@Override
//	public GeometryColumns queryForSameId(GeometryColumns data)
//			throws SQLException {
//		return queryForId(data.getId());
//	}
//
//	/**
//	 * {@inheritDoc}
//	 */
//	@Override
//	public int updateId(GeometryColumns data, TableColumnKey newId)
//			throws SQLException {
//		int count = 0;
//		GeometryColumns readData = queryForId(data.getId());
//		if (readData != null && newId != null) {
//			readData.setId(newId);
//			count = update(readData);
//		}
//		return count;
//	}
//
//	/**
//	 * {@inheritDoc}
//	 */
//	@Override
//	public int delete(GeometryColumns data) throws SQLException {
//		DeleteBuilder<GeometryColumns, TableColumnKey> db = deleteBuilder();
//
//		db.where().eq(GeometryColumns.COLUMN_TABLE_NAME, data.getTableName())
//				.and()
//				.eq(GeometryColumns.COLUMN_COLUMN_NAME, data.getColumnName());
//
//		PreparedDelete<GeometryColumns> deleteQuery = db.prepare();
//		int deleted = delete(deleteQuery);
//		return deleted;
//	}
//
//	/**
//	 * {@inheritDoc}
//	 */
//	@Override
//	public int deleteById(TableColumnKey id) throws SQLException {
//		int count = 0;
//		if (id != null) {
//			GeometryColumns geometryColumns = queryForId(id);
//			if (geometryColumns != null) {
//				count = delete(geometryColumns);
//			}
//		}
//		return count;
//	}
//
//	/**
//	 * {@inheritDoc}
//	 */
//	@Override
//	public int deleteIds(Collection<TableColumnKey> idCollection)
//			throws SQLException {
//		int count = 0;
//		if (idCollection != null) {
//			for (TableColumnKey id : idCollection) {
//				count += deleteById(id);
//			}
//		}
//		return count;
//	}
//
//	/**
//	 * {@inheritDoc}
//	 * 
//	 * Update using the complex key
//	 */
//	@Override
//	public int update(GeometryColumns geometryColumns) throws SQLException {
//
//		UpdateBuilder<GeometryColumns, TableColumnKey> ub = updateBuilder();
//		ub.updateColumnValue(GeometryColumns.COLUMN_GEOMETRY_TYPE_NAME,
//				geometryColumns.getGeometryTypeName());
//		ub.updateColumnValue(GeometryColumns.COLUMN_SRS_ID,
//				geometryColumns.getSrsId());
//		ub.updateColumnValue(GeometryColumns.COLUMN_Z, geometryColumns.getZ());
//		ub.updateColumnValue(GeometryColumns.COLUMN_M, geometryColumns.getM());
//
//		ub.where()
//				.eq(GeometryColumns.COLUMN_TABLE_NAME,
//						geometryColumns.getTableName())
//				.and()
//				.eq(GeometryColumns.COLUMN_COLUMN_NAME,
//						geometryColumns.getColumnName());
//
//		PreparedUpdate<GeometryColumns> update = ub.prepare();
//		int updated = update(update);
//
//		return updated;
//	}
//
//	/**
//	 * Query for the table name
//	 * 
//	 * @param tableName
//	 * @return geometry columns
//	 * @throws SQLException
//	 */
//	public GeometryColumns queryForTableName(String tableName)
//			throws SQLException {
//		GeometryColumns geometryColumns = null;
//		if (tableName != null) {
//			List<GeometryColumns> results = queryForEq(
//					GeometryColumns.COLUMN_TABLE_NAME, tableName);
//			if (!results.isEmpty()) {
//				if (results.size() > 1) {
//					throw new SQLException("More than one "
//							+ GeometryColumns.class.getSimpleName()
//							+ " returned for Table Name: " + tableName);
//				}
//				geometryColumns = results.get(0);
//			}
//		}
//		return geometryColumns;
//	}
//
}
